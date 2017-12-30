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

import io.buildpal.core.config.Constants;
import io.buildpal.core.domain.Entity;
import io.buildpal.core.domain.builder.Builder;
import io.buildpal.core.query.QuerySpec;
import io.buildpal.db.DbManager;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.jwt.JWTAuth;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import static io.buildpal.core.domain.Entity.populate;

public class CrudRouter<E extends Entity<E>> extends BaseRouter {
    public static final String API_PATH = "/api/v1";

    private static final Logger logger = LoggerFactory.getLogger(CrudRouter.class);

    final DbManager dbManager;
    final Builder<E> builder;

    public CrudRouter(Vertx vertx, JWTAuth jwtAuth, List<String> authorities,
                      DbManager dbManager, Builder<E> builder) {
        this(vertx, logger, jwtAuth, authorities, dbManager, builder);
    }

    public CrudRouter(Vertx vertx, Logger logger, JWTAuth jwtAuth, List<String> authorities,
                      DbManager dbManager, Builder<E> builder) {

        super(vertx, logger, jwtAuth);

        this.dbManager = dbManager;
        this.builder = builder;

        if (jwtAuth != null) {
            secureBaseRoute(authorities != null ? new HashSet<>(authorities) : null);

        } else {
            logger.warn("Router is not backed by an auth handler: " + getBasePath());
        }

        configureRoutes(getBasePath(), vertx);
    }

    @Override
    protected String getBasePath() {
        return "/" + dbManager.getCollectionName();
    }

    protected void configureRoutes(String collectionPath, Vertx vertx) {
        configureGetCollectionRoute(collectionPath);

        configureGetRoute(collectionPath);
        configurePostRoute(collectionPath);
        configurePutRoute(collectionPath);
        configureDeleteRoute(collectionPath);
    }

    void configureGetCollectionRoute(String collectionPath) {
        router.route(HttpMethod.GET, collectionPath).handler(routingContext -> {
            QuerySpec querySpec = buildQuerySpec(routingContext.request());
            dbManager.find(querySpec, r -> writeResponse(routingContext, r.result()));
        });
    }

    void configureGetRoute(String collectionPath) {
        router.route(HttpMethod.GET, collectionPath + ID_PATH).handler(routingContext -> {

            dbManager.get(routingContext.request().getParam(ID_PARAM),
                    r -> writeResponse(routingContext, r.result()));
        });
    }

    void configurePostRoute(String collectionPath) {
        router.route(HttpMethod.POST, collectionPath).handler(routingContext -> {

            E entity = builder.build(routingContext.getBodyAsJson());
            preparePost(entity, routingContext.user());

            dbManager.add(entity.json(), r -> write201Response(routingContext, r.result()));
        });
    }

    void configurePutRoute(String collectionPath) {
        router.route(HttpMethod.PUT, collectionPath + ID_PATH).handler(routingContext -> {

            E entity = builder.build(routingContext.getBodyAsJson());
            preparePut(entity, routingContext.user());

            dbManager.replace(entity.json(), r -> write202Response(routingContext, r.result()));
        });
    }

    void configureDeleteRoute(String collectionPath) {
        router.route(HttpMethod.DELETE, collectionPath + ID_PATH).handler(routingContext -> {

            dbManager.delete(routingContext.request().getParam(ID_PARAM),
                    r -> write202Response(routingContext, r.result()));
        });
    }

    void preparePost(E entity, User user) {
        populate(entity, user.principal().getString(Constants.SUBJECT));
    }

    void preparePut(E entity, User user) {
        entity.setUtcLastModifiedDate(Instant.now(Clock.systemUTC()))
                .setLastModifiedBy(user.principal().getString(Constants.SUBJECT));
    }
}
