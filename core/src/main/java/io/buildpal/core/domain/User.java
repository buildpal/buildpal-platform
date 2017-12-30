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

import java.time.Clock;
import java.time.Instant;

import static io.buildpal.core.config.Constants.ADMIN;
import static io.buildpal.core.config.Constants.SALT;

public class User extends Entity<User> {

    public enum Type {
        LOCAL,
        LDAP
    }

    private static final String OTP = "otp";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

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

    public static User newLocalUser(String userName, String password) {
        User user = new User(userName, password)
                .setOTP(true);

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
