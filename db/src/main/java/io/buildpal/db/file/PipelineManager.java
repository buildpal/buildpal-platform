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

package io.buildpal.db.file;

import io.buildpal.core.domain.Pipeline;
import io.buildpal.core.domain.PipelineJs;
import io.buildpal.core.domain.validation.PipelineJsValidator;
import io.buildpal.core.domain.validation.Validator;
import io.buildpal.core.util.FileUtils;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static io.buildpal.core.domain.Pipeline.JS;
import static io.buildpal.core.domain.Pipeline.REPOSITORY_ID;
import static io.buildpal.core.util.ResultUtils.addError;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.core.util.ResultUtils.newResult;
import static io.buildpal.core.util.ResultUtils.putEntity;
import static io.buildpal.core.util.VertxUtils.future;

public class PipelineManager extends FileDbManager {

    private static final Logger logger = LoggerFactory.getLogger(PipelineManager.class);

    private static final String PIPELINE_JS_PATH = "%s%s.js";

    private final RepositoryManager repositoryManager;
    private final PipelineJsValidator pipelineJsValidator;

    public PipelineManager(Vertx vertx, JsonObject  config, Validator validator,
                           RepositoryManager repositoryManager) {
        super(vertx, logger, config, validator);

        this.repositoryManager = repositoryManager;

        pipelineJsValidator = new PipelineJsValidator();
    }

    @Override
    public String getCollectionName() {
        return "pipelines";
    }

    @Override
    public void getGraph(String id, Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> future = future(handler);
        JsonObject result = newResult(validator.validateID(id));

        if (failed(result)) {
            future.complete(result);
            // Validation failed.
            return;
        }

        // See if value is in the map.
        putEntity(result, get(id));

        if (failed(result)) {
            future.complete(result);

            // Entity not found.
            return;
        }

        Pipeline pipeline = new Pipeline(get(id));

        if (StringUtils.isNotBlank(pipeline.getRepositoryID())) {
            JsonObject repository = repositoryManager.getGraph(pipeline.getRepositoryID());
            pipeline.setRepository(repository);
        }

        future.complete(putEntity(result, pipeline.json()));
    }

    public void uploadJs(String id, List<PipelineJs> pipelineJsList, Handler<AsyncResult<JsonObject>> handler) {

        Future<JsonObject> future = future(handler);
        JsonObject result = newResult(validateUpload(id, pipelineJsList));

        if (failed(result)) {
            future.complete(result);

        } else {
            uploadJs(id, pipelineJsList.get(0), result, future);
        }
    }

    public void downloadJs(String id, Handler<AsyncResult<JsonObject>> handler) {

        Future<JsonObject> future = future(handler);
        JsonObject result = newResult(validateDownload(id));

        if (failed(result)) {
            future.complete(result);

        } else {
            String path =  String.format(PIPELINE_JS_PATH, getCollectionPath(), id);

            Future<Boolean> existsFuture = Future.future();
            existsFuture
                    .compose(e -> getJsFuture(e, path))
                    .compose(js -> {

                        if (js != null) {
                            putEntity(result, new JsonObject().put(JS, js.toString()));

                        } else {
                            addError(result, "Pipeline javascript not found: " + id);
                        }

                        future.complete(result);

                    }, future);

            fs.exists(path, existsFuture.completer());
        }
    }

    @Override
    JsonObject validateEntity(JsonObject entity) {
        JsonObject result = newResult(validator.validate(entity));

        // No validation errors so far. Now, check for repository.
        if (!failed(result)) {

            if (entity.containsKey(REPOSITORY_ID) &&
                    repositoryManager.get(entity.getString(REPOSITORY_ID)) == null) {
                addError(result, "Specified repository does not exist.");
            }
        }

        return result;
    }

    @Override
    void save(JsonObject entity, boolean isAdd, JsonObject result, Future<JsonObject> future) {

        String js = entity.getString(JS);

        if (entity.containsKey(JS)) {
            entity.remove(JS);
        }

        // Save to file.
        fs.writeFile(getPath(entity), Buffer.buffer(entity.encode()), wh -> {

            if (wh.succeeded()) {
                // Save to map.
                collectionMap.put(getID(entity), entity);

                if (js != null) {
                    saveJs(entity, js, result, future);

                } else {
                    deleteJs(entity, result, future);
                }

            } else {
                String message = "Failed to save entity: " + entity;
                future.complete(addError(result, message));
                logger.error(message, wh.cause());
            }
        });
    }

    private List<String> validateUpload(String id, List<PipelineJs> pipelineJsList) {
        List<String> errors = validator.validateID(id);

        // If a valid ID is specified, check if the pipeline entity exists.
        if (errors.isEmpty()) {
            if (get(id) == null) {
                errors.add("Pipeline entity not found.");
            }
        }

        if (pipelineJsList.isEmpty() || pipelineJsList.size() != 1) {
            errors.add("A single pipeline javascript file has to be uploaded.");
        }

        pipelineJsList.forEach(pipelineJsValidator::validateEntity);

        return errors;
    }

    private List<String> validateDownload(String id) {
        List<String> errors = validator.validateID(id);

        // If a valid ID is specified, check if the pipeline entity exists.
        if (errors.isEmpty()) {
            if (get(id) == null) {
                errors.add("Pipeline entity not found.");
            }
        }

        return errors;
    }

    private void uploadJs(String id, PipelineJs pipelineJs, JsonObject result, Future<JsonObject> future) {
        String newPath =  String.format(PIPELINE_JS_PATH, getCollectionPath(), id);

        FileUtils.deleteIfPresent(fs, newPath, dh -> {

            if (dh.succeeded()) {
                fs.move(pipelineJs.getFilePath(), newPath, mh -> {

                    if (mh.failed()) {
                        logger.error("Unable to upload pipeline javascript file: " + id, mh.cause());
                        addError(result, "Unable to upload pipeline javascript file: " + id);
                    }

                    future.complete(result);
                });

            } else {
                logger.error("Unable to upload pipeline javascript file: " + id, dh.cause());
                addError(result, "Unable to upload pipeline javascript file: " + id);

                future.complete(result);
            }
        });
    }

    private void saveJs(JsonObject entity, String js, JsonObject result, Future<JsonObject> future) {
        String id = getID(entity);
        String newPath =  String.format(PIPELINE_JS_PATH, getCollectionPath(), id);

        FileUtils.deleteIfPresent(fs, newPath, dh -> {

            if (dh.succeeded()) {
                fs.writeFile(newPath, Buffer.buffer(js), wh -> {

                    if (wh.failed()) {
                        logger.error("Unable to save pipeline javascript file: " + id, wh.cause());
                        addError(result, "Unable to save pipeline javascript file: " + id);

                    } else {
                        putEntity(result, entity);
                    }

                    future.complete(result);
                });

            } else {
                logger.error("Unable to save pipeline javascript file: " + id, dh.cause());
                addError(result, "Unable to save pipeline javascript file: " + id);

                future.complete(result);
            }
        });
    }

    private void deleteJs(JsonObject entity, JsonObject result, Future<JsonObject> future) {
        String id = getID(entity);
        String newPath =  String.format(PIPELINE_JS_PATH, getCollectionPath(), id);

        FileUtils.deleteIfPresent(fs, newPath, dh -> {

            if (dh.succeeded()) {
                putEntity(result, entity);
                future.complete(result);

            } else {
                logger.error("Unable to delete pipeline javascript file: " + id, dh.cause());
                addError(result, "Unable to delete pipeline javascript file: " + id);

                future.complete(result);
            }
        });
    }

    private Future<Buffer> getJsFuture(boolean exists, String path) {
        Future<Buffer> getFuture = Future.future();

        if (exists) {
            fs.readFile(path, getFuture.completer());

        } else {
            getFuture.complete();
        }

        return getFuture;
    }
}
