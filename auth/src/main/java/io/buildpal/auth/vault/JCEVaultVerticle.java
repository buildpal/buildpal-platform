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

import io.buildpal.auth.util.AuthConfigUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;
import java.util.Objects;

import static io.buildpal.auth.util.AuthConfigUtils.getVaultKey;
import static io.buildpal.auth.vault.VaultService.RETRIEVE_DATA_ADDRESS;
import static io.buildpal.auth.vault.VaultService.RETRIEVE_HASH_ADDRESS;
import static io.buildpal.auth.vault.VaultService.SAVE_DATA_ADDRESS;
import static io.buildpal.auth.vault.VaultService.SAVE_HASH_ADDRESS;
import static io.buildpal.core.config.Constants.DATA;
import static io.buildpal.core.config.Constants.SALT;
import static io.buildpal.core.config.Constants.SUCCESS;
import static io.buildpal.core.config.Constants.SYSTEM_FOLDER_PATH;
import static io.buildpal.core.domain.Entity.NAME;
import static io.buildpal.core.util.FileUtils.slashify;

public class JCEVaultVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(JCEVaultVerticle.class);

    private static final String SECRETS_PATH = "./secrets/";
    private static final String VAULT_DATA_FILE = "vault_data.jceks";
    private static final String VAULT_HASH_FILE = "vault_hash.jceks";

    private Vault dataVault;
    private Vault hashVault;

    @Override
    public void start(Future<Void> startFuture) {
        try {

            String systemFolderPath = Objects.requireNonNull(config().getString(SYSTEM_FOLDER_PATH),
                    "System folder path must be configured.");

            String path = slashify(systemFolderPath) + SECRETS_PATH;

            if (!new File(path).exists()) {
                vertx.fileSystem().mkdirsBlocking(path);
            }

            String key = Objects.requireNonNull(getVaultKey(config()), "Vault key must be configured");

            // Can throw a null-pointer exception if the key isn't configured.
            this.dataVault = new Vault(key.toCharArray(), path + VAULT_DATA_FILE);
            this.hashVault = new Vault(key.toCharArray(), path + VAULT_HASH_FILE);

            vertx.eventBus().consumer(SAVE_DATA_ADDRESS, saveHandler());
            vertx.eventBus().consumer(SAVE_HASH_ADDRESS, saveHashHandler());
            vertx.eventBus().consumer(RETRIEVE_DATA_ADDRESS, retrieveHandler());
            vertx.eventBus().consumer(RETRIEVE_HASH_ADDRESS, retrieveHashHandler());

            startFuture.complete();

        } catch (Exception ex) {
            logger.error("Unable to initialize basic vault service provider.", ex);
            startFuture.fail(ex);
        }
    }

    private Handler<Message<JsonObject>> saveHandler() {
        return smh -> {
            try {
                JsonObject data = smh.body();

                dataVault.save(data.getString(NAME), data);

                smh.reply(new JsonObject().put(SUCCESS, true));

            } catch (Exception ex) {
                logger.error("Unable to save data in vault.", ex);
                smh.reply(new JsonObject().put(SUCCESS, false));
            }
        };
    }

    private Handler<Message<JsonObject>> saveHashHandler() {
        return smh -> {
            try {
                JsonObject message = smh.body();

                String data = message.getString(DATA);

                byte[] salt = message.getBinary(SALT);
                byte[] hash = AuthConfigUtils.hash(data.toCharArray(), salt);

                hashVault.save(message.getString(NAME), new Vault.Data(hash));

                smh.reply(new JsonObject().put(SUCCESS, true));

            } catch (Exception ex) {
                logger.error("Unable to save hash in vault.", ex);
                smh.reply(new JsonObject().put(SUCCESS, false));
            }
        };
    }

    private Handler<Message<JsonObject>> retrieveHandler() {
        return rmh -> {
            try {
                rmh.reply(dataVault.retrieveJson(rmh.body().getString(NAME)));

            } catch (Exception ex) {
                logger.error("Unable to retrieve data from vault.", ex);
                rmh.reply(null);
            }
        };
    }

    private Handler<Message<JsonObject>> retrieveHashHandler() {
        return rmh -> {
            try {
                String name = rmh.body().getString(NAME);
                Vault.Data data = hashVault.retrieve(name);

                rmh.reply(new JsonObject()
                        .put(NAME, name)
                        .put(DATA, data.getHashBytes()));

            } catch (Exception ex) {
                logger.error("Unable to retrieve hash from vault.", ex);
                rmh.reply(null);
            }
        };
    }
}
