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

package io.buildpal.core.pipeline.event;

import io.buildpal.core.domain.Phase;
import io.buildpal.core.domain.Repository;
import io.buildpal.core.domain.Workspace;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static io.buildpal.core.domain.validation.Validator.ERRORS;

public class Event {
    private static final String KEY = "key";
    private static final String BUILD_ID = "buildID";
    private static final String PHASE = "phase";
    private static final String STAGES = "stages";
    private static final String STATUS_CODE = "statusCode";
    private static final String STATUS_MESSAGE = "statusMessage";
    private static final String CHILD_REPOSITORY = "childRepository";

    protected JsonObject jsonObject;

    public Event() {
        jsonObject = new JsonObject();
    }

    public Event(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public EventKey getKey() {
        Object key = jsonObject.getValue(KEY);

        if (key == null || key instanceof EventKey) return (EventKey) key;

        if (key instanceof String) return EventKey.valueOf((String) key);

        throw new UnsupportedOperationException("The current value is not a valid event key.");
    }

    public Event setKey(EventKey key) {
        jsonObject.put(KEY, key);
        return this;
    }

    public String getBuildID() {
        return jsonObject.getString(BUILD_ID);
    }

    public Event setBuildID(String buildID) {
        jsonObject.put(BUILD_ID, buildID);
        return this;
    }

    public Workspace getWorkspace() {
        JsonObject workspace = jsonObject.getJsonObject(Workspace.WORKSPACE);

        if (workspace == null) return null;

        return new Workspace(workspace);
    }

    public Event setWorkspace(Workspace workspace) {
        jsonObject.put(Workspace.WORKSPACE, workspace.json());
        return this;
    }

    public boolean hasWorkspace() {
        return jsonObject.containsKey(Workspace.WORKSPACE);
    }

    public Repository getRepository() {
        JsonObject repository = jsonObject.getJsonObject(Repository.REPOSITORY);

        if (repository == null) return null;

        return new Repository(repository);
    }

    public Event setRepository(Repository repository) {
        if (repository != null) {
            jsonObject.put(Repository.REPOSITORY, repository.json());
        }

        return this;
    }

    public boolean hasRepository() {
        return jsonObject.containsKey(Repository.REPOSITORY);
    }

    public Repository getChildRepository() {
        JsonObject repository = jsonObject.getJsonObject(CHILD_REPOSITORY);

        if (repository == null) return null;

        return new Repository(repository);
    }

    public Event setChildRepository(Repository childRepository) {
        if (childRepository != null) {
            jsonObject.put(CHILD_REPOSITORY, childRepository.json());
        }

        return this;
    }

    public boolean hasChildRepository() {
        return jsonObject.containsKey(CHILD_REPOSITORY);
    }

    public Phase getPhase() {
        JsonObject currentPhase = jsonObject.getJsonObject(PHASE);

        if (currentPhase == null) return null;

        return new Phase(currentPhase);
    }

    public Event setPhase(Phase currentPhase) {
        jsonObject.put(PHASE, currentPhase.json());
        return this;
    }

    public JsonArray getStages() {
        return jsonObject.getJsonArray(STAGES);
    }

    public Event setStages(JsonArray stages) {
        jsonObject.put(STAGES, stages);
        return this;
    }

    public boolean hasStages() {
        return jsonObject.containsKey(STAGES);
    }

    public int getStatusCode() {
        return jsonObject.getInteger(STATUS_CODE, 200);
    }

    public Event setStatusCode(int statusCode) {
        jsonObject.put(STATUS_CODE, statusCode);
        return this;
    }

    public String getStatusMessage() {
        return jsonObject.getString(STATUS_MESSAGE);
    }

    public Event setStatusMessage(String statusMessage) {
        jsonObject.put(STATUS_MESSAGE, statusMessage);
        return this;
    }

    public JsonObject json() {
        return jsonObject;
    }
}

