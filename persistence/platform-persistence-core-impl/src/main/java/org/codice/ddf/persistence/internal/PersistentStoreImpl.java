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

import ddf.security.encryption.EncryptionService;
import org.apache.commons.lang.StringUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.solr.query.SolrQueryFilterVisitor;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.opengis.filter.Filter;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PersistentStoreImpl implements PersistentStore, ConfigurationWatcher {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistentStoreImpl.class);

    private static final String DEFAULT_SOLR_URL = "https://localhost:8993/solr";

    private String solrUrl = DEFAULT_SOLR_URL;
    private String keystoreLoc, keystorePass;

    private String truststoreLoc, truststorePass;

    private EncryptionService encryptService;
    
    private SolrServer solrServer;
    private ConcurrentHashMap<String, SolrServer> coreSolrServers = new ConcurrentHashMap<String,
            SolrServer>();

    public PersistentStoreImpl() {
        this(getSolrUrl());
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

    public PersistentStoreImpl(String solrUrl) {
        LOGGER.trace("INSIDE: PersistentStoreImpl constructor with solrUrl = {}", solrUrl);
        setSolrUrl(solrUrl);
    }

    public void setSolrUrl(String solrUrl) {
        setSolrUrl(solrUrl, false);
    }
    
    public void setSolrUrl(String solrUrl, boolean keystoreUpdate) {
        LOGGER.debug("Setting solrUrl to {}", solrUrl);
        if (solrUrl != null) {
            if (!StringUtils.equalsIgnoreCase(solrUrl.trim(), this.solrUrl) || this.solrServer == null || keystoreUpdate) {
                this.solrUrl = solrUrl.trim();
                if (this.solrServer != null) {
                    LOGGER.debug("Shutting down the connection manager to the Solr Server at {} and releasing allocated resources.", this.solrUrl);
                    this.solrServer.shutdown();
                    LOGGER.debug("Shutdown complete.");
                }
                LOGGER.debug("Connecting to solr URL {}", this.solrUrl);

                if (StringUtils.startsWith(this.solrUrl, "https") && StringUtils.isNotBlank(truststoreLoc)
                        && StringUtils.isNotBlank(truststorePass)
                        && StringUtils.isNotBlank(keystoreLoc)
                        && StringUtils.isNotBlank(keystorePass)) {
                    this.solrServer = new HttpSolrServer(this.solrUrl, getHttpClient());
                } else if(!StringUtils.startsWith(this.solrUrl, "https")) {
                    this.solrServer = new HttpSolrServer(this.solrUrl);
                }
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
            doRollback(coreSolrServer, type);
            throw new PersistenceException("SolrServerException while adding Solr index for persistent type " + type, e);
        } catch (IOException e) {
            LOGGER.info("IOException while adding Solr index for persistent type {}", type, e);
            doRollback(coreSolrServer, type);
            throw new PersistenceException("IOException while adding Solr index for persistent type " + type, e);
        } catch (RuntimeException e) {
            LOGGER.info("RuntimeException while adding Solr index for persistent type {}", type, e);
            doRollback(coreSolrServer, type);
            throw new PersistenceException("RuntimeException while adding Solr index for persistent type " + type, e);
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
            throw new PersistenceException("The type of object(s) to retrieve must be non-null and not blank, e.g., notification, metacard, etc.");
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
                LOGGER.info("SolrServerException while trying to delete items by ID for persistent type {}", type, e);
                doRollback(coreSolrServer, type);
                throw new PersistenceException("SolrServerException while trying to delete items by ID for persistent type " + type, e);
            } catch (IOException e) {
                LOGGER.info("IOException while trying to delete items by ID for persistent type {}", type, e);
                doRollback(coreSolrServer, type);
                throw new PersistenceException("IOException while trying to delete items by ID for persistent type " + type, e);
            } catch (RuntimeException e) {
                LOGGER.info("RuntimeException while trying to delete items by ID for persistent type {}", type, e);
                doRollback(coreSolrServer, type);
                throw new PersistenceException("RuntimeException while trying to delete items by ID for persistent type " + type, e);
            }
        }
        
        return idsToDelete.size();
    }
    
    private SolrServer getSolrCore(String storeName) {
        if (coreSolrServers.containsKey(storeName)) {
            LOGGER.info("Returning core {} from map of coreSolrServers", storeName);
            return coreSolrServers.get(storeName);
        }

        if (solrServer == null) {
            LOGGER.warn("Unable to create Solr Core '{}', please configure the Solr URL in the \"Persistent Store\" configuration.", storeName);
            return null;
        }
        
        // Must specify shard in URL so proper core is used
        HttpSolrServer coreSolrServer = new HttpSolrServer(this.solrUrl + "/" + storeName, getHttpClient());
        CoreAdminResponse response = null;
        if (!solrCoreExists(this.solrServer, storeName)) {
            //LOGGER.info("writing solr conf XML files from bundle to disk");
            LOGGER.info("Creating Solr core {}", storeName);
            String instanceDir = System.getProperty("karaf.home") + "/data/solr/" + storeName;
            String configFile = "solrconfig.xml";
            String schemaFile = "schema.xml";
            try {
                response = CoreAdminRequest.createCore(storeName, instanceDir, this.solrServer, configFile, schemaFile);
                coreSolrServers.put(storeName, coreSolrServer);
            } catch (SolrServerException e) {
                LOGGER.error("SolrServerException creating " + storeName + " core", e);
            } catch (IOException e) {
                LOGGER.error("IOException creating " + storeName + " core", e);
            }
        }     
        
        LOGGER.trace("EXITING: getSolrCore");
        
        return coreSolrServer;
    }

    private boolean solrCoreExists(SolrServer server, String coreName) {
        try {
            CoreAdminResponse response = CoreAdminRequest.getStatus(coreName,  server);
            return response.getCoreStatus(coreName).get("instanceDir") != null;
        } catch (SolrServerException e) {
            LOGGER.info("SolrServerException getting " + coreName + " core status", e);
            return false;
        } catch (IOException e) {
            LOGGER.info("IOException getting " + coreName + " core status", e);
            return false;
        }
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

        if (keystoresUpdated) {
            setSolrUrl(this.solrUrl, keystoresUpdated);
        }

    }
}

