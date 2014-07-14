/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverterFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.impl.CswRecordConverterFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * Tests that the certificates are properly added to outgoing requests and allow for mutual
 * authentication on a server that requires client auth.
 */
public class CswAuthenticationTest extends TestCswSourceBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswAuthenticationTest.class);

    private static Server server;

    private static int serverPort = 0;


    @BeforeClass
    public static void startServer() {
        // create jetty server
        server = new Server();
        server.setStopAtShutdown(true);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        // add dummy servlet that will return static response
        context.addServlet(CswServlet.class, "/services/csw");

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {context, new DefaultHandler()});
        server.setHandler(handlers);
        SslContextFactory sslContextFactory = new SslContextFactory();
        // server uses the server cert
        sslContextFactory.setKeyStorePath(
                CswAuthenticationTest.class.getResource("/serverKeystore.jks").getPath());
        sslContextFactory.setKeyStorePassword("changeit");

        // only accept connection with proper client certificate
        sslContextFactory.setNeedClientAuth(true);

        SslSocketConnector sslSocketConnector = new SslSocketConnector(sslContextFactory);
        sslSocketConnector.setPort(serverPort);
        server.addConnector(sslSocketConnector);

        try {
            server.start();
            if(server.getConnectors().length == 1) {
                serverPort = server.getConnectors()[0].getLocalPort();
                LOGGER.info("Server started on Port: {} ", serverPort);
            } else {
                LOGGER.warn("Got more than one connector back, could not determine correct port for SSL communication.");
            }
        } catch (Exception e) {
            LOGGER.warn("Could not start jetty server, expecting test failures.", e);
        }
    }

    /**
     * Tests that server properly accepts trusted certificates.
     */
    @Test
    public void testGoodCertificates() {

        CswSource cswSource = createSecuredSource(
                "/clientKeystore.jks", "changeit", "/clientTruststore.jks", "changeit");
        // hit server
        if (cswSource.getCapabilities() == null) {
            fail("Could not get capabilities from the test server. This means no connection was established.");
        }

    }

    /**
     * Tests that server fails on non-trusted client certificates.
     */
    @Test
    public void testBadClientCertificate() {

        CswSource cswSource = createSecuredSource(
                "/client-bad.jks", "", "/clientTruststore.jks", "changeit");
        // hit server
        if (cswSource.getCapabilities() != null) {
            fail("Server should have errored out with bad certificate but request passed instead.");
        }

    }

    /**
     * Tests that client fails on non-trusted server certificates.
     */
    @Test
    public void testBadServerCertificate() {

        CswSource cswSource = createSecuredSource(
                "/clientKeystore.jks", "changeit", "/client-bad.jks", "");
        // hit server
        if (cswSource.getCapabilities() != null) {
            fail("Cleint should have errored out with no valid certification path found, but request passed instead.");
        }

    }

    private CswSource createSecuredSource(String keyStorePath, String keyStorePassword,
            String trustStorePath, String trustStorePassword) {

        // set up csw configuration
        CswSourceConfiguration cswSourceConfiguration = new CswSourceConfiguration();
        cswSourceConfiguration.setContentTypeMapping(CswRecordMetacardType.CSW_TYPE);
        cswSourceConfiguration.setId(ID);
        cswSourceConfiguration.setModifiedDateMapping(Metacard.MODIFIED);
        cswSourceConfiguration.setProductRetrievalMethod(CswConstants.WCS_PRODUCT_RETRIEVAL);
        cswSourceConfiguration.setCswUrl("https://localhost:" + serverPort + "/services/csw");
        cswSourceConfiguration.setDisableSSLCertVerification(true);
        cswSourceConfiguration.setPollIntervalMinutes(1);
        RecordConverterFactory factory = new CswRecordConverterFactory();

        // create new source
        CswSource cswSource = new CswSource(null, mockContext, cswSourceConfiguration,
                Arrays.asList(factory));
        cswSource.setFilterAdapter(new GeotoolsFilterAdapterImpl());
        cswSource.setFilterBuilder(builder);
        cswSource.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);


        // client is using the client certificates
        Map<String, String> configurationMap = new HashMap<String, String>();
        configurationMap.put(ConfigurationManager.KEY_STORE, getClass().getResource(keyStorePath).getPath());
        configurationMap.put(ConfigurationManager.KEY_STORE_PASSWORD, keyStorePassword);
        configurationMap.put(ConfigurationManager.TRUST_STORE, getClass().getResource(trustStorePath).getPath());
        configurationMap.put(ConfigurationManager.TRUST_STORE_PASSWORD, trustStorePassword);

        cswSource.configurationUpdateCallback(configurationMap);

        cswSource.connectToRemoteCsw();

        return cswSource;
    }

}
