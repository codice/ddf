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

import static org.codice.ddf.commands.solr.SolrCommands.SOLR_CLIENT_PROP;
import static org.codice.ddf.commands.solr.SolrCommands.ZOOKEEPER_HOSTS_PROP;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.NamedList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BackupCommandTest extends SolrCommandTest {

  private static final String INVALID_COLLECTION_NAME = "myInvalidCollection";

  private static final Pattern ASCII_COLOR_CODES_REGEX = Pattern.compile("\u001B\\[[;\\d]*m");

  private static final long TIMEOUT_IN_MINUTES = 1;

  private static final String SEE_COMMAND_USAGE_MESSAGE =
      "Invalid Argument(s). Please see command usage for details.";

  private static final int SUCCESS_STATUS_CODE = 0;

  private static final int FAILURE_STATUS_CODE = 500;

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Mock SolrClient mockSolrClient;

  @BeforeClass
  public static void beforeClass() throws Exception {
    setDdfHome();
    setDdfEtc();
    createDefaultMiniSolrCloudCluster();
    addDocument("1");
  }

  @AfterClass
  public static void afterClass() throws Exception {
    if (miniSolrCloud != null) {
      miniSolrCloud.getSolrClient().close();
      miniSolrCloud.shutdown();
    }
  }

  @Before
  public void setUp() throws Exception {
    cipherSuites = System.getProperty("https.cipherSuites");
    System.setProperty(
        "https.cipherSuites",
        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA,TLS_RSA_WITH_AES_128_CBC_SHA");
    protocols = System.getProperty("https.protocols");
    System.setProperty("https.protocols", "TLSv1.1, TLSv1.2");
    System.setProperty("solr.http.url", "https://localhost:8994/solr");
    consoleOutput = new ConsoleOutput();
    consoleOutput.interceptSystemOut();
  }

  @After
  public void tearDown() {
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

  @Test
  public void testNoArgBackup() throws Exception {

    BackupCommand backupCommand =
        new BackupCommand() {
          HttpResponse sendGetRequest(URI backupUri) {
            return mockResponse(HttpStatus.SC_OK, "");
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

    BackupCommand backupCommand =
        new BackupCommand() {
          HttpResponse sendGetRequest(URI backupUri) {
            return mockResponse(HttpStatus.SC_OK, "");
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

    BackupCommand backupCommand =
        new BackupCommand() {
          HttpResponse sendGetRequest(URI backupUri) {
            return mockResponse(HttpStatus.SC_NOT_FOUND, "");
          }
        };

    backupCommand.coreName = coreName;
    backupCommand.execute();

    assertThat(
        consoleOutput.getOutput(),
        containsString(String.format("Backup request failed: %d", HttpStatus.SC_NOT_FOUND)));
  }

  @Test
  public void testSingleNodeBackupAsyncOptionSupplied() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    BackupCommand backupCommand =
        new BackupCommand() {
          HttpResponse sendGetRequest(URI backupUri) {
            return mockResponse(HttpStatus.SC_OK, "");
          }
        };
    backupCommand.asyncBackup = true;

    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudSynchronousBackup() throws Exception {

    // Set system properties
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getSynchronousBackupCommand(null, DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());

    // Perform Test
    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudSynchronousBackupNoCollection() throws Exception {

    // Set system properties
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    assertThat(consoleOutput.getOutput(), containsString("Backup failed."));
  }

  @Test
  public void testPerformSolrCloudSynchronousBackupNumberToKeepSupplied() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

    // Setup BackupCommand
    BackupCommand backupCommand =
        getSynchronousBackupCommand(
            getBackupLocation(), DEFAULT_CORE_NAME, miniSolrCloud.getSolrClient());
    backupCommand.numberToKeep = 3;

    // Perform Test
    backupCommand.execute();
  }

  @Test
  public void testPerformSolrCloudAsynchronousBackupNumberToKeepSupplied() throws Exception {

    // Setup exception expectations
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(SEE_COMMAND_USAGE_MESSAGE);

    // Set system properties
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    expectedException.expectMessage(
        "asyncBackupReqId must not be empty when checking on async status of a backup.");

    // Set system properties
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    setupSystemProperties(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

    // Setup BackupCommand (ie. solr:backup -i <request Id>)
    BackupCommand invalidBackupStatusCommand =
        getBackupCommand(null, null, false, false, "myRequestId0", miniSolrCloud.getSolrClient());

    invalidBackupStatusCommand.execute();
  }

  @Test
  public void testGetCloudSolrClientNoZkHosts() throws Exception {

    // Setup exception expectations
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Couldn't initialize a HttpClusterStateProvider");

    // Set the solr client type system property so that the
    // BackupCommand knows that it needs to backup solr cloud.
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
                "Status for request Id [%s] is [%s].",
                requestId, RequestStatusState.FAILED.getKey())));
    assertThat(consoleOutput.getOutput(), containsString("Status failed."));
    assertThat(
        consoleOutput.getOutput(),
        containsString("1. Error Name: error name 1; Error Value: error value 1"));
  }

  @Test
  public void testSolrCloudBackupStatusRequestThrowsException() throws Exception {

    // Set the solr client type system property so that the
    // BackupCommand knows that it needs to backup solr cloud.
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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

    assertThat(consoleOutput.getOutput(), containsString("Status failed."));
  }

  /**
   * Collections are optimized before backups. This test verifies that an error message is printed
   * to the console when optimization of a collection fails.
   */
  @Test
  public void testSolrCloudBackupFailsDuringOptimizationWithErrorCode() throws Exception {

    // Set the solr client type system property so that the
    // BackupCommand knows that it needs to backup solr cloud.
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
    assertThat(consoleOutput.getOutput(), containsString("Backup failed."));
  }

  /**
   * Collections are optimized before backups. This test verifies that an error message is printed
   * to the console when optimization of a collection throws an exception.
   */
  @Test
  public void testSolrCloudBackupFailsDuringOptimizationThrowsException() throws Exception {
    setupSolrClientType(SolrCommands.CLOUD_SOLR_CLIENT_TYPE);

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
        getResponseForStatus(FAILURE_STATUS_CODE, RequestStatusState.FAILED, backupErrorMessages);
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

  private BackupCommand getAsnychronousBackupCommand(
      String backupLocation, String collection, SolrClient solrClient) {
    return getBackupCommand(backupLocation, collection, true, false, null, solrClient);
  }

  private BackupCommand getStatusBackupCommand(String requestId, SolrClient solrClient) {
    return getBackupCommand(null, null, false, true, requestId, solrClient);
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
}
