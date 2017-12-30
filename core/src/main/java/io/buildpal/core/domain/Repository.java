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
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.List;

import static io.buildpal.core.domain.Secret.SECRET;

public class Repository extends Entity<Repository> {
    public static final String SECRET_ID = "secretID";
    public static final String CHILDREN = "children";

    public static final String HTTP_SSL_SCHEME = "https";
    public static final String P4_SSL_SCHEME = "p4javassl";

    public static final String REPOSITORY = "repository";

    private static final String REPO_URI = "uri";
    private static final String BRANCH = "branch";
    private static final String REMOTE = "remote";

    private static final String VIEW_MAPPINGS = "viewMappings";
    private static final String FORCE_UPDATE = "forceUpdate";
    private static final String QUIET = "quiet";
    private static final String SHELVED_LIST = "shelvedList";

    private static final String P4_CLIENT = "${P4_CLIENT}";

    private static final String PIPELINE_SCAN_ON = "pipelineScanOn";
    private static final String HAS_PIPELINE = "hasPipeline";
    private static final String METADATA = "metadata";

    public enum Type {
        NONE,
        FS,
        GIT,
        MULTI_GIT,
        P4,
        MULTI_P4
    }

    public Repository() {
        super();
    }

    public Repository(JsonObject jsonObject) {
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

    public Repository setType(Type type) {
        jsonObject.put(TYPE, type.name());
        return this;
    }

    public String getUri() {
        return jsonObject.getString(REPO_URI);
    }

    public boolean isUriSecure() {
        try {
            URI uri = new URI(getUri());

            return P4_SSL_SCHEME.equalsIgnoreCase(uri.getScheme()) ||
                    HTTP_SSL_SCHEME.equalsIgnoreCase(uri.getScheme());

        } catch (Exception ex) {
            return false;
        }
    }

    public Repository setUri(String uri) {
        jsonObject.put(REPO_URI, uri);
        return this;
    }

    public String getSecretID() {
        return jsonObject.getString(SECRET_ID);
    }

    public Repository setSecretID(String secretID) {
        jsonObject.put(SECRET_ID, secretID);
        return this;
    }

    public Secret getSecret() {
        JsonObject secret = jsonObject.getJsonObject(SECRET);

        if (secret == null) return null;

        return new Secret(secret);
    }

    public Repository setSecret(JsonObject secret) {
        if (secret != null) {
            jsonObject.put(SECRET, secret);
        }

        return this;
    }

    public boolean hasSecret() {
        return jsonObject.containsKey(SECRET);
    }

    public String getBranch() {
        return jsonObject.getString(BRANCH);
    }

    public Repository setBranch(String branch) {
        jsonObject.put(BRANCH, branch);
        return this;
    }

    public String getRemote() {
        return jsonObject.getString(REMOTE);
    }

    public Repository setRemote(String remote) {
        jsonObject.put(REMOTE, remote);
        return this;
    }

    public JsonArray getViewMappings() {
        return jsonObject.getJsonArray(VIEW_MAPPINGS);
    }

    public String[] viewMappings(String clientname) {
        JsonArray views = getViewMappings();

        if (views != null) {

            String[] viewMappings = new String[views.size()];

            for (int v=0; v<views.size(); v++) {
                viewMappings[v] = StringUtils.replace(views.getString(v), P4_CLIENT, clientname);
            }

            return viewMappings;
        }

        return new String[] {};
    }

    public Repository setViewMappings(List<String> viewMappings) {
        jsonObject.put(VIEW_MAPPINGS, viewMappings);
        return this;
    }

    public boolean isForceUpdate() {
        return jsonObject.getBoolean(FORCE_UPDATE, false);
    }

    public Repository setForceUpdate(boolean forceUpdate) {
        jsonObject.put(FORCE_UPDATE, forceUpdate);
        return this;
    }

    public boolean isQuiet() {
        return jsonObject.getBoolean(QUIET, false);
    }

    public Repository setQuiet(boolean quiet) {
        jsonObject.put(QUIET, quiet);
        return this;
    }

    public boolean hasShelvedList() {
        return jsonObject.containsKey(SHELVED_LIST) && StringUtils.isNotBlank(getShelvedList());
    }

    public String getShelvedList() {
        return jsonObject.getString(SHELVED_LIST);
    }

    public Repository setShelvedList(String shelvedList) {
        jsonObject.put(SHELVED_LIST, shelvedList);
        return this;
    }

    public JsonArray getChildren() {
        return jsonObject.getJsonArray(CHILDREN);
    }

    public boolean isPipelineScanOn() {
        return jsonObject.getBoolean(PIPELINE_SCAN_ON, false);
    }

    public Repository setPipelineScanOn(boolean pipelineScanOn) {
        jsonObject.put(PIPELINE_SCAN_ON, pipelineScanOn);
        return this;
    }

    public boolean hasPipeline() {
        return jsonObject.getBoolean(HAS_PIPELINE, false);
    }

    public Repository setHasPipeline(boolean hasPipeline) {
        jsonObject.put(HAS_PIPELINE, hasPipeline);
        return this;
    }

    public String getMetadata() {
        return jsonObject.getString(METADATA);
    }

    public Repository setMetadata(String metadata) {
        if (metadata != null) {
            jsonObject.put(METADATA, metadata);
        }

        return this;
    }
}
