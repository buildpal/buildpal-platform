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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static io.buildpal.core.domain.DataItem.DEFAULT_VALUE;

public class DataItemValidator extends BasicValidator<DataItem> {

    public DataItemValidator() {
        super(DataItem::new);
    }

    @Override
    public List<String> validateID(String id) {
        List<String> errors = new ArrayList<>();

        validateString(errors, id,"Specify an ID.");

        if (errors.isEmpty()) {
            if (!StringUtils.isAllUpperCase(id.replace("_", ""))) {
                errors.add("ID should be all uppercase.");
            }
        }

        validateAlphanumericUnderscore(errors, id, "ID should be alphanumeric without spaces.");

        return errors;
    }

    @Override
    protected List<String> validateEntity(DataItem entity) {
        List<String> errors = validateID(entity.getID());

        validateString(errors, entity.getName(),"Specify a name.");

        DataItem.Type type = entity.getType();
        validateObject(errors, type, "Specify a type.");

        if (entity.json().containsKey(DEFAULT_VALUE)) {
            validateString(errors, entity.getDefaultValue(),"Specify a default value.");
        }

        return errors;
    }
}
