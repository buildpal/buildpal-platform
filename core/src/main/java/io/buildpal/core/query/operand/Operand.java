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

package io.buildpal.core.query.operand;

public class Operand {

    private Object value;
    private String strValue;
    private Long longValue;
    private Double doubleValue;
    private Boolean boolValue;

    public Operand() {
        reset();
    }

    public String getStrValue() {
        return strValue;
    }

    public Operand setStrValue(String strValue) {
        reset();
        this.strValue = strValue;

        return this;
    }

    public Long getLongValue() {
        return longValue;
    }

    public Operand setLongValue(Long longValue) {
        reset();
        this.longValue = longValue;

        return this;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public Operand setDoubleValue(Double doubleValue) {
        reset();
        this.doubleValue = doubleValue;

        return this;
    }

    public Boolean getBoolValue() {
        return boolValue;
    }

    public Operand setBoolValue(Boolean boolValue) {
        reset();
        this.boolValue = boolValue;
        return this;
    }

    public Object getValue() {
        return value;
    }

    public Operand setValue(Object value) {
        reset();
        this.value = value;

        return this;
    }

    public Long toLong() {
        if (getValue() instanceof Long) {
            return (Long) getValue();

        } else if (getValue() instanceof Double) {
            return ((Double) getValue()).longValue();

        } else if (getValue() instanceof Integer) {
            return ((Integer) getValue()).longValue();
        }

        return null;
    }

    public Double toDouble() {
        if (getValue() instanceof Double) {
            return (Double) getValue();

        } else if (getValue() instanceof Long) {
            return ((Long) getValue()).doubleValue();

        } else if (getValue() instanceof Integer) {
            return ((Integer) getValue()).doubleValue();
        }

        return null;
    }

    private void reset() {
        value = null;
        strValue = null;
        longValue = null;
        doubleValue = null;
        boolValue = null;
    }
}
