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

import io.buildpal.core.domain.Entity;
import io.buildpal.core.domain.builder.Builder;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class BasicValidator<E extends Entity<E>> implements Validator {

    private Builder<E> builder;

    BasicValidator(Builder<E> builder) {
        this.builder = builder;
    }

    @Override
    public List<String> validate(JsonObject entity) {
        return validateEntity(builder.build(entity));
    }

    @Override
    public List<String> validateID(String id) {
        List<String> errors = new ArrayList<>();

        validateString(errors, id,"Specify an ID.", 32);

        return errors;
    }

    protected List<String> validateEntity(E entity) {
        List<String> errors = validateID(entity.getID());

        validateString(errors, entity.getName(),"Specify a name.");

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

    static void validateString(List<String> errors, String value, String message) {
        if (StringUtils.isBlank(value)) {
            errors.add(message);
        }
    }

    static void validateString(List<String> errors, String value, String message, int minLength) {
        if (StringUtils.isBlank(value)) {
            errors.add(message);

        } else if (StringUtils.trim(value).length() < minLength) {
            errors.add(message);
        }
    }

    static void validateAlphanumericUnderscore(List<String> errors, String value, String message) {
        if (!isAlphanumericUnderscore(value)) {
            errors.add(message);
        }
    }

    static boolean isAlphanumericUnderscore(final CharSequence cs) {
        if (StringUtils.isEmpty(cs)) {
            return false;
        }

        final int sz = cs.length();

        for (int i = 0; i < sz; i++) {
            if (cs.charAt(i) == '_' || Character.isLetterOrDigit(cs.charAt(i))) {
                continue;
            }

            return false;
        }
        return true;
    }

    static void validateInstant(List<String> errors, Instant value, String message) {
        if (value == null) {
            errors.add(message);
        }
    }

    static void validateObject(List<String> errors, Object value, String message) {
        if (value == null) {
            errors.add(message);
        }
    }

    static void validateUri(List<String> errors, String value, String message) {
        try {
            new URI(value);
        } catch (Exception ex) {
            errors.add(message);
        }
    }

    static void validateUri(List<String> errors, String value, Set<String> expectedSchemes, String message) {
        try {
            URI uri = new URI(value);

            if (!expectedSchemes.contains(uri.getScheme())) {
                errors.add(message);
            }

        } catch (Exception ex) {
            errors.add(message);
        }
    }

    /**
     * Ensures that the given value is a valid integer.
     *
     * @param errors Json array that represents validation errors on a given entity.
     * @param value the integer value to validate.
     * @param min the optional minimum value range check (inclusive).
     * @param max the optional maximum value range check (inclusive).
     * @param message error/validation message to add when the validation fails.
     */
    static void validateInteger(List<String> errors, Object value, Integer min, Integer max, String message) {
        try {
            if (value == null) {
                errors.add(message);

            } else {
                int integerValue = (Integer) value;

                if (min != null && min > integerValue) {
                    errors.add(message);
                }

                if (max != null && max < integerValue) {
                    errors.add(message);
                }
            }

        } catch (Exception ex) {
            errors.add(message);
        }
    }
}
