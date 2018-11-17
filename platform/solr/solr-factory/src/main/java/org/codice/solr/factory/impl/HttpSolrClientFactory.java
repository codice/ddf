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

import static org.codice.solr.factory.impl.HttpSolrClientFactory.ProtectedSolrSettings.getKeyStorePass;
import static org.codice.solr.factory.impl.HttpSolrClientFactory.ProtectedSolrSettings.getKeyStoreType;
import static org.codice.solr.factory.impl.HttpSolrClientFactory.ProtectedSolrSettings.getTrustStore;
import static org.codice.solr.factory.impl.HttpSolrClientFactory.ProtectedSolrSettings.getTrustStorePass;
import static org.codice.solr.factory.impl.HttpSolrClientFactory.ProtectedSolrSettings.isSslConfigured;

import com.google.common.annotations.VisibleForTesting;
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
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.PreemptiveAuth;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class used to create new {@link HttpSolrClient} instances.
 *
 * @deprecated This class may be removed in the future
 */
@Deprecated
public final class HttpSolrClientFactory implements SolrClientFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpSolrClientFactory.class);
  private SolrSettings solrSettings = new SolrSettings();

  private boolean solrCoreExists(SolrClient client, String coreName)
      throws IOException, SolrServerException {
    CoreAdminResponse response = CoreAdminRequest.getStatus(coreName, client);
    return response.getCoreStatus(coreName).get("instanceDir") != null;
  }

  @Override
  public org.codice.solr.client.solrj.SolrClient newClient(String coreName) {
    Validate.notEmpty(coreName, "Solr core name is missing. Cannot create Solr client.");
    LOGGER.debug(
        "Solr({}): Creating an HTTP Solr client using url [{}]",
        coreName,
        solrSettings.getCoreUrl(coreName));
    return new SolrClientAdapter(coreName, () -> createSolrHttpClient(coreName));
  }

  @VisibleForTesting
  SolrClient createSolrHttpClient(String coreName) throws IOException, SolrServerException {
    final HttpClientBuilder httpClientBuilder = createHttpBuilder();
    final HttpSolrClient.Builder solrClientBuilder =
        new HttpSolrClient.Builder(solrSettings.getCoreUrl(coreName));

    try (final Closer closer = new Closer()) {

      createSolrCore(coreName, closer.with(httpClientBuilder.build()));

      CloseableHttpClient noRetryHttpClient = httpClientBuilder.build();
      closer.with(noRetryHttpClient);
      CloseableHttpClient yesRetryHttpClient =
          httpClientBuilder.setRetryHandler(new SolrHttpRequestRetryHandler(coreName)).build();
      closer.with(yesRetryHttpClient);
      final HttpSolrClient noRetrySolrClient =
          solrClientBuilder.withHttpClient(noRetryHttpClient).build();
      final HttpSolrClient retrySolrClient =
          solrClientBuilder.withHttpClient(yesRetryHttpClient).build();
      return closer.returning(new PingAwareSolrClientProxy(retrySolrClient, noRetrySolrClient));
    }
  }

  private SSLContext getSslContext() {

    if (!isSslConfigured()) {
      throw new IllegalArgumentException("KeyStore and TrustStore system properties must be set.");
    }

    KeyStore trustStore = getKeyStore(getTrustStore(), getTrustStorePass());
    KeyStore keyStore = getKeyStore(ProtectedSolrSettings.getKeyStore(), getKeyStorePass());
    SSLContext sslContext;
    try {
      sslContext =
          SSLContexts.custom()
              .loadKeyMaterial(keyStore, getKeyStorePass().toCharArray())
              .loadTrustMaterial(trustStore)
              .useTLS()
              .build();
    } catch (UnrecoverableKeyException
        | NoSuchAlgorithmException
        | KeyStoreException
        | KeyManagementException e) {
      throw new IllegalArgumentException(
          "Unable to use javax.net.ssl.keyStorePassword to load key material to create SSL context for Solr client.");
    }
    sslContext.getDefaultSSLParameters().setWantClientAuth(true);
    return sslContext;
  }

  private KeyStore getKeyStore(String location, String password) {
    LOGGER.debug("Loading keystore from {}", location);
    KeyStore keyStore = null;
    try (FileInputStream storeStream = new FileInputStream(location)) {
      keyStore = KeyStore.getInstance(getKeyStoreType());
      keyStore.load(storeStream, password.toCharArray());
    } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
      LOGGER.warn("Unable to load keystore at {}", location, e);
    }

    return keyStore;
  }

  @VisibleForTesting
  void createSolrCore(String coreName, CloseableHttpClient httpClient)
      throws IOException, SolrServerException {
    Validate.isTrue(
        solrSettings.isSolrDataDirWritable(),
        "The solr data dir is not configured or is not writable. Cannot create core.");
    String solrUrl = solrSettings.getUrl();
    try (CloseableHttpClient closeableHttpClient = httpClient; // to make sure it gets closed
        HttpSolrClient client =
            (httpClient != null
                ? new HttpSolrClient.Builder(solrUrl).withHttpClient(httpClient).build()
                : new HttpSolrClient.Builder(solrUrl).build())) {

      HttpResponse ping = client.getHttpClient().execute(new HttpHead(solrUrl));
      if (ping != null && ping.getStatusLine().getStatusCode() == 200) {
        ConfigurationFileProxy configProxy = new ConfigurationFileProxy();
        configProxy.writeSolrConfiguration(coreName);
        if (!solrCoreExists(client, coreName)) {
          LOGGER.debug("Solr({}): Creating Solr core", coreName);
          String dataDir = solrSettings.getCoreDataDir(coreName);
          CoreAdminRequest.createCore(
              coreName,
              solrSettings.getCoreDir(coreName),
              client,
              solrSettings.getDefaultSolrconfigXml(),
              solrSettings.getDefaultSchemaXml(),
              dataDir,
              dataDir);
        } else {
          LOGGER.debug("Solr({}): Solr core already exists; reloading it", coreName);
          CoreAdminRequest.reloadCore(coreName, client);
        }
      } else {
        LOGGER.debug("Solr({}): Unable to ping Solr core at {}", coreName, solrUrl);
        throw new SolrServerException("Unable to ping Solr core");
      }
    }
  }

  private HttpClientBuilder createHttpBuilder() {

    HttpClientBuilder httpClientBuilder =
        HttpClients.custom()
            .setDefaultCookieStore(new BasicCookieStore())
            .setMaxConnTotal(128)
            .setMaxConnPerRoute(32);

    if (solrSettings.useTls()) {
      httpClientBuilder.setSSLSocketFactory(
          new SSLConnectionSocketFactory(
              getSslContext(),
              solrSettings.getSupportedProtocols(),
              solrSettings.getSupportedCipherSuites(),
              SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER));
    }

    if (solrSettings.useBasicAuth()) {
      httpClientBuilder.setDefaultCredentialsProvider(getCredentialsProvider());
      httpClientBuilder.addInterceptorFirst(new PreemptiveAuth(new BasicScheme()));
    }

    return httpClientBuilder;
  }

  private CredentialsProvider getCredentialsProvider() {
    CredentialsProvider provider = new BasicCredentialsProvider();

    org.apache.http.auth.UsernamePasswordCredentials credentials =
        new org.apache.http.auth.UsernamePasswordCredentials(
            solrSettings.getSolrUsername(), solrSettings.getPlainTextSolrPassword());
    provider.setCredentials(AuthScope.ANY, credentials);
    return provider;
  }

  /** Class to offload configuration-related tasks from the HttpSolrClientFactory. */
  static final class ProtectedSolrSettings {

    private static String keyStoreType;
    private static String keyStore;
    private static String keyStorePass;
    private static String trustStore;
    private static String trustStorePass;

    static {
      trustStore =
          AccessController.doPrivileged(
              (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.trustStore"));
      trustStorePass =
          AccessController.doPrivileged(
              (PrivilegedAction<String>)
                  () -> System.getProperty("javax.net.ssl.trustStorePassword"));
      keyStore =
          AccessController.doPrivileged(
              (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.keyStore"));
      keyStorePass =
          AccessController.doPrivileged(
              (PrivilegedAction<String>)
                  () -> System.getProperty("javax.net.ssl.keyStorePassword"));
      keyStoreType =
          AccessController.doPrivileged(
              (PrivilegedAction<String>) () -> System.getProperty("javax.net.ssl.keyStoreType"));
    }

    /** This class is not meant to be instantiated. */
    private ProtectedSolrSettings() {}

    static boolean isSslConfigured() {
      return Stream.of(getKeyStore(), getKeyStorePass(), getTrustStore(), getTrustStorePass())
          .allMatch(StringUtils::isNotEmpty);
    }

    static String getKeyStoreType() {
      return keyStoreType;
    }

    static String getKeyStore() {
      return keyStore;
    }

    static String getKeyStorePass() {
      return keyStorePass;
    }

    static String getTrustStore() {
      return trustStore;
    }

    static String getTrustStorePass() {
      return trustStorePass;
    }
  }
}
