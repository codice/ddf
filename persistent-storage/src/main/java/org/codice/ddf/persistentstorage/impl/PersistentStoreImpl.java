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
package org.codice.ddf.persistentstorage.impl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.CoreAdminRequest;
import org.apache.solr.client.solrj.response.CoreAdminResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.codice.ddf.persistentstorage.api.PersistentStore;
import org.codice.ddf.persistentstorage.api.PersistentStoreMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistentStoreImpl implements PersistentStore {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentStoreImpl.class);
    
    private String solrUrl;
    private SolrServer solrServer;
    private SolrServer coreSolrServer;
    private String storeName;
    
    
    public PersistentStoreImpl(String solrUrl) {
        LOGGER.info("INSIDE: PersistentStoreImpl constructor with solrUrl = {}", solrUrl);
        setSolrUrl(solrUrl);
    }
    
    public void setSolrUrl(String solrUrl) {
        LOGGER.info("Setting solrUrl to {}", solrUrl);
//        this.solrUrl = solrUrl;
//        this.solrServer = new HttpSolrServer(this.solrUrl);
        if (solrUrl != null) {
            if (!StringUtils.equalsIgnoreCase(solrUrl.trim(), this.solrUrl)) {
                this.solrUrl = solrUrl.trim();
                if (this.solrServer != null) {
                    LOGGER.info("Shutting down the connection manager to the Solr Server at {} and releasing allocated resources.", this.solrUrl);
                    this.solrServer.shutdown();
                    LOGGER.info("Shutdown complete.");
                }
                LOGGER.info("Connecting to solr URL {}", this.solrUrl);
                this.solrServer = new HttpSolrServer(this.solrUrl);
            }
        } else {
            // sets to null
            this.solrUrl = solrUrl;
        }

    }
    
    @Override
    public void addEntry(String type, Map<String, Object> properties) {
        LOGGER.info("type = {}", type);
        if (type == null || type.isEmpty()) {
            return;
        }
        if (properties == null || properties.isEmpty()) {
            return;
        }
        LOGGER.info("Adding entry of type {}", type);
        
        // Set Solr Core name to type and create/connect to Solr Core
        setSolrCore(type);
        
        String uuid = UUID.randomUUID().toString();
        Date now = new Date();
        //DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        //String createdDate = df.format(now);
        
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField(PersistentStoreMap.ID, uuid);
        solrInputDocument.addField("createddate_tdt", now);
        
        for (String key : properties.keySet()) {
            if (key.endsWith(PersistentStoreMap.TEXT_SET_SUFFIX)) {
                @SuppressWarnings("unchecked")
                Set<String> values = (Set<String>) properties.get(key);
                if (values != null && !values.isEmpty()) {
                    for (String value : values) {
                        solrInputDocument.addField(key, value);
                    }
                }
            } else if (key.endsWith(PersistentStoreMap.XML_SUFFIX) || key.endsWith(PersistentStoreMap.TEXT_SUFFIX)) {
                solrInputDocument.addField(key, (String) properties.get(key));
            } else if (key.endsWith(PersistentStoreMap.LONG_SUFFIX)) {
                solrInputDocument.addField(key, (Long) properties.get(key));
            } else if (key.endsWith(PersistentStoreMap.INT_SUFFIX)) {
                solrInputDocument.addField(key, (Integer) properties.get(key));
            }
        }

        try {
            UpdateResponse response = coreSolrServer.add(solrInputDocument);
            LOGGER.info("UpdateResponse from add of SolrInputDocument:  {}", response);
        } catch (SolrServerException e) {
            LOGGER.info("SolrServerException while adding Solr index for saved query", e);
            //TODO: rollback (delete) saved_query entry just added to Cassandra and throw exception
        } catch (IOException e) {
            LOGGER.info("IOException while adding Solr index for saved query", e);
            //TODO: rollback (delete) saved_query entry just added to Cassandra???
        }
    }
    
    private void setSolrCore(String storeName) {
        this.storeName = storeName;
        
        // Must specify shard in URL so proper core is used
        this.coreSolrServer = new HttpSolrServer(this.solrUrl + "/" + this.storeName);
        
        if (!solrCoreExists(solrServer, this.storeName)) {
            LOGGER.info("Creating Solr core {}", this.storeName);
            String instanceDir = System.getProperty("karaf.home") + "/data/solr/" + this.storeName;
            String configFile = "solrconfig.xml";
            String schemaFile = "schema.xml";
            try {
                CoreAdminResponse response = 
                        CoreAdminRequest.createCore(this.storeName, instanceDir, this.solrServer, configFile, schemaFile);
            } catch (SolrServerException e) {
                LOGGER.error("SolrServerException creating " + this.storeName + " core", e);
            } catch (IOException e) {
                LOGGER.error("IOException creating " + this.storeName + " core", e);
            }
        } else {
            LOGGER.info("Solr core {} already exists - just reload it", this.storeName);
            try {
                CoreAdminResponse response = 
                        CoreAdminRequest.reloadCore(this.storeName, this.solrServer);
            } catch (SolrServerException e) {
                LOGGER.error("SolrServerException reloading " + this.storeName + " core", e);
            } catch (IOException e) {
                LOGGER.error("IOException reloading " + this.storeName + " core", e);
            }
        }
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
