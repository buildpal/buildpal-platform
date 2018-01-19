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

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.buildpal.core.config.Constants.ADMIN;
import static io.buildpal.core.config.Constants.SALT;

public class User extends Entity<User> {

    public enum Type {
        LOCAL,
        LDAP
    }

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    private static final String ROLES = "roles";
    private static final String OTP = "otp";
    private static final String NODE_AFFINITY = "nodeAffinity";

    public User() {
        super();
    }

    public User(String userName, String password) {
        super();

        setType(Type.LOCAL);
        setUserName(userName);
        setPassword(password);
    }

    public User(JsonObject jsonObject) {
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

    public User setType(Type type) {
        jsonObject.put(TYPE, type.name());
        return this;
    }

    public String getUserName() {
        return jsonObject.getString(USERNAME);
    }

    public User setUserName(String userName) {
        jsonObject.put(USERNAME, userName.toLowerCase());
        return this;
    }

    public User normalize() {
        return normalize(this, ADMIN);
    }

    public String getPassword() {
        return jsonObject.getString(PASSWORD);
    }

    public User setPassword(String password) {
        jsonObject.put(PASSWORD, password);
        return this;
    }

    public User clearPassword() {
        jsonObject.remove(PASSWORD);
        return this;
    }

    public JsonArray getRoles() {
        return jsonObject.getJsonArray(ROLES);
    }

    @SuppressWarnings("unchecked")
    public User addRole(String role) {
        JsonArray rolesJson = getRoles();

        if (rolesJson == null || rolesJson.isEmpty()) {
            jsonObject.put(ROLES, new JsonArray().add(role));

        } else {
            Set<String> roles = new HashSet<>(rolesJson.getList());
            roles.add(role);

            final JsonArray newRolesJson = new JsonArray();

            roles.forEach(newRolesJson::add);

            // Finally, add the unique roles.
            jsonObject.put(ROLES, newRolesJson);
        }

        return this;
    }

    public byte[] getSalt() {
        return jsonObject.getBinary(SALT);
    }

    public User setSalt(byte[] salt) {
        jsonObject.put(SALT, salt);
        return this;
    }

    public boolean isOTP() {
        return jsonObject.getBoolean(OTP, true);
    }

    public User setOTP(boolean otp) {
        jsonObject.put(OTP, otp);
        return this;
    }

    public JsonArray getNodeAffinity() {
        return jsonObject.getJsonArray(NODE_AFFINITY);
    }

    @SuppressWarnings("unchecked")
    public User addNodeAffinity(String node) {
        JsonArray nodeAffinity = getNodeAffinity();

        if (nodeAffinity == null || nodeAffinity.isEmpty()) {
            jsonObject.put(NODE_AFFINITY, new JsonArray().add(node));

        } else {
            Set<String> nodes = new HashSet<>(nodeAffinity.getList());
            nodes.add(node);

            final JsonArray newNodeAffinity = new JsonArray();

            nodes.forEach(newNodeAffinity::add);

            // Finally, add the unique nodes.
            jsonObject.put(NODE_AFFINITY, newNodeAffinity);
        }

        return this;
    }

    public User clearNodeAffinity() {
        jsonObject.remove(NODE_AFFINITY);
        return this;
    }

    public static User newLocalUser(String userName, String password, String role) {
        User user = new User(userName, password)
                .addRole(role);

        return normalize(user, ADMIN);
    }

    public static User normalize(User user, String subject) {
        Instant utcNow = Instant.now(Clock.systemUTC());

        return user
                .setUtcCreatedDate(utcNow)
                .setCreatedBy(subject)
                .setUtcLastModifiedDate(utcNow)
                .setLastModifiedBy(subject)
                .setUserName(user.getUserName())
                .setID(user.getUserName());
    }
}
