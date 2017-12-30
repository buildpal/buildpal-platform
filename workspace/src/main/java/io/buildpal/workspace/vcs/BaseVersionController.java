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

package io.buildpal.workspace.vcs;

import io.buildpal.core.domain.Repository;
import io.buildpal.core.domain.Workspace;

import java.io.File;

public abstract class BaseVersionController implements VersionController {

    protected Repository repository;
    protected Workspace workspace;

    BaseVersionController(Repository repository, Workspace workspace) {
        this.repository = repository;
        this.workspace = workspace;
    }

    @Override
    public Repository getRepository() {
        return repository;
    }

    @Override
    public Workspace getWorkspace() {
        return workspace;
    }

    File workspacePath() {
        return workspace.path();
    }
}
