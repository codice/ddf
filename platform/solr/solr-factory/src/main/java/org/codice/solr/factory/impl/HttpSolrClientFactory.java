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
import ddf.platform.solr.security.SolrPasswordUpdate;
import ddf.security.encryption.EncryptionService;
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
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.PreemptiveAuth;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class used to create new {@link HttpSolrClient} instances. <br>
 * Uses the following system properties when creating an instance:
 *
 * <ul>
 *   <li>solr.data.dir: Absolute path to the directory where the Solr data will be stored
 *   <li>solr.http.url: Solr server URL
 *   <li>org.codice.ddf.system.threadPoolSize: Solr query thread pool size
 *   <li>https.protocols: Secure protocols supported by the Solr server
 *   <li>https.cipherSuites: Cipher suites supported by the Solr server
 * </ul>
 */
@Deprecated
public final class HttpSolrClientFactory implements SolrClientFactory {

  public static final String DEFAULT_SCHEMA_XML = "schema.xml";
  public static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml";
  private static final String HTTPS_PROTOCOLS = "https.protocols";
  private static final String HTTPS_CIPHER_SUITES = "https.cipherSuites";
  private static final String SOLR_CONTEXT = "/solr";
  private static final String SOLR_DATA_DIR = "solr.data.dir";
  private static final String SOLR_HTTP_URL = "solr.http.url";
  private static final String KEY_STORE_PASS = "javax.net.ssl.keyStorePassword";
  private static final String TRUST_STORE = "javax.net.ssl.trustStore";
  private static final String TRUST_STORE_PASS = "javax.net.ssl.trustStorePassword";
  private static final String KEY_STORE = "javax.net.ssl.keyStore";
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpSolrClientFactory.class);

  private final SolrPasswordUpdate solrPasswordUpdate;
  private final EncryptionService encryptionService;

  public HttpSolrClientFactory(
      EncryptionService encryptionService, SolrPasswordUpdate solrPasswordUpdate) {
    this.encryptionService = encryptionService;
    this.solrPasswordUpdate = solrPasswordUpdate;
  }

  @VisibleForTesting
  HttpSolrClientFactory() {
    solrPasswordUpdate = null;
    encryptionService = null;
  }

  /**
   * Gets the default Solr server secure HTTP address.
   *
   * @return Solr server secure HTTP address
   */
  public static String getDefaultHttpsAddress() {
    return SystemBaseUrl.INTERNAL.constructUrl("https", SOLR_CONTEXT);
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
    } catch (UnrecoverableKeyException
        | NoSuchAlgorithmException
        | KeyStoreException
        | KeyManagementException e) {
      throw new IllegalArgumentException(
          "Unable to use javax.net.ssl.keyStorePassword to load key material to create SSL context for Solr client.");
    }

    sslContext.getDefaultSSLParameters().setNeedClientAuth(true);
    sslContext.getDefaultSSLParameters().setWantClientAuth(true);

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

  private static void createSolrCore(
      String url, String coreName, String configFileName, CloseableHttpClient httpClient)
      throws IOException, SolrServerException {

    try (CloseableHttpClient closeableHttpClient = httpClient; // to make sure it gets closed
        HttpSolrClient client =
            (httpClient != null
                ? new HttpSolrClient.Builder(url).withHttpClient(httpClient).build()
                : new HttpSolrClient.Builder(url).build())) {

      HttpResponse ping = client.getHttpClient().execute(new HttpHead(url));
      if (ping != null && ping.getStatusLine().getStatusCode() == 200) {
        ConfigurationFileProxy configProxy =
            new ConfigurationFileProxy(ConfigurationStore.getInstance());
        configProxy.writeSolrConfiguration(coreName);
        if (!solrCoreExists(client, coreName)) {
          LOGGER.debug("Solr({}): Creating Solr core", coreName);

          String configFile = StringUtils.defaultIfBlank(configFileName, DEFAULT_SOLRCONFIG_XML);
          String solrDir;

          if (AccessController.doPrivileged(
              (PrivilegedAction<Boolean>) () -> System.getProperty(SOLR_DATA_DIR) != null)) {
            solrDir =
                AccessController.doPrivileged(
                    (PrivilegedAction<String>) () -> System.getProperty(SOLR_DATA_DIR));
          } else {
            solrDir =
                Paths.get(
                        AccessController.doPrivileged(
                            (PrivilegedAction<String>) () -> System.getProperty("karaf.home")),
                        "data",
                        "solr")
                    .toString();
          }

          String instanceDir = Paths.get(solrDir, coreName).toString();

          String dataDir = Paths.get(instanceDir, "data").toString();

          CoreAdminRequest.createCore(
              coreName, instanceDir, client, configFile, DEFAULT_SCHEMA_XML, dataDir, dataDir);
        } else {
          LOGGER.debug("Solr({}): Solr core already exists; reloading it", coreName);
          CoreAdminRequest.reloadCore(coreName, client);
        }
      } else {
        LOGGER.debug("Solr({}): Unable to ping Solr core at {}", coreName, url);
        throw new SolrServerException("Unable to ping Solr core");
      }
    }
  }

  private static boolean solrCoreExists(SolrClient client, String coreName)
      throws IOException, SolrServerException {
    CoreAdminResponse response = CoreAdminRequest.getStatus(coreName, client);
    return response.getCoreStatus(coreName).get("instanceDir") != null;
  }

  private static String[] commaSeparatedToArray(@Nullable String commaDelimitedString) {
    return (commaDelimitedString != null) ? commaDelimitedString.split("\\s*,\\s*") : new String[0];
  }

  @Override
  public org.codice.solr.client.solrj.SolrClient newClient(String core) {
    String solrUrl =
        StringUtils.defaultIfBlank(
            AccessController.doPrivileged(
                (PrivilegedAction<String>) () -> System.getProperty(SOLR_HTTP_URL)),
            getDefaultHttpsAddress());
    final String coreUrl = solrUrl + "/" + core;
    final String solrDataDir =
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(SOLR_DATA_DIR));

    if (solrDataDir != null) {
      ConfigurationStore.getInstance().setDataDirectoryPath(solrDataDir);
    }
    LOGGER.debug("Solr({}): Creating an HTTP Solr client using url [{}]", core, coreUrl);
    return new SolrClientAdapter(core, () -> createSolrHttpClient(solrUrl, core, coreUrl));
  }

  @VisibleForTesting
  SolrClient createSolrHttpClient(String url, String coreName, String coreUrl)
      throws IOException, SolrServerException {
    final HttpClientBuilder builder = createHttpBuilder();

    createSolrCore(url, coreName, null, builder.build());
    try (final Closer closer = new Closer()) {
      final HttpSolrClient noRetryClient =
          closer.with(new HttpSolrClient.Builder(coreUrl).withHttpClient(builder.build()).build());
      final HttpSolrClient retryClient =
          closer.with(
              new HttpSolrClient.Builder(coreUrl)
                  .withHttpClient(
                      builder.setRetryHandler(new SolrHttpRequestRetryHandler(coreName)).build())
                  .build());

      return closer.returning(new PingAwareSolrClientProxy(retryClient, noRetryClient));
    }
  }

  public boolean useTls() {
    return StringUtils.startsWithIgnoreCase(
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(SOLR_HTTP_URL)),
        "https");
  }

  private HttpClientBuilder createHttpBuilder() {

    HttpClientBuilder httpClientBuilder =
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

    boolean useBasicAuth =
        AccessController.doPrivileged(
            (PrivilegedAction<Boolean>)
                () -> Boolean.valueOf(System.getProperty("solr.useBasicAuth")));
    if (useBasicAuth) {
      solrPasswordUpdate.updateSolrPassword();
      httpClientBuilder.setDefaultCredentialsProvider(getCredentialsProvider());
      httpClientBuilder.addInterceptorFirst(new PreemptiveAuth(new BasicScheme()));
    }
    return httpClientBuilder;
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

  private String getPlainTextSolrPassword() {
    return encryptionService.decryptValue(
        AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty("solr.password")));
  }
}
