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

package io.buildpal.auth;

import io.buildpal.auth.util.AuthConfigUtils;
import io.buildpal.auth.vault.VaultService;
import io.buildpal.core.domain.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Arrays;

import static io.buildpal.core.config.Constants.DATA;
import static io.buildpal.core.util.VertxUtils.future;

public class VaultAuthenticator implements Authenticator {
    private static final Logger logger = LoggerFactory.getLogger(VaultAuthenticator.class);

    private final VaultService vaultService;

    public VaultAuthenticator(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @Override
    public void authenticate(User user, Handler<AsyncResult<User>> handler) {
        Future<User> authFuture = future(handler);

        // Get the hash and verify.
        vaultService.retrieveHash(user.getUserName(), rh -> {

            if (rh.succeeded()) {

                authFuture.complete(verifyHash(user, rh.result()));

            } else {
                logger.error("Unable to authenticate user.", rh.cause());
                authFuture.complete(null);
            }
        });
    }

    private User verifyHash(User user, JsonObject hashedData) {
        try {
            byte[] actualHash = AuthConfigUtils.hash(user.getPassword().toCharArray(), user.getSalt());
            byte[] expectedHash = hashedData.getBinary(DATA);

            if (Arrays.equals(actualHash, expectedHash)) return user;

        } catch (Exception ex) {
            logger.error("Unable to authenticate user.", ex);
        }

        return null;
    }
}
