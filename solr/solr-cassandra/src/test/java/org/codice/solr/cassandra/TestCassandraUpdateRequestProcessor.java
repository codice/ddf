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

import org.junit.Test;

public class TestCassandraUpdateRequestProcessor {

    @Test
    public void test() throws Exception {   
        /*
        XmlInputTransformer xit = new XmlInputTransformer();
        Metacard metacard = xit.transform(new FileInputStream("src/test/resources/sample-goodyear.xml"));

        DynamicSchemaResolver resolver = new DynamicSchemaResolver();
        SolrInputDocument solrInputDocument = new SolrInputDocument();
        resolver.addFields(metacard, solrInputDocument);
        CassandraClient cc = mock(CassandraClient.class);
        UpdateRequestProcessor urp = mock(UpdateRequestProcessor.class);
        CassandraUpdateRequestProcessor curp = new CassandraUpdateRequestProcessor("metacard", cc, urp);
        curp.persistToCassandraPrepared(solrInputDocument);
        */
    }

}
