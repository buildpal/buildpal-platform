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

package io.buildpal.auth.vault;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;

import static io.buildpal.core.config.Constants.DATA;
import static io.buildpal.core.config.Constants.SALT;
import static io.buildpal.core.config.Constants.SUCCESS;
import static io.buildpal.core.domain.Entity.NAME;
import static io.buildpal.core.util.VertxUtils.future;

public class VaultService {

    static final String SAVE_DATA_ADDRESS = "vault:saveData";
    static final String SAVE_HASH_ADDRESS = "vault:saveHash";
    static final String RETRIEVE_DATA_ADDRESS = "vault:retrieveData";
    static final String RETRIEVE_HASH_ADDRESS = "vault:retrieveHash";

    private final EventBus eb;

    public VaultService(Vertx vertx) {
        this.eb = vertx.eventBus();
    }


    public void save(JsonObject data, Handler<AsyncResult<Void>> saveHandler) {

        Future<Void> saveFuture = future(saveHandler);

        eb.<JsonObject>send(SAVE_DATA_ADDRESS, data, smh -> {
            if (smh.succeeded()) {
                boolean saved = smh.result().body().getBoolean(SUCCESS, false);

                if (saved) {
                    saveFuture.complete();

                } else {
                    saveFuture.fail("Unable to save data in vault.");
                }

            } else {
                saveFuture.fail(smh.cause());
            }
        });
    }

    public void save(String name, String data, byte[] salt, Handler<AsyncResult<Void>> saveHandler) {

        Future<Void> saveFuture = future(saveHandler);

        JsonObject message = new JsonObject().put(NAME, name).put(DATA, data).put(SALT, salt);

        eb.<JsonObject>send(SAVE_HASH_ADDRESS, message, smh -> {
            if (smh.succeeded()) {
                boolean saved = smh.result().body().getBoolean(SUCCESS, false);

                if (saved) {
                    saveFuture.complete();

                } else {
                    saveFuture.fail("Unable to save hash in vault.");
                }

            } else {
                saveFuture.fail(smh.cause());
            }
        });
    }

    public void retrieve(String name, Handler<AsyncResult<JsonObject>> retrieveHandler) {
        Future<JsonObject> retrieveFuture = future(retrieveHandler);
        JsonObject data = new JsonObject().put(NAME, name);

        eb.<JsonObject>send(RETRIEVE_DATA_ADDRESS, data, rh -> {
            if (rh.succeeded()) {
                JsonObject json = rh.result().body();

                if (json != null) {
                    retrieveFuture.complete(json);

                } else {
                    retrieveFuture.fail("Unable to retrieve data from vault.");
                }

            } else {
                retrieveFuture.fail(rh.cause());
            }
        });
    }

    public void retrieveHash(String name, Handler<AsyncResult<JsonObject>> retrieveHandler) {
        Future<JsonObject> retrieveFuture = future(retrieveHandler);
        JsonObject data = new JsonObject().put(NAME, name);

        eb.<JsonObject>send(RETRIEVE_HASH_ADDRESS, data, rh -> {
            if (rh.succeeded()) {
                JsonObject json = rh.result().body();

                if (json != null) {
                    retrieveFuture.complete(json);

                } else {
                    retrieveFuture.fail("Unable to retrieve hash from vault.");
                }

            } else {
                retrieveFuture.fail(rh.cause());
            }
        });
    }
}