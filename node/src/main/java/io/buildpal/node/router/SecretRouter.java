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

import io.buildpal.core.domain.Secret;
import io.buildpal.db.file.SecretManager;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;

import java.util.List;

public class SecretRouter extends CrudRouter<Secret> {
    private static final Logger logger = LoggerFactory.getLogger(SecretRouter.class);

    public SecretRouter(Vertx vertx, JWTAuth jwtAuth, List<String> authorities,
                        SecretManager secretManager) {

        super(vertx, logger, jwtAuth, authorities, secretManager, Secret::new);
    }

}
