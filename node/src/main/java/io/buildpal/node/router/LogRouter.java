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
import io.buildpal.db.DbManager;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class LogRouter extends CrudRouter<Build> {
    private static final Logger logger = LoggerFactory.getLogger(LogRouter.class);

    // TODO: Secure log route.
    public LogRouter(Vertx vertx, DbManager dbManager) {
        super(vertx, logger, null, null, dbManager, Build::new);
    }

    @Override
    protected void configureRoutes(String collectionPath, Vertx vertx) {

    }

    @Override
    protected String getBasePath() {
        return "/logs";
    }


}
