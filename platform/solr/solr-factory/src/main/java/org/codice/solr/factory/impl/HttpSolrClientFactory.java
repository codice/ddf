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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
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
public class HttpSolrClientFactory implements SolrClientFactory {

  private static final String HTTPS_PROTOCOLS = "https.protocols";
  private static final String HTTPS_CIPHER_SUITES = "https.cipherSuites";
  private static final String SOLR_CONTEXT = "/solr";
  private static final String SOLR_DATA_DIR = "solr.data.dir";
  private static final String KEY_STORE_PASS = "javax.net.ssl.keyStorePassword";
  private static final String TRUST_STORE = "javax.net.ssl.trustStore";
  private static final String TRUST_STORE_PASS = "javax.net.ssl.trustStorePassword";
  private static final String KEY_STORE = "javax.net.ssl.keyStore";
  public static final List<String> DEFAULT_PROTOCOLS;
  public static final List<String> DEFAULT_CIPHER_SUITES;

  static {
    DEFAULT_PROTOCOLS =
        AccessController.doPrivileged(
            (PrivilegedAction<List<String>>)
                () ->
                    Collections.unmodifiableList(
                        Arrays.asList(
                            StringUtils.split(System.getProperty(HTTPS_PROTOCOLS, ""), ","))));
    DEFAULT_CIPHER_SUITES =
        AccessController.doPrivileged(
            (PrivilegedAction<List<String>>)
                () ->
                    Collections.unmodifiableList(
                        Arrays.asList(
                            StringUtils.split(System.getProperty(HTTPS_CIPHER_SUITES, ""), ","))));
  }

  public static final String DEFAULT_SCHEMA_XML = "schema.xml";

  public static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml";

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpSolrClientFactory.class);

  private static ScheduledExecutorService executor = getThreadPool();

  private static final String DEFAULT_CORE_NAME = "core1";

  private static final String THREAD_POOL_DEFAULT_SIZE = "128";

  @Override
  public Future<SolrClient> newClient(String core) {
    String solrUrl =
        AccessController.doPrivileged(
            (PrivilegedAction<String>)
                () -> System.getProperty("solr.http.url", getDefaultHttpsAddress()));
    return getHttpSolrClient(solrUrl, core);
  }

  /**
   * Gets the default Solr server secure HTTP address.
   *
   * @return Solr server secure HTTP address
   */
  public static String getDefaultHttpsAddress() {
    return SystemBaseUrl.constructUrl("https", SOLR_CONTEXT);
  }

  /**
   * Gets the default Solr server HTTP address.
   *
   * @return Solr server HTTP address
   */
  public static String getDefaultHttpAddress() {
    return SystemBaseUrl.constructUrl("http", SOLR_CONTEXT);
  }

  /**
   * Creates a new {@link HttpSolrClient} using the URL provided. Uses the default Solr core name
   * ({@value #DEFAULT_CORE_NAME}) and configuration file ({@value #DEFAULT_SOLRCONFIG_XML}).
   *
   * @param url Solr server URL. If {@code null}, defaults to the system's base URL followed by
   *     {@code /solr}.
   * @return {@code Future} used to retrieve the new {@link HttpSolrClient} instance
   */
  public static Future<SolrClient> getHttpSolrClient(@Nullable String url) {
    return getHttpSolrClient(url, DEFAULT_CORE_NAME, null);
  }

  /**
   * Creates a new {@link HttpSolrClient} using the URL and core name provided. Uses the default
   * configuration file ({@value #DEFAULT_SOLRCONFIG_XML}).
   *
   * @param url Solr server URL. If {@code null}, defaults to the system's base URL followed by
   *     {@code /solr}.
   * @param coreName name of the Solr core to create
   * @return {@code Future} used to retrieve the new {@link HttpSolrClient} instance
   */
  public static Future<SolrClient> getHttpSolrClient(@Nullable String url, String coreName) {
    return getHttpSolrClient(url, coreName, null);
  }

  /**
   * Creates a new {@link HttpSolrClient} using the URL, core name and configuration file name
   * provided.
   *
   * @param url Solr server URL. If {@code null}, defaults to the system's base URL followed by
   *     {@code /solr}.
   * @param coreName name of the Solr core to create
   * @param configFile configuration file name. If {@code null}, defaults to {@value
   *     #DEFAULT_SOLRCONFIG_XML}.
   * @return {@code Future} used to retrieve the new {@link HttpSolrClient} instance
   */
  public static Future<SolrClient> getHttpSolrClient(
      @Nullable String url, String coreName, @Nullable String configFile) {
    String solrUrl = StringUtils.defaultIfBlank(url, SystemBaseUrl.constructUrl("/solr"));
    String coreUrl = url + "/" + coreName;

    if (AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(SOLR_DATA_DIR))
        != null) {
      ConfigurationStore.getInstance()
          .setDataDirectoryPath(
              AccessController.doPrivileged(
                  (PrivilegedAction<String>) () -> System.getProperty(SOLR_DATA_DIR)));
    }

    RetryPolicy retryPolicy =
        new RetryPolicy().withBackoff(10, TimeUnit.MINUTES.toMillis(1), TimeUnit.MILLISECONDS);
    return Failsafe.with(retryPolicy)
        .with(executor)
        .onRetry(
            (c, failure, ctx) ->
                LOGGER.debug(
                    "Attempt {} failed to create HTTP Solr client ({}). Retrying again.",
                    ctx.getExecutions(),
                    coreName))
        .onFailedAttempt(
            failure ->
                LOGGER.debug(
                    "Attempt failed to create HTTP Solr client (" + coreName + ")", failure))
        .onSuccess(client -> LOGGER.debug("Successfully created HTTP Solr client ({})", coreName))
        .onFailure(
            failure ->
                LOGGER.warn(
                    "All attempts failed to create HTTP Solr client (" + coreName + ")", failure))
        .get(() -> createSolrHttpClient(solrUrl, coreName, configFile, coreUrl));
  }

  static SolrClient getHttpSolrClient() {
    return new HttpSolrClient(getDefaultHttpAddress());
  }

  private static ScheduledExecutorService getThreadPool() throws NumberFormatException {
    Integer threadPoolSize =
        Integer.parseInt(
            AccessController.doPrivileged(
                (PrivilegedAction<String>)
                    () ->
                        System.getProperty(
                            "org.codice.ddf.system.threadPoolSize", THREAD_POOL_DEFAULT_SIZE)));
    return Executors.newScheduledThreadPool(
        threadPoolSize,
        StandardThreadFactoryBuilder.newThreadFactory("httpSolrClientFactoryThread"));
  }

  private static SolrClient createSolrHttpClient(
      String url, String coreName, String configFile, String coreUrl)
      throws IOException, SolrServerException {
    SolrClient client;
    if (StringUtils.startsWith(url, "https")) {
      createSolrCore(url, coreName, configFile, getSecureHttpClient(false));
      client = new HttpSolrClient(coreUrl, getSecureHttpClient(true));
    } else {
      createSolrCore(url, coreName, configFile, null);
      client = new HttpSolrClient(coreUrl);
    }
    return client;
  }

  private static CloseableHttpClient getSecureHttpClient(boolean retryRequestsOnError) {
    SSLConnectionSocketFactory sslConnectionSocketFactory =
        new SSLConnectionSocketFactory(
            getSslContext(),
            getProtocols(),
            getCipherSuites(),
            SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
    HttpRequestRetryHandler solrRetryHandler = new SolrHttpRequestRetryHandler();

    HttpClientBuilder builder =
        HttpClients.custom()
            .setSSLSocketFactory(sslConnectionSocketFactory)
            .setDefaultCookieStore(new BasicCookieStore())
            .setMaxConnTotal(128)
            .setMaxConnPerRoute(32);

    if (retryRequestsOnError) {
      builder.setRetryHandler(solrRetryHandler);
    }

    return builder.build();
  }

  private static String[] getProtocols() {
    if (AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(HTTPS_PROTOCOLS))
        != null) {
      return StringUtils.split(
          AccessController.doPrivileged(
              (PrivilegedAction<String>) () -> System.getProperty(HTTPS_PROTOCOLS)),
          ",");
    } else {
      return DEFAULT_PROTOCOLS.toArray(new String[DEFAULT_PROTOCOLS.size()]);
    }
  }

  private static String[] getCipherSuites() {
    if (AccessController.doPrivileged(
            (PrivilegedAction<String>) () -> System.getProperty(HTTPS_CIPHER_SUITES))
        != null) {
      return StringUtils.split(
          AccessController.doPrivileged(
              (PrivilegedAction<String>) () -> System.getProperty(HTTPS_CIPHER_SUITES)),
          ",");
    } else {
      return DEFAULT_CIPHER_SUITES.toArray(new String[DEFAULT_CIPHER_SUITES.size()]);
    }
  }

  private static SSLContext getSslContext() {
    Boolean check =
        AccessController.doPrivileged(
            (PrivilegedAction<Boolean>)
                () ->
                    (System.getProperty(KEY_STORE) == null
                        || //
                        System.getProperty(KEY_STORE_PASS) == null
                        || //
                        System.getProperty(TRUST_STORE) == null
                        || //
                        System.getProperty(TRUST_STORE_PASS) == null));
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
      String url, String coreName, String configFileName, HttpClient httpClient)
      throws IOException, SolrServerException {

    try (HttpSolrClient client =
        (httpClient != null ? new HttpSolrClient(url, httpClient) : new HttpSolrClient(url))) {

      HttpResponse ping = client.getHttpClient().execute(new HttpHead(url));
      if (ping != null && ping.getStatusLine().getStatusCode() == 200) {
        ConfigurationFileProxy configProxy =
            new ConfigurationFileProxy(ConfigurationStore.getInstance());
        configProxy.writeSolrConfiguration(coreName);
        if (!solrCoreExists(client, coreName)) {
          LOGGER.debug("Creating Solr core {}", coreName);

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
          LOGGER.debug("Solr core ({}) already exists - reloading it", coreName);
          CoreAdminRequest.reloadCore(coreName, client);
        }
      } else {
        LOGGER.debug("Unable to ping Solr core {}", coreName);
        throw new SolrServerException("Unable to ping Solr core");
      }
    }
  }

  private static boolean solrCoreExists(SolrClient client, String coreName)
      throws IOException, SolrServerException {
    CoreAdminResponse response = CoreAdminRequest.getStatus(coreName, client);
    return response.getCoreStatus(coreName).get("instanceDir") != null;
  }
}
