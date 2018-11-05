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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RestoreCommandTest extends SolrCommandTest {

  private static final String DEFAULT_CORE_NAME = "catalog";

  private static final String DEFAULT_CONFIGSET = "collection_configset_1";

  private static final String SOLR_STANDALONE_TYPE = "SolrStandalone";

  private static final Pattern ASCII_COLOR_CODES_REGEX = Pattern.compile("\u001B\\[[;\\d]*m");

  private static final long TIMEOUT_IN_MINUTES = 1;

  private static final int SUCCESS_STATUS_CODE = 0;

  private static final int FAILURE_STATUS_CODE = 500;

  private File backupFile;

  @BeforeClass
  public static void setupClass() throws Exception {
    setDdfHome();
    setDdfEtc();
    createDefaultMiniSolrCloudCluster();
  }

  @Before
  public void setUp() throws Exception {
    cipherSuites = System.getProperty("https.cipherSuites");
    System.setProperty(
        "https.cipherSuites",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA");
    protocols = System.getProperty("https.protocols");
    System.setProperty("https.protocols", "TLSv1.1, TLSv1.2");
    consoleOutput = new ConsoleOutput();
    consoleOutput.interceptSystemOut();

    mockHttpWrapper = mock(HttpWrapper.class);
    createDefaultCollection();
    addDocument("1");
    backupSolr(miniSolrCloud.getSolrClient());
  }

  @After
  public void tearDown() {
    consoleOutput.resetSystemOut();

    System.clearProperty(SolrCommands.SOLR_CLIENT_PROP);
    System.clearProperty(SolrCommands.ZOOKEEPER_HOSTS_PROP);

    if (cipherSuites != null) {
      System.setProperty("https.cipherSuites", cipherSuites);
    } else {
      System.clearProperty("https.cipherSuites");
    }
    if (protocols != null) {
      System.setProperty("https.protocols", protocols);
    } else {
      System.clearProperty("https.protocols");
    }
  }

  @AfterClass
  public static void tearDownClass() throws Exception {
    if (miniSolrCloud != null) {
      miniSolrCloud.getSolrClient().close();
      miniSolrCloud.shutdown();
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoArgRestore() throws Exception {
    BackupCommand backupCommand =
        new BackupCommand() {
          @Override
          protected HttpWrapper getHttpClient() {
            return mockHttpWrapper;
          }
        };
    backupCommand.execute();

    setupSystemProperties(SOLR_STANDALONE_TYPE);

    when(mockHttpWrapper.execute(any(URI.class))).thenReturn(mockResponse(HttpStatus.SC_OK, ""));

    RestoreCommand restoreCommand =
        new RestoreCommand() {
          @Override
          protected HttpWrapper getHttpClient() {
            return mockHttpWrapper;
          }
        };
    restoreCommand.execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSingleNodeRestoreAsyncOptionSupplied() throws Exception {
    when(mockHttpWrapper.execute(any(URI.class))).thenReturn(mockResponse(HttpStatus.SC_OK, ""));

    setupSystemProperties(SOLR_STANDALONE_TYPE);

    RestoreCommand restoreCommand =
        new RestoreCommand() {
          @Override
          protected HttpWrapper getHttpClient() {
            return mockHttpWrapper;
          }
        };
    restoreCommand.asyncRestore = true;

    restoreCommand.execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSystemPropertiesNotSet() throws Exception {
    RestoreCommand restoreCommand = new RestoreCommand();
    restoreCommand.execute();
  }

  @Test
  public void testPerformSolrCloudSynchronousRestore() throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getSynchronousRestoreCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    restoreCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Restoring collection [%s] from [%s] / [%s",
                DEFAULT_CORE_NAME, restoreCommand.backupLocation, backupFile.getName())));
    assertThat(consoleOutput.getOutput(), containsString("Restore complete."));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPerformSolrCloudSynchronousRestoreNoOptions() throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getSynchronousRestoreCommand(null, null, miniSolrCloud.getSolrClient());
    restoreCommand.execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPerformSolrCloudSynchronousRestoreNoBackupLocation() throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getSynchronousRestoreCommand(null, DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    restoreCommand.execute();
  }

  @Test
  public void testPerformSolrCloudSynchronousRestoreNoCollection() throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getSynchronousRestoreCommand(getBackupLocation(), null, miniSolrCloud.getSolrClient());
    restoreCommand.execute();

    String backupName = getBackupName(consoleOutput.getOutput());
    File backupFile =
        Paths.get(restoreCommand.backupLocation, backupName).toAbsolutePath().toFile();
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Restoring collection [%s] from [%s] / [%s",
                DEFAULT_CORE_NAME, restoreCommand.backupLocation, backupName)));
    assertThat(consoleOutput.getOutput(), containsString("Restore complete."));
    assertThat(backupFile.exists(), is(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPerformSolrCloudAsynchronousRestoreWithAsyncStatusOptionsSupplied()
      throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getRestoreCommand(
            getBackupLocation(),
            DEFAULT_CORE_NAME,
            true,
            true,
            "myRequestId1",
            miniSolrCloud.getSolrClient());
    restoreCommand.execute();
  }

  @Test
  public void testPerformSolrCloudAsynchronousRestore() throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getAsnychronousRestoreCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    restoreCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Restoring collection [%s] from [%s] / [%s_",
                DEFAULT_CORE_NAME, restoreCommand.backupLocation, DEFAULT_CORE_NAME)));
    assertThat(consoleOutput.getOutput(), containsString("Restore request Id:"));
  }

  @Test
  public void testPerformSolrCloudAsynchronousRestoreStatus() throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getAsnychronousRestoreCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    restoreCommand.execute();

    String requestId = getRequestId(consoleOutput.getOutput());
    RestoreCommand statusRestoreCommand =
        getStatusRestoreCommand(requestId, miniSolrCloud.getSolrClient());
    consoleOutput.reset();
    statusRestoreCommand.execute();
    String status = waitForCompletedStatusOrFail(statusRestoreCommand, consoleOutput);

    assertThat(status, is(RequestStatusState.COMPLETED.getKey()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetSolrCloudAsynchronousRestoreStatusNoRequestId() throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand statusRestoreCommand =
        getStatusRestoreCommand(null, miniSolrCloud.getSolrClient());
    statusRestoreCommand.execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetSolrCloudAsynchronousRestoreStatusNoStatusOption() throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand invalidRestoreStatusCommand =
        getRestoreCommand(null, null, false, false, "myRequestId0", miniSolrCloud.getSolrClient());
    invalidRestoreStatusCommand.execute();
  }

  /**
   * Verify that restore failure messages are printed to the console. In this test, the collection
   * optimization succeeds but the restore fails.
   */
  @Test
  public void testSolrCloudRestoreFailsWithErrorMessages() throws Exception {
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    setupMockSolrClientForRestoreFailure(DEFAULT_CORE_NAME, getErrorMessages(2));
    RestoreCommand restoreCommand =
        getSynchronousRestoreCommand(getBackupLocation(), DEFAULT_CORE_NAME, mockSolrClient);
    restoreCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Restoring collection [%s] from [%s] / [%s_",
                DEFAULT_CORE_NAME, restoreCommand.backupLocation, DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format("Optimizing of collection [%s] is in progress.", DEFAULT_CORE_NAME)));
    assertThat(consoleOutput.getOutput(), containsString("Restore failed."));
    assertThat(
        consoleOutput.getOutput(),
        containsString("1. Error Name: error name 1; Error Value: error value 1"));
    assertThat(
        consoleOutput.getOutput(),
        containsString("2. Error Name: error name 2; Error Value: error value 2"));
  }

  @Test
  public void testSolrCloudRestoreStatusRequestFailsWithErrorMessages() throws Exception {
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getAsnychronousRestoreCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    restoreCommand.execute();

    String requestId = getRequestId(consoleOutput.getOutput());
    setupMockSolrClientForRestoreStatusFailure(getErrorMessages(1));
    RestoreCommand restoreStatusCommand = getStatusRestoreCommand(requestId, mockSolrClient);
    restoreStatusCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Restoring collection [%s] from [%s] / [%s_",
                DEFAULT_CORE_NAME, restoreCommand.backupLocation, DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format("Optimizing of collection [%s] is in progress.", DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Status for request Id [%s] is [%s].",
                requestId, RequestStatusState.FAILED.getKey())));
    assertThat(
        consoleOutput.getOutput(),
        containsString("1. Error Name: error name 1; Error Value: error value 1"));
  }

  @Test
  public void testSolrCloudRestoreStatusRequestThrowsException() throws Exception {
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    RestoreCommand restoreCommand =
        getAsnychronousRestoreCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    restoreCommand.execute();

    String requestId = getRequestId(consoleOutput.getOutput());
    setupMockSolrClientForStatusThrowsException();
    RestoreCommand restoreStatusCommand = getStatusRestoreCommand(requestId, mockSolrClient);
    restoreStatusCommand.execute();

    assertThat(consoleOutput.getOutput(), containsString("Status failed."));
  }

  private NamedList<String> getErrorMessages(int numberOfMessages) {
    NamedList<String> errorMessages = new NamedList<>();
    for (int i = 0; i < numberOfMessages; i++) {
      errorMessages.add("error name " + (i + 1), "error value " + (i + 1));
    }
    return errorMessages;
  }

  private void setupMockSolrClientForRestoreFailure(
      String collection, NamedList<String> restoreErrorMessages) throws Exception {
    setupMockSolrClientForRestore(
        collection, SUCCESS_STATUS_CODE, FAILURE_STATUS_CODE, restoreErrorMessages);
  }

  /**
   * See
   * https://cwiki.apache.org/confluence/display/solr/Collections+API#CollectionsAPI-BACKUP:BackupCollection
   * for requests and responses.
   */
  private void setupMockSolrClientForRestore(
      String collection,
      int optimizationStatusCode,
      int restoreStatusCode,
      NamedList<String> restoreErrorMessages)
      throws Exception {

    UpdateResponse optimizationResponse = getMockOptimizationResponse(optimizationStatusCode);
    when(mockSolrClient.optimize(eq(collection))).thenReturn(optimizationResponse);

    NamedList<Object> responseHeader = getResponseHeader(restoreStatusCode);

    NamedList<Object> mockResponse = new NamedList<>();
    mockResponse.add("responseHeader", responseHeader);
    if (restoreErrorMessages != null) {
      mockResponse.add("failure", restoreErrorMessages);
    } else {
      mockResponse.add("success", new Object());
    }

    if (collection != null) {
      when(mockSolrClient.request(any(SolrRequest.class), eq(collection))).thenReturn(mockResponse);
    }
  }

  private void setupMockSolrClientForRestoreStatusFailure(NamedList<String> restoreErrorMessages)
      throws Exception {
    NamedList<Object> response =
        getResponseForStatus(FAILURE_STATUS_CODE, RequestStatusState.FAILED, restoreErrorMessages);
    when(mockSolrClient.request(any(SolrRequest.class), isNull(String.class))).thenReturn(response);
  }

  private void setupMockSolrClientForStatusThrowsException() throws Exception {
    when(mockSolrClient.request(any(SolrRequest.class), isNull(String.class)))
        .thenThrow(SolrServerException.class);
  }

  private UpdateResponse getMockOptimizationResponse(int status) {
    UpdateResponse mockOptimizationResponse = mock(UpdateResponse.class);
    when(mockOptimizationResponse.getStatus()).thenReturn(status);
    return mockOptimizationResponse;
  }

  private ResponseWrapper mockResponse(int statusCode, String responseBody) {
    return new ResponseWrapper(prepareResponse(statusCode, responseBody));
  }

  private HttpResponse prepareResponse(int statusCode, String responseBody) {
    HttpResponse httpResponse =
        new BasicHttpResponse(
            new BasicStatusLine(new ProtocolVersion("HTTP", 1, 1), statusCode, ""));
    httpResponse.setStatusCode(statusCode);
    try {
      httpResponse.setEntity(new StringEntity(responseBody));
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException(e);
    }

    return httpResponse;
  }

  private static void createDefaultMiniSolrCloudCluster() throws Exception {
    createMiniSolrCloudCluster();
    uploadDefaultConfigset();
  }

  private static void createMiniSolrCloudCluster() throws Exception {
    miniSolrCloud =
        new MiniSolrCloudCluster(
            1, getBaseDirPath(), JettyConfig.builder().setContext("/solr").build());
    miniSolrCloud.getSolrClient().connect();
  }

  private static void uploadDefaultConfigset() throws Exception {
    miniSolrCloud.uploadConfigSet(
        new File(RestoreCommandTest.class.getClassLoader().getResource("configset").getPath())
            .toPath(),
        DEFAULT_CONFIGSET);
  }

  private static void createDefaultCollection() throws Exception {
    cleanupCollections();
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

  private static void cleanupCollections() throws Exception {
    List<String> collections =
        CollectionAdminRequest.listCollections(miniSolrCloud.getSolrClient());
    for (String collection : collections) {
      CollectionAdminRequest.Delete delete = CollectionAdminRequest.deleteCollection(collection);
      delete.process(miniSolrCloud.getSolrClient());
    }
  }

  private static void addDocument(String uniqueId) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", uniqueId);
    miniSolrCloud.getSolrClient().add(doc);
    miniSolrCloud.getSolrClient().commit();
  }

  private RestoreCommand getSynchronousRestoreCommand(
      String backupLocation, String collection, SolrClient solrClient) {
    return getRestoreCommand(backupLocation, collection, false, false, null, solrClient);
  }

  private RestoreCommand getAsnychronousRestoreCommand(
      String backupLocation, String collection, SolrClient solrClient) {
    return getRestoreCommand(backupLocation, collection, true, false, null, solrClient);
  }

  private RestoreCommand getStatusRestoreCommand(String requestId, SolrClient solrClient) {
    return getRestoreCommand(null, null, false, true, requestId, solrClient);
  }

  private RestoreCommand getRestoreCommand(
      String backupLocation,
      String collection,
      boolean asyncRestore,
      boolean status,
      String requestId,
      SolrClient solrClient) {
    RestoreCommand restoreCommand =
        new RestoreCommand() {
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
    restoreCommand.force = true;
    if (backupLocation != null) {
      restoreCommand.backupLocation = getBackupLocation();
    }
    if (collection != null) {
      restoreCommand.coreName = collection;
    }
    if (asyncRestore) {
      restoreCommand.asyncRestore = true;
    }
    if (status) {
      restoreCommand.status = true;
    }
    if (requestId != null) {
      restoreCommand.requestId = requestId;
    }
    if (backupFile != null && backupFile.exists()) {
      restoreCommand.backupName = backupFile.getName();
    }
    return restoreCommand;
  }

  // Replace ASCII color codes in console output and get the request Id
  private String getRequestId(String consoleOutput) {
    return StringUtils.trim(
        StringUtils.substringAfterLast(
            ASCII_COLOR_CODES_REGEX.matcher(consoleOutput).replaceAll(""), ":"));
  }

  // Replace ASCII color codes in console output and get the status
  private String getRequestStatus(String consoleOutput) {
    return StringUtils.trim(
        StringUtils.substringsBetween(
            ASCII_COLOR_CODES_REGEX.matcher(consoleOutput).replaceAll(""), "[", "]")[1]);
  }

  private String getBackupName(String consoleOutput) {
    return StringUtils.trim(
        StringUtils.substringsBetween(
            ASCII_COLOR_CODES_REGEX.matcher(consoleOutput).replaceAll(""), "[", "]")[2]);
  }

  private String waitForCompletedStatusOrFail(
      RestoreCommand statusRestoreCommand, ConsoleOutput consoleOutput) throws Exception {
    long startTime = System.currentTimeMillis();
    long endTime = startTime + TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES);
    String status = getRequestStatus(consoleOutput.getOutput());

    while (!StringUtils.equals(status, RequestStatusState.COMPLETED.getKey())) {
      if (System.currentTimeMillis() >= endTime) {
        fail(
            String.format(
                "The restore status command did not complete within %s minute(s). Current restore status: %s.",
                TIMEOUT_IN_MINUTES, status));
      }
      TimeUnit.SECONDS.sleep(1);
      consoleOutput.reset();
      statusRestoreCommand.execute();
      status = getRequestStatus(consoleOutput.getOutput());
    }

    return status;
  }

  private void backupSolr(SolrClient solrClient) throws Exception {
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);
    BackupCommand backupCommand =
        new BackupCommand() {
          @Override
          protected SolrClient getCloudSolrClient() {
            return solrClient;
          }

          @Override
          protected void shutdown(SolrClient client) {
            // do nothing
          }
        };
    backupCommand.backupLocation = getBackupLocation();
    backupCommand.asyncBackup = false;
    backupCommand.asyncBackupStatus = false;
    backupCommand.coreName = DEFAULT_CORE_NAME;
    backupCommand.execute();
    String backupName = getBackupName(consoleOutput.getOutput());
    backupFile = Paths.get(backupCommand.backupLocation, backupName).toAbsolutePath().toFile();
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s",
                DEFAULT_CORE_NAME, backupCommand.backupLocation, backupName)));
    assertThat(consoleOutput.getOutput(), containsString("Backup complete."));
    assertThat(backupFile.exists(), is(true));
  }
}
