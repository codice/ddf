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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.solr.factory.SolrClientFactory;
import org.codice.solr.factory.impl.SolrClientFactoryImpl;
import org.codice.solr.query.SolrQueryFilterVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PersistentStoreImpl implements PersistentStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentStoreImpl.class);

    private SolrClientFactory clientFactory;

    private ConcurrentHashMap<String, Future<SolrClient>> solrFutures = new ConcurrentHashMap<>();

    public PersistentStoreImpl(SolrClientFactoryImpl clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public void add(String type, Collection<Map<String, Object>> items) throws PersistenceException {
        LOGGER.debug("type = {}", type);
        if (StringUtils.isEmpty(type)) {
            throw new PersistenceException(
                    "The type of object(s) to be added must be non-null and not blank, e.g., notification, metacard, etc.");
        }
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        // Set Solr Core name to type and create solr client
        SolrClient solrClient = getSolrClient(type);
        if (solrClient == null) {
            throw new PersistenceException("Unable to create Solr client.");
        }
        List<SolrInputDocument> inputDocuments = new ArrayList<>();
        for (Map<String, Object> properties : items) {

            if (MapUtils.isEmpty(properties)) {
                continue;
            }

            LOGGER.debug("Adding entry of type {}", type);

            SolrInputDocument solrInputDocument = new SolrInputDocument();
            solrInputDocument.addField("createddate_tdt", new Date());

            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                solrInputDocument.addField(entry.getKey(), entry.getValue());
            }
            inputDocuments.add(solrInputDocument);
        }

        if(inputDocuments.isEmpty()){
            return;
        }

        try {
            UpdateResponse response = solrClient.add(inputDocuments);
            LOGGER.debug("UpdateResponse from add of SolrInputDocument:  {}", response);
        } catch (SolrServerException e) {
            LOGGER.info("SolrServerException while adding Solr index for persistent type {}",
                    type,
                    e);
            doRollback(solrClient, type);
            throw new PersistenceException(
                    "SolrServerException while adding Solr index for persistent type " + type,
                    e);
        } catch (IOException e) {
            LOGGER.info("IOException while adding Solr index for persistent type {}", type, e);
            doRollback(solrClient, type);
            throw new PersistenceException(
                    "IOException while adding Solr index for persistent type " + type,
                    e);
        } catch (RuntimeException e) {
            LOGGER.info("RuntimeException while adding Solr index for persistent type {}", type, e);
            doRollback(solrClient, type);
            throw new PersistenceException(
                    "RuntimeException while adding Solr index for persistent type " + type,
                    e);
        }
    }
    @Override
    public void add(String type, Map<String, Object> properties) throws PersistenceException {
        add(type, Collections.singletonList(properties));
    }

    private void doRollback(SolrClient solrClient, String type) {
        LOGGER.debug("ENTERING: doRollback()");
        try {
            solrClient.rollback();
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

        List<Map<String, Object>> results = new ArrayList<>();

        // Set Solr Core name to type and create/connect to Solr Core
        SolrClient solrClient = getSolrClient(type);
        if (solrClient == null) {
            throw new PersistenceException("Unable to create Solr client.");
        }

        SolrQueryFilterVisitor visitor = new SolrQueryFilterVisitor(solrClient, type);

        try {
            SolrQuery solrQuery;
            // If not cql specified, then return all items
            if (StringUtils.isBlank(cql)) {
                solrQuery = new SolrQuery("*:*");
            } else {
                Filter filter = ECQL.toFilter(cql);
                solrQuery = (SolrQuery) filter.accept(visitor, null);
            }
            if (solrQuery == null) {
                throw new PersistenceException("Unsupported query " + cql);
            }
            QueryResponse solrResponse = solrClient.query(solrQuery, METHOD.POST);
            long numResults = solrResponse.getResults()
                    .getNumFound();
            LOGGER.debug("numResults = {}", numResults);

            SolrDocumentList docs = solrResponse.getResults();
            for (SolrDocument doc : docs) {
                PersistentItem result = new PersistentItem();
                Collection<String> fieldNames = doc.getFieldNames();
                for (String name : fieldNames) {
                    LOGGER.debug("field name = {} has value = {}", name, doc.getFieldValue(name));
                    if (name.endsWith(PersistentItem.TEXT_SUFFIX) && doc.getFieldValues(name)
                            .size() > 1) {
                        result.addProperty(name,
                                doc.getFieldValues(name)
                                        .stream()
                                        .filter(s -> s instanceof String)
                                        .map(s -> (String) s)
                                        .collect(Collectors.toSet()));
                    } else if (name.endsWith(PersistentItem.XML_SUFFIX)) {
                        result.addXmlProperty(name, (String) doc.getFirstValue(name));
                    } else if (name.endsWith(PersistentItem.TEXT_SUFFIX)) {
                        result.addProperty(name, (String) doc.getFirstValue(name));
                    } else if (name.endsWith(PersistentItem.LONG_SUFFIX)) {
                        result.addProperty(name, (Long) doc.getFirstValue(name));
                    } else if (name.endsWith(PersistentItem.INT_SUFFIX)) {
                        result.addProperty(name, (Integer) doc.getFirstValue(name));
                    } else if (name.endsWith(PersistentItem.DATE_SUFFIX)) {
                        result.addProperty(name, (Date) doc.getFirstValue(name));
                    } else if (name.endsWith(PersistentItem.BINARY_SUFFIX)) {
                        result.addProperty(name, (byte[]) doc.getFirstValue(name));
                    } else {
                        LOGGER.debug("Not adding field {} because it has invalid suffix", name);
                    }
                }
                results.add(result);
            }
        } catch (CQLException e) {
            throw new PersistenceException(
                    "CQLException while getting Solr data with cql statement " + cql,
                    e);
        } catch (SolrServerException | IOException e) {
            throw new PersistenceException(
                    "SolrServerException while getting Solr data with cql statement " + cql,
                    e);
        }

        return results;
    }

    @Override
    public int delete(String type, String cql) throws PersistenceException {
        List<Map<String, Object>> itemsToDelete = this.get(type, cql);
        SolrClient solrClient = getSolrClient(type);
        if (solrClient == null) {
            throw new PersistenceException("Unable to create Solr client.");
        }
        List<String> idsToDelete = new ArrayList<>();
        for (Map<String, Object> item : itemsToDelete) {
            String uuid = (String) item.get(PersistentItem.ID);
            if (StringUtils.isNotBlank(uuid)) {
                idsToDelete.add(uuid);
            }
        }

        if (!idsToDelete.isEmpty()) {
            try {
                LOGGER.debug("Deleting {} items by ID", idsToDelete.size());
                solrClient.deleteById(idsToDelete);
            } catch (SolrServerException e) {
                LOGGER.info(
                        "SolrServerException while trying to delete items by ID for persistent type {}",
                        type,
                        e);
                doRollback(solrClient, type);
                throw new PersistenceException(
                        "SolrServerException while trying to delete items by ID for persistent type "
                                + type,
                        e);
            } catch (IOException e) {
                LOGGER.info("IOException while trying to delete items by ID for persistent type {}",
                        type,
                        e);
                doRollback(solrClient, type);
                throw new PersistenceException(
                        "IOException while trying to delete items by ID for persistent type "
                                + type,
                        e);
            } catch (RuntimeException e) {
                LOGGER.info(
                        "RuntimeException while trying to delete items by ID for persistent type {}",
                        type,
                        e);
                doRollback(solrClient, type);
                throw new PersistenceException(
                        "RuntimeException while trying to delete items by ID for persistent type "
                                + type,
                        e);
            }
        }

        return idsToDelete.size();
    }

    private SolrClient getSolrClient(String storeName) {

        SolrClient solrClient = null;

        Future<SolrClient> clientFuture = solrFutures.computeIfAbsent(storeName, clientFactory::newClient);

        try {
            solrClient = clientFuture.get(5, TimeUnit.SECONDS);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.warn("Error getting solr server from future for core: {}", storeName, e);
        }
        return solrClient;
    }

}

