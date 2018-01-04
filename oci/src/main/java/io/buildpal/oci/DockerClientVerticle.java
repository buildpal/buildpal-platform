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

package io.buildpal.oci;

import io.buildpal.core.domain.Phase;
import io.buildpal.core.domain.Workspace;
import io.buildpal.core.pipeline.Plugin;
import io.buildpal.core.pipeline.event.Command;
import io.buildpal.core.pipeline.event.CommandKey;
import io.buildpal.core.pipeline.event.Event;
import io.buildpal.core.pipeline.event.EventKey;
import io.buildpal.core.util.FileUtils;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Objects;
import java.util.Set;

import static io.buildpal.core.config.Constants.BUILDPAL_DATA_VOLUME;
import static io.buildpal.core.config.Constants.DASH;
import static io.buildpal.core.config.Constants.DATA_FOLDER_PATH;
import static io.buildpal.core.util.FileUtils.COLON;
import static io.buildpal.core.util.FileUtils.DOT_SLASH;

public class DockerClientVerticle extends Plugin {
    private static final Logger logger = LoggerFactory.getLogger(DockerClientVerticle.class);

    private static final String FIND_IMAGE = "/images/json?filters=%s";
    private static final String PULL_IMAGE = "/images/create?fromImage=%s";
    private static final String CREATE = "/containers/create?name=%s";
    private static final String START = "/containers/%s/start";
    private static final String WAIT = "/containers/%s/wait";

    private static final String STATUS_CODE = "StatusCode";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_JSON = "application/json";

    private static final String FIND_IMAGE_ERROR = "Failed to find image %s. Error: %s";
    private static final String PULL_IMAGE_ERROR = "Failed to pull image %s. Error: %s";
    private static final String CREATE_ERROR = "Failed to create container %s. Error: %s";
    private static final String START_ERROR = "Failed to start container %s. Error: %s";
    private static final String WAIT_ERROR = "Failed to wait for container %s. Error: %s";

    private JsonArray binds = new JsonArray();
    private HttpClient dockerClient;

    @Override
    public Set<CommandKey> commandKeysToRegister() {
        return Set.of(CommandKey.RUN_PHASE);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public void start() throws Exception {
        if (!vertx.isNativeTransportEnabled()) {
            logger.error("Native transport enabled: " + vertx.isNativeTransportEnabled());
            throw new Exception("Native transport should be enabled and available");
        }

        String dataFolderPath = Objects.requireNonNull(config().getString(DATA_FOLDER_PATH),
                "Data folder path must be configured.");
        dataFolderPath = FileUtils.unslashify(dataFolderPath);

        binds.add(BUILDPAL_DATA_VOLUME + COLON + dataFolderPath);

        HttpClientOptions clientOptions = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_1_1_UNIX)
                .setDefaultHost("/var/run/docker.sock");

        dockerClient = vertx.createHttpClient(clientOptions);
    }

    @Override
    protected Handler<Message<JsonObject>> phaseHandler(EventBus eb) {
        return mh -> {
            Command command = new Command(mh.body());
            Phase phase = command.getPhase();

            Event phaseEndEvent = new Event()
                    .setKey(EventKey.PHASE_END)
                    .setBuildID(command.getBuild().getID())
                    .setPhase(phase);

            findImage(command, phase, phaseEndEvent);
        };
    }

    private void firePhaseEndEvent(Event phaseEndEvent) {
        vertx.eventBus().send(EventKey.PHASE_END.getAddress(), phaseEndEvent.json());
    }

    private void findImage(Command command, Phase phase, Event phaseEndEvent) {

        String image = phase.getContainerArgs().getImage();
        JsonObject filters = new JsonObject().put("reference", new JsonArray().add(image));

        HttpClientRequest request = dockerClient.get(String.format(FIND_IMAGE, filters.encode()), r -> {
            r.bodyHandler(bh -> {
                if (r.statusCode() != 200) {
                    error(String.format(FIND_IMAGE_ERROR, image, bh.toString()), phaseEndEvent, null);

                } else {
                    JsonArray images = bh.toJsonArray();

                    if (images.isEmpty()) {
                        pull(image, command, phase, phaseEndEvent);

                    } else {
                        createContainer(command, phase, phaseEndEvent);
                    }
                }
            });
        });

        request.exceptionHandler(ex ->
                error(String.format(FIND_IMAGE_ERROR, image, ""), phaseEndEvent, ex));

        addHeaders(request);
        request.end();
    }

    private void pull(String image, Command command, Phase phase, Event phaseEndEvent) {

        HttpClientRequest request = dockerClient.post(String.format(PULL_IMAGE, image), r -> {
            r.bodyHandler(bh -> {
                if (r.statusCode() != 200) {
                    error(String.format(PULL_IMAGE_ERROR, image, bh.toString()), phaseEndEvent, null);

                } else {
                    createContainer(command, phase, phaseEndEvent);
                }
            });
        });

        request.exceptionHandler(ex ->
                error(String.format(PULL_IMAGE_ERROR, image, ""), phaseEndEvent, ex));

        addHeaders(request);
        request.end();
    }

    private void createContainer(Command command, Phase phase, Event phaseEndEvent) {
        String name = getContainerName(command, phase);

        HttpClientRequest request = dockerClient.post(String.format(CREATE, name), r -> {
            r.bodyHandler(bh -> {
                if (r.statusCode() != 201) {
                    error(String.format(CREATE_ERROR, name, bh.toString()), phaseEndEvent, null);

                } else {
                    phase.setContainerID(bh.toJsonObject().getString("Id"));

                    startContainer(name, command, phase, phaseEndEvent);
                }
            });
        });

        request.exceptionHandler(ex ->
                error(String.format(CREATE_ERROR, name, ""), phaseEndEvent, ex));

        addHeaders(request);
        request.end(getContainerConfig(command.getBuild().getWorkspace(), phase).encode());
    }

    private void startContainer(String name, Command command, Phase phase, Event phaseEndEvent) {

        HttpClientRequest request = dockerClient.post(String.format(START, name), r -> {
            r.bodyHandler(bh -> {
                if (r.statusCode() != 204) {
                    error(String.format(START_ERROR, name, bh.toString()), phaseEndEvent, null);

                } else {
                    waitContainer(name, command, phase, phaseEndEvent);
                }
            });
        });

        request.exceptionHandler(ex ->
                error(String.format(START_ERROR, name, ""), phaseEndEvent, ex));

        addHeaders(request);
        request.end();
    }

    private void waitContainer(String name, Command command, Phase phase, Event phaseEndEvent) {

        HttpClientRequest request = dockerClient.post(String.format(WAIT, name), r -> {
            r.bodyHandler(bh -> {
                if (r.statusCode() != 200) {
                    error(String.format(WAIT_ERROR, name, bh.toString()), phaseEndEvent, null);

                } else {
                    // All done! Let's see if the container run was smooth or not.
                    int containerStatusCode = bh.toJsonObject().getInteger(STATUS_CODE, -1);

                    if (containerStatusCode != 0) {
                        String error = String.format(WAIT_ERROR, name, "");

                        // A stop/kill command was used. No need to log.
                        if (containerStatusCode == 137 || containerStatusCode == 128) {
                            phaseEndEvent.setStatusCode(containerStatusCode).setStatusMessage(error);

                        } else {
                            logger.error(error, bh.toString());
                            phaseEndEvent.setStatusCode(500).setStatusMessage(error);
                        }
                    }

                    firePhaseEndEvent(phaseEndEvent);
                }
            });
        });

        request.exceptionHandler(ex ->
                error(String.format(WAIT_ERROR, name, ""), phaseEndEvent, ex));

        addHeaders(request);
        request.end();
    }

    private String getContainerName(Command command, Phase phase) {
        return command.getBuild().getID() + DASH + phase.getID();
    }

    private JsonObject getContainerConfig(Workspace workspace, Phase phase) {
        Phase.ContainerArgs containerArgs = phase.getContainerArgs();

        JsonObject config = new JsonObject()
                .put("Image", containerArgs.getImage());

        if (containerArgs.hasUser()) {
            config.put("User", containerArgs.getUser());
        }

        config.put("WorkingDir", workspace.getPhasesPath());
        config.put("Cmd", String.format(DOT_SLASH, phase.getMainScriptFile()));

        config.put("HostConfig", new JsonObject().put("Binds", binds));

        return config;
    }

    private void addHeaders(HttpClientRequest request) {
        request.putHeader("host", "localhost");
        request.putHeader("accept", "application/json");
        request.putHeader(CONTENT_TYPE, CONTENT_JSON);
    }

    private void error(String error, Event event, Throwable cause) {
        logger.error(error, cause);

        event.setStatusCode(500).setStatusMessage(error);
        firePhaseEndEvent(event);
    }
}
