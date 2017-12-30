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

package io.buildpal.auth.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.buildpal.core.config.Constants.KEY;

public class AuthConfigUtils {
    private static final String AUTH = "auth";
    private static final String VAULT = "vault";
    private static final String LDAP = "ldap";
    private static final String LDAP_PROTOCOL = "ldap://";
    private static final String URL = "url";
    private static final String USER_DN_PATTERNS = "userDnPatterns";
    private static final String USER_DISPLAY_NAME_ATTRIBUTE_ID = "userDisplayNameAttributeID";

    private static final String LDAP_URL_ERROR = "Ldap's \"url\" must be specified and valid";
    private static final String USER_DN_PATTERNS_ERROR = "Ldap's \"userDnPatterns\" must be an array of patterns";

    private static final String PBKDF2_WITH_HMAC = "PBKDF2WithHmacSHA512";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATIONS = 2;

    private static final JsonObject EMPTY = new JsonObject();

    public static JsonObject getAuthConfig(JsonObject config) {
        return config.getJsonObject(AUTH, EMPTY);
    }

    public static JsonObject getVaultConfig(JsonObject config) {
        return getAuthConfig(config)
                .getJsonObject(VAULT, EMPTY);
    }

    public static String getVaultKey(JsonObject config) {
        String key = getVaultConfig(config)
                .getString(KEY);

        return Objects.requireNonNull(key, "Vault key must be configured");
    }

    public static boolean isLdapEnabled(JsonObject config) {
        return getAuthConfig(config)
                .containsKey(LDAP);
    }

    public static JsonObject getLdapConfig(JsonObject config) {
        return getAuthConfig(config)
                .getJsonObject(LDAP, EMPTY);
    }

    public static String getLdapUrl(JsonObject config) {
        String ldapUrl = getLdapConfig(config).getString(URL);

        if (StringUtils.isBlank(ldapUrl)) throw new AssertionError(LDAP_URL_ERROR);

        if (!ldapUrl.startsWith(LDAP_PROTOCOL)) throw new AssertionError(LDAP_URL_ERROR);

        return ldapUrl;
    }

    public static List<MessageFormat> getUserDnPatterns(JsonObject config) {
        JsonArray dnPatterns = Objects.requireNonNull(getLdapConfig(config).getJsonArray(USER_DN_PATTERNS),
                USER_DN_PATTERNS_ERROR);

        List<MessageFormat> userDnPatterns = new ArrayList<>();

        for (int i=0; i<dnPatterns.size(); i++) {
            userDnPatterns.add(new MessageFormat(dnPatterns.getString(i)));
        }

        return userDnPatterns;
    }

    public static String[] getDisplayNameAttributeID(JsonObject config) {
        String displayNameAttributeID = getLdapConfig(config)
                .getString(USER_DISPLAY_NAME_ATTRIBUTE_ID);

        if (displayNameAttributeID == null) return null;

        return new String[] { displayNameAttributeID };
    }

    public static byte[] hash(final char[] data, final byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_WITH_HMAC);
        PBEKeySpec spec = new PBEKeySpec(data, salt, ITERATIONS, KEY_LENGTH);

        SecretKey key = secretKeyFactory.generateSecret(spec);
        return key.getEncoded();
    }
}
