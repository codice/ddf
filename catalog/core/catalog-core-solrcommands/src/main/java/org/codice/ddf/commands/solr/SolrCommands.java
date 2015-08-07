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
package org.codice.ddf.commands.solr;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.codice.solr.factory.SolrServerFactory;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SolrCommands extends OsgiCommandSupport implements ConfigurationWatcher {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SolrCommands.class);

    protected static Map<String, String> configurationMap;

    protected static final String NAMESPACE = "solr";

    protected PrintStream console = System.out;

    private static final Color ERROR_COLOR = Ansi.Color.RED;

    private static final Color SUCCESS_COLOR = Ansi.Color.GREEN;

    protected abstract Object doExecute() throws Exception;

    @Override
    public void configurationUpdateCallback(Map<String, String> configuration) {
        configurationMap = configuration;
    }

    protected HttpClient getHttpClient() {
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                getSslContext(), getProtocols(), getCipherSuites(),
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

        return HttpClients.custom().setSSLSocketFactory(sslConnectionSocketFactory)
                .setDefaultCookieStore(new BasicCookieStore()).setMaxConnTotal(128)
                .setMaxConnPerRoute(32).build();
    }

    protected void printColor(Color color, String message) {
        String colorString;
        if (color == null || color.equals(Ansi.Color.DEFAULT)) {
            colorString = Ansi.ansi().reset().toString();
        } else {
            colorString = Ansi.ansi().fg(color).toString();
        }
        console.print(colorString);
        console.print(message);
        console.println(Ansi.ansi().reset().toString());
    }

    protected void printSuccessMessage(String message) {
        printColor(SUCCESS_COLOR, message);
    }

    protected void printErrorMessage(String message) {
        printColor(ERROR_COLOR, message);
    }

    private SSLContext getSslContext() {
        if (System.getProperty("javax.net.ssl.keyStore") == null ||
                System.getProperty("javax.net.ssl.keyStorePassword") == null ||
                System.getProperty("javax.net.ssl.trustStore") == null ||
                System.getProperty("javax.net.ssl.trustStorePassword") == null) {
            throw new IllegalArgumentException("KeyStore and TrustStore system properties must be set.");
        }

        KeyStore trustStore = getKeyStore(System.getProperty("javax.net.ssl.trustStore"),
                System.getProperty("javax.net.ssl.trustStorePassword"));
        KeyStore keyStore = getKeyStore(System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword"));

        SSLContext sslContext = null;

        try {
            sslContext = SSLContexts.custom().loadKeyMaterial(keyStore,
                    System.getProperty("javax.net.ssl.keyStorePassword").toCharArray())
                    .loadTrustMaterial(trustStore).useTLS().build();
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException |
                KeyManagementException e) {
            LOGGER.error("Unable to create secure HttpClient", e);
            return null;
        }

        sslContext.getDefaultSSLParameters().setNeedClientAuth(true);
        sslContext.getDefaultSSLParameters().setWantClientAuth(true);

        return sslContext;
    }

    private static KeyStore getKeyStore(String location, String password) {
        LOGGER.debug("Loading keystore from {}", location);
        KeyStore keyStore = null;

        try (FileInputStream storeStream = new FileInputStream(location)) {
            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(storeStream, password.toCharArray());
        } catch (CertificateException | IOException
                | NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("Unable to load keystore at " + location, e);
        }

        return keyStore;
    }

    private String[] getProtocols() {
        if (System.getProperty("https.protocols") != null) {
            return StringUtils.split(System.getProperty("https.protocols"), ",");
        } else {
            return SolrServerFactory.DEFAULT_PROTOCOLS;
        }
    }

    private static String[] getCipherSuites() {
        if (System.getProperty("https.cipherSuites") != null) {
            return StringUtils.split(System.getProperty("https.cipherSuites"), ",");
        } else {
            return SolrServerFactory.DEFAULT_CIPHER_SUITES;
        }
    }

}
