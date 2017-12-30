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

import io.buildpal.core.domain.Build;

import java.util.ArrayList;
import java.util.List;

public class BuildValidator extends BasicValidator<Build> {

    public BuildValidator() {
        super(Build::new);
    }

    @Override
    protected List<String> validateEntity(Build entity) {
        List<String> errors = new ArrayList<>();

        validateString(errors, entity.getID(),"Specify an ID.", 32);

        validateInstant(errors, entity.getUtcCreatedDate(),
                "Specify a value for when the entity was created.");
        validateString(errors, entity.getCreatedBy(),
                "Specify a value for who created the entity.");

        validateInstant(errors, entity.getUtcLastModifiedDate(),
                "Specify a value for when the entity was last modified.");
        validateString(errors, entity.getLastModifiedBy(),
                "Specify a value for who last modified the entity.");

        return errors;
    }
}
