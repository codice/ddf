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
package org.codice.ddf.cassandra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codice.ddf.cassandra.CassandraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ProtocolOptions.Compression;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.TableMetadata;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.policies.Policies;

public class CassandraClient {

    private static Logger LOGGER = LoggerFactory.getLogger(CassandraClient.class);
    
    private static final String DEFAULT_CLUSTER_NAME = "ddfcluster";
    
    private static final String DEFAULT_KEYSPACE = "ddf";
    
    private static final Pattern ALPHA_NUMERIC_UNDERSCORE_REGEX = Pattern.compile("^[A-Za-z0-9_]*$");
    
    private static final Pattern UUID_REGEX = Pattern.compile("(\\w{8})-(\\w{4})-(\\w{4})-(\\w{4})-(\\w{12})");

    
    private String keyspaceName;
    private Cluster cluster;
    
    
    public CassandraClient(String host, int port) {
        this(host, port, DEFAULT_KEYSPACE);
    }
    
    public CassandraClient(String host, int port, String keyspaceName) {
        
        this.cluster = createCluster(host, port);
        this.keyspaceName = keyspaceName;
        
        LOGGER.info("Creating {} keyspace", keyspaceName);
        createKeyspace(keyspaceName, true);
        
        // Sets the keyspace to be used for all subsequent CQL statements
        setKeyspace(keyspaceName);
    }
        
    private Cluster createCluster(String hostname, int port) {
        return Cluster.builder()
                .addContactPoint(hostname)
                .withPort(port)
                .withClusterName(DEFAULT_CLUSTER_NAME)
                .withCompression(Compression.SNAPPY)
                .withLoadBalancingPolicy(Policies.defaultLoadBalancingPolicy())
                .withRetryPolicy(Policies.defaultRetryPolicy())
                .withReconnectionPolicy(Policies.defaultReconnectionPolicy())
                .build();
    }
    
    public void createKeyspace(String keyspaceName) {
        createKeyspace(keyspaceName, true);
    }
    
    public void createKeyspace(String keyspaceName, boolean keyspaceDurableWrite) {
        final Session session = cluster.connect("system");
        final Row row = session.execute(
                "SELECT count(1) FROM schema_keyspaces WHERE keyspace_name='" + keyspaceName + "'").one();
        if (row.getLong(0) != 1) {
            StringBuilder createKeyspaceStatement = new StringBuilder("CREATE keyspace ");
            createKeyspaceStatement.append(keyspaceName);
            createKeyspaceStatement.append(" WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1}");
            if (!keyspaceDurableWrite) {
                createKeyspaceStatement.append(" AND DURABLE_WRITES=false");
            }
            session.execute(createKeyspaceStatement.toString());
        }
        session.close();
    }
    
    public void createTable(String keyspaceName, String tableName, String cqlColumnDescriptions) {   
        tableName = normalizeCqlName(tableName);
        try {
            if (tableName != null && !hasTable(tableName, keyspaceName)) {               
                String query = "CREATE TABLE " + keyspaceName + "." + tableName + " (" + cqlColumnDescriptions + ")";
                LOGGER.info("Executing query: {}", query);
                getSession(keyspaceName).execute(query);
            } else {
                LOGGER.info("Table {} already exists", tableName);
            }
        } catch (QueryExecutionException e) {
            LOGGER.info("QueryExecutionException: ", e);
        } catch (NoHostAvailableException e) {
            LOGGER.info("NoHostAvailableException: ", e);
        }
    }
    
    public Session getSession(String keyspaceName) {
        return cluster.connect(keyspaceName);
    }
    
    public void setKeyspace(String keyspaceName) {
        getSession(keyspaceName).execute("USE " + keyspaceName);
    }
    
    public List<String> getKeyspaceNames() {
        Metadata metadata = cluster.getMetadata();
        List<KeyspaceMetadata> keyspaceMetadataList = metadata.getKeyspaces();
        List<String> keyspaceNames = new ArrayList<String>();
        
        for (KeyspaceMetadata ksMetadata : keyspaceMetadataList) {
            keyspaceNames.add(ksMetadata.getName());
        }
        
        return keyspaceNames;
    }
    
    public boolean hasKeyspace(String keyspaceName) {
        return getKeyspaceNames().contains(keyspaceName);
    }
    
    public Map<String, TableMetadata> getTables(String keyspaceName) {
        Metadata metadata = cluster.getMetadata();
        List<KeyspaceMetadata> keyspaceMetadataList = metadata.getKeyspaces();
        Map<String, TableMetadata> tables = new HashMap<String, TableMetadata>();
        LOGGER.info("keyspaceMetadataList.size() = {}", keyspaceMetadataList.size());
        
        for (KeyspaceMetadata ksMetadata : keyspaceMetadataList) {
            LOGGER.info("keyspace name = {}", ksMetadata.getName());
            if (ksMetadata.getName().equals(keyspaceName)) {               
                for (TableMetadata tableMetadata : ksMetadata.getTables()) {
                    LOGGER.info("table name = {}", tableMetadata.getName());
                    tables.put(tableMetadata.getName(), tableMetadata);
                }
            }
        }
        
        return tables;
    }
    
    public TableMetadata getTable(String keyspaceName, String tableName) {
        Metadata metadata = cluster.getMetadata();
        List<KeyspaceMetadata> keyspaceMetadataList = metadata.getKeyspaces();
        Map<String, TableMetadata> tables = new HashMap<String, TableMetadata>();
        LOGGER.info("keyspaceMetadataList.size() = {}", keyspaceMetadataList.size());
        
        for (KeyspaceMetadata ksMetadata : keyspaceMetadataList) {
            LOGGER.info("keyspace name = {}", ksMetadata.getName());
            if (ksMetadata.getName().equals(keyspaceName)) {               
                for (TableMetadata tableMetadata : ksMetadata.getTables()) {
                    if (tableMetadata.getName().equals(tableName)) {
                        return tableMetadata;
                    }
                }
            }
        }
        
        return null;
    }
    
    public boolean hasTable(String tableName, String keyspaceName) {
        Map<String, TableMetadata> tables = getTables(keyspaceName);
        
        return tables.containsKey(tableName);
    }
    
    public boolean hasColumn(String tableName, String columnName) {
        TableMetadata tableMetadata = getTable(this.keyspaceName, tableName);
        if (tableMetadata == null) {
            return false;
        }
        return tableMetadata.getColumn(columnName) != null;
    }
    
    public void addColumn(String tableName, String columnName) {
        tableName = normalizeCqlName(tableName);
        columnName = normalizeCqlName(columnName);
        if (tableName != null && columnName != null && !hasColumn(tableName, columnName)) {
            LOGGER.info("Column {} does not exist in {} - altering table to add it", columnName, tableName);
            if (columnName.endsWith("_txt_set")) {
                addColumn(tableName, columnName, "set<text>");
            } else if (columnName.endsWith("_xml") || columnName.endsWith("_txt")) {
                addColumn(tableName, columnName, "text");
            } else if (columnName.endsWith("_lng")) {
                addColumn(tableName, columnName, "bigint");
            } else if (columnName.endsWith("_int")) {
                addColumn(tableName, columnName, "int");
            } else if (columnName.endsWith("_bin") || columnName.endsWith("_obj")) {
                addColumn(tableName, columnName, "blob");
            } else if (columnName.endsWith("_tdt")) {
                addColumn(tableName, columnName, "timestamp");
            } else if (columnName.endsWith("_geo")) {
                LOGGER.info("Suffix _geo is not yet implemented for Cassandra");
            } else {
                LOGGER.info("Suffix not supported on columnName {}", columnName);
            }
        } else {
            LOGGER.info("Table {} already has column {} in it", tableName, columnName);
        }
    }
    
    public void addColumn(String tableName, String columnName, String columnType) {
        Session session = getSession(this.keyspaceName);
        String cql = "ALTER TABLE " + tableName + " ADD " + columnName + " " + columnType;
        LOGGER.info("Adding column:  {}", cql);
        session.execute(cql);
    }
    
    public void addEntry(String keyspaceName, String cql) {
        LOGGER.info("Executing CQL:  {}", cql);
        Session session = getSession(keyspaceName);
        session.execute(cql);
    }
    
    public void addEntryPrepared(String keyspaceName, String query, Object[] values) {
        LOGGER.info("Executing CQL:  {}", query);
        Session session = getSession(keyspaceName);
        session.execute(query, values);
    }    
    
    /**
     * Only alphanumerics and underscores are allowed in CQL table and column names -
     * replace any dashes with underscores in the table/column name and verify the
     * name is then compliant with CQL.
     * 
     * @param name
     * @return
     */
    public static String normalizeCqlName(String name) {
        name = name.trim().replaceAll("-",  "_");
        Matcher matcher = ALPHA_NUMERIC_UNDERSCORE_REGEX.matcher(name);
        if (matcher.matches()) {
            return name;
        } 
        //TODO  eventually normalize name such that never have to return null
        LOGGER.info("Column name [{}] could not be normalized for CQL - return null", name);
        return null;
    }

    /**
     * Solr strips the dashes from the UUID before it stores it in the SolrInputDocument -
     * this method reinserts the dashes, which are required by CQL's uuid schema type.
     * 
     * @param uuid
     * @return
     */
    public static String normalizeUuid(String uuid) {
        Matcher matcher = UUID_REGEX.matcher(uuid);
        if (!matcher.matches()) {
            uuid = uuid.replaceAll("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",                            
                "$1-$2-$3-$4-$5");
        }
        return uuid;
    }
    
    public void shutdown() {
        LOGGER.info("Embedded Cassandra shutdown() invoked ...");
        //TODO ...
        //session.close();   //will this do it???
    }

    public void getEntry(String type, String cql) {
        LOGGER.info("Not yet implemented ...");
    }

    public void addEntry(String type, Map<String, Object> properties) {
        // TODO Auto-generated method stub
        
    }
}
