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

package io.buildpal.core.config;

import io.vertx.core.json.JsonObject;

/**
 * Shared constants across all modules.
 */
public class Constants {
    public static final JsonObject EMPTY_JSON = new JsonObject();

    public static final String SYSTEM_FOLDER_PATH = "systemFolderPath";
    public static final String DATA_FOLDER_PATH = "dataFolderPath";

    public static final String ADMIN = "admin";

    public static final String ITEMS = "items";
    public static final String ITEM = "item";

    public static final String KEY = "key";
    public static final String DATA = "data";
    public static final String SALT = "salt";
    public static final String SUBJECT = "subject";
    public static final String SYSTEM = "system";

    public static final String DASH = "-";
    public static final String DOT = ".";
    public static final String PIPE = "\\|";
    public static final String COMMA = ",";

    public static final String PATH = "path";
    public static final String TAIL = "tail";

    public static final String SUCCESS = "success";

    public static final String BUILD_UPDATE_ADDRESS = "build.update";
    public static final String DELETE_CONTAINERS_ADDRESS = "oci.containers.delete";
    public static final String KILL_CONTAINERS_ADDRESS = "oci.containers.kill";
    public static final String SAVE_USER_AFFINITY_ADDRESS = "user.affinity.save";
    public static final String DELETE_WORKSPACE_ADDRESS = "workspace.delete";

    public final static String BUILDPAL_DATA_VOLUME = "buildpal-data";

    public static final String NODE = "node";
    public static final String DOCKER_VERTICLE = "dockerVerticle";

    public static final String HTTP_PORT = "httpPort";
    public static final String HOST = "host";

    public static int getDockerVerticleHttpPort(JsonObject config, int defaultValue) {
        return config.getJsonObject(DOCKER_VERTICLE, EMPTY_JSON).getInteger(HTTP_PORT, defaultValue);
    }

    public static String getDockerVerticleHostOrIP(JsonObject config, String defaultValue) {
        return config.getJsonObject(DOCKER_VERTICLE, EMPTY_JSON).getString(HOST, defaultValue);
    }
}
