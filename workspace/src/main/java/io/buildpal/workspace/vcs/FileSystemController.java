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
import io.buildpal.core.domain.Secret;
import io.buildpal.core.domain.Workspace;
import io.buildpal.core.util.FileUtils;
import io.vertx.core.json.JsonObject;

import java.io.File;

public class FileSystemController extends BaseVersionController {

    public FileSystemController(Repository repository, Workspace workspace) {
        super(repository, workspace);
    }

    @Override
    public void sync(JsonObject data, Secret secret) throws Exception {
        FileUtils.copy(new File(getRepository().getUri()), workspacePath());
    }
}
