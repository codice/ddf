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
package ddf.catalog.test;

import com.jayway.restassured.response.Response;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.fail;

/**
 * Tests the Catalog framework components. Includes helper methods at the Catalog level.
 * 
 * @author Ashraf Barakat
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalog extends AbstractIntegrationTest {

    private static final String EXTERNAL_SOLR_CONFIG_PID = "ddf.catalog.solr.external.SolrHttpCatalogProvider";

    public static final String CACHING_FEDERATION_STRATEGY_PID = "ddf.catalog.federation.impl.CachingFederationStrategy";

    protected static final String SERVICE_ROOT = "http://localhost:" + HTTP_PORT + "/services";

    protected static final String REST_PATH = SERVICE_ROOT + "/catalog/";

    protected static final String OPENSEARCH_PATH = REST_PATH + "query/";

    private static boolean ranBefore = false;

    @Before
    public void beforeTest() {
        LOGGER.info("Before {}", testName.getMethodName());
        if (!ranBefore) {
            try {
                setLogLevels();
                configureBundles();
                waitForAllBundles();
                waitForCatalogProvider();
                waitForCxf();
                ranBefore = true;
            } catch (Exception e) {
                LOGGER.error("Failed to setup test", e);
                fail();
            }
        }
        LOGGER.info("Starting {}", testName.getMethodName());
    }

    @After
    public void afterTest() {
        LOGGER.info("End of {}", testName.getMethodName());
    }

    private void waitForCxf() throws InterruptedException {
        LOGGER.info("Waiting for CXF");
        boolean isCxfReady = false;
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        while (!isCxfReady) {
            Response response = get(SERVICE_ROOT);
            isCxfReady = response.getStatusCode() == 200 && response.getBody().print().contains(
                    "catalog/query");
            if (!isCxfReady) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    fail("CXF did not start in time.");
                }
                LOGGER.info("CXF not up, sleeping...");
                Thread.sleep(1000);
            }
        }
        LOGGER.info("Source status: \n");
        when().get(REST_PATH + "sources").then().log().all();
    }

    private void configureBundles() throws IOException, InterruptedException {
        LOGGER.info("Updating Solr configuration.");

        Configuration fedConfig = configAdmin.getConfiguration(CACHING_FEDERATION_STRATEGY_PID,
                null);

        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put("url", "http://localhost:" + HTTP_PORT + "/solr");

        fedConfig.update(properties);

        properties.put("forceAutoCommit", "true");

        Configuration solrConfig = configAdmin.getConfiguration(EXTERNAL_SOLR_CONFIG_PID, null);
        solrConfig.update(properties);
    }

    @Test
    public void testMetacardTransformersFromRest() {
        String id = ingestGeoJson(Library.getSimpleGeoJson());

        String url = REST_PATH + id;
        LOGGER.info("Getting response to {}", url);
        when().get(url).then().log().all().assertThat().body(
                hasXPath("/metacard[@id='" + id + "']"));

        deleteMetacard(id);
    }

    @Test
    public void testOpenSearchQuery() {
        String id = ingestGeoJson(Library.getSimpleGeoJson());

        String url = OPENSEARCH_PATH + "?q=*&format=xml";
        LOGGER.info("Getting response to {}", url);
        expect().body(containsString(id)).when().get(url).prettyPrint();

        deleteMetacard(id);
    }

    protected void deleteMetacard(String id) {
        LOGGER.info("Deleteing metacard {}", id);
        delete(REST_PATH + id).then().assertThat().statusCode(200).log().all();
    }

    protected String ingestGeoJson(String json) {
        return ingest(json, "application/json");
    }

    protected String ingest(String data, String mimeType) {
        LOGGER.info("Ingesting data of type {}:\n{}", mimeType, data);
        return given().body(data).header("Content-Type", mimeType).expect().log().all()
                .statusCode(201).when().post(REST_PATH).getHeader("id");
    }

}
