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

package io.buildpal.core.domain;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static io.buildpal.core.domain.Phase.CONTAINER_ID;

public class Build extends Entity<Build> {
    public static final String BUILD = "build";

    private static final String STATUS = "status";
    private static final String UTC_END_DATE = "utcEndDate";
    private static final String PIPELINE_ID = "pipelineID";
    private static final String DEPLOYMENT_ID = "deploymentID";
    private static final String PHASES = "phases";
    private static final String DATA = "data";

    private static final String REPO_SECRET = "_repoSecret";

    public Build() {
        super();
    }

    public Build(JsonObject jsonObject) {
        super(jsonObject);
    }

    public Repository getRepository() {
        JsonObject repository = jsonObject.getJsonObject(Repository.REPOSITORY);

        if (repository == null) return null;

        return new Repository(repository);
    }

    public Build setRepository(Repository repository) {
        if (repository == null) {
            repository = new Repository().setType(Repository.Type.NONE);
        }

        jsonObject.put(Repository.REPOSITORY, repository.json());
        return this;
    }

    public Workspace getWorkspace() {
        JsonObject workspace = jsonObject.getJsonObject(Workspace.WORKSPACE);

        if (workspace == null) return null;

        return new Workspace(workspace);
    }

    public Build setWorkspace(Workspace workspace) {
        jsonObject.put(Workspace.WORKSPACE, workspace.json());
        return this;
    }

    public String getPipelineID() {
        return jsonObject.getString(PIPELINE_ID);
    }

    public Build setPipelineID(String pipelineID) {
        jsonObject.put(PIPELINE_ID, pipelineID);
        return this;
    }

    public String getDeploymentID() {
        return jsonObject.getString(DEPLOYMENT_ID);
    }

    public Build setDeploymentID(String deploymentID) {
        jsonObject.put(DEPLOYMENT_ID, deploymentID);
        return this;
    }

    public JsonArray getPhases() {
        JsonArray phases = jsonObject.getJsonArray(PHASES);

        return phases != null ? phases : new JsonArray();
    }

    public Build setPhases(JsonArray phases) {
        jsonObject.put(PHASES, phases);
        return this;
    }

    public JsonObject data() {
        return jsonObject.containsKey(DATA) ? jsonObject.getJsonObject(DATA) : new JsonObject();
    }

    public Build setData(JsonObject rawData, List<DataItem> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            jsonObject.put(DATA, new JsonObject());

        } else {

            JsonObject data = new JsonObject();

            for (DataItem dataItem : dataList) {
                String id = dataItem.getID();

                if (rawData.containsKey(id)) {
                    dataItem.setValue(rawData.getString(id));
                }

                data.put(id, dataItem.json());
            }

            jsonObject.put(DATA, data);
        }
        return this;
    }

    public Secret getRepositorySecret() {
        JsonObject secret = jsonObject.getJsonObject(REPO_SECRET);

        if (secret == null) return null;

        return new Secret(secret);
    }

    public Build setRepositorySecret(JsonObject secret) {
        if (secret != null) {
            jsonObject.put(REPO_SECRET, secret);
        }

        return this;
    }

    public Build clearRepositorySecret() {
        if (jsonObject.containsKey(REPO_SECRET)) jsonObject.remove(REPO_SECRET);

        return this;
    }

    public Status getStatus() {
        Object status = jsonObject.getValue(STATUS);

        if (status == null || status instanceof Status) return (Status) status;

        if (status instanceof String) return Status.valueOf((String) status);

        throw new UnsupportedOperationException("The current value is not a valid status.");
    }

    public Build setStatus(Status status) {
        jsonObject.put(STATUS, status);
        return this;
    }

    public boolean canDelete() {
        Status status = getStatus();

        return status == Status.DONE || status == Status.FAILED ||
                status == Status.CANCELED;
    }

    public boolean canAbort() {
        return canAbort(getStatus());
    }

    /**
     * @return the list of container IDs that are currently running.
     */
    public List<String> markForAbort() {
        setStatus(Status.CANCELED);
        setUtcEndDate(Instant.now(Clock.systemUTC()));

        List<String> containerIDs = new ArrayList<>();

        JsonArray phases = getPhases();

        for (int p=0; p<phases.size(); p++) {
            BuildPhase phase = new BuildPhase(phases.getJsonObject(p));

            if (canAbort(phase.getStatus())) {
                phase.setStatus(Status.CANCELED);

                String containerID = phase.getContainerID();

                if (StringUtils.isNotBlank(containerID)) {
                    containerIDs.add(containerID);
                }
            }
        }

        return containerIDs;
    }

    public Build markForFailure() {
        setStatus(Status.FAILED);
        setUtcEndDate(Instant.now(Clock.systemUTC()));

        JsonArray phases = getPhases();

        for (int p=0; p<phases.size(); p++) {
            BuildPhase phase = new BuildPhase(phases.getJsonObject(p));

            if (canAbort(phase.getStatus())) {
                phase.setStatus(Status.FAILED);
            }
        }

        return this;
    }

    /**
     * Marks the build as DONE if the phases ran successfully.
     *
     * @return the {@link Build} instance.
     */
    public Build markForComplete() {
        setUtcEndDate(Instant.now(Clock.systemUTC()));

        JsonArray phases = getPhases();

        for (int p=0; p<phases.size(); p++) {
            BuildPhase phase = new BuildPhase(phases.getJsonObject(p));

            if (phase.getStatus() == Status.DONE) {
                continue;
            }

            return this.setStatus(Status.FAILED);
        }

        return this.setStatus(Status.DONE);
    }

    public List<String> getAllContainerIDs() {
        List<String> containerIDs = new ArrayList<>();

        JsonArray phases = getPhases();

        for (int p=0; p<phases.size(); p++) {
            BuildPhase phase = new BuildPhase(phases.getJsonObject(p));

            String containerID = phase.getContainerID();

            if (StringUtils.isNotBlank(containerID)) {
                containerIDs.add(containerID);
            }
        }

        return containerIDs;
    }

    public Build updatePhase(Phase phase) {
        JsonArray phases = getPhases();

        for (int p=0; p<phases.size(); p++) {
            BuildPhase buildPhase = new BuildPhase(phases.getJsonObject(p));

            if (phase.getID().equals(buildPhase.getID())) {
                buildPhase
                        .setStatus(phase.getStatus())
                        .setContainerID(phase.getContainerID());
                break;
            }
        }

        return this;
    }

    public Instant getUtcEndDate() {
        return jsonObject.getInstant(UTC_END_DATE);
    }

    public Build setUtcEndDate(Instant utcEndDate) {
        jsonObject.put(UTC_END_DATE, utcEndDate);
        return this;
    }

    public static class BuildPhase extends Entity<BuildPhase> {
        public BuildPhase() {
            super();
        }

        public BuildPhase(JsonObject jsonObject) {
            super(jsonObject);
        }

        public Status getStatus() {
            Object status = jsonObject.getValue(STATUS);

            if (status == null || status instanceof Status) return (Status) status;

            if (status instanceof String) return Status.valueOf((String) status);

            throw new UnsupportedOperationException("The current value is not a valid status.");
        }

        public BuildPhase setStatus(Status status) {
            jsonObject.put(STATUS, status);
            return this;
        }

        public String getContainerID() {
            return jsonObject.getString(CONTAINER_ID);
        }

        public BuildPhase setContainerID(String containerID) {
            jsonObject.put(CONTAINER_ID, containerID);
            return this;
        }

        public static BuildPhase fromPhase(Phase phase) {
            return new Build.BuildPhase()
                    .setID(phase.getID())
                    .setName(phase.getName())
                    .setStatus(phase.getStatus())
                    .setContainerID(phase.getContainerID());
        }
    }

    public static boolean canAbort(Status status) {
        return status == Status.PARKED || status == Status.PRE_FLIGHT ||
                status == Status.IN_FLIGHT || status == Status.WAITING;
    }
}
