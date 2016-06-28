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
package ddf.test.itests.catalog;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.path.json.JsonPath.with;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.composite;
import static com.xebialabs.restito.semantics.Action.header;
import static com.xebialabs.restito.semantics.Action.success;
import static ddf.common.test.restito.ChunkedContent.chunkedContentWithHeaders;
import static ddf.test.itests.AbstractIntegrationTest.DynamicUrl.INSECURE_ROOT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.bundle.core.BundleService;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.InvalidSyntaxException;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.path.xml.XmlPath;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Call;
import com.xebialabs.restito.semantics.Condition;
import com.xebialabs.restito.server.StubServer;
import com.xebialabs.restito.server.secure.SecureStubServer;

import ddf.catalog.data.Metacard;
import ddf.catalog.endpoint.CatalogEndpoint;
import ddf.catalog.endpoint.impl.CatalogEndpointImpl;
import ddf.common.test.BeforeExam;
import ddf.common.test.mock.csw.FederatedCswMockServer;
import ddf.test.itests.AbstractIntegrationTest;
import ddf.test.itests.common.CswQueryBuilder;
import ddf.test.itests.common.Library;
import ddf.test.itests.common.UrlResourceReaderConfigurator;

/**
 * Tests Federation aspects.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestFederation extends AbstractIntegrationTest {

    private static final XLogger LOGGER =
            new XLogger(LoggerFactory.getLogger(TestFederation.class));

    private static final String SAMPLE_DATA = "sample data";

    private static final String SUBSCRIBER = "/subscriber";

    private static final int EVENT_UPDATE_WAIT_INTERVAL = 200;

    private static boolean fatalError = false;

    private static final int XML_RECORD_INDEX = 1;

    private static final int GEOJSON_RECORD_INDEX = 0;

    private static final String DEFAULT_KEYWORD = "text";

    private static final String RECORD_TITLE_1 = "myTitle";

    private static final String RECORD_TITLE_2 = "myXmlTitle";

    private static final String CONNECTED_SOURCE_ID = "cswConnectedSource";

    private static final String CSW_STUB_SOURCE_ID = "cswStubServer";

    private static final String CSW_SOURCE_WITH_METACARD_XML_ID = "cswSource2";

    private static final String GMD_SOURCE_ID = "gmdSource";

    private static final String DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS = "data/products";

    private static final String DEFAULT_SAMPLE_PRODUCT_FILE_NAME = "sample.txt";

    private static final String CSW_STUB_RESOURCE_ID = "4cdb5d6bc8c1428f874f28a944d18b84";

    private static final String STUB_SERVER_TEST_STRING = "testData";

    private static final DynamicPort RESTITO_STUB_SERVER_PORT = new DynamicPort(6);

    private static final Path PRODUCT_CACHE = Paths.get("data", "Product_Cache");

    private static final DynamicPort CSW_STUB_SERVER_PORT = new DynamicPort(7);

    public static final DynamicUrl CSW_STUB_SERVER_PATH = new DynamicUrl(INSECURE_ROOT,
            CSW_STUB_SERVER_PORT,
            "/services/csw");

    private static String[] metacardIds = new String[2];

    private List<String> metacardsToDelete = new ArrayList<>();

    private List<String> resourcesToDelete = new ArrayList<>();

    private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

    public static final DynamicUrl RESTITO_STUB_SERVER = new DynamicUrl("https://localhost:",
            RESTITO_STUB_SERVER_PORT, SUBSCRIBER);

    private static StubServer server;

    private static FederatedCswMockServer cswServer;

    @Rule
    public TestName testName = new TestName();

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query?_wadl");

            OpenSearchSourceProperties openSearchProperties = new OpenSearchSourceProperties(
                    OPENSEARCH_SOURCE_ID);
            getServiceManager().createManagedService(OpenSearchSourceProperties.FACTORY_PID,
                    openSearchProperties);

            cswServer = new FederatedCswMockServer(CSW_STUB_SOURCE_ID,
                    INSECURE_ROOT,
                    Integer.parseInt(CSW_STUB_SERVER_PORT.getPort()));
            cswServer.start();

            CswSourceProperties cswStubServerProperties =
                    new CswSourceProperties(CSW_STUB_SOURCE_ID);
            cswStubServerProperties.put("cswUrl", CSW_STUB_SERVER_PATH.getUrl());
            getServiceManager().createManagedService(CswSourceProperties.FACTORY_PID,
                    cswStubServerProperties);

            getServiceManager().waitForHttpEndpoint(CSW_PATH + "?_wadl");
            get(CSW_PATH + "?_wadl").prettyPrint();
            CswSourceProperties cswProperties = new CswSourceProperties(CSW_SOURCE_ID);

            getServiceManager().createManagedService(CswSourceProperties.FACTORY_PID,
                    cswProperties);

            CswSourceProperties cswProperties2 = new CswSourceProperties(
                    CSW_SOURCE_WITH_METACARD_XML_ID);
            cswProperties2.put("outputSchema", "urn:catalog:metacard");
            getServiceManager().createManagedService(CswSourceProperties.FACTORY_PID,
                    cswProperties2);

            CswSourceProperties gmdProperties = new CswSourceProperties(GMD_SOURCE_ID,
                    CswSourceProperties.GMD_FACTORY_PID);
            getServiceManager().createManagedService(CswSourceProperties.GMD_FACTORY_PID,
                    gmdProperties);

            getCatalogBundle().waitForFederatedSource(OPENSEARCH_SOURCE_ID);
            getCatalogBundle().waitForFederatedSource(CSW_STUB_SOURCE_ID);
            getCatalogBundle().waitForFederatedSource(CSW_SOURCE_ID);
            getCatalogBundle().waitForFederatedSource(CSW_SOURCE_WITH_METACARD_XML_ID);
            getCatalogBundle().waitForFederatedSource(GMD_SOURCE_ID);

            getServiceManager().waitForSourcesToBeAvailable(REST_PATH.getUrl(),
                    OPENSEARCH_SOURCE_ID,
                    CSW_STUB_SOURCE_ID,
                    CSW_SOURCE_ID,
                    CSW_SOURCE_WITH_METACARD_XML_ID,
                    GMD_SOURCE_ID);

            metacardIds[GEOJSON_RECORD_INDEX] = TestCatalog.ingest(Library.getSimpleGeoJson(),
                    "application/json");

            metacardIds[XML_RECORD_INDEX] = ingestXmlWithProduct(DEFAULT_SAMPLE_PRODUCT_FILE_NAME);

            LOGGER.info("Source status: \n{}", get(REST_PATH.getUrl() + "sources").body()
                    .prettyPrint());
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @Before
    public void setup() throws Exception {
        cleanProductCache();
        getCatalogBundle().setupCaching(true);
        urlResourceReaderConfigurator = getUrlResourceReaderConfigurator();

        if (fatalError) {
            server.stop();

            fail("An unrecoverable error occurred from previous test");
        }

        server = new SecureStubServer(Integer.parseInt(RESTITO_STUB_SERVER_PORT.getPort())).run();
        server.start();

        cswServer.reset();
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
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS);

        if (resourcesToDelete != null) {
            for (String resource : resourcesToDelete) {
                FileUtils.deleteQuietly(new File(resource));
            }

            resourcesToDelete.clear();
        }

        cswServer.stop();

        if (server != null) {
            server.stop();
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
        String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=*&format=xml&src=" + OPENSEARCH_SOURCE_ID;

        // @formatter:off
        when().get(queryUrl).then().log().all().assertThat().body(hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_1 + "']"), hasXPath("/metacards/metacard/geometry/value"),
                hasXPath("/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_2 + "']"), hasXPath("/metacards/metacard/stringxml"));
        // @formatter:on
    }

    /**
     * Given what was ingested in beforeTest(), tests that a Federated wildcard search will return
     * all appropriate record(s) in ATOM format.
     *
     * @throws Exception
     */
    @Test
    public void testAtomFederatedQueryByWildCardSearchPhrase() throws Exception {
        String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=*&format=atom&src=" + OPENSEARCH_SOURCE_ID;

        // @formatter:off
        when().get(queryUrl).then().log().all().assertThat()
                .body(hasXPath("/feed/entry/title[text()='" + RECORD_TITLE_1 + "']"),
                        hasXPath("/feed/entry/title[text()='" + RECORD_TITLE_2 + "']"),
                        hasXPath("/feed/entry/content/metacard/geometry/value"));
        // @formatter:on
    }

    /**
     * Given what was ingested in beforeTest(), tests that a Federated search phrase will return the
     * appropriate record(s).
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryBySearchPhrase() throws Exception {
        String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=" + DEFAULT_KEYWORD + "&format=xml&src="
                + OPENSEARCH_SOURCE_ID;

        // @formatter:off
        when().get(queryUrl).then().log().all().assertThat().body(hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_1 + "']"), hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_2 + "']"));
        // @formatter:on
    }

    /**
     * Tests Source can retrieve based on a pure spatial query
     *
     * @throws Exception
     */
    @Test
    public void testFederatedSpatial() throws Exception {
        String queryUrl = OPENSEARCH_PATH.getUrl()
                + "?lat=10.0&lon=30.0&radius=250000&spatialType=POINT_RADIUS" + "&format=xml&src="
                + OPENSEARCH_SOURCE_ID;

        // @formatter:off
        when().get(queryUrl).then().log().all().assertThat().body(hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_1 + "']"), hasXPath(
                "/metacards/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='"
                        + RECORD_TITLE_2 + "']"));
        // @formatter:on
    }

    /**
     * Tests given bad spatial query, no result should be returned
     *
     * @throws Exception
     */
    @Test
    public void testFederatedNegativeSpatial() throws Exception {
        String queryUrl =
                OPENSEARCH_PATH.getUrl() + "?lat=-10.0&lon=-30.0&radius=1&spatialType=POINT_RADIUS"
                        + "&format=xml&src=" + OPENSEARCH_SOURCE_ID;

        // @formatter:off
        when().get(queryUrl).then().log().all().assertThat()
                .body(not(containsString(RECORD_TITLE_1)), not(containsString(RECORD_TITLE_2)));
        // @formatter:on
    }

    /**
     * Tests that given a bad test phrase, no records should have been returned.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryByNegativeSearchPhrase() throws Exception {
        String negativeSearchPhrase = "negative";
        String queryUrl =
                OPENSEARCH_PATH.getUrl() + "?q=" + negativeSearchPhrase + "&format=xml&src="
                        + OPENSEARCH_SOURCE_ID;

        // @formatter:off
        when().get(queryUrl).then().log().all().assertThat()
                .body(not(containsString(RECORD_TITLE_1)), not(containsString(RECORD_TITLE_2)));
        // @formatter:on
    }

    /**
     * Tests that a federated search by ID will return the right record.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryById() throws Exception {
        String restUrl = REST_PATH.getUrl() + "sources/" + OPENSEARCH_SOURCE_ID + "/"
                + metacardIds[GEOJSON_RECORD_INDEX];

        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().body(hasXPath(
                "/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='" + RECORD_TITLE_1
                        + "']"), not(containsString(RECORD_TITLE_2)));
        // @formatter:on
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
        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS,
                productDirectory);

        String restUrl = REST_PATH.getUrl() + "sources/" + OPENSEARCH_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Perform Test and Verify
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(SAMPLE_DATA));
        // @formatter:on
    }

    /**
     * Tests Source can retrieve existing product. The product is located in one of the
     * URLResourceReader's root resource directories, so it can be downloaded.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveExistingProductWithRange() throws Exception {
        /**
         * Setup
         * Add productDirectory to the URLResourceReader's set of valid root resource directories.
         */
        String fileName = testName.getMethodName() + ".txt";
        String metacardId = ingestXmlWithProduct(fileName);
        metacardsToDelete.add(metacardId);
        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS,
                productDirectory);

        int offset = 4;
        byte[] sampleDataByteArray = SAMPLE_DATA.getBytes();
        String partialSampleData = new String(Arrays.copyOfRange(sampleDataByteArray,
                offset,
                sampleDataByteArray.length));

        String restUrl = REST_PATH.getUrl() + "sources/" + OPENSEARCH_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Perform Test and Verify
        // @formatter:off
        given().header(CswConstants.RANGE_HEADER, String.format("bytes=%s-", offset)).get(restUrl)
                .then().log().all().assertThat().contentType("text/plain")
                .body(is(partialSampleData));
        // @formatter:on
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
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS);

        String restUrl = REST_PATH.getUrl() + "sources/" + OPENSEARCH_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Perform Test and Verify
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/html")
                .statusCode(equalTo(500)).body(containsString("Unable to transform Metacard."));
        // @formatter:on
    }

    /**
     * Tests Source CANNOT retrieve existing product. The product is NOT located in one of the
     * URLResourceReader's root resource directories, so it CANNOT be downloaded.
     * <p/>
     * For example:
     * The resource uri in the metacard is:
     * file:/Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-ddf/target/exam/e59b02bf-5774-489f-8aa9-53cf99c25d25/../../testFederatedRetrieveProductInvalidResourceUrlWithBackReferences.txt
     * which really means:
     * file:/Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-ddf/target/testFederatedRetrieveProductInvalidResourceUrlWithBackReferences.txt
     * <p/>
     * The URLResourceReader's root resource directories are:
     * <ddf.home>/data/products
     * and
     * /Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-ddf/target/exam/e59b02bf-5774-489f-8aa9-53cf99c25d25
     * <p/>
     * So the product (/Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-ddf/target/testFederatedRetrieveProductInvalidResourceUrlWithBackReferences.txt) is
     * not located under either of the URLResourceReader's root resource directories.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedRetrieveProductInvalidResourceUrlWithBackReferences()
            throws Exception {
        // Setup
        String fileName = testName.getMethodName() + HTTPS_PORT.getPort() + ".txt";
        String fileNameWithBackReferences =
                ".." + File.separator + ".." + File.separator + fileName;
        resourcesToDelete.add(fileNameWithBackReferences);
        // Add back references to file name
        String metacardId = ingestXmlWithProduct(fileNameWithBackReferences);
        metacardsToDelete.add(metacardId);
        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS,
                productDirectory);

        String restUrl = REST_PATH.getUrl() + "sources/" + OPENSEARCH_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Perform Test and Verify
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/html")
                .statusCode(equalTo(500)).body(containsString("Unable to transform Metacard."));
        // @formatter:on
    }

    @Test
    public void testFederatedRetrieveExistingProductCsw() throws Exception {
        String productDirectory = new File(DEFAULT_SAMPLE_PRODUCT_FILE_NAME).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS,
                productDirectory);

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_SOURCE_ID + "/"
                + metacardIds[XML_RECORD_INDEX] + "?transform=resource";

        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(SAMPLE_DATA));
        // @formatter:on
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
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS);
        String restUrl = REST_PATH.getUrl() + "sources/" + OPENSEARCH_SOURCE_ID + "/"
                + metacardIds[GEOJSON_RECORD_INDEX] + "?transform=resource";

        // Perform Test and Verify
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().statusCode(equalTo(500));
        // @formatter:on
    }

    @Test
    public void testFederatedRetrieveNoProductCsw() throws Exception {
        File[] rootDirectories = File.listRoots();
        String rootDir = rootDirectories[0].getCanonicalPath();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(rootDir);
        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_SOURCE_ID + "/"
                + metacardIds[GEOJSON_RECORD_INDEX] + "?transform=resource";

        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().statusCode(equalTo(500));
        // @formatter:on
    }

    @Test
    public void testCswQueryByWildCardSearchPhrase() throws Exception {
        String wildcardQuery = Library.getCswQuery("AnyText", "*");

        // @formatter:off
        given().contentType(ContentType.XML).body(wildcardQuery).when().post(CSW_PATH.getUrl())
                .then().log().all().assertThat()
                .body(hasXPath("/GetRecordsResponse/SearchResults/Record/identifier[text()='" +
                                metacardIds[GEOJSON_RECORD_INDEX] + "']"),
                        hasXPath("/GetRecordsResponse/SearchResults/Record/identifier[text()='" +
                                metacardIds[XML_RECORD_INDEX] + "']"),
                        hasXPath("/GetRecordsResponse/SearchResults/@numberOfRecordsReturned",
                                is("2")),
                        hasXPath("/GetRecordsResponse/SearchResults/Record/relation",
                                containsString("/services/catalog/sources/")));
        // @formatter:on
    }

    @Test
    public void testCswQueryWithValidationCheckerPlugin() throws Exception {

        // Construct a query to search for all metacards
        String query = new CswQueryBuilder().addAttributeFilter(CswQueryBuilder.PROPERTY_IS_LIKE,
                "AnyText",
                "*")
                .getQuery();

        // Declare array of matchers so we can be sure we use the same matchers in each assertion
        Matcher[] assertion = new Matcher[] {hasXPath(
                "/GetRecordsResponse/SearchResults/Record/identifier[text()='" +
                        metacardIds[GEOJSON_RECORD_INDEX] + "']"), hasXPath(
                "/GetRecordsResponse/SearchResults/Record/identifier[text()='" +
                        metacardIds[XML_RECORD_INDEX] + "']"), hasXPath(
                "/GetRecordsResponse/SearchResults/@numberOfRecordsReturned",
                is("2")), hasXPath("/GetRecordsResponse/SearchResults/Record/relation",
                containsString("/services/catalog/sources/"))};

        // Run a normal federated query to the CSW source and assert response
        // @formatter:off
        given().contentType(ContentType.XML).body(query).when().post(CSW_PATH.getUrl()).then().log()
                .all().assertThat().body(assertion[0], assertion);
        // @formatter:on

        // Start metacard validation plugin; this will add on [validation-warnings = null] AND [validation-errors = null]
        // filter to query
        getServiceManager().startFeature(true, "catalog-plugin-metacard-validation");

        // Assert that response is the same as without the plugin
        // @formatter:off
        given().contentType(ContentType.XML).body(query).when().post(CSW_PATH.getUrl()).then().log()
                .all().assertThat().body(assertion[0], assertion);
        // @formatter:on

        // Turn off plugin to not interfere with other tests
        getServiceManager().stopFeature(true, "catalog-plugin-metacard-validation");
    }

    @Test
    public void testCswQueryByTitle() throws Exception {
        String titleQuery = Library.getCswQuery("title", "myTitle");

        // @formatter:off
        given().contentType(ContentType.XML).body(titleQuery).when().post(CSW_PATH.getUrl()).then()
                .log().all().assertThat()
                .body(hasXPath("/GetRecordsResponse/SearchResults/Record/identifier",
                        is(metacardIds[GEOJSON_RECORD_INDEX])),
                        hasXPath("/GetRecordsResponse/SearchResults/@numberOfRecordsReturned",
                                is("1")));
        // @formatter:on
    }

    @Test
    public void testCswQueryForMetacardXml() throws Exception {
        String titleQuery = Library.getCswQueryMetacardXml("title", "myTitle");

        // @formatter:off
        given().contentType(ContentType.XML).body(titleQuery).when().post(CSW_PATH.getUrl()).then()
                .log().all().assertThat()
                .body(hasXPath("/GetRecordsResponse/SearchResults/metacard/@id",
                        is(metacardIds[GEOJSON_RECORD_INDEX])),
                        hasXPath("/GetRecordsResponse/SearchResults/@numberOfRecordsReturned",
                                is("1")),
                        hasXPath("/GetRecordsResponse/SearchResults/@recordSchema",
                                is("urn:catalog:metacard")));
        // @formatter:on
    }

    @Test
    public void testCswQueryForJson() throws Exception {
        String titleQuery = Library.getCswQueryJson("title", "myTitle");

        // @formatter:off
        given().headers("Accept", "application/json", "Content-Type", "application/xml")
                .body(titleQuery).when().post(CSW_PATH.getUrl()).then().log().all().assertThat()
                .contentType(ContentType.JSON)
                .body("results[0].metacard.properties.title", equalTo(RECORD_TITLE_1));
        // @formatter:on
    }

    @Test
    public void testOpensearchToCswSourceToCswEndpointQuerywithCswRecordXml() throws Exception {

        String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=" + DEFAULT_KEYWORD + "&format=xml&src="
                + CSW_SOURCE_ID;

        // @formatter:off
        when().get(queryUrl).then().log().all().assertThat()
                .body(containsString(RECORD_TITLE_1), containsString(RECORD_TITLE_2), hasXPath(
                        "/metacards/metacard/string[@name='" + Metacard.RESOURCE_DOWNLOAD_URL
                                + "']",
                        containsString("/services/catalog/sources/" + CSW_SOURCE_ID)));
        // @formatter:on
    }

    @Test
    public void testOpensearchToCswSourceToCswEndpointQuerywithMetacardXml() throws Exception {

        String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=" + DEFAULT_KEYWORD + "&format=xml&src="
                + CSW_SOURCE_WITH_METACARD_XML_ID;

        // @formatter:off
        when().get(queryUrl).then().log().all().assertThat()
                .body(containsString(RECORD_TITLE_1), containsString(RECORD_TITLE_2), hasXPath(
                        "/metacards/metacard/string[@name='" + Metacard.RESOURCE_DOWNLOAD_URL
                                + "']",
                        containsString("/services/catalog/sources/" + CSW_SOURCE_ID)));
        // @formatter:on
    }

    @Test
    public void testOpensearchToGmdSourceToGmdEndpointQuery() throws Exception {

        String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=" + RECORD_TITLE_1 + "&format=xml&src="
                + GMD_SOURCE_ID;

        when().get(queryUrl)
                .then()
                .log()
                .all()
                .assertThat()
                .body(containsString(RECORD_TITLE_1),
                        hasXPath(
                                "/metacards/metacard/stringxml/value/MD_Metadata/fileIdentifier/CharacterString",
                                is(metacardIds[GEOJSON_RECORD_INDEX])));
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

        // @formatter:off
        given().auth().basic("admin", "admin").when().get(ADMIN_ALL_SOURCES_PATH.getUrl()).then()
                .log().all().assertThat().body(containsString("\"fpid\":\"OpenSearchSource\""),
                containsString("\"fpid\":\"Csw_Federated_Source\"")/*,
                containsString("\"fpid\":\"Csw_Connected_Source\"")*/);
        // @formatter:on
    }

    @Test
    public void testFederatedSourceStatus() {
        // Find and test OpenSearch Federated Source
        // @formatter:off
        String json = given().auth().basic("admin", "admin").when()
                .get(ADMIN_ALL_SOURCES_PATH.getUrl()).asString();
        // @formatter:on

        List<Map<String, Object>> sources = with(json).param("name", "OpenSearchSource")
                .get("value.findAll { source -> source.id == name}");
        String openSearchPid = (String) ((ArrayList<Map<String, Object>>) (sources.get(0)
                .get("configurations"))).get(0)
                .get("id");

        // @formatter:off
        given().auth().basic("admin", "admin").when()
                .get(ADMIN_STATUS_PATH.getUrl() + openSearchPid).then().log().all().assertThat()
                .body(containsString("\"value\":true"));
        // @formatter:on
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

        // @formatter:off
        String json = given().auth().basic("admin", "admin").when()
                .get(ADMIN_ALL_SOURCES_PATH.getUrl()).asString();
        // @formatter:on

        List<Map<String, Object>> sources = with(json).param("name", "Csw_Connected_Source")
                .get("value.findAll { source -> source.id == name}");
        String connectedSourcePid = (String) ((ArrayList<Map<String, Object>>) (sources.get(0)
                .get("configurations"))).get(0)
                .get("id");

        // Test CSW Connected Source status
        // @formatter:off
        given().auth().basic("admin", "admin").when()
                .get(ADMIN_STATUS_PATH.getUrl() + connectedSourcePid).then().log().all()
                .assertThat().body(containsString("\"value\":true"));
        // @formatter:on
    }

    @Test
    public void testCatalogEndpointExposure() throws InvalidSyntaxException {
        // Check the service references
        ArrayList<String> expectedEndpoints = new ArrayList<>();
        expectedEndpoints.add("openSearchUrl");
        expectedEndpoints.add("cswUrl");

        CatalogEndpoint endpoint = getServiceManager().getService(CatalogEndpoint.class);
        String urlBindingName = endpoint.getEndpointProperties()
                .get(CatalogEndpointImpl.URL_BINDING_NAME_KEY);

        assertTrue("Catalog endpoint url binding name: '" + urlBindingName + "' is expected.",
                expectedEndpoints.contains(urlBindingName));
    }

    @Test
    public void testCswSubscriptionByWildCardSearchPhrase() throws Exception {
        whenHttp(server).match(Condition.post(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.get(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.delete(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.put(SUBSCRIBER))
                .then(success());

        String wildcardQuery = Library.getCswSubscription("xml", "*", RESTITO_STUB_SERVER.getUrl());

        // @formatter:off
        String subscriptionId = given().contentType(ContentType.XML).body(wildcardQuery).when().post(CSW_SUBSCRIPTION_PATH.getUrl())
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract().body().xmlPath().get("Acknowledgement.RequestId").toString();

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract().body().xmlPath().get("Acknowledgement.RequestId").toString();
        // @formatter:on

        String metacardId = TestCatalog.ingest(Library.getSimpleGeoJson(), "application/json");

        metacardsToDelete.add(metacardId);

        String[] subscrptionIds = {subscriptionId};

        verifyEvents(new HashSet(Arrays.asList(metacardId)),
                new HashSet(0),
                new HashSet(Arrays.asList(subscrptionIds)));

        // @formatter:off
        given().contentType(ContentType.XML).when().delete(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract().body().xmlPath().get("Acknowledgement.RequestId").toString();

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat().statusCode(404);
        // @formatter:on

    }

    @Test
    public void testCswDurableSubscription() throws Exception {
        whenHttp(server).match(Condition.post(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.get(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.delete(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.put(SUBSCRIBER))
                .then(success());

        String wildcardQuery = Library.getCswSubscription("xml", "*", RESTITO_STUB_SERVER.getUrl());

        //CswSubscribe
        // @formatter:off
        String subscriptionId = given().contentType(ContentType.XML).body(wildcardQuery).when().post(CSW_SUBSCRIPTION_PATH.getUrl())
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract().body().xmlPath().get("Acknowledgement.RequestId").toString();
        // @formatter:on

        BundleService bundleService = getServiceManager().getService(BundleService.class);
        Bundle bundle = bundleService.getBundle("spatial-csw-endpoint");
        bundle.stop();
        while (bundle.getState() != Bundle.RESOLVED) {
            sleep(1000);
        }
        bundle.start();
        while (bundle.getState() != Bundle.ACTIVE) {
            sleep(1000);
        }
        getServiceManager().waitForHttpEndpoint(CSW_SUBSCRIPTION_PATH + "?_wadl");
        //get subscription
        // @formatter:off
        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract().body().xmlPath().get("Acknowledgement.RequestId").toString();
        // @formatter:on

        String metacardId = TestCatalog.ingest(Library.getSimpleGeoJson(), "application/json");

        metacardsToDelete.add(metacardId);

        String[] subscrptionIds = {subscriptionId};

        verifyEvents(new HashSet(Arrays.asList(metacardId)),
                new HashSet(0),
                new HashSet(Arrays.asList(subscrptionIds)));

        // @formatter:off
        given().contentType(ContentType.XML).when().delete(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract().body().xmlPath().get("Acknowledgement.RequestId").toString();

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat().statusCode(404);
        // @formatter:on

    }

    @Test
    public void testCswCreateEventEndpoint() throws Exception {
        whenHttp(server).match(Condition.post(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.get(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.delete(SUBSCRIBER))
                .then(success());
        whenHttp(server).match(Condition.put(SUBSCRIBER))
                .then(success());

        String wildcardQuery = Library.getCswSubscription("xml", "*", RESTITO_STUB_SERVER.getUrl());

        String metacardId = "5b1688fa85fd46268e4ab7402a1750e0";
        String event = Library.getCswRecordResponse();

        // @formatter:off
        String subscriptionId = given().contentType(ContentType.XML).body(wildcardQuery).when().post(CSW_SUBSCRIPTION_PATH.getUrl())
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract()
                .body()
                .xmlPath()
                .get("Acknowledgement.RequestId")
                .toString();

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract()
                .body()
                .xmlPath()
                .get("Acknowledgement.RequestId")
                .toString();

        given().contentType(ContentType.XML).body(event).when().post(CSW_EVENT_PATH.getUrl())
                .then().assertThat()
                .statusCode(200);
        // @formatter:on

        String[] subscrptionIds = {subscriptionId};

        verifyEvents(new HashSet(Arrays.asList(metacardId)),
                new HashSet(0),
                new HashSet(Arrays.asList(subscrptionIds)));

        // @formatter:off
        given().contentType(ContentType.XML).when().delete(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract()
                .body()
                .xmlPath()
                .get("Acknowledgement.RequestId")
                .toString();

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl()+"/"+subscriptionId)
                .then().log().all().assertThat().statusCode(404);
        // @formatter:on

    }

    /**
     * Tests basic download from the live federated csw source
     *
     * @throws Exception
     */
    @Test
    public void testDownloadFromFederatedCswSource() throws Exception {
        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"))
                .then(getCswRetrievalHeaders(),
                        chunkedContentWithHeaders(STUB_SERVER_TEST_STRING,
                                Duration.ofMillis(0),
                                0));

        String restUrl =
                REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + CSW_STUB_RESOURCE_ID
                        + "?transform=resource";

        // Verify that the testData from the csw stub server is returned.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(STUB_SERVER_TEST_STRING));
        // @formatter:on
    }

    /**
     * Tests that if the endpoint disconnects twice, the retrieval retries both times
     *
     * @throws Exception
     */
    @Test
    public void testRetrievalReliablility() throws Exception {
        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"))
                .then(getCswRetrievalHeaders(),
                        chunkedContentWithHeaders(STUB_SERVER_TEST_STRING,
                                Duration.ofMillis(200),
                                2));

        String restUrl =
                REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + CSW_STUB_RESOURCE_ID
                        + "?transform=resource";

        // Verify that the testData from the csw stub server is returned.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(STUB_SERVER_TEST_STRING));
        // @formatter:on
        cswServer.verifyHttp()
                .times(3,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"));
    }

    /**
     * Tests that if the endpoint disconnects 3 times, the retrieval fails after 3 attempts
     *
     * @throws Exception
     */
    @Test
    public void testRetrievalReliabilityFails() throws Exception {
        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"))
                .then(getCswRetrievalHeaders(),
                        chunkedContentWithHeaders(STUB_SERVER_TEST_STRING,
                                Duration.ofMillis(200),
                                3));

        String restUrl =
                REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + CSW_STUB_RESOURCE_ID
                        + "?transform=resource";

        // Verify that product retrieval fails from the csw stub server.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().statusCode(500).contentType("text/plain")
                .body(containsString("cannot retrieve product"));
        // @formatter:on
    }

    /**
     * Tests that ddf will return the cached copy if there are no changes to the remote metacard
     *
     * @throws Exception
     */
    @Test
    public void testDownloadFromCacheIfAvailable() throws Exception {
        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"))
                .then(getCswRetrievalHeaders(),
                        chunkedContentWithHeaders(STUB_SERVER_TEST_STRING,
                                Duration.ofMillis(0),
                                0));

        String restUrl =
                REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + CSW_STUB_RESOURCE_ID
                        + "?transform=resource";

        // Download product twice, should only call the stub server to download once
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(STUB_SERVER_TEST_STRING));
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(STUB_SERVER_TEST_STRING));
        // @formatter:on
        cswServer.verifyHttp()
                .times(1,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"));
    }

    /**
     * Tests that ddf will redownload a product if the remote metacard has changed
     *
     * @throws Exception
     */
    @Test
    public void testCacheIsUpdatedIfRemoteProductChanges() throws Exception {
        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"))
                .then(getCswRetrievalHeaders(),
                        chunkedContentWithHeaders(STUB_SERVER_TEST_STRING,
                                Duration.ofMillis(0),
                                0));

        String restUrl =
                REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + CSW_STUB_RESOURCE_ID
                        + "?transform=resource";

        // Download product twice, and change metacard on stub server between calls.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(STUB_SERVER_TEST_STRING));
        cswServer.setQueryResponse("csw-query-response-modified.xml");
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(STUB_SERVER_TEST_STRING));
        // @formatter:on
        cswServer.verifyHttp()
                .times(2,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"));
    }

    @Ignore
    public void testFederatedDownloadProductToCacheOnlyCacheEnabled() throws Exception {
        /**
         * Setup Add productDirectory to the URLResourceReader's set of valid root resource
         * directories.
         */
        String fileName = testName.getMethodName() + ".txt";
        String metacardId = ingestXmlWithProduct(fileName);
        metacardsToDelete.add(metacardId);
        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS,
                productDirectory);

        getCatalogBundle().setupCaching(true);

        String resourceDownloadEndpoint =
                RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl() + CSW_SOURCE_ID + "/" + metacardId;

        // Perform Test and Verify
        // @formatter:off
        when().get(resourceDownloadEndpoint).then().log().all().assertThat().contentType("text/plain")
                .body(is(String.format("The product associated with metacard [%s] from source [%s] is being downloaded to the product cache.", metacardId, CSW_SOURCE_ID)));
        // @formatter:on

        assertThat(Files.exists(Paths.get(ddfHome)
                .resolve(PRODUCT_CACHE)
                .resolve(CSW_SOURCE_ID + "-" + metacardId)), is(true));
        assertThat(Files.exists(Paths.get(ddfHome)
                .resolve(PRODUCT_CACHE)
                .resolve(CSW_SOURCE_ID + "-" + metacardId + ".ser")), is(true));
    }

    @Test
    public void testFederatedDownloadProductToCacheOnlyCacheDisabled() throws Exception {
        /**
         * Setup Add productDirectory to the URLResourceReader's set of valid root resource
         * directories.
         */
        String fileName = testName.getMethodName() + ".txt";
        String metacardId = ingestXmlWithProduct(fileName);
        metacardsToDelete.add(metacardId);
        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS,
                productDirectory);

        getCatalogBundle().setupCaching(false);

        String resourceDownloadEndpoint =
                RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl() + CSW_SOURCE_ID + "/" + metacardId;

        // Perform Test and Verify
        // @formatter:off
        when().get(resourceDownloadEndpoint).then().log().all().assertThat().contentType("text/plain")
                .body(is("Caching of products is not enabled."));
        // @formatter:on

        assertThat(Files.exists(Paths.get(ddfHome)
                .resolve(PRODUCT_CACHE)
                .resolve(CSW_SOURCE_ID + "-" + metacardId)), is(false));
        assertThat(Files.exists(Paths.get(ddfHome)
                .resolve(PRODUCT_CACHE)
                .resolve(CSW_SOURCE_ID + "-" + metacardId + ".ser")), is(false));
    }

    private void verifyEvents(Set<String> metacardIdsExpected, Set<String> metacardIdsNotExpected,
            Set<String> subscriptionIds) {
        long millis = 0;

        boolean isAllEventsReceived = false;
        boolean isUnexpectedEventReceived = false;

        while (!isAllEventsReceived && !isUnexpectedEventReceived
                && millis < TimeUnit.MINUTES.toMillis(2)) {

            Set<String> foundIds = null;

            try {
                sleep(EVENT_UPDATE_WAIT_INTERVAL);
                millis += EVENT_UPDATE_WAIT_INTERVAL;
            } catch (InterruptedException e) {
                LOGGER.info("Interrupted exception while trying to sleep for events", e);
            }
            if ((millis % 1000) == 0) {
                LOGGER.info("Waiting for events to be received...{}ms", millis);
            }
            for (String id : subscriptionIds) {
                foundIds = getEvents(id);
                isAllEventsReceived = foundIds.containsAll(metacardIdsExpected);

                isUnexpectedEventReceived = foundIds.removeAll(metacardIdsNotExpected);
            }
        }
        assertTrue(isAllEventsReceived);
        assertFalse(isUnexpectedEventReceived);
    }

    private Set<String> getEvents(String subscriptionId) {

        HashSet<String> foundIds = new HashSet<String>();
        List<Call> calls = new ArrayList<>(server.getCalls());

        if (CollectionUtils.isNotEmpty(calls)) {
            for (Call call : calls) {

                if (call.getMethod()
                        .matchesMethod(Method.POST.name())
                        && StringUtils.isNotEmpty(call.getPostBody())) {
                    LOGGER.debug("Event received '{}'", call.getPostBody());

                    XmlPath xmlPath = new XmlPath(call.getPostBody());
                    String id;
                    try {
                        String foundSubscriptionId = xmlPath.get("GetRecordsResponse.RequestId");

                        if (StringUtils.isNotBlank(foundSubscriptionId) && subscriptionId.equals(
                                foundSubscriptionId)) {
                            id = xmlPath.get("GetRecordsResponse.SearchResults.Record.identifier");

                            if (StringUtils.isNotEmpty(id)) {
                                foundIds.add(StringUtils.trim(id));

                            }
                        } else {
                            LOGGER.info("event for id {} not found.", subscriptionId);
                        }
                    } catch (ClassCastException e) {
                        // not necessarily a problem that an particular path (event) wasn't found
                        LOGGER.info("Unable to evaluate path for event {}", subscriptionId);
                    }
                }
            }

            LOGGER.debug("Id {}, Event Found Ids: {}",
                    subscriptionId,
                    Arrays.toString(foundIds.toArray()));
        }
        return foundIds;

    }

    public void setupConnectedSources() throws IOException {
        CswConnectedSourceProperties connectedSourceProperties = new CswConnectedSourceProperties(
                CONNECTED_SOURCE_ID);
        getServiceManager().createManagedService(CswConnectedSourceProperties.FACTORY_PID,
                connectedSourceProperties);
    }

    private String ingestXmlWithProduct(String fileName) throws IOException {
        File file = new File(fileName);
        if (!file.createNewFile()) {
            fail("Unable to create " + fileName + " file.");
        }
        FileUtils.write(file, SAMPLE_DATA);
        String fileLocation = file.toURI()
                .toURL()
                .toString();
        LOGGER.debug("File Location: {}", fileLocation);
        String metacardId = TestCatalog.ingest(Library.getSimpleXml(fileLocation), "text/xml");
        return metacardId;
    }

    private void cleanProductCache() throws IOException {
        FileUtils.cleanDirectory(Paths.get(ddfHome).resolve(PRODUCT_CACHE).toFile());
    }

    private Action getCswRetrievalHeaders() {
        return composite(header("X-Csw-Product", "true"));
    }

    @Override
    protected Option[] configureCustom() {
        return options(mavenBundle("ddf.test.thirdparty", "restito").versionAsInProject());
    }
}
