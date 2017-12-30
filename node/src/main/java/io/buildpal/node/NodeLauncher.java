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

package io.buildpal.node;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.impl.launcher.VertxCommandLauncher;
import io.vertx.core.impl.launcher.VertxLifecycleHooks;
import io.vertx.core.json.JsonObject;

public class NodeLauncher extends VertxCommandLauncher implements VertxLifecycleHooks {

    private NodeLauncher() {}

    /**
     * Main entry point.
     *
     * @param args the user command line arguments.
     */
    public static void main(String[] args) {
        new NodeLauncher().dispatch(args);
    }

    /**
     * Hook after the config has been parsed.
     *
     * @param config the read config, empty if none are provided.
     */
    public void afterConfigParsed(JsonObject config) {}

    /**
     * Hook before the vertx instance is started.
     *
     * @param options the configured Vert.x options. Modify them to customize the Vert.x instance.
     */
    public void beforeStartingVertx(VertxOptions options) {
        options.setPreferNativeTransport(true);
    }

    /**
     * Hook after the vertx instance is started.
     *
     * @param vertx the created Vert.x instance
     */
    public void afterStartingVertx(Vertx vertx) {}

    /**
     * Hook before the verticle is deployed.
     *
     * @param deploymentOptions the current deployment options. Modify them to customize the deployment.
     */
    public void beforeDeployingVerticle(DeploymentOptions deploymentOptions) {}

    @Override
    public void beforeStoppingVertx(Vertx vertx) {}

    @Override
    public void afterStoppingVertx() {}

    /**
     * A deployment failure has been encountered. You can override this method to customize the behavior.
     * By default it closes the `vertx` instance.
     *
     * @param vertx             the vert.x instance
     * @param mainVerticle      the verticle
     * @param deploymentOptions the verticle deployment options
     * @param cause             the cause of the failure
     */
    public void handleDeployFailed(Vertx vertx, String mainVerticle, DeploymentOptions deploymentOptions, Throwable cause) {
        // Default behaviour is to close Vert.x if the deploy failed
        vertx.close();
    }
}
