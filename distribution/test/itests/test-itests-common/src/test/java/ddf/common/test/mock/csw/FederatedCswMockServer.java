/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.common.test.mock.csw;

import static org.junit.Assert.fail;
import static com.xebialabs.restito.semantics.Action.bytesContent;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.parameter;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xebialabs.restito.builder.stub.StubHttp;
import com.xebialabs.restito.builder.verify.VerifyHttp;
import com.xebialabs.restito.server.StubServer;

/**
 * Encompasses a Restito CSW Stub Server for use in integration testing. Handles handshake responses
 * and federated query responses for a csw endpoint.
 */
public class FederatedCswMockServer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(FederatedCswMockServer.class);

    private final String sourceId;

    private final String httpRoot;

    private final int port;

    private StubServer cswStubServer;

    private String defaultCapabilityResponseResourceName = "csw-get-capabilities-response.xml";

    private String defaultQueryResponseResourceName = "csw-query-response.xml";

    /**
     * Constructor for the federated CSW Restito stub server.
     *
     * @param sourceId DDF sourceId to give to the stub server.
     * @param httpRoot httpRoot address. Typically the non-secure root "http://localhost:".
     * @param port     Port number on which to respond.
     */
    public FederatedCswMockServer(String sourceId, String httpRoot, int port) {
        if (httpRoot.endsWith(":")) {
            this.httpRoot = httpRoot.substring(0, httpRoot.length() - 1);
        } else {
            this.httpRoot = httpRoot;
        }

        this.sourceId = sourceId;
        this.port = port;
    }

    /**
     * Sets the default response for the {@code GetCapabilities} request. Will be used when this
     * stub server is started and must be set before {@link #start()} is called.
     * </p>
     * If a different response is needed after startup, the normal {@link #whenHttp()} should be
     * used instead of replacing this default response.
     *
     * @param resourceName relative path to the file that contains the response to use, e.g.,
     *                     "csw-get-capabilities-response.xml"
     */
    public void setupDefaultCapabilityResponseExpectation(String resourceName) {
        this.defaultCapabilityResponseResourceName = resourceName;
    }

    /**
     * Sets the default response for the {@code GetRecords} request. Will be used when this stub
     * server is started and must be set before {@link #start()} is called.
     * </p>
     * If a different response is needed after startup, the normal {@link #whenHttp()} should be
     * used instead of replacing this default response.
     *
     * @param resourceName relative path to the file that contains the response to use, e.g.,
     *                     "csw-get-capabilities-response.xml"
     * @throws IOException thrown if the file can't be opened
     */
    public void setupDefaultQueryResponseExpectation(String resourceName) throws IOException {
        defaultQueryResponseResourceName = resourceName;
    }

    /**
     * Starts the Restito stub server.
     */
    public void start() {
        try {
            cswStubServer = new StubServer(port).run();

            setupDefaultCapabilityResponseExpectation();
            setupDefaultQueryResponseExpectation();
        } catch (IOException | RuntimeException e) {
            fail(String.format("Failed to setup %s: %s",
                    getClass().getSimpleName(),
                    e.getMessage()));
        }
    }

    /**
     * Stops the Restito stub server.
     */
    public void stop() {
        cswStubServer.stop();
    }

    /**
     * Resets the Restito server by stopping and starting it.
     */
    public void reset() {
        // There is no way in version 0.7 of Restito to clear the calls and reset the
        // StubServer so we need to stop the current one and create a new instance.
        // TODO - Replace this code when the new version of Restito is used.
        stop();
        start();
    }

    /**
     * Returns StubHttp's whenHttp method with the stub server as the parameter. Use is the same.
     * Sets up an Action when a specific Http call is made.
     *
     * @return StubHttp using whenHttp method with the stub server as the parameter.
     */
    public StubHttp whenHttp() {
        return StubHttp.whenHttp(cswStubServer);
    }

    /**
     * Returns VerifyHttp's verifyHttp method with the stub server as the parameter. Use is the same.
     * Provides itest verification when a specific Http call is made.
     *
     * @return VerifyHttp using verifyHttp method with the stub server as the parameter.
     */
    public VerifyHttp verifyHttp() {
        return VerifyHttp.verifyHttp(cswStubServer);
    }

    /**
     * Get the port being used by the stub server.
     *
     * @return the port being used by the stub server.
     */
    public int getPort() {
        return port;
    }

    /**
     * Get the root url being used by the stub server.
     *
     * @return the root url being used by the stub server.
     */
    public String getRoot() {
        return httpRoot;
    }

    /**
     * Returns the restito stub server to allow for any unsupported actions
     *
     * @return the stub server managed by this class
     */
    public StubServer getServer() {
        return cswStubServer;
    }

    private void setupDefaultCapabilityResponseExpectation() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(
                "/" + defaultCapabilityResponseResourceName)) {
            String document = substituteTags(IOUtils.toString(inputStream));

            LOGGER.debug("Capability response: \n{}", document);

            whenHttp().match(get("/services/csw"), parameter("request", "GetCapabilities"))
                    .then(ok(), contentType("text/xml"), bytesContent(document.getBytes()));
        }
    }

    private void setupDefaultQueryResponseExpectation() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(
                "/" + defaultQueryResponseResourceName)) {
            String document = substituteTags(IOUtils.toString(inputStream));

            LOGGER.debug("Query response: \n{}", document);

            whenHttp().match(post("/services/csw"), withPostBodyContaining("GetRecords"))
                    .then(ok(), contentType("text/xml"), bytesContent(document.getBytes()));
        }
    }

    private String substituteTags(String document) {
        String resultDocument = substituteSourceId(document);
        resultDocument = substitutePortNumber(resultDocument);
        return substituteHttpRoot(resultDocument);
    }

    private String substitutePortNumber(String document) {
        return document.replaceAll("\\$port\\$", Integer.toString(port));
    }

    private String substituteSourceId(String document) {
        return document.replaceAll("\\$sourceId\\$", sourceId);
    }

    private String substituteHttpRoot(String document) {
        return document.replaceAll("\\$httpRoot\\$", httpRoot);
    }
}
