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

package io.buildpal.node.router;

import io.buildpal.core.domain.Build;
import io.buildpal.core.domain.Phase;
import io.buildpal.core.domain.Status;
import io.buildpal.db.DbManager;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static io.buildpal.core.config.Constants.BUILD_UPDATE_ADDRESS;
import static io.buildpal.core.config.Constants.ITEM;
import static io.buildpal.core.config.Constants.SUBJECT;
import static io.buildpal.core.config.Constants.SYSTEM;
import static io.buildpal.core.config.Constants.TAIL;
import static io.buildpal.core.domain.Entity.ID;
import static io.buildpal.core.domain.Entity.populate;
import static io.buildpal.core.util.ResultUtils.addError;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.core.util.ResultUtils.getEntity;
import static io.buildpal.core.util.ResultUtils.newResult;
import static io.buildpal.node.engine.Engine.ABORT;
import static io.buildpal.node.engine.Engine.DELETE;

public class BuildRouter extends CrudRouter<Build> {
    private static final Logger logger = LoggerFactory.getLogger(BuildRouter.class);

    static final String ADD = "build.add";

    private static final String CONTAINER_ID = "containerID";
    private static final String LOGS_PATH = "/logs?id=%s&tail=%s";

    private HttpClient containerLogsClient;

    public BuildRouter(Vertx vertx, JWTAuth jwtAuth, List<String> authorities, DbManager dbManager) {
        super(vertx, logger, jwtAuth, authorities, dbManager, Build::new);

        containerLogsClient = vertx.createHttpClient();
    }

    @Override
    protected void configureRoutes(String collectionPath, Vertx vertx) {
        configureGetCollectionRoute(collectionPath);

        configureGetRoute(collectionPath);
        configureDeleteRoute(collectionPath);
        configureLogsRoute(collectionPath);
        configureAbortRoute(collectionPath);

        vertx.eventBus().consumer(ADD, addHandler());
        vertx.eventBus().consumer(BUILD_UPDATE_ADDRESS, updateHandler());
    }

    private Handler<Message<JsonObject>> addHandler() {
        return message -> {
            Build build = builder.build(message.body())
                    .setStatus(Status.PARKED);
            populate(build, message.headers().get(SUBJECT));

            dbManager.add(build.json(), ah -> message.reply(ah.result()));
        };
    }

    private Handler<Message<JsonObject>> updateHandler() {
        return message -> {
            Build build = builder.build(message.body())
                    .setUtcLastModifiedDate(Instant.now(Clock.systemUTC()))
                    .setLastModifiedBy(SYSTEM);

            dbManager.replace(build.json(), ah -> message.reply(ah.result()));
        };
    }

    @Override
    protected void configureDeleteRoute(String collectionPath) {
        router.route(HttpMethod.DELETE, collectionPath + ID_PATH).handler(routingContext -> {
            String id = routingContext.request().getParam(ID_PARAM);

            dbManager.get(id, gh -> {

                if (failed(gh)) {
                    writeResponse(routingContext, gh.result());

                } else {

                    Build build = new Build(gh.result().getJsonObject(ITEM));

                    if (build.canDelete()) {
                        dbManager.delete(id, r -> write202Response(routingContext, r.result()));

                        // Delete the pipeline instance asynchronously.
                        vertx.eventBus().send(DELETE, build.json());

                    } else {
                        JsonObject result = addError(newResult(),
                                "Cannot delete build when it is in " + build.getStatus() + " status.");
                        writeResponse(routingContext, result);
                    }
                }
            });
        });
    }

    private void configureLogsRoute(String collectionPath) {
        String logsPath = collectionPath + ID_PATH + "/logs";

        router.route(HttpMethod.GET, logsPath).handler(routingContext -> {
            String id = routingContext.request().getParam(ID_PARAM);
            String containerID = routingContext.request().getParam(CONTAINER_ID);

            if (StringUtils.isBlank(containerID)) {
                JsonObject result = addError(newResult(),
                        "Container ID should be provided for build logs: " + id);
                writeResponse(routingContext, result);
                return;
            }

            dbManager.get(id, gh -> {

                if (failed(gh)) {
                    writeResponse(routingContext, gh.result());

                } else {

                    Build build = new Build(getEntity(gh.result()));
                    Build.BuildPhase phase = build.findPhase(containerID);

                    if (phase != null && phase.hasContainerHost() && phase.hasContainerPort()) {
                        getContainerLogs(containerID, phase, routingContext);

                    } else {
                        JsonObject result = addError(newResult(),
                                "No matching phase found for container ID: " + containerID);
                        writeResponse(routingContext, result);
                    }
                }
            });
        });
    }

    private void configureAbortRoute(String collectionPath) {
        String abortPath = collectionPath + ID_PATH + "/abort";

        router.route(HttpMethod.POST, abortPath).handler(routingContext -> {
            String id = routingContext.request().getParam(ID_PARAM);

            // Pull the build from the DB to see if it can be aborted in the first place.
            dbManager.get(id, gh -> {

                if (failed(gh)) {
                    writeResponse(routingContext, gh.result());

                } else {

                    Build build = new Build(gh.result().getJsonObject(ITEM));

                    if (build.canAbort()) {
                        // Abort the pipeline instance (build).
                        vertx.eventBus().<JsonObject>send(ABORT, build.json(), reply -> {

                            if (reply.succeeded()) {
                                // Save updated build.
                                dbManager.replace(reply.result().body(),
                                        rh -> write202Response(routingContext, rh.result()));

                            } else {
                                writeResponse(routingContext, addError(newResult(), "Unable to abort the build."));
                            }
                        });


                    } else {
                        JsonObject result = addError(newResult(),
                                "Cannot abort build when it is in " + build.getStatus() + " status.");
                        writeResponse(routingContext, result);
                    }
                }
            });
        });
    }

    private void getContainerLogs(String containerID, Build.BuildPhase phase, RoutingContext routingContext) {
        String tail = routingContext.request().getParam(TAIL);

        if (StringUtils.isBlank(tail)) {
            tail = "all";
        }

        String requestUri = String.format(LOGS_PATH, containerID, tail);

        HttpClientRequest request = containerLogsClient.get(phase.getContainerPort(),
                phase.getContainerHost(),
                requestUri,
                logsResponse -> {

            if (logsResponse.statusCode() == 200) {
                routingContext.response().setChunked(true);

                Pump pump = Pump.pump(logsResponse, routingContext.response());

                logsResponse.endHandler(eh -> {
                    // pump.stop();
                    routingContext.response().end();
                });

                pump.start();

            } else {
                logsResponse.bodyHandler(bh -> writeResponse(routingContext, bh.toJsonObject()));
            }
        });

        request.exceptionHandler(ex -> {
            String error = "Unable to get logs for container: " + containerID;
            logger.error(error, ex);
            writeResponse(routingContext, addError(newResult(), error));
        });

        request.end();
    }
}
