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

import io.buildpal.core.util.Utils;
import io.vertx.core.json.JsonObject;

import java.time.Clock;
import java.time.Instant;

/**
 * Wrapper that holds the JSON representation of a domain entity.
 */
public abstract class Entity<E> {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String DESC = "description";
    public static final String TYPE = "type";

    public static final String UTC_CREATED_DATE = "utcCreatedDate";
    public static final String UTC_LAST_MODIFIED_DATE = "utcLastModifiedDate";
    public static final String CREATED_BY = "createdBy";
    public static final String LAST_MODIFIED_BY = "lastModifiedBy";

    protected JsonObject jsonObject;

    public Entity() {
        this.jsonObject = new JsonObject();
    }

    public Entity(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @SuppressWarnings("unchecked")
    public E merge(JsonObject jsonObject) {
        this.jsonObject.mergeIn(jsonObject);
        return (E) this;
    }

    @SuppressWarnings("unchecked")
    public E reload(JsonObject jsonObject) {
        this.jsonObject = jsonObject;
        return (E) this;
    }

    public JsonObject json() {
        return jsonObject;
    }

    public String getID() {
        return jsonObject.getString(ID);
    }

    @SuppressWarnings("unchecked")
    public E setID(String id) {
        jsonObject.put(ID, id);
        return (E) this;
    }

    public String getName() {
        return jsonObject.getString(NAME);
    }

    @SuppressWarnings("unchecked")
    public E setName(String name) {
        jsonObject.put(NAME, name);
        return (E) this;
    }

    String getDescription() {
        return jsonObject.getString(DESC);
    }

    @SuppressWarnings("unchecked")
    public E setDescription(String description) {
        jsonObject.put(DESC, description);
        return (E) this;
    }

    public Instant getUtcCreatedDate() {
        return jsonObject.getInstant(UTC_CREATED_DATE);
    }

    @SuppressWarnings("unchecked")
    public E setUtcCreatedDate(Instant utcCreatedDate) {
        jsonObject.put(UTC_CREATED_DATE, utcCreatedDate);
        return (E) this;
    }

    public Instant getUtcLastModifiedDate() {
        return jsonObject.getInstant(UTC_LAST_MODIFIED_DATE);
    }

    @SuppressWarnings("unchecked")
    public E setUtcLastModifiedDate(Instant utcLastModifiedDate) {
        jsonObject.put(UTC_LAST_MODIFIED_DATE, utcLastModifiedDate);
        return (E) this;
    }

    public String getCreatedBy() {
        return jsonObject.getString(CREATED_BY);
    }

    @SuppressWarnings("unchecked")
    public E setCreatedBy(String createdBy) {
        jsonObject.put(CREATED_BY, createdBy);
        return (E) this;
    }

    public String getLastModifiedBy() {
        return jsonObject.getString(LAST_MODIFIED_BY);
    }

    @SuppressWarnings("unchecked")
    public E setLastModifiedBy(String lastModifiedBy) {
        jsonObject.put(LAST_MODIFIED_BY, lastModifiedBy);
        return (E) this;
    }

    public static <Any extends Entity<Any>> void populate(Any entity, String subject) {
        Instant utcNow = Instant.now(Clock.systemUTC());

        entity.setID(Utils.newID())
                .setUtcCreatedDate(utcNow)
                .setCreatedBy(subject)
                .setUtcLastModifiedDate(utcNow)
                .setLastModifiedBy(subject);
    }
}
