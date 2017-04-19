/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.commands.solr;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;
import org.codice.solr.factory.impl.HttpSolrClientFactory;
import org.osgi.service.cm.Configuration;


@Command(scope = SolrCommands.NAMESPACE, name = "backup", description = "Makes a backup of the selected Solr core/collection.")
public class BackupCommand extends SolrCommands {

    @Option(name = "-d", aliases = {"--dir"}, multiValued = false, required = false,
            description = "Full path to location where backups will be written. If none specified, "
                    + "backup will be written in the $data directory for the selected core. For Solr Cloud "
                    + "backups, the backup location must be shared by all Solr nodes.")
    String backupLocation;

    @Option(name = "-c", aliases = {"--coreName"}, multiValued = false, required = false,
            description = "Name of the Solr core/collection to be backed up.  If not specified, the 'catalog' core/collection will be backed up.")
    String coreName = "catalog";

    @Option(name = "-a", aliases = {"--asyncBackup"}, multiValued = false, required = false,
            description = "Perform an asynchronous backup of Solr Cloud.")
    boolean asyncBackup;

    @Option(name = "-s", aliases = {"--asyncBackupStatus"}, multiValued = false, required = false,
            description = "Get the status of a Solr Cloud asynchronous backup. Used in conjunction with --asyncBackupReqId.")
    boolean asyncBackupStatus;

    @Option(name = "-i", aliases = {"--asyncBackupReqId"}, multiValued = false, required = false,
            description = "Request Id returned after performing a Solr Cloud asynchronous backup. This request Id is used to track"
                    + " the status of a given backup. When requesting a backup status, --asyncBackupStatus and --asyncBackupReqId"
                    + " are both required options.")
    String asyncBackupReqId;

    @Option(name = "-n", aliases = {"--numToKeep"}, multiValued = false, required = false,
            description =
                    "Number of backups to be maintained.  If this backup opertion will result"
                            + " in exceeding this threshold, the oldest backup will be deleted. This option is not supported"
                            + " when backing up Solr Cloud.")
    int numberToKeep = 0;

    private static final String SOLR_CLIENT_PROP = "solr.client";

    private static final String CLOUD_SOLR_CLIENT_TYPE = "CloudSolrClient";

    @Override
    public Object doExecute() throws Exception {

        if (isSystemConfiguredWithSolrCloud()) {
            SolrClient client = null;
            try {
                client = getSolrClient();

                if(client == null) {
                    printErrorMessage(String.format(
                            "Could not determine Zookeeper Hosts. Please verify that the system property %s is configured in %s.",
                            ZOOKEEPER_HOSTS_PROP, Paths.get(System.getProperty("ddf.home"), "etc", "system.properties")));
                    return null;
                }

                if (asyncBackupStatus) {
                    if (!isCloudBackupStatusInputValid()) {
                        return null;
                    }
                    getBackupStatus(client, asyncBackupReqId);
                } else {
                    if (!isCloudBackupInputValid()) {
                        return null;
                    }
                    performSolrCloudBackup(client);
                }
            } finally {
                shutdown(client);
            }
        } else {
            preformStandaloneSolrBackup();
        }

        return null;
    }

    private void preformStandaloneSolrBackup() throws Exception {
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
    }

    private void processResponse(ResponseWrapper responseWrapper) throws Exception {

        if (responseWrapper.getStatusCode() == HttpStatus.SC_OK) {
            printSuccessMessage(String.format("%nBackup of [%s] complete.%n", coreName));
        } else {
            printErrorMessage(String.format("Error backing up Solr core: [%s]", coreName));
            printErrorMessage(String.format("Backup command failed due to: %d - %s %n Request: %s",
                    responseWrapper.getStatusCode(),
                    responseWrapper.getStatusPhrase(),
                    getBackupUrl(coreName)));
        }
    }

    private String getBackupUrl(String coreName) {
        String backupUrl = HttpSolrClientFactory.getDefaultHttpsAddress();

        if (configurationAdmin != null) {
            try {
                Configuration solrConfig = configurationAdmin.getConfiguration(
                        "(service.pid=ddf.catalog.solr.external.SolrHttpCatalogProvider)");
                if (solrConfig != null) {
                    if (solrConfig.getProperties() != null && solrConfig.getProperties()
                            .get("url") != null) {
                        LOGGER.debug("Found url property in config, setting backup url");
                        backupUrl = (String) solrConfig.getProperties()
                                .get("url");
                    } else {
                        LOGGER.debug("No Solr config found, checking System settings");
                        if (System.getProperty("host") != null
                                && System.getProperty("jetty.port") != null && System.getProperty(
                                "hostContext") != null) {
                            backupUrl = System.getProperty("urlScheme", "http") + "://"
                                    + System.getProperty("host") +
                                    ":" + System.getProperty("jetty.port") + "/"
                                    + StringUtils.strip(System.getProperty("hostContext"), "/");
                            LOGGER.debug("Trying system configured URL instead: {}", backupUrl);
                        } else {
                            LOGGER.info("No Solr url configured, defaulting to: {}",
                                    HttpSolrClientFactory.getDefaultHttpsAddress());
                        }
                    }
                }
            } catch (IOException e) {
                LOGGER.debug(
                        "Unable to get Solr url from bundle config, will check system properties.");
            }
        }
        return backupUrl + "/" + coreName + "/replication";
    }

    private boolean isSystemConfiguredWithSolrCloud() {
        String solrClientType = System.getProperty(SOLR_CLIENT_PROP);
        LOGGER.debug("solr client type: {}", solrClientType);
        if (solrClientType != null) {
            return StringUtils.equals(solrClientType, CLOUD_SOLR_CLIENT_TYPE);
        } else {
            printErrorMessage(String.format(
                    "Could not determine Solr Client Type. Please verify that the system property %s is configured in %s.",
                    SOLR_CLIENT_PROP, Paths.get(System.getProperty("ddf.home"), "etc", "system.properties")));
            return false;
        }
    }

    private boolean backup(SolrClient client, String collection, String backupLocation,
            String backupName) throws IOException, SolrServerException {
        CollectionAdminRequest.Backup backup = CollectionAdminRequest.backupCollection(collection,
                backupName)
                .setLocation(backupLocation);
        CollectionAdminResponse response = backup.process(client, collection);
        LOGGER.debug("Backup status: {}", response.getStatus());
        return response.isSuccess();
    }

    private String backupAsync(SolrClient client, String collection, String backupLocation,
            String backupName) throws IOException, SolrServerException {

        CollectionAdminRequest.Backup backup =
                CollectionAdminRequest.AsyncCollectionAdminRequest.backupCollection(collection,
                        backupName)
                        .setLocation(backupLocation);

        String requestId = backup.processAsync(client);
        LOGGER.debug("Async backup request Id: {}", requestId);
        return requestId;
    }

    private void getBackupStatus(SolrClient client, String requestId) {
        try {
            CollectionAdminRequest.RequestStatusResponse requestStatusResponse =
                    CollectionAdminRequest.requestStatus(requestId)
                            .process(client);
            RequestStatusState requestStatus = requestStatusResponse.getRequestStatus();
            printInfoMessage(String.format("Backup status for request Id [%s] is [%s].",
                    asyncBackupReqId,
                    requestStatus.getKey()));
            LOGGER.debug("Async backup request status: {}", requestStatus.getKey());
            if (requestStatus == RequestStatusState.FAILED) {
                NamedList<String> errorMessages = requestStatusResponse.getErrorMessages();
                if (errorMessages != null) {
                    for (int i = 0; i < errorMessages.size(); i++) {
                        String name = errorMessages.getName(i);
                        String value = errorMessages.getVal(i);
                        printErrorMessage(String.format("\t%d. Name: %s; Value: %s",
                                i,
                                name,
                                value));
                    }
                }
            }
        } catch (Exception e) {
            printErrorMessage(String.format("Backup status failed. %s", e.getMessage()));
        }
    }

    private String getBackupName() {
        long timestamp = System.currentTimeMillis();
        return String.format("%s_%d", coreName, timestamp);
    }

    private boolean isCloudBackupInputValid() {
        if (StringUtils.isBlank(backupLocation)) {
            printErrorMessage("Insufficient options. Run solr:backup --help for usage details.");
            return false;
        } else {
            return true;
        }
    }

    private boolean isCloudBackupStatusInputValid() {
        if (StringUtils.isBlank(asyncBackupReqId)) {
            printErrorMessage("Insufficient options. Run solr:backup --help for usage details.");
            return false;
        } else {
            return true;
        }
    }

    private void performSolrCloudBackup(SolrClient client) {
        String backupName = getBackupName();
        LOGGER.debug("Backing up collection {} to {} using backup name {}.",
                coreName,
                backupLocation,
                backupName);
        printInfoMessage(String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s].",
                coreName,
                backupLocation,
                backupName));
        if (optimizeCollection(client, coreName)) {
            try {
                if (asyncBackup) {
                    String requestId = backupAsync(client, coreName, backupLocation, backupName);
                    printInfoMessage("Solr Cloud backup request Id: " + requestId);
                } else {
                    boolean isSuccess = backup(client, coreName, backupLocation, backupName);
                    if (isSuccess) {
                        printInfoMessage("Backup complete.");
                    } else {
                        printErrorMessage("Backup failed.");
                    }
                }
            } catch (Exception e) {
                printErrorMessage(String.format("Backup failed. %s", e.getMessage()));
            }
        }
    }

    private boolean optimizeCollection(SolrClient client, String collection) {
        try {
            UpdateResponse updateResponse = client.optimize(collection);
            LOGGER.debug("Optimization status: {}", updateResponse.getStatus());
            if (updateResponse.getStatus() != 0) {
                printErrorMessage(String.format("Backup failed. Unable to optimize collection [%s].",
                        coreName));
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            printErrorMessage(String.format("Backup failed. Unable to optimize collection [%s]. %s",
                    coreName,
                    e.getMessage()));
            return false;
        }
    }
}
