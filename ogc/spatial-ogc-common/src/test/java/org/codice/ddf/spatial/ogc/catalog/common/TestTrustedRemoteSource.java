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
package org.codice.ddf.spatial.ogc.catalog.common;

import ddf.security.settings.SecuritySettingsService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
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

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.client.ClientException;
import java.io.File;
import java.io.FileInputStream;
import java.net.SocketException;
import java.security.KeyStore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests that the certificates are properly added to outgoing requests and allow for mutual
 * authentication on a server that requires client auth.
 */
public class TestTrustedRemoteSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestTrustedRemoteSource.class);

    private static Server server;

    private static int serverPort = 0;

    private static final String GOOD_KEYSTORE_PATH = TestTrustedRemoteSource.class.getResource(
            "/clientKeystore.jks").getPath();

    private static final String GOOD_TRUSTSTORE_PATH = TestTrustedRemoteSource.class
            .getResource("/clientTruststore.jks").getPath();

    private static final String BAD_KEYSTORE_PATH = TestTrustedRemoteSource.class
            .getResource("/client-bad.jks").getPath();

    private static final String GOOD_PASSWORD = "changeit";

    private static final String BAD_PASSWORD = "";

    private static KeyStore keyStore;

    private static KeyStore trustStore;

    private static KeyStore badStore;

    @BeforeClass
    public static void startServer() {
        // create jetty server
        server = new Server();
        server.setStopAtShutdown(true);
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        // add dummy servlet that will return static response
        context.addServlet(TrustedServlet.class, "/");

        HandlerCollection handlers = new HandlerCollection();
        handlers.setHandlers(new Handler[] {context, new DefaultHandler()});
        server.setHandler(handlers);
        SslContextFactory sslContextFactory = new SslContextFactory();
        // server uses the server cert
        sslContextFactory.setKeyStorePath(
                TestTrustedRemoteSource.class.getResource("/serverKeystore.jks").getPath());
        sslContextFactory.setKeyStorePassword("changeit");

        // only accept connection with proper client certificate
        sslContextFactory.setNeedClientAuth(true);

        SslSocketConnector sslSocketConnector = new SslSocketConnector(sslContextFactory);
        sslSocketConnector.setPort(serverPort);
        server.addConnector(sslSocketConnector);

        try {
            server.start();
            if (server.getConnectors().length == 1) {
                serverPort = server.getConnectors()[0].getLocalPort();
                LOGGER.info("Server started on Port: {} ", serverPort);
            } else {
                LOGGER.warn(
                        "Got more than one connector back, could not determine correct port for SSL communication.");
            }
        } catch (Exception e) {
            LOGGER.warn("Could not start jetty server, expecting test failures.", e);
        }
    }

    @BeforeClass
    public static void createKeystores() {
        trustStore = createKeyStore(GOOD_TRUSTSTORE_PATH, GOOD_PASSWORD);
        keyStore = createKeyStore(GOOD_KEYSTORE_PATH, GOOD_PASSWORD);
        badStore = createKeyStore(BAD_KEYSTORE_PATH, BAD_PASSWORD);
    }

    /**
     * Tests that server properly accepts trusted certificates.
     */
    @Test
    public void testGoodCertificates() {
        RemoteSource remoteSource = createSecuredSource(keyStore, GOOD_PASSWORD, trustStore, 30000,
                60000);
        // hit server
        if (remoteSource.get() == null) {
            fail("Could not get capabilities from the test server. This means no connection was established.");
        }

    }

    /**
     * Tests that server fails on non-trusted client certificates.
     */
    @Test
    public void testBadClientCertificate() {
        RemoteSource remoteSource = createSecuredSource(badStore, BAD_PASSWORD, trustStore, 30000,
                60000);
        // hit server
        try {
            if (remoteSource.get() != null) {
                fail("Server should have errored out with bad certificate but request passed instead.");
            }
        } catch (ClientException e) {
            assertThat(e.getCause(),
                    anyOf(is(SSLHandshakeException.class), is(SocketException.class)));
        }

    }

    /**
     * Tests that client fails on non-trusted server certificates.
     */
    @Test
    public void testBadServerCertificate() {
        RemoteSource remoteSource = createSecuredSource(keyStore, GOOD_PASSWORD, badStore, 30000,
                60000);
        // hit server
        try {
            if (remoteSource.get() != null) {
                fail("Client should have errored out with no valid certification path found, but request passed instead.");
            }
        } catch (ClientException e) {
            assertThat(e.getCause(), is(SSLHandshakeException.class));
        }

    }

    private RemoteSource createSecuredSource(KeyStore keyStore, String keystorePassword,
            KeyStore trustStore, Integer connectionTimeout, Integer receiveTimeout) {
        RemoteSource rs = new RemoteSource("https://localhost:" + serverPort + "/", true);
        rs.setTimeouts(connectionTimeout, receiveTimeout);
        SecuritySettingsService securitySettingsService = mock(SecuritySettingsService.class);
        when(securitySettingsService.getTLSParameters())
                .thenReturn(getTLSParameters(keyStore, keystorePassword, trustStore));
        rs.setSecuritySettings(securitySettingsService);
        rs.setTlsParameters();
        return rs;
    }

    private static KeyStore createKeyStore(String path, String password) {
        KeyStore keyStore = null;
        File keyStoreFile = new File(path);
        FileInputStream fis = null;
        if (StringUtils.isNotBlank(password)) {
            try {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                fis = new FileInputStream(keyStoreFile);
                keyStore.load(fis, password.toCharArray());
            } catch (Exception e) {
                LOGGER.warn("Could not load keystore from {} with password {}", path, password);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }
        return keyStore;
    }

    private TLSClientParameters getTLSParameters(KeyStore keyStore, String keystorePassword,
            KeyStore trustStore) {
        TLSClientParameters tlsParams = new TLSClientParameters();
        try {
            TrustManagerFactory trustFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory
                            .getDefaultAlgorithm());
            trustFactory.init(trustStore);
            TrustManager[] tm = trustFactory.getTrustManagers();
            tlsParams.setTrustManagers(tm);

            KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                    .getDefaultAlgorithm());
            keyFactory.init(keyStore, keystorePassword.toCharArray());
            KeyManager[] km = keyFactory.getKeyManagers();
            tlsParams.setKeyManagers(km);
        } catch (Exception e) {
            LOGGER.warn("Could not load keystores, may be an error with the filesystem", e);
        }

        FiltersType filter = new FiltersType();
        filter.getInclude().addAll(SecuritySettingsService.SSL_ALLOWED_ALGORITHMS);
        filter.getExclude().addAll(SecuritySettingsService.SSL_DISALLOWED_ALGORITHMS);
        tlsParams.setCipherSuitesFilter(filter);

        return tlsParams;
    }

}
