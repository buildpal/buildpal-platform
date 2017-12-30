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

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.handler.StaticHandler;

public class StaticRouter extends BaseRouter {
    private static final Logger logger = LoggerFactory.getLogger(StaticRouter.class);

    private static final String WEB_ROOT = "webroot";

    public StaticRouter(Vertx vertx, JsonObject config) {
        super(vertx, logger);

        StaticHandler handler = StaticHandler.create();
        setWebRoot(config.getString(WEB_ROOT), handler);
        router.route().handler(handler);
    }

    @Override
    protected String getBasePath() {
        return ROOT_PATH;
    }

    private void setWebRoot(String path, StaticHandler staticHandler) {
        if (path == null) return;

        staticHandler.setWebRoot(path).setFilesReadOnly(true);
        logger.info("Files will be served from: " + path);
    }
}
