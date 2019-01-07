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
import java.util.concurrent.TimeUnit;
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
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.codice.solr.factory.impl.HttpSolrClientFactory;

@Service
@Command(
  scope = SolrCommands.NAMESPACE,
  name = "restore",
  description = "Restores a selected Solr core/collection from backup."
)
public class RestoreCommand extends SolrCommands {
  @Reference private EncryptionService encryptionService;

  @Option(
    name = "-d",
    aliases = {"--dir"},
    description = "The location of the backup files to be restored."
  )
  @VisibleForTesting
  protected String backupLocation;

  @Option(
    name = "-n",
    aliases = {"--name"},
    description =
        "The name of the backed up index snapshot to be restored. If the name is not provided it looks for backups with snapshot.<timestamp> format in the location directory. It picks the latest timestamp backup in that case."
  )
  @VisibleForTesting
  protected String backupName;

  @Option(
    name = "-c",
    aliases = {"--core"},
    description =
        "Specify the Solr core to operate on (e.g. catalog). Defaults to catalog if one isn't provided."
  )
  @VisibleForTesting
  protected String coreName = DEFAULT_CORE_NAME;

  @Option(
    name = "-s",
    aliases = {"--restoreStatus"},
    description =
        "Get the status of a SolrCloud asynchronous restore. Used in conjunction with --requestId."
  )
  @VisibleForTesting
  protected boolean status;

  @Option(
    name = "-i",
    aliases = {"--requestId"},
    description =
        "Request Id returned after performing a restore. This request Id is used to track"
            + " the status of a given restore. When requesting a restore status, --restoreStatus and --requestId"
            + " are both required options."
  )
  @VisibleForTesting
  protected String requestId;

  @Option(
    name = "-a",
    aliases = {"--asyncRestore"},
    description = "Perform an asynchronous restore."
  )
  @VisibleForTesting
  protected boolean asyncRestore;

  @Option(
    name = "-f",
    aliases = {"--force"},
    description = "Forces the restore. Will delete the collection if it already exists"
  )
  @VisibleForTesting
  protected boolean force = false;

  @Override
  public Object execute() throws Exception {
    if (isSystemConfiguredWithSolrCloud()) {
      performSolrCloudRestore();
    } else {
      performSingleNodeSolrRestore();
    }

    return null;
  }

  private void performSingleNodeSolrRestore() throws URISyntaxException {
    String restoreUrl = getReplicationUrl(coreName);

    try {
      createSolrCore();

      httpBuilder = new org.codice.solr.factory.impl.HttpClientBuilder(encryptionService);
      URIBuilder uriBuilder = new URIBuilder(restoreUrl);
      uriBuilder.addParameter("command", "restore");

      if (StringUtils.isNotBlank(backupLocation)) {
        uriBuilder.addParameter("location", backupLocation);
      }

      URI restoreUri = uriBuilder.build();
      LOGGER.debug("Sending request to {}", restoreUri);

      printResponse(sendGetRequest(restoreUri));
    } catch (IOException | SolrServerException e) {
      LOGGER.info("Unable to perform single node Solr restore, core: {}", coreName, e);
    }
  }

  private void performSolrCloudRestore() throws IOException {
    SolrClient client = null;
    try {
      client = getCloudSolrClient();
      if (status) {
        verifyStatusInput();
        printStatus(client, requestId);
      } else {
        verifyRestoreInput();
        performSolrCloudRestore(client);
      }
    } finally {
      shutdown(client);
    }
  }

  private void printResponse(@Nullable HttpResponse response) {
    if (response != null) {
      StatusLine statusLine = response.getStatusLine();
      if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
        printSuccessMessage(String.format("%nRestore of [%s] complete.%n", coreName));
      } else {
        printErrorMessage(String.format("Error restoring Solr core: [%s]", coreName));
        printErrorMessage(
            String.format(
                "Restore command failed due to: %d - %s",
                statusLine.getStatusCode(), statusLine.getReasonPhrase()));
        printErrorMessage(String.format("Request: %s", getReplicationUrl(coreName)));
      }
    }
  }

  private boolean restore(
      SolrClient client, String collection, String backupLocation, String backupName)
      throws IOException, SolrServerException {
    if (!canRestore(client, collection)) {
      LOGGER.warn("Unable to restore collection {}", collection);
      return false;
    }

    CollectionAdminRequest.Restore restore =
        CollectionAdminRequest.AsyncCollectionAdminRequest.restoreCollection(collection, backupName)
            .setLocation(backupLocation);

    String syncReqId = restore.processAsync(client);

    while (true) {
      CollectionAdminRequest.RequestStatusResponse requestStatusResponse =
          CollectionAdminRequest.requestStatus(syncReqId).process(client);
      RequestStatusState requestStatus = requestStatusResponse.getRequestStatus();
      if (requestStatus == RequestStatusState.COMPLETED) {
        LOGGER.debug("Restore status: {}", requestStatus);
        return true;
      } else if (requestStatus == RequestStatusState.FAILED
          || requestStatus == RequestStatusState.NOT_FOUND) {
        LOGGER.debug("Restore status: {}", requestStatus);
        printErrorMessage("Restore failed. ");
        printResponseErrorMessages(requestStatusResponse);
        return false;
      }
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private String restoreAsync(
      SolrClient client, String collection, String backupLocation, String backupName)
      throws IOException, SolrServerException {

    if (canRestore(client, collection)) {
      CollectionAdminRequest.Restore restore =
          CollectionAdminRequest.AsyncCollectionAdminRequest.restoreCollection(
                  collection, backupName)
              .setLocation(backupLocation);

      String requestId = restore.processAsync(client);
      LOGGER.debug("Restore request Id: {}", requestId);
      return requestId;
    } else {
      printErrorMessage(
          String.format("Unable to restore %s, collection already exists.", collection));
      LOGGER.debug("Unable to restore: {}, collection already exists", collection);
      return null;
    }
  }

  private void performSolrCloudRestore(SolrClient client) {
    LOGGER.debug("Restoring collection {} from {} / {}.", coreName, backupLocation, backupName);
    printInfoMessage(
        String.format(
            "Restoring collection [%s] from [%s] / [%s].", coreName, backupLocation, backupName));
    try {
      if (asyncRestore) {
        String requestId = restoreAsync(client, coreName, backupLocation, backupName);
        printInfoMessage("Restore request Id: " + requestId);
      } else {
        boolean isSuccess = restore(client, coreName, backupLocation, backupName);
        if (isSuccess) {
          printInfoMessage("Restore complete.");
        }
      }
    } catch (Exception e) {
      String message = e.getMessage() != null ? e.getMessage() : "";
      printErrorMessage(String.format("Restore failed. %s", message));
      LOGGER.debug("Restore failed for core: {}.", coreName, e);
    }
  }

  private boolean canRestore(SolrClient client, String collection)
      throws IOException, SolrServerException {

    LOGGER.trace("Checking restoration capability of collection {}", collection);
    if (collectionExists(client, collection)) {
      LOGGER.trace("Collection {} already exists", collection);
      if (force) {
        LOGGER.trace("Force option set, deleting existing {} collection", collection);
        CollectionAdminRequest.Delete delete = CollectionAdminRequest.deleteCollection(collection);
        CollectionAdminResponse response = delete.process(client);
        return response.isSuccess();
      } else {
        printErrorMessage(
            String.format(
                "Force option not set, cannot restore %s collection. Use --force to restore when the collection exists.",
                collection));
        LOGGER.trace("Force option not set, cannot restore {} collection", collection);
        return false;
      }
    }
    return true;
  }

  private void createSolrCore() throws IOException, SolrServerException {
    httpBuilder = new org.codice.solr.factory.impl.HttpClientBuilder(encryptionService);
    String url = HttpSolrClientFactory.getDefaultHttpsAddress();
    final String solrDataDir = HttpSolrClientFactory.getSolrDataDir();
    if (solrDataDir != null) {
      ConfigurationStore.getInstance().setDataDirectoryPath(solrDataDir);
    }

    HttpSolrClientFactory.createSolrCore(url, coreName, null, httpBuilder.get().build());
  }

  private void verifyStatusInput() {
    if (status && StringUtils.isBlank(requestId)) {
      throw new IllegalArgumentException(
          "Status request is missing requestId." + SEE_COMMAND_USAGE_MESSAGE);
    }

    if (asyncRestore) {
      throw new IllegalArgumentException(
          "asyncRestore passed to status request." + SEE_COMMAND_USAGE_MESSAGE);
    }
  }

  private void verifyRestoreInput() {

    if (StringUtils.isNotBlank(requestId)) {
      LOGGER.debug("requestId option given without asyncStatus option, ignoring.");
    }

    if (StringUtils.isBlank(backupLocation)) {
      throw new IllegalArgumentException(
          "backupLocation must be provided. " + SEE_COMMAND_USAGE_MESSAGE);
    }

    if (StringUtils.isBlank(backupName)) {
      throw new IllegalArgumentException(
          "backupName must be provided. " + SEE_COMMAND_USAGE_MESSAGE);
    }
  }
}
