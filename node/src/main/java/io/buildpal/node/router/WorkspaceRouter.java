/*
 * Copyright 2018 Buildpal
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

import io.buildpal.core.config.Constants;
import io.buildpal.core.util.ResultUtils;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;

import static io.buildpal.core.config.Constants.DELETE_WORKSPACE_ADDRESS;
import static io.buildpal.core.domain.Entity.ID;

public class WorkspaceRouter extends BaseRouter {
    private static final Logger logger = LoggerFactory.getLogger(WorkspaceRouter.class);

    public WorkspaceRouter(Vertx vertx, JWTAuth jwtAuth) {
        super(vertx, logger, jwtAuth);

        if (jwtAuth != null) {
            secureBaseRoute(null);

        } else {
            logger.warn("Router is not backed by an auth handler: " + getBasePath());
        }

        configureDeleteRoute();
    }

    @Override
    protected String getBasePath() {
        return "/workspaces/user";
    }

    private void configureDeleteRoute() {
        String path = getBasePath() + ID_PATH;

        router.route(HttpMethod.DELETE, path).handler(routingContext -> {
            String userID = routingContext.request().getParam(ID_PARAM);

            // Users can delete workspaces that they own.
            if (userID.equals(routingContext.user().principal().getString(Constants.SUBJECT))) {
                JsonObject entity = new JsonObject().put(ID, userID);

                vertx.eventBus().publish(DELETE_WORKSPACE_ADDRESS, entity);

                write202Response(routingContext, ResultUtils.prepareResult(entity));

            } else {
                routingContext.fail(401);
            }
        });
    }
}
