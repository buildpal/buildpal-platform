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

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class Pipeline extends Entity<Pipeline> {

    public static final String REPOSITORY_ID = "repositoryID";
    public static final String JS = "js";

    public static final String DATA_LIST = "dataList";

    public Pipeline() {
        super();
    }

    public Pipeline(JsonObject jsonObject) {
        super(jsonObject);
    }

    public String getRepositoryID() {
        return jsonObject.getString(REPOSITORY_ID);
    }

    public Pipeline setRepositoryID(String repositoryID) {
        jsonObject.put(REPOSITORY_ID, repositoryID);
        return this;
    }

    public Repository getRepository() {
        JsonObject repository = jsonObject.getJsonObject(Repository.REPOSITORY);

        if (repository == null) return null;

        return new Repository(repository);
    }

    public Pipeline setRepository(JsonObject repository) {
        if (repository != null) {
            jsonObject.put(Repository.REPOSITORY, repository);
        }

        return this;
    }

    public JsonArray getDataList() {
        return jsonObject.getJsonArray(DATA_LIST, new JsonArray());
    }

    public List<DataItem> dataList() {
        JsonArray rawDataList = getDataList();
        List<DataItem> dataList = new ArrayList<>();

        for (int d=0; d<rawDataList.size(); d++) {
            dataList.add(new DataItem(rawDataList.getJsonObject(d)));
        }

        return dataList;
    }

    public Pipeline setDataList(List<JsonObject> dataList) {
        jsonObject.put(DATA_LIST, dataList);
        return this;
    }
}
