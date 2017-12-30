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

package io.buildpal.db;

import io.buildpal.core.query.QuerySpec;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface DbManager {
    String getCollectionName();

    void init(JsonObject config, Handler<AsyncResult<JsonObject>> handler);

    JsonObject get(String id);

    void get(String id, Handler<AsyncResult<JsonObject>> handler);

    void getGraph(String id, Handler<AsyncResult<JsonObject>> handler);

    void add(JsonObject entity, Handler<AsyncResult<JsonObject>> handler);

    void replace(JsonObject entity, Handler<AsyncResult<JsonObject>> handler);

    void delete(String id, Handler<AsyncResult<JsonObject>> handler);

    void find(QuerySpec querySpec, Handler<AsyncResult<JsonObject>> handler);
}
