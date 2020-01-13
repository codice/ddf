/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.test.itests.core;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.awaitility.Awaitility.await;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.INSECURE_ROOT;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.CSW_FEDERATED_SOURCE_FACTORY_PID;
import static org.codice.ddf.itests.common.csw.CswTestCommons.GMD_CSW_FEDERATED_SOURCE_FACTORY_PID;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswInsertRequest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswQuery;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswSourceProperties;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getMetacardIdFromCswInsertResponse;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.OPENSEARCH_FACTORY_PID;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearch;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearchSourceProperties;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.xml.HasXPath.hasXPath;

import com.google.common.collect.ImmutableMap;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.response.Response;
import com.xebialabs.restito.server.StubServer;
import com.xebialabs.restito.server.secure.SecureStubServer;
import ddf.catalog.data.Metacard;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.config.UrlResourceReaderConfigurator;
import org.codice.ddf.itests.common.csw.mock.FederatedCswMockServer;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class DdfCoreIT extends AbstractIntegrationTest {

  private static final DynamicUrl SECURE_ROOT_AND_PORT =
      new DynamicUrl(DynamicUrl.SECURE_ROOT, HTTPS_PORT);

  private static final DynamicUrl ADMIN_PATH =
      new DynamicUrl(SECURE_ROOT_AND_PORT, "/admin/index.html");

  private static final String RECORD_TITLE_1 = "myTitle";

  private static final String RECORD_TITLE_2 = "myXmlTitle";

  private static final DynamicPort RESTITO_STUB_SERVER_PORT = new DynamicPort(6);
  private UrlResourceReaderConfigurator urlResourceReaderConfigurator;
  private static StubServer server;
  private static final String SAMPLE_DATA = "sample data";
  private static FederatedCswMockServer cswServer;
  private static final String DEFAULT_SAMPLE_PRODUCT_FILE_NAME = "sample.txt";
  private static final String DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS = "data/products";
  private final List<String> resourcesToDelete = new ArrayList<>();

  private static final int MAX_DOWNLOAD_RETRY_ATTEMPTS = 3;
  private static final String CSW_STUB_SOURCE_ID = "cswStubServer";
  private static final DynamicPort CSW_STUB_SERVER_PORT = new DynamicPort(7);
  private static final DynamicUrl CSW_STUB_SERVER_PATH =
      new DynamicUrl(INSECURE_ROOT, CSW_STUB_SERVER_PORT, "/services/csw");
  private static final String POLL_INTERVAL = "pollInterval";
  private static final int CSW_SOURCE_POLL_INTERVAL = 10;
  private static final String CSW_SOURCE_WITH_METACARD_XML_ID = "cswSource2";
  private static final String GMD_SOURCE_ID = "gmdSource";

  @BeforeExam
  public void beforeExam() throws Exception {
    getCatalogBundle().setupMaxDownloadRetryAttempts(MAX_DOWNLOAD_RETRY_ATTEMPTS);

    setupOpenSearch();

    setupCswServer();

    setupGmd();

    getCatalogBundle().waitForFederatedSource(OPENSEARCH_SOURCE_ID);
    getCatalogBundle().waitForFederatedSource(CSW_STUB_SOURCE_ID);
    getCatalogBundle().waitForFederatedSource(CSW_SOURCE_ID);
    getCatalogBundle().waitForFederatedSource(CSW_SOURCE_WITH_METACARD_XML_ID);
    getCatalogBundle().waitForFederatedSource(GMD_SOURCE_ID);

    getServiceManager()
        .waitForSourcesToBeAvailable(
            REST_PATH.getUrl(),
            OPENSEARCH_SOURCE_ID,
            CSW_STUB_SOURCE_ID,
            CSW_SOURCE_ID,
            CSW_SOURCE_WITH_METACARD_XML_ID,
            GMD_SOURCE_ID);

    getCatalogBundle().setDownloadRetryDelayInSeconds(1);
    getCatalogBundle().setupCaching(false);

    LOGGER.info("Source status: \n{}", get(REST_PATH.getUrl() + "sources").body().prettyPrint());
  }

  @AfterExam
  public void afterExam() {
    if (cswServer != null) {
      cswServer.stop();
    }
  }

  @Before
  public void setup() throws Exception {
    urlResourceReaderConfigurator = getUrlResourceReaderConfigurator();

    server = new SecureStubServer(Integer.parseInt(RESTITO_STUB_SERVER_PORT.getPort())).run();
    server.start();

    cswServer.reset();
  }

  @After
  public void tearDown() throws Exception {
    clearCatalogAndWait();
    configureRestForGuest();

    urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
        DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS);

    for (String resource : resourcesToDelete) {
      FileUtils.deleteQuietly(new File(resource));
    }

    resourcesToDelete.clear();

    cswServer.stop();

    if (server != null) {
      server.stop();
    }
  }

  @Test
  public void testCswIngest() {
    Response response = ingestCswRecord();

    response
        .then()
        .body(
            hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
            hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
            hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
            hasXPath(
                "//TransactionResponse/InsertResult/BriefRecord/title",
                is("Aliquam fermentum purus quis arcu")),
            hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));
  }

  @Test
  public void testCswDeleteMultiple() {
    ingestCswRecord();
    ingestCswRecord();

    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswFilterDeleteRequest"))
        .post(CSW_PATH.getUrl())
        .then()
        .body(
            hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("2")),
            hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
            hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
  }

  @Test
  public void testCswUpdateByFilterConstraint() throws IOException, XPathExpressionException {
    Response firstResponse = ingestCswRecord();
    Response secondResponse = ingestCswRecord();

    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateByFilterConstraintRequest"))
        .post(CSW_PATH.getUrl())
        .then()
        .body(
            hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
            hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
            hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("2")));

    String firstId = getMetacardIdFromCswInsertResponse(firstResponse);
    String secondId = getMetacardIdFromCswInsertResponse(secondResponse);

    String firstUrl = REST_PATH.getUrl() + firstId;
    when()
        .get(firstUrl)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        // Check that the updated attributes were changed.
        .body(
            hasXPath("//metacard/dateTime[@name='modified']/value", startsWith("2015-08-25")),
            hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
            hasXPath("//metacard/string[@name='media.format']/value", is("")),
            // Check that an attribute that was not updated was not changed.
            hasXPath(
                "//metacard/string[@name='topic.category']/value",
                is("Hydrography--Dictionaries")));

    String secondUrl = REST_PATH.getUrl() + secondId;
    when()
        .get(secondUrl)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        // Check that the updated attributes were changed.
        .body(
            hasXPath("//metacard/dateTime[@name='modified']/value", startsWith("2015-08-25")),
            hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
            hasXPath("//metacard/string[@name='media.format']/value", is("")),
            // Check that an attribute that was not updated was not changed.
            hasXPath(
                "//metacard/string[@name='topic.category']/value",
                is("Hydrography--Dictionaries")));
  }

  @Test
  public void testBasicRestAccess() throws Exception {
    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";

    waitForSecurityHandlers(url);

    configureRestForBasic("/services/sdk");

    // Make sure that no credentials receives a 401
    getSecurityPolicy().waitForBasicAuthReady(url);
    when().get(url).then().log().all().assertThat().statusCode(equalTo(401));

    // A random user receives a 401
    given()
        .auth()
        .basic("bad", "user")
        .when()
        .get(url)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(401));

    // A real user receives a SSO token
    String cookie =
        given()
            .auth()
            .basic("admin", "admin")
            .when()
            .get(url)
            .then()
            .log()
            .ifValidationFails()
            .assertThat()
            .statusCode(equalTo(200))
            .assertThat()
            .header("Set-Cookie", containsString("JSESSIONID"))
            .extract()
            .cookie("JSESSIONID");

    // Try the session instead of basic auth
    given()
        .cookie("JSESSIONID", cookie)
        .when()
        .get(url)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200));

    // Admin user should be able to access the admin page
    given()
        .cookie("JSESSIONID", cookie)
        .when()
        .get(ADMIN_PATH.getUrl())
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200));
  }

  @Test
  public void testFederatedSpatial() throws IOException {
    ingest(getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"), "application/json");
    ingestXmlWithProduct(DEFAULT_SAMPLE_PRODUCT_FILE_NAME);

    getOpenSearch(
            "xml",
            null,
            null,
            "lat=10.0",
            "lon=30.0",
            "radius=250000",
            "spatialType=POINT_RADIUS",
            "src=" + OPENSEARCH_SOURCE_ID)
        .assertThat()
        .body(
            Matchers.hasXPath(
                "/metacards/metacard/string[@name='"
                    + Metacard.TITLE
                    + "']/value[text()='"
                    + RECORD_TITLE_1
                    + "']"),
            Matchers.hasXPath(
                "/metacards/metacard/string[@name='"
                    + Metacard.TITLE
                    + "']/value[text()='"
                    + RECORD_TITLE_2
                    + "']"));
  }

  @Test
  public void testCswQueryByTitle() {
    String geojsonId =
        ingest(
            getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"), "application/json");

    String titleQuery =
        getCswQuery("title", "myTitle", "application/xml", "http://www.opengis.net/cat/csw/2.0.2");

    given()
        .contentType(ContentType.XML)
        .body(titleQuery)
        .when()
        .post(CSW_PATH.getUrl())
        .then()
        .assertThat()
        .body(
            Matchers.hasXPath(
                "/GetRecordsResponse/SearchResults/Record/identifier", Matchers.is(geojsonId)),
            Matchers.hasXPath(
                "/GetRecordsResponse/SearchResults/@numberOfRecordsReturned", Matchers.is("1")));
  }

  private Response ingestCswRecord() {
    String uuid = UUID.randomUUID().toString().replaceAll("-", "");

    return given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(
            getCswInsertRequest(
                "csw:Record",
                getFileContent(
                    CSW_RECORD_RESOURCE_PATH + "/CswRecord", ImmutableMap.of("id", uuid))))
        .post(CSW_PATH.getUrl());
  }

  private void waitForSecurityHandlers(String url) {
    await("Waiting for security handlers to become available")
        .atMost(5, TimeUnit.MINUTES)
        .pollDelay(1, TimeUnit.SECONDS)
        .until(() -> get(url).statusCode() != 503);
  }

  private String ingestXmlWithProduct(String filename) throws IOException {
    Path path = Paths.get(filename);

    if (Files.exists(path)) {
      Files.delete(path);
    }

    Files.createFile(path);
    Files.write(path, Collections.singleton(SAMPLE_DATA));

    String fileLocation = path.toUri().toURL().toString();
    LOGGER.debug("File Location: {}", fileLocation);
    return ingest(getSimpleXml(fileLocation), "text/xml");
  }

  public static String getSimpleXml(String uri) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + getFileContent(
            XML_RECORD_RESOURCE_PATH + "/SimpleXmlNoDecMetacard", ImmutableMap.of("uri", uri));
  }

  private void setupOpenSearch() throws IOException {
    Map<String, Object> openSearchProperties =
        getOpenSearchSourceProperties(
            OPENSEARCH_SOURCE_ID, OPENSEARCH_PATH.getUrl(), getServiceManager());
    getServiceManager().createManagedService(OPENSEARCH_FACTORY_PID, openSearchProperties);
  }

  private void setupCswServer() throws IOException, InterruptedException {
    cswServer =
        new FederatedCswMockServer(
            CSW_STUB_SOURCE_ID, INSECURE_ROOT, Integer.parseInt(CSW_STUB_SERVER_PORT.getPort()));
    cswServer.start();

    Map<String, Object> cswStubServerProperties =
        getCswSourceProperties(CSW_STUB_SOURCE_ID, CSW_PATH.getUrl(), getServiceManager());
    cswStubServerProperties.put("cswUrl", CSW_STUB_SERVER_PATH.getUrl());
    cswStubServerProperties.put(POLL_INTERVAL, CSW_SOURCE_POLL_INTERVAL);
    getServiceManager()
        .createManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID, cswStubServerProperties)
        .getPid();

    getServiceManager().waitForHttpEndpoint(CSW_PATH + "?_wadl");

    Map<String, Object> cswProperties =
        getCswSourceProperties(CSW_SOURCE_ID, CSW_PATH.getUrl(), getServiceManager());

    cswProperties.put(POLL_INTERVAL, CSW_SOURCE_POLL_INTERVAL);
    getServiceManager()
        .createManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID, cswProperties)
        .getPid();

    Map<String, Object> cswProperties2 =
        getCswSourceProperties(
            CSW_SOURCE_WITH_METACARD_XML_ID, CSW_PATH.getUrl(), getServiceManager());
    cswProperties2.put("outputSchema", "urn:catalog:metacard");
    cswProperties2.put(POLL_INTERVAL, CSW_SOURCE_POLL_INTERVAL);
    getServiceManager()
        .createManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID, cswProperties2)
        .getPid();
  }

  private void setupGmd() throws IOException {
    Map<String, Object> gmdProperties =
        getCswSourceProperties(
            GMD_SOURCE_ID,
            GMD_CSW_FEDERATED_SOURCE_FACTORY_PID,
            CSW_PATH.getUrl(),
            getServiceManager());

    gmdProperties.put(POLL_INTERVAL, CSW_SOURCE_POLL_INTERVAL);
    getServiceManager()
        .createManagedService(GMD_CSW_FEDERATED_SOURCE_FACTORY_PID, gmdProperties)
        .getPid();
  }
}
