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

import com.google.common.annotations.VisibleForTesting;
import ddf.platform.solr.credentials.api.SolrUsernamePasswordCredentials;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang.StringUtils;
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
import org.apache.http.util.Args;
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

  public static final String DEFAULT_SCHEMA_XML = "schema.xml";
  public static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml";
  private static final String HTTPS_CIPHER_SUITES;
  private static final String HTTPS_PROTOCOLS;
  private static final String KEY_STORE;
  private static final String KEY_STORE_PASS;
  public static final String KEY_STORE_TYPE;
  private static final String SOLR_DATA_DIR;
  private static final String SOLR_HTTP_URL;
  public static final String SOLR_USE_BASIC_AUTH;
  private static final String TRUST_STORE;
  private static final String TRUST_STORE_PASS;
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpSolrClientFactory.class);
  private final SolrUsernamePasswordCredentials usernamePasswordCredentials;

  static {
    HTTPS_CIPHER_SUITES = getProperty("https.cipherSuites");
    HTTPS_PROTOCOLS = getProperty("https.protocols");
    KEY_STORE = getProperty("javax.net.ssl.keyStore");
    KEY_STORE_PASS = getProperty("javax.net.ssl.keyStorePassword");
    KEY_STORE_TYPE = getProperty("javax.net.ssl.keyStoreType");
    SOLR_DATA_DIR = getProperty("solr.data.dir");
    SOLR_HTTP_URL = getProperty("solr.http.url");
    SOLR_USE_BASIC_AUTH = getProperty("solr.useBasicAuth");
    TRUST_STORE = getProperty("javax.net.ssl.trustStore");
    TRUST_STORE_PASS = getProperty("javax.net.ssl.trustStorePassword");
  }

  public HttpSolrClientFactory(SolrUsernamePasswordCredentials usernamePasswordCredentials) {
    this.usernamePasswordCredentials = usernamePasswordCredentials;
  }

  @Override
  public org.codice.solr.client.solrj.SolrClient newClient(String coreName) {
    Args.notEmpty(coreName, "Cannot create Solr client. Solr core name");
    ConfigurationStore.getInstance().setDataDirectoryPath(SOLR_DATA_DIR);
    LOGGER.debug(
        "Solr({}): Creating an HTTP Solr client using url [{}]", coreName, getCoreUrl(coreName));
    return new SolrClientAdapter(coreName, () -> createSolrHttpClient(coreName));
  }

  @VisibleForTesting
  SolrClient createSolrHttpClient(String coreName) throws IOException, SolrServerException {
    final HttpClientBuilder httpClientBuilder = createHttpBuilder();
    final HttpSolrClient.Builder solrClientBuilder =
        new HttpSolrClient.Builder(getCoreUrl(coreName));

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

  private boolean useTls() {
    return StringUtils.startsWithIgnoreCase(SOLR_HTTP_URL, "https");
  }

  /**
   * Gets the default Solr server secure HTTP address.
   *
   * @return Solr server secure HTTP address
   */
  public static String getDefaultHttpsAddress() {
    return SOLR_HTTP_URL;
  }

  private SSLContext getSslContext() {

    if (!isSslConfigured()) {
      throw new IllegalArgumentException("KeyStore and TrustStore system properties must be set.");
    }

    KeyStore trustStore = getKeyStore(TRUST_STORE, TRUST_STORE_PASS);
    KeyStore keyStore = getKeyStore(KEY_STORE, KEY_STORE_PASS);
    SSLContext sslContext;
    try {
      sslContext =
          SSLContexts.custom()
              .loadKeyMaterial(keyStore, KEY_STORE_PASS.toCharArray())
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

  private boolean isSslConfigured() {
    return Stream.of(KEY_STORE, KEY_STORE_PASS, TRUST_STORE, TRUST_STORE_PASS)
        .noneMatch(StringUtils::isNotEmpty);
  }

  private KeyStore getKeyStore(String location, String password) {
    LOGGER.debug("Loading keystore from {}", location);
    KeyStore keyStore = null;
    try (FileInputStream storeStream = new FileInputStream(location)) {
      keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
      keyStore.load(storeStream, password.toCharArray());
    } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException e) {
      LOGGER.warn("Unable to load keystore at {}", location, e);
    }

    return keyStore;
  }

  private void createSolrCore(String coreName, CloseableHttpClient httpClient)
      throws IOException, SolrServerException {

    String solrUrl = SOLR_HTTP_URL;
    try (CloseableHttpClient closeableHttpClient = httpClient; // to make sure it gets closed
        HttpSolrClient client =
            (httpClient != null
                ? new HttpSolrClient.Builder(solrUrl).withHttpClient(httpClient).build()
                : new HttpSolrClient.Builder(solrUrl).build())) {

      HttpResponse ping = client.getHttpClient().execute(new HttpHead(solrUrl));
      if (ping != null && ping.getStatusLine().getStatusCode() == 200) {
        ConfigurationFileProxy configProxy =
            new ConfigurationFileProxy(ConfigurationStore.getInstance());
        configProxy.writeSolrConfiguration(coreName);
        if (!solrCoreExists(client, coreName)) {
          LOGGER.debug("Solr({}): Creating Solr core", coreName);
          String dataDir = getCoreDataDir(coreName);
          CoreAdminRequest.createCore(
              coreName,
              getCoreDir(coreName),
              client,
              DEFAULT_SOLRCONFIG_XML,
              DEFAULT_SCHEMA_XML,
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

    if (useTls()) {
      httpClientBuilder.setSSLSocketFactory(
          new SSLConnectionSocketFactory(
              getSslContext(),
              getSupportedProtocols(),
              getSupportedCipherSuites(),
              SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER));
    }

    if (useBasicAuth()) {
      httpClientBuilder.setDefaultCredentialsProvider(getCredentialsProvider());
      httpClientBuilder.addInterceptorFirst(new PreemptiveAuth(new BasicScheme()));
    }

    return httpClientBuilder;
  }

  /**
   * This method retrieves the system property using elevated privileges. The information returned
   * by this method is visible to any object that can call the method. The supported cipher suites
   * do not need this protection and therefore this method is not leaking sensitive information.
   *
   * @return supported cipher suites as an array
   */
  public static String[] getSupportedCipherSuites() {
    return commaSeparatedToArray(HTTPS_CIPHER_SUITES);
  }

  /**
   * This method retrieves the system property using elevated privileges. The information returned
   * by this method is visible to any object that can call the method. The supported protocols do
   * not need this protection and therefore this method is not leaking sensitive information.
   *
   * @return supported cipher suites as an array
   */
  public static String[] getSupportedProtocols() {
    return commaSeparatedToArray(HTTPS_PROTOCOLS);
  }

  private boolean useBasicAuth() {
    return Boolean.valueOf(SOLR_USE_BASIC_AUTH);
  }

  private String getCoreUrl(String coreName) {
    return SOLR_HTTP_URL + "/" + coreName;
  }

  private CredentialsProvider getCredentialsProvider() {
    CredentialsProvider provider = new BasicCredentialsProvider();
    org.apache.http.auth.UsernamePasswordCredentials credentials =
        new org.apache.http.auth.UsernamePasswordCredentials(
            usernamePasswordCredentials.getUsername(), usernamePasswordCredentials.getPassword());
    provider.setCredentials(AuthScope.ANY, credentials);
    return provider;
  }

  private String getCoreDataDir(String coreName) {
    return concatenatePaths(getCoreDir(coreName), "data");
  }

  private String getCoreDir(String coreName) {
    return concatenatePaths(SOLR_DATA_DIR, coreName);
  }

  private static String concatenatePaths(String first, String more) {
    return Paths.get(first, more).toString();
  }

  private static String getProperty(String propertyName) {
    return AccessController.doPrivileged(
        (PrivilegedAction<String>) () -> System.getProperty(propertyName));
  }

  private static boolean solrCoreExists(SolrClient client, String coreName)
      throws IOException, SolrServerException {
    CoreAdminResponse response = CoreAdminRequest.getStatus(coreName, client);
    return response.getCoreStatus(coreName).get("instanceDir") != null;
  }

  private static String[] commaSeparatedToArray(@Nullable String commaDelimitedString) {

    return (commaDelimitedString != null) ? commaDelimitedString.split("\\s*,\\s*") : new String[0];
  }
}
