package io.buildpal.workspace;

import io.buildpal.core.domain.Build;
import io.buildpal.core.domain.Phase;
import io.buildpal.core.domain.Repository;
import io.buildpal.core.domain.Workspace;
import io.buildpal.core.pipeline.Plugin;
import io.buildpal.core.pipeline.event.Command;
import io.buildpal.core.pipeline.event.CommandKey;
import io.buildpal.core.pipeline.event.Event;
import io.buildpal.core.pipeline.event.EventKey;
import io.buildpal.core.util.FileUtils;
import io.buildpal.workspace.vcs.FileSystemController;
import io.buildpal.workspace.vcs.GitController;
import io.buildpal.workspace.vcs.P4Controller;
import io.buildpal.workspace.vcs.SyncResult;
import io.buildpal.workspace.vcs.VersionController;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.buildpal.auth.vault.VaultService.RETRIEVE_DATA_ADDRESS;
import static io.buildpal.core.config.Constants.DATA_FOLDER_PATH;
import static io.buildpal.core.config.Constants.DELETE_WORKSPACE_ADDRESS;
import static io.buildpal.core.domain.Entity.ID;
import static io.buildpal.core.domain.Entity.NAME;
import static io.buildpal.core.util.FileUtils.SLASH;
import static io.buildpal.core.util.FileUtils.slashify;

public class WorkspaceVerticle extends Plugin {
    private final static Logger logger = LoggerFactory.getLogger(WorkspaceVerticle.class);

    private static final String WORKSPACES = "workspaces";
    private static final String PERMS_755 = "rwxr-xr-x";
    private static final String PHASES_PATH = "/.buildpal/phases/";

    private FileSystem fs;
    private WorkerExecutor workerExecutor;
    private LocalMap<String, JsonObject> activeWorkspaces;

    private String workspacesRootPath;

    @Override
    public Set<CommandKey> commandKeysToRegister() {
        return Set.of(CommandKey.SETUP, CommandKey.RUN_PHASE, CommandKey.TEAR_DOWN);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public void start() {
        fs = vertx.fileSystem();

        activeWorkspaces = vertx.sharedData().getLocalMap(WORKSPACES);

        // TODO: Use config for pool size and max execution time.
        workerExecutor = vertx.createSharedWorkerExecutor("WORKSPACE-POOL", 5, 7200000000000L);

        preparePaths(config());
    }

    @Override
    protected void starting(Future<Void> startFuture) {
        vertx.eventBus().consumer(DELETE_WORKSPACE_ADDRESS, deleteHandler());

        startFuture.complete();
    }

    /**
     * Creates and sets the workspace for a build by executing the following steps:
     * 1) Obtain a lock on the workspace path
     * 2) Create a new workspace - deletes existing folders and files, if any
     * 3) Download source code from the repository
     */
    @Override
    protected Handler<Message<JsonObject>> setupHandler(EventBus eb) {
        return mh -> {

            Command command = new Command(mh.body());
            Build build = command.getBuild();

            Workspace workspace = makeWorkspace(build);

            Event setupEndEvent = new Event()
                    .setKey(EventKey.SETUP_END)
                    .setBuildID(build.getID())
                    .setWorkspace(workspace);

            if (lockWorkspace(workspace.getID(), build.getID())) {

                try {
                    // Prepare user folder.
                    if (!fs.existsBlocking(workspace.getUserPath())) {
                        fs.mkdirBlocking(workspace.getUserPath());

                        // Grant permissions so non-root users can set this path for caching and other things.
                        FileUtils.chmod757(workspace.getUserPath());
                    }

                    syncWorkspaceWithCreds(build, workspace, build.getRepository(), setupEndEvent);

                } catch (Exception ex) {
                    syncError(workspace, setupEndEvent, ex);
                    fireSetupEndEvent(setupEndEvent);
                }

            } else {
                syncError(workspace, setupEndEvent, new Exception("Workspace is in use: " + workspace.getID()));
                fireSetupEndEvent(setupEndEvent);
            }
        };
    }

    /**
     * Sync the current phase of the build.
     */
    protected Handler<Message<JsonObject>> phaseHandler(EventBus eb) {
        return mh -> {

            Command command = new Command(mh.body());
            Build build = command.getBuild();
            Workspace workspace = build.getWorkspace();
            Phase phase = command.getPhase();

            Event phaseEndEvent = new Event()
                    .setKey(EventKey.PHASE_END)
                    .setBuildID(build.getID())
                    .setPhase(phase);

            boolean syncInProgress = false;

            try {
                Repository repository = build.getRepository();
                Repository childRepo = findChildRepo(repository, phase);

                if (childRepo != null) {
                    Workspace childWorkspace = getChildRepoWorkspace(workspace, childRepo);

                    if (!fs.existsBlocking(childWorkspace.getPath())) {
                        syncInProgress = true;
                        syncWorkspaceForPhaseWithCreds(build, repository, childWorkspace, childRepo, phaseEndEvent);

                    } else {
                        logger.info("Child repo already synced: " + childRepo.getName());
                    }
                }

            } catch (Exception ex) {
                syncInProgress = false;
                syncError(workspace, phaseEndEvent, ex);
            }

            if (!syncInProgress) {
                firePhaseEndEvent(phaseEndEvent);
            }
        };
    }

    protected Handler<Message<JsonObject>> tearDownHandler(EventBus eb) {
        return mh -> {
            Command command = new Command(mh.body());
            Build build = command.getBuild();

            Event tearDownEndEvent = new Event()
                    .setKey(EventKey.TEAR_DOWN_END)
                    .setBuildID(build.getID());

            revertWithCreds(build, tearDownEndEvent);
        };
    }

    private Handler<Message<JsonObject>> deleteHandler() {
        return mh -> {
            String userID = mh.body().getString(ID);
            String userPath = workspacesRootPath + userID;

            long count = activeWorkspaces.keySet().stream().filter(key -> key.startsWith(userID)).count();

            if (count == 0) {
                deleteUserWorkspace(userPath);
            } else {
                logger.warn("Cannot delete active workspace for user: " + userID);
            }
        };
    }

    private void fireSetupEndEvent(Event setupEndEvent) {
        vertx.eventBus().send(setupEndEvent.getKey().getAddress(), setupEndEvent.json());
    }

    private void firePhaseEndEvent(Event phaseEndEvent) {
        vertx.eventBus().send(EventKey.PHASE_END.getAddress(), phaseEndEvent.json());
    }

    private void fireTearDownEndEvent(Event tearDownEndEvent) {
        vertx.eventBus().send(tearDownEndEvent.getKey().getAddress(), tearDownEndEvent.json());
    }

    private void preparePaths(JsonObject config) {

        String dataFolderPath = Objects.requireNonNull(config.getString(DATA_FOLDER_PATH),
                "Data folder path must be configured.");

        workspacesRootPath = slashify(dataFolderPath) + WORKSPACES + SLASH;

        File workspacesRoot = new File(workspacesRootPath);

        if (!workspacesRoot.exists()) {
            fs.mkdirsBlocking(workspacesRootPath, PERMS_755);
        }
    }

    private Workspace makeWorkspace(Build build) {
        String userID = build.getCreatedBy();
        String userPath = workspacesRootPath + userID;
        String workspaceID = userID + "_" + build.getPipelineID();
        String workspacePath = userPath + SLASH + build.getPipelineID();
        String phasesPath = workspacePath + PHASES_PATH;

        return new Workspace()
                .setID(workspaceID)
                .setUserPath(userPath)
                .setRootPath(workspacePath)
                .setPath(workspacePath)
                .setPhasesPath(phasesPath);
    }

    private boolean lockWorkspace(String workspaceID, String buildID) {
        if (activeWorkspaces.containsKey(workspaceID)) {
            return false;
        }

        activeWorkspaces.putIfAbsent(workspaceID, new JsonObject().put(ID, buildID));
        return true;
    }

    private boolean isWorkspaceLockedByBuild(String workspaceID, String buildID) {
        JsonObject data = activeWorkspaces.get(workspaceID);

        return data != null && buildID.equals(data.getString(ID));
    }

    private void prepareWorkspace(Workspace workspace) throws Exception {
        // Delete existing workspace.
        if (fs.existsBlocking(workspace.getPath())) {
            fs.deleteRecursiveBlocking(workspace.getPath(), true);
        }

        // Prepare new workspace.
        fs.mkdirBlocking(workspace.getPath());
        FileUtils.chmod757(workspace.getPath());
    }

    private void syncWorkspaceWithCreds(Build build, Workspace workspace, Repository repository, Event setupEndEvent) {
        if (repository != null && repository.hasSecret()) {

            JsonObject data = new JsonObject().put(NAME, repository.getSecret().getName());

            vertx.eventBus().<JsonObject>send(RETRIEVE_DATA_ADDRESS, data, rh -> {
                if (rh.succeeded()) {
                    JsonObject secretJson = rh.result().body();

                    if (secretJson != null) {
                        build.setRepositorySecret(secretJson);
                        syncWorkspace(build, workspace, repository, setupEndEvent);

                    } else {
                        syncError(workspace, setupEndEvent, new Exception("Unable to retrieve data from vault."));
                        fireSetupEndEvent(setupEndEvent);
                    }

                } else {
                    syncError(workspace, setupEndEvent, rh.cause());
                    fireSetupEndEvent(setupEndEvent);
                }
            });

        } else {
            syncWorkspace(build, workspace, repository, setupEndEvent);
        }
    }

    private void syncWorkspace(Build build, Workspace workspace, Repository repository, Event setupEndEvent) {
        // Run on the worker pool - might take a while to sync from VCS remote server.
        workerExecutor.executeBlocking(bch -> {
            try {
                prepareWorkspace(workspace);

                VersionController versionController = repoVersionController(workspace, repository);
                versionController.sync(build.data(), build.getRepositorySecret());

                bch.complete();

            } catch (Exception ex) {
                bch.fail(ex);

            } finally {
                build.clearRepositorySecret();
            }

        }, false, rh -> {
            if (rh.succeeded()) {
                // Create phases folder.
                fs.mkdirsBlocking(workspace.getPhasesPath(), PERMS_755);

                // Pass the updated repository (updated metadata).
                setupEndEvent.setRepository(repository);

            } else {
                syncError(workspace, setupEndEvent, rh.cause());
            }

            fireSetupEndEvent(setupEndEvent);
        });
    }

    private void syncWorkspaceForPhaseWithCreds(Build build, Repository repository, Workspace childWorkspace,
                                                Repository childRepo, Event phaseEndEvent) {

        if (repository != null && repository.hasSecret()) {

            JsonObject data = new JsonObject().put(NAME, repository.getSecret().getName());

            vertx.eventBus().<JsonObject>send(RETRIEVE_DATA_ADDRESS, data, rh -> {
                if (rh.succeeded()) {
                    JsonObject secretJson = rh.result().body();

                    if (secretJson != null) {
                        build.setRepositorySecret(secretJson);
                        syncWorkspaceForPhase(build, childWorkspace, repository, childRepo, phaseEndEvent);

                    } else {
                        syncError(childWorkspace, phaseEndEvent, new Exception("Unable to retrieve data from vault."));
                        firePhaseEndEvent(phaseEndEvent);
                    }

                } else {
                    syncError(childWorkspace, phaseEndEvent, rh.cause());
                    firePhaseEndEvent(phaseEndEvent);
                }
            });

        } else {
            syncWorkspaceForPhase(build, childWorkspace, repository, childRepo, phaseEndEvent);
        }
    }

    private void syncWorkspaceForPhase(Build build, Workspace childWorkspace,
                                       Repository repository, Repository childRepo, Event phaseEndEvent) {
        // Run on the worker pool - might take a while to sync from VCS remote server.
        workerExecutor.executeBlocking(bch -> {
            try {
                VersionController versionController = repoVersionController(childWorkspace, childRepo);
                versionController.sync(build.data(), build.getRepositorySecret());

                bch.complete();

            } catch (Exception ex) {
                bch.fail(ex);

            } finally {
                build.clearRepositorySecret();
            }

        }, false, rh -> {
            if (rh.failed()) {
                syncError(childWorkspace, phaseEndEvent, rh.cause());
            }

            // Pass the updated repository (updated metadata).
            phaseEndEvent.setChildRepository(childRepo);

            firePhaseEndEvent(phaseEndEvent);
        });
    }

    private void revertWithCreds(Build build, Event tearDownEvent) {
        Workspace workspace = build.getWorkspace();
        Repository repository = build.getRepository();

        if (repository != null && repository.hasSecret()) {

            JsonObject data = new JsonObject().put(NAME, repository.getSecret().getName());

            vertx.eventBus().<JsonObject>send(RETRIEVE_DATA_ADDRESS, data, rh -> {
                if (rh.succeeded()) {
                    JsonObject secretJson = rh.result().body();

                    if (secretJson != null) {
                        build.setRepositorySecret(secretJson);
                        revert(build, workspace, repository, tearDownEvent);

                    } else {
                        revertError(workspace, tearDownEvent, new Exception("Unable to retrieve data from vault."));
                        fireTearDownEndEvent(tearDownEvent);
                    }

                } else {
                    revertError(workspace, tearDownEvent, rh.cause());
                    fireTearDownEndEvent(tearDownEvent);
                }
            });

        } else {
            revert(build, workspace, repository, tearDownEvent);
        }
    }

    private void revert(Build build, Workspace workspace, Repository repository, Event tearDownEndEvent) {
        workerExecutor.executeBlocking(bch -> {

            List<VersionController>  versionControllers = repoVersionControllersToRevert(workspace, repository);

            for (VersionController versionController : versionControllers) {
                try {
                    versionController.revert(build.getRepositorySecret());

                } catch (Exception ex) {
                    logger.error("Unable to revert: " + versionController.getRepository().json(), ex);
                }
            }

            build.clearRepositorySecret();

            bch.complete();

        }, false, rh -> {

            String workspaceID = getWorkspaceID(build, workspace);

            if (workspaceID != null) {
                // Unlock the workspace only if the current build owns it.
                if (isWorkspaceLockedByBuild(workspaceID, build.getID())) {

                    if (activeWorkspaces.containsKey(workspaceID)) {
                        activeWorkspaces.remove(workspaceID);

                    } else {
                        logger.error("Unable to unlock workspace: " + workspaceID);
                    }

                } else {
                    logger.warn("Workspace locked by another build: " + workspaceID);
                }
            }

            fireTearDownEndEvent(tearDownEndEvent);
        });
    }

    private void deleteUserWorkspace(String userPath) {
        vertx.fileSystem().exists(userPath, eh -> {
            if (eh.result()) {
                vertx.fileSystem().deleteRecursive(userPath, true, dh-> {
                    if (dh.failed()) {
                        logger.error("Unable to delete user workspace: " + userPath, dh.cause());
                    }
                });
            }
        });
    }

    private String getWorkspaceID(Build build, Workspace workspace) {
        if (workspace != null) {
            return workspace.getID();

        } else {
            String buildID = build.getID();

            for (Map.Entry<String, JsonObject> entry : activeWorkspaces.entrySet()) {
                JsonObject data = entry.getValue();

                if (data != null && buildID.equals(data.getString(ID))) {
                    return entry.getKey();
                }
            }

            return null;
        }
    }

    private VersionController repoVersionController(Workspace workspace, Repository repository) {
        VersionController versionController;

        switch (repository.getType()) {
            case FS:
                versionController = new FileSystemController(repository, workspace);
                break;

            case GIT:
                versionController = new GitController(repository, workspace);
                break;

            case P4:
                versionController = new P4Controller(repository, workspace);
                break;

            case MULTI_GIT:
            case MULTI_P4:
                versionController = multiRepoVersionController(workspace, repository);
                break;

            default:
                versionController = (data, secret) ->
                        logger.warn("Workspace has no associated version control: " + workspace.getID());
        }

        return versionController;
    }

    private VersionController multiRepoVersionController(Workspace workspace, Repository repository) {

        // See if one of the children is marked for search - find pipeline.js.
        JsonArray children = repository.getChildren();

        for (int c=0; c<children.size(); c++) {
            Repository childRepo = new Repository(children.getJsonObject(c));

            if (childRepo.hasPipeline()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Child repo with pipeline found: " + childRepo.json());
                }

                // Mark the main repository for pipeline scan.
                repository.setHasPipeline(true);

                if (childRepo.getType() == Repository.Type.GIT) {
                    return new GitController(childRepo, getChildRepoWorkspace(workspace, childRepo));

                } else {
                    return new P4Controller(childRepo, getChildRepoWorkspace(workspace, childRepo));
                }
            }
        }

        return (data, sh) -> SyncResult.success();
    }

    private List<VersionController> repoVersionControllersToRevert(Workspace workspace, Repository repository) {
        List<VersionController> versionControllers = new ArrayList<>();

        if (workspace != null && repository != null) {
            // Only perforce syncs needs corresponding reverts.
            switch (repository.getType()) {
                case P4:
                    versionControllers.add(new P4Controller(repository, workspace));
                    break;

                case MULTI_P4:
                    JsonArray children = repository.getChildren();

                    for (int c = 0; c < children.size(); c++) {
                        Repository childRepo = new Repository(children.getJsonObject(c));

                        if (childRepo.getType() == Repository.Type.P4) {
                            versionControllers.add(new P4Controller(childRepo, getChildRepoWorkspace(workspace, childRepo)));
                        }
                    }

                    break;

                default:
                    break;
            }
        }

        return versionControllers;
    }

    private Repository findChildRepo(Repository repository, Phase phase) {
        Repository.Type type = repository.getType();

        if (type == Repository.Type.MULTI_GIT || type == Repository.Type.MULTI_P4) {
            JsonArray children = repository.getChildren();

            for (int c = 0; c < children.size(); c++) {
                Repository childRepo = new Repository(children.getJsonObject(c));

                if (phase.getRepo().equalsIgnoreCase(childRepo.getName())) {

                    if (repository.hasSecret()) {
                        childRepo.setSecret(repository.getSecret().json());
                    }

                    return childRepo;
                }
            }
        }

        return null;
    }

    private Workspace getChildRepoWorkspace(Workspace workspace, Repository childRepo) {
        return workspace.cloneMe()
                .setPath(slashify(workspace.getPath()) + childRepo.getName());
    }

    private void syncError(Workspace workspace, Event event, Throwable cause) {
        String error = "Unable to sync workspace: " + workspace.getID();
        logger.error(error, cause);

        event.setStatusCode(500).setStatusMessage(error);
    }

    private void revertError(Workspace workspace, Event event, Throwable cause) {
        String error = "Unable to revert workspace: " + workspace.getID();
        logger.error(error, cause);

        event.setStatusCode(500).setStatusMessage(error);
    }
}
