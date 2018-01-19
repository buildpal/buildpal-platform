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
import io.buildpal.core.domain.User;
import io.buildpal.core.domain.validation.Validator;
import io.buildpal.core.util.Utils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static io.buildpal.core.domain.Entity.ID;
import static io.buildpal.core.domain.User.PASSWORD;
import static io.buildpal.core.util.ResultUtils.addError;

public class UserManager extends FileDbManager {
    private static final Logger logger = LoggerFactory.getLogger(UserManager.class);

    private final VaultService vaultService;

    public UserManager(Vertx vertx, JsonObject config, Validator validator) {
        super(vertx, logger, config, validator);

        vaultService = new VaultService(vertx);
    }

    @Override
    public String getCollectionName() {
        return "users";
    }

    @Override
    void save(JsonObject entity, boolean isAdd, JsonObject result, Future<JsonObject> future) {

        User user = new User(entity);

        if (user.getType() == User.Type.LOCAL) {
            saveUserHash(isAdd, user, result, future);

        } else {
            saveToFile(user.json(), result, future);
        }
    }

    private void saveUserHash(boolean isAdd, User user, JsonObject result, Future<JsonObject> future) {

        boolean saveToVault = false;

        if (isAdd || user.json().containsKey(PASSWORD)) {
            user.setSalt(Utils.newID().getBytes());
            saveToVault = true;
        }

        if (saveToVault) {
            // Add or update the hash in the vault.
            vaultService.save(user.getUserName(), user.getPassword(), user.getSalt(), sh -> {
                // No need to store the pass in the clear.
                user.clearPassword();

                if (sh.succeeded()) {
                    saveToFile(user.json(), result, future);

                } else {
                    String message = "Unable to save hash for user: " + user.getName();
                    future.complete(addError(result, message));
                    logger.error(message, sh.cause());
                }
            });

        } else {
            saveToFile(user.json(), result, future);
        }
    }
}
