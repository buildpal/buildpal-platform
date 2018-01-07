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

package io.buildpal.core.pipeline;

import io.buildpal.core.pipeline.event.CommandKey;
import io.buildpal.core.pipeline.event.EventKey;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;

import java.util.Set;

public abstract class Plugin extends AbstractVerticle {

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        start();

        EventBus eb = getVertx().eventBus();

        for (CommandKey key : commandKeysToRegister()) {
            switch (key) {
                case SETUP:
                    eb.localConsumer(key.getAddress(order()), setupHandler(eb));
                    break;

                case RUN_PHASE:
                    eb.localConsumer(key.getAddress(order()), phaseHandler(eb));
                    break;

                case TEAR_DOWN:
                    eb.localConsumer(key.getAddress(order()), tearDownHandler(eb));
                    break;
            }
        }

        starting(startFuture);
    }

    public abstract Set<CommandKey> commandKeysToRegister();

    public abstract Logger getLogger();

    public int order() {
        return 100;
    }

    protected void starting(Future<Void> startFuture) throws Exception {
        startFuture.complete();
    }

    protected Handler<Message<JsonObject>> setupHandler(EventBus eb) {
        return mh -> {
            getLogger().info(mh.body());
            eb.send(EventKey.SETUP_END.getAddress(), mh.body());
        };
    }

    protected Handler<Message<JsonObject>> phaseHandler(EventBus eb) {
        return mh -> {
            getLogger().info(mh.body());
            eb.send(EventKey.PHASE_END.getAddress(), mh.body());
        };
    }

    protected Handler<Message<JsonObject>> tearDownHandler(EventBus eb) {
        return mh -> {
            getLogger().info(mh.body());
            eb.send(EventKey.TEAR_DOWN_END.getAddress(), mh.body());
        };
    }
}
