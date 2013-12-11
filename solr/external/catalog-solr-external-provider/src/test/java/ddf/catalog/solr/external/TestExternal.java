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
package ddf.catalog.solr.external;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Ignore;

import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.source.solr.ConfigurationStore;
import ddf.catalog.source.solr.SolrCatalogProvider;
import ddf.catalog.source.solr.SolrFilterDelegateFactoryImpl;
import ddf.catalog.source.solr.SolrServerFactory;
import ddf.catalog.source.solr.TestSolrProvider;

/**
 * This is a convenience class to run integration and regression tests against an external Solr
 * Server.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
@Ignore
public class TestExternal extends TestSolrProvider {

    private static final Logger LOGGER = Logger.getLogger(TestExternal.class);

    @BeforeClass
    public static void setup() throws Exception {
        LOGGER.info("RUNNING setup.");

        SolrCatalogProvider solrCatalogProvider = new SolrCatalogProvider(
                SolrServerFactory.getHttpSolrServer("http://localhost:8181/solr"),
                new GeotoolsFilterAdapterImpl(), new SolrFilterDelegateFactoryImpl());

        /*
         * necessary for automated test purposes. forces commits so that verification of tests will
         * have valid counts/results
         */
        ConfigurationStore.getInstance().setForceAutoCommit(true);

        provider = solrCatalogProvider;

        // Mask the id, this is something that the CatalogFramework would
        // usually do
        provider.setId(MASKED_ID);
    }

}
