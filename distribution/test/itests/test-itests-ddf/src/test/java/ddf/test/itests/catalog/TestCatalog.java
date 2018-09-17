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
package ddf.test.itests.catalog;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static ddf.catalog.data.MetacardType.DEFAULT_METACARD_TYPE_NAME;
import static java.lang.String.format;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.SECURE_ROOT;
import static org.codice.ddf.itests.common.WaitCondition.expect;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.deleteMetacard;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestGeoJson;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.update;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureEnforceValidityErrorsAndWarnings;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureFilterInvalidMetacards;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureShowInvalidMetacards;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswFunctionQuery;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswInsertRequest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswQuery;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getMetacardIdFromCswInsertResponse;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearch;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jayway.restassured.path.xml.XmlPath;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import ddf.catalog.plugin.PostIngestPlugin;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPathExpressionException;
import org.apache.camel.CamelContext;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor;
import org.codice.ddf.catalog.plugin.metacard.backup.storage.filestorage.MetacardFileStorageRoute;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule.ConditionalIgnore;
import org.codice.ddf.itests.common.annotations.SkipUnstableTest;
import org.codice.ddf.itests.common.catalog.CatalogTestCommons;
import org.codice.ddf.itests.common.config.UrlResourceReaderConfigurator;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.PersistentStore.PersistenceType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.json.simple.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.w3c.dom.Node;

/** Tests the Catalog framework components. Includes helper methods at the Catalog level. */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestCatalog extends AbstractIntegrationTest {

  private static final String ADMIN = "admin";

  private static final String ADMIN_EMAIL = "admin@localhost.local";

  private static final String JSON_RECORD_POC = "admin@local";

  private static final String METACARD_X_PATH = "/metacards/metacard[@id='%s']";

  private static final String SAMPLE_DATA = "sample data";

  private static final String SAMPLE_IMAGE = "/9466484_b06f26d579_o.jpg";

  private static final String SAMPLE_MP4 = "sample.mp4";

  private static final String METACARD_BACKUP_PATH_TEMPLATE =
      "data/backup/metacard/{{substring id 0 3}}/{{substring id 3 6}}/{{id}}.xml";

  private static final String METACARD_BACKUP_FILE_STORAGE_FEATURE =
      "catalog-metacard-backup-filestorage";

  private static final String DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS = "data/products";

  private static final DynamicUrl SOLR_SCHEMA_PATH =
      new DynamicUrl(SECURE_ROOT, HTTPS_PORT, "/solr/catalog/schema");

  private static final String SOLR_CLIENT_PID = "ddf.catalog.source.solr.rest.SolrRest";

  @Rule public TestName testName = new TestName();

  @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

  private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

  public static String getGetRecordByIdProductRetrievalUrl() {
    return "?service=CSW&version=2.0.2&request=GetRecordById&NAMESPACE=xmlns="
        + "http://www.opengis.net/cat/csw/2.0.2&"
        + "outputFormat=application/octet-stream&outputSchema="
        + "http://www.iana.org/assignments/media-types/application/octet-stream&"
        + "id=placeholder_id";
  }

  public static String getSimpleXml(String uri) {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        + getFileContent(
            XML_RECORD_RESOURCE_PATH + "/SimpleXmlNoDecMetacard", ImmutableMap.of("uri", uri));
  }

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      waitForSystemReady();
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @Before
  public void setup() {
    urlResourceReaderConfigurator = getUrlResourceReaderConfigurator();
  }

  @After
  public void tearDown() throws IOException {
    urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
        new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS});
    clearCatalog();
  }

  @Test
  public void testCreateStorage() throws IOException {
    String fileName = testName.getMethodName() + ".jpg";
    File tmpFile =
        createTemporaryFile(fileName, IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));
    String id =
        given()
            .multiPart(tmpFile)
            .expect()
            .log()
            .headers()
            .statusCode(201)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    deleteMetacard(id);
  }

  @Test
  public void testReadStorage() throws IOException {
    String fileName = testName.getMethodName() + ".jpg";
    File tmpFile =
        createTemporaryFile(fileName, IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));
    String id =
        given()
            .multiPart(tmpFile)
            .expect()
            .log()
            .headers()
            .statusCode(201)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    final String url =
        CSW_PATH.getUrl() + getGetRecordByIdProductRetrievalUrl().replace("placeholder_id", id);

    given()
        .get(url)
        .then()
        .log()
        .headers()
        .assertThat()
        .statusCode(equalTo(200))
        .header(HttpHeaders.CONTENT_TYPE, Matchers.is("image/jpeg"));

    deleteMetacard(id);
  }

  @Test
  public void testReadDerivedStorage() throws IOException {
    String fileName = testName.getMethodName() + ".jpg";
    File tmpFile = createTemporaryFile(fileName, getFileContentAsStream(SAMPLE_IMAGE));
    String id =
        given()
            .multiPart(tmpFile)
            .expect()
            .log()
            .headers()
            .statusCode(201)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    final String url =
        REST_PATH.getUrl()
            + "sources/ddf.distribution/"
            + id
            + "?transform=resource&qualifier=preview";

    given()
        .get(url)
        .then()
        .log()
        .headers()
        .assertThat()
        .statusCode(equalTo(200))
        .header(HttpHeaders.CONTENT_TYPE, Matchers.is("image/jpeg"));

    deleteMetacard(id);
  }

  @Test
  public void testMetacardTransformersFromRest() {
    String id = ingestGeoJson(getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"));

    String url = REST_PATH.getUrl() + id;
    LOGGER.info("Getting response to {}", url);
    when().get(url).then().log().all().assertThat().body(hasXPath("/metacard[@id='" + id + "']"));

    deleteMetacard(id);
  }

  @Test
  public void testPointOfContactSetOnIngestWhenLoggedIn() {
    String id =
        given()
            .auth()
            .preemptive()
            .basic(ADMIN, ADMIN)
            .body(getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"))
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .expect()
            .log()
            .all()
            .statusCode(HttpStatus.SC_CREATED)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    String url = REST_PATH.getUrl() + id;
    LOGGER.info("Getting response to {}", url);
    when()
        .get(url)
        .then()
        .log()
        .all()
        .assertThat()
        .body(hasXPath("/metacard[@id='" + id + "']"))
        .body(
            hasXPath(
                "/metacard/string[@name='point-of-contact']/value[text()='"
                    + JSON_RECORD_POC
                    + "']"));

    deleteMetacard(id);
  }

  @Test
  public void testPointOfContactIsReadOnly() throws Exception {
    LOGGER.debug("Ingesting SimpleGeoJsonRecord");
    String id =
        given()
            .auth()
            .preemptive()
            .basic(ADMIN, ADMIN)
            .body(getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"))
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .expect()
            .log()
            .all()
            .statusCode(HttpStatus.SC_CREATED)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    LOGGER.debug("Updating SimpleGeoJsonRecord");

    given()
        .auth()
        .preemptive()
        .basic(ADMIN, ADMIN)
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .body(getFileContent(JSON_RECORD_RESOURCE_PATH + "/UpdatedSimpleGeoJsonRecord"))
        .expect()
        .log()
        .all()
        .statusCode(HttpStatus.SC_BAD_REQUEST)
        .when()
        .put(new DynamicUrl(REST_PATH, id).getUrl());

    deleteMetacard(id);
  }

  @Test
  public void testPointOfContactUpdatePlugin() throws Exception {
    StringWriter writer = new StringWriter();
    IOUtils.copy(IOUtils.toInputStream(getFileContent("/metacard1.xml")), writer);
    String id =
        given()
            .body(writer.toString())
            .auth()
            .preemptive()
            .basic(ADMIN, ADMIN)
            .header(HttpHeaders.CONTENT_TYPE, "text/xml")
            .expect()
            .log()
            .all()
            .statusCode(HttpStatus.SC_CREATED)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    given()
        .auth()
        .preemptive()
        .basic(ADMIN, ADMIN)
        .header(HttpHeaders.CONTENT_TYPE, "text/xml")
        .body(writer.toString())
        .expect()
        .log()
        .all()
        .statusCode(HttpStatus.SC_OK)
        .when()
        .put(new DynamicUrl(REST_PATH, id).getUrl());

    when()
        .get(REST_PATH.getUrl() + id)
        .then()
        .log()
        .all()
        .assertThat()
        .body(hasXPath("/metacard[@id='" + id + "']"))
        .body(
            hasXPath(
                "/metacard/string[@name='point-of-contact']/value[text()='" + ADMIN_EMAIL + "']"));

    deleteMetacard(id);
  }

  @Test
  public void testOpenSearchQuery() throws IOException {
    String id1 = ingestXmlFromResource("/metacard1.xml");
    String id2 = ingestXmlFromResource("/metacard2.xml");
    String id3 = ingestXmlFromResource("/metacard3.xml");
    String id4 = ingestXmlFromResource("/metacard4.xml");

    // Test xml-format response for an all-query
    ValidatableResponse response = getOpenSearch("xml", null, null, "q=*");
    response
        .body(hasXPath(format(METACARD_X_PATH, id1)))
        .body(hasXPath(format(METACARD_X_PATH, id2)))
        .body(hasXPath(format(METACARD_X_PATH, id3)))
        .body(hasXPath(format(METACARD_X_PATH, id4)));

    // Execute a text search against a value in an indexed field (metadata)
    response = getOpenSearch("xml", null, null, "q=dunder*");
    response
        .body(hasXPath(format(METACARD_X_PATH, id3)))
        .body(not(hasXPath(format(METACARD_X_PATH, id1))))
        .body(not(hasXPath(format(METACARD_X_PATH, id2))))
        .body(not(hasXPath(format(METACARD_X_PATH, id4))));

    // Execute a text search against a value that isn't in any indexed fields
    response = getOpenSearch("xml", null, null, "q=whatisthedealwithairlinefood");
    response.body("metacards.metacard.size()", equalTo(0));

    // Execute a geo search that should match a point card
    response = getOpenSearch("xml", null, null, "lat=40.689", "lon=-74.045", "radius=250");
    response
        .body(hasXPath(format(METACARD_X_PATH, id1)))
        .body(not(hasXPath(format(METACARD_X_PATH, id2))))
        .body(not(hasXPath(format(METACARD_X_PATH, id3))))
        .body(not(hasXPath(format(METACARD_X_PATH, id4))));

    // Execute a geo search...this should match two cards, both polygons around the Space Needle
    response = getOpenSearch("xml", null, null, "lat=47.62", "lon=-122.356", "radius=500");
    response
        .body(hasXPath(format(METACARD_X_PATH, id2)))
        .body(hasXPath(format(METACARD_X_PATH, id4)))
        .body(not(hasXPath(format(METACARD_X_PATH, id1))))
        .body(not(hasXPath(format(METACARD_X_PATH, id3))));

    deleteMetacard(id1);
    deleteMetacard(id2);
    deleteMetacard(id3);
    deleteMetacard(id4);
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

  private Response ingestXmlViaCsw() {
    return given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(
            getCswInsertRequest(
                "xml",
                getFileContent(
                    XML_RECORD_RESOURCE_PATH + "/SimpleXmlNoDecMetacard",
                    ImmutableMap.of("uri", "http://example.com"))))
        .post(CSW_PATH.getUrl());
  }

  private Response ingestXmlWithHeaderMetacard() {
    return given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(
            getCswInsertRequest(
                "xml",
                getFileContent(
                    XML_RECORD_RESOURCE_PATH + "/SimpleXmlMetacard",
                    ImmutableMap.of("uri", "http://example.com"))))
        .post(CSW_PATH.getUrl());
  }

  @Test
  public void testCswIngest() {
    Response response = ingestCswRecord();
    ValidatableResponse validatableResponse = response.then();

    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath(
            "//TransactionResponse/InsertResult/BriefRecord/title",
            is("Aliquam fermentum purus quis arcu")),
        hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));

    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Test
  public void testCswIngestWithMetadataBackup() throws Exception {
    getServiceManager().startFeature(true, METACARD_BACKUP_FILE_STORAGE_FEATURE);

    int startingPostIngestServices = 0;
    Collection<ServiceReference<PostIngestPlugin>> serviceRefs =
        getServiceManager().getServiceReferences(PostIngestPlugin.class, null);
    if (CollectionUtils.isNotEmpty(serviceRefs)) {
      startingPostIngestServices = serviceRefs.size();
    }

    Map<String, Object> storageProps = new HashMap<>();
    storageProps.put("outputPathTemplate", METACARD_BACKUP_PATH_TEMPLATE);
    storageProps.put("metacardTransformerId", "metadata");
    storageProps.put("keepDeletedMetacards", true);
    storageProps.put("backupInvalidMetacards", true);
    storageProps.put("backupMetacardTags", Arrays.asList("resource"));
    Configuration storageRouteConfiguration =
        getServiceManager().createManagedService("Metacard_File_Storage_Route", storageProps);

    expect("Service to be available: " + MetacardFileStorageRoute.class.getName())
        .within(30, TimeUnit.SECONDS)
        .checkEvery(5, TimeUnit.SECONDS)
        .until(
            () -> getServiceManager().getServiceReferences(PostIngestPlugin.class, null).size(),
            greaterThan(startingPostIngestServices));

    expect("Camel Context to be available")
        .within(30, TimeUnit.SECONDS)
        .checkEvery(5, TimeUnit.SECONDS)
        .until(
            () ->
                getServiceManager()
                    .getServiceReferences(
                        CamelContext.class, "(camel.context.name=metacardBackupCamelContext)"),
            not(empty()));

    BundleContext bundleContext = FrameworkUtil.getBundle(TestCatalog.class).getBundleContext();
    Collection<ServiceReference<CamelContext>> camelContextServiceRefs =
        getServiceManager()
            .getServiceReferences(
                CamelContext.class, "(camel.context.name=metacardBackupCamelContext)");
    CamelContext fileStorageRouteCamelContext = null;
    for (ServiceReference<CamelContext> camelContextServiceReference : camelContextServiceRefs) {
      fileStorageRouteCamelContext = bundleContext.getService(camelContextServiceReference);

      if (fileStorageRouteCamelContext != null) {
        break;
      }
    }

    final CamelContext camelContext = fileStorageRouteCamelContext;
    assertThat(camelContext, notNullValue());
    expect("Camel route definitions were not found")
        .within(30, TimeUnit.SECONDS)
        .checkEvery(5, TimeUnit.SECONDS)
        .until(() -> camelContext.getRouteDefinitions(), hasSize(2));

    camelContext.startAllRoutes();

    expect("Camel routes are started")
        .within(30, TimeUnit.SECONDS)
        .checkEvery(5, TimeUnit.SECONDS)
        .until(() -> camelContext.isStartingRoutes(), is(false));

    Response response = ingestCswRecord();
    ValidatableResponse validatableResponse = response.then();

    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath(
            "//TransactionResponse/InsertResult/BriefRecord/title",
            is("Aliquam fermentum purus quis arcu")),
        hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));
    verifyMetadataBackup();
    getServiceManager().stopManagedService(storageRouteConfiguration.getPid());
    getServiceManager().stopFeature(true, METACARD_BACKUP_FILE_STORAGE_FEATURE);
    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Test
  public void testCswXmlIngest() {
    Response response = ingestXmlViaCsw();
    ValidatableResponse validatableResponse = response.then();

    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath("//TransactionResponse/InsertResult/BriefRecord/title", is("myXmlTitle")),
        hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));

    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Test
  public void testCswXmlWithHeaderIngest() {
    Response response = ingestXmlWithHeaderMetacard();

    response
        .then()
        .body(
            hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
            hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
            hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
            hasXPath("//TransactionResponse/InsertResult/BriefRecord/title", is("myXmlTitle")),
            hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));

    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Test
  public void testCswUtmQuery() {
    Response response = ingestXmlViaCsw();
    response.then();

    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswQueryWithUtmIntersect"))
        .post(CSW_PATH.getUrl())
        .then()
        .assertThat()
        .statusCode(equalTo(200))
        .body(hasXPath("/GetRecordsResponse/SearchResults[@numberOfRecordsReturned]"), not("0"));

    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Test
  public void testCswCQLFunctionQuery() {
    String id = ingest(getFileContent("metacard5.xml"), "text/xml");
    try {

      given()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
          .get(
              CSW_PATH.getUrl()
                  + "?service=CSW&version=2.0.2&"
                  + "request=GetRecords&"
                  + "outputFormat=application/xml&"
                  + "outputSchema=http://www.opengis.net/cat/csw/2.0.2&"
                  + "NAMESPACE=xmlns(csw=http://www.opengis.net/cat/csw/2.0.2)&"
                  + "resultType=results&typeNames=csw:Record&"
                  + "ElementSetName=brief&ConstraintLanguage=CQL_TEXT&"
                  + "constraint=proximity(metadata,2,'All Hail Our SysAdmin')=true")
          .then()
          .assertThat()
          .statusCode(equalTo(200))
          .body(hasXPath("/GetRecordsResponse/SearchResults[@numberOfRecordsReturned]"), not("0"));
    } finally {
      deleteMetacard(id);
    }
  }

  @Test
  public void testCswFunctionQuery() {
    String id = ingest(getFileContent("metacard5.xml"), "text/xml");
    try {
      given()
          .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
          .body(
              getCswFunctionQuery(
                  "metadata",
                  true,
                  "application/xml",
                  "http://www.opengis.net/cat/csw/2.0.2",
                  "proximity",
                  2,
                  "All Hail Our SysAdmin"))
          .post(CSW_PATH.getUrl())
          .then()
          .assertThat()
          .statusCode(equalTo(200))
          .body(hasXPath("/GetRecordsResponse/SearchResults[@numberOfRecordsReturned]"), not("0"));
    } finally {
      deleteMetacard(id);
    }
  }

  @Test
  public void testCswDeleteOneWithFilter() {
    ingestCswRecord();

    ValidatableResponse response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswFilterDeleteRequest"))
            .post(CSW_PATH.getUrl())
            .then();
    response.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
  }

  @Test
  public void testCswDeleteOneWithCQL() {
    ingestCswRecord();

    ValidatableResponse response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(
                getFileContent(
                    CSW_REQUEST_RESOURCE_PATH + "/CswCqlDeleteRequest",
                    ImmutableMap.of("title", "Aliquam fermentum purus quis arcu")))
            .post(CSW_PATH.getUrl())
            .then();
    response.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
  }

  @Test
  public void testCswDeleteNone() {
    Response response = ingestCswRecord();

    ValidatableResponse validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(
                getFileContent(
                    CSW_REQUEST_RESOURCE_PATH + "/CswCqlDeleteRequest",
                    ImmutableMap.of("title", "fake title")))
            .post(CSW_PATH.getUrl())
            .then();
    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Test
  public void testCombinedCswIngestAndDelete() {
    // This record will be deleted with the <Delete> in the next transaction request.
    ingestCswRecord();

    // The record being inserted in this transaction request will be deleted at the end of the
    // test.
    Response response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswInsertAndDeleteRequest"))
            .post(CSW_PATH.getUrl());
    ValidatableResponse validatableResponse = response.then();
    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Test
  public void testCswDeleteMultiple() {
    ingestCswRecord();
    ingestCswRecord();

    ValidatableResponse response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswFilterDeleteRequest"))
            .post(CSW_PATH.getUrl())
            .then();
    response.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("2")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
  }

  @Test
  public void testCswUpdateByNewRecord() {
    Response response = ingestCswRecord();

    String requestXml = getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateRecordRequest");

    String id;

    try {
      id = getMetacardIdFromCswInsertResponse(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
      return;
    }

    requestXml = requestXml.replace("identifier placeholder", id);

    ValidatableResponse validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(requestXml)
            .post(CSW_PATH.getUrl())
            .then();
    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("1")));

    String url = REST_PATH.getUrl() + id;
    when()
        .get(url)
        .then()
        .log()
        .all()
        .assertThat()
        .body(
            hasXPath("//metacard/dateTime[@name='modified']/value", startsWith("2015-08-10")),
            hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
            hasXPath("//metacard/string[@name='topic.category']/value", is("Updated Subject")),
            hasXPath(
                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[1]",
                is("1.0 2.0")),
            hasXPath(
                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[2]",
                is("3.0 2.0")),
            hasXPath(
                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[3]",
                is("3.0 4.0")),
            hasXPath(
                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[4]",
                is("1.0 4.0")),
            hasXPath(
                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[5]",
                is("1.0 2.0")));

    deleteMetacard(id);
  }

  @Test
  public void testCswUpdateByNewRecordNoExistingMetacards() {
    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateRecordRequest"))
        .post(CSW_PATH.getUrl())
        .then()
        .assertThat()
        .statusCode(400);
  }

  @Test
  public void testCswUpdateByNewRecordNoMetacardFound()
      throws IOException, XPathExpressionException {
    Response response = ingestCswRecord();
    try {
      ValidatableResponse validatableResponse =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateRecordRequest"))
              .post(CSW_PATH.getUrl())
              .then();
      validatableResponse.assertThat().statusCode(400);
    } finally {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    }
  }

  @Test
  public void testCswUpdateByFilterConstraint() {
    Response firstResponse = ingestCswRecord();
    Response secondResponse = ingestCswRecord();

    ValidatableResponse validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateByFilterConstraintRequest"))
            .post(CSW_PATH.getUrl())
            .then();
    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("2")));

    String firstId;
    String secondId;

    try {
      firstId = getMetacardIdFromCswInsertResponse(firstResponse);
      secondId = getMetacardIdFromCswInsertResponse(secondResponse);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
      return;
    }

    String firstUrl = REST_PATH.getUrl() + firstId;
    when()
        .get(firstUrl)
        .then()
        .log()
        .all()
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
        .all()
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

    deleteMetacard(firstId);
    deleteMetacard(secondId);
  }

  @Test
  public void testCswUpdateAllRecordsByFilterConstraint() {
    int numRecords = 25;
    for (int i = 0; i < numRecords; i++) {
      ingestCswRecord();
    }

    ValidatableResponse response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateAllByFilterRequest"))
            .post(CSW_PATH.getUrl())
            .then();

    response.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath(
            "//TransactionResponse/TransactionSummary/totalUpdated",
            is(Integer.toString(numRecords))));
  }

  @Test
  public void testCswUpdateByFilterConstraintNoExistingMetacards() {
    ValidatableResponse response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateByFilterConstraintRequest"))
            .post(CSW_PATH.getUrl())
            .then();

    response.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
  }

  @Test
  public void testCswUpdateByFilterConstraintNoMetacardsFound() {
    Response response = ingestCswRecord();

    String updateRequest =
        getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateByFilterConstraintRequest");

    // Change the <Filter> property being searched for so no results will be found.
    updateRequest = updateRequest.replace("title", "subject");

    ValidatableResponse validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(updateRequest)
            .post(CSW_PATH.getUrl())
            .then();
    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

    try {
      CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
    }
  }

  @Test
  public void testCswGetRecordsWithHitsResultType() {

    Response response = ingestCswRecord();

    String query =
        getCswQuery("AnyText", "*", "application/xml", "http://www.opengis.net/cat/csw/2.0.2");

    String id;

    try {
      id = getMetacardIdFromCswInsertResponse(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
      return;
    }

    // test with resultType="results" first
    ValidatableResponse validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    validatableResponse.body(hasXPath("/GetRecordsResponse/SearchResults/Record"));

    // test with resultType="hits"
    query = query.replace("results", "hits");
    validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();
    // assert that no records have been returned
    validatableResponse.body(not(hasXPath("//Record")));

    // testing with resultType='validate' is not
    // possible due to DDF-1537, this test will need
    // to be updated to test this once it is fixed.

    deleteMetacard(id);
  }

  @Test
  public void testCswUpdateRemoveAttributesByCqlConstraint() {
    Response response = ingestCswRecord();

    String id;

    try {
      id = getMetacardIdFromCswInsertResponse(response);
    } catch (IOException | XPathExpressionException e) {
      fail("Could not retrieve the ingested record's ID from the response.");
      return;
    }

    String url = REST_PATH.getUrl() + id;
    when()
        .get(url)
        .then()
        .log()
        .all()
        .assertThat()
        // Check that the attributes about to be removed in the update are present.
        .body(
            hasXPath("//metacard/dateTime[@name='modified']"),
            hasXPath("//metacard/string[@name='title']"),
            hasXPath("//metacard/geometry[@name='location']"));

    ValidatableResponse validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(
                getFileContent(
                    CSW_REQUEST_RESOURCE_PATH + "/CswUpdateRemoveAttributesByCqlConstraintRequest"))
            .post(CSW_PATH.getUrl())
            .then();
    validatableResponse.body(
        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("1")));

    when()
        .get(url)
        .then()
        .log()
        .all()
        .assertThat()
        // Check that the updated attributes were removed.
        .body(
            not(hasXPath("//metacard/string[@name='title']")),
            not(hasXPath("//metacard/geometry[@name='location']")),
            // Check that an attribute that was not updated was not changed.
            hasXPath("//metacard/dateTime[@name='modified']"),
            hasXPath(
                "//metacard/string[@name='topic.category']/value",
                is("Hydrography--Dictionaries")));

    deleteMetacard(id);
  }

  @Test
  public void testCswNumericalQuery() throws IOException {
    // ingest test record
    String id = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/testNumerical.xml");

    // query for it by the numerical query
    String numericalQuery =
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
            + "<GetRecords resultType=\"results\"\n"
            + "            outputFormat=\"application/xml\"\n"
            + "            outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
            + "            startPosition=\"1\"\n"
            + "            maxRecords=\"10\"\n"
            + "            service=\"CSW\"\n"
            + "            version=\"2.0.2\"\n"
            + "            xmlns=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
            + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
            + "            xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
            + "  <Query typeNames=\"csw:Record\">\n"
            + "    <ElementSetName>full</ElementSetName>\n"
            + "    <Constraint version=\"1.1.0\">\n"
            + "      <ogc:Filter>\n"
            + "        <ogc:PropertyIsEqualTo wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\" matchCase=\"true\">\n"
            + "          <ogc:PropertyName>media.width-pixels</ogc:PropertyName>\n"
            + "          <ogc:Literal>12</ogc:Literal>\n"
            + "        </ogc:PropertyIsEqualTo>\n"
            + "      </ogc:Filter>\n"
            + "    </Constraint>\n"
            + "  </Query>\n"
            + "</GetRecords>\n";

    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(numericalQuery)
        .post(CSW_PATH.getUrl())
        .then()
        .assertThat()
        .statusCode(equalTo(200))
        .body(hasXPath("/GetRecordsResponse/SearchResults[@numberOfRecordsReturned='1']"));

    deleteMetacard(id);
  }

  @Test
  public void testGetRecordById() throws IOException, XPathExpressionException {
    final Response firstResponse = ingestCswRecord();
    final Response secondResponse = ingestCswRecord();

    final String firstId = getMetacardIdFromCswInsertResponse(firstResponse);
    final String secondId = getMetacardIdFromCswInsertResponse(secondResponse);
    final String requestIds = firstId + "," + secondId;

    String cswUrlGetRecordsParmaters =
        "?service=CSW&version=2.0.2&request=GetRecordById&NAMESPACE=xmlns="
            + "http://www.opengis.net/cat/csw/2.0.2&ElementSetName=full&"
            + "outputFormat=application/xml&outputSchema=http://www.opengis.net/cat/csw/2.0.2&"
            + "id=placeholder_id";

    // Request the records we just added.
    final String url =
        CSW_PATH.getUrl() + cswUrlGetRecordsParmaters.replace("placeholder_id", requestIds);

    final ValidatableResponse response = when().get(url).then().log().all();

    verifyGetRecordByIdResponse(response, firstId, secondId);

    deleteMetacard(firstId);
    deleteMetacard(secondId);
  }

  @Test
  public void testPostGetRecordById() throws IOException, XPathExpressionException {
    final Response firstResponse = ingestCswRecord();
    final Response secondResponse = ingestCswRecord();

    final String firstId = getMetacardIdFromCswInsertResponse(firstResponse);
    final String secondId = getMetacardIdFromCswInsertResponse(secondResponse);

    final String requestXml =
        getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswByIdsQuery")
            .replace("placeholder_id_1", firstId)
            .replace("placeholder_id_2", secondId);

    final ValidatableResponse response =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(requestXml)
            .post(CSW_PATH.getUrl())
            .then();

    verifyGetRecordByIdResponse(response, firstId, secondId);

    deleteMetacard(firstId);
    deleteMetacard(secondId);
  }

  @Test
  public void testGetRecordByIdProductRetrieval() throws IOException, XPathExpressionException {
    String fileName = testName.getMethodName() + ".txt";
    String metacardId = ingestXmlWithProduct(fileName);
    final String url =
        CSW_PATH.getUrl()
            + getGetRecordByIdProductRetrievalUrl().replace("placeholder_id", metacardId);

    String productDirectory = new File(fileName).getAbsoluteFile().getParent();
    urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
        new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

    given().get(url).then().log().all().assertThat().statusCode(equalTo(200)).body(is(SAMPLE_DATA));

    deleteMetacard(metacardId);
  }

  @Test
  public void testUpdateContentResourceUri() throws IOException {
    String fileName = testName.getMethodName() + ".txt";
    String metacardId = ingestXmlWithProduct(fileName);
    update(metacardId, getSimpleXml("content:" + metacardId), "text/xml");
    given()
        .header(HttpHeaders.CONTENT_TYPE, "text/xml")
        .body(getSimpleXml("foo:bar"))
        .expect()
        .log()
        .all()
        .statusCode(HttpStatus.SC_BAD_REQUEST)
        .when()
        .put(new DynamicUrl(REST_PATH, metacardId).getUrl());
  }

  @Test
  public void testCachedContentLengthHeader() throws IOException {

    String fileName = "testCachedContentLengthHeader" + ".jpg";
    File tmpFile =
        createTemporaryFile(fileName, IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));

    String id =
        given()
            .multiPart(tmpFile)
            .expect()
            .log()
            .headers()
            .statusCode(201)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    final String url =
        REST_PATH.getUrl() + "sources/ddf.distribution/" + id + "?transform=resource";

    LOGGER.error("URL: " + url);

    // Get the product once
    get(url).then().log().headers();

    // Get again to hit the cache
    get(url + "&mode=cache")
        .then()
        .log()
        .headers()
        .assertThat()
        .header(HttpHeaders.CONTENT_LENGTH, notNullValue());

    deleteMetacard(id);
  }

  @Test
  public void testPostGetRecordByIdProductRetrieval() throws IOException, XPathExpressionException {
    String fileName = testName.getMethodName() + ".txt";
    String metacardId = ingestXmlWithProduct(fileName);

    String productDirectory = new File(fileName).getAbsoluteFile().getParent();
    urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
        new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

    final String requestXml =
        getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswByIdQuery")
            .replace("placeholder_id_1", metacardId);

    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)
        .body(requestXml)
        .post(CSW_PATH.getUrl())
        .then()
        .log()
        .all()
        .assertThat()
        .statusCode(equalTo(200))
        .body(is(SAMPLE_DATA));

    deleteMetacard(metacardId);
  }

  @Test
  public void testPostGetRecordByIdProductRetrievalWithRange()
      throws IOException, XPathExpressionException {
    String fileName = testName.getMethodName() + ".txt";
    String metacardId = ingestXmlWithProduct(fileName);

    String productDirectory = new File(fileName).getAbsoluteFile().getParent();
    urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
        new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

    final String requestXml =
        getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswByIdQuery")
            .replace("placeholder_id_1", metacardId);

    int offset = 4;
    byte[] sampleDataByteArray = SAMPLE_DATA.getBytes();
    String partialSampleData =
        new String(Arrays.copyOfRange(sampleDataByteArray, offset, sampleDataByteArray.length));

    // @formatter:off
    given()
        .headers(
            HttpHeaders.CONTENT_TYPE,
            MediaType.TEXT_XML,
            CswConstants.RANGE_HEADER,
            format("bytes=%s-", offset))
        .body(requestXml)
        .post(CSW_PATH.getUrl())
        .then()
        .log()
        .all()
        .assertThat()
        .statusCode(equalTo(200))
        .assertThat()
        .header(CswConstants.ACCEPT_RANGES_HEADER, is(equalTo(CswConstants.BYTES)))
        .body(is(partialSampleData));
    // @formatter:on

    deleteMetacard(metacardId);
  }

  @Test
  public void testPostGetRecordByIdProductRetrievalWithInvalidRange()
      throws IOException, XPathExpressionException {
    String fileName = testName.getMethodName() + ".txt";
    String metacardId = ingestXmlWithProduct(fileName);

    String productDirectory = new File(fileName).getAbsoluteFile().getParent();
    urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(
        new String[] {DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

    final String requestXml =
        getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswByIdQuery")
            .replace("placeholder_id_1", metacardId);

    String invalidRange = "100";

    // @formatter:off
    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)
        .header(CswConstants.RANGE_HEADER, invalidRange)
        .body(requestXml)
        .post(CSW_PATH.getUrl())
        .then()
        .log()
        .all()
        .assertThat()
        .statusCode(equalTo(400));
    // @formatter:on

    deleteMetacard(metacardId);
  }

  private void verifyGetRecordByIdResponse(
      final ValidatableResponse response, final String... ids) {
    final String xPathGetRecordWithId = "//GetRecordByIdResponse/Record[identifier=\"%s\"]";
    final String xPathValidateTitleWithId =
        xPathGetRecordWithId + "/title[text()=\"Aliquam fermentum purus quis arcu\"]";
    final String xPathValidateBboxLowerWithId =
        xPathGetRecordWithId + "/BoundingBox/LowerCorner[text()=\"44.792 -6.171\"]";
    final String xPathValidateBboxUpperWithId =
        xPathGetRecordWithId + "/BoundingBox/UpperCorner[text()=\"51.126 -2.228\"]";

    final String xPathValidateId = "//GetRecordByIdResponse/Record/identifier[text()=\"%s\"]";

    final String xPathCountRecords = "count(//GetRecordByIdResponse/Record)";

    response.body(hasXPath(xPathCountRecords, is(String.valueOf(ids.length))));

    for (String id : ids) {
      // Check that the IDs of the returned records are the IDs we requested.
      response
          .body(hasXPath(format(xPathValidateId, id)))
          // Check the contents of the returned records.
          .body(hasXPath(format(xPathValidateTitleWithId, id)))
          .body(hasXPath(format(xPathValidateBboxLowerWithId, id)))
          .body(hasXPath(format(xPathValidateBboxUpperWithId, id)));
    }
  }

  @Test
  public void testFilterPlugin() throws Exception {
    // Ingest the metacard
    String id1 = ingestXmlFromResource("/metacard1.xml");
    String xPath = format(METACARD_X_PATH, id1);

    // Test without filtering
    ValidatableResponse response = getOpenSearch("xml", null, null, "q=*");
    response.body(hasXPath(xPath));

    getServiceManager().startFeature(true, "sample-filter");

    try {
      // Configure the PDP
      PdpProperties pdpProperties = new PdpProperties();
      pdpProperties.put(
          "matchAllMappings",
          Arrays.asList(
              "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=point-of-contact"));
      Configuration config =
          configAdmin.getConfiguration("ddf.security.pdp.realm.AuthzRealm", null);
      Dictionary<String, ?> configProps = new Hashtable<>(pdpProperties);
      config.update(configProps);
      getServiceManager().waitForAllBundles();

      // Test with filtering with out point-of-contact
      response = getOpenSearch("xml", null, null, "q=*");
      response.body(not(hasXPath(xPath)));

      response = getOpenSearch("xml", "admin", "admin", "q=*");

      response.body(hasXPath(xPath));

    } finally {
      Configuration config =
          configAdmin.getConfiguration("ddf.security.pdp.realm.AuthzRealm", null);
      Dictionary<String, ?> configProps = new Hashtable<>(new PdpProperties());
      config.update(configProps);
      deleteMetacard(id1);
      getServiceManager().stopFeature(true, "sample-filter");
    }
  }

  @Test
  public void testIngestPlugin() throws Exception {

    // ingest a data set to make sure we don't have any issues initially
    String id1 = ingestGeoJson(getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"));
    String xPath1 = format(METACARD_X_PATH, id1);

    // verify ingest by querying
    ValidatableResponse response = getOpenSearch("xml", null, null, "q=*");
    response.body(hasXPath(xPath1));

    // change ingest plugin role to ingest
    CatalogPolicyProperties catalogPolicyProperties = new CatalogPolicyProperties();
    catalogPolicyProperties.put(
        "createPermissions",
        new String[] {"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=ingest"});
    Configuration config =
        configAdmin.getConfiguration("org.codice.ddf.catalog.security.CatalogPolicy", null);
    Dictionary<String, Object> configProps = new Hashtable<>(catalogPolicyProperties);
    config.update(configProps);
    getServiceManager().waitForAllBundles();

    // try ingesting again - it should fail this time
    given()
        .body(getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"))
        .header(HttpHeaders.CONTENT_TYPE, "application/json")
        .expect()
        .log()
        .all()
        .statusCode(400)
        .when()
        .post(REST_PATH.getUrl());

    // verify query for first id works
    response = getOpenSearch("xml", null, null, "q=*");
    response.body(hasXPath(xPath1));

    // revert to original configuration
    configProps.put(
        "createPermissions",
        new String[] {"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=guest"});
    config.update(configProps);
    getServiceManager().waitForAllBundles();

    deleteMetacard(id1);
  }

  @Test
  @Ignore("Until DDF-4117 is addressed")
  public void testMetacardIngestNetworkPlugin() throws Exception {

    /* MATCHERS
    Used for validation */
    final Matcher<Node> hasInitialDescription =
        hasMetacardAttributeXPath("description", "Lombardi");
    final Matcher<Node> hasInitialDerivedUrls =
        hasMetacardAttributeXPath(
            "resource.derived-download-url",
            "http://example.com/1",
            "http://example.com/2",
            "http://example.com/3");
    final Matcher<Node> hasNewDescription = hasMetacardAttributeXPath("description", "None");
    final Matcher<Node> hasNewDerivedUrls =
        hasMetacardAttributeXPath(
            "resource.derived-download-url", "http://example.com/4", "http://example.com/5");
    final Matcher<Node> hasNewSecurityAccessGroups =
        hasMetacardAttributeXPath("security.access-groups", "FIFA", "NFL");

    /* CONFIG
    Setup the services that will conditionally add attributes
    clientIP rule currently broken - not getting what we expect */
    final String factoryPid = "org.codice.ddf.catalog.plugin.metacard.MetacardIngestNetworkPlugin";
    final String bundleSymbolicName = "catalog-plugin-metacardingest-network";
    final String clientIP = "127.0.0.1";
    final String clientScheme = "https";

    Map<String, Object> networkRule1Properties = new HashMap<>();
    Map<String, Object> networkRule2Properties = new HashMap<>();

    List<Object> attributeAdjustmentsForRule1 =
        ImmutableList.of("security.access-groups = FIFA, NFL");

    List<Object> attributeAdjustmentsForRule2 =
        ImmutableList.of(
            "description = None",
            "resource.derived-download-url = http://example.com/4, http://example.com/5");

    networkRule1Properties.put("criteriaKey", "remoteAddr");
    networkRule1Properties.put("expectedValue", clientIP);
    networkRule1Properties.put("newAttributes", attributeAdjustmentsForRule1);

    networkRule2Properties.put("criteriaKey", "scheme");
    networkRule2Properties.put("expectedValue", clientScheme);
    networkRule2Properties.put("newAttributes", attributeAdjustmentsForRule2);

    Configuration managedService = null;
    Configuration managedService1 = null;
    try {

      /* START SERVICES
      Add instances of the rules */
      managedService = getServiceManager().createManagedService(factoryPid, networkRule1Properties);
      managedService1 =
          getServiceManager().createManagedService(factoryPid, networkRule2Properties);
      getServiceManager().waitForRequiredBundles(bundleSymbolicName);

      /* INGEST
      Working with two metacard xml files and one txt file to serve as a tika product */
      String simpleProductId = ingestMetacardAndGetId("simple-product.txt", MediaType.TEXT_PLAIN);
      String card1Id = ingestMetacardAndGetId("modified-metacard-1.xml", MediaType.TEXT_XML);
      String card2Id = ingestMetacardAndGetId("modified-metacard-2.xml", MediaType.TEXT_XML);

      /* VALIDATION
      Note only the first validation gets security since it's parsed by tika */
      getMetacardValidatableResponse(simpleProductId)
          .assertThat()
          .body(hasNewDescription)
          .body(hasNewDerivedUrls)
          .body(hasNewSecurityAccessGroups);
      getMetacardValidatableResponse(card1Id)
          .assertThat()
          .body(hasInitialDescription)
          .body(hasNewDerivedUrls);
      getMetacardValidatableResponse(card2Id)
          .assertThat()
          .body(hasNewDescription)
          .body(hasInitialDerivedUrls);

    } finally {

      /* CLEAN UP
      Don't let the conditions spill-over and impact other tests */
      if (managedService != null) {
        getServiceManager().stopManagedService(managedService.getFactoryPid());
      }
      if (managedService1 != null) {
        getServiceManager().stopManagedService(managedService1.getFactoryPid());
      }
    }
  }

  private ValidatableResponse getMetacardValidatableResponse(String metacardId) {
    return getMetacardValidatableResponse("ddf.distribution", metacardId);
  }

  private ValidatableResponse getMetacardValidatableResponse(String source, String metacardId) {
    final String metacardUrlFormat = SERVICE_ROOT.toString() + "/catalog/sources/%s/%s";
    return given()
        .expect()
        .log()
        .all()
        .statusCode(HttpStatus.SC_OK)
        .when()
        .get(URI.create(format(metacardUrlFormat, source, metacardId)))
        .then();
  }

  private String ingestMetacardAndGetId(String fileName, String contentType) {
    return given()
        .body(getFileContent(fileName))
        .header(HttpHeaders.CONTENT_TYPE, contentType)
        .expect()
        .log()
        .all()
        .statusCode(HttpStatus.SC_CREATED)
        .when()
        .post(REST_PATH.getUrl())
        .getHeader("id");
  }

  private Matcher<Node> hasMetacardAttributeXPath(String attributeName, String... attributeValues) {
    final String basePathFormat = "/metacard/string[@name=\"%s\"][value=\"%s\"";
    final String additionalValueFormat = " and value=\"%s\"";

    StringBuilder xPathBuilder =
        new StringBuilder(format(basePathFormat, attributeName, attributeValues[0]));
    for (int i = 1; i < attributeValues.length; i++) {
      xPathBuilder.append(format(additionalValueFormat, attributeValues[i]));
    }
    xPathBuilder.append("]");

    return hasXPath(xPathBuilder.toString());
  }

  @Test
  public void testVideoThumbnail() throws Exception {

    try (InputStream inputStream = IOUtils.toInputStream(getFileContent("sample.mp4"))) {
      final byte[] fileBytes = IOUtils.toByteArray(inputStream);

      given()
          .multiPart("file", SAMPLE_MP4, fileBytes, "video/mp4")
          .post(REST_PATH.getUrl())
          .then()
          .statusCode(201);
    }
  }

  @Test
  public void testContentDirectoryMonitor() throws Exception {
    final String TMP_PREFIX = "tcdm_";
    Path tmpDir = Files.createTempDirectory(TMP_PREFIX);
    tmpDir.toFile().deleteOnExit();
    Path tmpFile = Files.createTempFile(tmpDir, TMP_PREFIX, "_tmp.xml");
    tmpFile.toFile().deleteOnExit();
    Files.copy(
        getFileContentAsStream("metacard5.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    Map<String, Object> cdmProperties = new HashMap<>();
    cdmProperties.putAll(
        getServiceManager()
            .getMetatypeDefaults(
                "content-core-directorymonitor",
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor"));
    cdmProperties.put("monitoredDirectoryPath", tmpDir.toString() + "/");
    cdmProperties.put("processingMechanism", "delete");
    Configuration managedService =
        getServiceManager()
            .createManagedService(
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor", cdmProperties);

    assertIngestedDirectoryMonitor("SysAdmin", 1);

    getServiceManager().stopManagedService(managedService.getPid());

    given()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
        .body(
            getFileContent(
                CSW_REQUEST_RESOURCE_PATH + "/CswCqlDeleteRequest",
                ImmutableMap.of("title", "Metacard-5")))
        .post(CSW_PATH.getUrl())
        .then()
        .body(
            hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
            hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
            hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
  }

  @Test
  public void testAttributeOverridesOnUpdate() throws Exception {
    final String TMP_PREFIX = "tcdm_";
    final String attribute = "title";
    final String value = "someTitleIMadeUp";

    Path tmpDir = Files.createTempDirectory(TMP_PREFIX);
    tmpDir.toFile().deleteOnExit();
    Path tmpFile = Files.createTempFile(tmpDir, TMP_PREFIX, "_tmp.xml");
    tmpFile.toFile().deleteOnExit();
    Files.copy(
        getFileContentAsStream("metacard5.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    Map<String, Object> cdmProperties = new HashMap<>();
    cdmProperties.putAll(
        getServiceManager()
            .getMetatypeDefaults(
                "content-core-directorymonitor",
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor"));
    cdmProperties.put("monitoredDirectoryPath", tmpDir.toString() + "/");
    cdmProperties.put("processingMechanism", ContentDirectoryMonitor.IN_PLACE);
    cdmProperties.put("attributeOverrides", format("%s=%s", attribute, value));
    Configuration managedService =
        getServiceManager()
            .createManagedService(
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor", cdmProperties);

    // assert that the file was ingested
    ValidatableResponse response = assertIngestedDirectoryMonitor("SysAdmin", 1);

    // assert that the metacard contains the overridden attribute
    assertStringMetacardAttribute(response, attribute, value);

    // edit the file
    Files.copy(
        getFileContentAsStream("metacard4.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    // assert updated
    response = assertIngestedDirectoryMonitor("Space", 1);

    // assert that the metacard still contains the overridden attribute
    assertStringMetacardAttribute(response, attribute, value);

    // delete the file
    tmpFile.toFile().delete();

    // assert deleted
    assertIngestedDirectoryMonitor("SysAdmin", 0);

    getServiceManager().stopManagedService(managedService.getPid());
  }

  private ValidatableResponse assertStringMetacardAttribute(
      ValidatableResponse response, String attribute, String value) {
    String attributeValueXpath = format("/metacards/metacard/string[@name='%s']/value", attribute);
    response.body(hasXPath(attributeValueXpath, is(value)));
    return response;
  }

  @Test
  public void testInPlaceDirectoryMonitor() throws Exception {
    final String TMP_PREFIX = "tcdm_";
    Path tmpDir = Files.createTempDirectory(TMP_PREFIX);
    tmpDir.toFile().deleteOnExit();
    Path tmpFile = Files.createTempFile(tmpDir, TMP_PREFIX, "_tmp.xml");
    tmpFile.toFile().deleteOnExit();
    Files.copy(
        getFileContentAsStream("metacard5.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    Map<String, Object> cdmProperties = new HashMap<>();
    cdmProperties.putAll(
        getServiceManager()
            .getMetatypeDefaults(
                "content-core-directorymonitor",
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor"));
    cdmProperties.put("monitoredDirectoryPath", tmpDir.toString() + "/");
    cdmProperties.put("processingMechanism", ContentDirectoryMonitor.IN_PLACE);
    Configuration managedService =
        getServiceManager()
            .createManagedService(
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor", cdmProperties);

    // assert that the file was ingested
    assertIngestedDirectoryMonitor("SysAdmin", 1);

    // edit the file
    Files.copy(
        getFileContentAsStream("metacard4.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    // assert updated
    assertIngestedDirectoryMonitor("Space", 1);

    // rename the file and change
    Path newPath = Paths.get(tmpFile.toAbsolutePath().toString().replace("tmp.xml", "tmp2.xml"));
    Files.move(tmpFile, newPath, StandardCopyOption.REPLACE_EXISTING);
    tmpFile = newPath;
    Files.copy(
        getFileContentAsStream("metacard5.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    // assert renamed
    assertIngestedDirectoryMonitor("SysAdmin", 1);

    // delete the file
    tmpFile.toFile().delete();

    // assert deleted
    assertIngestedDirectoryMonitor("SysAdmin", 0);

    getServiceManager().stopManagedService(managedService.getPid());
  }

  @Test
  public void testInPlaceDirectoryMonitorPersistence() throws Exception {
    final String TMP_PREFIX = "tcdm_";
    Path tmpDir = Files.createTempDirectory(TMP_PREFIX);
    tmpDir.toFile().deleteOnExit();
    Path tmpFile = Files.createTempFile(tmpDir, TMP_PREFIX, "_tmp.xml");
    tmpFile.toFile().deleteOnExit();
    Files.copy(
        getFileContentAsStream("metacard5.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    Map<String, Object> cdmProperties = new HashMap<>();
    cdmProperties.putAll(
        getServiceManager()
            .getMetatypeDefaults(
                "content-core-directorymonitor",
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor"));
    cdmProperties.put("monitoredDirectoryPath", tmpDir.toString() + "/");
    cdmProperties.put("processingMechanism", ContentDirectoryMonitor.IN_PLACE);
    Configuration managedService =
        getServiceManager()
            .createManagedService(
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor", cdmProperties);

    // assert that the file was ingested
    assertIngestedDirectoryMonitor("SysAdmin", 1);

    getServiceManager()
        .stopBundle("org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor");
    getServiceManager()
        .startBundle("org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor");

    // edit the file
    Files.copy(
        getFileContentAsStream("metacard4.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    // assert updated
    assertIngestedDirectoryMonitor("Space", 1);

    getServiceManager()
        .stopBundle("org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor");
    getServiceManager()
        .startBundle("org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor");

    // rename the file and change
    Path newPath = Paths.get(tmpFile.toAbsolutePath().toString().replace("tmp.xml", "tmp2.xml"));
    Files.move(tmpFile, newPath, StandardCopyOption.REPLACE_EXISTING);
    tmpFile = newPath;
    Files.copy(
        getFileContentAsStream("metacard5.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    getServiceManager()
        .stopBundle("org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor");
    getServiceManager()
        .startBundle("org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor");

    // assert renamed
    assertIngestedDirectoryMonitor("SysAdmin", 1);

    // delete the file
    tmpFile.toFile().delete();

    // assert deleted
    assertIngestedDirectoryMonitor("SysAdmin", 0);

    getServiceManager().stopManagedService(managedService.getPid());
  }

  private ValidatableResponse assertIngestedDirectoryMonitor(String query, int numResults) {
    long startTime = System.nanoTime();
    ValidatableResponse response;
    do {
      response = getOpenSearch("xml", null, null, "q=*" + query + "*");
      if (response.extract().xmlPath().getList("metacards.metacard").size() == numResults) {
        break;
      }
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException e) {
      }
    } while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
        < TimeUnit.MINUTES.toMillis(3));
    response.body("metacards.metacard.size()", equalTo(numResults));
    return response;
  }

  @Test
  public void testIngestXmlNoExtension() throws Exception {
    final String TMP_PREFIX = "tcdm_";
    Path tmpDir = Files.createTempDirectory(TMP_PREFIX);
    tmpDir.toFile().deleteOnExit();
    Path tmpFile = Files.createTempFile(tmpDir, TMP_PREFIX, "_tmp");
    tmpFile.toFile().deleteOnExit();
    Files.copy(
        getFileContentAsStream("metacard5.xml"), tmpFile, StandardCopyOption.REPLACE_EXISTING);

    Map<String, Object> cdmProperties = new HashMap<>();
    cdmProperties.putAll(
        getServiceManager()
            .getMetatypeDefaults(
                "content-core-directorymonitor",
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor"));
    cdmProperties.put("monitoredDirectoryPath", tmpDir.toString() + "/");
    cdmProperties.put("processingMechanism", ContentDirectoryMonitor.IN_PLACE);
    Configuration managedService =
        getServiceManager()
            .createManagedService(
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor", cdmProperties);

    ValidatableResponse response = assertIngestedDirectoryMonitor("SysAdmin", 1);
    response.extract().xmlPath().getString("metacards.metacard.type").equals("ddf.metacard");

    getServiceManager().stopManagedService(managedService.getPid());
  }

  @Test
  public void persistObjectToWorkspace() throws Exception {
    persistToWorkspace(100);
  }

  private File copyFileToDefinitionsDir(String filename) throws IOException {
    Path definitionsDirPath = Paths.get(System.getProperty(DDF_HOME_PROPERTY), "etc/definitions");
    definitionsDirPath = Files.createDirectories(definitionsDirPath);
    definitionsDirPath.toFile().deleteOnExit();

    Path tmpFile = definitionsDirPath.resolve(filename);
    tmpFile.toFile().deleteOnExit();
    Files.copy(IOUtils.toInputStream(getFileContent(filename)), tmpFile);
    return tmpFile.toFile();
  }

  private File ingestDefinitionJsonWithWaitCondition(String filename, Callable<Void> waitCondition)
      throws Exception {
    File definitionFile = copyFileToDefinitionsDir(filename);
    waitCondition.call();
    return definitionFile;
  }

  private void uninstallDefinitionJson(File definitionFile, Callable<Void> waitCondition)
      throws Exception {
    boolean success = definitionFile.delete();
    if (!success) {
      throw new Exception("Could not delete file(" + definitionFile.getAbsolutePath() + ")");
    }
    waitCondition.call();
  }

  @Test
  @ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-2743
  public void testMetacardDefinitionJsonFile() throws Exception {
    final String newMetacardTypeName = "new.metacard.type";
    String id = null;
    File file = null;

    try {
      getServiceManager().stopFeature(true, "catalog-security-filter");
      file =
          ingestDefinitionJsonWithWaitCondition(
              "definitions.json",
              () -> {
                expect("Service to be available: " + MetacardType.class.getName())
                    .within(30, TimeUnit.SECONDS)
                    .until(
                        () ->
                            getServiceManager()
                                .getServiceReferences(
                                    MetacardType.class, "(name=" + newMetacardTypeName + ")"),
                        not(empty()));
                return null;
              });

      String ddfMetacardXml = getFileContent("metacard1.xml");

      String modifiedMetacardXml =
          ddfMetacardXml
              .replaceFirst("ddf\\.metacard", newMetacardTypeName)
              .replaceFirst("resource-uri", "new-attribute-required-2");
      id = ingest(modifiedMetacardXml, "text/xml");
      configureShowInvalidMetacards("true", "true", getAdminConfig());

      String newMetacardXpath = format("/metacards/metacard[@id=\"%s\"]", id);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          .body(hasXPath(newMetacardXpath + "/type", is(newMetacardTypeName)))
          .body(
              hasXPath(
                  "count(" + newMetacardXpath + "/string[@name=\"validation-errors\"]/value)",
                  is("1")))
          .body(
              hasXPath(
                  newMetacardXpath
                      + "/string[@name=\"validation-errors\"]/value[text()=\"new-attribute-required-1 is required\"]"))
          .body(
              hasXPath(
                  newMetacardXpath + "/string[@name=\"new-attribute-required-2\"]/value",
                  is("\" + uri + \"")));
    } finally {
      deleteMetacard(id);
      uninstallDefinitionJson(
          file,
          () -> {
            AttributeRegistry attributeRegistry =
                getServiceManager().getService(AttributeRegistry.class);
            expect("Attributes to be unregistered")
                .within(10, TimeUnit.SECONDS)
                .until(() -> !attributeRegistry.lookup("new-attribute-required-2").isPresent());
            return null;
          });
      getServiceManager().startFeature(true, "catalog-security-filter");
      configureShowInvalidMetacards("false", "false", getAdminConfig());
    }
  }

  private String getDefaultExpirationAsString() {
    final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
    format.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Z")));
    final Date defaultExpiration =
        Date.from(OffsetDateTime.of(2020, 2, 2, 2, 2, 2, 0, ZoneOffset.UTC).toInstant());
    return format.format(defaultExpiration);
  }

  private void verifyMetacardDoesNotContainAttribute(String metacardXml, String attribute) {
    assertThat(metacardXml, not(containsString(attribute)));
  }

  @Test
  @ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-2743
  public void testDefaultValuesCreate() throws Exception {
    final String customMetacardTypeName = "custom";
    File file =
        ingestDefinitionJsonWithWaitCondition(
            "defaults.json",
            () -> {
              expect("Service to be available: " + MetacardType.class.getName())
                  .within(30, TimeUnit.SECONDS)
                  .until(
                      () ->
                          getServiceManager()
                              .getServiceReferences(
                                  MetacardType.class, "(name=" + customMetacardTypeName + ")"),
                      not(empty()));
              return null;
            });

    String metacard3Xml = getFileContent("metacard3.xml");

    String metacard4Xml = getFileContent("metacard4.xml");

    metacard4Xml = metacard4Xml.replaceFirst("ddf\\.metacard", customMetacardTypeName);

    verifyMetacardDoesNotContainAttribute(metacard3Xml, Metacard.DESCRIPTION);
    verifyMetacardDoesNotContainAttribute(metacard3Xml, Metacard.EXPIRATION);
    verifyMetacardDoesNotContainAttribute(metacard4Xml, Metacard.DESCRIPTION);
    verifyMetacardDoesNotContainAttribute(metacard4Xml, Metacard.EXPIRATION);

    final String id3 = ingest(metacard3Xml, MediaType.APPLICATION_XML);
    final String id4 = ingest(metacard4Xml, MediaType.APPLICATION_XML);

    try {
      final String defaultDescription = "Default description";
      final String defaultCustomMetacardDescription = "Default custom description";
      final String defaultExpiration = getDefaultExpirationAsString();

      final String metacard3XPath = format(METACARD_X_PATH, id3);
      final String metacard4XPath = format(METACARD_X_PATH, id4);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          // The metacard had a title, so it should not have been set to the default
          .body(hasXPath(metacard3XPath + "/string[@name='title']/value", is("Metacard-3")))
          .body(
              hasXPath(
                  metacard3XPath + "/string[@name='description']/value", is(defaultDescription)))
          .body(
              hasXPath(
                  metacard3XPath + "/dateTime[@name='expiration']/value", is(defaultExpiration)))
          // The metacard had a title, so it should not have been set to the default
          .body(hasXPath(metacard4XPath + "/string[@name='title']/value", is("Metacard-4")))
          .body(
              hasXPath(
                  metacard4XPath + "/string[@name='description']/value",
                  is(defaultCustomMetacardDescription)))
          .body(
              hasXPath(
                  metacard4XPath + "/dateTime[@name='expiration']/value", is(defaultExpiration)));
    } finally {
      deleteMetacard(id3);
      deleteMetacard(id4);
      uninstallDefinitionJson(
          file,
          () -> {
            DefaultAttributeValueRegistry defaultsRegistry =
                getServiceManager().getService(DefaultAttributeValueRegistry.class);
            expect("Defaults to be unregistered")
                .within(10, TimeUnit.SECONDS)
                .until(
                    () ->
                        !defaultsRegistry
                            .getDefaultValue(customMetacardTypeName, Metacard.DESCRIPTION)
                            .isPresent());
            return null;
          });
    }
  }

  @Test
  @ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-2743
  public void testDefaultValuesUpdate() throws Exception {
    final String customMetacardTypeName = "custom";
    File file =
        ingestDefinitionJsonWithWaitCondition(
            "defaults.json",
            () -> {
              expect("Service to be available: " + MetacardType.class.getName())
                  .within(30, TimeUnit.SECONDS)
                  .until(
                      () ->
                          getServiceManager()
                              .getServiceReferences(
                                  MetacardType.class, "(name=" + customMetacardTypeName + ")"),
                      not(empty()));
              return null;
            });

    String metacard1Xml = getFileContent("metacard1.xml");

    final String id1 = ingest(metacard1Xml, MediaType.APPLICATION_XML);

    String metacard2Xml = getFileContent("metacard2.xml");

    metacard2Xml = metacard2Xml.replaceFirst("ddf\\.metacard", customMetacardTypeName);

    final String id2 = ingest(metacard2Xml, MediaType.APPLICATION_XML);

    try {
      final String updatedTitle1 = "Metacard-1 (Updated)";
      final String updatedTitle2 = "Metacard-2 (Updated)";
      metacard1Xml = metacard1Xml.replaceFirst("Metacard\\-1", updatedTitle1);
      metacard2Xml = metacard2Xml.replaceFirst("Metacard\\-2", updatedTitle2);

      verifyMetacardDoesNotContainAttribute(metacard1Xml, Metacard.DESCRIPTION);
      verifyMetacardDoesNotContainAttribute(metacard1Xml, Metacard.EXPIRATION);
      verifyMetacardDoesNotContainAttribute(metacard2Xml, Metacard.DESCRIPTION);
      verifyMetacardDoesNotContainAttribute(metacard2Xml, Metacard.EXPIRATION);

      update(id1, metacard1Xml, MediaType.APPLICATION_XML);
      update(id2, metacard2Xml, MediaType.APPLICATION_XML);

      final String defaultDescription = "Default description";
      final String defaultCustomMetacardDescription = "Default custom description";
      final String defaultExpiration = getDefaultExpirationAsString();

      final String metacard1XPath = format(METACARD_X_PATH, id1);
      final String metacard2XPath = format(METACARD_X_PATH, id2);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          .body(hasXPath(metacard1XPath + "/string[@name='title']/value", is(updatedTitle1)))
          .body(
              hasXPath(
                  metacard1XPath + "/string[@name='description']/value", is(defaultDescription)))
          .body(
              hasXPath(
                  metacard1XPath + "/dateTime[@name='expiration']/value", is(defaultExpiration)))
          .body(hasXPath(metacard2XPath + "/string[@name='title']/value", is(updatedTitle2)))
          .body(
              hasXPath(
                  metacard2XPath + "/string[@name='description']/value",
                  is(defaultCustomMetacardDescription)))
          .body(
              hasXPath(
                  metacard2XPath + "/dateTime[@name='expiration']/value", is(defaultExpiration)));
    } finally {
      deleteMetacard(id1);
      deleteMetacard(id2);
      uninstallDefinitionJson(
          file,
          () -> {
            DefaultAttributeValueRegistry defaultsRegistry =
                getServiceManager().getService(DefaultAttributeValueRegistry.class);
            expect("Defaults to be unregistered")
                .within(10, TimeUnit.SECONDS)
                .until(
                    () ->
                        !defaultsRegistry
                            .getDefaultValue(customMetacardTypeName, Metacard.DESCRIPTION)
                            .isPresent());
            return null;
          });
    }
  }

  @Test
  @ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-2743
  public void testInjectAttributesOnCreate() throws Exception {
    final String id = ingestXmlFromResource("/metacard-injections.xml");

    final String originalMetacardXml = getFileContent("metacard-injections.xml");
    final String basicMetacardTypeName = DEFAULT_METACARD_TYPE_NAME;
    final String otherMetacardTypeName = "other.metacard.type";

    final String otherMetacardXml =
        originalMetacardXml.replaceFirst(
            Pattern.quote(basicMetacardTypeName), otherMetacardTypeName);

    final String id2 = ingest(otherMetacardXml, MediaType.APPLICATION_XML);

    try {
      final String basicMetacardXpath = format(METACARD_X_PATH, id);
      final String otherMetacardXpath = format(METACARD_X_PATH, id2);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          .body(hasXPath(basicMetacardXpath + "/type", is(basicMetacardTypeName)))
          .body(hasXPath(basicMetacardXpath + "/int[@name='page-count']/value", is("55")))
          .body(not(hasXPath(basicMetacardXpath + "/double[@name='temperature']")))
          .body(hasXPath(otherMetacardXpath + "/type", is(otherMetacardTypeName)))
          .body(hasXPath(otherMetacardXpath + "/int[@name='page-count']/value", is("55")))
          .body(hasXPath(otherMetacardXpath + "/double[@name='temperature']/value", is("-12.541")));
    } finally {
      deleteMetacard(id);
      deleteMetacard(id2);
    }
  }

  @Test
  @ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-2743
  public void testInjectAttributesOnUpdate() throws Exception {
    final String id = ingestXmlFromResource("/metacard1.xml");
    final String id2 = ingestXmlFromResource("/metacard1.xml");

    try {
      final String basicMetacardTypeName = DEFAULT_METACARD_TYPE_NAME;
      final String otherMetacardTypeName = "other.metacard.type";

      final String updateBasicMetacardXml = getFileContent("metacard-injections.xml");

      final String updateOtherMetacardXml =
          updateBasicMetacardXml.replaceFirst(
              Pattern.quote(basicMetacardTypeName), otherMetacardTypeName);

      update(id, updateBasicMetacardXml, MediaType.APPLICATION_XML);
      update(id2, updateOtherMetacardXml, MediaType.APPLICATION_XML);

      final String basicMetacardXpath = format(METACARD_X_PATH, id);
      final String otherMetacardXpath = format(METACARD_X_PATH, id2);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          .body(hasXPath(basicMetacardXpath + "/type", is(basicMetacardTypeName)))
          .body(hasXPath(basicMetacardXpath + "/int[@name='page-count']/value", is("55")))
          .body(not(hasXPath(basicMetacardXpath + "/double[@name='temperature']")))
          .body(not(hasXPath(basicMetacardXpath + "/string[@name='point-of-contact']")))
          .body(hasXPath(otherMetacardXpath + "/type", is(otherMetacardTypeName)))
          .body(hasXPath(otherMetacardXpath + "/int[@name='page-count']/value", is("55")))
          .body(hasXPath(otherMetacardXpath + "/double[@name='temperature']/value", is("-12.541")))
          .body(not(hasXPath(basicMetacardXpath + "/string[@name='point-of-contact']")));

    } finally {
      deleteMetacard(id);
      deleteMetacard(id2);
    }
  }

  @Ignore("Ignored until DDF-1571 is addressed")
  @Test
  public void persistLargeObjectToWorkspace() throws Exception {
    persistToWorkspace(40000);
  }

  private void persistToWorkspace(int size) throws Exception {
    getServiceManager().startFeature(true, "search-ui-app");
    // Generate very large data block
    Map<String, String> map = Maps.newHashMap();
    for (int i = 0; i < size; i++) {
      map.put("Key-" + i, "Val-" + i);
    }

    JSONObject jsonObject = new JSONObject();
    jsonObject.putAll(map);
    String jsonString = jsonObject.toJSONString();

    final PersistentStore pstore = getServiceManager().getService(PersistentStore.class);

    PersistentItem item = new PersistentItem();
    item.addIdProperty("itest");
    item.addProperty("user", "itest");
    item.addProperty("workspaces_json", jsonString);

    final String WORKSPACE_TYPE = PersistenceType.WORKSPACE_TYPE.toString();
    try {
      int wait = 0;
      while (true) {
        try {
          pstore.get(WORKSPACE_TYPE);
          break;
        } catch (Exception e) {
          LOGGER.info("Waiting for persistence store to come online.");
          Thread.sleep(1000);
          if (wait++ > 60) {
            break;
          }
        }
      }
      assertThat(pstore.get(WORKSPACE_TYPE), is(empty()));
      pstore.add(WORKSPACE_TYPE, item);

      expect("Solr core to be spun up and item to be persisted")
          .within(5, TimeUnit.MINUTES)
          .until(() -> pstore.get(WORKSPACE_TYPE).size(), equalTo(1));

      List<Map<String, Object>> storedWs = pstore.get(WORKSPACE_TYPE, "\"id\" = 'itest'");
      assertThat(storedWs, hasSize(1));
      assertThat(storedWs.get(0).get("user_txt"), is("itest"));
    } finally {
      pstore.delete(WORKSPACE_TYPE, "\"id\" = 'itest'");
      expect("Workspace to be empty")
          .within(5, TimeUnit.MINUTES)
          .until(() -> pstore.get(WORKSPACE_TYPE).size(), equalTo(0));
    }
  }

  @Test
  @ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-2743
  public void testTypeValidation() throws Exception {
    String invalidCardId = null;
    String validCardId = null;
    try {
      getServiceManager().stopFeature(true, "catalog-security-filter");
      final String newMetacardTypeName = "customtype1";

      ingestDefinitionJsonWithWaitCondition(
          "customtypedefinitions.json",
          () -> {
            expect("Service to be available: " + MetacardType.class.getName())
                .within(30, TimeUnit.SECONDS)
                .until(
                    () ->
                        getServiceManager()
                            .getServiceReferences(
                                MetacardType.class, "(name=" + newMetacardTypeName + ")"),
                    not(empty()));
            return null;
          });

      invalidCardId = ingestXmlFromResource("/metacard-datatype-validation.xml");

      configureShowInvalidMetacards("true", "true", getAdminConfig());
      String newMetacardXpath = format("/metacards/metacard[@id=\"%s\"]", invalidCardId);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          .body(
              hasXPath(
                  "count(" + newMetacardXpath + "/string[@name=\"validation-errors\"]/value)",
                  is("1")));

      String ddfMetacardXml = getFileContent("metacard-datatype-validation.xml");

      String modifiedMetacardXml = ddfMetacardXml.replaceFirst("Invalid Type", "Image");
      validCardId = ingest(modifiedMetacardXml, "text/xml");

      String newMetacardXpath2 = format("/metacards/metacard[@id=\"%s\"]", validCardId);

      getOpenSearch("xml", null, null, "q=*")
          .log()
          .all()
          .assertThat()
          .body(
              hasXPath(
                  "count(" + newMetacardXpath2 + "/string[@name=\"validation-errors\"]/value)",
                  is("0")));
    } finally {

      if (invalidCardId != null) {
        deleteMetacard(invalidCardId);
      }

      if (validCardId != null) {
        deleteMetacard(validCardId);
      }

      getServiceManager().startFeature(true, "catalog-security-filter");
      configureShowInvalidMetacardsReset();
    }
  }

  @Test
  public void testCreateStorageCannotOverrideResourceUri() throws IOException {
    String fileName = testName.getMethodName() + ".jpg";
    String overrideResourceUri = "content:abc123";
    String overrideTitle = "overrideTitle";

    File tmpFile =
        createTemporaryFile(fileName, IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));

    String id =
        given()
            .multiPart("parse.resource", tmpFile)
            .multiPart(Core.TITLE, overrideTitle)
            .multiPart(Core.RESOURCE_URI, overrideResourceUri)
            .expect()
            .log()
            .headers()
            .statusCode(201)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    String metacardPath = format("/metacards/metacard[@id=\"%s\"]", id);

    ValidatableResponse response;
    response = getOpenSearch("xml", null, null, "q=*").log().all();

    response
        .assertThat()
        .body(
            hasXPath(
                metacardPath + "/string[@name=\"" + Core.RESOURCE_URI + "\"]/value",
                is(not(overrideResourceUri))));

    response
        .assertThat()
        .body(
            hasXPath(
                metacardPath + "/string[@name=\"" + Core.TITLE + "\"]/value", is(overrideTitle)));

    deleteMetacard(id);
  }

  @Test
  public void testUpdateStorageCannotOverrideResourceUri() throws IOException {
    String fileName = testName.getMethodName() + ".jpg";
    String overrideResourceUri = "content:abc123";
    String overrideTitle = "overrideTitle";

    File tmpFile =
        createTemporaryFile(fileName, IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));

    String id =
        given()
            .multiPart("parse.resource", tmpFile)
            .multiPart(Core.TITLE, overrideTitle)
            .multiPart(Core.RESOURCE_URI, overrideResourceUri)
            .expect()
            .log()
            .headers()
            .statusCode(HttpStatus.SC_CREATED)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    given()
        .multiPart("parse.resource", tmpFile)
        .multiPart(Core.RESOURCE_URI, overrideResourceUri)
        .expect()
        .log()
        .headers()
        .statusCode(HttpStatus.SC_BAD_REQUEST)
        .when()
        .put(REST_PATH.getUrl() + id);

    deleteMetacard(id);
  }

  @Test
  public void testCswMultiSortAsc() throws IOException {
    final String sortCardId1 = ingestXmlFromResource("/sorttestcard1.xml");
    final String sortCardId2 = ingestXmlFromResource("/sorttestcard2.xml");

    String query = getFileContent("/csw-multi-sort-asc.xml");

    ValidatableResponse validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    validatableResponse.body(containsString(sortCardId1));
    validatableResponse.body(not(containsString(sortCardId2)));
  }

  @Test
  public void testCswMultiSortDesc() throws IOException {
    final String sortCardId1 = ingestXmlFromResource("/sorttestcard1.xml");
    final String sortCardId2 = ingestXmlFromResource("/sorttestcard2.xml");

    String query = getFileContent("/csw-multi-sort-desc.xml");

    ValidatableResponse validatableResponse =
        given()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .body(query)
            .post(CSW_PATH.getUrl())
            .then();

    validatableResponse.body(not(containsString(sortCardId1)));
    validatableResponse.body(containsString(sortCardId2));
  }

  @Test
  public void testIngestSanitizationBadFile() throws Exception {
    // DDF-3172 bad.files and bad.file.extensions in custom.system.properties is not being respected

    // setup
    String fileName = "robots.txt"; // filename in bad.files
    File tmpFile = createTemporaryFile(fileName, IOUtils.toInputStream("Test"));

    // ingest
    String id =
        given()
            .multiPart(tmpFile)
            .expect()
            .log()
            .headers()
            .statusCode(HttpStatus.SC_CREATED)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    // query - check if sanitized properly
    getOpenSearch("xml", null, null, "q=*")
        .log()
        .all()
        .assertThat()
        .body(
            hasXPath(format(METACARD_X_PATH, id) + "/string[@name='title']/value", is("file.bin")));

    // clean up
    deleteMetacard(id);
  }

  @Test
  public void testIngestSanitizationBadExtension() throws Exception {
    // DDF-3172 bad.files and bad.file.extensions in custom.system.properties is not being respected

    // setup
    String fileName = "bad_file.cgi"; // file extension in bad.file.extensions
    File tmpFile = createTemporaryFile(fileName, IOUtils.toInputStream("Test"));

    // ingest
    String id =
        given()
            .multiPart(tmpFile)
            .expect()
            .log()
            .headers()
            .statusCode(HttpStatus.SC_CREATED)
            .when()
            .post(REST_PATH.getUrl())
            .getHeader("id");

    // query - check if sanitized properly
    getOpenSearch("xml", null, null, "q=*")
        .log()
        .all()
        .assertThat()
        .body(
            hasXPath(
                format(METACARD_X_PATH, id) + "/string[@name='title']/value", is("bad_file.bin")));

    // clean up
    deleteMetacard(id);
  }

  @Test
  public void testIngestIgnore() throws Exception {
    // DDF-3172 bad.files and bad.file.extensions in custom.system.properties is not being respected
    String fileName = "thumbs.db"; // filename in ignore.files

    File tmpFile = createTemporaryFile(fileName, IOUtils.toInputStream("Test"));

    // ingest
    given()
        .multiPart(tmpFile)
        .expect()
        .log()
        .headers()
        .statusCode(HttpStatus.SC_BAD_REQUEST)
        .when()
        .post(REST_PATH.getUrl());
  }

  // TODO: Turn on this test once DDF-3340 is complete
  @Ignore
  @Test
  public void testSolrSimilarityConfiguration() throws Exception {
    getServiceManager().startFeature(true, "catalog-solr-solrclient");
    getServiceManager().waitForHttpEndpoint(SOLR_SCHEMA_PATH.getUrl());

    Dictionary<String, Object> props = new Hashtable<>();
    props.put("solrSchemaUrl", SOLR_SCHEMA_PATH.getUrl());
    props.put("b", .943f);
    props.put("k1", 1.067f);
    Configuration config = configAdmin.getConfiguration(SOLR_CLIENT_PID, null);
    config.update(props);

    expect("Solr Configuration Updated for instance " + SOLR_SCHEMA_PATH.getUrl())
        .within(60, TimeUnit.SECONDS)
        .checkEvery(1, TimeUnit.SECONDS)
        .until(
            () -> {
              Response response =
                  given()
                      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                      .get(SOLR_SCHEMA_PATH.getUrl());
              String responseString = response.getBody().prettyPrint();
              return responseString.contains("\"b\":\"0.943\"");
            });
  }

  protected String ingestXmlFromResource(String resourceName) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(IOUtils.toInputStream(getFileContent(resourceName)), writer);
    return ingest(writer.toString(), "text/xml");
  }

  protected String ingestXmlFromResource(String resourceName, boolean checkResponse)
      throws IOException {
    if (checkResponse) {
      return ingestXmlFromResource(resourceName);
    } else {
      StringWriter writer = new StringWriter();
      IOUtils.copy(IOUtils.toInputStream(getFileContent(resourceName)), writer);
      return ingest(writer.toString(), "text/xml", checkResponse);
    }
  }

  private String ingestXmlWithProduct(String fileName) throws IOException {
    File file = new File(fileName);
    if (!file.createNewFile()) {
      fail("Unable to create " + fileName + " file.");
    }
    FileUtils.write(file, SAMPLE_DATA);
    String fileLocation = file.toURI().toURL().toString();
    LOGGER.debug("File Location: {}", fileLocation);
    String metacardId = ingest(getSimpleXml(fileLocation), "text/xml");
    return metacardId;
  }

  private File createTemporaryFile(String fileName, InputStream data) throws IOException {
    File file = new File(fileName);
    if (!file.createNewFile()) {
      fail("Unable to create " + fileName + " file.");
    }
    FileUtils.writeByteArrayToFile(file, IOUtils.toByteArray(data));
    return file;
  }

  public void configureShowInvalidMetacardsReset() throws IOException {
    configureShowInvalidMetacards("false", "true", getAdminConfig());
  }

  public void configureFilterInvalidMetacardsReset() throws IOException {
    configureFilterInvalidMetacards("true", "false", getAdminConfig());
  }

  protected void configureEnforceValidityErrorsAndWarningsReset() throws IOException {
    configureEnforceValidityErrorsAndWarnings("true", "false", getAdminConfig());
  }

  private void verifyMetadataBackup() throws Exception {
    StringBuilder buffer =
        new StringBuilder(OPENSEARCH_PATH.getUrl())
            .append("?")
            .append("format=")
            .append("xml")
            .append("&")
            .append("q=*")
            .append("&")
            .append("count=100");

    final Response response = when().get(buffer.toString());
    String id = XmlPath.given(response.asString()).get("metacards.metacard[0].@gml:id");
    Path path =
        Paths.get("data/backup/metacard", id.substring(0, 3), id.substring(3, 6), id + ".xml");

    expect("The metacard backup file is not found: " + path.toAbsolutePath())
        .within(60, TimeUnit.SECONDS)
        .checkEvery(1, TimeUnit.SECONDS)
        .until(() -> path.toFile().exists());

    assertThat(path.toFile().exists(), is(true));
  }

  public class PdpProperties extends HashMap<String, Object> {

    public static final String SYMBOLIC_NAME = "security-pdp-authzrealm";

    public static final String FACTORY_PID = "ddf.security.pdp.realm.AuthzRealm";

    public PdpProperties() {
      this.putAll(getServiceManager().getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
    }
  }

  public class CatalogPolicyProperties extends HashMap<String, Object> {
    public static final String SYMBOLIC_NAME = "catalog-security-policyplugin";

    public static final String FACTORY_PID = "org.codice.ddf.catalog.security.CatalogPolicy";

    public CatalogPolicyProperties() {
      this.putAll(getServiceManager().getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
    }
  }
}
