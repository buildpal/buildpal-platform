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

import io.buildpal.core.domain.Build;
import io.vertx.core.json.JsonObject;

import static io.buildpal.core.domain.Build.BUILD;

public class Command extends Event {
    private static final String COMMAND_KEY = "commandKey";
    private static final String SCRIPT = "script";

    public Command() {
        super();
    }

    public Command(JsonObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public EventKey getKey() {
        return null;
    }

    public CommandKey getCommandKey() {
        Object key = jsonObject.getValue(COMMAND_KEY);

        if (key == null || key instanceof CommandKey) return (CommandKey) key;

        if (key instanceof String) return CommandKey.valueOf((String) key);

        throw new UnsupportedOperationException("The current value is not a valid command key.");
    }

    public Command setCommandKey(CommandKey key) {
        jsonObject.put(COMMAND_KEY, key);
        return this;
    }

    public Build getBuild() {
        JsonObject build = jsonObject.getJsonObject(BUILD);

        if (build == null) return null;

        return new Build(build);
    }

    public Command setBuild(Build build) {
        jsonObject.put(BUILD, build.json());
        return this;
    }

    public String getScript() {
        return jsonObject.getString(SCRIPT);
    }

    public Command setScript(String script) {
        jsonObject.put(SCRIPT, script);
        return this;
    }
}
