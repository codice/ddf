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
package org.codice.solr.factory.impl;

import ddf.security.encryption.EncryptionService;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang.StringUtils;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.impl.PreemptiveAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientBuilder implements Supplier<org.apache.http.impl.client.HttpClientBuilder> {

  private static final String KEY_STORE_PASS = "javax.net.ssl.keyStorePassword";
  private static final String TRUST_STORE = "javax.net.ssl.trustStore";
  private static final String TRUST_STORE_PASS = "javax.net.ssl.trustStorePassword";
  private static final String KEY_STORE = "javax.net.ssl.keyStore";
  private static final String HTTPS_PROTOCOLS = "https.protocols";
  private static final String HTTPS_CIPHER_SUITES = "https.cipherSuites";
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientBuilder.class);
  private static final String SOLR_HTTP_URL = "solr.http.url";
  private final EncryptionService encryptionService;

  public HttpClientBuilder(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  public org.apache.http.impl.client.HttpClientBuilder get() {

    final org.apache.http.impl.client.HttpClientBuilder httpClientBuilder =
        HttpClients.custom()
            .setDefaultCookieStore(new BasicCookieStore())
            .setMaxConnTotal(128)
            .setMaxConnPerRoute(32);

    if (useTls()) {
      String[] defaultProtocols =
          AccessController.doPrivileged(
              (PrivilegedAction<String[]>)
                  () -> commaSeparatedToArray(System.getProperty(HTTPS_PROTOCOLS)));

      String[] defaultCipherSuites =
          AccessController.doPrivileged(
              (PrivilegedAction<String[]>)
                  () -> commaSeparatedToArray(System.getProperty(HTTPS_CIPHER_SUITES)));

      httpClientBuilder.setSSLSocketFactory(
          new SSLConnectionSocketFactory(
              getSslContext(),
              defaultProtocols,
              defaultCipherSuites,
              SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER));
    }
    if (isConfiguredForBasicAuth()) {
      httpClientBuilder.setDefaultCredentialsProvider(getCredentialsProvider());
      httpClientBuilder.addInterceptorFirst(new PreemptiveAuth(new BasicScheme()));
    }
    return httpClientBuilder;
  }

  private Boolean isConfiguredForBasicAuth() {
    return AccessController.doPrivileged(
        (PrivilegedAction<Boolean>) () -> Boolean.valueOf(System.getProperty("solr.useBasicAuth")));
  }

  private boolean useTls() {
    return StringUtils.startsWithIgnoreCase(
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(SOLR_HTTP_URL)),
        "https");
  }

  private CredentialsProvider getCredentialsProvider() {
    CredentialsProvider provider = new BasicCredentialsProvider();

    org.apache.http.auth.UsernamePasswordCredentials credentials =
        new org.apache.http.auth.UsernamePasswordCredentials(
            AccessController.doPrivileged(
                (PrivilegedAction<String>) () -> System.getProperty("solr.username")),
            getPlainTextSolrPassword());
    provider.setCredentials(AuthScope.ANY, credentials);
    return provider;
  }

  private static SSLContext getSslContext() {
    final Boolean check =
        AccessController.doPrivileged(
            (PrivilegedAction<Boolean>)
                () ->
                    (System.getProperty(KEY_STORE) == null
                        || System.getProperty(KEY_STORE_PASS) == null
                        || System.getProperty(TRUST_STORE) == null
                        || System.getProperty(TRUST_STORE_PASS) == null));

    if (check) {
      throw new IllegalArgumentException("KeyStore and TrustStore system properties must be set.");
    }

    final KeyStore[] trustStore = new KeyStore[1];
    final KeyStore[] keyStore = new KeyStore[1];

    AccessController.doPrivileged(
        (PrivilegedAction<Object>)
            () -> {
              trustStore[0] =
                  getKeyStore(
                      System.getProperty(TRUST_STORE), System.getProperty(TRUST_STORE_PASS));
              keyStore[0] =
                  getKeyStore(System.getProperty(KEY_STORE), System.getProperty(KEY_STORE_PASS));
              return null;
            });

    SSLContext sslContext = null;

    try {
      sslContext =
          SSLContexts.custom()
              .loadKeyMaterial(
                  keyStore[0],
                  AccessController.doPrivileged(
                          (PrivilegedAction<String>) () -> System.getProperty(KEY_STORE_PASS))
                      .toCharArray())
              .loadTrustMaterial(trustStore[0])
              .useTLS()
              .build();
      sslContext.getDefaultSSLParameters().setNeedClientAuth(true);
      sslContext.getDefaultSSLParameters().setWantClientAuth(true);
    } catch (UnrecoverableKeyException
        | NoSuchAlgorithmException
        | KeyStoreException
        | KeyManagementException e) {
      throw new IllegalArgumentException(
          "Unable to use javax.net.ssl.keyStorePassword to load key material to create SSL context for Solr client.");
    }

    return sslContext;
  }

  private static KeyStore getKeyStore(String location, String password) {
    LOGGER.debug("Loading keystore from {}", location);
    KeyStore keyStore = null;

    try (FileInputStream storeStream = new FileInputStream(location)) {
      keyStore =
          KeyStore.getInstance(
              AccessController.doPrivileged(
                  (PrivilegedAction<String>)
                      () -> System.getProperty("javax.net.ssl.keyStoreType")));
      keyStore.load(storeStream, password.toCharArray());
    } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
      LOGGER.warn("Unable to load keystore at {}", location, e);
    }

    return keyStore;
  }

  private static String[] commaSeparatedToArray(@Nullable String commaDelimitedString) {
    return (commaDelimitedString != null) ? commaDelimitedString.split("\\s*,\\s*") : new String[0];
  }

  private String getPlainTextSolrPassword() {
    return encryptionService.decryptValue(
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.password")));
  }
}
