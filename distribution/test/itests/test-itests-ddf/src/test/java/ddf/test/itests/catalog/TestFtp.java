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
package ddf.test.itests.catalog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.response.Response;

import ddf.common.test.AfterExam;
import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;

/**
 * Integration Tests for the FTP/S Endpoint supporting ingest.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestFtp extends AbstractIntegrationTest {

    private static final String FTP_SERVER = "localhost";

    private static final String FTP_PORT_PROPERTY = "org.codice.ddf.catalog.ftp.port";

    private static final DynamicPort FTP_PORT = new DynamicPort(FTP_PORT_PROPERTY, 6);

    private static final String FTP_ENDPOINT_FEATURE = "catalog-ftp";

    private static final String USERNAME = "admin";

    private static final String PASSWORD = "admin";

    private static final String SAMPLE_DATA = "sample test data";

    private FTPClient client;

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();

            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();

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
            LOGGER.error("Failed in @AfterExam: ", e);
            fail("Failed in @AfterExam: " + e.getMessage());
        }
    }

    @After
    public void tearDown() throws Exception {
        disconnect();
        client = null;
    }

    /**
     * Simple test verifying FTP client can be connected and configured successfully
     * 
     * @throws Exception
     */
    @Test
    public void testFtp() throws Exception {
        client = createInsecureClient();
        assertTrue(client.sendNoOp());
    }

    /**
     * Upload a file via insecure FTP for ingest using input stream
     * 
     * @throws Exception
     */
    @Test
    public void testInsecureFtpPut() throws Exception {
        client = createInsecureClient();

        ftpPut(SAMPLE_DATA);

        // verify FTP PUT resulted in ingest, catalogued data
        Response response = executeOpenSearch("xml", "q=*", "count=100");
        response.then().log().all().body("metacards.metacard.size()", equalTo(1));

        // clean up test data
        TestCatalog.deleteMetacard(getMetacardIdFromResponse(response));
    }

    /**
     * Upload a file via insecure FTP for ingest using streaming method
     * 
     * @throws Exception
     */
    @Test
    public void testInsecureFtpPutStream() throws Exception {
        client = createInsecureClient();

        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream("/metacard1.xml"), writer);
        ftpPutStreaming(writer.toString());

        // verify FTP PUT resulted in ingest, catalogued data
        Response response = executeOpenSearch("xml", "q=*", "count=100");
        response.then().log().all().body("metacards.metacard.size()", equalTo(1));

        // clean up test data
        TestCatalog.deleteMetacard(getMetacardIdFromResponse(response));
    }

    private FTPClient createInsecureClient() throws Exception {
        FTPClient ftp = new FTPClient();

        ftp.connect(FTP_SERVER, Integer.parseInt(FTP_PORT.getPort()));
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

    private void disconnect() {
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

    private Response executeOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH.getUrl()).append("?")
                .append("format=").append(format);

        for (String term : query) {
            buffer.append("&").append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return when().get(url);
    }

    private void ftpPut(String data) throws IOException {
        LOGGER.info("Start data upload via FTP PUT...");

        boolean done;

        try (InputStream ios = new ByteArrayInputStream(data.getBytes())) {
            // file will not actually be written to disk on ftp server
            done = client.storeFile("test", ios);
        }

        showServerReply(client);

        if (done) {
            LOGGER.debug("File uploaded successfully.");
        } else {
            LOGGER.error("Failed to upload file.");
        }
    }

    private void ftpPutStreaming(String data) throws IOException {
        LOGGER.info("Start data upload via FTP PUT...");

        try (
                InputStream is = new ByteArrayInputStream(data.getBytes());
                OutputStream os = client.storeFileStream("test");
        ) {
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

    private String getMetacardIdFromResponse(Response response)
        throws IOException, XPathExpressionException {
        return XmlPath.given(response.asString())
                // gpath to get the single ingested element ID
                .get("metacards.metacard[0].@gml:id");
    }

    private void showServerReply(FTPClient ftpClient) {
        String[] replies = ftpClient.getReplyStrings();
        if (replies != null && replies.length > 0) {
            for (String aReply : replies) {
                LOGGER.info("Server response: " + aReply);
            }
        }
    }
}
