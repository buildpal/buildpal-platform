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

package io.buildpal.core.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for common vert.x operations.
 */
public class VertxUtils {
    public static <T> Future<T> future(Handler<AsyncResult<T>> handler) {
        Future<T> future = Future.future();
        future.setHandler(handler);

        return future;
    }

    public static void deployVerticles(Vertx vertx,
                                List<Verticle> verticles,
                                JsonObject config,
                                Handler<AsyncResult<CompositeFuture>> handler) {

        List<Future> futures = new ArrayList<>();

        for (Verticle verticle : verticles) {
            futures.add(deploy(vertx, verticle.getClass().getName(), new DeploymentOptions().setConfig(config)));
        }

        CompositeFuture.all(futures).setHandler(handler);
    }

    private static Future<String> deploy(Vertx vertx, String name, DeploymentOptions options) {
        Future<String> deployFuture = Future.future();

        vertx.deployVerticle(name, options, dh -> {
            if (dh.succeeded()) {
                deployFuture.complete(dh.result());

            } else {
                deployFuture.fail(dh.cause());
            }
        });

        return deployFuture;
    }
}
