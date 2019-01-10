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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class SolrCommandTest {
  protected static final String DEFAULT_ZK_HOSTS =
      "zookeeperhost1:2181,zookeeperhost2:2181,zookeeperhost3:2181";

  protected static final String DEFAULT_CONFIGSET = "collection_configset_1";

  protected static final String DEFAULT_CORE_NAME = "catalog";

  protected static final String DDF_HOME_PROP = "ddf.home";

  protected static final String DDF_ETC_PROP = "ddf.etc";

  protected static final String DEFAULT_DDF_HOME = "/opt/ddf";

  @Rule @ClassRule public static TemporaryFolder baseDir = new TemporaryFolder();

  @Rule @ClassRule public static TemporaryFolder backupLocation = new TemporaryFolder();

  protected ConsoleOutput consoleOutput;

  protected static String cipherSuites;

  protected static String protocols;

  protected static MiniSolrCloudCluster miniSolrCloud;

  protected static Path getBaseDirPath() {
    return baseDir.getRoot().toPath();
  }

  protected String getBackupLocation() {
    return backupLocation.getRoot().getPath();
  }

  protected void setupSystemProperties(String solrClientType) {
    setupSolrClientType(solrClientType);
    setupZkHost();
  }

  protected static void setDdfHome() {
    System.setProperty(DDF_HOME_PROP, DEFAULT_DDF_HOME);
  }

  protected static void setDdfEtc() {
    System.setProperty(DDF_ETC_PROP, Paths.get(DEFAULT_DDF_HOME, "etc").toString());
  }

  protected void setupSolrClientType(String solrClientType) {
    System.setProperty(SolrCommands.SOLR_CLIENT_PROP, solrClientType);
  }

  protected void setupZkHost() {
    System.setProperty(SolrCommands.ZOOKEEPER_HOSTS_PROP, DEFAULT_ZK_HOSTS);
  }

  /**
   * See
   * https://cwiki.apache.org/confluence/display/solr/Collections+API#CollectionsAPI-BACKUP:BackupCollection
   * for requests and responses.
   */
  protected NamedList<Object> getResponseHeader(int statusCode) {
    NamedList<Object> responseHeader = new NamedList<>();
    responseHeader.add("status", statusCode);
    responseHeader.add("QTime", 345);
    return responseHeader;
  }

  /**
   * See
   * https://cwiki.apache.org/confluence/display/solr/Collections+API#CollectionsAPI-BACKUP:BackupCollection
   * for requests and responses.
   */
  protected NamedList<Object> getResponseForStatus(
      int statusCode, RequestStatusState requestStatusState, NamedList<String> errorMessages) {
    NamedList<Object> responseHeader = getResponseHeader(statusCode);
    NamedList<String> status = getStatus(requestStatusState);
    NamedList<Object> response = new NamedList<>();
    response.add("status", status);
    response.add("responseHeader", responseHeader);
    if (errorMessages != null) {
      response.add("failure", errorMessages);
    } else {
      response.add("success", new Object());
    }

    return response;
  }

  /**
   * On https://github.com/apache/lucene-solr see
   * CollectionAdminRequest.RequestStatusResponse.getRequestStatus() for example usage.
   * https://github.com/apache/lucene-solr/blob/master/solr/solrj/src/java/org/apache/solr/client/solrj/request/CollectionAdminRequest.java#L1343-L1346
   */
  protected NamedList<String> getStatus(RequestStatusState requestStatusState) {
    NamedList<String> status = new NamedList();
    status.add("state", requestStatusState.getKey());
    return status;
  }

  protected HttpResponse mockResponse(int statusCode, String responseBody) {
    StatusLine mockStatusLine = mock(StatusLine.class);
    doReturn(statusCode).when(mockStatusLine).getStatusCode();
    doReturn(responseBody).when(mockStatusLine).getReasonPhrase();
    HttpResponse mockResponse = mock(HttpResponse.class);
    doReturn(mockStatusLine).when(mockResponse).getStatusLine();
    return mockResponse;
  }

  protected static void createMiniSolrCloudCluster() throws Exception {
    miniSolrCloud =
        new MiniSolrCloudCluster(
            1, getBaseDirPath(), JettyConfig.builder().setContext("/solr").build());
    miniSolrCloud.getSolrClient().connect();
    System.setProperty("solr.cloud.shardCount", "1");
    System.setProperty("solr.cloud.replicationFactor", "1");
    System.setProperty("solr.cloud.maxShardPerNode", "1");
    System.setProperty("solr.cloud.zookeeper.chroot", "/solr");
    System.setProperty(SolrCommands.ZOOKEEPER_HOSTS_PROP, miniSolrCloud.getZkServer().getZkHost());
    // Set soft commit and hard commit times high, so they will not impact the tests.
    System.setProperty("solr.autoSoftCommit.maxTime", String.valueOf(100));
    System.setProperty("solr.autoCommit.maxTime", String.valueOf(100));
  }

  protected static void uploadDefaultConfigset() throws Exception {
    miniSolrCloud.uploadConfigSet(
        new File(BackupCommandTest.class.getClassLoader().getResource("configset").getPath())
            .toPath(),
        DEFAULT_CONFIGSET);
  }

  protected static void createDefaultCollection() throws Exception {
    CollectionAdminRequest.Create create =
        CollectionAdminRequest.createCollection(DEFAULT_CORE_NAME, DEFAULT_CONFIGSET, 1, 1);
    CollectionAdminResponse response = create.process(miniSolrCloud.getSolrClient());
    if (response.getStatus() != 0 || response.getErrorMessages() != null) {
      fail("Could not create collection. Response: " + response.toString());
    }

    List<String> collections =
        CollectionAdminRequest.listCollections(miniSolrCloud.getSolrClient());
    assertThat(collections.size(), is(1));
    miniSolrCloud.getSolrClient().setDefaultCollection(DEFAULT_CORE_NAME);
  }

  protected static void createDefaultMiniSolrCloudCluster() throws Exception {
    createMiniSolrCloudCluster();
    uploadDefaultConfigset();
    createDefaultCollection();
  }

  protected static void addDocument(String uniqueId) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", uniqueId);
    miniSolrCloud.getSolrClient().add(doc);
    miniSolrCloud.getSolrClient().commit();
  }

  protected BackupCommand getBackupCommand(
      String backupLocation,
      String collection,
      boolean asyncBackup,
      boolean asyncBackupStatus,
      String requestId,
      SolrClient solrClient) {
    BackupCommand backupCommand =
        new BackupCommand() {
          @Override
          protected SolrClient getCloudSolrClient() {
            return solrClient;
          }

          // We get the solr client from the MiniSolrCloudCluster, so we don't
          // want to shut it down after each test as there is no way to restart it.
          // We don't create a MiniSolrCloudCluster for each test to reduce the
          // time it takes to run the tests.
          @Override
          protected void shutdown(SolrClient client) {
            // do nothing
          }
        };
    if (backupLocation != null) {
      backupCommand.backupLocation = getBackupLocation();
    }
    if (collection != null) {
      backupCommand.coreName = collection;
    }
    if (asyncBackup) {
      backupCommand.asyncBackup = true;
    }
    if (asyncBackupStatus) {
      backupCommand.asyncBackupStatus = true;
    }
    if (requestId != null) {
      backupCommand.asyncBackupReqId = requestId;
    }
    return backupCommand;
  }

  protected BackupCommand getSynchronousBackupCommand(
      String backupLocation, String collection, SolrClient solrClient) {
    return getBackupCommand(backupLocation, collection, false, false, null, solrClient);
  }

  protected static void cleanupCollections() throws Exception {
    List<String> collections =
        CollectionAdminRequest.listCollections(miniSolrCloud.getSolrClient());
    for (String collection : collections) {
      CollectionAdminRequest.Delete delete = CollectionAdminRequest.deleteCollection(collection);
      delete.process(miniSolrCloud.getSolrClient());
    }
  }
}
