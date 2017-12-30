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
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Objects;

import static io.buildpal.core.config.Constants.PIPE;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class Sort {
    public enum Direction {
        ASC,
        DESC
    }

    public enum Type {
        DYNAMIC,
        INSTANT
    }

    private final String key;
    private final Direction direction;
    private final Type type;

    Sort(String key, Direction direction, Type type) {
        this.key = Objects.requireNonNull(key);
        this.direction = direction;
        this.type = type;
    }

    public String key() {
        return key;
    }

    public Direction dir() {
        return direction;
    }

    public Type type() {
        return type;
    }

    public static Sort tryParse(String sort) {
        try {
            String[] splits = sort.split(PIPE);

            String key = StringUtils.isNotBlank(splits[0]) ? splits[0].trim() : null;
            Direction dir = Direction.ASC;
            Type type = Type.DYNAMIC;

            if (splits.length > 1) {
                dir = Direction.valueOf(splits[1].trim().toUpperCase());
            }

            if (splits.length == 3) {
                type = Type.valueOf(splits[2].trim().toUpperCase());
            }

            return new Sort(key, dir, type);

        } catch (Exception ex) {
            return null;
        }
    }

    @FunctionalInterface
    public interface Comparer {
        int compare(String key, JsonObject jo1, JsonObject jo2);
    }

    public static class NoCompare implements Comparer {
        @Override
        public int compare(String key, JsonObject jo1, JsonObject jo2) {
            return 0;
        }
    }

    public static class StringCompare implements Comparer {
        @Override
        public int compare(String key, JsonObject jo1, JsonObject jo2) {
            return jo1.getString(key, EMPTY).compareTo(jo2.getString(key, EMPTY));
        }
    }

    public static class LongCompare implements Comparer {
        @Override
        public int compare(String key, JsonObject jo1, JsonObject jo2) {
            return jo1.getLong(key, 0L).compareTo(jo2.getLong(key, 0L));
        }
    }

    public static class InstantCompare implements Comparer {
        @Override
        public int compare(String key, JsonObject jo1, JsonObject jo2) {
            return jo1.getInstant(key, Instant.MIN).compareTo(jo2.getInstant(key, Instant.MIN));
        }
    }
}