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

package io.buildpal.core.query.sort;

import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import static org.apache.commons.lang3.StringUtils.EMPTY;

public class Sorter {

    private Comparator<JsonObject> comparator;

    public Sorter(List<Sort> sorts, JsonObject sample) {

        comparator = null;

        for (Sort sort : sorts) {
            comparator = addComparator(comparator, sample, sort);

            if (sort.dir() == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }
        }
    }

    public Comparator<JsonObject> comparator() {
        return comparator;
    }

    private Comparator<JsonObject> addComparator(Comparator<JsonObject> comparator, JsonObject sample, Sort sort) {
        String key = sort.key();
        Object value = sample.getValue(key);

        if (value instanceof String) {
            if (sort.type() == Sort.Type.INSTANT) {
                if (comparator == null) {
                    return Comparator.comparing(instantExtractor(key));

                } else {
                    return comparator.thenComparing(instantExtractor(key));
                }

            } else {
                if (comparator == null) {
                    return Comparator.comparing(stringExtractor(key));

                } else {
                    return comparator.thenComparing(stringExtractor(key));
                }
            }

        }

        if (value instanceof Integer) {
            if (comparator == null) {
                return Comparator.comparing(integerExtractor(key));

            } else {
                return comparator.thenComparing(integerExtractor(key));
            }
        }

        if (value instanceof Long) {
            if (comparator == null) {
                return Comparator.comparing(longExtractor(key));

            } else {
                return comparator.thenComparing(longExtractor(key));
            }
        }

        if (value instanceof Double) {
            if (comparator == null) {
                return Comparator.comparing(doubleExtractor(key));

            } else {
                return comparator.thenComparing(doubleExtractor(key));
            }
        }

        if (comparator == null) {
            return Comparator.comparing(noExtractor());

        } else {
            return comparator.thenComparing(noExtractor());
        }
    }

    private Function<JsonObject, Integer> noExtractor() {
        return jo -> 0;
    }

    private Function<JsonObject, String> stringExtractor(String key) {
        return jo -> jo.getString(key, EMPTY);
    }

    private Function<JsonObject, Integer> integerExtractor(String key) {
        return jo -> jo.getInteger(key, 0);
    }

    private Function<JsonObject, Long> longExtractor(String key) {
        return jo -> jo.getLong(key, 0L);
    }

    private Function<JsonObject, Double> doubleExtractor(String key) {
        return jo -> jo.getDouble(key, 0D);
    }

    private Function<JsonObject, Instant> instantExtractor(String key) {
        return jo -> jo.getInstant(key, Instant.MIN);
    }
}

