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

package io.buildpal.node.auth;

import io.buildpal.auth.Authenticator;
import io.buildpal.auth.LdapAuthenticator;
import io.buildpal.auth.VaultAuthenticator;
import io.buildpal.auth.vault.VaultService;
import io.buildpal.core.config.Constants;
import io.buildpal.core.domain.Entity;
import io.buildpal.core.domain.User;
import io.buildpal.core.query.QuerySpec;
import io.buildpal.core.util.VertxUtils;
import io.buildpal.db.file.UserManager;
import io.buildpal.node.router.BaseRouter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static io.buildpal.auth.util.AuthConfigUtils.isLdapEnabled;
import static io.buildpal.core.util.ResultUtils.failed;
import static io.buildpal.core.util.ResultUtils.getEntities;

public class LoginAuthHandler implements Handler<RoutingContext> {
    private static final Logger logger = LoggerFactory.getLogger(LoginAuthHandler.class);

    private static final String ACCESS_TOKEN = "access_token";

    private static final String USERNAME_QUERY = "username eq \"%s\"";

    private final JWTAuth jwtAuth;
    private final UserManager userManager;

    private final Authenticator vaultAuthenticator;
    private final Authenticator ldapAuthenticator;

    public LoginAuthHandler(Vertx vertx, JsonObject config, JWTAuth jwtAuth, UserManager userManager) {
        this.jwtAuth = jwtAuth;
        this.userManager = userManager;

        vaultAuthenticator = new VaultAuthenticator(new VaultService(vertx));

        if (isLdapEnabled(config)) {
            ldapAuthenticator = new LdapAuthenticator(config);

        } else {
            ldapAuthenticator = (User user, Handler<AsyncResult<User>> handler) ->
                    VertxUtils.future(handler).complete(null);
        }
    }

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest req = context.request();

        if (req.method() != HttpMethod.POST) {
            context.fail(405); // Must be a POST

        } else {
            User user = new User(context.getBodyAsJson());

            if (isValid(user)) {
                // Make the username all lowercase.
                checkUserInDB(user.setUserName(user.getUserName()), context);

            } else {
                fail(user.getUserName(), context);
            }
        }
    }

    private void checkUserInDB(User user, RoutingContext context) {
        QuerySpec querySpec = new QuerySpec()
                .setQuery(String.format(USERNAME_QUERY, user.getUserName()));

        userManager.find(querySpec, fh -> {
            if (failed(fh)) {
                fail(user.getUserName(), context);

            } else {
                List<JsonObject> items = getEntities(fh.result());

                if (items.size() == 1) {
                    user.merge(items.get(0));

                    authenticate(user, context);

                } else {
                    // Perhaps, this is an LDAP user logging in for the first time.
                    logger.debug("Possible LDAP first time user: " + user.getUserName());

                    authenticate(user.setType(User.Type.LDAP).setID(null), context);
                }
            }
        });
    }

    private void authenticate(User user, RoutingContext context) {
        Authenticator authenticator = user.getType() == User.Type.LOCAL ? vaultAuthenticator : ldapAuthenticator;

        authenticator.authenticate(user, ah -> {
            user.clearPassword();

            if (ah.result() != null) {

                if (user.getID() == null) {
                    // Add first time LDAP user to the DB.
                    saveLdapUserToDB(ah.result(), context);

                } else {
                    // TODO: Check OTP and redirect.
                    // All good. Send JWT.
                    BaseRouter.writeResponse(context, generateJWT(user.getUserName()));
                }

            } else {
                fail(user.getUserName(), context);
            }
        });
    }

    private void saveLdapUserToDB(User user, RoutingContext context) {
        if (logger.isDebugEnabled()) {
            logger.debug("Saving LDAP user: " + user.json().encode());
        }

        userManager.add(user.json(), ah -> {
            if (failed(ah)) {
                logger.error("Unable to save first time LDAP user. Object: " + ah.result());
                fail(user.getUserName(), context);

            } else {
                // All good. Send JWT.
                BaseRouter.writeResponse(context, generateJWT(user.getUserName()));
            }
        });
    }

    private JsonObject generateJWT(String username) {
        JsonObject options = new JsonObject()
                .put(Entity.ID, username)
                .put(Constants.SUBJECT, username);

        // Generate jwt.
        return new JsonObject()
                .put(Entity.ID, username)
                .put(ACCESS_TOKEN, jwtAuth.generateToken(options));
    }

    private void fail(String userName, RoutingContext context) {
        logger.error("Unable to authenticate user: " + userName);
        context.fail(401);
    }

    private boolean isValid(User user) {
        return StringUtils.isNotBlank(user.getUserName()) && StringUtils.isNotBlank(user.getPassword());
    }
}
