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
package org.codice.ddf.persistence.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.solr.factory.SolrServerFactory;
import org.codice.solr.query.SolrQueryFilterVisitor;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentStoreImpl implements PersistentStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentStoreImpl.class);

    private PropertyResolver solrUrl;

    private ConcurrentHashMap<String, SolrServer> coreSolrServers = new ConcurrentHashMap<>();

    public PersistentStoreImpl(String solrUrl) {
        LOGGER.trace("INSIDE: PersistentStoreImpl constructor with solrUrl = {}", solrUrl);
        setSolrUrl(solrUrl);
    }

    public void setSolrUrl(String url) {

        LOGGER.debug("Setting solrUrl to {}", url);
        if (url != null) {
            if (solrUrl == null || !StringUtils.equalsIgnoreCase(url.trim(),
                    solrUrl.getResolvedString())) {
                solrUrl = new PropertyResolver(url.trim());

                List<SolrServer> servers = new ArrayList<>(coreSolrServers.values());
                coreSolrServers.clear();
                for (SolrServer server : servers) {
                    server.shutdown();
                }
            }
        } else {
            // sets to null
            solrUrl = new PropertyResolver(url);
        }
    }

    @Override
    // Input Map is expected to have the suffixes on the key names
    public void add(String type, Map<String, Object> properties) throws PersistenceException {
        LOGGER.debug("type = {}", type);
        if (type == null || type.isEmpty()) {
            return;
        }
        if (properties == null || properties.isEmpty() || properties.containsValue("guest")) {
            return;
        }

        LOGGER.debug("Adding entry of type {}", type);

        // Set Solr Core name to type and create/connect to Solr Core
        SolrServer coreSolrServer = getSolrCore(type);
        if (coreSolrServer == null) {
            return;
        }

        Date now = new Date();
        //DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        //String createdDate = df.format(now);

        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("createddate_tdt", now);

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getKey()
                    .equals(PersistentItem.ID)) {
                solrInputDocument.addField(entry.getKey(), entry.getValue());
            } else if (entry.getKey()
                    .endsWith(PersistentItem.TEXT_SET_SUFFIX)) {
                @SuppressWarnings("unchecked")
                Set<String> values = (Set<String>) entry.getValue();
                if (values != null && !values.isEmpty()) {
                    for (String value : values) {
                        solrInputDocument.addField(entry.getKey(), value);
                    }
                }
            } else if (entry.getKey()
                    .endsWith(PersistentItem.XML_SUFFIX) || entry.getKey()
                    .endsWith(PersistentItem.TEXT_SUFFIX)) {
                solrInputDocument.addField(entry.getKey(), entry.getValue());
            } else if (entry.getKey()
                    .endsWith(PersistentItem.LONG_SUFFIX)) {
                solrInputDocument.addField(entry.getKey(), entry.getValue());
            } else if (entry.getKey()
                    .endsWith(PersistentItem.INT_SUFFIX)) {
                solrInputDocument.addField(entry.getKey(), entry.getValue());
            }
        }

        try {
            UpdateResponse response = coreSolrServer.add(solrInputDocument);
            LOGGER.debug("UpdateResponse from add of SolrInputDocument:  {}", response);
        } catch (SolrServerException e) {
            LOGGER.info("SolrServerException while adding Solr index for persistent type {}",
                    type,
                    e);
            doRollback(coreSolrServer, type);
            throw new PersistenceException(
                    "SolrServerException while adding Solr index for persistent type " + type,
                    e);
        } catch (IOException e) {
            LOGGER.info("IOException while adding Solr index for persistent type {}", type, e);
            doRollback(coreSolrServer, type);
            throw new PersistenceException(
                    "IOException while adding Solr index for persistent type " + type,
                    e);
        } catch (RuntimeException e) {
            LOGGER.info("RuntimeException while adding Solr index for persistent type {}", type, e);
            doRollback(coreSolrServer, type);
            throw new PersistenceException(
                    "RuntimeException while adding Solr index for persistent type " + type,
                    e);
        }
    }

    private void doRollback(SolrServer coreSolrServer, String type) {
        LOGGER.debug("ENTERING: doRollback()");
        try {
            coreSolrServer.rollback();
        } catch (SolrServerException e) {
            LOGGER.info("SolrServerException while doing rollback for persistent type {}", type, e);
        } catch (IOException e) {
            LOGGER.info("IOException while doing rollback for persistent type {}", type, e);
        }
        LOGGER.debug("EXITING: doRollback()");
    }

    @Override
    public List<Map<String, Object>> get(String type) throws PersistenceException {
        return get(type, "");
    }

    @Override
    // Returned Map will have suffixes in the key names - client is responsible for handling them
    public List<Map<String, Object>> get(String type, String cql) throws PersistenceException {
        if (StringUtils.isBlank(type)) {
            throw new PersistenceException(
                    "The type of object(s) to retrieve must be non-null and not blank, e.g., notification, metacard, etc.");
        }

        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();

        // Set Solr Core name to type and create/connect to Solr Core
        SolrServer coreSolrServer = getSolrCore(type);
        if (coreSolrServer == null) {
            return results;
        }

        SolrQueryFilterVisitor visitor = new SolrQueryFilterVisitor(coreSolrServer, type);

        try {
            SolrQuery solrQuery;
            // If not cql specified, then return all items
            if (StringUtils.isBlank(cql)) {
                solrQuery = new SolrQuery("*:*");
            } else {
                Filter filter = CQL.toFilter(cql);
                solrQuery = (SolrQuery) filter.accept(visitor, null);
            }
            QueryResponse solrResponse = coreSolrServer.query(solrQuery, METHOD.POST);
            long numResults = solrResponse.getResults()
                    .getNumFound();
            LOGGER.debug("numResults = {}", numResults);

            SolrDocumentList docs = solrResponse.getResults();
            for (SolrDocument doc : docs) {
                PersistentItem result = new PersistentItem();
                Collection<String> fieldNames = doc.getFieldNames();
                for (String name : fieldNames) {
                    LOGGER.debug("field name = {} has value = {}", name, doc.getFieldValue(name));
                    if (name.endsWith(PersistentItem.TEXT_SET_SUFFIX)) {
                        result.addProperty(name, (Set<String>) doc.getFieldValue(name));
                    } else if (name.endsWith(PersistentItem.XML_SUFFIX)) {
                        result.addXmlProperty(name, (String) doc.getFieldValue(name));
                    } else if (name.endsWith(PersistentItem.TEXT_SUFFIX)) {
                        result.addProperty(name, (String) doc.getFieldValue(name));
                    } else if (name.endsWith(PersistentItem.LONG_SUFFIX)) {
                        result.addProperty(name, (Long) doc.getFieldValue(name));
                    } else if (name.endsWith(PersistentItem.INT_SUFFIX)) {
                        result.addProperty(name, (Integer) doc.getFieldValue(name));
                    } else if (name.endsWith(PersistentItem.DATE_SUFFIX)) {
                        result.addProperty(name, (Date) doc.getFieldValue(name));
                    } else {
                        LOGGER.info("Not adding field {} because it has invalid suffix", name);
                    }
                }
                results.add(result);
            }
        } catch (CQLException e) {
            throw new PersistenceException(
                    "CQLException while getting Solr data with cql statement " + cql,
                    e);
        } catch (SolrServerException e) {
            throw new PersistenceException(
                    "SolrServerException while getting Solr data with cql statement " + cql,
                    e);
        }

        return results;
    }

    @Override
    public int delete(String type, String cql) throws PersistenceException {
        List<Map<String, Object>> itemsToDelete = this.get(type, cql);
        SolrServer coreSolrServer = getSolrCore(type);
        if (coreSolrServer == null) {
            return 0;
        }
        List<String> idsToDelete = new ArrayList<String>();
        for (Map<String, Object> item : itemsToDelete) {
            String uuid = (String) item.get(PersistentItem.ID);
            if (StringUtils.isNotBlank(uuid)) {
                idsToDelete.add(uuid);
            }
        }

        if (!idsToDelete.isEmpty()) {
            try {
                LOGGER.info("Deleting {} items by ID", idsToDelete.size());
                coreSolrServer.deleteById(idsToDelete);
            } catch (SolrServerException e) {
                LOGGER.info(
                        "SolrServerException while trying to delete items by ID for persistent type {}",
                        type,
                        e);
                doRollback(coreSolrServer, type);
                throw new PersistenceException(
                        "SolrServerException while trying to delete items by ID for persistent type "
                                + type,
                        e);
            } catch (IOException e) {
                LOGGER.info("IOException while trying to delete items by ID for persistent type {}",
                        type,
                        e);
                doRollback(coreSolrServer, type);
                throw new PersistenceException(
                        "IOException while trying to delete items by ID for persistent type "
                                + type,
                        e);
            } catch (RuntimeException e) {
                LOGGER.info(
                        "RuntimeException while trying to delete items by ID for persistent type {}",
                        type,
                        e);
                doRollback(coreSolrServer, type);
                throw new PersistenceException(
                        "RuntimeException while trying to delete items by ID for persistent type "
                                + type,
                        e);
            }
        }

        return idsToDelete.size();
    }

    private SolrServer getSolrCore(String storeName) {
        if (coreSolrServers.containsKey(storeName)) {
            LOGGER.info("Returning core {} from map of coreSolrServers", storeName);
            return coreSolrServers.get(storeName);
        }

        // Must specify shard in URL so proper core is used
        SolrServer coreSolrServer = SolrServerFactory.getHttpSolrServer(solrUrl.getResolvedString(),
                storeName);
        coreSolrServers.put(storeName, coreSolrServer);

        LOGGER.trace("EXITING: getSolrCore");

        return coreSolrServer;
    }

}

