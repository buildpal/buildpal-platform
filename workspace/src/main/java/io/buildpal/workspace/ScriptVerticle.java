/*
 * Copyright 2017 Buildpal
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import io.buildpal.core.process.ExternalProcess;
import io.buildpal.core.util.FileUtils;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static io.buildpal.core.config.Constants.COMMA;
import static io.buildpal.core.config.Constants.SYSTEM_FOLDER_PATH;
import static io.buildpal.core.util.FileUtils.DOT_SLASH;
import static io.buildpal.core.util.FileUtils.NEW_LINE;

public class ScriptVerticle extends Plugin {
    private final static Logger logger = LoggerFactory.getLogger(ScriptVerticle.class);

    private static final String INSTANCES_PATH = "instances/";

    private static final String HEADER_TEMPLATE = "/templates/header";
    private static final String FOOTER_TEMPLATE = "/templates/footer";

    private static final String END_FIELD = "'" + COMMA + NEW_LINE;

    private static final String  SH_PERMS = "rwxr-xr-x";

    private static final JsonArray EMPTY_ARRAY = new JsonArray();

    private String instancesRootPath;
    private WorkerExecutor workerExecutor;

    @Override
    public Set<CommandKey> commandKeysToRegister() {
        return Set.of(CommandKey.SETUP);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public void start() throws Exception {
        String systemFolderPath = Objects.requireNonNull(config().getString(SYSTEM_FOLDER_PATH),
                "System folder path must be configured.");

        instancesRootPath = FileUtils.slashify(systemFolderPath) + INSTANCES_PATH;

        if (!new File(instancesRootPath).exists()) {
            vertx.fileSystem().mkdirsBlocking(instancesRootPath);
        }

        File jsRoot = new File(instancesRootPath, "js");

        // Always use the latest and greatest JS classes.
        if (jsRoot.exists()) {
            vertx.fileSystem().deleteRecursiveBlocking(jsRoot.getPath(), true);
        }

        vertx.fileSystem().mkdirBlocking(jsRoot.getPath());

        for (File file : listResourceFiles("js")) {
            copyResource(file, jsRoot);
        }

        // TODO: Use config for pool size and max execution time.
        workerExecutor = vertx.createSharedWorkerExecutor("SCRIPT-POOL", 5, 7200000000000L);
    }

    @Override
    protected Handler<Message<JsonObject>> setupHandler(EventBus eb) {
        return mh -> {
            Command command = new Command(mh.body());
            Build build = command.getBuild();
            Repository repository = build.getRepository();

            Event setupEndEvent = new Event()
                    .setKey(EventKey.SETUP_END)
                    .setBuildID(build.getID());

            if (repository.isPipelineScanOn() || repository.hasPipeline()) {
                // Try scanning the repository for script.[java|js]
                scanScript(build, setupEndEvent);

            } else {
                writeInstance(build, null, command.getScript(), setupEndEvent);
            }
        };
    }

    private void fireSetupEndEvent(Event setupEndEvent) {
        vertx.eventBus().send(setupEndEvent.getKey().getAddress(), setupEndEvent.json());
    }

    private void scanScript(Build build, Event setupEndEvent) {

        vertx.<Path>executeBlocking(bch -> {
            bch.complete(FileUtils.tryFind(build.getWorkspace().path(), "script.{js,java}"));

        }, false, ssh -> {
            Path scriptPath = ssh.result();

            if (scriptPath != null) {
                writeInstance(build, scriptPath, null, setupEndEvent);

            } else {
                // Nothing much can be done when we don't have the script.
                error(build, setupEndEvent, new Exception("Workspace scan did not find the pipeline script"));
            }
        });
    }

    private void writeInstance(Build build, Path scriptPath, String script, Event setupEndEvent) {

        try {
            // TODO: Get script extension from build object
            String ext = scriptPath != null ? FileUtils.extension(scriptPath) : ".js";

            // WARNING: Not memory efficient. If files get too large, use an alternate approach.
            List<String> lines = new ArrayList<>(getHeader(ext));

            lines.add("\nvar __buildID__ = '" + build.getID() + "';\n");
            lines.add(varGlobalEnv(build));
            lines.add(varData(build));

            if (scriptPath != null) {
                lines.addAll(Files.readAllLines(scriptPath));

            } else {
                lines.add(script);
            }

            lines.addAll(getFooter(ext));

            String fullScriptPath = instancesRootPath + build.getID() + ext;
            Files.write(Paths.get(fullScriptPath), lines);

            deployScript(build, fullScriptPath, setupEndEvent);

        } catch (Exception ex) {
            error(build, setupEndEvent, ex);
        }
    }

    private void deployScript(Build build, String fullScriptPath, Event setupEndEvent) {

        vertx.deployVerticle(fullScriptPath, dh -> {

            if (dh.succeeded()) {
                build.setDeploymentID(dh.result());

                vertx.eventBus().<JsonObject>send(build.getID(), "dryRun", rh -> {

                    JsonObject msg = rh.result().body();
                    boolean dryRunSuccess = msg.getBoolean("dryRunSuccess");

                    vertx.undeploy(build.getDeploymentID());

                    if (dryRunSuccess) {
                        setupEndEvent.setStages(msg.getJsonArray("_stages", EMPTY_ARRAY));
                        processScripts(build, setupEndEvent);

                    } else {
                        setupEndEvent.setStatusCode(500).setStatusMessage("Unable to evaluate pipeline script.");
                        fireSetupEndEvent(setupEndEvent);
                    }
                });

            } else {
                error(build, setupEndEvent, dh.cause());
            }
        });
    }

    private void processScripts(Build build, Event setupEndEvent) {
        List<Phase> allPhases = getAllPhases(setupEndEvent);

        String phasesPath = build.getWorkspace().getPhasesPath();

        createPreScripts(allPhases, phasesPath, build, setupEndEvent);
    }

    private void createPreScripts(List<Phase> allPhases, String phasesPath, Build build, Event setupEndEvent) {
        List<Future> createPreScriptFutures = new ArrayList<>();

        for (Phase phase : allPhases) {
            if (phase.hasPreScript()) {
                createPreScriptFutures.add(createFile(phasesPath, phase.getPreScriptFile()));
            }
        }

        CompositeFuture.all(createPreScriptFutures).setHandler(cpsh -> {
            if (cpsh.succeeded()) {
                writePreScripts(allPhases, phasesPath, build, setupEndEvent);

            } else {
                error(build, setupEndEvent, cpsh.cause());
            }
        });
    }

    private void writePreScripts(List<Phase> allPhases, String phasesPath, Build build, Event setupEndEvent) {
        List<Future> writePreScriptFutures = new ArrayList<>();

        for (Phase phase : allPhases) {
            if (phase.hasPreScript()) {
                writePreScriptFutures.add(writeFile(phasesPath, phase.getPreScriptFile(), phase.getPreScript()));
            }
        }

        CompositeFuture.all(writePreScriptFutures).setHandler(wpsh -> {
            if (wpsh.succeeded()) {
                runPreScripts(allPhases, phasesPath, build, setupEndEvent);

            } else {
                error(build, setupEndEvent, wpsh.cause());
            }
        });
    }

    private void runPreScripts(List<Phase> allPhases, String phasesPath, Build build, Event setupEndEvent) {
        List<Future> runPreScriptFutures = new ArrayList<>();

        for (Phase phase : allPhases) {
            if (phase.hasPreScript()) {
                runPreScriptFutures.add(runFile(phasesPath, phase.getPreScriptFile()));
            }
        }

        CompositeFuture.all(runPreScriptFutures).setHandler(rpsh -> {
            if (rpsh.succeeded()) {
                createMainScripts(allPhases, phasesPath, build, setupEndEvent);

            } else {
                error(build, setupEndEvent, rpsh.cause());
            }
        });
    }

    private void createMainScripts(List<Phase> allPhases, String phasesPath, Build build, Event setupEndEvent) {
        List<Future> createMainScriptFutures = new ArrayList<>();

        for (Phase phase : allPhases) {
            createMainScriptFutures.add(createFile(phasesPath, phase.getMainScriptFile()));
        }

        CompositeFuture.all(createMainScriptFutures).setHandler(cmsh -> {
            if (cmsh.succeeded()) {
                writeMainScripts(allPhases, phasesPath, build, setupEndEvent);

            } else {
                error(build, setupEndEvent, cmsh.cause());
            }
        });
    }

    private void writeMainScripts(List<Phase> allPhases, String phasesPath, Build build, Event setupEndEvent) {
        List<Future> writeMainScriptFutures = new ArrayList<>();

        for (Phase phase : allPhases) {
            writeMainScriptFutures.add(writeFile(phasesPath, phase.getMainScriptFile(), phase.getMainScript()));
        }

        CompositeFuture.all(writeMainScriptFutures).setHandler(wpsh -> {
            if (wpsh.succeeded()) {
                // Finally, all done!
                fireSetupEndEvent(setupEndEvent);

            } else {
                error(build, setupEndEvent, wpsh.cause());
            }
        });
    }

    private Future<Void> createFile(String parentPath, String fileName) {
        Future<Void> createFileFuture = Future.future();

        vertx.fileSystem()
                .createFile(parentPath + fileName, SH_PERMS, createFileFuture.completer());

        return createFileFuture;
    }

    private Future<Void> writeFile(String parentPath, String fileName, String data) {
        Future<Void> writeFileFuture = Future.future();

        vertx.fileSystem()
                .writeFile(parentPath + fileName, Buffer.buffer(data), writeFileFuture.completer());

        return writeFileFuture;
    }

    private Future<Void> runFile(String parentPath, String fileName) {
        Future<Void> runFileFuture = Future.future();

        // Run on the worker pool since the time to run the script is not predictable.
        workerExecutor.executeBlocking(bch -> {
            try {

                ExternalProcess process = new ExternalProcess()
                        .inDirectory(parentPath)
                        .withCommand(String.format(DOT_SLASH, fileName));

                int exitCode = process.run();

                if (exitCode == 0) {
                    bch.complete();

                } else {
                    bch.fail(new Exception(String.format("Unable to run pre-phase (%d): %s", exitCode, fileName)));
                }

            } catch (Exception ex) {
                logger.error("Unable to run pre-phase: " + fileName, ex);
                bch.fail(ex);
            }

        }, false, runFileFuture.completer());

        return runFileFuture;
    }

    private List<Phase> getAllPhases(Event setupEndEvent) {
        List<Phase> allPhases = new ArrayList<>();
        JsonArray stages = setupEndEvent.getStages();

        for (int s = 0; s < stages.size(); s++) {
            JsonArray phasesJson = stages.getJsonArray(s);

            for (int p = 0; p < phasesJson.size(); p++) {
                Phase phase = new Phase(phasesJson.getJsonObject(p));

                allPhases.add(phase);
            }
        }

        return allPhases;
    }

    private List<String> getHeader(String ext) throws IOException {
        return readResource(HEADER_TEMPLATE + ext);
    }

    private List<String> getFooter(String ext) throws IOException {
        return readResource(FOOTER_TEMPLATE + ext);
    }

    private String varGlobalEnv(Build build) {
        Workspace workspace = build.getWorkspace();

        return "var globalEnv = {" + NEW_LINE +
                "    'USER_ID': '" + build.getCreatedBy() + END_FIELD +
                "    'USER_PATH': '" + workspace.getUserPath() + END_FIELD +
                "    'WORKSPACE_ID': '" + workspace.getID() + END_FIELD +
                "    'WORKSPACE_PATH': '" + workspace.getPath() + END_FIELD +
                "    'BUILD_ID': '" + build.getID() + "'" + NEW_LINE +
                "};";
    }

    private String varData(Build build) {
        return "var data = JSON.parse('" + build.data().encode() + "');" + NEW_LINE;
    }

    private void error(Build build, Event event, Throwable cause) {
        String error = "Unable to run script plugin for build: " + build.getID();
        logger.error(error, cause);

        event.setStatusCode(500).setStatusMessage(error);

        fireSetupEndEvent(event);
    }

    private List<String> readResource(String resourcePath) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(resourcePath);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            List<String> result = new ArrayList<>();

            while (true) {
                String line = reader.readLine();

                if (line == null) break;

                result.add(line);
            }

            return result;
        }
    }

    private void copyResource(File resource, File destinationFolder) throws Exception {
        Files.copy(getClass().getResourceAsStream(resource.getPath()),
                new File(destinationFolder, resource.getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private List<File> listResourceFiles(String resourcePath) throws Exception {
        String path = "/" + resourcePath;
        URL url = getClass().getResource(path);

        if (url != null && url.getProtocol().equals("jar")) {

            String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));

            Enumeration<JarEntry> entries = jar.entries();
            Set<File> result = new HashSet<>();

            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();

                if (name.startsWith(resourcePath) && name.length() > (resourcePath.length() + 1)) {
                    result.add(new File("/" + name));
                }
            }

            return new ArrayList<>(result);
        }

        return List.of();
    }
}

