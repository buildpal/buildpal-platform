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

module io.buildpal.workspace {
    exports io.buildpal.workspace;

    provides io.buildpal.core.pipeline.Plugin
            with io.buildpal.workspace.WorkspaceVerticle, io.buildpal.workspace.ScriptVerticle;

    requires java.scripting;
    requires jdk.scripting.nashorn;

    requires vertx.core;
    requires vertx.lang.js;

    requires org.apache.commons.lang3;

    requires org.eclipse.jgit;

    requires p4java;

    requires io.buildpal.auth;
    requires io.buildpal.core;
}