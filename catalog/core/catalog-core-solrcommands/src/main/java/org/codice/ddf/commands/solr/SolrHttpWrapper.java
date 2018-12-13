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

import ddf.security.SecurityConstants;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrHttpWrapper implements HttpWrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrHttpWrapper.class);

  HttpClient solrClient;

  public SolrHttpWrapper() {

    SSLConnectionSocketFactory sslConnectionSocketFactory =
        new SSLConnectionSocketFactory(
            getSslContext(),
            getProtocols(),
            getCipherSuites(),
            SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

    solrClient =
        HttpClients.custom()
            .setSSLSocketFactory(sslConnectionSocketFactory)
            .setDefaultCookieStore(new BasicCookieStore())
            .setMaxConnTotal(128)
            .setMaxConnPerRoute(32)
            .build();
  }

  private static KeyStore getKeyStore(String location, String password) {
    LOGGER.debug("Loading keystore from {}", location);
    KeyStore keyStore = null;

    try (FileInputStream storeStream = new FileInputStream(location)) {
      keyStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));
      keyStore.load(storeStream, password.toCharArray());
    } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
      LOGGER.warn("Unable to load keystore at {}", location, e);
    }

    return keyStore;
  }

  private String[] getCipherSuites() {
    return AccessController.doPrivileged(
        (PrivilegedAction<String[]>)
            () -> commaSeparatedToArray(System.getProperty("https.cipherSuites")));
  }

  @Override
  public ResponseWrapper execute(URI uri) {
    HttpResponse httpResponse;
    HttpGet get = new HttpGet(uri);
    try {
      LOGGER.debug("Executing uri: {}", uri);
      httpResponse = solrClient.execute(get);
      return new ResponseWrapper(httpResponse);
    } catch (IOException e) {
      LOGGER.debug("Error during request. Returning null response.");
    }
    BasicHttpResponse response =
        new BasicHttpResponse(null, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error during request.");
    return new ResponseWrapper(response);
  }

  private SSLContext getSslContext() {
    String keystorePath = System.getProperty(SecurityConstants.KEYSTORE_PATH);
    String keystorePassword = System.getProperty(SecurityConstants.KEYSTORE_PASSWORD);
    String truststorePath = System.getProperty(SecurityConstants.TRUSTSTORE_PATH);
    String truststorePassword = System.getProperty(SecurityConstants.TRUSTSTORE_PASSWORD);
    if (keystorePath == null
        || keystorePassword == null
        || truststorePath == null
        || truststorePassword == null) {
      throw new IllegalArgumentException("KeyStore and TrustStore system properties must be set.");
    }

    KeyStore trustStore = getKeyStore(truststorePath, truststorePassword);
    KeyStore keyStore = getKeyStore(keystorePath, keystorePassword);

    SSLContext sslContext;

    try {
      sslContext =
          SSLContexts.custom()
              .loadKeyMaterial(keyStore, keystorePassword.toCharArray())
              .loadTrustMaterial(trustStore)
              .useTLS()
              .build();
    } catch (UnrecoverableKeyException
        | NoSuchAlgorithmException
        | KeyStoreException
        | KeyManagementException e) {
      LOGGER.error(
          "Unable to create secure HttpClient for Solr. The server should not be used in this state.",
          e);
      return null;
    }

    sslContext.getDefaultSSLParameters().setNeedClientAuth(true);
    sslContext.getDefaultSSLParameters().setWantClientAuth(true);

    return sslContext;
  }

  private String[] getProtocols() {
    return AccessController.doPrivileged(
        (PrivilegedAction<String[]>)
            () -> commaSeparatedToArray(System.getProperty("https.protocols")));
  }

  private static String[] commaSeparatedToArray(@Nullable String commaDelimitedString) {
    return (commaDelimitedString != null) ? commaDelimitedString.split("\\s*,\\s*") : new String[0];
  }
}
