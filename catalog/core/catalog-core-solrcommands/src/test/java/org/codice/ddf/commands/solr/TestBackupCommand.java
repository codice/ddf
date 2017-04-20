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
 */
package org.codice.ddf.commands.solr;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.apache.solr.client.solrj.embedded.JettyConfig;
import org.apache.solr.client.solrj.request.CollectionAdminRequest;
import org.apache.solr.client.solrj.response.CollectionAdminResponse;
import org.apache.solr.client.solrj.response.RequestStatusState;
import org.apache.solr.cloud.MiniSolrCloudCluster;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestBackupCommand {

    private static final String DEFAULT_CORE_NAME = "catalog";

    private static final String SOLR_CLOUD_CLIENT_TYPE = "CloudSolrClient";

    private static final String SOLR_CLIENT_PROP = "solr.client";

    private static final String INVALID_COLLECTION_NAME = "myInvalidCollection";

    private static final String DEFAULT_CONFIGSET = "collection_configset_1";

    private static final Pattern ASCII_COLOR_CODES_REGEX = Pattern.compile("\u001B\\[[;\\d]*m");

    private static final long TIMEOUT_IN_MINUTES = 1;

    HttpWrapper mockHttpWrapper;

    ConsoleOutput consoleOutput;

    private static MiniSolrCloudCluster miniSolrCloud;

    private static String cipherSuites;

    private static String protocols;

    @Rule
    @ClassRule
    public static TemporaryFolder baseDir = new TemporaryFolder();

    @Rule
    @ClassRule
    public static TemporaryFolder backupLocation = new TemporaryFolder();

    @BeforeClass
    public static void beforeClass() throws Exception {
        createDefaultMiniSolrCloudCluster();
        addDocument("1");
    }

    @Before
    public void before() throws Exception {
        cipherSuites = System.getProperty("https.cipherSuites");
        System.setProperty("https.cipherSuites",
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
            miniSolrCloud.getSolrClient()
                    .close();
            miniSolrCloud.shutdown();
        }
    }

    @Test
    public void testNoArgBackup() throws Exception {

        when(mockHttpWrapper.execute(any(URI.class))).thenReturn(mockResponse(HttpStatus.SC_OK,
                ""));

        BackupCommand backupCommand = new BackupCommand() {
            @Override
            protected HttpWrapper getHttpClient() {
                return mockHttpWrapper;
            }
        };
        backupCommand.doExecute();

        assertThat(consoleOutput.getOutput(),
                containsString(String.format("Backup of [%s] complete.", DEFAULT_CORE_NAME)));

    }

    @Test
    public void testBackupSpecificCore() throws Exception {
        final String coreName = "core";

        when(mockHttpWrapper.execute(any(URI.class))).thenReturn(mockResponse(HttpStatus.SC_OK,
                ""));

        BackupCommand backupCommand = new BackupCommand() {
            @Override
            protected HttpWrapper getHttpClient() {
                return mockHttpWrapper;
            }
        };

        backupCommand.coreName = coreName;
        backupCommand.doExecute();

        assertThat(consoleOutput.getOutput(),
                containsString(String.format("Backup of [%s] complete.", coreName)));

    }

    @Test
    public void testBackupInvalidCore() throws Exception {
        final String coreName = "badCoreName";

        when(mockHttpWrapper.execute(any(URI.class))).thenReturn(mockResponse(HttpStatus.SC_NOT_FOUND,
                ""));

        BackupCommand backupCommand = new BackupCommand() {
            @Override
            protected HttpWrapper getHttpClient() {
                return mockHttpWrapper;
            }
        };

        backupCommand.coreName = coreName;
        backupCommand.doExecute();

        assertThat(consoleOutput.getOutput(),
                containsString(String.format("Backup command failed due to: %d",
                        HttpStatus.SC_NOT_FOUND)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSystemPropertiesNotSet() throws Exception {

        BackupCommand backupCommand = new BackupCommand();
        backupCommand.doExecute();
    }

    @Test
    public void testPerformSolrCloudSynchronousBackup() throws Exception {

        // Set the solr client type system property so that the
        // BackupCommand knows that it needs to backup solr cloud.
        setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

        // Setup BackupCommand
        BackupCommand backupCommand = getSynchronousBackupCommand(getBackupLocation(), DEFAULT_CORE_NAME);

        // Perform Test
        backupCommand.doExecute();

        // Verify
        assertThat(consoleOutput.getOutput(),
                containsString(String.format(
                        "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                        DEFAULT_CORE_NAME,
                        backupCommand.backupLocation,
                        DEFAULT_CORE_NAME)));
        assertThat(consoleOutput.getOutput(), containsString("Backup complete."));
    }

    @Test
    public void testPerformSolrCloudSynchronousBackupNoOptions() throws Exception {

        // Set the solr client type system property so that the
        // BackupCommand knows that it needs to backup solr cloud.
        setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

        // Setup BackupCommand
        BackupCommand backupCommand = getSynchronousBackupCommand(null, null);

        // Perform Test
        backupCommand.doExecute();

        // Verify
        assertThat(consoleOutput.getOutput(), containsString("Insufficient options. Run solr:backup --help for usage details."));
    }

    @Test
    public void testPerformSolrCloudSynchronousBackupNoBackupLocation() throws Exception {

        // Set the solr client type system property so that the
        // BackupCommand knows that it needs to backup solr cloud.
        setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

        // Setup BackupCommand
        BackupCommand backupCommand = getSynchronousBackupCommand(null, DEFAULT_CORE_NAME);

        // Perform Test
        backupCommand.doExecute();

        // Verify
        assertThat(consoleOutput.getOutput(),
                containsString("Insufficient options. Run solr:backup --help for usage details."));
    }

    @Test
    public void testPerformSolrCloudSynchronousBackupNoCollection() throws Exception {

        // Set the solr client type system property so that the
        // BackupCommand knows that it needs to backup solr cloud.
        setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

        // Setup BackupCommand
        BackupCommand backupCommand = getSynchronousBackupCommand(getBackupLocation(), null);

        // Perform Test
        backupCommand.doExecute();

        // Verify
        assertThat(consoleOutput.getOutput(),
                containsString(String.format(
                        "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                        DEFAULT_CORE_NAME,
                        backupCommand.backupLocation,
                        DEFAULT_CORE_NAME)));
        assertThat(consoleOutput.getOutput(), containsString("Backup complete."));
    }

    @Test
    public void testPerformSolrCloudSynchronousBackupInvalidCollectionName() throws Exception {

        // Set the solr client type system property so that the
        // BackupCommand knows that it needs to backup solr cloud.
        setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

        // Setup BackupCommand
        BackupCommand backupCommand = getSynchronousBackupCommand(getBackupLocation(), INVALID_COLLECTION_NAME);

        // Perform Test
        backupCommand.doExecute();

        // Verify
        assertThat(consoleOutput.getOutput(),
                containsString(String.format(
                        "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                        INVALID_COLLECTION_NAME,
                        backupCommand.backupLocation,
                        INVALID_COLLECTION_NAME)));
        assertThat(consoleOutput.getOutput(),
                containsString(String.format(
                        "Backup failed. Unable to optimize collection [%s]. Collection not found: %s",
                        INVALID_COLLECTION_NAME,
                        INVALID_COLLECTION_NAME)));
    }

    @Test
    public void testPerformSolrCloudAsynchronousBackupAndStatus() throws Exception {

        // Set the solr client type system property so that the
        // BackupCommand knows that it needs to backup solr cloud.
        setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

        // Setup BackupCommand for async backup
        BackupCommand backupCommand = getAsnychronousBackupCommand(getBackupLocation(), DEFAULT_CORE_NAME);

        // Perform Test (backup)
        backupCommand.doExecute();

        // Verify
        assertThat(consoleOutput.getOutput(),
                containsString(String.format(
                        "Backing up collection [%s] to shared location [%s] using backup name [%s_",
                        DEFAULT_CORE_NAME,
                        backupCommand.backupLocation,
                        DEFAULT_CORE_NAME)));
        assertThat(consoleOutput.getOutput(), containsString("Solr Cloud backup request Id:"));

        String requestId = getRequestId(consoleOutput.getOutput());
        consoleOutput.reset();

        // Setup BackupCommand for status lookup
        BackupCommand statusBackupCommand = getStatusBackupCommand(requestId);

        // Perform status lookup
        statusBackupCommand.doExecute();

        String status = waitForCompletedStatusOrFail(statusBackupCommand, consoleOutput);

        assertThat(status, is(RequestStatusState.COMPLETED.getKey()));
    }

    @Test
    public void testGetSolrCloudAsynchronousBackupStatusNoRequestId() throws Exception {

        // Set the solr client type system property so that the
        // BackupCommand knows that it needs to backup solr cloud.
        setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

        // Setup BackupCommand for status lookup
        BackupCommand statusBackupCommand = getStatusBackupCommand(null);

        statusBackupCommand.doExecute();

        assertThat(consoleOutput.getOutput(),
                containsString("Insufficient options. Run solr:backup --help for usage details."));
    }

    @Test
    public void testGetSolrCloudAsynchronousBackupStatusNoStatusOption() throws Exception {

        // Set the solr client type system property so that the
        // BackupCommand knows that it needs to backup solr cloud.
        setupSolrClientType(SOLR_CLOUD_CLIENT_TYPE);

        // Setup BackupCommand (ie. solr:backup -i <request Id>)
        BackupCommand invalidBackupStatusCommand = getBackupCommand(null, null, false, false, "myRequestId0");

        invalidBackupStatusCommand.doExecute();

        assertThat(consoleOutput.getOutput(),
                containsString("Insufficient options. Run solr:backup --help for usage details."));
    }

    private ResponseWrapper mockResponse(int statusCode, String responseBody) {
        return new ResponseWrapper(prepareResponse(statusCode, responseBody));
    }

    private HttpResponse prepareResponse(int statusCode, String responseBody) {
        HttpResponse httpResponse = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion(
                "HTTP",
                1,
                1), statusCode, ""));
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
        miniSolrCloud = new MiniSolrCloudCluster(1,
                getBaseDirPath(),
                JettyConfig.builder()
                        .setContext("/solr")
                        .build());
        miniSolrCloud.getSolrClient()
                .connect();
    }

    private static void uploadDefaultConfigset() throws Exception {
        miniSolrCloud.uploadConfigSet(Paths.get(TestBackupCommand.class.getClassLoader()
                .getResource("configset")
                .getPath()), DEFAULT_CONFIGSET);
    }

    private static void createDefaultCollection() throws Exception {
        CollectionAdminRequest.Create create = CollectionAdminRequest.createCollection(
                DEFAULT_CORE_NAME,
                DEFAULT_CONFIGSET,
                1,
                1);
        CollectionAdminResponse response = create.process(miniSolrCloud.getSolrClient());
        if (response.getStatus() != 0 || response.getErrorMessages() != null) {
            fail("Could not create collection. Response: " + response.toString());
        }

        List<String> collections =
                CollectionAdminRequest.listCollections(miniSolrCloud.getSolrClient());
        assertThat(collections.size(), is(1));
        miniSolrCloud.getSolrClient()
                .setDefaultCollection(DEFAULT_CORE_NAME);
    }

    private static void addDocument(String uniqueId) throws Exception {
        SolrInputDocument doc = new SolrInputDocument();
        doc.setField("id", uniqueId);
        miniSolrCloud.getSolrClient()
                .add(doc);
        miniSolrCloud.getSolrClient()
                .commit();
    }

    private BackupCommand getSynchronousBackupCommand(String backupLocation, String collection) {
        return getBackupCommand(backupLocation, collection, false, false, null);
    }

    private BackupCommand getAsnychronousBackupCommand(String backupLocation, String collection) {
        return getBackupCommand(backupLocation, collection, true, false, null);
    }

    private BackupCommand getStatusBackupCommand(String requestId) {
        return getBackupCommand(null, null, false, true, requestId);
    }

    private BackupCommand getBackupCommand(String backupLocation, String collection,
            boolean asyncBackup, boolean asyncBackupStatus, String requestId) {
        BackupCommand backupCommand = new BackupCommand() {
            @Override
            SolrClient getSolrClient() {
                return miniSolrCloud.getSolrClient();
            }

            // We get the solr client from the MiniSolrCloudCluster, so we don't
            // want to shut it down after each test as there is no way to restart it.
            // We don't create a MiniSolrCloudCluster for each test to reduce the
            // time it takes to run the tests.
            @Override
            void shutdown(SolrClient client) {
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
        return StringUtils.trim(StringUtils.substringAfterLast(ASCII_COLOR_CODES_REGEX.matcher(consoleOutput).replaceAll(""), ":"));
    }

    // Replace ASCII color codes in console output and get the status
    private String getRequestStatus(String consoleOutput) {
        return StringUtils.trim(StringUtils.substringsBetween(ASCII_COLOR_CODES_REGEX.matcher(consoleOutput).replaceAll(""), "[", "]")[1]);
    }

    private String waitForCompletedStatusOrFail(BackupCommand statusBackupCommand,
            ConsoleOutput consoleOutput) throws Exception {
        long startTime = System.currentTimeMillis();
        long endTime = startTime + TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES);
        String status = getRequestStatus(consoleOutput.getOutput());

        while (!StringUtils.equals(status, RequestStatusState.COMPLETED.getKey())) {
            if (System.currentTimeMillis() >= endTime) {
                fail(String.format(
                        "The backup status command did not complete within %s minute(s). Current backup status: %s.",
                        TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES),
                        status));
            }
            TimeUnit.SECONDS.sleep(1);
            consoleOutput.reset();
            statusBackupCommand.doExecute();
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

    private void setupSolrClientType(String solrClientType) {
        System.setProperty(SOLR_CLIENT_PROP, solrClientType);
    }
}