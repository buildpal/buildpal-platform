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

import io.buildpal.core.domain.validation.Validator;
import io.buildpal.core.query.QueryEngine;
import io.buildpal.core.query.QuerySpec;
import io.buildpal.core.util.FileUtils;
import io.buildpal.core.util.ResultUtils;
import io.buildpal.db.DbManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.shareddata.LocalMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.buildpal.core.config.Constants.SYSTEM_FOLDER_PATH;
import static io.buildpal.core.domain.Entity.ID;
import static io.buildpal.core.util.FileUtils.SLASH;
import static io.buildpal.core.util.ResultUtils.addError;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.core.util.ResultUtils.newResult;
import static io.buildpal.core.util.ResultUtils.putEntity;
import static io.buildpal.core.util.VertxUtils.future;

public abstract class FileDbManager implements DbManager {

    private static final String DB_PATH = "db/";
    private static final String ENTITY_PATH = "%s%s.json";
    private static final String JSON_FILTER = ".+\\.json";

    protected final Vertx vertx;
    final FileSystem fs;
    private final Logger logger;

    final Validator validator;

    private final String collectionPath;
    final LocalMap<String, JsonObject> collectionMap;

    FileDbManager(Vertx vertx, Logger logger, JsonObject config, Validator validator) {
        this.vertx = vertx;
        this.fs = vertx.fileSystem();
        this.logger = logger;

        this.validator = validator;

        collectionPath = setCollectionPath(config);
        collectionMap = vertx.sharedData().getLocalMap(getCollectionName());
    }

    String getCollectionPath() {
        return collectionPath;
    }

    JsonObject getGraph(String id) {
        return collectionMap.get(id);
    }

    JsonObject validateEntity(JsonObject entity) {
        return newResult(validator.validate(entity));
    }

    @Override
    public void init(JsonObject config, Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> future = future(handler);

        // Test for the presence of collection folder. If it does not exist, create one.
        FileUtils.createIfAbsent(fs, collectionPath, ch -> {
            if (ch.succeeded()) {

                if (logger.isDebugEnabled()) {
                    logger.debug("Reading collection: " + getCollectionName());
                }

                // Read existing entities into the map.
                FileUtils.readFiles(fs, collectionPath, JSON_FILTER, rfh -> {

                    if (rfh.succeeded()) {
                        rfh.result()
                                .forEach((key, value) -> collectionMap.put(key, new JsonObject(value)));

                        future.complete();

                    } else {
                        error(future, rfh);
                    }
                });

            } else {
                error(future, ch);
            }
        });
    }

    @Override
    public JsonObject get(String id) {
        return collectionMap.get(id);
    }

    @Override
    public void get(String id, Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> future = future(handler);
        JsonObject result = newResult(validator.validateID(id));

        // Return value from the local map.
        if (!failed(result)) {
            putEntity(result, collectionMap.get(id));
        }

        future.complete(result);
    }

    @Override
    public void getGraph(String id, Handler<AsyncResult<JsonObject>> handler) {
        get(id, handler);
    }

    @Override
    public void add(JsonObject entity, Handler<AsyncResult<JsonObject>> handler) {
        save(entity, true, handler);
    }

    @Override
    public void replace(JsonObject entity, Handler<AsyncResult<JsonObject>> handler) {
        save(entity, false, handler);
    }

    @Override
    public void delete(String id, Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> future = future(handler);
        JsonObject result = newResult(validator.validateID(id));

        if (!failed(result)) {

            final JsonObject entity = collectionMap.remove(id);

            // Check if entity exists in the map.
            if (entity != null) {
                // Delete entity file.
                fs.delete(getPath(id), dh -> {

                    if (dh.failed()) {
                        // Add the entity back to the map.
                        collectionMap.put(id, entity);

                        String message = "Failed to delete entity: " + entity;
                        addError(result, message);
                        logger.error(message, dh.cause());

                    } else {
                        putEntity(result, entity);
                    }

                    future.complete(result);
                });

            } else {
                future.complete(addError(result,
                        "Failed to delete entity. Entity not found: " + id));
            }

        } else {
            future.complete(result);
        }
    }

    @Override
    public void find(QuerySpec querySpec, Handler<AsyncResult<JsonObject>> handler) {
        // Dump the map.
        Future<JsonObject> future = future(handler);
        JsonObject result = ResultUtils.newResult();

        List<JsonObject> items = new ArrayList<>();
        collectionMap.keySet().forEach(k -> items.add(collectionMap.get(k)));

        try {
            ResultUtils.addEntities(result, QueryEngine.run(querySpec, items));

        } catch (Exception ex) {
            String message = "Unable to find entities in: " + getCollectionName();
            logger.error(message, ex);
            ResultUtils.addError(result, message);
        }

        future.complete(result);
    }

    String getID(JsonObject entity) {
        return entity.getString(ID);
    }

    String getPath(JsonObject entity) {
        return String.format(ENTITY_PATH, collectionPath, entity.getString(ID));
    }

    private String getPath(String id) {
        return String.format(ENTITY_PATH, collectionPath, id);
    }

    private String setCollectionPath(JsonObject config) {
        String systemFolderPath = Objects.requireNonNull(config.getString(SYSTEM_FOLDER_PATH),
                "System folder path must be configured.");

        return FileUtils.slashify(systemFolderPath) + DB_PATH + getCollectionName() + SLASH;
    }

    private void save(JsonObject entity, boolean isAdd, Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> future = future(handler);
        JsonObject result = validateEntity(entity);

        if (failed(result)) {
            future.complete(result);

        } else {

            if (isAdd && get(getID(entity)) != null) {
                addError(result, "Entity should have a unique ID.");

            } else if (!isAdd && get(getID(entity)) == null) {
                addError(result, "Entity not found.");
            }

            if (failed(result)) {
                future.complete();

            } else {
                save(entity, isAdd, result, future);
            }
        }
    }

    void save(JsonObject entity, boolean isAdd, JsonObject result, Future<JsonObject> future) {
        saveToFile(entity, result, future);
    }

    void saveToFile(JsonObject entity, JsonObject result, Future<JsonObject> future) {
        fs.writeFile(getPath(entity), Buffer.buffer(entity.encode()), wh -> {

            if (wh.succeeded()) {
                // Save to map.
                collectionMap.put(getID(entity), entity);
                future.complete(putEntity(result, entity));

            } else {
                String message = "Failed to save entity: " + entity;
                future.complete(addError(result, message));
                logger.error(message, wh.cause());
            }
        });
    }

    private void error(Future<JsonObject> future, AsyncResult asyncResult) {
        String message = "Unable to initialize collection: " + getCollectionName();
        future.fail(message);
        logger.error(message, asyncResult.cause());
    }
}
