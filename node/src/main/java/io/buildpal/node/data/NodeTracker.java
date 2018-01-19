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

package io.buildpal.node.data;

import io.buildpal.core.domain.User;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.LocalMap;

import java.util.ArrayList;
import java.util.List;

import static io.buildpal.core.domain.Entity.ID;

public class NodeTracker {
    private static final Logger logger = LoggerFactory.getLogger(NodeTracker.class);

    private LocalMap<String, JsonArray> userNodeMapping;

    public static NodeTracker ME = new NodeTracker();

    private NodeTracker() {}

    public void load(Vertx vertx, List<JsonObject> items) {
        userNodeMapping = vertx.sharedData().getLocalMap("userNodeMapping");

        for (int i=0; i<items.size(); i++) {
            User user = new User(items.get(i));

            JsonArray nodeAffinity = user.getNodeAffinity();
            if (nodeAffinity != null) {
                userNodeMapping.put(user.getID(), user.getNodeAffinity());
            }
        }

        logger.info("User-Node mapping loaded. Count: " + userNodeMapping.size());
    }

    public void saveNode(User user) {
        if (userNodeMapping == null) {
            logger.warn("Cannot save node. User-Node mapping was not loaded.");

        } else {
            userNodeMapping.put(user.getID(), user.getNodeAffinity());
        }
    }

    public String getNode(String userID) {
        if (userNodeMapping == null) {
            logger.warn("Cannot get node. User-Node mapping was not loaded.");
            return null;
        }

        JsonArray nodeAffinity = userNodeMapping.get(userID);

        if (nodeAffinity == null || nodeAffinity.isEmpty()) return null;

        return nodeAffinity.getString(0);
    }

    public void removeNode(User user) {
        if (userNodeMapping == null) {
            logger.warn("Cannot save node. User-Node mapping was not loaded.");

        } else {
            userNodeMapping.remove(user.getID());
        }
    }

    public List<JsonObject> getAffinities() {
        List<JsonObject> items = new ArrayList<>();

        if (userNodeMapping != null) {
            userNodeMapping.forEach((key, value) ->
                    items.add(new JsonObject().put(ID, key).put("nodes", value)));
        }

        return items;
    }
}
