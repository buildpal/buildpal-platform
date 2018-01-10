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

public enum EventKey {
    SETUP_END("pipeline:setup:end"),
    PHASE_UPDATE("pipeline:phase:update"),
    PHASE_END("pipeline:phase:end"),
    TEAR_DOWN_END("pipeline:tearDown:end");

    private String address;

    EventKey(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }
}

