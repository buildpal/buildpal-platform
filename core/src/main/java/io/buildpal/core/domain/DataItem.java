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

package io.buildpal.core.domain;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class DataItem extends Entity<DataItem> {

    public static final String DEFAULT_VALUE = "defaultValue";

    static final String VALUE = "value";

    private static final String DATA_KEY = "${data.%s}";

    public enum Type {
        STRING
    }

    public DataItem() {
        super();
    }

    public DataItem(JsonObject jsonObject) {
        super(jsonObject);
    }

    public Type getType() {
        if (jsonObject.containsKey(TYPE)) {
            try {
                return Type.valueOf(jsonObject.getString(TYPE));

            } catch (IllegalArgumentException ex) {
                return null;
            }
        }

        return null;
    }

    public DataItem setType(Type type) {
        jsonObject.put(TYPE, type.name());
        return this;
    }

    public boolean hasDefaultValue() {
        return jsonObject.containsKey(DEFAULT_VALUE) && StringUtils.isNotBlank(getDefaultValue());
    }

    public String value() {
        return jsonObject.getString(VALUE, getDefaultValue());
    }

    public DataItem setValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            jsonObject.put(VALUE, value.trim());
        }

        return this;
    }

    public String getDefaultValue() {
        return jsonObject.getString(DEFAULT_VALUE, EMPTY);
    }

    public DataItem setDefaultValue(String defaultValue) {
        if (StringUtils.isNotBlank(defaultValue)) {
            jsonObject.put(DEFAULT_VALUE, defaultValue.trim());
        }

        return this;
    }

    public String key() {
        return String.format(DATA_KEY, getID());
    }
}
