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
package ddf.catalog.source.solr;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.solr.factory.impl.ConfigurationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import ddf.catalog.util.impl.MaskableImpl;

public abstract class RemoteSolrCatalogProvider extends MaskableImpl implements CatalogProvider {

    private static final String PING_ERROR_MESSAGE = "Solr ping failed.";

    private static final String OK_STATUS = "OK";

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteSolrCatalogProvider.class);

    private static Properties describableProperties = new Properties();

    static {
        try (InputStream inputStream = RemoteSolrCatalogProvider.class.getResourceAsStream(
                DESCRIBABLE_PROPERTIES_FILE)) {
            describableProperties.load(inputStream);
        } catch (IOException e) {
            LOGGER.info("Did not load properties properly.", e);
        }
    }

    protected static final String SOLR_CATALOG_CORE_NAME = "catalog";

    protected String url;

    private CatalogProvider provider = new UnconfiguredCatalogProvider();

    private SolrClient client;

    private FilterAdapter filterAdapter;

    private SolrFilterDelegateFactory solrFilterDelegateFactory;

    private DynamicSchemaResolver resolver;

    private Future<SolrClient> clientFuture;

    /**
     * Simple constructor
     *
     * @param filterAdapter
     * @param client        - {@link SolrClient} to handle requests
     */
    public RemoteSolrCatalogProvider(FilterAdapter filterAdapter, SolrClient client,
            SolrFilterDelegateFactory solrFilterDelegateFactory, DynamicSchemaResolver resolver) {

        this.filterAdapter = filterAdapter;
        this.client = client;
        this.solrFilterDelegateFactory = solrFilterDelegateFactory;
        this.resolver = (resolver == null) ? new DynamicSchemaResolver() : resolver;

    }

    public RemoteSolrCatalogProvider(FilterAdapter filterAdapter, SolrClient client,
            SolrFilterDelegateFactory solrFilterDelegateFactory) {
        this(filterAdapter, client, solrFilterDelegateFactory, null);
    }

    public RemoteSolrCatalogProvider(FilterAdapter filterAdapter,
            SolrFilterDelegateFactory solrFilterDelegateFactory) {
        this(filterAdapter, null, solrFilterDelegateFactory, null);
        updateClient();
    }

    @Override
    public void maskId(String id) {
        super.maskId(id);
        if (!(provider instanceof UnconfiguredCatalogProvider)) {
            provider.maskId(id);
        }
    }

    /**
     * Used to signal to the Solr client to commit on every transaction. Updates
     * the underlying ConfigurationStore so that the property is propagated
     * throughout the Solr Catalog Provider code
     *
     * @param forceAutoCommit
     */
    public void setForceAutoCommit(boolean forceAutoCommit) {
        ConfigurationStore.getInstance()
                .setForceAutoCommit(forceAutoCommit);
    }

    public void setDisableTextPath(boolean disableTextPath) {
        ConfigurationStore.getInstance()
                .setDisableTextPath(disableTextPath);
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
     * Shutdown the connection to Solr and releases resources.
     */
    public void shutdown() {
        LOGGER.debug("Closing connection to solr client.");
        if (getClient() != null) {
            try {
                getClient().close();
            } catch (IOException e) {
                LOGGER.info("Unable to close Solr client", e);
            }
        }
    }

    public String getUrl() {
        return url;
    }

    /**
     * This method exists only as a workaround to a Aries Blueprint bug. If Blueprint is upgraded or
     * fixed, this method should be removed and a different update(Map properties) method should be
     * called directly.
     *
     * @param url
     */
    public void setUrl(String url) {
        updateClient(PropertyResolver.resolveProperties(url));
    }

    /**
     * Updates the configuration of Solr if necessary
     *
     * @param urlValue - url to Solr
     */
    public void updateClient(String urlValue) {
        LOGGER.debug("New url {}", urlValue);

        if (urlValue != null) {
            if (!StringUtils.equalsIgnoreCase(urlValue.trim(), url) || getClient() == null) {
                url = urlValue.trim();

                if (getClient() != null) {
                    LOGGER.debug(
                            "Shutting down the connection manager to Solr and releasing allocated resources.");
                    try {
                        getClient().close();
                    } catch (IOException e) {
                        LOGGER.info("Unable to close Solr client", e);
                    }
                    LOGGER.debug("Shutdown complete.");
                }

                updateClient();
            }

        } else {
            // sets to null
            url = null;
        }
    }

    public void updateClient() {
        clientFuture = createClient();
        client = null;
    }

    protected abstract Future<SolrClient> createClient();

    private CatalogProvider getProvider() {

        if (!isClientConnected(getClient())) {
            return new UnconfiguredCatalogProvider();
        }

        if (provider instanceof UnconfiguredCatalogProvider) {
            provider = new SolrCatalogProvider(getClient(),
                    filterAdapter,
                    solrFilterDelegateFactory,
                    resolver);
        }

        provider.maskId(getId());
        return provider;

    }

    private boolean isClientConnected(SolrClient solr) {

        if (solr == null) {
            return false;
        }

        try {
            return OK_STATUS.equals(solr.ping()
                    .getResponse()
                    .get("status"));
        } catch (Exception e) {
            /*
             * if we get any type of exception, whether declared by Solr or not, we do not want to
             * fail, we just want to return false
             */
            LOGGER.info(PING_ERROR_MESSAGE, e);
        }
        return false;
    }

    private SolrClient getClient() {
        if (client == null && clientFuture != null) {
            try {
                return clientFuture.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                LOGGER.debug("Failed to get client from future", e);
            }
        }
        return client;
    }

    /**
     * This class is used to signify an unconfigured CatalogProvider instance. If a user tries to
     * unsuccessfully connect to Solr, then a message will be displayed to check the
     * connection.
     */
    private static class UnconfiguredCatalogProvider implements CatalogProvider {

        private static final String CLIENT_DISCONNECTED_MESSAGE =
                "Solr client is not connected. Please check Solr status or url, and then retry.";

        @Override
        public Set<ContentType> getContentTypes() {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
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
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getDescription() {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getId() {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getOrganization() {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getTitle() {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

        @Override
        public String getVersion() {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

        @Override
        public void maskId(String arg0) {
            // no op
        }

        @Override
        public CreateResponse create(CreateRequest arg0) throws IngestException {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

        @Override
        public DeleteResponse delete(DeleteRequest arg0) throws IngestException {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

        @Override
        public UpdateResponse update(UpdateRequest arg0) throws IngestException {
            throw new IllegalArgumentException(CLIENT_DISCONNECTED_MESSAGE);
        }

    }
}
