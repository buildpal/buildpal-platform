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

import io.buildpal.auth.vault.VaultService;
import io.buildpal.core.domain.validation.Validator;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static io.buildpal.core.domain.Entity.CREATED_BY;
import static io.buildpal.core.domain.Entity.DESC;
import static io.buildpal.core.domain.Entity.ID;
import static io.buildpal.core.domain.Entity.LAST_MODIFIED_BY;
import static io.buildpal.core.domain.Entity.NAME;
import static io.buildpal.core.domain.Entity.UTC_CREATED_DATE;
import static io.buildpal.core.domain.Entity.UTC_LAST_MODIFIED_DATE;
import static io.buildpal.core.util.ResultUtils.addError;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.core.util.ResultUtils.newResult;
import static io.buildpal.core.util.ResultUtils.putEntity;
import static io.buildpal.core.util.VertxUtils.future;

public class SecretManager extends FileDbManager {
    private static final Logger logger = LoggerFactory.getLogger(SecretManager.class);

    private final VaultService vaultService;

    public SecretManager(Vertx vertx, JsonObject config, Validator validator) {
        super(vertx, logger, config, validator);

        vaultService = new VaultService(vertx);
    }

    @Override
    public String getCollectionName() {
        return "secrets";
    }

    @Override
    public void getGraph(String id, Handler<AsyncResult<JsonObject>> handler) {
        Future<JsonObject> future = future(handler);
        JsonObject result = newResult(validator.validateID(id));

        if (!failed(result)) {

            JsonObject secret = collectionMap.get(id);

            if (secret != null) {
                vaultService.retrieve(secret.getString(NAME), rh -> {
                    if (rh.succeeded()) {
                        putEntity(result, rh.result());

                    } else {
                        String message = "Failed to retrieve secret: " + secret;
                        addError(result, message);
                        logger.error(message, rh.cause());
                    }

                    future.complete(result);
                });

            } else {
                putEntity(result, secret);
                future.complete(result);
            }

        } else {
            future.complete(result);
        }
    }

    @Override
    void save(JsonObject entity, boolean isAdd, JsonObject result, Future<JsonObject> future) {

        JsonObject secret = new JsonObject()
                .put(ID, entity.getString(ID))
                .put(NAME, entity.getString(NAME))
                .put(CREATED_BY, entity.getString(CREATED_BY))
                .put(UTC_CREATED_DATE, entity.getString(UTC_CREATED_DATE))
                .put(UTC_LAST_MODIFIED_DATE, entity.getString(UTC_LAST_MODIFIED_DATE))
                .put(LAST_MODIFIED_BY, entity.getString(LAST_MODIFIED_BY));

        if (entity.containsKey(DESC)) {
            secret.put(DESC, entity.getString(DESC));
        }

        // Save to vault and then save the public properties to file.
        vaultService.save(entity, sh -> {
            if (sh.succeeded()) {
                saveToFile(secret, result, future);

            } else {
                String message = "Failed to save secret: " + secret;
                future.complete(addError(result, message));
                logger.error(message, sh.cause());
            }
        });
    }
}
