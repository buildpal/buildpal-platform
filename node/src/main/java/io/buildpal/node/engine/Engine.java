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

package io.buildpal.node.engine;

import io.buildpal.core.domain.Build;
import io.buildpal.core.pipeline.Plugin;
import io.buildpal.core.pipeline.event.CommandKey;
import io.buildpal.core.pipeline.event.Event;
import io.buildpal.core.pipeline.event.EventKey;
import io.buildpal.core.util.VertxUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

import static io.buildpal.core.config.Constants.DELETE_CONTAINERS_ADDRESS;
import static io.buildpal.core.config.Constants.KILL_CONTAINERS_ADDRESS;
import static io.buildpal.core.util.ResultUtils.newEntity;
import static io.buildpal.node.engine.Flow.END;

public class Engine extends AbstractVerticle {
    private final static Logger logger = LoggerFactory.getLogger(Engine.class);

    public static final String START = "pipeline.instance.start";
    public static final String ABORT = "pipeline.instance.abort";
    public static final String DELETE = "pipeline.instance.delete";

    private List<Plugin> setupPlugins;
    private List<Plugin> tearDownPlugins;
    private List<Plugin> phasePlugins;

    private LocalMap<String, Flow> currentFlows;

    @Override
    public void start(Future<Void> startFuture) {
        try {
            currentFlows = vertx.sharedData().getLocalMap("currentFlows");

            registerPipelineHandlers();
            registerFlowHandlers();

            deployPlugins(loadPlugins(), startFuture);

        } catch (Exception ex) {
            logger.error("Unable to start pipeline engine", ex);
            startFuture.fail(ex);
        }
    }

    private void registerPipelineHandlers() {

        vertx.eventBus().<JsonObject>consumer(START, mh -> {
            // Received a request to start the build.
            // Start the pipeline flow.
            Flow flow = new Flow(mh.body(), setupPlugins, tearDownPlugins, phasePlugins, vertx.eventBus());
            currentFlows.put(flow.getBuild().getID(), flow);

            flow.start();
        });

        vertx.eventBus().<JsonObject>consumer(DELETE, mh -> {
            // Received a request to delete the build.
            final Build build = new Build(mh.body());

            List<String> containerIDs = build.getAllContainerIDs();

            // Delete the containers, if any on this instance, asynchronously --best effort (relying on vertx).
            vertx.eventBus().send(DELETE_CONTAINERS_ADDRESS, newEntity(containerIDs));
        });

        vertx.eventBus().<JsonObject>consumer(ABORT, mh -> {
            // Received a request to abort the build.
            Flow flow = currentFlows.get(new Build(mh.body()).getID());

            if (flow == null) return;

            // Notify the flow to abort.
            List<String> containerIDs = flow.abort();

            if (!containerIDs.isEmpty()) {
                // Kill the containers asynchronously - best effort (relying on vertx).
                vertx.eventBus().send(KILL_CONTAINERS_ADDRESS, newEntity(containerIDs));
            }

            mh.reply(flow.getBuild().json());
        });
    }

    private void registerFlowHandlers() {
        vertx.eventBus().localConsumer(EventKey.SETUP_END.getAddress(), flowHandler());
        vertx.eventBus().localConsumer(EventKey.PHASE_END.getAddress(), flowHandler());
        vertx.eventBus().localConsumer(EventKey.TEAR_DOWN_END.getAddress(), flowHandler());

        vertx.eventBus().<JsonObject>localConsumer(END, mh -> {
            Build build = new Build(mh.body());
            currentFlows.remove(build.getID());

            logger.info("Flow processed and removed from the queue: " + build.getID());
        });
    }

    private Handler<Message<JsonObject>> flowHandler() {
        return mh -> {
            try {
                Event event = new Event(mh.body());

                Flow flow = currentFlows.get(event.getBuildID());
                flow.process(event);

            } catch (Exception ex) {
                logger.error("Unable to process flow.", ex);
            }
        };
    }

    private List<Plugin> loadPlugins() {
        List<Plugin> setupPlugins = new ArrayList<>();
        List<Plugin> tearDownPlugins = new ArrayList<>();
        List<Plugin> phasePlugins = new ArrayList<>();

        List<Plugin> plugins = new ArrayList<>();

        ServiceLoader<Plugin> pluginServices = ServiceLoader.load(Plugin.class);

        for (Plugin plugin : pluginServices) {
            plugins.add(plugin);

            for (CommandKey key : plugin.commandKeysToRegister()) {
                switch (key) {
                    case SETUP:
                        setupPlugins.add(plugin);
                        break;

                    case RUN_PHASE:
                        phasePlugins.add(plugin);
                        break;

                    case TEAR_DOWN:
                        tearDownPlugins.add(plugin);
                        break;
                }
            }
        }

        setupPlugins.sort(Comparator.comparingInt(Plugin::order));
        phasePlugins.sort(Comparator.comparingInt(Plugin::order));
        tearDownPlugins.sort(Comparator.comparingInt(Plugin::order));

        this.setupPlugins = Collections.unmodifiableList(setupPlugins);
        this.tearDownPlugins = Collections.unmodifiableList(tearDownPlugins);
        this.phasePlugins = Collections.unmodifiableList(phasePlugins);

        logger.info("Registered plugins count: " + plugins.size());

        return plugins;
    }

    private void deployPlugins(List<Plugin> plugins, Future<Void> startFuture) {

        VertxUtils.deployVerticles(vertx, new ArrayList<>(plugins), config(), h -> {
            if (h.succeeded()) {
                startFuture.complete();

            } else {
                startFuture.fail(h.cause());
            }
        });
    }
}
