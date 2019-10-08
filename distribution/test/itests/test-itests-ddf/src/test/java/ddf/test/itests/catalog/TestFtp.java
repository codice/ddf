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
package ddf.test.itests.catalog;

import static ddf.catalog.ftp.FtpServerManager.CLIENT_AUTH_PROPERTY_KEY;
import static ddf.catalog.ftp.FtpServerManager.NEED;
import static ddf.catalog.ftp.FtpServerManager.WANT;
import static org.codice.ddf.itests.common.WaitCondition.expect;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearch;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.jayway.restassured.response.Response;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.KeyManagerUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.service.cm.Configuration;

/** Integration Tests for the FTP/S Endpoint supporting ingest. */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestFtp extends AbstractIntegrationTest {

  private static final String FTP_SERVER = "localhost";

  private static final String FTP_PORT_PROPERTY = "org.codice.ddf.catalog.ftp.port";

  private static final DynamicPort FTP_PORT = new DynamicPort(FTP_PORT_PROPERTY, 6);

  private static final String FTP_ENDPOINT_FEATURE = "catalog-ftp";

  private static final String USERNAME = "admin";

  private static final String PASSWORD = "admin";

  private static final String SAMPLE_DATA = "sample test data";

  private static final String SAMPLE_DATA_TITLE = "test.bin";

  private static final String METACARD_TITLE = "Metacard-1";

  private static final String METACARD_FILE = "metacard1.xml";

  private static final int SET_CLIENT_AUTH_TIMEOUT_SEC = (int) TimeUnit.MINUTES.toSeconds(5);

  private static final int VERIFY_INGEST_TIMEOUT_SEC = 10;

  private FTPClient client;

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      System.setProperty(FTP_PORT_PROPERTY, FTP_PORT.getPort());
      getServiceManager().startFeature(true, FTP_ENDPOINT_FEATURE);

    } catch (Exception e) {
      LOGGER.error("Failed in @BeforeExam: ", e);
      fail("Failed in @BeforeExam: " + e.getMessage());
    }
  }

  @AfterExam
  public void afterExam() throws Exception {
    try {
      // Turn off feature to not interfere with other tests
      getServiceManager().stopFeature(true, FTP_ENDPOINT_FEATURE);

    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @AfterExam: ");
    }
  }

  @After
  public void tearDown() {
    disconnectClient(client);
    clearCatalog();
  }

  /**
   * Simple test verifying FTP client can be connected and successfully complete SSL handshake with
   * FTP server when clientAuth = "want"
   *
   * @throws Exception
   */
  @Test
  public void testFtpWantClientAuth() throws Exception {
    setClientAuthConfiguration(WANT);
    client = createInsecureClient();
    assertTrue(client.sendNoOp());
  }

  /**
   * Simple test verifying FTPS client with keystore can be connected and successfully complete SSL
   * handshake with FTP server when clientAuth = "want"
   *
   * @throws Exception
   */
  @Test
  public void testFtpsWithKeystoreWantClientAuth() throws Exception {
    setClientAuthConfiguration(WANT);
    client = createSecureClient(true);
    assertTrue(client.sendNoOp());
  }

  /**
   * Simple test verifying FTPS client with keystore can be connected and successfully complete SSL
   * handshake with FTP server when clientAuth = "need"
   *
   * @throws Exception
   */
  @Test
  public void testFtpsWithKeystoreNeedClientAuth() throws Exception {
    setClientAuthConfiguration(NEED);
    client = createSecureClient(true);
    assertTrue(client.sendNoOp());
  }

  /**
   * Simple test verifying FTPS client without keystore can be connected and successfully complete
   * SSL handshake with FTP server when clientAuth = "want"
   *
   * @throws Exception
   */
  @Test
  public void testFtpsWithoutKeystoreWantClientAuth() throws Exception {
    setClientAuthConfiguration(WANT);
    client = createSecureClient(false);
    assertTrue(client.sendNoOp());
  }

  /**
   * Simple test verifying FTPS client without keystore can be connected and successfully complete
   * SSL handshake with FTP server when clientAuth = "need"
   *
   * @throws Exception
   */
  @Test(expected = SSLException.class)
  public void testFtpsWithoutKeystoreNeedClientAuth() throws Exception {
    setClientAuthConfiguration(NEED);
    client = createSecureClient(false);
  }

  /**
   * Upload a file via insecure FTP for ingest using input stream
   *
   * @throws Exception
   */
  @Test
  public void testFtpPut() throws Exception {
    setClientAuthConfiguration(WANT);
    client = createInsecureClient();

    ftpPut(client, SAMPLE_DATA, SAMPLE_DATA_TITLE);

    // verify FTP PUT resulted in ingest, catalogued data
    verifyIngest(1, SAMPLE_DATA_TITLE);
  }

  /**
   * Upload a file via insecure FTP for ingest using streaming method
   *
   * @throws Exception
   */
  @Test
  public void testFtpPutStream() throws Exception {
    setClientAuthConfiguration(WANT);
    client = createInsecureClient();

    ftpPutStreaming(client, getFileContent(METACARD_FILE), METACARD_TITLE);

    // verify FTP PUT resulted in ingest, catalogued data
    verifyIngest(1, METACARD_TITLE);
  }

  /**
   * Upload a file via FTPS for ingest using input stream
   *
   * @throws Exception
   */
  @Test
  public void testFtpsPut() throws Exception {
    setClientAuthConfiguration(NEED);
    client = createSecureClient(true);

    ftpPut(client, SAMPLE_DATA, SAMPLE_DATA_TITLE);

    // verify FTP PUT resulted in ingest, catalogued data
    verifyIngest(1, SAMPLE_DATA_TITLE);
  }

  /**
   * Test the ftp command sequence of uploading a dot-file (i.e. .foo) and then renaming that file
   * to the final filename (i.e. test.txt). Confirm that the catalog contains only one object and
   * the title of that object is "test.txt".
   */
  @Test
  public void testFtpsPutDotFile() throws Exception {

    setClientAuthConfiguration(NEED);
    FTPSClient client = createSecureClient(true);

    String dotFilename = ".foo";
    String finalFilename = "test.txt";

    ftpPut(client, SAMPLE_DATA, dotFilename);
    boolean ret = client.rename(dotFilename, finalFilename);
    assertTrue(ret);

    verifyIngest(1, finalFilename);
  }

  @Test
  public void testFtpsMkdirCwdPutFile() throws Exception {

    setClientAuthConfiguration(NEED);
    FTPSClient client = createSecureClient(true);

    String newDir = "newDirectory";
    String finalFilename = "test.txt";

    boolean ret = client.makeDirectory(newDir);
    assertTrue(ret);
    ret = client.changeWorkingDirectory(newDir);
    assertTrue(ret);
    ftpPut(client, SAMPLE_DATA, finalFilename);

    verifyIngest(1, finalFilename);
  }

  /**
   * Upload a file via FTPS for ingest using streaming method
   *
   * @throws Exception
   */
  @Test
  public void testFtpsPutStream() throws Exception {
    setClientAuthConfiguration(NEED);
    client = createSecureClient(true);

    ftpPutStreaming(client, getFileContent(METACARD_FILE), METACARD_TITLE);

    // verify FTP PUT resulted in ingest, catalogued data
    verifyIngest(1, METACARD_TITLE);
  }

  private FTPClient createInsecureClient() throws Exception {
    FTPClient ftp = new FTPClient();

    int attempts = 0;
    while (true) {
      try {
        ftp.connect(FTP_SERVER, Integer.parseInt(FTP_PORT.getPort()));
        break;
      } catch (SocketException e) {
        // a socket exception can be thrown if the ftp server is still in the process of coming up
        // or down
        Thread.sleep(1000);
        if (attempts++ > 30) {
          throw e;
        }
      }
    }

    showServerReply(ftp);
    int connectionReply = ftp.getReplyCode();
    if (!FTPReply.isPositiveCompletion(connectionReply)) {
      fail("FTP server refused connection: " + connectionReply);
    }

    boolean success = ftp.login(USERNAME, PASSWORD);
    showServerReply(ftp);
    if (!success) {
      fail("Could not log in to the FTP server.");
    }

    ftp.enterLocalPassiveMode();
    ftp.setControlKeepAliveTimeout(300);
    ftp.setFileType(FTP.BINARY_FILE_TYPE);

    return ftp;
  }

  private FTPSClient createSecureClient(boolean setKeystore) throws Exception {
    FTPSClient ftps = new FTPSClient();

    if (setKeystore) {
      KeyManager keyManager =
          KeyManagerUtils.createClientKeyManager(
              new File(System.getProperty("javax.net.ssl.keyStore")),
              System.getProperty("javax.net.ssl.keyStorePassword"));
      ftps.setKeyManager(keyManager);
    }

    int attempts = 0;
    while (true) {
      try {
        ftps.connect(FTP_SERVER, Integer.parseInt(FTP_PORT.getPort()));
        break;
      } catch (SocketException e) {
        // a socket exception can be thrown if the ftp server is still in the process of coming up
        // or down
        Thread.sleep(1000);
        if (attempts++ > 30) {
          throw e;
        }
      }
    }

    showServerReply(ftps);
    int connectionReply = ftps.getReplyCode();
    if (!FTPReply.isPositiveCompletion(connectionReply)) {
      fail("FTP server refused connection: " + connectionReply);
    }

    boolean success = ftps.login(USERNAME, PASSWORD);
    showServerReply(ftps);
    if (!success) {
      fail("Could not log in to the FTP server.");
    }

    ftps.enterLocalPassiveMode();
    ftps.setControlKeepAliveTimeout(300);
    ftps.setFileType(FTP.BINARY_FILE_TYPE);

    return ftps;
  }

  private void disconnectClient(FTPClient client) {
    if (client != null && client.isConnected()) {
      try {
        client.logout();
      } catch (IOException ioe) {
        // ignore
      }
      try {
        client.disconnect();
      } catch (IOException ioe) {
        // ignore
      }
    }
  }

  private void ftpPut(FTPClient client, String data, String fileTitle) throws IOException {
    LOGGER.info("Start data upload via FTP PUT...");

    boolean done;

    try (InputStream ios = new ByteArrayInputStream(data.getBytes())) {
      // file will not actually be written to disk on ftp server
      done = client.storeFile(fileTitle, ios);
    }

    showServerReply(client);

    if (done) {
      LOGGER.debug("File uploaded successfully.");
    } else {
      LOGGER.error("Failed to upload file.");
    }
  }

  private void ftpPutStreaming(FTPClient client, String data, String fileName) throws IOException {
    LOGGER.info("Start data upload via FTP PUT...");

    try (InputStream is = new ByteArrayInputStream(data.getBytes());
        OutputStream os = client.storeFileStream(fileName)) {
      byte[] bytesIn = new byte[4096];
      int read = 0;

      while ((read = is.read(bytesIn)) != -1) {
        os.write(bytesIn, 0, read);
      }
    }

    showServerReply(client);

    // finalize the file transfer
    boolean done = client.completePendingCommand();
    if (done) {
      LOGGER.debug("File uploaded successfully.");
    } else {
      LOGGER.error("Failed to upload file.");
    }
  }

  private void showServerReply(FTPClient ftpClient) {
    String[] replies = ftpClient.getReplyStrings();
    if (replies != null && replies.length > 0) {
      for (String aReply : replies) {
        LOGGER.info("Server response: {}", aReply);
      }
    }
  }

  /**
   * Sets the clientAuth configuration in catalog-ftp feature.
   *
   * <p>An FTPS client without a certificate is used to indicate when the clientAuth configuration
   * has taken effect at the FTP server level. - When the clientAuth is set to "want", this FTPS
   * client without a certificate should be able to connect to the FTP server and successfully
   * complete the SSL handshake. - When the clientAuth is set to "need", this FTPS client without a
   * certificate should be able to connect to the FTP server but fail to complete the SSL handshake.
   *
   * <p>SocketException and FTPConnectionClosedException are thrown when the client cannot connect
   * to the server or the connection was closed unexpectedly. These exceptions are thrown when the
   * server is being updated. SSLException is thrown only after a client has successfully connected
   * and when the SSL handshake between the client and server fails.
   *
   * @throws Exception
   */
  private void setClientAuthConfiguration(String clientAuth) throws Exception {
    Configuration config = getAdminConfig().getConfiguration("ddf.catalog.ftp.FtpServerManager");
    config.setBundleLocation("mvn:ddf.catalog/ftp/" + System.getProperty("ddf.version"));
    Dictionary properties = new Hashtable<>();
    properties.put(CLIENT_AUTH_PROPERTY_KEY, clientAuth);
    config.update(properties);

    // wait until the clientAuth configuration has taken effect at the FTP server level
    switch (clientAuth) {
      case WANT:
        expect(
                "SSL handshake to succeed with FTPS client without certificate because clientAuth = \"want\"")
            .within(SET_CLIENT_AUTH_TIMEOUT_SEC, TimeUnit.SECONDS)
            .until(
                () -> {
                  FTPSClient client = null;
                  try {
                    client = createSecureClient(false);
                    disconnectClient(client);
                    return true;
                  } catch (SSLException e) {
                    // SSL handshake failed
                    return false;
                  } catch (SocketException | FTPConnectionClosedException e) {
                    // connection failed
                    return false;
                  }
                });
        break;
      case NEED:
        expect(
                "SSL handshake to fail with FTPS client without certificate because clientAuth = \"need\"")
            .within(SET_CLIENT_AUTH_TIMEOUT_SEC, TimeUnit.SECONDS)
            .until(
                () -> {
                  FTPSClient client = null;
                  try {
                    client = createSecureClient(false);
                    disconnectClient(client);
                    return false;
                  } catch (SSLException e) {
                    // SSL handshake failed
                    return true;
                  } catch (SocketException | FTPConnectionClosedException e) {
                    // connection failed
                    return false;
                  }
                });
    }
  }

  private void verifyIngest(int expectedResults, String expectedTitle) {
    // verify FTP PUT resulted in ingest, catalogued data
    expect(
            String.format(
                "Failed to verify FTP ingest expectedResult(s) of %d and expectedTitle of %s",
                expectedResults, expectedTitle))
        .within(VERIFY_INGEST_TIMEOUT_SEC, TimeUnit.SECONDS)
        .until(
            () -> {
              final Response response =
                  getOpenSearch("xml", null, null, "q=*", "count=100").extract().response();

              int numOfResults = response.xmlPath().getList("metacards.metacard").size();

              String title =
                  response
                      .xmlPath()
                      .get("metacards.metacard.string.findAll { it.@name == 'title' }.value");

              boolean success = numOfResults == expectedResults && title.equals(expectedTitle);

              return success;
            });
  }
}
