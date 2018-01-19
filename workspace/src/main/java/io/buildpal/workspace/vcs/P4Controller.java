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

package io.buildpal.workspace.vcs;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.option.UsageOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import com.perforce.p4java.server.ServerFactory;
import io.buildpal.core.domain.Repository;
import io.buildpal.core.domain.Secret;
import io.buildpal.core.domain.Workspace;
import io.buildpal.core.util.DataUtils;
import io.buildpal.core.util.Utils;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class P4Controller extends BaseVersionController {
    private final static Logger logger = LoggerFactory.getLogger(P4Controller.class);

    public P4Controller(Repository repository, Workspace workspace) {
        super(repository, workspace);
    }

    @Override
    public void sync(JsonObject data, Secret secret) throws Exception {
        String clientName = null;
        IOptionsServer server = null;

        try {
            server = connect(repository);

            if (secret != null) {
                server.setUserName(secret.getUserName());
                server.login(secret.getPwd());
            }

            clientName = Utils.newID();
            sync(server, clientName, data);


        } finally {
            repository.setMetadata(clientName);
            closeQuietly(server);
        }
    }

    @Override
    public void revert(Secret secret) throws Exception {
        if (StringUtils.isBlank(repository.getMetadata())) {
            logger.warn("Client name not found for repository: " + repository.json());
            return;
        }

        IOptionsServer server = null;

        try {
            server = connect(repository);

            if (secret != null) {
                server.setUserName(secret.getUserName());
                server.login(secret.getPwd());
            }

            IClient client = server.getClient(repository.getMetadata());
            server.setCurrentClient(client);

            String path = client.getRoot() + "/...";
            List<IFileSpec> fileSpecs = FileSpecBuilder.makeFileSpecList(path);

            RevertFilesOptions options = new RevertFilesOptions();

            // Revert all changes.
            client.revertFiles(fileSpecs, options);

            // Delete the client.
            server.deleteClient(client.getName(), false);

            logger.debug("Client reverted and deleted: " + repository.getMetadata());

        } catch (Exception ex) {
            logger.error(String.format("Unable to revert client for repository %s", repository.json()), ex);

        } finally {
            closeQuietly(server);
        }
    }

    private IOptionsServer connect(Repository repository) throws Exception {
        UsageOptions options = new UsageOptions(null).setProgramName("Buildpal");

        IOptionsServer server = ServerFactory.getOptionsServer(repository.getUri(), null, options);

        if (repository.isUriSecure()) {
            // FIXME: Is blind trust okay?
            server.addTrust(new TrustOptions().setForce(true).setAutoAccept(true));
        }

        server.connect();

        return server;
    }

    private void sync(IOptionsServer server, String clientName, JsonObject data) throws Exception {
        String[] viewMappings = repository.viewMappings(clientName);

        if (viewMappings != null) {
            for (int v=0; v<viewMappings.length; v++) {
                viewMappings[v] = DataUtils.eval(viewMappings[v], data);
            }
        }

        IClient client = Client.newClient(server, clientName, null, workspace.getPath(), viewMappings);
        server.createClient(client);
        server.setCurrentClient(client);

        logServerInfo(server);

        SyncOptions syncOptions = new SyncOptions()
                .setForceUpdate(repository.isForceUpdate())
                .setQuiet(repository.isQuiet());

        client.sync(new ArrayList<>(), syncOptions);

        if (repository.hasShelvedList()) {
            String shelvedList = DataUtils.eval(repository.getShelvedList(), data);

            for (int cl : DataUtils.parseIntList(shelvedList)) {
                List<IFileSpec> fileSpecs = client.unshelveChangelist(cl, null, IChangelist.DEFAULT, true, false);
                logger.debug(String.format("CL %d un-shelved (%d files)", cl, fileSpecs.size()));
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Sync repo %s to %s completed with data: %s",
                    repository.getName(), workspace.getPath(), data.encode()));
        }
    }

    private void logServerInfo(IOptionsServer server) throws Exception {
        if (logger.isDebugEnabled()) {
            IServerInfo info = server.getServerInfo();

            if (info != null) {
                logger.debug("Info from Perforce server at URI '"
                        + repository.getUri() + "' for '"
                        + server.getUsageOptions().getProgramName() + "':");

                logger.debug("Server address: " + info.getServerAddress() + "\n"
                        + "Server version" + info.getServerVersion() + "\n"
                        + "Client address: " + info.getClientAddress() + "\n"
                        + "Client working directory: " + info.getClientCurrentDirectory() + "\n"
                        + "Client name: " + info.getClientName() + "\n"
                        + "User name: " + info.getUserName());
            }
        }
    }

    private void closeQuietly(IOptionsServer server) {
        if (server != null) {
            try {
                server.logout();
                server.disconnect();

            } catch (Exception e) {
                // Be silent.
            }
        }
    }
}
