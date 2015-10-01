/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.path.json.JsonPath.with;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.jayway.restassured.http.ContentType;

import ddf.catalog.data.Metacard;
import ddf.common.test.BeforeExam;

/**
 * Tests Federation aspects.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestFederation extends AbstractIntegrationTest {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(TestFederation.class));

    private static final String SAMPLE_DATA = "sample data";

    private static final int XML_RECORD_INDEX = 1;

    private static final int GEOJSON_RECORD_INDEX = 0;

    private static final String DEFAULT_KEYWORD = "text";

    private static final String RECORD_TITLE_1 = "myTitle";

    private static final String RECORD_TITLE_2 = "myXmlTitle";

    private static final String CONNECTED_SOURCE_ID = "cswConnectedSource";

    private static final String CSW_SOURCE_WITH_METACARD_XML_ID = "cswSource2";

    private static final String ADMIN_SOURCE_PATH = "https://localhost:" + HTTPS_PORT;

    private static final String ADMIN_ALL_SOURCES_PATH = ADMIN_SOURCE_PATH
            + "/jolokia/exec/org.codice.ddf.catalog.admin.plugin.AdminSourcePollerServiceBean:service=admin-source-poller-service/allSourceInfo";

    private static final String ADMIN_STATUS_PATH = ADMIN_SOURCE_PATH
            + "/jolokia/exec/org.codice.ddf.catalog.admin.plugin.AdminSourcePollerServiceBean:service=admin-source-poller-service/sourceStatus/";

    private static final String DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS = "data/products";

    private static final String DEFAULT_SAMPLE_PRODUCT_FILE_NAME = "sample.txt";

    private static String[] metacardIds = new String[2];

    private String localSourceID = "";

    private List<String> metacardsToDelete = new ArrayList<>();

    private List<String> resourcesToDelete = new ArrayList<>();

    private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

    @Rule
    public TestName testName = new TestName();

    @BeforeExam
    public void beforeExam() throws Exception {
        setLogLevels();
        waitForAllBundles();
        waitForCatalogProvider();
        waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query?_wadl");

        OpenSearchSourceProperties openSearchProperties = new OpenSearchSourceProperties(
                OPENSEARCH_SOURCE_ID);
        createManagedService(OpenSearchSourceProperties.FACTORY_PID, openSearchProperties);

        waitForHttpEndpoint(CSW_PATH + "?_wadl");
        get(CSW_PATH + "?_wadl").prettyPrint();
        CswSourceProperties cswProperties = new CswSourceProperties(CSW_SOURCE_ID);
        createManagedService(CswSourceProperties.FACTORY_PID, cswProperties);

        CswSourceProperties cswProperties2 = new CswSourceProperties(
                CSW_SOURCE_WITH_METACARD_XML_ID);
        cswProperties2.put("outputSchema", "urn:catalog:metacard");
        createManagedService(CswSourceProperties.FACTORY_PID, cswProperties2);

        waitForFederatedSource(OPENSEARCH_SOURCE_ID);
        waitForFederatedSource(CSW_SOURCE_ID);
        waitForFederatedSource(CSW_SOURCE_WITH_METACARD_XML_ID);

        waitForSourcesToBeAvailable(OPENSEARCH_SOURCE_ID, CSW_SOURCE_ID,
                CSW_SOURCE_WITH_METACARD_XML_ID);

        metacardIds[GEOJSON_RECORD_INDEX] = TestCatalog
                .ingest(Library.getSimpleGeoJson(), "application/json");

        metacardIds[XML_RECORD_INDEX] = ingestXmlWithProduct(DEFAULT_SAMPLE_PRODUCT_FILE_NAME);

        LOGGER.info("Source status: \n{}", get(REST_PATH + "sources").body());
    }

    @Before
    public void setup() {
        urlResourceReaderConfigurator = getUrlResourceReaderConfigurator();
    }

    @After
    public void tearDown() throws Exception {
        if (metacardsToDelete != null) {
            for (String metacardId : metacardsToDelete) {
                TestCatalog.deleteMetacard(metacardId);
            }
            metacardsToDelete.clear();
        }
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS});

        if (resourcesToDelete != null) {
            for (String resource : resourcesToDelete) {
                FileUtils.deleteQuietly(new File(resource));
            }

            resourcesToDelete.clear();
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
        String queryUrl = OPENSEARCH_PATH + "?q=*&format=xml&src=" + OPENSEARCH_SOURCE_ID;

        when().get(queryUrl).then().log().all().assertThat()
                .body(hasXPath("/metacards/metacard/string[@name='" + Metacard.TITLE
                                + "']/value[text()='" + RECORD_TITLE_1 + "']"),
                        hasXPath("/metacards/metacard/geometry/value"), hasXPath(
                                "/metacards/metacard/string[@name='" + Metacard.TITLE
                                        + "']/value[text()='" + RECORD_TITLE_2 + "']"),
                        hasXPath("/metacards/metacard/stringxml"));
    }

    /**
     * Given what was ingested in beforeTest(), tests that a Federated wildcard search will return
     * all appropriate record(s) in ATOM format.
     *
     * @throws Exception
     */
    @Test
    public void testAtomFederatedQueryByWildCardSearchPhrase() throws Exception {
        String queryUrl = OPENSEARCH_PATH + "?q=*&format=atom&src=" + OPENSEARCH_SOURCE_ID;

        when().get(queryUrl).then().log().all().assertThat()
                .body(hasXPath("/feed/entry/title[text()='" + RECORD_TITLE_1 + "']"),
                        hasXPath("/feed/entry/title[text()='" + RECORD_TITLE_2 + "']"),
                        hasXPath("/feed/entry/content/metacard/geometry/value"));
    }

    /**
     * Given what was ingested in beforeTest(), tests that a Federated search phrase will return the
     * appropriate record(s).
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryBySearchPhrase() throws Exception {
        String queryUrl = OPENSEARCH_PATH + "?q=" + DEFAULT_KEYWORD + "&format=xml&src="
                + OPENSEARCH_SOURCE_ID;

        when().get(queryUrl).then().log().all().assertThat().body(hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_1 + "']"), hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_2 + "']"));
    }

    /**
     * Tests Source can retrieve based on a pure spatial query
     *
     * @throws Exception
     */
    @Test
    public void testFederatedSpatial() throws Exception {
        String queryUrl =
                OPENSEARCH_PATH + "?lat=10.0&lon=30.0&radius=250000&spatialType=POINT_RADIUS"
                        + "&format=xml&src=" + OPENSEARCH_SOURCE_ID;
        when().get(queryUrl).then().log().all().assertThat().body(hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_1 + "']"), hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_2 + "']"));
    }

    /**
     * Tests given bad spatial query, no result should be returned
     *
     * @throws Exception
     */
    @Test
    public void testFederatedNegativeSpatial() throws Exception {
        String queryUrl = OPENSEARCH_PATH + "?lat=-10.0&lon=-30.0&radius=1&spatialType=POINT_RADIUS"
                + "&format=xml&src=" + OPENSEARCH_SOURCE_ID;
        when().get(queryUrl).then().log().all().assertThat()
                .body(not(containsString(RECORD_TITLE_1)), not(containsString(RECORD_TITLE_2)));
    }

    /**
     * Tests that given a bad test phrase, no records should have been returned.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryByNegativeSearchPhrase() throws Exception {
        String negativeSearchPhrase = "negative";
        String queryUrl = OPENSEARCH_PATH + "?q=" + negativeSearchPhrase + "&format=xml&src="
                + OPENSEARCH_SOURCE_ID;

        when().get(queryUrl).then().log().all().assertThat()
                .body(not(containsString(RECORD_TITLE_1)), not(containsString(RECORD_TITLE_2)));
    }

    /**
     * Tests that a federated search by ID will return the right record.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryById() throws Exception {
        String restUrl = REST_PATH + "sources/" + OPENSEARCH_SOURCE_ID + "/"
                + metacardIds[GEOJSON_RECORD_INDEX];

        when().get(restUrl).then().log().all().assertThat().body(hasXPath(
                "/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='" + RECORD_TITLE_1
                        + "']"), not(containsString(RECORD_TITLE_2)));
    }

    /**
     * Tests Source can retrieve existing product. The product is located in one of the
     * URLResourceReader's root resource directories, so it can be downloaded.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveExistingProduct() throws Exception {
        /**
         * Setup 
         * Add productDirectory to the URLResourceReader's set of valid root resource directories.
         */
        String fileName = testName.getMethodName() + ".txt";
        String metacardId = ingestXmlWithProduct(fileName);
        metacardsToDelete.add(metacardId);
        String productDirectory = new File(fileName).getAbsoluteFile().getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

        String restUrl = REST_PATH + "sources/" + OPENSEARCH_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Perform Test and Verify
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(SAMPLE_DATA));
    }

    /**
     * Tests Source CANNOT retrieve existing product. The product is NOT located in one of the
     * URLResourceReader's root resource directories, so it CANNOT be downloaded.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveProductInvalidResourceUrl() throws Exception {
        // Setup
        String fileName = testName.getMethodName() + ".txt";
        String metacardId = ingestXmlWithProduct(fileName);
        metacardsToDelete.add(metacardId);
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS});

        String restUrl = REST_PATH + "sources/" + OPENSEARCH_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Perform Test and Verify
        when().get(restUrl).then().log().all().assertThat().contentType("text/html")
                .statusCode(equalTo(500)).body(containsString("Unable to transform Metacard."));
    }

    /**
     * Tests Source CANNOT retrieve existing product. The product is NOT located in one of the
     * URLResourceReader's root resource directories, so it CANNOT be downloaded.
     * <p/>
     * For example:
     * The resource uri in the metacard is:
     * file:/Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-catalog/target/exam/e59b02bf-5774-489f-8aa9-53cf99c25d25/../../testFederatedRetrieveProductInvalidResourceUrlWithBackReferences.txt
     * which really means:
     * file:/Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-catalog/target/testFederatedRetrieveProductInvalidResourceUrlWithBackReferences.txt
     * <p/>
     * The URLResourceReader's root resource directories are:
     * <ddf.home>/data/products
     * and
     * /Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-catalog/target/exam/e59b02bf-5774-489f-8aa9-53cf99c25d25
     * <p/>
     * So the product (/Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-catalog/target/testFederatedRetrieveProductInvalidResourceUrlWithBackReferences.txt) is
     * not located under either of the URLResourceReader's root resource directories.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveProductInvalidResourceUrlWithBackReferences()
            throws Exception {
        // Setup
        String fileName = testName.getMethodName() + ".txt";
        String fileNameWithBackReferences =
                ".." + File.separator + ".." + File.separator + fileName;
        resourcesToDelete.add(fileNameWithBackReferences);
        // Add back references to file name
        String metacardId = ingestXmlWithProduct(fileNameWithBackReferences);
        metacardsToDelete.add(metacardId);
        String productDirectory = new File(fileName).getAbsoluteFile().getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

        String restUrl = REST_PATH + "sources/" + OPENSEARCH_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Perform Test and Verify
        when().get(restUrl).then().log().all().assertThat().contentType("text/html")
                .statusCode(equalTo(500)).body(containsString("Unable to transform Metacard."));
    }

    @Test
    public void testFederatedRetrieveExistingProductCsw() throws Exception {
        String productDirectory = new File(DEFAULT_SAMPLE_PRODUCT_FILE_NAME).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

        String restUrl =
                REST_PATH + "sources/" + CSW_SOURCE_ID + "/" + metacardIds[XML_RECORD_INDEX]
                        + "?transform=resource";

        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(SAMPLE_DATA));
    }

    /**
     * Tests Source can retrieve nonexistent product.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveNoProduct() throws Exception {
        // Setup 
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS});
        String restUrl = REST_PATH + "sources/" + OPENSEARCH_SOURCE_ID + "/"
                + metacardIds[GEOJSON_RECORD_INDEX] + "?transform=resource";

        // Perform Test and Verify
        when().get(restUrl).then().log().all().assertThat().statusCode(equalTo(500));
    }

    @Test
    public void testFederatedRetrieveNoProductCsw() throws Exception {
        File[] rootDirectories = File.listRoots();
        String rootDir = rootDirectories[0].getCanonicalPath();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(new String[] {rootDir});
        String restUrl =
                REST_PATH + "sources/" + CSW_SOURCE_ID + "/" + metacardIds[GEOJSON_RECORD_INDEX]
                        + "?transform=resource";
        when().get(restUrl).then().log().all().assertThat().statusCode(equalTo(500));
    }

    @Test
    public void testCswQueryByWildCardSearchPhrase() throws Exception {
        String wildcardQuery = Library.getCswQuery("AnyText", "*");

        given().contentType(ContentType.XML).body(wildcardQuery).when().post(CSW_PATH).then().log()
                .all().assertThat()
                .body(hasXPath("/GetRecordsResponse/SearchResults/Record/identifier[text()='" +
                                metacardIds[GEOJSON_RECORD_INDEX] + "']"),
                        hasXPath("/GetRecordsResponse/SearchResults/Record/identifier[text()='" +
                                metacardIds[XML_RECORD_INDEX] + "']"),
                        hasXPath("/GetRecordsResponse/SearchResults/@numberOfRecordsReturned",
                                is("2")),
                        hasXPath("/GetRecordsResponse/SearchResults/Record/relation",
                                containsString("/services/catalog/sources/")));
    }

    @Test
    public void testCswQueryByTitle() throws Exception {
        String titleQuery = Library.getCswQuery("title", "myTitle");

        given().contentType(ContentType.XML).body(titleQuery).when().post(CSW_PATH).then().log()
                .all().assertThat()
                .body(hasXPath("/GetRecordsResponse/SearchResults/Record/identifier",
                                is(metacardIds[GEOJSON_RECORD_INDEX])),
                        hasXPath("/GetRecordsResponse/SearchResults/@numberOfRecordsReturned",
                                is("1")));
    }

    @Test
    public void testCswQueryForMetacardXml() throws Exception {
        String titleQuery = Library.getCswQueryMetacardXml("title", "myTitle");

        given().contentType(ContentType.XML).body(titleQuery).when().post(CSW_PATH).then().log()
                .all().assertThat().body(hasXPath("/GetRecordsResponse/SearchResults/metacard/@id",
                        is(metacardIds[GEOJSON_RECORD_INDEX])),
                hasXPath("/GetRecordsResponse/SearchResults/@numberOfRecordsReturned", is("1")),
                hasXPath("/GetRecordsResponse/SearchResults/@recordSchema",
                        is("urn:catalog:metacard")));
    }

    @Test
    public void testCswQueryForJson() throws Exception {
        String titleQuery = Library.getCswQueryJson("title", "myTitle");

        given().headers("Accept", "application/json", "Content-Type", "application/xml")
                .body(titleQuery).when().post(CSW_PATH).then().log().all().assertThat()
                .contentType(ContentType.JSON)
                .body("results[0].metacard.properties.title", equalTo(RECORD_TITLE_1));
    }

    @Test
    public void testFanoutQueryAgainstUnknownSource() throws IOException, InterruptedException {
        setFanout(true);
        waitForAllBundles();

        String queryUrl = OPENSEARCH_PATH + "?q=" + DEFAULT_KEYWORD + "&src=" + CSW_SOURCE_ID;

        when().get(queryUrl).then().log().all().assertThat().body(containsString("Unknown source"));

        setFanout(false);
        waitForAllBundles();
    }

    @Test
    public void testFanoutQueryAgainstKnownSource() throws IOException, InterruptedException {

        setFanout(true);
        waitForAllBundles();

        String queryUrl = OPENSEARCH_PATH + "?q=" + DEFAULT_KEYWORD + "&src=" + localSourceID;

        when().get(queryUrl).then().log().all().assertThat().body(containsString(localSourceID));

        setFanout(false);
        waitForAllBundles();
    }

    @Test
    public void testOpensearchToCswSourceToCswEndpointQuerywithCswRecordXml() throws Exception {

        String queryUrl =
                OPENSEARCH_PATH + "?q=" + DEFAULT_KEYWORD + "&format=xml&src=" + CSW_SOURCE_ID;

        when().get(queryUrl).then().log().all().assertThat()
                .body(containsString(RECORD_TITLE_1), containsString(RECORD_TITLE_2), hasXPath(
                        "/metacards/metacard/string[@name='" + Metacard.RESOURCE_DOWNLOAD_URL
                                + "']",
                        containsString("/services/catalog/sources/" + CSW_SOURCE_ID)));
    }

    @Test
    public void testOpensearchToCswSourceToCswEndpointQuerywithMetacardXml() throws Exception {

        String queryUrl = OPENSEARCH_PATH + "?q=" + DEFAULT_KEYWORD + "&format=xml&src="
                + CSW_SOURCE_WITH_METACARD_XML_ID;

        when().get(queryUrl).then().log().all().assertThat()
                .body(containsString(RECORD_TITLE_1), containsString(RECORD_TITLE_2), hasXPath(
                        "/metacards/metacard/string[@name='" + Metacard.RESOURCE_DOWNLOAD_URL
                                + "']",
                        containsString("/services/catalog/sources/" + CSW_SOURCE_ID)));
    }

    @Test
    public void testListAllSourceInfo() {

        // TODO: Connected csw/wfs sources are broken. Ticket: DDF-1366
        /*
        try {
            setupConnectedSources();
        } catch (IOException e) {
            LOGGER.error("Couldn't create connected sources: {}", e.getMessage());
        }
        */

        given().auth().basic("admin", "admin").when().get(ADMIN_ALL_SOURCES_PATH).then().log().all()
                .assertThat().body(containsString("\"fpid\":\"OpenSearchSource\""),
                containsString("\"fpid\":\"Csw_Federated_Source\"")/*,
                containsString("\"fpid\":\"Csw_Connected_Source\"")*/);
    }

    @Test
    public void testFederatedSourceStatus() {
        // Find and test OpenSearch Federated Source
        String json = given().auth().basic("admin", "admin").when().get(ADMIN_ALL_SOURCES_PATH)
                .asString();

        List<Map<String, Object>> sources = with(json).param("name", "OpenSearchSource")
                .get("value.findAll { source -> source.id == name}");
        String openSearchPid = (String) ((ArrayList<Map<String, Object>>) (sources.get(0)
                .get("configurations"))).get(0).get("id");

        given().auth().basic("admin", "admin").when().get(ADMIN_STATUS_PATH + openSearchPid).then()
                .log().all().assertThat().body(containsString("\"value\":true"));
    }

    // TODO: Connected csw/wfs sources are broken. Ticket: DDF-1366
    @Ignore
    @Test
    public void testConnectedSourceStatus() {
        try {
            setupConnectedSources();
        } catch (IOException e) {
            LOGGER.error("Couldn't create connected sources: {}", e);
        }

        String json = given().auth().basic("admin", "admin").when().get(ADMIN_ALL_SOURCES_PATH)
                .asString();

        List<Map<String, Object>> sources = with(json).param("name", "Csw_Connected_Source")
                .get("value.findAll { source -> source.id == name}");
        String connectedSourcePid = (String) ((ArrayList<Map<String, Object>>) (sources.get(0)
                .get("configurations"))).get(0).get("id");

        // Test CSW Connected Source status
        given().auth().basic("admin", "admin").when().get(ADMIN_STATUS_PATH + connectedSourcePid)
                .then().log().all().assertThat().body(containsString("\"value\":true"));
    }

    public void setupConnectedSources() throws IOException {
        CswConnectedSourceProperties connectedSourceProperties = new CswConnectedSourceProperties(
                CONNECTED_SOURCE_ID);
        createManagedService(CswConnectedSourceProperties.FACTORY_PID, connectedSourceProperties);
    }

    private String ingestXmlWithProduct(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.createNewFile()) {
            fail("Unable to create " + fileName + " file.");
        }
        FileUtils.write(file, SAMPLE_DATA);
        String fileLocation = file.toURI().toURL().toString();
        LOGGER.debug("File Location: {}", fileLocation);
        String metacardId = TestCatalog.ingest(Library.getSimpleXml(fileLocation), "text/xml");
        return metacardId;
    }

}
