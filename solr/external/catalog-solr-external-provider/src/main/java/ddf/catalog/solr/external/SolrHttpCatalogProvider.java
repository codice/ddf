/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.solr.external;

import java.io.IOException;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
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
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.util.MaskableImpl;

/**
 * Catalog Provider that interfaces with a Standalone external (HTTP) Solr
 * Server
 * 
 * @author Ashraf Barakat, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public class SolrHttpCatalogProvider extends MaskableImpl implements
        CatalogProvider {

    private static final String PING_ERROR_MESSAGE = "Solr Server ping failed.";

    private static final String OK_STATUS = "OK";

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private String url;

    private CatalogProvider provider = new UnconfiguredCatalogProvider();

    private SolrServer server;

    private FilterAdapter filterAdapter;

    private boolean firstUse;
    
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SolrHttpCatalogProvider.class);

    private static Properties describableProperties = new Properties();

    static {
        try {
            describableProperties.load(SolrHttpCatalogProvider.class
                    .getResourceAsStream(DESCRIBABLE_PROPERTIES_FILE));
        } catch (IOException e) {
            LOGGER.info("Did not laod properties properly.", e);
        }

    }

    /**
     * Simple constructor
     * 
     * @param filterAdapter
     * @param server
     *            - {@link SolrServer} to handle requests
     */
    public SolrHttpCatalogProvider(FilterAdapter filterAdapter,
            SolrServer server) {

        this.filterAdapter = filterAdapter;
        this.server = server;
        this.firstUse = true;
    }

    @Override
    public void maskId(String id) {
        super.maskId(id);
        if (provider != null
                && !(provider instanceof UnconfiguredCatalogProvider)) {
            provider.maskId(id);
        }
    }

    public void setForceAutoCommit(boolean forceAutoCommit) {
        ConfigurationStore.getInstance().setForceAutoCommit(forceAutoCommit);
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
    public SourceResponse query(QueryRequest queryRequest)
            throws UnsupportedQueryException {

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
    public CreateResponse create(CreateRequest createRequest)
            throws IngestException {
        return getProvider().create(createRequest);
    }

    @Override
    public DeleteResponse delete(DeleteRequest deleteRequest)
            throws IngestException {
        return getProvider().delete(deleteRequest);
    }

    @Override
    public UpdateResponse update(UpdateRequest updateRequest)
            throws IngestException {
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
     * This method exists only as a workaround to a Aries Blueprint bug. If
     * Blueprint is upgraded or fixed, this method should be removed and a
     * different update(Map properties) method should be called directly.
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
     * @param urlValue
     *            - url to the Solr Server
     * 
     */
    public void updateServer(String urlValue) {
        LOGGER.info("New url {}", urlValue);
        
        if (urlValue != null) {
    
            if (!StringUtils.equalsIgnoreCase(urlValue.trim(), url)) {
    
                this.url = urlValue.trim();
    
                if (server != null) {
    
                    LOGGER.info("Shutting down the connection manager to the Solr Server and releasing allocated resources.");
                    server.shutdown();
                    LOGGER.info("Shutdown complete.");
                }
    
                server = new HttpSolrServer(url);
                
                firstUse = true;
    
            }
    
        } else {
            // sets to null
            this.url = urlValue;
        }
    
    }

    private CatalogProvider getProvider() {
        if (firstUse) {
            if (isServerUp(this.server)) {
                provider = new SolrCatalogProvider(server, filterAdapter);
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
            return OK_STATUS.equals(solrServer.ping().getResponse()
                    .get("status"));
        } catch (Exception e) {
            /*
             * if we get any type of exception, whether declared by Solr or not,
             * we do not want to fail, we just want to return false
             */
            LOGGER.warn(PING_ERROR_MESSAGE, e);
        }
        return false;
    }

    /**
     * This class is used to signify an unconfigured CatalogProvider instance.
     * If a user tries to unsuccessfully connect to a Solr Server, then a
     * message will be displayed to check the connection.
     * 
     * @author Ashraf Barakat, Lockheed Martin
     * @author ddf.isgs@lmco.com
     * 
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
        public SourceResponse query(QueryRequest arg0)
                throws UnsupportedQueryException {
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
