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

package io.buildpal.node;

import io.buildpal.auth.util.AuthConfigUtils;
import io.buildpal.auth.vault.JCEVaultVerticle;
import io.buildpal.core.domain.Project;
import io.buildpal.core.domain.Repository;
import io.buildpal.core.domain.User;
import io.buildpal.core.util.Utils;
import io.buildpal.core.util.VertxUtils;
import io.buildpal.db.DbManager;
import io.buildpal.db.DbManagers;
import io.buildpal.node.auth.LoginAuthHandler;
import io.buildpal.node.auth.LogoutAuthHandler;
import io.buildpal.node.data.BuildScavenger;
import io.buildpal.node.engine.Engine;
import io.buildpal.node.router.BaseRouter;
import io.buildpal.node.router.BuildRouter;
import io.buildpal.node.router.CrudRouter;
import io.buildpal.node.router.PipelineRouter;
import io.buildpal.node.router.SecretRouter;
import io.buildpal.node.router.StaticRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static io.buildpal.core.config.Constants.ADMIN;
import static io.buildpal.core.config.Constants.EMPTY_JSON;
import static io.buildpal.core.config.Constants.HTTP_PORT;
import static io.buildpal.core.config.Constants.NODE;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.node.router.CrudRouter.API_PATH;

public class Node extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(Node.class);

    private static final String WILDCARD = "*";
    private static final String CORS_ENABLED = "corsEnabled";
    private static final String CORS_ORIGIN_PATTERN = "corsOriginPattern";
    private static final String BODY_LIMIT = "bodyLimit";

    private Router mainRouter;
    private DbManagers dbManagers;

    @Override
    public void start(Future<Void> startFuture) {
        boolean isServer = isServer(config());

        if (isServer) {
            dbManagers = new DbManagers(vertx, config());

            Future<Void> dbFuture = Future.future();
            dbFuture
                    .compose(d -> deployVerticles())
                    .compose(s -> serve(startFuture), startFuture);

            configureDb(dbFuture);

        } else {
            deployVerticles()
                    .compose(d -> startFuture.complete(), startFuture);
        }
    }

    private Router getMainRouter() {
        return mainRouter;
    }

    private void serve(Future<Void> startFuture) {
        try {
            JWTAuthOptions jwtAuthOptions = new JWTAuthOptions(AuthConfigUtils.getAuthConfig(config()));
            JWTAuth jwtAuth = JWTAuth.create(vertx, jwtAuthOptions);

            JsonObject nodeConfig = config().getJsonObject(NODE);

            configureMainRouter(nodeConfig);
            configureCors(nodeConfig);

            // Order is important. Keep an eye on sub routes and main routes.
            configureLoginLogout(jwtAuth);
            configureApi(jwtAuth);
            configureStaticFiles();

            createAdminAndListen(startFuture);

        } catch (Exception ex) {
            logger.error("Unable to start buildpal server.", ex);
            startFuture.fail(ex);
        }
    }

    private void configureMainRouter(JsonObject serverConfig) {
        BodyHandler bodyHandler = BodyHandler.create()
                .setBodyLimit(serverConfig.getLong(BODY_LIMIT, -1L));

        mainRouter = Router.router(vertx);
        mainRouter.route().handler(bodyHandler);
    }

    private void configureCors(JsonObject serverConfig) {
        if (serverConfig.getBoolean(CORS_ENABLED, false)) {
            Set<HttpMethod> allowedMethods = new HashSet<>();
            allowedMethods.add(HttpMethod.GET);
            allowedMethods.add(HttpMethod.POST);
            allowedMethods.add(HttpMethod.PUT);
            allowedMethods.add(HttpMethod.DELETE);

            Set<String> allowedHeaders = new HashSet<>();
            allowedHeaders.add("Content-Type");
            allowedHeaders.add("Authorization");

            String corsOriginPattern = serverConfig.getString(CORS_ORIGIN_PATTERN, WILDCARD);

            if (StringUtils.isBlank(corsOriginPattern)) {
                corsOriginPattern = WILDCARD;
            }

            if (WILDCARD.equals(corsOriginPattern)) {
                logger.warn("CORS handler configured to allow requests from ANY origin.");
            }

            CorsHandler corsHandler = CorsHandler.create(corsOriginPattern)
                    .allowedHeaders(allowedHeaders)
                    .allowedMethods(allowedMethods)
                    .allowCredentials(!WILDCARD.equals(corsOriginPattern));

            getMainRouter().route().handler(corsHandler);
        }
    }

    private void configureDb(Future<Void> dbFuture) {
        List<DbManager> managers = dbManagers.getManagers();
        List<Boolean> inits = new ArrayList<>();

        final AtomicInteger counter = new AtomicInteger();
        final int total = managers.size();

        managers.forEach(manager -> manager.init(config(), ih -> {
            inits.add(ih.succeeded());

            if (counter.incrementAndGet() == total) {
                if (inits.stream().anyMatch(initialized -> !initialized)) {
                    dbFuture.fail("Unable to initialize one or more db managers.");

                } else {
                    dbFuture.complete();
                }
            }
        }));
    }

    private void configureLoginLogout(JWTAuth jwtAuth)  {
        getMainRouter()
                .route("/login")
                .handler(new LoginAuthHandler(vertx, config(), jwtAuth, dbManagers.getUserManager()));

        getMainRouter()
                .route("/logout")
                .handler(new LogoutAuthHandler());
    }

    @SuppressWarnings("unchecked")
    private void configureApi(JWTAuth jwtAuth) {

        mainRouter.mountSubRouter(API_PATH,
                new CrudRouter(vertx, jwtAuth, null, dbManagers.getProjectManager(), Project::new)
                        .getRouter());

        mainRouter.mountSubRouter(API_PATH,
                new SecretRouter(vertx, jwtAuth, null, dbManagers.getSecretManager())
                        .getRouter());

        mainRouter.mountSubRouter(API_PATH,
                new CrudRouter(vertx, jwtAuth, null, dbManagers.getRepositoryManager(), Repository::new)
                        .getRouter());

        mainRouter.mountSubRouter(API_PATH,
                new PipelineRouter(vertx, jwtAuth, null, dbManagers.getPipelineManager())
                        .getRouter());

        mainRouter.mountSubRouter(API_PATH,
                new BuildRouter(vertx, jwtAuth, null, dbManagers.getBuildManager())
                        .getRouter());
    }

    private void configureStaticFiles() {
        getMainRouter().mountSubRouter(BaseRouter.ROOT_PATH, new StaticRouter(vertx, config()).getRouter());
    }

    private void createAdminAndListen(Future<Void> startFuture) {

        // Ensure that the admin user is registered.
        JsonObject userJson = dbManagers.getUserManager().get(ADMIN);

        // If no admin user is found, create the admin user with a temp password.
        if (userJson == null) {
            String tmp = Utils.newID();

            User adminUser = User.newLocalUser(ADMIN, tmp);

            // admin ID is "admin"
            adminUser.setID(ADMIN);

            dbManagers.getUserManager().add(adminUser.json(), ah -> {
                if (failed(ah)) {
                    String message = "Unable to create and save admin. Result: " + ah.result();
                    logger.error(message, ah.result());

                    startFuture.fail(message);

                } else {
                    logger.info("Admin password: " + tmp);
                    listen(startFuture);
                }
            });

        } else {
            listen(startFuture);
        }
    }

    private void listen(Future<Void> startFuture) {
        HttpServer httpServer = vertx.createHttpServer()
                .requestHandler(getMainRouter()::accept);

        httpServer.listen(getNodeHttpPort(config()), lh -> {
            if (lh.succeeded()) {
                logger.info(String.format("Buildpal server running on port: %d", lh.result().actualPort()));

                startFuture.complete();

            } else {
                startFuture.fail(lh.cause());
            }
        });
    }

    private Future<Void> deployVerticles() {
        Future<Void> deployFuture = Future.future();
        List<Verticle> verticles;

        if (isServer(config())) {

            if (isEngine(config())) {
                verticles = List.of(new JCEVaultVerticle(), new BuildScavenger(), new Engine());

            } else {
                verticles = List.of(new JCEVaultVerticle(), new BuildScavenger());
            }

        } else {
            verticles = List.of(new Engine());
        }

        VertxUtils.deployVerticles(vertx, verticles, config(), dh -> {
            if (dh.succeeded()) {
                deployFuture.complete();

            } else {
                deployFuture.fail(dh.cause());
            }
        });

        return deployFuture;
    }

    private int getNodeHttpPort(JsonObject config) {
        return config.getJsonObject(NODE, EMPTY_JSON).getInteger(HTTP_PORT, 8080);
    }

    private boolean isServer(JsonObject config) {
        return config.getJsonObject(NODE, EMPTY_JSON).getBoolean("isServer", false);
    }

    private boolean isEngine(JsonObject config) {
        return config.getJsonObject(NODE, EMPTY_JSON).getBoolean("isEngine", true);
    }
}
