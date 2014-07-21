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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.cassandraunit.utils.EmbeddedCassandraServerHelper;
import org.codice.ddf.cassandra.CassandraClient;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;

public class TestCassandraUpdateRequestProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCassandraUpdateRequestProcessor.class);
    
    
    @Test
    @Ignore
    public void test() throws Exception { 
        //cassandra-unit approach for starting embedded Cassandra server
        String cassandraHost = "localhost";
        int cassandraPort = 9052;
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/";
        System.setProperty("log4j.configuration", "file:" + workingDir + "log4j.properties");
        EmbeddedCassandraServerHelper.startEmbeddedCassandra("/cassandra.yaml");
        Cluster cluster = new Cluster.Builder().addContactPoints(cassandraHost).withPort(cassandraPort).build();
        Session session = cluster.connect();
        session.execute("CREATE KEYSPACE IF NOT EXISTS ddf WITH replication={'class' : 'SimpleStrategy', 'replication_factor':1}");
        session.execute("USE ddf");
        
        CassandraClient cc = new CassandraClient(cassandraHost, cassandraPort);
        UpdateRequestProcessor urp = mock(UpdateRequestProcessor.class);
        CassandraUpdateRequestProcessor curp = new CassandraUpdateRequestProcessor("metacard", cc, urp);

        SolrInputDocument solrInputDocument = new SolrInputDocument();
        solrInputDocument.addField("id_txt", "44a429c34c3748ecbda0e5b501c524b3");
        solrInputDocument.addField("location_geo", "POINT (-112.39018 33.42904)");
        solrInputDocument.addField("created_tdt", "2014-07-16T19:49:48.506Z");

        curp.persistToCassandra(solrInputDocument);

        ResultSet results = session.execute("SELECT * FROM metacard");
        assertNotNull(results);
        Row row = results.one();
        assertNotNull(row);
        String locationGeo = row.getString("location_geo");
        LOGGER.info("locationGeo = {}", locationGeo);
        
        Date createdDate = row.getDate("created_tdt");
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(tz);
        LOGGER.info("created date = {}", df.format(createdDate));
    }

}
