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
package org.codice.solr.cassandra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.codice.ddf.cassandra.CassandraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraUpdateRequestProcessorFactory extends UpdateRequestProcessorFactory implements SolrCoreAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraUpdateRequestProcessorFactory.class);
       
    private static final String CHAIN_NAME = "lux-update-chain";
    
    private SolrCore solrCore;
    private CassandraClient cassandraClient;
    
            
    @Override
    public void inform(SolrCore solrCore) {
        LOGGER.trace("ENTERING: inform()");
        this.solrCore = solrCore;
        NamedList<?> initArgs = null;
        for (PluginInfo info : solrCore.getSolrConfig().getPluginInfos(UpdateRequestProcessorChain.class.getName())) {
            
            if (info.name.equals(CHAIN_NAME)) {
                initArgs = info.initArgs;
                LOGGER.debug("initArgs = {}", initArgs);
                if (initArgs != null) {
                    this.cassandraClient = getCassandraClient(initArgs);
                }
            } else {
                LOGGER.debug("Skipping info.name = {}", info.name);
            }
        }
    }
    
    /**
     * Hierarchical check for definition of Cassandra hostname and port to connect to.
     * Order of checking is:
     *     system properties cassandra.host and cassandra.port, usually defined in setenv script
     *     <INSTALL_DIR>/data/solr/cassandra_connection.properties file
     *     defined as arguments in the solrconfig.xml definition of the Solr update chain
     *     
     * @param args
     * @return
     */
    private CassandraClient getCassandraClient(NamedList<?> args) {
        CassandraClient cassandraClient = null;
        int cassandraCqlPort = -1;
        
        LOGGER.debug("Checking if any system properties set for Cassandra host/port connection");
        String cassandraHost = System.getProperty("cassandra.host");
        String port = System.getProperty("cassandra.port");
        if (StringUtils.isNotBlank(cassandraHost) && StringUtils.isNotBlank(port)) {
            cassandraCqlPort = Integer.valueOf(port);
            try {
                cassandraClient = new CassandraClient(cassandraHost, cassandraCqlPort);
                LOGGER.debug("Successfully connected to Cassandra at host={}, port={} using system properties",
                        cassandraHost, cassandraCqlPort);    
                return cassandraClient;
            } catch (Exception e) {
                LOGGER.debug("Exception trying to get CassandraClient connection using system properties cassandra.host={}, cassandra.port={}", 
                        cassandraHost, cassandraCqlPort);
            }
        } else {
            LOGGER.debug("No system properties set for cassandra.host and cassandra.port");
        }
        
        String cassandraConnectionPropertiesFilename = System.getProperty("karaf.home") + "/data/solr/cassandra_connection.properties";
        File f = new File(cassandraConnectionPropertiesFilename);
        if (f.exists()) {
            LOGGER.debug("Attempting Cassandra connection using host/port from properties file {}", 
                    cassandraConnectionPropertiesFilename);
            try {
                InputStream is = new FileInputStream(f);
                Properties props = new Properties();
                props.load(is);
                cassandraHost = props.getProperty("cassandra.host");
                port = props.getProperty("cassandra.port");
                if (StringUtils.isNotBlank(cassandraHost) && StringUtils.isNotBlank(port)) {
                    cassandraCqlPort = Integer.valueOf(port);
                    try {
                        cassandraClient = new CassandraClient(cassandraHost, cassandraCqlPort);
                        LOGGER.debug("Successfully connected to Cassandra at host={}, port={} using properties file {}", 
                                cassandraHost, cassandraCqlPort, cassandraConnectionPropertiesFilename);
                        return cassandraClient;
                    } catch (Exception e) {
                        LOGGER.debug("Exception trying to get CassandraClient connection using cassandra_connection.properties file with cassandra.host={}, cassandra.port={}", 
                                cassandraHost, cassandraCqlPort, e);
                    }
                } else {
                    LOGGER.debug("Cassandra properties file's values for cassandra.host and cassandra.port were either null or empty");
                }
            } catch (FileNotFoundException e) {
                LOGGER.info("FileNotFoundException trying to connect to Cassandra using properties file", e);
            } catch (IOException e) {
                LOGGER.info("IOException trying to connect to Cassandra using properties file", e);
            }
        } else {
            LOGGER.debug("Cassandra connection properties file {} did not exist", cassandraConnectionPropertiesFilename);
        }
        
        LOGGER.debug("Attempting Cassandra connection using host/port from solrconfig.xml");
        cassandraHost = (String) args.get("cassandra-host");
        port = (String) args.get("cassandra-cql-port");
        if (StringUtils.isNotBlank(port)) {
            cassandraCqlPort = Integer.valueOf(port);
        }
        try {
            LOGGER.debug("Attempting Cassandra connection using host={}, port={} from solrconfig.xml", 
                    cassandraHost, cassandraCqlPort);
            cassandraClient = new CassandraClient(cassandraHost, cassandraCqlPort);
            return cassandraClient;
        } catch (Exception e) {
            LOGGER.info("Exception trying to get CassandraClient connection using args host={}, port={} from solrconfig.xml", 
                    cassandraHost, cassandraCqlPort);
        }
        
        return null;
    }

    @Override
    public UpdateRequestProcessor getInstance(SolrQueryRequest solrQueryRequest, SolrQueryResponse solrQueryResponse,
            UpdateRequestProcessor next) {
        LOGGER.trace("ENTERING: getInstance()");
        return new CassandraUpdateRequestProcessor(solrCore.getName(), cassandraClient, next);
    }

}
