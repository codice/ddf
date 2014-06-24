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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.CommitUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.RollbackUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.codice.ddf.cassandra.CassandraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CassandraUpdateRequestProcessor extends UpdateRequestProcessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraUpdateRequestProcessor.class);
    
    private static final String CASSANDRA_KEYSPACE_NAME = "ddf";
    
    // The minimum schema/columns that every Cassandra table has
    private static final String CASSANDRA_BASE_TABLE_SCHEMA = 
            "id_txt uuid PRIMARY KEY, createddate_tdt timestamp";
    
    private String storeName;
    private CassandraClient cassandraClient;

    public CassandraUpdateRequestProcessor(String storeName, CassandraClient cassandraClient, UpdateRequestProcessor next) {
        super(next);
        LOGGER.info("INSIDE: CassandraUpdateRequestProcessor constructor");
        this.storeName = storeName;
        this.cassandraClient = cassandraClient;
    }
    
    @Override
    public void processAdd(AddUpdateCommand cmd) throws IOException {
        LOGGER.info("ENTERING: processAdd()");
        try {
            // Add SolrInputDocument's contents into Cassandra table corresponding to SolrCore
            SolrInputDocument solrInputDocument = cmd.getSolrInputDocument();
//            persistToCassandraPrepared(solrInputDocument);
            persistToCassandra(solrInputDocument);
            
            // Call next UpdateRequestProcessor in the chain
            super.processAdd(cmd);
        } catch (Exception e) {
            LOGGER.info("Need to do rollback here");
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void processCommit(CommitUpdateCommand cmd) throws IOException {
        LOGGER.info("ENTERING: processCommit()");
        super.processCommit(cmd);
    }
    
    @Override
    public void processDelete(DeleteUpdateCommand cmd) throws IOException {
        LOGGER.info("ENTERING: processDelete()");
        super.processDelete(cmd);
    }
    
    @Override
    public void processRollback(RollbackUpdateCommand cmd) throws IOException {
        LOGGER.info("ENTERING: processRollback()");
        super.processRollback(cmd);
    }

    public void persistToCassandraPrepared(SolrInputDocument solrInputDocument) {
        LOGGER.info("ENTERING: persistToCassandraPrepared()");
        
        String columnNamesClause = "";
        String valuesClause = "";

        int count = 1;
        Collection<String> fieldNames = solrInputDocument.getFieldNames();
        List<Object> preparedValues = new ArrayList<Object>();
        for (String fieldName : fieldNames) {
            LOGGER.info("Working on field name {}", fieldName);
            SolrInputField fieldValue = (SolrInputField) solrInputDocument.getField(fieldName);

            boolean validColumn = false;
            if (fieldName.equals("id_txt")) {
                String value = (String) fieldValue.getValue();
                preparedValues.add(CassandraClient.normalizeUuid(value));  // do not quote UUID value
                valuesClause += "?";
                validColumn = true;
            } else if (fieldName.endsWith("_txt_set")) {
                Collection<Object> values = fieldValue.getValues();     
                if (values != null && !values.isEmpty()) {
                    preparedValues.add(new HashSet<Object>(values));
                    valuesClause += "?";
                    validColumn = true;
                }
            } else if (fieldName.equals("lux_xml")) {
                LOGGER.info("Ignoring field {} as it should not be stored in Cassandra", fieldName);
            } else if (fieldName.endsWith("_xml") || fieldName.endsWith("_txt")) {
                String value = (String) fieldValue.getValue();
                preparedValues.add("'" + value + "'");
                valuesClause += "?";
                validColumn = true;
            } else if (fieldName.endsWith("_lng")) {
                Long value = Long.valueOf((String) fieldValue.getValue());
                preparedValues.add(value);
                valuesClause += "?";
                validColumn = true;
            } else if (fieldName.endsWith("_int")) {
                Integer value = Integer.valueOf((String) fieldValue.getValue());
                preparedValues.add(value);
                valuesClause += "?";
                validColumn = true;
            } else if (fieldName.endsWith("_geo")) {
                LOGGER.info("_geo field not yet supported");
            } else if (fieldName.endsWith("_tdt")) {
                LOGGER.info("_tdt field not yet supported");
            } else if (fieldName.endsWith("_obj") || fieldName.endsWith("_bin")) {
                //LOGGER.info("_obj and _bin fields not yet supported - fieldName = {}", fieldName);
                byte[] bytes = (byte[]) fieldValue.getValue();
                preparedValues.add(ByteBuffer.wrap(bytes));
                valuesClause += "?";
                validColumn = true;
            }

            if (validColumn) {
                columnNamesClause += CassandraClient.normalizeCqlName(fieldName);
                if (count != fieldNames.size()) {
                    columnNamesClause += ", ";
                    valuesClause += ", ";
                }
            } else {
                LOGGER.info("Column {} was deemed invalid and not added to the INSERT", fieldName);
            }

            count++;
        }
            
        columnNamesClause = columnNamesClause.trim();
        if (columnNamesClause.endsWith(",")) {
            columnNamesClause = StringUtils.chop(columnNamesClause);
        }
        valuesClause = valuesClause.trim();
        if (valuesClause.endsWith(",")) {
            valuesClause = StringUtils.chop(valuesClause);
        }
        String cql = "INSERT INTO " + storeName + "(";
        cql += columnNamesClause + ") VALUES (" + valuesClause + ")";
        LOGGER.info("cql = {}", cql);
        try {
            cassandraClient.addEntryPrepared(CASSANDRA_KEYSPACE_NAME, cql, preparedValues.toArray());
        } catch (Exception e) {
            LOGGER.info("Exception trying to INSERT entry for table {}. Will attempt to create table/schema and retry INSERT", storeName);
            // Assume exception is because Cassandra table or its columns do not exist yet.
            // Create and execute the CQL commands to create the table with a schema based on
            // the field types in the SolrInputDoument (the suffixes on the field names will indicate
            // the table schema to generate, e.g., "_txt" suffix indicates a "text" column type).
            createTable(storeName, columnNamesClause.split(","));
            
            // Re-execute CQL to insert entry
            LOGGER.info("Re-trying INSERT with cql = {}", cql);
            cassandraClient.addEntryPrepared(CASSANDRA_KEYSPACE_NAME, cql, preparedValues.toArray());
        }
        LOGGER.info("EXITING: persistToCassandraPrepared()");
    }

    public void persistToCassandra(SolrInputDocument solrInputDocument) {
        LOGGER.info("ENTERING: persistToCassandra()");
        
        String columnNamesClause = "";
        String valuesClause = "";

        int count = 1;
        Collection<String> fieldNames = solrInputDocument.getFieldNames();
        for (String fieldName : fieldNames) {
            LOGGER.info("Working on field name {}", fieldName);
            SolrInputField fieldValue = (SolrInputField) solrInputDocument.getField(fieldName);

            boolean validColumn = false;
            if (fieldName.equals("id_txt")) {
                String value = (String) fieldValue.getValue();
                valuesClause += CassandraClient.normalizeUuid(value);  // do not quote UUID value
                validColumn = true;
            } else if (fieldName.endsWith("_txt_set")) {
                Collection<Object> values = fieldValue.getValues();
                if (values != null && !values.isEmpty()) {
                    valuesClause += "{";
                    int setCount = 1;
                    for (Object value : values) {
                        valuesClause += "'" + (String) value + "'";
                        if (setCount != values.size()) {
                            valuesClause += ", ";
                        }
                        setCount++;
                    }
                    valuesClause += "}";
                    validColumn = true;
                }
            } else if (fieldName.equals("lux_xml")) {
                LOGGER.info("Ignoring field {} as it should not be stored in Cassandra", fieldName);
            } else if (fieldName.endsWith("_xml") || fieldName.endsWith("_txt")) {
                String value = (String) fieldValue.getValue();
                valuesClause += "'" + value + "'";
                validColumn = true;
            } else if (fieldName.endsWith("_lng")) {
                Long value = Long.valueOf((String) fieldValue.getValue());
                valuesClause += value;
                validColumn = true;
            } else if (fieldName.endsWith("_int")) {
                Integer value = Integer.valueOf((String) fieldValue.getValue());
                valuesClause += value;
                validColumn = true;
            } else if (fieldName.endsWith("_geo")) {
                LOGGER.info("_geo field not yet supported");
            } else if (fieldName.endsWith("_tdt")) {
                LOGGER.info("_tdt field not yet supported");
            } else if (fieldName.endsWith("_obj") || fieldName.endsWith("_bin")) {
                LOGGER.info("_obj and _bin fields not yet supported - fieldName = {}", fieldName);
            }

            if (validColumn) {
                columnNamesClause += CassandraClient.normalizeCqlName(fieldName);
                if (count != fieldNames.size()) {
                    columnNamesClause += ", ";
                    valuesClause += ", ";
                }
            } else {
                LOGGER.info("Column {} was deemed invalid and not added to the INSERT", fieldName);
            }

            count++;
        }
            
        columnNamesClause = columnNamesClause.trim();
        if (columnNamesClause.endsWith(",")) {
            columnNamesClause = StringUtils.chop(columnNamesClause);
        }
        valuesClause = valuesClause.trim();
        if (valuesClause.endsWith(",")) {
            valuesClause = StringUtils.chop(valuesClause);
        }
        String cql = "INSERT INTO " + storeName + "(";
        cql += columnNamesClause + ") VALUES (" + valuesClause + ")";
        LOGGER.info("cql = {}", cql);
        try {
            cassandraClient.addEntry(CASSANDRA_KEYSPACE_NAME, cql);
        } catch (Exception e) {
            LOGGER.info("Exception trying to INSERT entry for table {}. Will attempt to create table/schema and retry INSERT", storeName);
            // Assume exception is because Cassandra table or its columns do not exist yet.
            // Create and execute the CQL commands to create the table with a schema based on
            // the field types in the SolrInputDoument (the suffixes on the field names will indicate
            // the table schema to generate, e.g., "_txt" suffix indicates a "text" column type).
            createTable(storeName, columnNamesClause.split(","));
            
            // Re-execute CQL to insert entry
            LOGGER.info("Re-trying INSERT with cql = {}", cql);
            cassandraClient.addEntry(CASSANDRA_KEYSPACE_NAME, cql);
        }
    }
    
    private void createTable(String tableName, String[] columnNames) {
        cassandraClient.createTable(CASSANDRA_KEYSPACE_NAME, tableName, CASSANDRA_BASE_TABLE_SCHEMA);
        
        for (String columnName : columnNames) {
            cassandraClient.addColumn(tableName, columnName);
        }
    }

}
