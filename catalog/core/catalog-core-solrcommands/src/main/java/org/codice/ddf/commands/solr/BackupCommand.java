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

import com.google.common.annotations.VisibleForTesting;
import ddf.security.encryption.EncryptionService;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;

@Service
@Command(
  scope = SolrCommands.NAMESPACE,
  name = "backup",
  description = "Makes a backup of the selected Solr core/collection."
)
public class BackupCommand extends SolrCommands {

  private static final String SOLR_CLIENT_PROP = "solr.client";
  private static final String CLOUD_SOLR_CLIENT_TYPE = "CloudSolrClient";
  private static final String DEFAULT_CORE_NAME = "catalog";
  private static final String SEE_COMMAND_USAGE_MESSAGE =
      "Invalid Argument(s). Please see command usage for details.";
  @Reference EncryptionService encryptionService;

  @Option(
    name = "-d",
    aliases = {"--dir"},
    multiValued = false,
    required = false,
    description =
        "Full path to location where backups will be written. If this option is not supplied "
            + " for single node Solr backups, the backup will be written in the data directory "
            + " for the selected core. For SolrCloud  backups, the backup location must be shared "
            + " by all Solr nodes."
  )
  String backupLocation;

  @Option(
    name = "-c",
    aliases = {"--coreName"},
    multiValued = false,
    required = false,
    description =
        "Name of the Solr core/collection to be backed up. If not specified, the 'catalog' core/collection will be backed up."
  )
  String coreName = DEFAULT_CORE_NAME;

  @Option(
    name = "-a",
    aliases = {"--asyncBackup"},
    multiValued = false,
    required = false,
    description = "Perform an asynchronous backup of SolrCloud."
  )
  boolean asyncBackup;

  @Option(
    name = "-s",
    aliases = {"--asyncBackupStatus"},
    multiValued = false,
    required = false,
    description =
        "Get the status of a SolrCloud asynchronous backup. Used in conjunction with --asyncBackupReqId."
  )
  boolean asyncBackupStatus;

  @Option(
    name = "-i",
    aliases = {"--asyncBackupReqId"},
    multiValued = false,
    required = false,
    description =
        "Request Id returned after performing a SolrCloud asynchronous backup. This request Id is used to track"
            + " the status of a given backup. When requesting a backup status, --asyncBackupStatus and --asyncBackupReqId"
            + " are both required options."
  )
  String asyncBackupReqId;

  @Option(
    name = "-n",
    aliases = {"--numToKeep"},
    multiValued = false,
    required = false,
    description =
        "Number of backups to be maintained. If this backup opertion will result"
            + " in exceeding this threshold, the oldest backup will be deleted. This option is not supported"
            + " when backing up SolrCloud."
  )
  int numberToKeep = 0;

  private org.codice.solr.factory.impl.HttpClientBuilder httpBuilder;

  @Override
  public Object execute() throws Exception {

    if (isSystemConfiguredWithSolrCloud()) {
      performSolrCloudBackup();

    } else {
      performSingleNodeSolrBackup();
    }

    return null;
  }

  private void performSingleNodeSolrBackup() throws URISyntaxException {
    verifySingleNodeBackupInput();

    httpBuilder = new org.codice.solr.factory.impl.HttpClientBuilder(encryptionService);

    String url =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.http.url"));
    String backupUrl = url + "/" + coreName;

    URIBuilder uriBuilder = new URIBuilder(backupUrl);
    uriBuilder.addParameter("command", "backup");

    if (StringUtils.isNotBlank(backupLocation)) {
      uriBuilder.addParameter("location", backupLocation);
    }
    if (numberToKeep > 0) {
      uriBuilder.addParameter("numberToKeep", Integer.toString(numberToKeep));
    }

    URI backupUri = uriBuilder.build();
    processResponse(sendGetRequest(backupUri));
  }

  @VisibleForTesting
  @Nullable
  HttpResponse sendGetRequest(URI backupUri) {
    HttpResponse httpResponse = null;
    HttpGet get = new HttpGet(backupUri);
    HttpClient client = httpBuilder.get().build();

    try {
      LOGGER.debug("Sending request to {}", backupUri);
      httpResponse = client.execute(get);
    } catch (IOException e) {
      LOGGER.debug("Error during request. Returning null response.");
    }

    return httpResponse;
  }

  private void performSolrCloudBackup() throws IOException {
    SolrClient client = null;
    try {
      client = getCloudSolrClient();

      if (asyncBackupStatus) {
        verifyCloudBackupStatusInput();
        getBackupStatus(client, asyncBackupReqId);
      } else {
        verifyCloudBackupInput();
        performSolrCloudBackup(client);
      }
    } finally {
      shutdown(client);
    }
  }

  private void processResponse(@Nullable HttpResponse response) {
    if (response != null) {
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
        printSuccessMessage(String.format("%nBackup of [%s] complete.%n", coreName));
      } else {
        printErrorMessage(String.format("Error backing up Solr core: [%s]", coreName));
        printErrorMessage(
            String.format(
                "Backup request failed: %d - %s %n",
                statusLine.getStatusCode(), statusLine.getReasonPhrase()));
      }
    } else {
      printErrorMessage(String.format("Could not communicate with Solr. %n"));
    }
  }

  private boolean isSystemConfiguredWithSolrCloud() {
    String solrClientType = System.getProperty(SOLR_CLIENT_PROP);
    LOGGER.debug("solr client type: {}", solrClientType);
    if (solrClientType != null) {
      return StringUtils.equals(solrClientType, CLOUD_SOLR_CLIENT_TYPE);
    } else {
      printErrorMessage(
          String.format(
              "Could not determine Solr Client Type. Please verify that the system property %s is configured in %s.",
              SOLR_CLIENT_PROP, SYSTEM_PROPERTIES_PATH));
      return false;
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
      printErrorMessage("Backup failed. ");
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

  private void getBackupStatus(SolrClient client, String requestId) {
    try {
      CollectionAdminRequest.RequestStatusResponse requestStatusResponse =
          CollectionAdminRequest.requestStatus(requestId).process(client);
      RequestStatusState requestStatus = requestStatusResponse.getRequestStatus();
      printInfoMessage(
          String.format(
              "Backup status for request Id [%s] is [%s].",
              asyncBackupReqId, requestStatus.getKey()));
      LOGGER.debug("Async backup request status: {}", requestStatus.getKey());
      if (requestStatus == RequestStatusState.FAILED) {
        printErrorMessage("Backup status failed. ");
        printResponseErrorMessages(requestStatusResponse);
      }
    } catch (Exception e) {
      String message = e.getMessage() != null ? e.getMessage() : "Unable to get status of backup.";
      printErrorMessage(String.format("Backup status failed. %s", message));
    }
  }

  private void printResponseErrorMessages(CollectionAdminResponse response) {
    NamedList<String> errorMessages = response.getErrorMessages();
    if (errorMessages != null) {
      for (int i = 0; i < errorMessages.size(); i++) {
        String name = errorMessages.getName(i);
        String value = errorMessages.getVal(i);
        printErrorMessage(
            String.format("\t%d. Error Name: %s; Error Value: %s", i + 1, name, value));
      }
    }
  }

  private String getBackupName() {
    long timestamp = System.currentTimeMillis();
    return String.format("%s_%d", coreName, timestamp);
  }

  private void verifySingleNodeBackupInput() {
    if (asyncBackup || asyncBackupStatus || StringUtils.isNotBlank(asyncBackupReqId)) {
      throw new IllegalArgumentException(SEE_COMMAND_USAGE_MESSAGE);
    }
  }

  private void verifyCloudBackupInput() {
    if (StringUtils.isBlank(backupLocation)
        || asyncBackupStatus
        || StringUtils.isNotBlank(asyncBackupReqId)
        || numberToKeep > 0) {
      throw new IllegalArgumentException(SEE_COMMAND_USAGE_MESSAGE);
    }
  }

  private void verifyCloudBackupStatusInput() {
    if (StringUtils.isBlank(asyncBackupReqId)
        || numberToKeep > 0
        || StringUtils.isNotBlank(backupLocation)
        || !StringUtils.equals(coreName, DEFAULT_CORE_NAME)
        || asyncBackup) {
      throw new IllegalArgumentException(SEE_COMMAND_USAGE_MESSAGE);
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
    try {
      optimizeCollection(client, coreName);
      if (asyncBackup) {
        String requestId = backupAsync(client, coreName, backupLocation, backupName);
        printInfoMessage("Solr Cloud backup request Id: " + requestId);
      } else {
        boolean isSuccess = backup(client, coreName, backupLocation, backupName);
        if (isSuccess) {
          printInfoMessage("Backup complete.");
        }
      }
    } catch (Exception e) {
      String message = e.getMessage() != null ? e.getMessage() : "";
      printErrorMessage(String.format("Backup failed. %s", message));
    }
  }

  private void optimizeCollection(SolrClient client, String collection)
      throws IOException, SolrServerException {
    LOGGER.debug("Optimization of collection [{}] is in progress.", collection);
    printInfoMessage(String.format("Optimizing of collection [%s] is in progress.", collection));
    UpdateResponse updateResponse = client.optimize(collection);
    LOGGER.debug("Optimization status: {}", updateResponse.getStatus());
    if (updateResponse.getStatus() != 0) {
      throw new SolrServerException(String.format("Unable to optimize collection [%s].", coreName));
    }
  }
}
