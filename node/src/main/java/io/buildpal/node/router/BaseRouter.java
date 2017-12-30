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

import io.buildpal.core.query.QuerySpec;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;

import java.util.List;
import java.util.Set;

/**
 * Router with basic features shared across all routes.
 */
public abstract class BaseRouter {

    public static final String ROOT_PATH = "/";

    static final String ID_PARAM = "id";
    static final String ID_PATH = "/:" + ID_PARAM;

    private static final String ERRORS = "errors";
    private static final String CONTENT_LENGTH = "Content-Length";

    protected final Vertx vertx;
    protected final Router router;
    protected final Logger logger;
    private final JWTAuth jwtAuth;

    BaseRouter(Vertx vertx, Logger logger) {
        this(vertx, logger, null);
    }

    BaseRouter(Vertx vertx, Logger logger, JWTAuth jwtAuth) {
        this.vertx = vertx;
        this.router = Router.router(vertx);
        this.logger = logger;
        this.jwtAuth = jwtAuth;
    }

    public Router getRouter() {
        return router;
    }

    protected abstract String getBasePath();

    void secureBaseRoute(Set<String> authorities) {
        AuthHandler jwtAuthHandler = JWTAuthHandler.create(jwtAuth);

        if (authorities != null) {
            jwtAuthHandler.addAuthorities(authorities);
        }

        router.route(getBasePath() + "/*").handler(jwtAuthHandler);
    }

    public static void writeResponse(RoutingContext routingContext, JsonObject result) {
        writeResponse(routingContext, result, 200);
    }

    public static void write201Response(RoutingContext routingContext, JsonObject result) {
        writeResponse(routingContext, result, 201);
    }

    public static void write202Response(RoutingContext routingContext, JsonObject result) {
        writeResponse(routingContext, result, 202);
    }

    public static void writeResponse(RoutingContext routingContext, JsonObject result, int statusCode) {
        if (result.containsKey(ERRORS) && result.getJsonArray(ERRORS).size() > 0) {
            writeErrorResponse(routingContext, result.toString());

        } else {
            routingContext.response().setStatusCode(statusCode);
            writeSuccessResponse(routingContext, result.toString(), false);
        }
    }

    static void writeSuccessResponse(RoutingContext routingContext, String body, boolean chunked) {
        HttpServerResponse response = routingContext.response();

        if (chunked) {
            response.setChunked(true);

        } else {
            setContentLength(response, body);
        }

        response.write(body).end();
    }

    static void writeErrorResponse(RoutingContext routingContext, List<String> errors) {
        JsonObject result = new JsonObject().put(ERRORS, errors);
        writeErrorResponse(routingContext, result.encode());
    }

    static void writeErrorResponse(RoutingContext routingContext, String body) {
        HttpServerResponse response = routingContext.response();
        setContentLength(response, body);

        response.setStatusCode(500).write(body).end();
    }

    static void setContentLength(HttpServerResponse response, String body) {
        response.putHeader(CONTENT_LENGTH, String.valueOf(body.length()));
    }

    static QuerySpec buildQuerySpec(HttpServerRequest request) {
        return new QuerySpec(request.params());
    }
}
