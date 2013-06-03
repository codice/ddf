/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.test;

import static com.jayway.restassured.RestAssured.expect;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasXPath;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.service.cm.Configuration;

/**
 * Tests the Catalog framework components. Includes helper methods at the
 * Catalog level.
 * 
 * @author Ashraf Barakat
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class TestCatalog extends AbstractIntegrationTest {

    private static final String SOLR_CONFIG_PID = "ddf.catalog.source.solr.SolrCatalogProvider";
    private static final String EXTERNAL_SOLR_CONFIG_PID = "ddf.catalog.solr.external.SolrHttpCatalogProvider";
    private static final String CATALOG_SYMBOLIC_NAME_PREFIX = "catalog-";

    private static final String SERVICE_ROOT = "http://localhost:" + HTTP_PORT
            + "/services";
    private static final String REST_PATH = SERVICE_ROOT + "/catalog/";
    private static final String OPENSEARCH_PATH = REST_PATH + "query/";

    @Before
    public void beforeTest() throws InterruptedException, IOException {
        setLogLevels();
        waitForRequiredBundles(CATALOG_SYMBOLIC_NAME_PREFIX);
        setSolrSoftCommit();
        waitForCatalogProviderToBeAvailable();
    }

    private void setSolrSoftCommit() throws IOException {
        Configuration solrConfig = configAdmin.getConfiguration(
                SOLR_CONFIG_PID, null);
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("forceAutoCommit", "true");
        solrConfig.update(properties);
    }

    @Test
    public void testMetacardTransformersFromRest() throws Exception {
        String id = ingestGeoJson(Library.getSimpleGeoJson());

        expect().body(hasXPath("/metacard[@id='" + id + "']")).when()
                .get(REST_PATH + id);
        
        deleteMetacard(id);
    }
    
    @Test
    public void testOpenSearchQuery() throws Exception {
        String id = ingestGeoJson(Library.getSimpleGeoJson());

        expect().body(containsString(id)).when()
                .get(OPENSEARCH_PATH + "?q=*&format=xml");
        
        deleteMetacard(id);
    }
    
    @Test
    public void testExternalSolr() throws Exception {
        installExternalSolrAndProvider();
        
        testOpenSearchQuery();
    }

    private void installExternalSolrAndProvider() throws Exception,
            InterruptedException, IOException {
        features.uninstallFeature("catalog-solr-embedded-provider");
        features.installFeature("catalog-solr-server");
        features.installFeature("catalog-solr-external-provider");
        
        waitForRequiredBundles(CATALOG_SYMBOLIC_NAME_PREFIX);
        configureExternalSolrProvider();
        waitForCatalogProviderToBeAvailable();
    }

    private void configureExternalSolrProvider() throws IOException {
        Configuration solrConfig = configAdmin.getConfiguration(
                EXTERNAL_SOLR_CONFIG_PID, null);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("url", "http://localhost:" + HTTP_PORT + "/solr");
        properties.put("forceAutoCommit", "true");
        
        solrConfig.update(properties);
    }

    private void deleteMetacard(String id) {
        expect().statusCode(200).when().delete(REST_PATH + id);
    }

    private String ingestGeoJson(String json) {
        return ingest(json, "application/json");
    }
    
    private String ingestXml(String xml) {
        return ingest(xml, "text/xml");
    }
    
    private String ingest(String data, String mimeType) {
        return expect().statusCode(201).when().given().body(data)
                .header("Content-Type", mimeType).post(REST_PATH)
                .getHeader("id");
    }

}
