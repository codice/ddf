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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BackupCommandTest {

  private static final String DEFAULT_CORE_NAME = "catalog";

  private static final String SOLR_CLOUD_CLIENT_TYPE = "CloudSolrClient";

  private static final String SOLR_CLIENT_PROP = "solr.client";

  private static final String INVALID_COLLECTION_NAME = "myInvalidCollection";

  private static final String DEFAULT_CONFIGSET = "collection_configset_1";

  private static final Pattern ASCII_COLOR_CODES_REGEX = Pattern.compile("\u001B\\[[;\\d]*m");

  private static final long TIMEOUT_IN_MINUTES = 1;

  private static final String ZOOKEEPER_HOSTS_PROP = "solr.cloud.zookeeper";

  private static final String DEFAULT_ZK_HOSTS =
      "zookeeperhost1:2181,zookeeperhost2:2181,zookeeperhost3:2181";

  private static final String DDF_HOME_PROP = "ddf.home";

  private static final String DDF_ETC_PROP = "ddf.etc";

  private static final String DEFAULT_DDF_HOME = "/opt/ddf";

  private static final Path SYSTEM_PROPERTIES_PATH =
      Paths.get(DEFAULT_DDF_HOME, "etc", "custom.system.properties");

  private static final String SEE_COMMAND_USAGE_MESSAGE =
      "Invalid Argument(s). Please see command usage for details.";

  private static final int SUCCESS_STATUS_CODE = 0;

  private static final int FAILURE_STATUS_CODE = 500;

  HttpWrapper mockHttpWrapper;

  ConsoleOutput consoleOutput;

  private static MiniSolrCloudCluster miniSolrCloud;

  private static String cipherSuites;

  private static String protocols;

  @Mock SolrClient mockSolrClient;

  @Rule @ClassRule public static TemporaryFolder baseDir = new TemporaryFolder();

  @Rule @ClassRule public static TemporaryFolder backupLocation = new TemporaryFolder();

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @BeforeClass
  public static void beforeClass() throws Exception {
    setDdfHome();
    setDdfEtc();
    createDefaultMiniSolrCloudCluster();
    addDocument("1");
  }

  @Before
  public void before() throws Exception {
    cipherSuites = System.getProperty("https.cipherSuites");
    System.setProperty(
        "https.cipherSuites",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA");
    protocols = System.getProperty("https.protocols");
    System.setProperty("https.protocols", "TLSv1.1, TLSv1.2");
    consoleOutput = new ConsoleOutput();
    consoleOutput.interceptSystemOut();

    mockHttpWrapper = mock(HttpWrapper.class);
  }

  @After
  public void after() {
    consoleOutput.resetSystemOut();

    System.clearProperty(SOLR_CLIENT_PROP);
    System.clearProperty(ZOOKEEPER_HOSTS_PROP);

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
  public static void afterClass() throws Exception {
    if (miniSolrCloud != null) {
      miniSolrCloud.getSolrClient().close();
      miniSolrCloud.shutdown();
    }
  }

  @Test
  public void testNoArgBackup() throws Exception {

    when(mockHttpWrapper.execute(any(URI.class))).thenReturn(mockResponse(HttpStatus.SC_OK, ""));

    BackupCommand backupCommand =
        new BackupCommand() {
          @Override
          protected HttpWrapper getHttpClient() {
            return mockHttpWrapper;
          }
        };
    backupCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format("Backup of [%s] complete.", DEFAULT_CORE_NAME)));
  }

  @Test
  public void testBackupSpecificCore() throws Exception {
    final String coreName = "core";

    when(mockHttpWrapper.execute(any(URI.class))).thenReturn(mockResponse(HttpStatus.SC_OK, ""));

    BackupCommand backupCommand =
        new BackupCommand() {
          @Override
          protected HttpWrapper getHttpClient() {
            return mockHttpWrapper;
          }
        };

    backupCommand.coreName = coreName;
    backupCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format("Backup of [%s] complete.", coreName)));
  }

  @Test
  public void testBackupInvalidCore() throws Exception {
    final String coreName = "badCoreName";

    when(mockHttpWrapper.execute(any(URI.class)))
        .thenReturn(mockResponse(HttpStatus.SC_NOT_FOUND, ""));

    BackupCommand backupCommand =
        new BackupCommand() {
          @Override
          protected HttpWrapper getHttpClient() {
            return mockHttpWrapper;
          }
        };

    backupCommand.coreName = coreName;
    backupCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format("Backup command failed due to: %d", HttpStatus.SC_NOT_FOUND)));
  }

  @Test
  public void testSingleNodeBackupAsyncOptionSupplied() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    when(mockHttpWrapper.execute(any(URI.class))).thenReturn(mockResponse(HttpStatus.SC_OK, ""));

    BackupCommand backupCommand =
        new BackupCommand() {
          @Override
          protected HttpWrapper getHttpClient() {
            return mockHttpWrapper;
          }
        };
    backupCommand.asyncBackup = true;

    backupCommand.execute();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSystemPropertiesNotSet() throws Exception {

    BackupCommand backupCommand = new BackupCommand();
    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudSynchronousBackup() throws Exception {

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getSynchronousBackupCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());

    // Perform Test
    backupCommand.execute();

    // Verify
    String backupName = getBackupName(consoleOutput.getOutput());
    File backupFile = Paths.get(backupCommand.backupLocation, backupName).toAbsolutePath().toFile();
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s",
                DEFAULT_CORE_NAME, backupCommand.backupLocation, backupName)));
    assertThat(consoleOutput.getOutput(), containsString("Backup complete."));
    assertThat(backupFile.exists(), is(true));
  }

  @Test
  public void testPerformSolrCloudSynchronousBackupNoOptions() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getSynchronousBackupCommand(null, null, miniSolrCloud.getSolrClient());

    // Perform Test
    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudSynchronousBackupNoBackupLocation() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getSynchronousBackupCommand(null, DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());

    // Perform Test
    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudSynchronousBackupNoCollection() throws Exception {

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getSynchronousBackupCommand(getBackupLocation(), null, miniSolrCloud.getSolrClient());

    // Perform Test
    backupCommand.execute();

    // Verify
    String backupName = getBackupName(consoleOutput.getOutput());
    File backupFile = Paths.get(backupCommand.backupLocation, backupName).toAbsolutePath().toFile();
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s",
                DEFAULT_CORE_NAME, backupCommand.backupLocation, backupName)));
    assertThat(consoleOutput.getOutput(), containsString("Backup complete."));
    assertThat(backupFile.exists(), is(true));
  }

  @Test
  public void testPerformSolrCloudSynchronousBackupInvalidCollectionName() throws Exception {

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getSynchronousBackupCommand(
            getBackupLocation(), INVALID_COLLECTION_NAME, miniSolrCloud.getSolrClient());

    // Perform Test
    backupCommand.execute();

    // Verify
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                INVALID_COLLECTION_NAME, backupCommand.backupLocation, INVALID_COLLECTION_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Optimizing of collection [%s] is in progress.", INVALID_COLLECTION_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format("Backup failed. Collection not found: %s", INVALID_COLLECTION_NAME)));
  }

  @Test
  public void testPerformSolrCloudSynchronousBackupNumberToKeepSupplied() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getSynchronousBackupCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    backupCommand.numberToKeep = 3;

    // Perform Test
    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudAsynchronousBackupWithAsyncStatusOptionsSupplied()
      throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getBackupCommand(
            getBackupLocation(),
            DEFAULT_CORE_NAME,
            true,
            true,
            "myRequestId1",
            miniSolrCloud.getSolrClient());

    // Perform Test
    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudAsynchronousBackupNumberToKeepSupplied() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getAsnychronousBackupCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    backupCommand.numberToKeep = 3;

    // Perform Test
    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudAsynchronousBackup() throws Exception {

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand for async backup
    BackupCommand backupCommand =
        getAsnychronousBackupCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());

    // Perform Test (backup)
    backupCommand.execute();

    // Verify
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                DEFAULT_CORE_NAME, backupCommand.backupLocation, DEFAULT_CORE_NAME)));
    assertThat(consoleOutput.getOutput(), containsString("Solr Cloud backup request Id:"));
  }

  @Test
  public void testPerformSolrCloudAsynchronousBackupStatus() throws Exception {

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand for async backup
    BackupCommand backupCommand =
        getAsnychronousBackupCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());

    // Perform Test (backup)
    backupCommand.execute();

    String requestId = getRequestId(consoleOutput.getOutput());

    // Setup BackupCommand for status lookup
    BackupCommand statusBackupCommand =
        getStatusBackupCommand(requestId, miniSolrCloud.getSolrClient());

    consoleOutput.reset();

    // Perform status lookup
    statusBackupCommand.execute();

    String status = waitForCompletedStatusOrFail(statusBackupCommand, consoleOutput);

    assertThat(status, is(RequestStatusState.COMPLETED.getKey()));
  }

  @Test
  public void testGetSolrCloudAsynchronousBackupStatusNoRequestId() throws Exception {
    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand for status lookup
    BackupCommand statusBackupCommand = getStatusBackupCommand(null, miniSolrCloud.getSolrClient());

    statusBackupCommand.execute();
  }

  @Test
  public void testGetSolrCloudAsynchronousBackupStatusNoStatusOption() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand (ie. solr:backup -i <request Id>)
    BackupCommand invalidBackupStatusCommand =
        getBackupCommand(null, null, false, false, "myRequestId0", miniSolrCloud.getSolrClient());

    invalidBackupStatusCommand.execute();
  }

  @Test
  public void testGetCloudSolrClientNoZkHosts() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(
        String.format(
            "Could not determine Zookeeper Hosts. Please verify that the system property %s is configured in %s.",
            ZOOKEEPER_HOSTS_PROP, SYSTEM_PROPERTIES_PATH));

    // Set the solr client type system property so that the
    // BackupCommand knows that it needs to backup solr cloud.
    setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand = new BackupCommand();
    backupCommand.backupLocation = getBackupLocation();

    backupCommand.execute();
  }

  /**
   * Verify that backup failure messages are printed to the console. In this test, the colleciton
   * optimization succeeds but the backup fails.
   */
  @Test
  public void testSolrCloudBackupFailsWithErrorMessages() throws Exception {

    // Set the solr client type system property so that the
    // BackupCommand knows that it needs to backup solr cloud.
    setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

    setupMockSolrClientForBackupFailure(DEFAULT_CORE_NAME, getErrorMessages(2));

    BackupCommand backupCommand =
        getSynchronousBackupCommand(getBackupLocation(), DEFAULT_CORE_NAME, mockSolrClient);

    backupCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                DEFAULT_CORE_NAME, backupCommand.backupLocation, DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format("Optimizing of collection [%s] is in progress.", DEFAULT_CORE_NAME)));
    assertThat(consoleOutput.getOutput(), containsString("Backup failed."));
    assertThat(
        consoleOutput.getOutput(),
        containsString("1. Error Name: error name 1; Error Value: error value 1"));
    assertThat(
        consoleOutput.getOutput(),
        containsString("2. Error Name: error name 2; Error Value: error value 2"));
  }

  /** Verify that backup status failure messages are printed to the console. */
  @Test
  public void testSolrCloudBackupStatusRequestFailsWithErrorMessages() throws Exception {

    // Set the solr client type system property so that the
    // BackupCommand knows that it needs to backup solr cloud.
    setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

    BackupCommand backupCommand =
        getAsnychronousBackupCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());

    // Perform async backup
    backupCommand.execute();

    // Get requestId so that we can request backup status
    String requestId = getRequestId(consoleOutput.getOutput());

    setupMockSolrClientForBackupStatusFailure(getErrorMessages(1));
    BackupCommand backupStatusCommand = getStatusBackupCommand(requestId, mockSolrClient);

    // Perform backup status request
    backupStatusCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                DEFAULT_CORE_NAME, backupCommand.backupLocation, DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format("Optimizing of collection [%s] is in progress.", DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backup status for request Id [%s] is [%s].",
                requestId, RequestStatusState.FAILED.getKey())));
    assertThat(consoleOutput.getOutput(), containsString("Backup status failed."));
    assertThat(
        consoleOutput.getOutput(),
        containsString("1. Error Name: error name 1; Error Value: error value 1"));
  }

  @Test
  public void testSolrCloudBackupStatusRequestThrowsException() throws Exception {

    // Set the solr client type system property so that the
    // BackupCommand knows that it needs to backup solr cloud.
    setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

    BackupCommand backupCommand =
        getAsnychronousBackupCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());

    // Perform async backup
    backupCommand.execute();

    // Get requestId so that we can request backup status
    String requestId = getRequestId(consoleOutput.getOutput());

    setupMockSolrClientForStatusThrowsException();
    BackupCommand backupStatusCommand = getStatusBackupCommand(requestId, mockSolrClient);

    // Perform backup status request
    backupStatusCommand.execute();

    assertThat(consoleOutput.getOutput(), containsString("Backup status failed."));
  }

  /**
   * Collections are optimized before backups. This test verifies that an error message is printed
   * to the console when optimization of a collection fails.
   */
  @Test
  public void testSolrCloudBackupFailsDuringOptimizationWithErrorCode() throws Exception {

    // Set the solr client type system property so that the
    // BackupCommand knows that it needs to backup solr cloud.
    setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

    setupMockSolrClientForCollectionOptimization(DEFAULT_CORE_NAME, FAILURE_STATUS_CODE);

    BackupCommand backupCommand =
        getSynchronousBackupCommand(getBackupLocation(), DEFAULT_CORE_NAME, mockSolrClient);

    // Peform sync backup
    backupCommand.execute();

    verify(mockSolrClient).optimize(DEFAULT_CORE_NAME);

    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                DEFAULT_CORE_NAME, backupCommand.backupLocation, DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format("Optimizing of collection [%s] is in progress.", DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format("Backup failed. Unable to optimize collection [%s]", DEFAULT_CORE_NAME)));
  }

  /**
   * Collections are optimized before backups. This test verifies that an error message is printed
   * to the console when optimization of a collection throws an exception.
   */
  @Test
  public void testSolrCloudBackupFailsDuringOptimizationThrowsException() throws Exception {
    setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

    setupMockSolrClientForCollectionOptimizationThrowsException(DEFAULT_CORE_NAME);

    BackupCommand backupCommand =
        getSynchronousBackupCommand(getBackupLocation(), DEFAULT_CORE_NAME, mockSolrClient);

    backupCommand.execute();

    verify(mockSolrClient).optimize(DEFAULT_CORE_NAME);

    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format(
                "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                DEFAULT_CORE_NAME, backupCommand.backupLocation, DEFAULT_CORE_NAME)));
    assertThat(
        consoleOutput.getOutput(),
        containsString(
            String.format("Optimizing of collection [%s] is in progress.", DEFAULT_CORE_NAME)));
    assertThat(consoleOutput.getOutput(), containsString("Backup failed."));
  }

  private NamedList<String> getErrorMessages(int numberOfMessages) {
    NamedList<String> errorMessages = new NamedList<>();
    for (int i = 0; i < numberOfMessages; i++) {
      errorMessages.add("error name " + (i + 1), "error value " + (i + 1));
    }
    return errorMessages;
  }

  private void setupMockSolrClientForBackupFailure(
      String collection, NamedList<String> backupErrorMessages) throws Exception {
    setupMockSolrClientForBackup(
        collection, SUCCESS_STATUS_CODE, FAILURE_STATUS_CODE, backupErrorMessages);
  }

  /**
   * See
   * https://cwiki.apache.org/confluence/display/solr/Collections+API#CollectionsAPI-BACKUP:BackupCollection
   * for requests and responses.
   */
  private NamedList<Object> getResponseHeader(int statusCode) {
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
  private void setupMockSolrClientForBackup(
      String collection,
      int optimizationStatusCode,
      int backupStatusCode,
      NamedList<String> backupErrorMessages)
      throws Exception {

    UpdateResponse optimizationResponse = getMockOptimizationResponse(optimizationStatusCode);
    when(mockSolrClient.optimize(eq(collection))).thenReturn(optimizationResponse);

    NamedList<Object> responseHeader = getResponseHeader(backupStatusCode);

    NamedList<Object> mockResponse = new NamedList<>();
    mockResponse.add("responseHeader", responseHeader);
    if (backupErrorMessages != null) {
      mockResponse.add("failure", backupErrorMessages);
    } else {
      mockResponse.add("success", new Object());
    }

    if (collection != null) {
      when(mockSolrClient.request(any(SolrRequest.class), eq(collection))).thenReturn(mockResponse);
    }
  }

  private void setupMockSolrClientForCollectionOptimization(
      String collection, int optimizationStatusCode) throws Exception {
    UpdateResponse optimizationResponse = getMockOptimizationResponse(optimizationStatusCode);
    when(mockSolrClient.optimize(eq(collection))).thenReturn(optimizationResponse);
  }

  private void setupMockSolrClientForCollectionOptimizationThrowsException(String collection)
      throws Exception {
    when(mockSolrClient.optimize(eq(collection))).thenThrow(SolrServerException.class);
  }

  private void setupMockSolrClientForBackupStatusFailure(NamedList<String> backupErrorMessages)
      throws Exception {
    NamedList<Object> response =
        getResponseForBackupStatus(
            FAILURE_STATUS_CODE, RequestStatusState.FAILED, backupErrorMessages);
    when(mockSolrClient.request(any(SolrRequest.class), isNull(String.class))).thenReturn(response);
  }

  private void setupMockSolrClientForStatusThrowsException() throws Exception {
    when(mockSolrClient.request(any(SolrRequest.class), isNull(String.class)))
        .thenThrow(SolrServerException.class);
  }

  /**
   * See
   * https://cwiki.apache.org/confluence/display/solr/Collections+API#CollectionsAPI-BACKUP:BackupCollection
   * for requests and responses.
   */
  private NamedList<Object> getResponseForBackupStatus(
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
  private NamedList<String> getStatus(RequestStatusState requestStatusState) {
    NamedList<String> status = new NamedList();
    if (requestStatusState == RequestStatusState.FAILED) {
      status.add("state", RequestStatusState.FAILED.getKey());
    } else if (requestStatusState == RequestStatusState.COMPLETED) {
      status.add("state", RequestStatusState.COMPLETED.getKey());
    } else if (requestStatusState == RequestStatusState.NOT_FOUND) {
      status.add("state", RequestStatusState.NOT_FOUND.getKey());
    } else if (requestStatusState == RequestStatusState.RUNNING) {
      status.add("state", RequestStatusState.RUNNING.getKey());
    } else if (requestStatusState == RequestStatusState.SUBMITTED) {
      status.add("state", RequestStatusState.SUBMITTED.getKey());
    }
    return status;
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
    createDefaultCollection();
  }

  private static void createMiniSolrCloudCluster() throws Exception {
    miniSolrCloud =
        new MiniSolrCloudCluster(
            1, getBaseDirPath(), JettyConfig.builder().setContext("/solr").build());
    miniSolrCloud.getSolrClient().connect();
  }

  private static void uploadDefaultConfigset() throws Exception {
    miniSolrCloud.uploadConfigSet(
        new File(BackupCommandTest.class.getClassLoader().getResource("configset").getPath())
            .toPath(),
        DEFAULT_CONFIGSET);
  }

  private static void createDefaultCollection() throws Exception {
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

  private static void addDocument(String uniqueId) throws Exception {
    SolrInputDocument doc = new SolrInputDocument();
    doc.setField("id", uniqueId);
    miniSolrCloud.getSolrClient().add(doc);
    miniSolrCloud.getSolrClient().commit();
  }

  private BackupCommand getSynchronousBackupCommand(
      String backupLocation, String collection, SolrClient solrClient) {
    return getBackupCommand(backupLocation, collection, false, false, null, solrClient);
  }

  private BackupCommand getAsnychronousBackupCommand(
      String backupLocation, String collection, SolrClient solrClient) {
    return getBackupCommand(backupLocation, collection, true, false, null, solrClient);
  }

  private BackupCommand getStatusBackupCommand(String requestId, SolrClient solrClient) {
    return getBackupCommand(null, null, false, true, requestId, solrClient);
  }

  private BackupCommand getBackupCommand(
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
      BackupCommand statusBackupCommand, ConsoleOutput consoleOutput) throws Exception {
    long startTime = System.currentTimeMillis();
    long endTime = startTime + TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES);
    String status = getRequestStatus(consoleOutput.getOutput());

    while (!StringUtils.equals(status, RequestStatusState.COMPLETED.getKey())) {
      if (System.currentTimeMillis() >= endTime) {
        fail(
            String.format(
                "The backup status command did not complete within %s minute(s). Current backup status: %s.",
                TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES), status));
      }
      TimeUnit.SECONDS.sleep(1);
      consoleOutput.reset();
      statusBackupCommand.execute();
      status = getRequestStatus(consoleOutput.getOutput());
    }

    return status;
  }

  private static Path getBaseDirPath() {
    return baseDir.getRoot().toPath();
  }

  private String getBackupLocation() {
    return backupLocation.getRoot().getPath().toString();
  }

  private void setupSystemProperties(String solrClientType) {
    setupSolrClientType(solrClientType);
    setupZkHost();
  }

  private static void setDdfHome() {
    System.setProperty(DDF_HOME_PROP, DEFAULT_DDF_HOME);
  }

  private static void setDdfEtc() {
    System.setProperty(DDF_ETC_PROP, Paths.get(DEFAULT_DDF_HOME, "etc").toString());
  }

  private void setupSolrClientType(String solrClientType) {
    System.setProperty(SOLR_CLIENT_PROP, solrClientType);
  }

  private void setupZkHost() {
    System.setProperty(ZOOKEEPER_HOSTS_PROP, DEFAULT_ZK_HOSTS);
  }
}
