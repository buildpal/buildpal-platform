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
import io.buildpal.core.domain.Pipeline;
import io.buildpal.core.domain.PipelineJs;
import io.buildpal.core.domain.Status;
import io.buildpal.db.file.PipelineManager;
import io.buildpal.node.engine.Engine;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;

import java.util.ArrayList;
import java.util.List;

import static io.buildpal.core.config.Constants.ITEM;
import static io.buildpal.core.config.Constants.SUBJECT;
import static io.buildpal.core.domain.Build.BUILD;
import static io.buildpal.core.domain.Pipeline.JS;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.core.util.ResultUtils.msgFailed;

public class PipelineRouter extends CrudRouter<Pipeline> {
    private static final Logger logger = LoggerFactory.getLogger(PipelineRouter.class);

    private static final String SCRIPT = "script";

    private final PipelineManager pipelineManager;

    public PipelineRouter(Vertx vertx, JWTAuth jwtAuth, List<String> authorities,
                          PipelineManager pipelineManager) {

        super(vertx, logger, jwtAuth, authorities, pipelineManager, Pipeline::new);

        this.pipelineManager = pipelineManager;
    }

    @Override
    protected void configureRoutes(String collectionPath, Vertx vertx) {
        super.configureRoutes(collectionPath, vertx);

        configureStartRoute(collectionPath);
        configureUploadRoute(collectionPath);
        configureDownloadRoute(collectionPath);
    }

    private void configureStartRoute(String collectionPath) {
        String path = collectionPath + ID_PATH + "/start";

        router.route(HttpMethod.POST, path).handler(routingContext -> {

            String id = routingContext.request().getParam(ID_PARAM);
            JsonObject rawData = tryParseData(routingContext);

            // Get pipeline.
            dbManager.getGraph(id, gh -> {
                if (failed(gh)) {
                    writeResponse(routingContext, gh.result());

                } else {
                    // Get pipeline script
                    pipelineManager.downloadJs(id, dh -> {
                        String script = null;

                        if (!failed(dh.result())) {
                            script = dh.result().getJsonObject(ITEM).getString(JS);
                        }

                        // Save pipeline instance and send start message.
                        saveAndStartInstance(newInstance(rawData, gh.result()), script, routingContext);
                    });
                }
            });
        });
    }

    private void configureUploadRoute(String collectionPath) {
        String path = collectionPath + ID_PATH + "/upload";

        router.route(HttpMethod.POST, path).handler(routingContext -> {

            String id = routingContext.request().getParam(ID_PARAM);
            List<PipelineJs> pipelineJsList = new ArrayList<>();

            routingContext.fileUploads().forEach(fileUpload -> pipelineJsList
                    .add(new PipelineJs().setID(fileUpload.fileName()).setFilePath(fileUpload.uploadedFileName())));

            pipelineManager.uploadJs(id, pipelineJsList, r -> writeResponse(routingContext, r.result()));
        });
    }

    private void configureDownloadRoute(String collectionPath) {
        String path = collectionPath + ID_PATH + "/download";

        router.route(HttpMethod.GET, path).handler(routingContext -> {

            String id = routingContext.request().getParam(ID_PARAM);
            pipelineManager.downloadJs(id, r -> writeResponse(routingContext, r.result()));
        });
    }

    private void saveAndStartInstance(Build build, String script, RoutingContext routingContext) {

        vertx.eventBus().<JsonObject>send(BuildRouter.ADD, build.json(), options(routingContext), reply -> {
            if (msgFailed(reply)) {
                logger.error("Unable to start pipeline instance", reply.cause());

            } else {
                // Asynchronously start the instance.
                JsonObject engineMessage = new JsonObject()
                        .put(BUILD,reply.result().body().getJsonObject(ITEM))
                        .put(SCRIPT, script);

                vertx.eventBus().send(Engine.START, engineMessage);
            }

            writeResponse(routingContext, reply.result().body());
        });
    }

    private Build newInstance(JsonObject rawData, JsonObject pipelineResult) {
        Pipeline pipeline = new Pipeline(pipelineResult.getJsonObject(ITEM));

        Build build = new Build()
                .setName(pipeline.getName())
                .setPipelineID(pipeline.getID())
                .setRepository(pipeline.getRepository())
                .setStatus(Status.PARKED)
                .setData(rawData, pipeline.dataList());

        return build;
    }

    private DeliveryOptions options(RoutingContext routingContext) {
        DeliveryOptions options = new DeliveryOptions();
        options.addHeader(SUBJECT, routingContext.user().principal().getString(SUBJECT));

        return options;
    }

    private static JsonObject tryParseData(RoutingContext routingContext) {
        JsonObject data;

        try {
            data = routingContext.getBodyAsJson();

        } catch (Exception ex) {
            data = new JsonObject();
        }

        return data;
    }
}
