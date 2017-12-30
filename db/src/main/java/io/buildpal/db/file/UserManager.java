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

        User user = isAdd ? new User(entity) : new User(get(entity.getString(ID)));

        if (user.getType() == User.Type.LOCAL) {
            saveLocalUser(isAdd, entity, user, result, future);

        } else {
            saveUser(user, result, future);
        }
    }

    private void saveLocalUser(boolean isAdd, JsonObject entity, User user,
                               JsonObject result, Future<JsonObject> future) {
        if (isAdd) {
            user.setSalt(Utils.newID().getBytes());

        } else {
            // NOTE: Only pwd and salt can be updated for a user.
            user.setPassword(entity.getString(PASSWORD))
                    .setSalt(Utils.newID().getBytes());
        }

        vaultService.save(user.getUserName(), user.getPassword(), user.getSalt(), sh -> {
            user.clearPassword();

            if (sh.succeeded()) {
                saveToFile(user.json(), result, future);

            } else {
                String message = "Unable to save hash for user: " + user.getName();
                future.complete(addError(result, message));
                logger.error(message, sh.cause());
            }
        });
    }

    private void saveUser(User user, JsonObject result, Future<JsonObject> future) {
        saveToFile(user.json(), result, future);
    }
}
