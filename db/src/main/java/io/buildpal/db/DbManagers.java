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

package io.buildpal.db;

import io.buildpal.core.domain.validation.BuildValidator;
import io.buildpal.core.domain.validation.PipelineValidator;
import io.buildpal.core.domain.validation.ProjectValidator;
import io.buildpal.core.domain.validation.RepositoryValidator;
import io.buildpal.core.domain.validation.SecretValidator;
import io.buildpal.core.domain.validation.UserValidator;
import io.buildpal.db.file.BuildManager;
import io.buildpal.db.file.PipelineManager;
import io.buildpal.db.file.ProjectManager;
import io.buildpal.db.file.RepositoryManager;
import io.buildpal.db.file.SecretManager;
import io.buildpal.db.file.UserManager;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class DbManagers {
    private final List<DbManager> managers;

    private final UserManager userManager;
    private final ProjectManager projectManager;
    private final RepositoryManager repositoryManager;
    private final PipelineManager pipelineManager;
    private final BuildManager buildManager;
    private final SecretManager secretManager;

    public DbManagers(Vertx vertx, JsonObject config) {
        managers = new ArrayList<>();

        userManager = new UserManager(vertx, config, new UserValidator());
        managers.add(userManager);

        projectManager = new ProjectManager(vertx, config, new ProjectValidator());
        managers.add(projectManager);

        secretManager = new SecretManager(vertx, config, new SecretValidator());
        managers.add(secretManager);

        repositoryManager = new RepositoryManager(vertx, config, new RepositoryValidator(), secretManager);
        managers.add(repositoryManager);

        pipelineManager = new PipelineManager(vertx, config, new PipelineValidator(),
                repositoryManager);
        managers.add(pipelineManager);

        buildManager = new BuildManager(vertx, config, new BuildValidator());
        managers.add(buildManager);
    }

    public UserManager getUserManager() {
        return userManager;
    }

    public ProjectManager getProjectManager() {
        return projectManager;
    }

    public SecretManager getSecretManager() {
        return secretManager;
    }

    public RepositoryManager getRepositoryManager() {
        return repositoryManager;
    }

    public PipelineManager getPipelineManager() {
        return pipelineManager;
    }

    public BuildManager getBuildManager() {
        return buildManager;
    }

    public List<DbManager> getManagers() {
        return managers;
    }
}
