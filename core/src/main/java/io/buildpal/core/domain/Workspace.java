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

import io.vertx.core.json.JsonObject;

import java.io.File;

public class Workspace extends Entity<Workspace> {
    public static final String PATH = "path";

    public static final String WORKSPACE = "workspace";

    private static final String USER_PATH = "userPath";
    private static final String ROOT_PATH = "rootPath";
    private static final String PHASES_PATH = "phasesPath";
    private static final String BUILD_CONTEXT_PATH = "buildContextPath";

    public Workspace() {
        super();
    }

    public Workspace(JsonObject jsonObject) {
        super(jsonObject);
    }

    public String getPath() {
        return jsonObject.getString(PATH);
    }

    public File path() {
        return new File(getPath());
    }

    public Workspace setPath(String path) {
        jsonObject.put(PATH, path);
        return this;
    }

    public String getUserPath() {
        return jsonObject.getString(USER_PATH);
    }

    public Workspace setUserPath(String userPath) {
        jsonObject.put(USER_PATH, userPath);
        return this;
    }

    public String getRootPath() {
        return jsonObject.getString(ROOT_PATH);
    }

    public Workspace setRootPath(String rootPath) {
        jsonObject.put(ROOT_PATH, rootPath);
        return this;
    }

    public String getPhasesPath() {
        return jsonObject.getString(PHASES_PATH);
    }

    public Workspace setPhasesPath(String phasesPath) {
        jsonObject.put(PHASES_PATH, phasesPath);
        return this;
    }

    public String getBuildContextPath() {
        return jsonObject.getString(BUILD_CONTEXT_PATH);
    }

    public File buildContextPath() {
        return new File(getBuildContextPath());
    }

    public Workspace setBuildContextPath(String buildContextPath) {
        jsonObject.put(BUILD_CONTEXT_PATH, buildContextPath);
        return this;
    }

    public Workspace cloneMe() {
        return new Workspace(json().copy());
    }
}
