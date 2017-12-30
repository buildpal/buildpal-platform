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

import io.buildpal.core.domain.DataItem;
import io.buildpal.core.domain.Pipeline;

import java.util.List;

import static io.buildpal.core.domain.Pipeline.DATA_LIST;

public class PipelineValidator extends BasicValidator<Pipeline> {

    public PipelineValidator() {
        super(Pipeline::new);
    }

    @Override
    protected List<String> validateEntity(Pipeline entity) {
        List<String> errors = super.validateEntity(entity);

        if (entity.json().containsKey(DATA_LIST)) {
            try {

                DataItemValidator dataItemValidator = new DataItemValidator();

                for (DataItem data : entity.dataList()) {
                    errors.addAll(dataItemValidator.validateEntity(data));
                }

            } catch (Exception ex) {
                errors.add("Specify a valid data list.");
            }
        }

        return errors;
    }
}
