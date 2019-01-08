/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.commands.solr;

import ddf.security.encryption.EncryptionService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.utils.URIBuilder;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;

@Service
@Command(
  scope = SolrCommands.NAMESPACE,
  name = "backup",
  description = "Makes a backup of the selected Solr core/collection."
)
public class BackupCommand extends SolrCommands {
  private static final String DEFAULT_CORE_NAME = "catalog";
  private static final String SEE_COMMAND_USAGE_MESSAGE =
      "Invalid Argument(s). Please see command usage for details.";
  @Reference private EncryptionService encryptionService;

  @Option(
    name = "-d",
    aliases = {"--dir"},
    description =
        "Full path to location where backups will be written. If this option is not supplied"
            + " for single node Solr backups, the backup will be written in the data directory"
            + " for the selected core. For SolrCloud backups, the backup location must be shared"
            + " by all Solr nodes."
  )
  String backupLocation;

  @Option(
    name = "-c",
    aliases = {"--coreName"},
    description =
        "Name of the Solr core/collection to be backed up. If not specified, the 'catalog' core/collection will be backed up."
  )
  String coreName = DEFAULT_CORE_NAME;

  @Option(
    name = "-a",
    aliases = {"--asyncBackup"},
    description = "Perform an asynchronous backup of SolrCloud."
  )
  boolean asyncBackup;

  @Option(
    name = "-s",
    aliases = {"--asyncBackupStatus"},
    description =
        "Get the status of a SolrCloud asynchronous backup. Used in conjunction with --asyncBackupReqId."
  )
  boolean asyncBackupStatus;

  @Option(
    name = "-i",
    aliases = {"--asyncBackupReqId"},
    description =
        "Request Id returned after performing a SolrCloud asynchronous backup. This request Id is used to track"
            + " the status of a given backup. When requesting a backup status, --asyncBackupStatus and --asyncBackupReqId"
            + " are both required options."
  )
  String asyncBackupReqId;

  @Option(
    name = "-n",
    aliases = {"--numToKeep"},
    description =
        "Number of backups to be maintained. If this backup operation will result"
            + " in exceeding this threshold, the oldest backup will be deleted. This option is not supported"
            + " when backing up SolrCloud."
  )
  int numberToKeep = 0;

  @Override
  public Object execute() throws Exception {
    if (isSystemConfiguredWithSolrCloud()) {
      LOGGER.trace("System configuration: Solr Cloud");
      performSolrCloudBackup();
    } else {
      LOGGER.trace("System configuration: Single Node Solr Client");
      performSingleNodeSolrBackup();
    }

    return null;
  }

  private void performSingleNodeSolrBackup() throws URISyntaxException {
    verifySingleNodeBackupInput();

    String backupUrl = getReplicationUrl(coreName);
    httpBuilder = new org.codice.solr.factory.impl.HttpClientBuilder(encryptionService);

    URIBuilder uriBuilder = new URIBuilder(backupUrl);
    uriBuilder.addParameter("command", "backup");

    if (StringUtils.isNotBlank(backupLocation)) {
      uriBuilder.addParameter("location", backupLocation);
    }
    if (numberToKeep > 0) {
      uriBuilder.addParameter("numberToKeep", Integer.toString(numberToKeep));
    }

    URI backupUri = uriBuilder.build();
    printResponse(sendGetRequest(backupUri));
  }

  private void performSolrCloudBackup() throws IOException {
    SolrClient client = null;
    try {
      client = getCloudSolrClient();

      if (asyncBackupStatus) {
        verifyCloudBackupStatusInput();
        printStatus(client, asyncBackupReqId);
      } else {
        verifyCloudBackupInput();
        performSolrCloudBackup(client);
      }
    } finally {
      shutdown(client);
    }
  }

  private void printResponse(@Nullable HttpResponse response) {
    if (response != null) {
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
        printSuccessMessage(String.format("%nBackup of [%s] complete.%n", coreName));
        LOGGER.trace("Backup of {} complete.", coreName);
      } else {
        printErrorMessage(String.format("Error backing up Solr core: [%s]", coreName));
        printErrorMessage(
            String.format(
                "Backup request failed: %d - %s %n",
                statusLine.getStatusCode(), statusLine.getReasonPhrase()));
        LOGGER.debug(
            "Backup request failed: {} - {}",
            statusLine.getStatusCode(),
            statusLine.getReasonPhrase());
      }
    } else {
      printErrorMessage("Could not communicate with Solr. %n");
      LOGGER.debug("Could not communicate with Solr, response was null");
    }
  }

  private boolean backup(
      SolrClient client, String collection, String backupLocation, String backupName)
      throws IOException, SolrServerException {
    CollectionAdminRequest.Backup backup =
        CollectionAdminRequest.backupCollection(collection, backupName).setLocation(backupLocation);
    CollectionAdminResponse response = backup.process(client, collection);
    LOGGER.debug("Backup status: {}", response.getStatus());
    if (response.getStatus() != 0) {
      printErrorMessage("Backup failed.");
      printResponseErrorMessages(response);
    }
    return response.isSuccess();
  }

  private String backupAsync(
      SolrClient client, String collection, String backupLocation, String backupName)
      throws IOException, SolrServerException {

    CollectionAdminRequest.Backup backup =
        CollectionAdminRequest.AsyncCollectionAdminRequest.backupCollection(collection, backupName)
            .setLocation(backupLocation);

    String requestId = backup.processAsync(client);
    LOGGER.debug("Async backup request Id: {}", requestId);
    return requestId;
  }

  private String getBackupName() {
    long timestamp = System.currentTimeMillis();
    return String.format("%s_%d", coreName, timestamp);
  }

  private void verifySingleNodeBackupInput() {
    LOGGER.trace("Verifying single node backup input.");
    if (asyncBackup || asyncBackupStatus || StringUtils.isNotBlank(asyncBackupReqId)) {
      throw new IllegalArgumentException(SEE_COMMAND_USAGE_MESSAGE);
    }
  }

  /** Verify that cloud backup input has all required options. */
  private void verifyCloudBackupInput() {
    LOGGER.trace("Verifying cloud backup input.");
    if (StringUtils.isBlank(backupLocation)) {
      throw new IllegalArgumentException(
          "Backup location must be provided in Cloud Backup." + SEE_COMMAND_USAGE_MESSAGE);
    } else if (StringUtils.isNotBlank(asyncBackupReqId)) {
      throw new IllegalArgumentException(
          "asyncBackupReqId provided without asyncBackupStatus option."
              + SEE_COMMAND_USAGE_MESSAGE);
    } else if (numberToKeep > 0) {
      throw new IllegalArgumentException(
          "numberToKeep provided but is not supported for Solr Cloud backups."
              + SEE_COMMAND_USAGE_MESSAGE);
    }
  }

  /** Verify that cloud backup status input has all required options. */
  private void verifyCloudBackupStatusInput() {
    LOGGER.trace("Verifying cloud backup status input.");
    if (StringUtils.isNotBlank(backupLocation)) {
      LOGGER.debug("Backup location provided during status check, ignoring option.");
    }

    if (asyncBackup) {
      LOGGER.debug("asyncBackup option provided during status check, ignoring option.");
    }

    if (StringUtils.isBlank(asyncBackupReqId)) {
      throw new IllegalArgumentException(
          "asyncBackupReqId must not be empty when checking on async status of a backup.");
    } else if (numberToKeep > 0) {
      throw new IllegalArgumentException(
          "numberToKeep provided but is not supported for Solr Cloud backups."
              + SEE_COMMAND_USAGE_MESSAGE);
    } else if (!StringUtils.equals(coreName, DEFAULT_CORE_NAME)) {
      throw new IllegalArgumentException(
          "coreName provided during status check but is not used. Use the async req id to check the status of your backup."
              + SEE_COMMAND_USAGE_MESSAGE);
    }
  }

  private void performSolrCloudBackup(SolrClient client) {
    String backupName = getBackupName();
    LOGGER.debug(
        "Backing up collection {} to {} using backup name {}.",
        coreName,
        backupLocation,
        backupName);
    printInfoMessage(
        String.format(
            "Backing up collection [%s] to shared location [%s] using backup name [%s].",
            coreName, backupLocation, backupName));
    LOGGER.trace(
        "Backing up collection {} to shared location {} using backup name {}.",
        coreName,
        backupLocation,
        backupName);
    try {
      optimizeCollection(client, coreName);
      if (asyncBackup) {
        String requestId = backupAsync(client, coreName, backupLocation, backupName);
        printInfoMessage("Solr Cloud backup request Id: " + requestId);
        LOGGER.trace("Solr Cloud backup request Id: {}", requestId);
      } else {
        boolean isSuccess = backup(client, coreName, backupLocation, backupName);
        if (isSuccess) {
          printInfoMessage("Backup complete.");
          LOGGER.trace("Backup complete.");
        }
      }
    } catch (Exception e) {
      printErrorMessage("Backup failed.");
      LOGGER.debug("Backup failed.", e);
    }
  }
}
