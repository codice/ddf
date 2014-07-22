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
package org.codice.ddf.persistence.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.solr.query.SolrQueryFilterVisitor;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentStoreImpl implements PersistentStore {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentStoreImpl.class);
    
    private String solrUrl;
    private SolrServer solrServer;
    private SolrServer coreSolrServer;
    private String storeName;
    
    
    public PersistentStoreImpl(String solrUrl) {
        LOGGER.trace("INSIDE: PersistentStoreImpl constructor with solrUrl = {}", solrUrl);
        setSolrUrl(solrUrl);
    }
    
    public void setSolrUrl(String solrUrl) {
        LOGGER.debug("Setting solrUrl to {}", solrUrl);
        if (solrUrl != null) {
            if (!StringUtils.equalsIgnoreCase(solrUrl.trim(), this.solrUrl)) {
                this.solrUrl = solrUrl.trim();
                if (this.solrServer != null) {
                    LOGGER.debug("Shutting down the connection manager to the Solr Server at {} and releasing allocated resources.", this.solrUrl);
                    this.solrServer.shutdown();
                    LOGGER.debug("Shutdown complete.");
                }
                LOGGER.debug("Connecting to solr URL {}", this.solrUrl);
                this.solrServer = new HttpSolrServer(this.solrUrl);
            }
        } else {
            // sets to null
            this.solrUrl = solrUrl;
        }

    }
    
    @Override
    // Input Map is expected to have the suffixes on the key names
    public void add(String type, Map<String, Object> properties) throws PersistenceException {
        LOGGER.debug("type = {}", type);
        if (type == null || type.isEmpty()) {
            return;
        }
        if (properties == null || properties.isEmpty()) {
            return;
        }
        LOGGER.debug("Adding entry of type {}", type);
        
        // Set Solr Core name to type and create/connect to Solr Core
        setSolrCore(type);
        
        Date now = new Date();
        //DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        //String createdDate = df.format(now);
        
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("createddate_tdt", now);
        
        for (String key : properties.keySet()) {
            if (key.equals(PersistentItem.ID)) {
                solrInputDocument.addField(key, (String) properties.get(key));
            } else if (key.endsWith(PersistentItem.TEXT_SET_SUFFIX)) {
                @SuppressWarnings("unchecked")
                Set<String> values = (Set<String>) properties.get(key);
                if (values != null && !values.isEmpty()) {
                    for (String value : values) {
                        solrInputDocument.addField(key, value);
                    }
                }
            } else if (key.endsWith(PersistentItem.XML_SUFFIX) || key.endsWith(PersistentItem.TEXT_SUFFIX)) {
                solrInputDocument.addField(key, (String) properties.get(key));
            } else if (key.endsWith(PersistentItem.LONG_SUFFIX)) {
                solrInputDocument.addField(key, (Long) properties.get(key));
            } else if (key.endsWith(PersistentItem.INT_SUFFIX)) {
                solrInputDocument.addField(key, (Integer) properties.get(key));
            }
        }

        try {
            UpdateResponse response = coreSolrServer.add(solrInputDocument);
            LOGGER.debug("UpdateResponse from add of SolrInputDocument:  {}", response);
        } catch (SolrServerException e) {
            LOGGER.info("SolrServerException while adding Solr index for persistent type {}", type, e);
            doRollback(type);
            throw new PersistenceException("SolrServerException while adding Solr index for persistent type " + type, e);
        } catch (IOException e) {
            LOGGER.info("IOException while adding Solr index for persistent type {}", type, e);
            doRollback(type);
            throw new PersistenceException("IOException while adding Solr index for persistent type " + type, e);
        } catch (RuntimeException e) {
            LOGGER.info("RuntimeException while adding Solr index for persistent type {}", type, e);
            doRollback(type);
            throw new PersistenceException("RuntimeException while adding Solr index for persistent type " + type, e);
        }
    }
    
    private void doRollback(String type) {
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
            throw new PersistenceException("The type of object(s) to retrieve must be non-null and not blank, e.g., notification, metacard, etc.");
        }
        
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        
        // Set Solr Core name to type and create/connect to Solr Core
        setSolrCore(type);
        
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
            long numResults = solrResponse.getResults().getNumFound();
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
            throw new PersistenceException("CQLException while getting Solr data with cql statement " + cql, e);
        } catch (SolrServerException e) {
            throw new PersistenceException("SolrServerException while getting Solr data with cql statement " + cql, e);
        }        
        
        return results;
    }
    
    @Override
    public int delete(String type, String cql) throws PersistenceException {
        List<Map<String, Object>> itemsToDelete = this.get(type, cql);
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
                this.coreSolrServer.deleteById(idsToDelete);
            } catch (SolrServerException e) {
                LOGGER.info("SolrServerException while trying to delete items by ID for persistent type {}", type, e);
                doRollback(type);
                throw new PersistenceException("SolrServerException while trying to delete items by ID for persistent type " + type, e);
            } catch (IOException e) {
                LOGGER.info("IOException while trying to delete items by ID for persistent type {}", type, e);
                doRollback(type);
                throw new PersistenceException("IOException while trying to delete items by ID for persistent type " + type, e);
            } catch (RuntimeException e) {
                LOGGER.info("RuntimeException while trying to delete items by ID for persistent type {}", type, e);
                doRollback(type);
                throw new PersistenceException("RuntimeException while trying to delete items by ID for persistent type " + type, e);
            }
        }
        
        return idsToDelete.size();
    }
    
    private void setSolrCore(String storeName) {
        this.storeName = storeName;
        
        // Must specify shard in URL so proper core is used
        this.coreSolrServer = new HttpSolrServer(this.solrUrl + "/" + this.storeName);
        CoreAdminResponse response = null;
        if (!solrCoreExists(solrServer, this.storeName)) {
            //LOGGER.info("writing solr conf XML files from bundle to disk");
            
            
            LOGGER.info("Creating Solr core {}", this.storeName);
            String instanceDir = System.getProperty("karaf.home") + "/data/solr/" + this.storeName;
            String configFile = "solrconfig.xml";
            String schemaFile = "schema.xml";
            try {
                response = CoreAdminRequest.createCore(this.storeName, instanceDir, this.solrServer, configFile, schemaFile);
            } catch (SolrServerException e) {
                LOGGER.error("SolrServerException creating " + this.storeName + " core", e);
            } catch (IOException e) {
                LOGGER.error("IOException creating " + this.storeName + " core", e);
            }
        } else {
            LOGGER.info("Solr core {} already exists - just reload it", this.storeName);
            try {
                response = CoreAdminRequest.reloadCore(this.storeName, this.solrServer);
            } catch (SolrServerException e) {
                LOGGER.error("SolrServerException reloading " + this.storeName + " core", e);
            } catch (IOException e) {
                LOGGER.error("IOException reloading " + this.storeName + " core", e);
            }
        }        
        
        LOGGER.trace("EXITING: setSolrCore");
    }

    private boolean solrCoreExists(SolrServer solrServer, String coreName) {
        try {
            CoreAdminResponse response = CoreAdminRequest.getStatus(coreName,  solrServer);
            return response.getCoreStatus(coreName).get("instanceDir") != null;
        } catch (SolrServerException e) {
            LOGGER.info("SolrServerException getting " + coreName + " core status", e);
            return false;
        } catch (IOException e) {
            LOGGER.info("IOException getting " + coreName + " core status", e);
            return false;
        }
    }

}

