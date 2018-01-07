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

package io.buildpal.core.util;

import io.buildpal.core.domain.Entity;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

import static io.buildpal.core.config.Constants.ITEM;
import static io.buildpal.core.config.Constants.ITEMS;
import static io.buildpal.core.domain.validation.Validator.ERRORS;

public class ResultUtils {
    private static final String IDs = "ids";

    public static boolean failed(AsyncResult<JsonObject> r) {
        return r.failed() ||
                (r.result().containsKey(ERRORS) &&
                        r.result().getJsonArray(ERRORS).size() > 0);
    }

    public static boolean msgFailed(AsyncResult<Message<JsonObject>> r) {
        return r.failed() ||
                (r.result().body().containsKey(ERRORS) &&
                        r.result().body().getJsonArray(ERRORS).size() > 0);
    }

    public static boolean failed(JsonObject result) {
        return result.containsKey(ERRORS) && result.getJsonArray(ERRORS).size() > 0;
    }

    public static JsonObject newEntity(String id) {
        return new JsonObject().put(Entity.ID, id);
    }

    public static JsonObject newEntity(List<String> ids) {
        return new JsonObject().put(IDs, ids);
    }

    public static JsonObject item(JsonObject result) {
        return result.getJsonObject(ITEM);
    }

    public static JsonObject prepareResult(JsonObject item) {
        return putEntity(newResult(), item);
    }

    public static JsonObject newResult() {
        return new JsonObject().put(ERRORS, new JsonArray());
    }

    public static JsonObject newResult(List<String> errors) {
        return new JsonObject().put(ERRORS, new JsonArray(errors));
    }

    public static JsonObject newResult(JsonObject jsonObject) {
        return new JsonObject(jsonObject.getMap()).put(ERRORS, new JsonArray());
    }

    public static JsonObject putEntity(JsonObject result, String entity) {
        return putEntity(result, entity != null ? new JsonObject(entity) : null);
    }

    public static JsonObject putEntity(JsonObject result, JsonObject entity) {
        if (entity != null) {
            result.put(ITEM, entity);

        } else {
            result.getJsonArray(ERRORS).add("Entity not found.");
        }

        return result;
    }

    public static JsonObject getEntity(JsonObject result) {
        return result.getJsonObject(ITEM);
    }

    public static JsonObject addEntities(JsonObject result, List<JsonObject> entities) {
        return result.put(ITEMS, entities);
    }

    @SuppressWarnings("unchecked")
    public static List<JsonObject> getEntities(JsonObject jsonObject) {
        return (List<JsonObject>) jsonObject.getJsonArray(ITEMS).getList();
    }

    @SuppressWarnings("unchecked")
    public static List<String> getIDs(JsonObject jsonObject) {
        return (List<String>) jsonObject.getJsonArray(IDs).getList();
    }

    public static JsonObject addError(JsonObject result, String error) {
        result.getJsonArray(ERRORS).add(error);
        return result;
    }

    public static boolean hasError(JsonObject result, String error) {
        if (result.containsKey(ERRORS)) {
            JsonArray errors = result.getJsonArray(ERRORS);

            for (int e=0; e<errors.size(); e++) {
                if (errors.getString(e).contains(error)) return true;
            }
        }

        return false;
    }
}
