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

import io.buildpal.core.domain.PipelineJs;

import java.util.ArrayList;
import java.util.List;

public class PipelineJsValidator extends BasicValidator<PipelineJs> {

    private static final String EXT_JS = ".js";

    public PipelineJsValidator() {
        super(PipelineJs::new);
    }

    @Override
    public List<String> validateEntity(PipelineJs entity) {
        List<String> errors = new ArrayList<>();

        validateString(errors, entity.getID(), "Specify a pipeline javascript file.");

        // If the javascript file is specified, make sure it has a js extension.
        if (errors.isEmpty()) {
            if (!entity.getID().endsWith(EXT_JS)) {
                errors.add("Please specify a valid pipeline javascript file.");
            }
        }

        validateString(errors, entity.getFilePath(), "Specify the path to the pipeline javascript file.");

        return errors;
    }
}
