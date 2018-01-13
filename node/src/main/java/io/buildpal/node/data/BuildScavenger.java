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

package io.buildpal.node.data;

import io.buildpal.node.router.BuildRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static io.buildpal.core.config.Constants.ITEMS;
import static io.buildpal.core.config.Constants.NODE;
import static io.buildpal.core.util.ResultUtils.failed;

public class BuildScavenger extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(BuildScavenger.class);

    private static final String QUERY = "(utcEndDate pr and utcEndDate lt i\"%s\") and " +
            "(status eq \"DONE\" or status eq \"FAILED\" or status eq \"CANCELED\")";

    private static final String INFO = "%d builds were deemed old enough and they were requested to be purged";

    private int daysOldToDelete;

    @Override
    public void start(Future<Void> startFuture) {

        try {
            JsonObject scavengerConfig = config().getJsonObject(NODE).getJsonObject("buildScavenger");

            daysOldToDelete = scavengerConfig.getInteger("daysOldToDelete");

            long scavengerInterval = scavengerConfig.getLong("interval");

            vertx.setPeriodic(scavengerInterval, ph -> requestOldBuilds());

            vertx.eventBus().localConsumer(BuildRouter.FIND_REPLY_ADDRESS, foundOldBuildsHandler());

            startFuture.complete();

        } catch (Exception ex) {
            startFuture.fail(ex);
        }
    }

    private void requestOldBuilds() {
        Instant olderThan = Instant.now(Clock.systemUTC())
                .minus(daysOldToDelete, ChronoUnit.DAYS);

        JsonObject request = new JsonObject()
                .put("q", String.format(QUERY, olderThan.toString()));

        vertx.eventBus().send(BuildRouter.FIND_ADDRESS, request);
    }

    private Handler<Message<JsonObject>> foundOldBuildsHandler() {
        return mh -> {
            if (failed(mh.body())) {
                logger.error("Unable to retrieve old builds. Error: " + mh.body());

            } else {
                JsonArray builds = mh.body().getJsonArray(ITEMS);

                if (builds.isEmpty()) return;

                for (int b=0; b<builds.size(); b++) {
                    vertx.eventBus().send(BuildRouter.DELETE_ADDRESS, builds.getJsonObject(b));
                }

                logger.info(String.format(INFO, builds.size()));
            }
        };
    }
}

