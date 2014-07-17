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

import javax.xml.bind.DatatypeConverter;

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
            "id_txt text PRIMARY KEY, createddate_tdt timestamp";
            //"id_txt uuid PRIMARY KEY, createddate_tdt timestamp";
    
    private String storeName;
    private CassandraClient cassandraClient;

    public CassandraUpdateRequestProcessor(String storeName, CassandraClient cassandraClient, UpdateRequestProcessor next) {
        super(next);
        LOGGER.trace("INSIDE: CassandraUpdateRequestProcessor constructor");
        this.storeName = storeName;
        this.cassandraClient = cassandraClient;
    }
    
    @Override
    public void processAdd(AddUpdateCommand cmd) throws IOException {
        LOGGER.trace("ENTERING: processAdd()");
        try {
            // Add SolrInputDocument's contents into Cassandra table corresponding to SolrCore
            SolrInputDocument solrInputDocument = cmd.getSolrInputDocument();
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
        LOGGER.trace("ENTERING: processCommit()");
        super.processCommit(cmd);
    }
    
    @Override
    public void processDelete(DeleteUpdateCommand cmd) throws IOException {
        LOGGER.trace("ENTERING: processDelete()");
        String id = cmd.getId();
        LOGGER.debug("Deleting row with id = {}", id);
        cassandraClient.deleteEntryById(storeName, id);
        super.processDelete(cmd);
    }
    
    @Override
    public void processRollback(RollbackUpdateCommand cmd) throws IOException {
        LOGGER.trace("ENTERING: processRollback()");
        super.processRollback(cmd);
    }

    public void persistToCassandra(SolrInputDocument solrInputDocument) {
        LOGGER.trace("ENTERING: persistToCassandraPrepared()");
        
        String columnNamesClause = "";
        String valuesClause = "";

        int count = 1;
        Collection<String> fieldNames = solrInputDocument.getFieldNames();
        List<Object> preparedValues = new ArrayList<Object>();
        for (String fieldName : fieldNames) {
            LOGGER.debug("Working on field name {}", fieldName);
            SolrInputField fieldValue = (SolrInputField) solrInputDocument.getField(fieldName);

            boolean validColumn = false;
            if (fieldName.equals("id_txt")) {
                String value = (String) fieldValue.getValue();
                // No longer necessary since changed id_txt to a text type
                //preparedValues.add(CassandraClient.normalizeUuid(value));  // do not quote UUID value
                preparedValues.add("'" + value + "'");
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
                LOGGER.debug("Ignoring field {} as it should not be stored in Cassandra", fieldName);
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
                // Should be a WKT string for geometry data
                String value = (String) fieldValue.getValue();
                preparedValues.add("'" + value + "'");
                valuesClause += "?";
                validColumn = true;
            } else if (fieldName.endsWith("_tdt")) {
                String value = (String) fieldValue.getValue();
                long dateTimeInMillis = DatatypeConverter.parseDateTime(value).getTimeInMillis();
                preparedValues.add(dateTimeInMillis);
                valuesClause += "?";
                validColumn = true;
            } else if (fieldName.endsWith("_obj") || fieldName.endsWith("_bin")) {
                //LOGGER.debug("_obj and _bin fields not yet supported - fieldName = {}", fieldName);
                byte[] bytes = null;
                if (byte[].class.isAssignableFrom(fieldValue.getValue().getClass())) {
                    LOGGER.debug("fieldValue is already a byte[]");
                    bytes = (byte[]) fieldValue.getValue();
                } else if (String.class.isAssignableFrom(fieldValue.getValue().getClass())) {
                    LOGGER.debug("fieldValue is String that will be converted to a byte[]");
                    bytes = ((String) fieldValue.getValue()).getBytes();
                }
                //byte[] bytes = (byte[]) fieldValue.getValue();
                if (bytes != null && bytes.length > 0) {
                    LOGGER.debug("Wrapping {} bytes as a prepared value", bytes.length);
                    preparedValues.add(ByteBuffer.wrap(bytes));
                    valuesClause += "?";
                    validColumn = true;
                } else {
                    LOGGER.info("Could not get fieldValue as a byte[]");
                }
            }

            if (validColumn) {
                columnNamesClause += CassandraClient.normalizeCqlName(fieldName);
                if (count != fieldNames.size()) {
                    columnNamesClause += ", ";
                    valuesClause += ", ";
                }
            } else {
                LOGGER.debug("Column {} was deemed invalid and not added to the INSERT", fieldName);
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
        LOGGER.debug("cql = {}", cql);
        try {
            cassandraClient.addEntryPrepared(CASSANDRA_KEYSPACE_NAME, cql, preparedValues.toArray());
        } catch (Exception e) {
            LOGGER.debug("Exception trying to INSERT entry for table {}. Will attempt to create table/schema and retry INSERT", storeName);
            // Assume exception is because Cassandra table or its columns do not exist yet.
            // Create and execute the CQL commands to create the table with a schema based on
            // the field types in the SolrInputDoument (the suffixes on the field names will indicate
            // the table schema to generate, e.g., "_txt" suffix indicates a "text" column type).
            createTable(storeName, columnNamesClause.split(","));
            
            // Re-execute CQL to insert entry
            LOGGER.debug("Re-trying INSERT with cql = {}", cql);
            cassandraClient.addEntryPrepared(CASSANDRA_KEYSPACE_NAME, cql, preparedValues.toArray());
        }
        LOGGER.trace("EXITING: persistToCassandraPrepared()");
    }
    
    private void createTable(String tableName, String[] columnNames) {
        cassandraClient.createTable(CASSANDRA_KEYSPACE_NAME, tableName, CASSANDRA_BASE_TABLE_SCHEMA);
        
        for (String columnName : columnNames) {
            cassandraClient.addColumn(tableName, columnName);
        }
    }

}
