/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.test.itests.catalog;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
import static com.xebialabs.restito.semantics.Action.bytesContent;
import static com.xebialabs.restito.semantics.Action.composite;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.header;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Action.success;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withPostBodyContaining;
import static ddf.common.test.WaitCondition.expect;
import static ddf.test.itests.AbstractIntegrationTest.DynamicUrl.INSECURE_ROOT;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
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
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.internal.http.Method;
import com.jayway.restassured.path.json.JsonPath;
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
import ddf.common.test.cometd.CometDClient;
import ddf.common.test.cometd.CometDMessageValidator;
import ddf.common.test.mock.csw.FederatedCswMockServer;
import ddf.common.test.restito.ChunkedContent;
import ddf.common.test.restito.HeaderCapture;
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

    protected static final Logger LOGGER = LoggerFactory.getLogger(TestFederation.class);

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

    private static final DynamicPort RESTITO_STUB_SERVER_PORT = new DynamicPort(6);

    private static final Path PRODUCT_CACHE = Paths.get("data", "Product_Cache");

    private static final DynamicPort CSW_STUB_SERVER_PORT = new DynamicPort(7);

    private static final int NO_RETRIES = 0;

    private static final int FAIL_RETRIES = 3;

    private static final String ACTIVITIES_COMPLETED_MESSAGE = "completed";

    private static final String ACTIVITIES_FAILED_MESSAGE = "failed";

    private static final String ACTIVITES_STARTED_MESSAGE = "started";

    private static final int DOWNLOAD_SIZE = 6;

    public static final DynamicUrl CSW_STUB_SERVER_PATH = new DynamicUrl(INSECURE_ROOT,
            CSW_STUB_SERVER_PORT,
            "/services/csw");

    private static String[] metacardIds = new String[2];

    private List<String> metacardsToDelete = new ArrayList<>();

    private List<String> resourcesToDelete = new ArrayList<>();

    private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

    public static final DynamicUrl RESTITO_STUB_SERVER = new DynamicUrl("https://localhost:",
            RESTITO_STUB_SERVER_PORT,
            SUBSCRIBER);

    private static StubServer server;

    private static FederatedCswMockServer cswServer;

    private static final String NOTIFICATIONS_CHANNEL = "/ddf/notifications/**";

    private static final String ACTIVITIES_CHANNEL = "/ddf/activities/**";

    private static final String FIND_ACTION_URL_PATTERN =
            "data.results[0].metacard.actions.find {it.title=='%s'}.url";

    private static final String POLL_INTERVAL = "pollInterval";

    private static final String ADMIN_USERNAME = "admin";

    private static final String ADMIN_PASSWORD = "admin";

    private static final String LOCALHOST_USERNAME = "localhost";

    private static final String LOCALHOST_PASSWORD = "localhost";

    private static final int CSW_SOURCE_POLL_INTERVAL = 10;

    private static final int MAX_DOWNLOAD_RETRY_ATTEMPTS = 3;

    private CometDClient cometDClient;

    private CometDClient adminCometDClient;

    private CometDClient localhostCometDClient;

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
            getCatalogBundle().setupMaxDownloadRetryAttempts(MAX_DOWNLOAD_RETRY_ATTEMPTS);
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
            cswProperties.put(POLL_INTERVAL, CSW_SOURCE_POLL_INTERVAL);
            getServiceManager().createManagedService(CswSourceProperties.FACTORY_PID,
                    cswProperties);

            CswSourceProperties cswProperties2 = new CswSourceProperties(
                    CSW_SOURCE_WITH_METACARD_XML_ID);
            cswProperties2.put("outputSchema", "urn:catalog:metacard");
            cswProperties2.put(POLL_INTERVAL, CSW_SOURCE_POLL_INTERVAL);
            getServiceManager().createManagedService(CswSourceProperties.FACTORY_PID,
                    cswProperties2);

            CswSourceProperties gmdProperties = new CswSourceProperties(GMD_SOURCE_ID,
                    CswSourceProperties.GMD_FACTORY_PID);
            gmdProperties.put(POLL_INTERVAL, CSW_SOURCE_POLL_INTERVAL);
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

            getServiceManager().startFeature(true, "search-ui");

        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @Before
    public void setup() throws Exception {

        getCatalogBundle().setDownloadRetryDelayInSeconds(1);

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

        getSecurityPolicy().configureRestForGuest();

        // @formatter:off
        expect("List of active downloads is empty").within(30, SECONDS)
                .until(() -> when().get(RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl())
                        .then().log().all().extract().body().jsonPath().getList(""), hasSize(0));
        // @formatter:on

        if (server != null) {
            server.stop();
        }

        cleanupCometDClients();
    }

    private void cleanupCometDClients() {
        Arrays.asList(cometDClient, adminCometDClient, localhostCometDClient)
                .stream()
                .filter(Objects::nonNull)
                .forEach(cometDClient -> {
                    try {
                        cometDClient.shutdown();
                    } catch (Exception e) {
                        fail("Failed to shutdown cometD client!");
                    }
                });
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
     * Tests that a federated search by ID will return the right record after we change the id.
     *
     * @throws Exception
     */
    @Test
    public void testFederatedQueryByIdAfterIdChange() throws Exception {
        Configuration openSourceConfig = null;
        String newOpenSearchSourceId = OPENSEARCH_SOURCE_ID + "2";
        try {
            //change the opensearch source id
            OpenSearchSourceProperties openSearchProperties = new OpenSearchSourceProperties(
                    newOpenSearchSourceId);
            Configuration[] configs = configAdmin.listConfigurations(String.format("(%s=%s)",
                    ConfigurationAdmin.SERVICE_FACTORYPID,
                    OpenSearchSourceProperties.FACTORY_PID));
            openSourceConfig = configs[0];
            Dictionary<String, ?> configProps = new Hashtable<>(openSearchProperties);
            openSourceConfig.update(configProps);
            getServiceManager().waitForAllBundles();

            String restUrl = REST_PATH.getUrl() + "sources/" + newOpenSearchSourceId + "/"
                    + metacardIds[GEOJSON_RECORD_INDEX];

            // @formatter:off
            when().get(restUrl).then().log().all().assertThat().body(hasXPath(
                    "/metacard/string[@name='" + Metacard.TITLE + "']/value[text()='" + RECORD_TITLE_1
                            + "']"), not(containsString(RECORD_TITLE_2)));
            // @formatter:on
        } finally {
            //reset the opensearch source id
            OpenSearchSourceProperties openSearchProperties = new OpenSearchSourceProperties(
                    OPENSEARCH_SOURCE_ID);
            Dictionary<String, ?> configProps = new Hashtable<>(openSearchProperties);
            openSourceConfig.update(configProps);
            getServiceManager().waitForAllBundles();
        }

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
     * <p>
     * For example:
     * The resource uri in the metacard is:
     * file:/Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-ddf/target/exam/e59b02bf-5774-489f-8aa9-53cf99c25d25/../../testFederatedRetrieveProductInvalidResourceUrlWithBackReferences.txt
     * which really means:
     * file:/Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-ddf/target/testFederatedRetrieveProductInvalidResourceUrlWithBackReferences.txt
     * <p>
     * The URLResourceReader's root resource directories are:
     * <ddf.home>/data/products
     * and
     * /Users/andrewreynolds/projects/ddf-projects/ddf/distribution/test/itests/test-itests-ddf/target/exam/e59b02bf-5774-489f-8aa9-53cf99c25d25
     * <p>
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
                "/GetRecordsResponse/SearchResults/Record/identifier[text()='"
                        + metacardIds[GEOJSON_RECORD_INDEX] + "']"), hasXPath(
                "/GetRecordsResponse/SearchResults/Record/identifier[text()='"
                        + metacardIds[XML_RECORD_INDEX] + "']"), hasXPath(
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
            LOGGER.error("Couldn't create connected sources: {}", e.searchMessages());
        }
        */

        // @formatter:off
        given().auth().basic(ADMIN_USERNAME, ADMIN_PASSWORD).when().get(ADMIN_ALL_SOURCES_PATH.getUrl()).then()
                .log().all().assertThat().body(containsString("\"fpid\":\"OpenSearchSource\""),
                containsString("\"fpid\":\"Csw_Federated_Source\"")/*,
                containsString("\"fpid\":\"Csw_Connected_Source\"")*/);
        // @formatter:on
    }

    @Test
    public void testFederatedSourceStatus() {
        // Find and test OpenSearch Federated Source
        // @formatter:off
        String json = given().auth().basic(ADMIN_USERNAME, ADMIN_PASSWORD).when()
                .get(ADMIN_ALL_SOURCES_PATH.getUrl()).asString();
        // @formatter:on

        List<Map<String, Object>> sources = with(json).param("name", "OpenSearchSource")
                .get("value.findAll { source -> source.id == name}");
        String openSearchPid = (String) ((ArrayList<Map<String, Object>>) (sources.get(0)
                .get("configurations"))).get(0)
                .get("id");

        // @formatter:off
        given().auth().basic(ADMIN_USERNAME, ADMIN_PASSWORD).when()
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
        String json = given().auth().basic(ADMIN_USERNAME, ADMIN_PASSWORD).when()
                .get(ADMIN_ALL_SOURCES_PATH.getUrl()).asString();
        // @formatter:on

        List<Map<String, Object>> sources = with(json).param("name", "Csw_Connected_Source")
                .get("value.findAll { source -> source.id == name}");
        String connectedSourcePid = (String) ((ArrayList<Map<String, Object>>) (sources.get(0)
                .get("configurations"))).get(0)
                .get("id");

        // Test CSW Connected Source status
        // @formatter:off
        given().auth().basic(ADMIN_USERNAME, ADMIN_PASSWORD).when()
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

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
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
        given().contentType(ContentType.XML).when().delete(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract().body().xmlPath().get("Acknowledgement.RequestId").toString();

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
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
        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
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
        given().contentType(ContentType.XML).when().delete(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract().body().xmlPath().get("Acknowledgement.RequestId").toString();

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
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

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
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
        given().contentType(ContentType.XML).when()
                .delete(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
                .then().log().all().assertThat()
                .body(hasXPath("/Acknowledgement/RequestId"))
                .extract()
                .body()
                .xmlPath()
                .get("Acknowledgement.RequestId")
                .toString();

        given().contentType(ContentType.XML).when().get(CSW_SUBSCRIPTION_PATH.getUrl() + "/" + subscriptionId)
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
        cometDClient = setupCometDClient(Arrays.asList(NOTIFICATIONS_CHANNEL, ACTIVITIES_CHANNEL));

        String filename = "product1.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId
                + "?transform=resource" + "&session=" + cometDClient.getClientId();

        // Verify that the testData from the csw stub server is returned.
        // @formatter:off

        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData));
        // @formatter:on

        expect("Waiting for notifications").within(10, SECONDS)
                .until(() -> cometDClient.getMessages(NOTIFICATIONS_CHANNEL)
                        .size() == 1);
        expect("Waiting for activities").within(10, SECONDS)
                .until(() -> cometDClient.getMessages(ACTIVITIES_CHANNEL)
                        .size() == 2);

        assertThat(cometDClient.getAllMessages()
                .size(), is(3));

        List<String> notifications = cometDClient.getMessagesInAscOrder(NOTIFICATIONS_CHANNEL);
        assertThat(notifications.size(), is(1));
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(0)),
                filename,
                getResourceRetrievalCompletedMessage(resourceData.length()),
                "complete");

        List<String> activities = cometDClient.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        assertThat(activities.size(), is(2));
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(0)),
                filename,
                "Resource retrieval started. ",
                "STARTED");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(1)),
                filename,
                getResourceRetrievalCompletedMessage(resourceData.length()),
                "COMPLETE");
    }

    /**
     * Tests that if the endpoint disconnects twice, the retrieval retries both times
     *
     * @throws Exception
     */
    @Test
    public void testRetrievalReliablility() throws Exception {
        getSecurityPolicy().configureWebContextPolicy(null,
                "/=SAML|basic,/solr=SAML|PKI|basic",
                null,
                null);
        localhostCometDClient = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), LOCALHOST_USERNAME, LOCALHOST_PASSWORD);

        String filename = "product2.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                Duration.ofMillis(200))
                .fail(2)
                .build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Verify that the testData from the csw stub server is returned.
        // @formatter:off
        given().auth().preemptive().basic(LOCALHOST_USERNAME, LOCALHOST_PASSWORD)
                .get(restUrl).then().log().all()
                .assertThat().contentType("text/plain").body(is(resourceData));
        // @formatter:on

        cswServer.verifyHttp()
                .times(3,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId));

        /**
         * Verify that we get 3 notifictions: 2 retrying and 1 complete.
         */
        expect("Waiting for notifications").within(10, SECONDS)
                .until(() -> localhostCometDClient.getMessages(NOTIFICATIONS_CHANNEL)
                        .size() == 3);
        /**
         * Verify that we get 9 activity messages: started, downloading, retrying, and complete.
         */
        expect("Waiting for activities").within(10, SECONDS)
                .until(() -> localhostCometDClient.getMessages(ACTIVITIES_CHANNEL)
                        .size() == 9);

        List<String> notifications = localhostCometDClient.getMessagesInAscOrder(
                NOTIFICATIONS_CHANNEL);
        assertThat(notifications.size(), is(3));
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(0)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 1 of 3.",
                "retrying");
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(1)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 2 of 3.",
                "retrying");
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(2)),
                filename,
                String.format("Resource retrieval completed, %d bytes retrieved. ",
                        resourceData.length()),
                "complete");

        List<String> activities = localhostCometDClient.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        assertThat(activities.size(), is(9));
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(0)),
                filename,
                "Resource retrieval started. ",
                "STARTED");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(1)),
                filename,
                "Resource retrieval downloading . ",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(2)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 1 of 3.",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(3)),
                filename,
                "Resource retrieval downloading . ",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(4)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 2 of 3.",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(5)),
                filename,
                "Resource retrieval downloading . ",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(6)),
                filename,
                "Resource retrieval downloading . ",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(7)),
                filename,
                "Resource retrieval downloading . ",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(8)),
                filename,
                String.format("Resource retrieval completed, %d bytes retrieved. ",
                        resourceData.length()),
                "COMPLETE");
    }

    /**
     * Tests that if the endpoint disconnects twice, the retrieval retries both times
     * This test will respond with the correct Partial Content when a range header is sent in the request
     *
     * @throws Exception
     */
    @Test
    public void testRetrievalWithByteOffset() throws Exception {

        cometDClient = setupCometDClient(Arrays.asList(NOTIFICATIONS_CHANNEL, ACTIVITIES_CHANNEL));
        String filename = "product2.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);
        HeaderCapture headerCapture = new HeaderCapture();
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                Duration.ofMillis(200))
                .fail(2)
                .allowPartialContent(headerCapture)
                .build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId),
                        Condition.custom(headerCapture))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId
                + "?transform=resource" + "&session=" + cometDClient.getClientId();

        // Verify that the testData from the csw stub server is returned.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData));
        // @formatter:on

        cswServer.verifyHttp()
                .times(3,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId));

        // Add CometD notification and activity assertions when DDF-2272 has been addressed.

    }

    /**
     * Tests that if the endpoint disconnects 3 times, the retrieval fails after 3 attempts
     *
     * @throws Exception
     */
    @Test
    public void testRetrievalReliabilityFails() throws Exception {
        cometDClient = setupCometDClient(Arrays.asList(NOTIFICATIONS_CHANNEL, ACTIVITIES_CHANNEL));

        String filename = "product3.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                Duration.ofMillis(200))
                .fail(3)
                .build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId
                + "?transform=resource" + "&session=" + cometDClient.getClientId();

        // Verify that product retrieval fails from the csw stub server.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().statusCode(500).contentType("text/plain")
                .body(containsString("cannot retrieve product"));
        // @formatter:on

        expect("Waiting for notifications").within(10, SECONDS)
                .until(() -> cometDClient.getMessages(NOTIFICATIONS_CHANNEL)
                        .size() == 4);
        expect("Waiting for activities").within(10, SECONDS)
                .until(() -> cometDClient.getMessages(ACTIVITIES_CHANNEL)
                        .size() == 8);

        List<String> notifications = cometDClient.getMessagesInAscOrder(NOTIFICATIONS_CHANNEL);
        assertThat(notifications.size(), is(4));
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(0)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 1 of 3.",
                "retrying");
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(1)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 2 of 3.",
                "retrying");
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(2)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 3 of 3.",
                "retrying");
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(3)),
                filename,
                "Resource retrieval failed. Unable to retrieve product file.",
                "failed");

        List<String> activities = cometDClient.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        assertThat(activities.size(), is(8));
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(0)),
                filename,
                "Resource retrieval started. ",
                "STARTED");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(1)),
                filename,
                "Resource retrieval downloading . ",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(2)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 1 of 3.",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(3)),
                filename,
                "Resource retrieval downloading . ",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(4)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 2 of 3.",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(5)),
                filename,
                "Resource retrieval downloading . ",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(6)),
                filename,
                "Resource retrieval retrying after 1 bytes. Attempt 3 of 3.",
                "RUNNING");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(7)),
                filename,
                "Resource retrieval failed. Unable to retrieve product file.",
                "FAILED");
    }

    /**
     * Tests that ddf will return the cached copy if there are no changes to the remote metacard
     * Also tests that the file caches correctly when range headers are not supported
     *
     * @throws Exception
     */
    @Test
    public void testDownloadFromCacheIfAvailable() throws Exception {
        cometDClient = setupCometDClient(Arrays.asList(NOTIFICATIONS_CHANNEL, ACTIVITIES_CHANNEL));

        String filename = "product4.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId
                + "?transform=resource" + "&session=" + cometDClient.getClientId();

        // Download product twice, should only call the stub server to download once
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData));

        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData));
        // @formatter:on

        cswServer.verifyHttp()
                .times(1,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId));

        expect("Waiting for notifications. Received " + cometDClient.getMessages(
                NOTIFICATIONS_CHANNEL)
                .size() + " of 1").within(10, SECONDS)
                .until(() -> cometDClient.getMessages(NOTIFICATIONS_CHANNEL)
                        .size() == 1);
        expect("Waiting for activities. Received " + cometDClient.getMessages(ACTIVITIES_CHANNEL)
                .size() + " of 2").within(10, SECONDS)
                .until(() -> cometDClient.getMessages(ACTIVITIES_CHANNEL)
                        .size() == 2);

        List<String> notifications = cometDClient.getMessagesInAscOrder(NOTIFICATIONS_CHANNEL);
        assertThat(notifications.size(), is(1));
        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(0)),
                filename,
                getResourceRetrievalCompletedMessage(resourceData.length()),
                "complete");

        List<String> activities = cometDClient.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        assertThat(activities.size(), is(2));
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(0)),
                filename,
                "Resource retrieval started. ",
                "STARTED");
        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(1)),
                filename,
                getResourceRetrievalCompletedMessage(resourceData.length()),
                "COMPLETE");
    }

    /**
     * Tests that ddf will redownload a product if the remote metacard has changed
     *
     * @throws Exception
     */
    @Test
    public void testCacheIsUpdatedIfRemoteProductChanges() throws Exception {
        String filename = "product5.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId
                + "?transform=resource";

        // Download product twice, and change metacard on stub server between calls.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData));
        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId, OffsetDateTime.now()).getBytes()));
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData));
        // @formatter:on

        cswServer.verifyHttp()
                .times(2,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId));
    }

    /**
     * Tests that a product caches correctly when the download is interrupted twice and ddf uses
     * range header requests to re-eretrieve the undownloaded portion.
     *
     * @throws Exception
     */
    @Test
    public void testFileCachesCorrectlyWhenRangeHeadersAreSupported() throws Exception {
        cometDClient = setupCometDClient(Arrays.asList(NOTIFICATIONS_CHANNEL, ACTIVITIES_CHANNEL));
        String filename = "product2.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);
        HeaderCapture headerCapture = new HeaderCapture();
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                Duration.ofMillis(200))
                .fail(2)
                .allowPartialContent(headerCapture)
                .build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId),
                        Condition.custom(headerCapture))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId
                + "?transform=resource" + "&session=" + cometDClient.getClientId();

        // Verify that the testData from the csw stub server is returned.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData));

        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData));
        // @formatter:on

        cswServer.verifyHttp()
                .times(3,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId));

    }

    @Test
    public void testProductDownloadListEmptyWhenNoDownloads() {
        String getAllDownloadsUrl = RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl();

        assertThat(when().get(getAllDownloadsUrl)
                .then()
                .log()
                .all()
                .extract()
                .body()
                .jsonPath()
                .getList(""), is(empty()));
    }

    @Test
    public void testProductDownloadListWithOneActiveDownload() throws IOException {

        String filename = "product.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);

        int delayBetweenChunksInMillis = 500;

        DownloadHandle downloadHandle = new DownloadHandle(metacardId,
                filename,
                resourceData,
                NO_RETRIES,
                delayBetweenChunksInMillis);

        downloadHandle.startDownload();

        String getAllDownloadsUrl = RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl();

        // @formatter:off
        List<Map<String, Object>> downloads = expect("List of active downloads is not empty").within(30, SECONDS)
                .until(() -> when().get(getAllDownloadsUrl)
                        .then().log().all().extract().body().jsonPath().getList(""), hasSize(1)).lastResult();
        // @formatter:on

        Map download = downloads.get(0);
        assertThat(download.size(), is(DOWNLOAD_SIZE));

        downloadHandle.verifyGuestDownloadInProgress(download);

    }

    /**
     * Similar to the test for one active download but checks that two downloads can be active at once.
     *
     * @throws Exception
     */
    @Test
    public void testProductDownloadListWithTwoActiveDownloads() throws IOException {

        String filename1 = "product1.txt";
        String metacardId1 = generateUniqueMetacardId();
        String resourceData1 = getResourceData(metacardId1);

        String filename2 = "product2.txt";
        String metacardId2 = generateUniqueMetacardId();
        String resourceData2 = getResourceData(metacardId2);

        int delayBetweenChunksInMillis = 200;

        DownloadHandle downloadHandle1 = new DownloadHandle(metacardId1,
                filename1,
                resourceData1,
                NO_RETRIES,
                delayBetweenChunksInMillis);

        DownloadHandle downloadHandle2 = new DownloadHandle(metacardId2,
                filename2,
                resourceData2,
                NO_RETRIES,
                delayBetweenChunksInMillis);

        downloadHandle1.startDownload();
        String downloadId1 = downloadHandle1.getDownloadId();

        downloadHandle2.startDownload();
        String downloadId2 = downloadHandle2.getDownloadId();

        String getAllDownloadsUrl = RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl();

        // @formatter:off
        List<Map<String, Object>> downloads =
                expect("List of active downloads is not empty").within(30, SECONDS)
                        .until(() -> when().get(getAllDownloadsUrl).then().log().all()
                                .extract().body().jsonPath().getList(""), hasSize(2))
                        .lastResult();
        // @formatter:on

        Map download1 = downloads.get(0);
        assertThat(download1.size(), is(DOWNLOAD_SIZE));
        Map download2 = downloads.get(1);
        assertThat(download2.size(), is(DOWNLOAD_SIZE));

        if (download1.get("downloadId")
                .equals(downloadId1)) {
            downloadHandle1.verifyGuestDownloadInProgress(download1);
            downloadHandle2.verifyGuestDownloadInProgress(download2);

        } else if (download1.get("downloadId")
                .equals(downloadId2)) {
            downloadHandle2.verifyGuestDownloadInProgress(download1);
            downloadHandle1.verifyGuestDownloadInProgress(download2);
        } else {
            LOGGER.error("Unexpected data in the download");
            fail("Unexpected data in the download");
        }
    }

    /**
     * Determines that when two downloads are downloaded at once and one fails, it does not affect the
     * success of the other download.
     *
     * @throws Exception
     */
    @Test
    public void testProductDownloadListWithTwoActiveDownloadsOneFails() throws Exception {

        cometDClient = setupCometDClient(Arrays.asList(ACTIVITIES_CHANNEL));

        String filename1 = "product1.txt";
        String metacardId1 = generateUniqueMetacardId();
        String resourceData1 = getResourceData(metacardId1);

        String failFilename = "failProduct.txt";
        String failMetacardId = generateUniqueMetacardId();
        String failResourceData = getResourceData(failMetacardId);

        int delayBetweenChunksInMillis = 200;

        String restUrlFail =
                REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + failMetacardId
                        + "?transform=resource" + "&session=" + cometDClient.getClientId();

        String restUrl = REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId1
                + "?transform=resource" + "&session=" + cometDClient.getClientId();

        DownloadHandle downloadHandle1 = new DownloadHandle(metacardId1,
                filename1,
                resourceData1,
                NO_RETRIES,
                delayBetweenChunksInMillis);
        DownloadHandle downloadHandleFail = new DownloadHandle(failMetacardId,
                failFilename,
                failResourceData,
                FAIL_RETRIES,
                delayBetweenChunksInMillis);

        // Verify that product retrieval fails from the csw stub server.
        // @formatter:off
        when().get(restUrlFail).then().log().all().assertThat().statusCode(500).contentType("text/plain")
                .body(containsString("cannot retrieve product"));
        // @formatter:on

        //verify that before the successful download is started, there are zero requests for it
        //(to later confirm that the one request received is the test's request)
        cswServer.verifyHttp()
                .times(0,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId1));

        // Verify that the testData from the csw stub server is returned.
        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData1));
        // @formatter:on

        List<String> activities = cometDClient.getMessagesInAscOrder(ACTIVITIES_CHANNEL);

        expect("Waiting for activities").within(10, SECONDS)
                .until(() -> {
                    if (foundExpectedActivity(activities, filename1, ACTIVITES_STARTED_MESSAGE)
                            && foundExpectedActivity(activities,
                            failFilename,
                            ACTIVITES_STARTED_MESSAGE) && foundExpectedActivity(activities,
                            failFilename,
                            ACTIVITIES_FAILED_MESSAGE) && foundExpectedActivity(activities,
                            filename1,
                            ACTIVITIES_COMPLETED_MESSAGE)) {
                        return true;
                    }
                    return false;
                });

        downloadHandleFail.startDownload();
        downloadHandle1.startDownload();

        cswServer.verifyHttp()
                .times(1,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId1));

        //download again to confirm the successfully downloaded product is not downloaded from
        //the server a second time
        DownloadHandle repeatDownloadHandle = new DownloadHandle(metacardId1,
                filename1,
                resourceData1,
                NO_RETRIES,
                delayBetweenChunksInMillis);
        repeatDownloadHandle.startDownload();

        // @formatter:off
        when().get(restUrl).then().log().all().assertThat().contentType("text/plain")
                .body(is(resourceData1));
        // @formatter:on

        //we should still only have accessed the server once, because the data should come from
        //the cache the second time it is requested
        cswServer.verifyHttp()
                .times(1,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId1));

    }

    /**
     * Helper method used to determine that a certain message is showing up in the cometDClient activities.
     *
     * @param activities    the activity messages extracted from the CometDClient.
     * @param filename      the filename of the resource to check against the CometDClient.
     * @param messageToFind the message to find in the CometDClient activities connected to the filename.
     * @return a boolean that is only true when the message has been found in the activities and matched to the filename.
     */
    private boolean foundExpectedActivity(List<String> activities, String filename,
            String messageToFind) throws Exception {

        boolean found;

        if (filename.equals("") || messageToFind.equals("")) {
            throw new IllegalArgumentException();
        } else {
            LOGGER.debug("Found wanted messageToFind? {}",
                    activities.stream()
                            .anyMatch(activity -> activity.toString()
                                    .contains(messageToFind)));
            LOGGER.debug("Found wanted filename? {}",
                    activities.stream()
                            .anyMatch(activity -> activity.toString()
                                    .contains(filename)));

            found = activities.stream()
                    .anyMatch(activity -> {
                        if (activity.toString()
                                .contains(messageToFind) && activity.toString()
                                .contains(filename)) {
                            return true;
                        }
                        return false;
                    });

        }

        return found;
    }

    @Test
    public void testCancelDownload() throws Exception {
        getSecurityPolicy().configureWebContextPolicy(null,
                "/=SAML|basic,/solr=SAML|PKI|basic",
                null,
                null);
        localhostCometDClient = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), "localhost", "localhost");
        String filename = testName + ".txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                Duration.ofMillis(200))
                .fail(0)
                .build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);

        String startDownloadUrl =
                RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl() + "?source=" + CSW_STUB_SOURCE_ID
                        + "&metacard=" + metacardId;

        // @formatter:off
        String downloadId = given().auth().preemptive().basic(LOCALHOST_USERNAME, LOCALHOST_PASSWORD)
                .get(startDownloadUrl).then().log().all()
                .extract().jsonPath().getString("downloadId");
        // @formatter:on

        localhostCometDClient.cancelDownload(downloadId);

        expect("Waiting for notifications.").within(10, SECONDS)
                .until(() -> localhostCometDClient.getMessages(NOTIFICATIONS_CHANNEL)
                        .size() == 1);

        /**
         * Wait for 2 activities. The first one is the download started, and the second one is the
         * download canceled.
         */
        expect("Waiting for activities.").within(10, SECONDS)
                .until(() -> localhostCometDClient.getMessages(ACTIVITIES_CHANNEL)
                        .size() == 2);

        List<String> notifications = localhostCometDClient.getMessagesInAscOrder(
                NOTIFICATIONS_CHANNEL);

        List<String> activities = localhostCometDClient.getMessagesInAscOrder(ACTIVITIES_CHANNEL);

        CometDMessageValidator.verifyNotification(JsonPath.from(notifications.get(0)),
                filename,
                "Resource retrieval cancelled. ",
                "cancelled");

        CometDMessageValidator.verifyActivity(JsonPath.from(activities.get(1)),
                filename,
                "Resource retrieval cancelled. ",
                "STOPPED");
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
        // TODO - Need to update assertion when test is re-enabled
        // @formatter:on

        assertThat(Files.exists(Paths.get(ddfHome)
                .resolve(PRODUCT_CACHE)
                .resolve(CSW_SOURCE_ID + "-" + metacardId)), is(true));
        assertThat(Files.exists(Paths.get(ddfHome)
                .resolve(PRODUCT_CACHE)
                .resolve(CSW_SOURCE_ID + "-" + metacardId + ".ser")), is(true));
    }

    @Ignore
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

    @Test
    public void testProductDownloadWithTwoUsers() throws Exception {
        getSecurityPolicy().configureWebContextPolicy(null,
                "/=SAML|basic,/solr=SAML|PKI|basic",
                null,
                null);

        adminCometDClient = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), ADMIN_USERNAME, ADMIN_PASSWORD);
        localhostCometDClient = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), LOCALHOST_USERNAME, LOCALHOST_PASSWORD);

        String filename1 = "product4.txt";
        String metacardId1 = generateUniqueMetacardId();
        String resourceData1 = getResourceData(metacardId1);
        Action response1 = new ChunkedContent.ChunkedContentBuilder(resourceData1).build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId1))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId1).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId1))
                .then(getCswRetrievalHeaders(filename1), response1);

        String filename2 = "product5.txt";
        String metacardId2 = generateUniqueMetacardId();
        String resourceData2 = getResourceData(metacardId2);
        Action response2 = new ChunkedContent.ChunkedContentBuilder(resourceData2).build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId2))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId2).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId2))
                .then(getCswRetrievalHeaders(filename2), response2);

        String resourceDownloadUrlAdminUser = String.format("%ssources/%s/%s?transform=resource",
                REST_PATH.getUrl(),
                CSW_STUB_SOURCE_ID,
                metacardId1);

        String resourceDownloadUrlLocalhostUser =
                String.format("%ssources/%s/%s?transform=resource",
                        REST_PATH.getUrl(),
                        CSW_STUB_SOURCE_ID,
                        metacardId2);

        // @formatter:off
        given().auth().preemptive().basic(ADMIN_USERNAME, ADMIN_PASSWORD).when()
                .get(resourceDownloadUrlAdminUser).then().log().all()
                .assertThat().contentType("text/plain").body(is(resourceData1));
        given().auth().preemptive().basic(LOCALHOST_USERNAME, LOCALHOST_PASSWORD)
                .get(resourceDownloadUrlLocalhostUser).then().log().all()
                .assertThat().contentType("text/plain").body(is(resourceData2));
        // @formatter:on

        cswServer.verifyHttp()
                .times(1,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId1));

        cswServer.verifyHttp()
                .times(1,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId2));

        expect("Waiting for notifications. Received " + adminCometDClient.getMessages(
                NOTIFICATIONS_CHANNEL)
                .size() + " of 1").within(10, SECONDS)
                .until(() -> adminCometDClient.getMessages(NOTIFICATIONS_CHANNEL)
                        .size() == 1);
        expect("Waiting for activities. Received " + adminCometDClient.getMessages(
                ACTIVITIES_CHANNEL)
                .size() + " of 2").within(10, SECONDS)
                .until(() -> adminCometDClient.getMessages(ACTIVITIES_CHANNEL)
                        .size() == 2);

        expect("Waiting for notifications. Received " + localhostCometDClient.getMessages(
                NOTIFICATIONS_CHANNEL)
                .size() + " of 1").within(10, SECONDS)
                .until(() -> localhostCometDClient.getMessages(NOTIFICATIONS_CHANNEL)
                        .size() == 1);
        expect("Waiting for activities. Received " + localhostCometDClient.getMessages(
                ACTIVITIES_CHANNEL)
                .size() + " of 2").within(10, SECONDS)
                .until(() -> localhostCometDClient.getMessages(ACTIVITIES_CHANNEL)
                        .size() == 2);

        List<String> adminNotifications = adminCometDClient.getMessagesInAscOrder(
                NOTIFICATIONS_CHANNEL);
        assertThat(adminNotifications.size(), is(1));
        CometDMessageValidator.verifyNotification(JsonPath.from(adminNotifications.get(0)),
                filename1,
                String.format("Resource retrieval completed, %d bytes retrieved. ",
                        resourceData1.length()),
                "complete");

        List<String> localhostNotifications = localhostCometDClient.getMessagesInAscOrder(
                NOTIFICATIONS_CHANNEL);
        assertThat(adminNotifications.size(), is(1));
        CometDMessageValidator.verifyNotification(JsonPath.from(localhostNotifications.get(0)),
                filename2,
                String.format("Resource retrieval completed, %d bytes retrieved. ",
                        resourceData2.length()),
                "complete");

        List<String> adminActivities = adminCometDClient.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        assertThat(adminActivities.size(), is(2));
        CometDMessageValidator.verifyActivity(JsonPath.from(adminActivities.get(0)),
                filename1,
                "Resource retrieval started. ",
                "STARTED");
        CometDMessageValidator.verifyActivity(JsonPath.from(adminActivities.get(1)),
                filename1,
                String.format("Resource retrieval completed, %d bytes retrieved. ",
                        resourceData1.length()),
                "COMPLETE");

        List<String> localhostActivities = localhostCometDClient.getMessagesInAscOrder(
                ACTIVITIES_CHANNEL);
        assertThat(localhostActivities.size(), is(2));
        CometDMessageValidator.verifyActivity(JsonPath.from(localhostActivities.get(0)),
                filename2,
                "Resource retrieval started. ",
                "STARTED");
        CometDMessageValidator.verifyActivity(JsonPath.from(localhostActivities.get(1)),
                filename2,
                String.format("Resource retrieval completed, %d bytes retrieved. ",
                        resourceData2.length()),
                "COMPLETE");
    }

    @Test
    public void testSingleUserDownloadSameProductSyncAndAsync() throws Exception {
        getSecurityPolicy().configureWebContextPolicy(null,
                "/=SAML|basic,/solr=SAML|PKI|basic",
                null,
                null);

        adminCometDClient = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), ADMIN_USERNAME, ADMIN_PASSWORD);

        String filename = "product4.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);

        setupStubCswResponse(filename, metacardId, resourceData);

        String resourceDownloadUrlLocalhostUserSync =
                REST_PATH.getUrl() + "sources/" + CSW_STUB_SOURCE_ID + "/" + metacardId
                        + "?transform=resource";

        String resourceDownloadUrlLocalhostUserAsync =
                RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl() + "?source=" + CSW_STUB_SOURCE_ID
                        + "&metacard=" + metacardId;

        // Download product via async and then sync, should only call the stub server to download once
        String downloadId = given().auth()
                .preemptive()
                .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
                .when()
                .get(resourceDownloadUrlLocalhostUserAsync)
                .then()
                .log()
                .all()
                .extract()
                .jsonPath()
                .getString("downloadId");
        assertThat(downloadId, is(notNullValue()));
        given().auth()
                .preemptive()
                .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
                .when()
                .get(resourceDownloadUrlLocalhostUserSync)
                .then()
                .log()
                .all()
                .assertThat()
                .contentType("text/plain")
                .body(is(resourceData));

        verifyCswStubCall(1, metacardId);

        List<String> adminNotifications = adminCometDClient.getMessagesInAscOrder(
                NOTIFICATIONS_CHANNEL);
        assertThat(adminNotifications.size(), is(1));
        CometDMessageValidator.verifyNotification(JsonPath.from(adminNotifications.get(0)),
                filename,
                String.format("Resource retrieval completed, %d bytes retrieved. ",
                        resourceData.length()),
                "complete");
    }

    @Test
    public void testSingleUserDownloadSameProductAsync() throws Exception {
        getSecurityPolicy().configureWebContextPolicy(null,
                "/=SAML|basic,/solr=SAML|PKI|basic",
                null,
                null);

        adminCometDClient = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), ADMIN_USERNAME, ADMIN_PASSWORD);

        String filename = "product4.txt";
        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);

        setupStubCswResponse(filename, metacardId, resourceData);

        String resourceDownloadUrlLocalhostUserAsync =
                RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl() + "?source=" + CSW_STUB_SOURCE_ID
                        + "&metacard=" + metacardId;

        // Download product twice via async, should only call the stub server to download once
        String downloadId = given().auth()
                .preemptive()
                .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
                .when()
                .get(resourceDownloadUrlLocalhostUserAsync)
                .then()
                .log()
                .all()
                .extract()
                .jsonPath()
                .getString("downloadId");
        assertThat(downloadId, is(notNullValue()));
        String downloadId2 = given().auth()
                .preemptive()
                .basic(ADMIN_USERNAME, ADMIN_PASSWORD)
                .when()
                .get(resourceDownloadUrlLocalhostUserAsync)
                .then()
                .log()
                .all()
                .extract()
                .jsonPath()
                .getString("downloadId");
        assertThat(downloadId2, is(nullValue()));

        verifyCswStubCall(1, metacardId);

        List<String> adminNotifications = adminCometDClient.getMessagesInAscOrder(
                NOTIFICATIONS_CHANNEL);
        assertThat(adminNotifications.size(), is(1));
        CometDMessageValidator.verifyNotification(JsonPath.from(adminNotifications.get(0)),
                filename,
                String.format("Resource retrieval completed, %d bytes retrieved. ",
                        resourceData.length()),
                "complete");
    }

    private void setupStubCswResponse(String filename, String metacardId, String resourceData) {
        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                Duration.ofMillis(0))
                .fail(NO_RETRIES)
                .build();
        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);
    }

    private void verifyCswStubCall(int expectedCallCount, String metacardId) {
        cswServer.verifyHttp()
                .times(expectedCallCount,
                        Condition.uri("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId));
    }

    @Test
    public void testTwoUsersSameProductRetrySuccess() throws Exception {

        String filename = "product2.txt";

        CometDClient cometDClient1 = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), "localhost", "localhost");
        CometDClient cometDClient2 = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), "admin", "admin");

        setupCswServerForSuccess(cometDClient1, filename);
        setupCswServerForSuccess(cometDClient2, filename);

        // Verify that we get 3 notifications: 2 retrying and 1 complete.
        // Verify that we get 9 activity messages: started, downloading, retrying, downloading,
        // retrying, downloading, downloading, downloading, completed
        checkExpectations(cometDClient1, 3, 9);
        checkExpectations(cometDClient2, 3, 9);

        List<String> activities = cometDClient1.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        boolean activityFound = foundExpectedActivity(activities, filename, "COMPLETE");
        assertThat(activityFound, is(equalTo(true)));

        activities = cometDClient2.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        activityFound = foundExpectedActivity(activities, filename, "COMPLETE");
        assertThat(activityFound, is(equalTo(true)));

        List<String> notifications = cometDClient1.getMessagesInAscOrder(NOTIFICATIONS_CHANNEL);
        boolean notificationFound = foundExpectedActivity(notifications, filename, "completed");
        assertThat(notificationFound, is(equalTo(true)));

        notifications = cometDClient2.getMessagesInAscOrder(NOTIFICATIONS_CHANNEL);
        notificationFound = foundExpectedActivity(notifications, filename, "completed");
        assertThat(notificationFound, is(equalTo(true)));

    }

    @Test
    public void testTwoUsersSameProductRetryFailure() throws Exception {

        String filename = "product2.txt";

        CometDClient cometDClient1 = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), "localhost", "localhost");
        CometDClient cometDClient2 = setupCometDClientWithUser(Arrays.asList(NOTIFICATIONS_CHANNEL,
                ACTIVITIES_CHANNEL), "admin", "admin");

        setupCswServerForFailure(cometDClient1, filename);
        setupCswServerForFailure(cometDClient2, filename);

        // Verify that we get 4 notifications: 3 retrying and 1 complete.
        // Verify that we get 8 activity messages: started, downloading, retrying, downloading,
        // retrying, downloading, retrying, failed
        checkExpectations(cometDClient1, 4, 8);
        checkExpectations(cometDClient2, 4, 8);

        List<String> activities = cometDClient1.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        boolean activityFound = foundExpectedActivity(activities, filename, "failed");
        assertThat(activityFound, is(equalTo(true)));

        activities = cometDClient2.getMessagesInAscOrder(ACTIVITIES_CHANNEL);
        activityFound = foundExpectedActivity(activities, filename, "failed");
        assertThat(activityFound, is(equalTo(true)));

        List<String> notifications = cometDClient1.getMessagesInAscOrder(NOTIFICATIONS_CHANNEL);
        boolean notificationFound = foundExpectedActivity(notifications, filename, "failed");
        assertThat(notificationFound, is(equalTo(true)));

        notifications = cometDClient2.getMessagesInAscOrder(NOTIFICATIONS_CHANNEL);
        notificationFound = foundExpectedActivity(notifications, filename, "failed");
        assertThat(notificationFound, is(equalTo(true)));

    }

    /**
     * Tests that the async download action's URL is returned in the CometD search results.
     *
     * @throws Exception
     */
    @Test
    public void testAsyncDownloadActionPresentUsingCometDClient() throws Exception {

        String src = "ddf.distribution";
        String metacardId = ingestXmlWithProduct(String.format("%s.txt", testName.getMethodName()));
        String responseChannelId = "0193d9e7f9ed4f8f8bd02103143c41d6";
        String responseChannelPath = String.format("/%s", responseChannelId);
        String expectedUrl = String.format("%s?source=%s&metacard=%s",
                RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl(),
                src,
                metacardId);

        metacardsToDelete.add(metacardId);

        cometDClient = setupCometDClient(Collections.singletonList(responseChannelPath));

        cometDClient.searchByMetacardId(responseChannelId, src, metacardId);

        expect("CometD query response").within(20, SECONDS)
                .until(() -> cometDClient.getMessages(responseChannelPath)
                        .size() >= 1);

        Optional<String> foundString = cometDClient.searchMessages(metacardId);

        assertThat("Async download action not found", foundString.isPresent(), is(true));

        JsonPath path = JsonPath.from(foundString.get());

        assertThat(path.getString(String.format(FIND_ACTION_URL_PATTERN,
                "Download resource to local cache")), is(expectedUrl));
    }

    private void setupCswServerForSuccess(CometDClient cometDClient, String filename)
            throws Exception {

        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);

        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                Duration.ofMillis(200))
                .fail(2)
                .build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = String.format("%s%s%s%s%s%s%s%s",
                REST_PATH.getUrl(),
                "sources/",
                CSW_STUB_SOURCE_ID,
                "/",
                metacardId,
                "?transform=resource",
                "&session=",
                cometDClient.getClientId());

        // Verify that the testData from the csw stub server is returned.
        when().get(restUrl)
                .then()
                .log()
                .all()
                .assertThat()
                .contentType("text/plain")
                .body(is(resourceData));

    }

    private void setupCswServerForFailure(CometDClient cometDClient, String filename)
            throws Exception {

        String metacardId = generateUniqueMetacardId();
        String resourceData = getResourceData(metacardId);

        Action response = new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                Duration.ofMillis(200))
                .fail(3)
                .build();

        cswServer.whenHttp()
                .match(post("/services/csw"),
                        withPostBodyContaining("GetRecords"),
                        withPostBodyContaining(metacardId))
                .then(ok(),
                        contentType("text/xml"),
                        bytesContent(getCswQueryResponse(metacardId).getBytes()));

        cswServer.whenHttp()
                .match(Condition.get("/services/csw"),
                        Condition.parameter("request", "GetRecordById"),
                        Condition.parameter("id", metacardId))
                .then(getCswRetrievalHeaders(filename), response);

        String restUrl = String.format("%s%s%s%s%s%s%s%s",
                REST_PATH.getUrl(),
                "sources/",
                CSW_STUB_SOURCE_ID,
                "/",
                metacardId,
                "?transform=resource",
                "&session=",
                cometDClient.getClientId());

        // Verify that product retrieval fails from the csw stub server.
        when().get(restUrl)
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(500)
                .contentType("text/plain")
                .body(containsString("cannot retrieve product"));

    }

    private void checkExpectations(CometDClient cometDClient, Integer numNotifications,
            Integer numActivities) throws Exception {
        expect("Waiting for notifications").within(10, SECONDS)
                .until(() -> cometDClient.getMessages(NOTIFICATIONS_CHANNEL)
                        .size() == numNotifications);
        expect("Waiting for activities").within(10, SECONDS)
                .until(() -> cometDClient.getMessages(ACTIVITIES_CHANNEL)
                        .size() == numActivities);

    }

    private String generateUniqueMetacardId() {
        return UUID.randomUUID()
                .toString();
    }

    private String getCswQueryResponse(String metacardId) {
        return getCswQueryResponse(metacardId,
                OffsetDateTime.of(2016, 6, 15, 12, 30, 25, 100, ZoneOffset.ofHours(-7)));
    }

    private String getCswQueryResponse(String metacardId, OffsetDateTime modifiedTimestamp) {
        String modifiedTime = modifiedTimestamp.format(DateTimeFormatter.ofPattern(
                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));

        return Library.getFileContent("/csw-query-response.xml",
                ImmutableMap.of("sourceId",
                        CSW_STUB_SOURCE_ID,
                        "httpRoot",
                        INSECURE_ROOT,
                        "port",
                        CSW_STUB_SERVER_PORT.getPort(),
                        "modifiedTime",
                        modifiedTime,
                        "metacardId",
                        metacardId));
    }

    private void verifyEvents(Set<String> metacardIdsExpected, Set<String> metacardIdsNotExpected,
            Set<String> subscriptionIds) {
        long millis = 0;

        boolean isAllEventsReceived = false;
        boolean isUnexpectedEventReceived = false;

        while (!isAllEventsReceived && !isUnexpectedEventReceived
                && millis < TimeUnit.MINUTES.toMillis(2)) {

            Set<String> foundIds;

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

        HashSet<String> foundIds = new HashSet<>();
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

    private void setupConnectedSources() throws IOException {
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
        return TestCatalog.ingest(Library.getSimpleXml(fileLocation), "text/xml");
    }

    private Action getCswRetrievalHeaders(String filename) {
        return composite(header("X-Csw-Product", "true"),
                header("Content-Disposition", "filename=" + filename));
    }

    private String getResourceData(String metacardId) {
        return String.format("Data for metacard ID %s", metacardId);
    }

    private String getResourceRetrievalCompletedMessage(int bytesRetrieved) {
        return String.format("Resource retrieval completed, %d bytes retrieved. ", bytesRetrieved);
    }

    private CometDClient setupCometDClient(List<String> channels) throws Exception {
        String cometDEndpointUrl = COMETD_ENDPOINT.getUrl();

        CometDClient cometDClient = new CometDClient(cometDEndpointUrl);
        cometDClient.start();
        channels.forEach(cometDClient::subscribe);
        return cometDClient;
    }

    private CometDClient setupCometDClientWithUser(List<String> channels, String user,
            String password) throws Exception {
        String cometDEndpointUrl = COMETD_ENDPOINT.getUrl();

        CometDClient cometDClient = new CometDClient(cometDEndpointUrl, "karaf", user, password);
        cometDClient.start();
        channels.forEach(cometDClient::subscribe);
        return cometDClient;
    }

    @Override
    protected Option[] configureCustom() {
        return options(mavenBundle("ddf.test.thirdparty", "restito").versionAsInProject());
    }

    /**
     * Sets up a download response and starts a download for testing in progress downloads.
     */
    private class DownloadHandle {

        private String filename;

        private String resourceData;

        private String startDownloadUrl;

        String downloadId;

        DownloadHandle(String metacardId, String filename, String resourceData, int retries,
                int delayBetweenChunksInMillis) {

            this.filename = filename;
            this.resourceData = resourceData;
            downloadId = "";

            Action response =
                    new ChunkedContent.ChunkedContentBuilder(resourceData).delayBetweenChunks(
                            Duration.ofMillis(delayBetweenChunksInMillis))
                            .fail(retries)
                            .build();

            cswServer.whenHttp()
                    .match(post("/services/csw"),
                            withPostBodyContaining("GetRecords"),
                            withPostBodyContaining(metacardId))
                    .then(ok(),
                            contentType("text/xml"),
                            bytesContent(getCswQueryResponse(metacardId).getBytes()));

            cswServer.whenHttp()
                    .match(Condition.get("/services/csw"),
                            Condition.parameter("request", "GetRecordById"),
                            Condition.parameter("id", metacardId))
                    .then(getCswRetrievalHeaders(filename), response);

            startDownloadUrl =
                    RESOURCE_DOWNLOAD_ENDPOINT_ROOT.getUrl() + "?source=" + CSW_STUB_SOURCE_ID
                            + "&metacard=" + metacardId;
        }

        /**
         * Starts the download and stores the download Id. This must be called before
         * getDownloadId() to get a valid download id.
         */
        public void startDownload() {
            // @formatter:off
            downloadId = when().get(startDownloadUrl).then().log().all()
                    .extract().jsonPath().getString("downloadId");
            // @formatter:on
            assertThat(downloadId, not(isEmptyString()));
        }

        /**
         * Used to compare the expected information about the download and the actual information
         * pulled from the list about a download.
         *
         * @param download the map containing the actual download list information to compare.
         */
        public void verifyGuestDownloadInProgress(Map download) {
            assertThat(download.get("downloadId"), is(downloadId));
            assertThat(download.get("fileName"), is(filename));
            assertThat(download.get("status"), is("IN_PROGRESS"));
            int bytesDownloaded = (int) download.get("bytesDownloaded");
            assertThat(bytesDownloaded, is(greaterThan(0)));
            assertThat(bytesDownloaded, is(lessThan(resourceData.length() + 1)));
            assertTrue(((String) download.get("percentDownloaded")).matches(
                    "UNKNOWN|[0-9]|[0-9]{2}|100"));
            assertThat((List<String>) download.get("users"), contains("Guest@Guest@127.0.0.1"));

        }

        /**
         * @return the download id. If there is no valid download id, it will be an empty string.
         */
        public String getDownloadId() {
            return downloadId;
        }
    }
}
