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

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.source.FederatedSource;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.spi.reactors.EagerSingleStagedReactorFactory;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
* Tests Federation aspects.
*
* @author Ashraf Barakat
* @author Phillip Klinefelter
* @author ddf.isgs@lmco.com
*
*/
@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(EagerSingleStagedReactorFactory.class)
public class TestFederation extends TestCatalog {

    private static XLogger LOGGER = new XLogger(LoggerFactory.getLogger(TestFederation.class));

    private static final String SAMPLE_DATA = "sample data";

    private static final int XML_RECORD_INDEX = 1;

    private static final int GEOJSON_RECORD_INDEX = 0;

    /* ************************ */

    private static final String DEFAULT_KEYWORD = "text";

    private static final String RECORD_TITLE_1 = "myTitle";

    private static final String RECORD_TITLE_2 = "myXmlTitle";

    private static final String REMOTE_SITE_NAME = "remoteSite";

    /*
     * The fields must be static if they are purposely used across all test methods.
     */
    private static boolean ranBefore = false;

    private static String[] metacardIds = new String[2];

    private static FederatedSource source;

    /**
     * Runs each time before each test, items that don't need to be run each time have a conditional
     * flag.
     *
     * @throws InterruptedException
     * @throws IOException
     */
    @Before
    public void beforeFederation() {

        if (!ranBefore) {
            try {
                LOGGER.info("Running one-time federation setup.");

                FederatedSourceProperties sourceProperties = new FederatedSourceProperties();

                createManagedService(FederatedSourceProperties.FACTORY_PID,
                        sourceProperties.createDefaultProperties(String.valueOf(HTTP_PORT),
                                REMOTE_SITE_NAME), 1000);

                this.source = waitForFederatedSource();
                File file = new File("sample.txt");
                file.createNewFile();
                FileUtils.write(file, SAMPLE_DATA);
                String fileLocation = file.toURI().toURL().toString();
                metacardIds[GEOJSON_RECORD_INDEX] = ingest(Library.getSimpleGeoJson(),
                        "application/json");

                LOGGER.debug("File Location: {}", fileLocation);
                metacardIds[XML_RECORD_INDEX] = ingest(Library.getSimpleXml(fileLocation), "text/xml");
                ranBefore = true;
            } catch (Exception e) {
                LOGGER.error("Failed to setup federation.", e);
                fail("Failed to setup federation.");
            }
        }

    }

    /**
     * Given what was ingested in beforeTest(), tests that a Federated wildcard search will return
     * all appropriate record(s).
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryByWildCardSearchPhrase() throws Exception {

        // given

        // when
        String queryUrl = OPENSEARCH_PATH + "?q=*&format=xml&src=" + REMOTE_SITE_NAME;

        String result = read(queryUrl);

        // then
        assertNotNull(result);
        assertTrue("Record should include the first record title.", result.contains(RECORD_TITLE_1));
        assertTrue("Record should include the second record title.",
                result.contains(RECORD_TITLE_2));

    }

    /**
     * Given what was ingested in beforeTest(), tests that a Federated search phrase will return the
     * appropriate record(s).
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryBySearchPhrase() throws Exception {

        // given

        // when
        String queryUrl = OPENSEARCH_PATH + "?q=" + DEFAULT_KEYWORD + "&format=xml&src="
                + REMOTE_SITE_NAME;

        String result = read(queryUrl);

        // then
        assertNotNull(result);
        assertTrue("Record should include the first record title.", result.contains(RECORD_TITLE_1));
        assertTrue("Record should include the second record title.",
                result.contains(RECORD_TITLE_2));

    }

    /**
     * Tests that given a bad test phrase, no records should have been returned.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryByNegativeSearchPhrase() throws Exception {

        // given

        // when
        String negativeSearchPhrase = "negative";
        String queryUrl = OPENSEARCH_PATH + "?q=" + negativeSearchPhrase + "&format=xml&src="
                + REMOTE_SITE_NAME;
        String result = read(queryUrl);

        // then
        assertNotNull(result);
        assertTrue("No records should have been returned.", !result.contains(RECORD_TITLE_1));

    }

    /**
     * Tests that a federated search by ID will return the right record.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryById() throws Exception {

        // given

        // when
        String restUrl = REST_PATH + "sources/" + REMOTE_SITE_NAME + "/"
                + metacardIds[GEOJSON_RECORD_INDEX];

        String result = read(restUrl);

        // then
        assertNotNull(result);
        LOGGER.debug("testFederatedQueryById result\n" + result);
        assertTrue("Record should include the right title.", result.contains(RECORD_TITLE_1));

    }

    /**
     * Tests Source can retrieve product existing product.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveExistingProduct() throws Exception {

        // given
        Map<String, Serializable> requestProperties = new Hashtable<String, Serializable>();
        requestProperties.put(Metacard.ID, metacardIds[XML_RECORD_INDEX]);

        // when
        ResourceResponse response = source.retrieveResource(null, requestProperties);

        // then
        String mimeTypeValue = response.getResource().getMimeTypeValue();
        LOGGER.info("MimeType returned [{}]", mimeTypeValue);
        assertEquals("text/plain", mimeTypeValue);
        assertEquals(SAMPLE_DATA, IOUtils.toString(response.getResource().getInputStream()));
    }

    /**
     * Tests Source can retrieve nonexistent product.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveNoProduct() throws Exception {
        String restUrl = REST_PATH + "sources/" + REMOTE_SITE_NAME + "/"
                + metacardIds[GEOJSON_RECORD_INDEX] + "?transform=resource";

        LOGGER.info(get(restUrl).prettyPrint());
        expect().log().all().body(containsString("Unknown resource request")).when().get(restUrl);
    }

    private FederatedSource waitForFederatedSource() throws InterruptedException {
        ServiceTracker st = new ServiceTracker(bundleCtx, FederatedSource.class.getName(), null);
        st.open();
        FederatedSource source = (FederatedSource) st.waitForService(5000);

        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
        boolean available = source.isAvailable();

        while (!available && System.currentTimeMillis() < timeoutLimit) {
            available = source.isAvailable();
            if (!available) {
                Thread.sleep(100);
            }
        }

        if (!available) {
            fail("Federated Source was not created in a timely manner.");
        }

        return source;
    }

    private String read(String getUrl) throws IOException {
        return get(getUrl).getBody().prettyPrint();
    }

    public class FederatedSourceProperties extends Hashtable<String, Object> {

        public static final String FACTORY_PID = "OpenSearchSource";

        public Dictionary<String, Object> createDefaultProperties(String port, String remoteSiteName) {

            this.put("shortname", remoteSiteName);
            this.put("endpointUrl", "http://localhost:" + port + "/services/catalog/query");
            this.put("localQueryOnly", "true");

            return this;
        }

    }

}
