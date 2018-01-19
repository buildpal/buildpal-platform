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

import io.buildpal.core.domain.User;
import io.buildpal.db.file.UserManager;
import io.buildpal.node.data.NodeTracker;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static io.buildpal.core.config.Constants.ITEM;
import static io.buildpal.core.config.Constants.NODE;
import static io.buildpal.core.config.Constants.SAVE_USER_AFFINITY_ADDRESS;
import static io.buildpal.core.domain.Entity.ID;
import static io.buildpal.core.util.ResultUtils.addEntities;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.core.util.ResultUtils.getEntity;
import static io.buildpal.core.util.ResultUtils.newResult;

public class UserRouter extends CrudRouter<User> {
    private static final Logger logger = LoggerFactory.getLogger(UserRouter.class);

    public UserRouter(Vertx vertx, JWTAuth jwtAuth, List<String> authorities,
                        UserManager userManager) {

        super(vertx, logger, jwtAuth, authorities, userManager, User::new);
    }

    @Override
    protected void configureRoutes(String collectionPath, Vertx vertx) {
        NodeTracker.ME.load(vertx, dbManager.list());

        configureAffinitiesRoute(collectionPath);
        configureDeleteAffinityRoute(collectionPath);

        vertx.eventBus().consumer(SAVE_USER_AFFINITY_ADDRESS, saveNodeAffinityHandler());
    }

    private void configureAffinitiesRoute(String collectionPath) {
        String path = collectionPath + "/affinity";

        router.route(HttpMethod.GET, path).handler(routingContext -> {

            JsonObject result = newResult();
            addEntities(result, NodeTracker.ME.getAffinities());

            writeResponse(routingContext, result);
        });
    }

    private void configureDeleteAffinityRoute(String collectionPath) {
        String path = collectionPath + ID_PATH + "/affinity";

        router.route(HttpMethod.DELETE, path).handler(routingContext -> {
            String userID = routingContext.request().getParam(ID_PARAM);

            dbManager.get(userID, gh -> {
                JsonObject item = getEntity(gh.result());

                if (!failed(gh) && item != null) {
                    User user = new User(item)
                            .clearNodeAffinity()
                            .setUtcLastModifiedDate(Instant.now(Clock.systemUTC()))
                            .setLastModifiedBy(userID);

                    dbManager.replace(user.json(), sh -> {
                        if (!failed(sh)) {
                            NodeTracker.ME.removeNode(user);
                        }

                        sh.result().remove(ITEM);
                        writeResponse(routingContext, sh.result());
                    });

                } else {
                    writeResponse(routingContext, gh.result());
                }
            });
        });
    }

    private Handler<Message<JsonObject>> saveNodeAffinityHandler() {
        return mh -> {
            String userID = mh.body().getString(ID);
            String node = mh.body().getString(NODE);

            dbManager.get(userID, gh -> {
                JsonObject item = getEntity(gh.result());

               if (failed(gh) || item == null) {
                   logger.error("Unable to save node affinity for user: " + userID + ". Error: " + gh.result());

               } else {
                   User user = new User(item)
                           .addNodeAffinity(node)
                           .setUtcLastModifiedDate(Instant.now(Clock.systemUTC()))
                           .setLastModifiedBy(userID);

                   saveNodeAffinity(user);
               }
            });
        };
    }

    private void saveNodeAffinity(User user) {
        dbManager.replace(user.json(), sh -> {
            if (failed(sh)) {
                logger.error("Unable to save user: " + user.getID() + ". Error: " + sh.result());

            } else {
                NodeTracker.ME.saveNode(user);
            }
        });
    }
}
