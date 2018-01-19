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
import io.buildpal.core.domain.Phase;
import io.buildpal.core.domain.Status;
import io.buildpal.core.pipeline.Plugin;
import io.buildpal.core.pipeline.event.Command;
import io.buildpal.core.pipeline.event.CommandKey;
import io.buildpal.core.pipeline.event.Event;
import io.buildpal.core.pipeline.event.EventKey;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.Shareable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static io.buildpal.core.config.Constants.BUILD_UPDATE_ADDRESS;
import static io.buildpal.core.domain.Build.BUILD;

/**
 * Coordinates a pipeline instance's state transitions and invokes the plugins registered at
 * the various states.
 *
 * IMPORTANT: This class is marked {@link Shareable} as we expect the {@link Flow} instances
 * to be accessed by a single {@link Engine} verticle per vert.x instance.
 */
class Flow implements Shareable {
    static final String END = "flow.end";

    private final static Logger logger = LoggerFactory.getLogger(Flow.class);

    private static final String SCRIPT = "script";

    private enum State {
        SETUP,
        RUN,
        TEAR_DOWN
    }

    private final Build build;
    private final String script;

    private final List<Plugin> setupPlugins;
    private final List<Plugin> tearDownPlugins;
    private final List<Plugin> phasePlugins;

    private final Queue<State> statesQueue;

    private final EventBus eb;

    private final AtomicInteger pluginCounter;

    private final Queue<List<Phase>> stagesQueue;
    private final Map<Integer, AtomicInteger> phasesCounter;

    private boolean aborted;

    Flow(JsonObject message,
         List<Plugin> setupPlugins,
         List<Plugin> tearDownPlugins,
         List<Plugin> phasePlugins,
         EventBus eb) {

        this.build = new Build(message.getJsonObject(BUILD));
        this.script = message.getString(SCRIPT);

        this.setupPlugins = setupPlugins;
        this.tearDownPlugins = tearDownPlugins;
        this.phasePlugins = phasePlugins;

        this.eb = eb;

        this.statesQueue = new LinkedList<>(List.of(State.SETUP, State.RUN, State.TEAR_DOWN));
        this.pluginCounter = new AtomicInteger(-1);

        this.stagesQueue = new LinkedList<>();
        this.phasesCounter = new HashMap<>();

        this.aborted = false;
    }

    public Build getBuild() {
        return build;
    }

    public void start() {
        build.setStatus(Status.IN_FLIGHT);
        process(null);

        if (logger.isDebugEnabled()) {
            logger.debug("Pipeline instance started: " + build.getID());
        }
    }

    public List<String> abort() {
        if (aborted) {
            logger.warn("Pipeline instance was previously aborted: " + build.getID());
            return List.of();
        }

        aborted = true;
        List<String> containerIDs = build.markForAbort();

        if (logger.isDebugEnabled()) {
            logger.debug("Pipeline instance aborted: " + build.getID());
        }

        return containerIDs;
    }

    public void process(Event event) {
        // Check if this an ad-hoc phase update event.
        if (event != null && event.getKey() == EventKey.PHASE_UPDATE) {
            updateBuildFromAdhocPhaseUpdateEvent(event);
            return;
        }


        State currentState = statesQueue.peek();

        if (aborted && currentState != State.TEAR_DOWN) {
            // If the flow was marked as aborted, move to the tear-down stage.
            nextState();
            return;
        }

        switch (currentState) {
            case SETUP:
                setup(event);
                break;

            case RUN:
                run(event);
                break;

            case TEAR_DOWN:
                tearDown(event);
                break;
        }
    }

    private void setup(Event event) {
        if (updateBuildFromEvent(event)) {
            int counter = pluginCounter.incrementAndGet();

            if (counter == setupPlugins.size()) {
                nextState();

            } else {
                int order = setupPlugins.get(counter).order();
                eb.send(CommandKey.SETUP.getAddress(order), setupCommand());
            }

        } else {
            // Setup processing failed. Skip to tear-down.
            skipFromSetupToTearDown();
        }
    }

    private void run(Event event) {
        List<Phase> stage = stagesQueue.peek();

        if (stage == null) {
            nextState();
            return;
        }

        if (event == null) {
            // Start the stage.
            phasesCounter.clear();

            for (int p=0; p<stage.size(); p++) {
                Phase phase = stage.get(p);
                phase.setIndex(p);
                phase.setStatus(Status.IN_FLIGHT);

                build.updatePhase(phase);

                phasesCounter.put(p, new AtomicInteger(0));

                int order = phasePlugins.get(phasesCounter.get(p).get()).order();

                // Start processing phase.
                // Phases in the same stage are processed in parallel.
                phase.addRunResult();
                eb.send(CommandKey.RUN_PHASE.getAddress(order), runPhaseCommand(phase));
            }

            // Send updates about the parallel phases to DB.
            eb.send(BUILD_UPDATE_ADDRESS, build.json());

        } else {

            // Plugin completed processing the phase.
            Phase eventPhase = event.getPhase();
            int phaseIndex = eventPhase.getIndex();
            Phase stagePhase = stage.get(phaseIndex);

            if (updateBuildFromPhaseEvent(event, eventPhase, stagePhase)) {

                int counter = phasesCounter.get(phaseIndex).incrementAndGet();

                if (counter == phasePlugins.size()) {
                    // All plugins applied on the current phase. Update status.
                    stagePhase.setStatus(stagePhase.getFinalResult());
                    build.updatePhase(stagePhase);

                    verifyStageCompletion(stage);

                } else {
                    int order = phasePlugins.get(counter).order();

                    // Continue processing the phase by passing it to the next plugin.
                    stagePhase.addRunResult();
                    eb.send(CommandKey.RUN_PHASE.getAddress(order), runPhaseCommand(stagePhase));
                }

            } else {
                verifyStageCompletion(stage);
            }
        }
    }

    private void skipFromSetupToTearDown() {
        // Eject SETUP state.
        statesQueue.poll();

        // Calling next state would eject the RUN state, so we move TEAR_DOWN state.
        nextState();
    }

    private void tearDown(Event event) {
        if (updateBuildFromEvent(event)) {
            int counter = pluginCounter.incrementAndGet();

            if (counter == tearDownPlugins.size()) {

                // Mark the build as complete if it wasn't marked for failure or if it was not aborted.
                if (build.getStatus() != Status.FAILED && !aborted) {
                    build.markForComplete();

                    // Update DB again.
                    eb.send(BUILD_UPDATE_ADDRESS, build.json());
                }

                // Notify flow end event.
                eb.send(END, build.json());

            } else {
                int order = tearDownPlugins.get(counter).order();
                eb.send(CommandKey.TEAR_DOWN.getAddress(order), tearDownCommand());
            }

        } else {
            logger.info("Pipeline instance failed: " + build.getID());

            // Tear-down failed. Notify engine that the flow can be removed from its queue.
            eb.send(END, build.json());
        }
    }

    private void nextState() {
        pluginCounter.set(-1);
        statesQueue.poll();

        process(null);
    }

    private boolean updateBuildFromEvent(Event event) {
        if (event == null || aborted) return true;

        // Refresh build from event payload.
        if (event.getStatusCode() == 200) {

            if (event.hasWorkspace()) {
                build.setWorkspace(event.getWorkspace());
            }

            if (event.hasRepository()) {
                build.setRepository(event.getRepository());
            }

            if (event.hasStages()) {
                JsonArray stages = event.getStages();

                JsonArray allPhasesJson = new JsonArray();
                build.setPhases(allPhasesJson);

                for (int s = 0; s < stages.size(); s++) {
                    JsonArray phasesJson = stages.getJsonArray(s);

                    List<Phase> phases = new ArrayList<>();
                    stagesQueue.add(phases);

                    for (int p = 0; p < phasesJson.size(); p++) {
                        Phase phase = new Phase(phasesJson.getJsonObject(p))
                                .setStatus(Status.PARKED);

                        phases.add(phase);

                        allPhasesJson.add(Build.BuildPhase.fromPhase(phase).json());
                    }
                }
            }

            // Update DB.
            eb.send(BUILD_UPDATE_ADDRESS, build.json());

            return true;

        } else {
            build.markForFailure();

            // Update DB.
            eb.send(BUILD_UPDATE_ADDRESS, build.json());

            return false;
        }
    }

    private boolean updateBuildFromPhaseEvent(Event event, Phase eventPhase, Phase stagePhase) {
        // If possible, always update the child repository.
        if (event.hasChildRepository()) {
            // The plugin can update the repo's metadata.
            build.getRepository()
                    .updateChildRepository(event.getChildRepository());
        }

        if (event.getStatusCode() == 200) {
            stagePhase.updateRunResult(Status.DONE);

            if (eventPhase.hasContainerID()) {
                stagePhase.setContainerID(eventPhase.getContainerID());

                if (eventPhase.hasContainerHost()) {
                    stagePhase.setContainerHost(eventPhase.getContainerHost());
                }

                if (eventPhase.hasContainerPort()) {
                    stagePhase.setContainerPort(eventPhase.getContainerPort());
                }
            }

            return true;

        } else {
            stagePhase.updateRunResult(Status.FAILED);
            stagePhase.setStatus(Status.FAILED);

            build.updatePhase(stagePhase);

            // Update DB.
            eb.send(BUILD_UPDATE_ADDRESS, build.json());

            return false;
        }
    }

    private void updateBuildFromAdhocPhaseUpdateEvent(Event adhocEvent) {
        Phase eventPhase = adhocEvent.getPhase();
        List<Phase> stage = stagesQueue.peek();

        if (eventPhase == null || stage == null) return;

        if (adhocEvent.getStatusCode() == 200) {
            Phase stagePhase = stage.get(eventPhase.getIndex());

            if (eventPhase.hasContainerID()) {
                stagePhase.setContainerID(eventPhase.getContainerID());

                if (eventPhase.hasContainerHost()) {
                    stagePhase.setContainerHost(eventPhase.getContainerHost());
                }

                if (eventPhase.hasContainerPort()) {
                    stagePhase.setContainerPort(eventPhase.getContainerPort());
                }

                build.updatePhase(stagePhase);

                // Update DB.
                eb.send(BUILD_UPDATE_ADDRESS, build.json());
            }
        }
    }

    private void verifyStageCompletion(List<Phase> stage) {
        if (isStageComplete(stage)) {
            // We are done with the current stage.
            stagesQueue.poll();

            if (!isStageSuccess(stage)) {
                // No point in running the next stage.
                for (List<Phase> nextStage : stagesQueue) {
                    // Mark all next stages as cancelled.
                    nextStage.forEach(phase -> {
                        phase.setStatus(Status.CANCELED);
                        build.updatePhase(phase);
                    });
                }

                stagesQueue.clear();
            }

            // Update DB.
            eb.send(BUILD_UPDATE_ADDRESS, build.json());

            process(null);
        }
    }

    private JsonObject setupCommand() {
        Command command = new Command()
                .setCommandKey(CommandKey.SETUP)
                .setBuild(build)
                .setScript(script);

        return command.json();
    }

    private JsonObject runPhaseCommand(Phase phase) {
        Command command = new Command()
                .setCommandKey(CommandKey.RUN_PHASE)
                .setBuild(build);

        command.setPhase(phase);

        return command.json();
    }

    private JsonObject tearDownCommand() {
        Command command = new Command()
                .setCommandKey(CommandKey.TEAR_DOWN)
                .setBuild(build);

        return command.json();
    }

    private boolean isStageComplete(List<Phase> stage) {
        return stage.stream().allMatch(p -> {
            Status status = p.getFinalResult();
            return status == Status.DONE || status == Status.FAILED || status == Status.CANCELED;
        });
    }

    private boolean isStageSuccess(List<Phase> stage) {
        return stage.stream().allMatch(p -> p.getStatus() == Status.DONE);
    }
}
