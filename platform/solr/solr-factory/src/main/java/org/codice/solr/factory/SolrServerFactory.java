/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.solr.factory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DurationFormatUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

/**
 * Factory that creates {@link org.apache.solr.client.solrj.SolrClient} instances.
 */
public class SolrServerFactory {

    protected static final Logger LOGGER = LoggerFactory.getLogger(SolrServerFactory.class);

    private static ExecutorService pool = getThreadPool();

    public static final String DEFAULT_CORE_NAME = "core1";

    public static final List<String> DEFAULT_PROTOCOLS = Collections.unmodifiableList(Arrays.asList(
            StringUtils.split(System.getProperty("https.protocols"), ",")));

    public static final List<String> DEFAULT_CIPHER_SUITES =
            Collections.unmodifiableList(Arrays.asList(StringUtils.split(System.getProperty(
                    "https.cipherSuites"), ",")));

    public static final String DEFAULT_SCHEMA_XML = "schema.xml";

    public static final String DEFAULT_SOLRCONFIG_XML = "solrconfig.xml";

    private static final Integer MAX_RETRY_COUNT = 11;

    private static final String THREAD_POOL_DEFAULT_SIZE = "128";

    private static ExecutorService getThreadPool() throws NumberFormatException {
        Integer threadPoolSize = Integer.parseInt(System.getProperty(
                    "org.codice.ddf.system.threadPoolSize", THREAD_POOL_DEFAULT_SIZE));
        return Executors.newFixedThreadPool(threadPoolSize);
    }

    public static String getDefaultHttpsAddress() {
        return SystemBaseUrl.constructUrl("https", "/solr");
    }

    public static String getDefaultHttpAddress() {
        return SystemBaseUrl.constructUrl("http", "/solr");
    }

    /**
     * Creates an {@link org.apache.solr.client.solrj.SolrClient} with the default http address
     * url.
     *
     * @return SolrClient
     */
    static SolrClient getHttpSolrServer() {
        return new HttpSolrClient(getDefaultHttpAddress());
    }

    public static Future<SolrClient> getHttpSolrServer(String url) {
        return getHttpSolrServer(url, DEFAULT_CORE_NAME, null);
    }

    public static Future<SolrClient> getHttpSolrServer(String url, String coreName) {
        return getHttpSolrServer(url, coreName, null);
    }

    public static Future<SolrClient> getHttpSolrServer(String url, String coreName,
            String configFile) {
        if (StringUtils.isBlank(url)) {
            url = SystemBaseUrl.constructUrl("/solr");
        }

        String coreUrl = url + "/" + coreName;
        SolrClient client;
        try {
            client = getSolrServer(url, coreName, configFile, coreUrl);
        } catch (Exception ex) {
            LOGGER.info("Returning future for HTTP Solr client ({})", coreName);
            LOGGER.debug("Failed to create Solr client (" + coreName + ")", ex);
            return pool.submit(new SolrClientFetcher(url, coreName, configFile, coreUrl));
        }

        LOGGER.info("Created HTTP Solr client ({})", coreName);
        return Futures.immediateFuture(client);
    }

    private static SolrClient getSolrServer(String url, String coreName, String configFile,
            String coreUrl) throws IOException, SolrServerException {
        SolrClient server;
        if (StringUtils.startsWith(url, "https")) {
            createSolrCore(url, coreName, configFile, getHttpClient(false));
            server = new HttpSolrClient(coreUrl, getHttpClient(true));
        } else {
            createSolrCore(url, coreName, configFile, null);
            server = new HttpSolrClient(coreUrl);
        }
        return server;
    }

    private static CloseableHttpClient getHttpClient(boolean retryRequestsOnError) {
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                getSslContext(),
                getProtocols(),
                getCipherSuites(),
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        HttpRequestRetryHandler solrRetryHandler = new SolrServerHttpRequestRetryHandler();

        HttpClientBuilder builder = HttpClients.custom()
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
        if (System.getProperty("https.protocols") != null) {
            return StringUtils.split(System.getProperty("https.protocols"), ",");
        } else {
            return DEFAULT_PROTOCOLS.toArray(new String[DEFAULT_PROTOCOLS.size()]);
        }
    }

    private static String[] getCipherSuites() {
        if (System.getProperty("https.cipherSuites") != null) {
            return StringUtils.split(System.getProperty("https.cipherSuites"), ",");
        } else {
            return DEFAULT_CIPHER_SUITES.toArray(new String[DEFAULT_CIPHER_SUITES.size()]);
        }
    }

    private static SSLContext getSslContext() {
        if (System.getProperty("javax.net.ssl.keyStore") == null ||
                System.getProperty("javax.net.ssl.keyStorePassword") == null ||
                System.getProperty("javax.net.ssl.trustStore") == null ||
                System.getProperty("javax.net.ssl.trustStorePassword") == null) {
            throw new IllegalArgumentException(
                    "KeyStore and TrustStore system properties must be" + " set.");
        }

        KeyStore trustStore = getKeyStore(System.getProperty("javax.net.ssl.trustStore"),
                System.getProperty("javax.net.ssl.trustStorePassword"));
        KeyStore keyStore = getKeyStore(System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword"));

        SSLContext sslContext = null;

        try {
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore,
                            System.getProperty("javax.net.ssl.keyStorePassword")
                                    .toCharArray())
                    .loadTrustMaterial(trustStore)
                    .useTLS()
                    .build();
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException |
                KeyManagementException e) {
            LOGGER.error("Unable to create secure HttpClient", e);
            return null;
        }

        sslContext.getDefaultSSLParameters()
                .setNeedClientAuth(true);
        sslContext.getDefaultSSLParameters()
                .setWantClientAuth(true);

        return sslContext;
    }

    private static KeyStore getKeyStore(String location, String password) {
        LOGGER.debug("Loading keystore from {}", location);
        KeyStore keyStore = null;

        try (FileInputStream storeStream = new FileInputStream(location)) {
            keyStore = KeyStore.getInstance(System.getProperty("javax.net.ssl.keyStoreType"));
            keyStore.load(storeStream, password.toCharArray());
        } catch (CertificateException | IOException
                | NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("Unable to load keystore at " + location, e);
        }

        return keyStore;
    }

    private static void createSolrCore(String url, String coreName, String configFileName,
            HttpClient httpClient) throws IOException, SolrServerException {
        HttpSolrClient client;
        if (httpClient != null) {
            client = new HttpSolrClient(url, httpClient);
        } else {
            client = new HttpSolrClient(url);
        }

        HttpResponse ping = client.getHttpClient()
                .execute(new HttpHead(url));
        if (ping != null && ping.getStatusLine()
                .getStatusCode() == 200) {
            if (!solrCoreExists(client, coreName)) {
                LOGGER.debug("Creating Solr core {}", coreName);

                String configFile = StringUtils.defaultIfBlank(configFileName,
                        DEFAULT_SOLRCONFIG_XML);

                String instanceDir = Paths.get(System.getProperty("karaf.home"),
                        "data",
                        "solr",
                        coreName)
                        .toString();

                CoreAdminRequest.createCore(coreName,
                        instanceDir,
                        client,
                        configFile,
                        DEFAULT_SCHEMA_XML);
            } else {
                LOGGER.debug("Solr core ({}) already exists - reloading it", coreName);
                CoreAdminRequest.reloadCore(coreName, client);
            }
        }

    }

    private static boolean solrCoreExists(SolrClient client, String coreName) {
        try {
            CoreAdminResponse response = CoreAdminRequest.getStatus(coreName, client);
            return response.getCoreStatus(coreName)
                    .get("instanceDir") != null;
        } catch (Exception e) {
            LOGGER.debug("Exception getting " + coreName + " core status", e);
            return false;
        }
    }

    private static class SolrClientFetcher implements Callable<SolrClient> {
        private final String url;

        private final String coreName;

        private final String configFile;

        private final String coreUrl;

        private int retryCount;

        public SolrClientFetcher(String url, String coreName, String configFile, String coreUrl) {
            this.url = url;
            this.coreName = coreName;
            this.configFile = configFile;
            this.coreUrl = coreUrl;
            this.retryCount = 0;
        }

        @Override
        public SolrClient call() throws Exception {
            while (true) {
                int retryIndex = retryCount + 1;
                try {
                    LOGGER.info("Retry {} to create Solr client for ({})", retryIndex, coreName);
                    SolrClient client = getSolrServer(url, coreName, configFile, coreUrl);
                    LOGGER.info("Future for HTTP Solr client ({}) finished", coreName);
                    return client;
                } catch (Exception e) {
                    retryCount = Math.min(retryCount + 1, MAX_RETRY_COUNT);
                    long retrySleepMillis = (long) Math.pow(2, Math.min(retryCount,
                            MAX_RETRY_COUNT)) * 50;
                    LOGGER.info("Failed retry {} to create Solr client ({}), trying again in {}",
                            retryIndex,
                            coreName,
                            DurationFormatUtils.formatDurationWords(retrySleepMillis, true, true));
                    LOGGER.debug("Retry failed", e);
                    Thread.sleep(retrySleepMillis);
                }
            }
        }
    }
}
