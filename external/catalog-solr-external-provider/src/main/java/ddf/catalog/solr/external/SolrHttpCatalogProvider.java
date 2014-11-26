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
package ddf.catalog.solr.external;

import ddf.catalog.data.ContentType;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.DynamicSchemaResolver;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrFilterDelegateFactory;
import ddf.catalog.source.solr.SolrServerFactory;
import ddf.catalog.util.impl.MaskableImpl;
import ddf.security.encryption.EncryptionService;
import org.apache.commons.lang.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.SolrServer;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Catalog Provider that interfaces with a Standalone external (HTTP) Solr Server
 */
public class SolrHttpCatalogProvider extends MaskableImpl implements CatalogProvider,
        ConfigurationWatcher {

    private static final String PING_ERROR_MESSAGE = "Solr Server ping failed.";

    private static final String OK_STATUS = "OK";

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final String SOLR_CATALOG_CORE_NAME = "catalog";

    private static final String DEFAULT_SOLR_URL = "http://localhost:8181/solr";

    private String url;

    private CatalogProvider provider = new UnconfiguredCatalogProvider();

    private SolrServer server;

    private FilterAdapter filterAdapter;

    private boolean firstUse;

    private SolrFilterDelegateFactory solrFilterDelegateFactory;

    private DynamicSchemaResolver resolver;

    private String keystoreLoc, keystorePass;

    private String truststoreLoc, truststorePass;

    private EncryptionService encryptService;

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrHttpCatalogProvider.class);

    private static final String SOLR_CATALOG_CONFIG_FILE = "solrcatalogconfig.xml";

    private static Properties describableProperties = new Properties();

    static {
        try {
            describableProperties.load(SolrHttpCatalogProvider.class
                    .getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE));
        } catch (IOException e) {
            LOGGER.info("Did not load properties properly.", e);
        }

    }

    /**
     * Simple constructor
     *
     * @param filterAdapter
     * @param server        - {@link SolrServer} to handle requests
     */
    public SolrHttpCatalogProvider(FilterAdapter filterAdapter, SolrServer server,
            SolrFilterDelegateFactory solrFilterDelegateFactory, DynamicSchemaResolver resolver) {

        this.filterAdapter = filterAdapter;
        this.server = server;
        this.firstUse = true;
        this.solrFilterDelegateFactory = solrFilterDelegateFactory;
        this.resolver = resolver;
    }

    public SolrHttpCatalogProvider(FilterAdapter filterAdapter, SolrServer server,
            SolrFilterDelegateFactory solrFilterDelegateFactory) {
        this(filterAdapter, server, solrFilterDelegateFactory, null);
    }

    public SolrHttpCatalogProvider(FilterAdapter filterAdapter, SolrFilterDelegateFactory
            solrFilterDelegateFactory) {
        this(filterAdapter, SolrServerFactory
                        .getHttpSolrServer(SolrHttpCatalogProvider.getSolrUrl(),
                                SOLR_CATALOG_CORE_NAME,
                                SOLR_CATALOG_CONFIG_FILE),
                solrFilterDelegateFactory, null);
    }

    private static String getSolrUrl() {
        String url = DEFAULT_SOLR_URL;
        if (System.getProperty("host") != null && System.getProperty("jetty.port") != null && System
                .getProperty("hostContext") != null) {
            url = "http://" + System.getProperty("host") + ":" + System.getProperty("jetty.port") +
                    "/" + StringUtils.stripStart(System.getProperty("hostContext"), "/");
        }
        return url;
    }

    @Override
    public void maskId(String id) {
        super.maskId(id);
        if (provider != null && !(provider instanceof UnconfiguredCatalogProvider)) {
            provider.maskId(id);
        }
    }

    /**
     * Used to signal to the Solr server to commit on every transaction. Updates
     * the underlying ConfigurationStore so that the property is propagated
     * throughout the Solr Catalog Provider code
     *
     * @param forceAutoCommit
     */
    public void setForceAutoCommit(boolean forceAutoCommit) {
        ConfigurationStore.getInstance().setForceAutoCommit(forceAutoCommit);
    }

    public void setDisableTextPath(boolean disableTextPath) {
        ConfigurationStore.getInstance().setDisableTextPath(disableTextPath);
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return getProvider().getContentTypes();
    }

    @Override
    public boolean isAvailable() {
        return getProvider().isAvailable();
    }

    @Override
    public boolean isAvailable(SourceMonitor arg0) {
        return getProvider().isAvailable(arg0);
    }

    @Override
    public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
        return getProvider().query(queryRequest);
    }

    @Override
    public String getDescription() {

        return describableProperties.getProperty("description");
    }

    @Override
    public String getOrganization() {

        return describableProperties.getProperty("organization");
    }

    @Override
    public String getTitle() {

        return describableProperties.getProperty("name");
    }

    @Override
    public String getVersion() {

        return describableProperties.getProperty("version");
    }

    @Override
    public CreateResponse create(CreateRequest createRequest) throws IngestException {
        return getProvider().create(createRequest);
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest) throws IngestException {
        return getProvider().delete(deleteRequest);
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest) throws IngestException {
        return getProvider().update(updateRequest);
    }

    /**
     * Shutdown the connection to the Solr Server and releases resources.
     */
    public void shutdown() {
        LOGGER.info("Releasing connection to solr server.");
        if (server != null) {
            server.shutdown();
        }
    }

    /**
     * This method exists only as a workaround to a Aries Blueprint bug. If Blueprint is upgraded or
     * fixed, this method should be removed and a different update(Map properties) method should be
     * called directly.
     *
     * @param url
     */
    public void setUrl(String url) {
        updateServer(url);
    }

    public String getUrl() {
        return url;
    }

    /**
     * Updates the configuration of the Solr Server if necessary
     *
     * @param urlValue - url to the Solr Server
     */
    public void updateServer(String urlValue) {
        updateServer(urlValue, false);
    }

    public void updateServer(String urlValue, boolean keystoreUpdate) {
        LOGGER.info("New url {}", urlValue);

        if (urlValue != null) {
            if (!StringUtils.equalsIgnoreCase(urlValue.trim(), url) || keystoreUpdate) {
                url = urlValue.trim();

                if (server != null) {
                    LOGGER.info(
                            "Shutting down the connection manager to the Solr Server and releasing allocated resources.");
                    server.shutdown();
                    LOGGER.info("Shutdown complete.");
                }

                if (StringUtils.startsWith(url, "https") && StringUtils.isNotBlank(truststoreLoc)
                        && StringUtils.isNotBlank(truststorePass)
                        && StringUtils.isNotBlank(keystoreLoc)
                        && StringUtils.isNotBlank(keystorePass)) {
                    server = SolrServerFactory.getHttpSolrServer(url, SOLR_CATALOG_CORE_NAME,
                            SOLR_CATALOG_CONFIG_FILE, getHttpClient());
                } else {
                    server = SolrServerFactory.getHttpSolrServer(url, SOLR_CATALOG_CORE_NAME,
                            SOLR_CATALOG_CONFIG_FILE);
                }

                firstUse = true;
            }
        } else {
            // sets to null
            url = urlValue;
        }
    }

    private CatalogProvider getProvider() {
        if (firstUse) {
            if (isServerUp(this.server)) {
                if (resolver == null) {
                    provider = new SolrCatalogProvider(server, filterAdapter,
                            solrFilterDelegateFactory);
                } else {
                    provider = new SolrCatalogProvider(server, filterAdapter,
                            solrFilterDelegateFactory, resolver);
                }
                provider.maskId(getId());
                this.firstUse = false;
                return provider;
            }
            return new UnconfiguredCatalogProvider();
        }
        return provider;

    }

    private boolean isServerUp(SolrServer solrServer) {

        if (solrServer == null) {
            return false;
        }

        try {
            return OK_STATUS.equals(solrServer.ping().getResponse().get("status"));
        } catch (Exception e) {
            /*
             * if we get any type of exception, whether declared by Solr or not, we do not want to
             * fail, we just want to return false
             */
            LOGGER.warn(PING_ERROR_MESSAGE, e);
        }
        return false;
    }

    private CloseableHttpClient getHttpClient() {
        // Allow TLS protocol and secure ciphers only
        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
                getSslContext(),
                new String[] {
                        "TLSv1",
                        "TLSv1.1",
                        "TLSv1.2"
                },
                new String[] {
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                        "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                        "TLS_RSA_WITH_AES_128_CBC_SHA"
                },
                SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);

        return HttpClients.custom()
                .setSSLSocketFactory(sslConnectionSocketFactory)
                .setDefaultCookieStore(new BasicCookieStore())
                .setMaxConnTotal(128)
                .setMaxConnPerRoute(32)
                .build();
    }

    private SSLContext getSslContext() {
        KeyStore trustStore = getKeyStore(truststoreLoc, truststorePass);
        KeyStore keyStore = getKeyStore(keystoreLoc, keystorePass);

        SSLContext sslContext = null;

        try {
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(keyStore, keystorePass.toCharArray())
                    .loadTrustMaterial(trustStore)
                    .useTLS()
                    .build();
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException |
                KeyManagementException e) {
            LOGGER.error("Unable to create secure HttpClient", e);
            return null;
        }

        sslContext.getDefaultSSLParameters().setNeedClientAuth(true);
        sslContext.getDefaultSSLParameters().setWantClientAuth(true);

        return sslContext;
    }

    private KeyStore getKeyStore(String location, String password) {
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

    public void setEncryptService(EncryptionService encryptService) {
        this.encryptService = encryptService;
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> props) {
        LOGGER.debug("Got a new configuration.");
        String keystoreLocation = props.get(ConfigurationManager.KEY_STORE);
        String keystorePassword = encryptService.decryptValue(props
                .get(ConfigurationManager.KEY_STORE_PASSWORD));

        String truststoreLocation = props.get(ConfigurationManager.TRUST_STORE);
        String truststorePassword = encryptService.decryptValue(props
                .get(ConfigurationManager.TRUST_STORE_PASSWORD));

        boolean keystoresUpdated = false;

        if (StringUtils.isNotBlank(keystoreLocation)
                && (!StringUtils.equals(this.keystoreLoc, keystoreLocation) || !StringUtils.equals(
                this.keystorePass, keystorePassword))) {
            if (new File(keystoreLocation).exists()) {
                LOGGER.debug("Detected a change in the values for the keystore.");
                this.keystoreLoc = keystoreLocation;
                this.keystorePass = keystorePassword;
                keystoresUpdated = true;
            } else {
                LOGGER.debug(
                        "Keystore file does not exist at location {}, not updating keystore values.");
            }
        }
        if (StringUtils.isNotBlank(truststoreLocation)
                && (!StringUtils.equals(this.truststoreLoc, truststoreLocation) || !StringUtils
                .equals(this.truststorePass, truststorePassword))) {
            if (new File(truststoreLocation).exists()) {
                LOGGER.debug("Detected a change in the values for the truststore.");
                this.truststoreLoc = truststoreLocation;
                this.truststorePass = truststorePassword;
                keystoresUpdated = true;
            } else {
                LOGGER.debug(
                        "Truststore file does not exist at location {}, not updating truststore values.");
            }
        }

        if (keystoresUpdated && StringUtils.startsWith(url, "https")) {
            updateServer(url, true);
        }

    }

    /**
     * This class is used to signify an unconfigured CatalogProvider instance. If a user tries to
     * unsuccessfully connect to a Solr Server, then a message will be displayed to check the
     * connection.
     *
     * @author Ashraf Barakat, Lockheed Martin
     * @author ddf.isgs@lmco.com
     */
    private static class UnconfiguredCatalogProvider implements CatalogProvider {

        private static final String SERVER_DISCONNECTED_MESSAGE = "Solr Server is not connected. Please check the Solr Server status or url, and then retry.";

        @Override
        public Set<ContentType> getContentTypes() {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public boolean isAvailable(SourceMonitor arg0) {
            return false;
        }

        @Override
        public SourceResponse query(QueryRequest arg0) throws UnsupportedQueryException {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getDescription() {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getId() {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getOrganization() {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getTitle() {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getVersion() {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public void maskId(String arg0) {
            // no op
        }

        @Override
        public CreateResponse create(CreateRequest arg0) throws IngestException {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public DeleteResponse delete(DeleteRequest arg0) throws IngestException {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

        @Override
        public UpdateResponse update(UpdateRequest arg0) throws IngestException {
            throw new IllegalArgumentException(SERVER_DISCONNECTED_MESSAGE);
        }

    }
}
