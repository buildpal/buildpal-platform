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

module io.buildpal.core {
    exports io.buildpal.core.config;
    exports io.buildpal.core.domain;
    exports io.buildpal.core.domain.builder;
    exports io.buildpal.core.domain.validation;
    exports io.buildpal.core.pipeline;
    exports io.buildpal.core.pipeline.event;
    exports io.buildpal.core.process;
    exports io.buildpal.core.util;
    exports io.buildpal.core.query;

    requires vertx.core;

    requires org.apache.commons.lang3;

    requires antlr4.runtime;
}