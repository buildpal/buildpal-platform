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

package io.buildpal.db.file;

import io.buildpal.core.domain.validation.Validator;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class BuildManager extends FileDbManager {
    private static final Logger logger = LoggerFactory.getLogger(BuildManager.class);

    public BuildManager(Vertx vertx, JsonObject  config, Validator validator) {
        super(vertx, logger, config, validator);
    }

    @Override
    public String getCollectionName() {
        return "builds";
    }
}
