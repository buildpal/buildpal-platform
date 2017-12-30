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

package io.buildpal.core.domain.validation;

import io.buildpal.core.domain.Repository;
import io.vertx.core.json.JsonArray;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.buildpal.core.domain.Repository.CHILDREN;
import static io.buildpal.core.domain.Repository.P4_SSL_SCHEME;

public class RepositoryValidator extends BasicValidator<Repository> {

    private static final String P4_CLIENT_REF = "//${P4_CLIENT}/";

    private static final Set<String> P4_SCHEMES;

    static {
        P4_SCHEMES = new HashSet<>();
        P4_SCHEMES.add("p4java");
        P4_SCHEMES.add(P4_SSL_SCHEME);
    }

    public RepositoryValidator() {
        super(Repository::new);
    }

    @Override
    protected List<String> validateEntity(Repository entity) {
        List<String> errors = super.validateEntity(entity);

        Repository.Type type = entity.getType();
        validateObject(errors, type, "Specify a type.");

        if (type == null) return errors;

        try {
            switch (entity.getType()) {
                case GIT:
                    validateGit(entity, errors);
                    break;

                case MULTI_GIT:
                    if (validateMulti(entity, errors)) {
                        JsonArray children = entity.getChildren();

                        for (int c = 0; c < children.size(); c++) {
                            Repository childRepo = new Repository(children.getJsonObject(c));
                            validateChildGit(childRepo, errors);
                        }
                    }

                    break;

                case FS:
                    validateUri(errors, entity.getUri(), "Specify the file path.");
                    break;

                case P4:
                    validatePerforce(entity, errors);
                    break;

                case MULTI_P4:
                    if (validateMulti(entity, errors)) {
                        JsonArray children = entity.getChildren();

                        for (int c = 0; c < children.size(); c++) {
                            Repository childRepo = new Repository(children.getJsonObject(c));
                            validateChildPerforce(childRepo, errors);
                        }
                    }
                    break;

                case NONE:
                default:
                    break;
            }
        } catch (Exception ex) {
            // TODO: Add proper documentation.
            errors.add("Invalid repository object.");
        }

        return errors;
    }

    private void validateGit(Repository entity, List<String> errors) {
        validateUri(errors, entity.getUri(), "Specify the git url.");
        validateString(errors, entity.getRemote(), "Specify the remote name.");
        validateString(errors, entity.getBranch(), "Specify the remote branch.");

        if (entity.json().containsKey(CHILDREN)) {
            errors.add("Child repositories are not allowed.");
        }
    }

    private void validateChildGit(Repository childRepo, List<String> errors) {
        Repository.Type childType = childRepo.getType();

        validateObject(errors, childType, "Type should be GIT.");

        if (childRepo.getType() != null && childRepo.getType() != Repository.Type.GIT) {
            errors.add("Type should be GIT.");
        }

        validateString(errors, childRepo.getName(),"Specify a name.");
        validateGit(childRepo, errors);
    }

    private void validatePerforce(Repository entity, List<String> errors) {
        validateUri(errors, entity.getUri(), P4_SCHEMES, "Specify a valid perforce url (p4java scheme).");

        try {
            JsonArray viewMappings = entity.getViewMappings();

            if (viewMappings == null || viewMappings.isEmpty()) {
                errors.add("Specify one or more view mappings.");

            } else {
                for (int v = 0; v < viewMappings.size(); v++) {
                    String viewMapping = viewMappings.getString(v);

                    if (StringUtils.isBlank(viewMapping)) {
                        errors.add("View mapping cannot be empty. Index: " + v);

                    } else if (!viewMapping.contains(P4_CLIENT_REF)) {
                        errors.add("Client side of the view mapping should start with (inside the single quotes) '//${P4_CLIENT}/'. Index: " + v);
                    }
                }
            }

        } catch (Exception ex) {
            errors.add("Specify one or more view mappings as an array.");
        }

        if (entity.json().containsKey(CHILDREN)) {
            errors.add("Child repositories are not allowed.");
        }
    }

    private void validateChildPerforce(Repository childRepo, List<String> errors) {
        Repository.Type childType = childRepo.getType();

        validateObject(errors, childType, "Type should be P4.");

        if (childRepo.getType() != null && childRepo.getType() != Repository.Type.P4) {
            errors.add("Type should be P4.");
        }

        validateString(errors, childRepo.getName(),"Specify a name.");
        validatePerforce(childRepo, errors);
    }

    private boolean validateMulti(Repository entity, List<String> errors) {
        try {
            JsonArray children = entity.getChildren();

            if (children == null || children.isEmpty()) {
                errors.add("Multi type repository should have one or more children.");
                return false;
            }

            return true;

        } catch (Exception ex) {
            errors.add("Multi type repository should have one or more children.");
            return false;
        }
    }
}
