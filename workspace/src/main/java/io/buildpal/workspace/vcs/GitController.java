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
import io.buildpal.core.util.DataUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.eclipse.jgit.api.Git;

public class GitController extends BaseVersionController {
    private final static Logger logger = LoggerFactory.getLogger(GitController.class);

    public GitController(Repository repository, Workspace workspace) {
        super(repository, workspace);
    }

    @Override
    public void sync(JsonObject data, Secret secret) throws Exception {

        String branch = DataUtils.eval(repository.getBranch(), data);

        // TODO: Add SSH and Oauth support.

        Git.cloneRepository()
                .setURI(repository.getUri())
                .setBranch(branch)
                .setRemote(repository.getRemote())
                .setDirectory(workspacePath())
                .call()
                .close();

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Sync repo %s to %s completed with data: %s ",
                    getRepository().getName(), workspace.getPath(), data.encode()));
        }
    }
}
