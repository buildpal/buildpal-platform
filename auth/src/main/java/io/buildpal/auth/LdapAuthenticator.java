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

package io.buildpal.auth;

import io.buildpal.core.domain.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import static io.buildpal.auth.util.AuthConfigUtils.getDisplayNameAttributeID;
import static io.buildpal.auth.util.AuthConfigUtils.getLdapUrl;
import static io.buildpal.auth.util.AuthConfigUtils.getUserDnPatterns;
import static io.buildpal.core.util.VertxUtils.future;

public class LdapAuthenticator implements Authenticator {
    private static final Logger logger = LoggerFactory.getLogger(LdapAuthenticator.class);

    private final static String SUN_CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
    private final static String SIMPLE_AUTHENTICATION = "simple";

    private String url;
    private List<MessageFormat> userDnPatterns;
    private String[] displayNameAttributeID;

    public LdapAuthenticator(JsonObject config) {
        url = getLdapUrl(config);
        userDnPatterns = getUserDnPatterns(config);
        displayNameAttributeID = getDisplayNameAttributeID(config);
    }

    @Override
    public void authenticate(User user, Handler<AsyncResult<User>> handler) {
        Future<User> authFuture = future(handler);

        for (String userDn : prepareUserDns(user.getUserName())) {

            User authenticatedUser = bindUser(userDn, user);

            if (authenticatedUser != null) {
                authFuture.complete(authenticatedUser);
                return;
            }
        }

        authFuture.complete(null);
    }

    private User bindUser(String userDn, User user) {
        logger.debug("LDAP bind operation for user: " + userDn);

        Hashtable<String, String> env = new Hashtable<>();

        env.put(Context.SECURITY_AUTHENTICATION, SIMPLE_AUTHENTICATION);
        env.put(Context.SECURITY_PRINCIPAL, userDn);
        env.put(Context.SECURITY_CREDENTIALS, user.getPassword());
        env.put(Context.INITIAL_CONTEXT_FACTORY, SUN_CONTEXT_FACTORY);
        env.put(Context.PROVIDER_URL, url);

        DirContext dirContext = null;

        try {
            dirContext = new InitialDirContext(env);

            User authenticatedUser = new User()
                    .setUserName(user.getUserName())
                    .setType(User.Type.LDAP);

            if (displayNameAttributeID != null) {
                Attributes attr = dirContext.getAttributes(userDn, displayNameAttributeID);

                if (attr != null) {
                    authenticatedUser.setName(attr.get(displayNameAttributeID[0]).get().toString());
                }
            }

            return authenticatedUser.normalize();

        } catch (NamingException ex) {
            logger.error("Unable to bind user: " + userDn, ex);

        } finally {
            close(dirContext);
        }

        return null;
    }

    private List<String> prepareUserDns(String username) {
        List<String> userDns = new ArrayList<>(userDnPatterns.size());
        String[] args = new String[] { username };

        for (MessageFormat userDnPattern : userDnPatterns) {
            userDns.add(userDnPattern.format(args));
        }

        return userDns;
    }

    private void close(DirContext dirContext) {
        if (dirContext == null) return;

        try {
            dirContext.close();

        } catch (NamingException e) {
            // Do nothing --silent close.
        }
    }
}
