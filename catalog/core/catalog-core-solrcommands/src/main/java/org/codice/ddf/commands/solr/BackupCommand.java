/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.commands.solr;


import java.io.IOException;
import java.net.URI;
import java.util.Dictionary;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.codice.solr.factory.SolrServerFactory;
import org.osgi.service.cm.Configuration;


@Command(scope = SolrCommands.NAMESPACE, name = "backup", description = "Makes a backup of the selected Solr core.")
public class BackupCommand extends SolrCommands {

    @Option(name = "-d", aliases = {"--dir"}, multiValued = false, required = false,
            description = "Full path to location where backups will be written. If none specified, " +
            "backup will be written in the $data directory for the selected core.")
    String backupLocation;

    @Option(name = "-c", aliases = {"--coreName"}, multiValued = false, required = false,
            description = "Name of the Solr core to be backed up.  If not specified, the 'catalog' core will be backed up.")
    String coreName="catalog";

    @Option(name = "-n", aliases = {"--numToKeep"}, multiValued = false, required = false,
            description = "Number of backups to be maintained.  If this backup operation will result" +
                    " in exceeding this threshold, the oldest backup will be deleted.")
    int numberToKeep=0;

    @Override
    public Object doExecute() throws Exception {

        String backupUrl = getBackupUrl(coreName);

        URIBuilder uriBuilder = new URIBuilder(backupUrl);
        uriBuilder.addParameter("command", "backup");

        if (StringUtils.isNotBlank(backupLocation)) {
            uriBuilder.addParameter("location", backupLocation);
        }
        if (numberToKeep > 0) {
            uriBuilder.addParameter("numberToKeep", Integer.toString(numberToKeep));
        }

        URI backupUri = uriBuilder.build();
        LOGGER.debug("Sending request to {}", backupUri.toString());

        HttpWrapper httpClient = getHttpClient();

        processResponse(httpClient.execute(backupUri));


        return null;
    }

    private void processResponse(ResponseWrapper responseWrapper) throws Exception {

        if (responseWrapper.getStatusCode() == HttpStatus.SC_OK) {
            printSuccessMessage(String.format("\nBackup of [%s] complete.\n", coreName));
        } else {
            printErrorMessage(String.format("Error backing up Solr core: [%s]", coreName));
            printErrorMessage(String.format("Backup command failed due to: %d - %s \n Request: %s",
                    responseWrapper.getStatusCode(),
                    responseWrapper.getStatusPhrase(), getBackupUrl(coreName)));
        }
    }

    private String getBackupUrl(String coreName) {
        String backupUrl = SolrServerFactory.DEFAULT_HTTPS_ADDRESS;

        LOGGER.info("Configuration Admin: {}", configurationAdmin);

        if (configurationAdmin != null) {
            try {
                Configuration solrConfig = configurationAdmin.getConfiguration("(service.pid=ddf.catalog.solr.external.SolrHttpCatalogProvider)");

                if (solrConfig != null) {
                    if (solrConfig.getProperties() != null) {
                        Dictionary<String, Object> props = solrConfig.getProperties();
                        if (StringUtils.isNotBlank((String) props.get("url"))) {
                            backupUrl = (String) props.get("url");
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("Unable to get Solr url from config, defaulting to: {}", SolrServerFactory.DEFAULT_HTTPS_ADDRESS);
            }
        }
        return backupUrl + "/" + coreName + "/replication";
    }
}
