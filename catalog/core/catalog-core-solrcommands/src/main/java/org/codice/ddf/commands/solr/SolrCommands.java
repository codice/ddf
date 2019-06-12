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
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.Nullable;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SolrCommands implements Action {

  protected static final Logger LOGGER = LoggerFactory.getLogger(SolrCommands.class);

  protected static Map<String, String> configurationMap;

  protected static final String NAMESPACE = "solr";

  protected PrintStream console = System.out;

  @Reference protected ConfigurationAdmin configurationAdmin;

  protected static final String DEFAULT_CORE_NAME = "catalog";

  protected static final String SEE_COMMAND_USAGE_MESSAGE =
      "Invalid Argument(s). Please see command usage for details.";

  protected static final String ZOOKEEPER_HOSTS_PROP = "solr.cloud.zookeeper";

  private static final String SOLR_URL_PROP = "solr.http.url";

  private static final String SOLR_DATA_DIR = "solr.data.dir";

  protected static final Path SYSTEM_PROPERTIES_PATH =
      Paths.get(System.getProperty("ddf.etc"), "custom.system.properties");

  protected static final String SOLR_CLIENT_PROP = "solr.client";

  protected static final String CLOUD_SOLR_CLIENT_TYPE = "CloudSolrClient";

  private static final Color ERROR_COLOR = Ansi.Color.RED;

  private static final Color SUCCESS_COLOR = Ansi.Color.GREEN;

  private static final Color INFO_COLOR = Ansi.Color.CYAN;

  protected org.codice.solr.factory.impl.HttpClientBuilder httpBuilder;

  public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
    LOGGER.debug("setConfigurationAdmin: {}", configurationAdmin);
    this.configurationAdmin = configurationAdmin;
  }

  protected void printColor(Color color, String message) {
    String colorString;
    if (color == null || color.equals(Ansi.Color.DEFAULT)) {
      colorString = Ansi.ansi().reset().toString();
    } else {
      colorString = Ansi.ansi().fg(color).toString();
    }
    console.print(colorString);
    console.print(message);
    console.println(Ansi.ansi().reset().toString());
  }

  protected void printSuccessMessage(String message) {
    printColor(SUCCESS_COLOR, message);
  }

  protected void printErrorMessage(String message) {
    printColor(ERROR_COLOR, message);
  }

  protected void printInfoMessage(String message) {
    printColor(INFO_COLOR, message);
  }

  protected SolrClient getCloudSolrClient() {
    String zkHosts = System.getProperty(ZOOKEEPER_HOSTS_PROP);
    String solrUrl = System.getProperty(SOLR_URL_PROP);
    LOGGER.debug("Zookeeper hosts: {}", zkHosts);

    if (StringUtils.isNotBlank(zkHosts)) {
      String[] zkHostArr = zkHosts.split(",");
      List<String> zkHostsList = Arrays.asList(zkHostArr);
      SolrClient client = new CloudSolrClient.Builder(zkHostsList, Optional.empty()).build();
      LOGGER.debug("Created solr client: {}", client);
      return client;
    } else if (StringUtils.isNotBlank(solrUrl)) {
      SolrClient client = new CloudSolrClient.Builder(Collections.singletonList(solrUrl)).build();
      LOGGER.debug("Created solr client: {}", client);
      return client;
    } else {
      throw new IllegalArgumentException(
          String.format(
              "Could not determine Zookeeper Hosts. Please verify that the system property %s is configured in %s.",
              ZOOKEEPER_HOSTS_PROP, SYSTEM_PROPERTIES_PATH));
    }
  }

  protected void shutdown(SolrClient client) throws IOException {
    if (client != null) {
      LOGGER.debug("Closing solr client");
      client.close();
    }
  }

  protected boolean isSystemConfiguredWithSolrCloud() {
    String solrClientType = System.getProperty(SOLR_CLIENT_PROP);
    LOGGER.debug("Solr client type: {}", solrClientType);
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

  protected String getCoreUrl(String coreName) {
    return getBackupUrl() + "/" + coreName;
  }

  protected final String getBackupUrl() {
    return AccessController.doPrivileged(
        (PrivilegedAction<String>) () -> System.getProperty(SOLR_URL_PROP));
  }

  protected static String getSolrDataDir() {
    return AccessController.doPrivileged(
        (PrivilegedAction<String>) () -> System.getProperty(SOLR_DATA_DIR));
  }

  protected String getReplicationUrl(String coreName) {
    return getCoreUrl(coreName) + "/replication";
  }

  protected void optimizeCollection(SolrClient client, String collection)
      throws IOException, SolrServerException {
    LOGGER.debug("Optimization of collection [{}] is in progress.", collection);
    printInfoMessage(String.format("Optimizing of collection [%s] is in progress.", collection));
    UpdateResponse updateResponse = client.optimize(collection);
    LOGGER.debug("Optimization status: {}", updateResponse.getStatus());
    if (updateResponse.getStatus() != 0) {
      throw new SolrServerException(
          String.format("Unable to optimize collection [%s].", collection));
    }
  }

  protected void printStatus(SolrClient client, String requestId) {
    try {
      CollectionAdminRequest.RequestStatusResponse requestStatusResponse =
          CollectionAdminRequest.requestStatus(requestId).process(client);
      RequestStatusState requestStatus = requestStatusResponse.getRequestStatus();
      printInfoMessage(
          String.format("Status for request Id [%s] is [%s].", requestId, requestStatus.getKey()));
      LOGGER.debug("Status: {}", requestStatus.getKey());
      if (requestStatus == RequestStatusState.FAILED) {
        printErrorMessage("Status failed. ");
        printResponseErrorMessages(requestStatusResponse);
      }
    } catch (Exception e) {
      String message = e.getMessage() != null ? e.getMessage() : "Unable to get status.";
      printErrorMessage(String.format("Status failed. %s", message));
      LOGGER.debug("Unable to get status for request id: {}", requestId, e);
    }
  }

  protected void printResponseErrorMessages(CollectionAdminResponse response) {
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

  protected static boolean collectionExists(SolrClient client, String collection)
      throws IOException, SolrServerException {
    CollectionAdminResponse response = new CollectionAdminRequest.List().process(client);

    if (response.getResponse() == null) {
      return false;
    }
    List<String> existingCollections = (List<String>) response.getResponse().get("collections");

    if (CollectionUtils.isNotEmpty(existingCollections)) {
      return existingCollections.contains(collection);
    }
    return false;
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
}
