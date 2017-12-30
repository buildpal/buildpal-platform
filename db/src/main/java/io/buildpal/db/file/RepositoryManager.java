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

import io.buildpal.core.domain.Repository;
import io.buildpal.core.domain.validation.Validator;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import static io.buildpal.core.domain.Repository.SECRET_ID;
import static io.buildpal.core.util.ResultUtils.addError;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.core.util.ResultUtils.newResult;

public class RepositoryManager extends FileDbManager {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryManager.class);

    private final SecretManager secretManager;

    public RepositoryManager(Vertx vertx, JsonObject config, Validator validator, SecretManager secretManager) {
        super(vertx, logger, config, validator);

        this.secretManager = secretManager;
    }

    @Override
    public String getCollectionName() {
        return "repositories";
    }

    @Override
    JsonObject getGraph(String id) {
        JsonObject entity = get(id);

        if (entity != null) {
            Repository repository = new Repository(entity);

            if (StringUtils.isNotBlank(repository.getSecretID())) {
                repository.setSecret(secretManager.get(repository.getSecretID()));
            }

            return repository.json();
        }

        return null;
    }

    @Override
    JsonObject validateEntity(JsonObject entity) {
        JsonObject result = newResult(validator.validate(entity));

        // No validation errors so far. Now, check for secret.
        if (!failed(result)) {

            if (entity.containsKey(SECRET_ID) &&
                    secretManager.get(entity.getString(SECRET_ID)) == null) {
                addError(result, "System cannot find the specified credentials.");
            }
        }

        return result;
    }
}
