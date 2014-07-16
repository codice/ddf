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
package org.codice.solr.query;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.apache.solr.schema.IndexSchema;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Test;
import org.junit.Ignore;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SolrQueryFilterVisitorTest {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(SolrQueryFilterVisitorTest.class);
    
    public static final String CORE_NAME = "core1";

    @Test
    @Ignore
    public void test() throws Exception {
        LOGGER.info("Running test ...");
        
        // setup
        String workingDir = System.getProperty("user.dir") + "/src/test/resources/";
        String solrConfDir = workingDir + "solr/conf/";
        File solrConfigFile = new File(solrConfDir + "solrconfig.xml");  //getConfigFile(solrConfigFileName, configProxy);
        assertTrue(solrConfigFile.exists());
        File solrSchemaFile = new File(solrConfDir + "schema.xml");  //getConfigFile(schemaFileName, configProxy);
        assertTrue(solrSchemaFile.exists());
        File solrFile = new File(solrConfDir + "solr.xml");  //getConfigFile(DEFAULT_SOLR_XML, configProxy);
        assertTrue(solrFile.exists());

        File solrConfigHome = new File(solrConfigFile.getParent());
        assertTrue(solrConfigHome.exists());

        SolrConfig solrConfig = null;
        IndexSchema indexSchema = null;
        CoreContainer container = null;

        try {
            // NamedSPILoader uses the thread context classloader to lookup
            // codecs, posting formats, and analyzers
            solrConfig = new SolrConfig(solrConfigHome.getParent(), "solrConfig.xml",
                    new InputSource(FileUtils.openInputStream(solrConfigFile)));
            indexSchema = new IndexSchema(solrConfig, "schema.xml", new InputSource(
                    FileUtils.openInputStream(solrSchemaFile)));
            container = CoreContainer.createAndLoad(solrConfigHome.getAbsolutePath(),
                    solrFile);
        } catch (ParserConfigurationException e) {
            LOGGER.warn("Parser configuration exception loading index schema", e);
        } catch (IOException e) {
            LOGGER.warn("IO exception loading index schema", e);
        } catch (SAXException e) {
            LOGGER.warn("SAX exception loading index schema", e);
        }

        CoreDescriptor coreDescriptor = new CoreDescriptor(container, CORE_NAME, solrConfig
                .getResourceLoader().getInstanceDir());

        File dataDir = new File(workingDir + "data");  //configProxy.getDataDirectory();
        LOGGER.debug("Using data directory [{}]", dataDir);
        SolrCore core = new SolrCore(CORE_NAME, dataDir.getAbsolutePath(), solrConfig, indexSchema,
                coreDescriptor);
        container.register(CORE_NAME, core, false);

        EmbeddedSolrServer solrServer = new EmbeddedSolrServer(container, CORE_NAME);
        
        // the test
        SolrQueryFilterVisitor visitor = new SolrQueryFilterVisitor(solrServer, CORE_NAME);
        Filter filter = ECQL.toFilter("Name = 'Hugh'");
        SolrQuery solrQuery = (SolrQuery) filter.accept(visitor, null);
        assertNotNull(solrQuery);
        
        // Solr does not support outside parenthesis in certain queries and throws EOF exception.
        String queryPhrase = solrQuery.getQuery().trim();
        if (queryPhrase.matches("\\(\\s*\\{!.*\\)")) {
            solrQuery.setQuery(queryPhrase.replaceAll("^\\(\\s*|\\s*\\)$", ""));
        }
        LOGGER.info("solrQuery = {}", solrQuery);
        
        QueryResponse solrResponse = solrServer.query(solrQuery, METHOD.POST);
        assertNotNull(solrResponse);
        long numResults = solrResponse.getResults().getNumFound();
        LOGGER.info("numResults = {}", numResults);
    }

}
