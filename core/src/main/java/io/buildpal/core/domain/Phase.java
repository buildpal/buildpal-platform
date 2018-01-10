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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class Phase extends Entity<Phase> {
    static final String CONTAINER_ID = "containerID";

    static final String CONTAINER_HOST = "containerHost";
    static final String CONTAINER_PORT = "containerPort";

    private static final String INDEX = "index";
    private static final String STATUS = "status";

    private static final String _ID = "_id";
    private static final String _NAME = "_name";
    private static final String _CONTAINER_ARGS = "_containerArgs";
    private static final String _DOCKER = "_docker";
    private static final String _ENV = "_env";
    private static final String _REPO = "_repo";

    private static final String _PRE_SCRIPT = "_preScript";
    private static final String _MAIN_SCRIPT = "_mainScript";

    private static final String SH = ".sh";
    private static final String PRE_SH = "_pre.sh";

    private List<Status> runResults = new ArrayList<>();

    public Phase() {
        super();
    }

    public Phase(JsonObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public String getID() {
        return jsonObject.getString(_ID);
    }

    @Override
    public String getName() {
        return jsonObject.getString(_NAME);
    }

    @Override
    public Phase setName(String name) {
        jsonObject.put(_NAME, name);
        return this;
    }

    public int getIndex() {
        return jsonObject.getInteger(INDEX);
    }

    public Phase setIndex(int index) {
        jsonObject.put(INDEX, index);
        return this;
    }

    public String getContainerID() {
        return jsonObject.getString(CONTAINER_ID);
    }

    public Phase setContainerID(String containerID) {
        jsonObject.put(CONTAINER_ID, containerID);
        return this;
    }

    public boolean hasContainerID() {
        return jsonObject.containsKey(CONTAINER_ID) && StringUtils.isNotBlank(getContainerID());
    }

    public String getContainerHost() {
        return jsonObject.getString(CONTAINER_HOST);
    }

    public Phase setContainerHost(String containerHost) {
        jsonObject.put(CONTAINER_HOST, containerHost);
        return this;
    }

    public boolean hasContainerHost() {
        return jsonObject.containsKey(CONTAINER_HOST) && StringUtils.isNotBlank(getContainerHost());
    }

    public int getContainerPort() {
        return jsonObject.getInteger(CONTAINER_PORT);
    }

    public Phase setContainerPort(int containerPort) {
        jsonObject.put(CONTAINER_PORT, containerPort);
        return this;
    }

    public boolean hasContainerPort() {
        try {
            return jsonObject.containsKey(CONTAINER_PORT) && getContainerPort() > -1;

        } catch (Exception ignore) {
            return false;
        }
    }

    public ContainerArgs getContainerArgs() {
        JsonObject containerArgs = jsonObject.getJsonObject(_CONTAINER_ARGS);

        if (containerArgs == null) return null;

        return new ContainerArgs(containerArgs);
    }

    public Docker getDocker() {
        JsonObject docker = jsonObject.getJsonObject(_DOCKER);

        if (docker == null) return null;

        return new Docker(docker);
    }

    public JsonObject getEnv() {
        return jsonObject.getJsonObject(_ENV);
    }

    public String getRepo() {
        return jsonObject.getString(_REPO, EMPTY);
    }

    public String getPreScript() {
        return jsonObject.getString(_PRE_SCRIPT);
    }

    public boolean hasPreScript() {
        return jsonObject.containsKey(_PRE_SCRIPT) && StringUtils.isNotBlank(getPreScript());
    }

    public String getPreScriptFile() {
        return getID() + PRE_SH;
    }

    public String getMainScript() {
        return jsonObject.getString(_MAIN_SCRIPT);
    }

    public String getMainScriptFile() {
        return getID() + SH;
    }

    public Status getStatus() {
        Object status = jsonObject.getValue(STATUS);

        if (status == null || status instanceof Status) return (Status) status;

        if (status instanceof String) return Status.valueOf((String) status);

        throw new UnsupportedOperationException("The current value is not a valid status.");
    }

    public Phase setStatus(Status status) {
        jsonObject.put(STATUS, status);
        return this;
    }

    public Phase addRunResult() {
        runResults.add(Status.IN_FLIGHT);
        return this;
    }

    public Phase updateRunResult(Status status) {
        runResults.remove(runResults.size() - 1);
        runResults.add(status);
        return this;
    }

    public Status getFinalResult() {
        if (runResults.isEmpty()) {
            return Status.IN_FLIGHT;
        }

        for (Status status : runResults) {
            if (status == Status.IN_FLIGHT || status == Status.FAILED || status == Status.CANCELED) return status;
        }

        return Status.DONE;
    }

    public class ContainerArgs extends Entity<ContainerArgs> {
        public final static String USER = "User";

        private static final String _RAW_ARGS = "_rawArgs";
        private static final String IMAGE = "Img";

        public ContainerArgs(JsonObject jsonObject) {
            super(jsonObject);
        }

        public JsonObject rawArgs() {
            return jsonObject.getJsonObject(_RAW_ARGS);
        }

        public boolean hasUser() {
            return rawArgs().containsKey(USER) && StringUtils.isNotBlank(getUser());
        }

        public String getImage() {
            return rawArgs().getString(IMAGE);
        }

        public String getUser() {
            return rawArgs().getString(USER);
        }
    }

    public class Docker extends Entity<Docker> {
        private static final String _BUILD_ENABLED = "_buildEnabled";
        private static final String _PUSH_ENABLED = "_pushEnabled";
        private static final String _COPY_WORKSPACE = "_copyWorkspace";
        private static final String _FOLDERS_TO_COPY = "_foldersToCopy";
        private static final String _TAGS = "_tags";

        public Docker(JsonObject jsonObject) {
            super(jsonObject);
        }

        public boolean buildEnabled() {
            return jsonObject.getBoolean(_BUILD_ENABLED, false);
        }

        public boolean pushEnabled() {
            return jsonObject.getBoolean(_PUSH_ENABLED, false);
        }

        public boolean shouldCopyWorkspace() {
            return jsonObject.getBoolean(_COPY_WORKSPACE, false);
        }

        public List<String> foldersToCopy() {
            JsonArray folders = jsonObject.getJsonArray(_FOLDERS_TO_COPY);
            List<String> foldersToCopy = new ArrayList<>(folders.size());

            for (int f=0; f<folders.size(); f++) {
                foldersToCopy.add(folders.getString(f));
            }

            return foldersToCopy;
        }

        public Set<String> tags() {
            JsonArray _tags = jsonObject.getJsonArray(_TAGS);

            Set<String> tags = new HashSet<>();

            if (_tags != null) {
                for (int t=0; t<_tags.size(); t++) {
                    tags.add(_tags.getString(t));
                }

            }

            return tags;
        }
    }
}
