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

import static org.codice.ddf.itests.common.WaitCondition.expect;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.deleteMetacard;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingestGeoJson;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.update;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureEnforceValidityErrorsAndWarnings;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureEnforcedMetacardValidators;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureFilterInvalidMetacards;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureMetacardValidityFilterPlugin;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureShowInvalidMetacards;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.AND;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.NOT;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.OR;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_EQUAL_TO;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_LIKE;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswInsertRequest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswQuery;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getMetacardIdFromCswInsertResponse;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static ddf.catalog.data.MetacardType.DEFAULT_METACARD_TYPE_NAME;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
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
import java.util.Collections;
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.annotations.BeforeExam;
import org.codice.ddf.itests.common.catalog.CatalogTestCommons;
import org.codice.ddf.itests.common.config.UrlResourceReaderConfigurator;
import org.codice.ddf.itests.common.csw.CswQueryBuilder;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
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
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Validation;

/**
 * Tests the Catalog framework components. Includes helper methods at the Catalog level.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalog extends AbstractIntegrationTest {
    private static final String METACARD_X_PATH = "/metacards/metacard[@id='%s']";

    private static final String SAMPLE_DATA = "sample data";

    private static final String SAMPLE_IMAGE = "/9466484_b06f26d579_o.jpg";

    private static final String SAMPLE_MP4 = "/sample.mp4";

    private static final String DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS = "data/products";

    @Rule
    public TestName testName = new TestName();

    private UrlResourceReaderConfigurator urlResourceReaderConfigurator;

    @BeforeExam
    public void beforeExam() throws Exception {
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
    }

    @Before
    public void setup() {
        urlResourceReaderConfigurator = getUrlResourceReaderConfigurator();
    }

    @After
    public void tearDown() throws IOException {
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(new String[] {
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS});
    }

    @Test
    public void testCreateStorage() throws IOException {
        String fileName = testName.getMethodName() + ".jpg";
        File tmpFile = createTemporaryFile(fileName,
                IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));
        String id = given().multiPart(tmpFile)
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
        File tmpFile = createTemporaryFile(fileName,
                IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));
        String id = given().multiPart(tmpFile)
                .expect()
                .log()
                .headers()
                .statusCode(201)
                .when()
                .post(REST_PATH.getUrl())
                .getHeader("id");

        final String url = CSW_PATH.getUrl() + getGetRecordByIdProductRetrievalUrl().replace(
                "placeholder_id",
                id);

        given().get(url)
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
        String id = given().multiPart(tmpFile)
                .expect()
                .log()
                .headers()
                .statusCode(201)
                .when()
                .post(REST_PATH.getUrl())
                .getHeader("id");

        final String url = REST_PATH.getUrl() + "sources/ddf.distribution/" + id
                + "?transform=resource&qualifier=preview";

        given().get(url)
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
        String id = ingestGeoJson(getFileContent(JSON_RECORD_RESOURCE_PATH  + "/SimpleGeoJsonRecord"));

        String url = REST_PATH.getUrl() + id;
        LOGGER.info("Getting response to {}", url);
        when().get(url)
                .then()
                .log()
                .all()
                .assertThat()
                .body(hasXPath("/metacard[@id='" + id + "']"));

        deleteMetacard(id);
    }

    @Test
    public void testOpenSearchQuery() throws IOException {
        String id1 = ingestXmlFromResource("/metacard1.xml");
        String id2 = ingestXmlFromResource("/metacard2.xml");
        String id3 = ingestXmlFromResource("/metacard3.xml");
        String id4 = ingestXmlFromResource("/metacard4.xml");

        // Test xml-format response for an all-query
        ValidatableResponse response = executeOpenSearch("xml", "q=*");
        response.body(hasXPath(String.format(METACARD_X_PATH, id1)))
                .body(hasXPath(String.format(METACARD_X_PATH, id2)))
                .body(hasXPath(String.format(METACARD_X_PATH, id3)))
                .body(hasXPath(String.format(METACARD_X_PATH, id4)));

        // Execute a text search against a value in an indexed field (metadata)
        response = executeOpenSearch("xml", "q=dunder*");
        response.body(hasXPath(String.format(METACARD_X_PATH, id3)))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id1))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id2))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id4))));

        // Execute a text search against a value that isn't in any indexed fields
        response = executeOpenSearch("xml", "q=whatisthedealwithairlinefood");
        response.body("metacards.metacard.size()", equalTo(0));

        // Execute a geo search that should match a point card
        response = executeOpenSearch("xml", "lat=40.689", "lon=-74.045", "radius=250");
        response.body(hasXPath(String.format(METACARD_X_PATH, id1)))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id2))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id3))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id4))));

        // Execute a geo search...this should match two cards, both polygons around the Space Needle
        response = executeOpenSearch("xml", "lat=47.62", "lon=-122.356", "radius=500");
        response.body(hasXPath(String.format(METACARD_X_PATH, id2)))
                .body(hasXPath(String.format(METACARD_X_PATH, id4)))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id1))))
                .body(not(hasXPath(String.format(METACARD_X_PATH, id3))));

        deleteMetacard(id1);
        deleteMetacard(id2);
        deleteMetacard(id3);
        deleteMetacard(id4);
    }

    private Response ingestCswRecord() {

        String uuid = UUID.randomUUID()
                .toString()
                .replaceAll("-", "");

        return given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .body(getCswInsertRequest("csw:Record",
                        getFileContent(CSW_RECORD_RESOURCE_PATH + "/CswRecord", ImmutableMap.of("id", uuid))))
                .post(CSW_PATH.getUrl());
    }

    private Response ingestXmlViaCsw() {
        return given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .body(getCswInsertRequest("xml",
                        getFileContent(XML_RECORD_RESOURCE_PATH + "/SimpleXmlNoDecMetacard",
                                ImmutableMap.of("uri", "http://example.com"))))
                .post(CSW_PATH.getUrl());
    }

    @Test
    public void testCswIngest() {
        Response response = ingestCswRecord();
        ValidatableResponse validatableResponse = response.then();

        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted",
                is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                hasXPath("//TransactionResponse/InsertResult/BriefRecord/title",
                        is("Aliquam fermentum purus quis arcu")),
                hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));

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

        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted",
                is("1")),
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
    public void testCswDeleteOneWithFilter() {
        ingestCswRecord();

        ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswFilterDeleteRequest"))
                .post(CSW_PATH.getUrl())
                .then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswDeleteOneWithCQL() {
        ingestCswRecord();

        ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswCqlDeleteRequest",
                        ImmutableMap.of("title", "Aliquam fermentum purus quis arcu")))
                .post(CSW_PATH.getUrl())
                .then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswDeleteNone() {
        Response response = ingestCswRecord();

        ValidatableResponse validatableResponse = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswCqlDeleteRequest",
                        ImmutableMap.of("title", "fake title")))
                .post(CSW_PATH.getUrl())
                .then();
        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                is("0")),
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
        Response response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswInsertAndDeleteRequest"))
                .post(CSW_PATH.getUrl());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                is("1")),
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

        ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswFilterDeleteRequest"))
                .post(CSW_PATH.getUrl())
                .then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("2")),
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

        ValidatableResponse validatableResponse = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(requestXml)
                .post(CSW_PATH.getUrl())
                .then();
        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("1")));

        String url = REST_PATH.getUrl() + id;
        when().get(url)
                .then()
                .log()
                .all()
                .assertThat()
                .body(hasXPath("//metacard/dateTime[@name='modified']/value", startsWith("2015-08-10")),
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
        given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
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
            ValidatableResponse validatableResponse = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateRecordRequest"))
                    .post(CSW_PATH.getUrl())
                    .then();
            validatableResponse.assertThat()
                    .statusCode(400);
        } finally {
            CatalogTestCommons.deleteMetacardUsingCswResponseId(response);
        }
    }

    @Test
    public void testCswUpdateByFilterConstraint() {
        Response firstResponse = ingestCswRecord();
        Response secondResponse = ingestCswRecord();

        ValidatableResponse validatableResponse = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateByFilterConstraintRequest"))
                .post(CSW_PATH.getUrl())
                .then();
        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                is("0")),
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
        when().get(firstUrl)
                .then()
                .log()
                .all()
                .assertThat()
                // Check that the updated attributes were changed.
                .body(hasXPath("//metacard/dateTime[@name='modified']/value", startsWith("2015-08-25")),
                        hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
                        hasXPath("//metacard/string[@name='media.format']/value", is("")),
                        // Check that an attribute that was not updated was not changed.
                        hasXPath("//metacard/string[@name='topic.category']/value",
                                is("Hydrography--Dictionaries")));

        String secondUrl = REST_PATH.getUrl() + secondId;
        when().get(secondUrl)
                .then()
                .log()
                .all()
                .assertThat()
                // Check that the updated attributes were changed.
                .body(hasXPath("//metacard/dateTime[@name='modified']/value", startsWith("2015-08-25")),
                        hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
                        hasXPath("//metacard/string[@name='media.format']/value", is("")),
                        // Check that an attribute that was not updated was not changed.
                        hasXPath("//metacard/string[@name='topic.category']/value",
                                is("Hydrography--Dictionaries")));

        deleteMetacard(firstId);
        deleteMetacard(secondId);
    }

    @Test
    public void testCswUpdateByFilterConstraintNoExistingMetacards() {
        ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateByFilterConstraintRequest"))
                .post(CSW_PATH.getUrl())
                .then();

        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswUpdateByFilterConstraintNoMetacardsFound() {
        Response response = ingestCswRecord();

        String updateRequest = getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateByFilterConstraintRequest");

        // Change the <Filter> property being searched for so no results will be found.
        updateRequest = updateRequest.replace("title", "subject");

        ValidatableResponse validatableResponse = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(updateRequest)
                .post(CSW_PATH.getUrl())
                .then();
        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                is("0")),
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

        String query = getCswQuery("AnyText",
                "*",
                "application/xml",
                "http://www.opengis.net/cat/csw/2.0.2");

        String id;

        try {
            id = getMetacardIdFromCswInsertResponse(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
            return;
        }

        //test with resultType="results" first
        ValidatableResponse validatableResponse = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(query)
                .post(CSW_PATH.getUrl())
                .then();

        validatableResponse.body(hasXPath("/GetRecordsResponse/SearchResults/Record"));

        //test with resultType="hits"
        query = query.replace("results", "hits");
        validatableResponse = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .body(query)
                .post(CSW_PATH.getUrl())
                .then();
        //assert that no records have been returned
        validatableResponse.body(not(hasXPath("//Record")));

        //testing with resultType='validate' is not
        //possible due to DDF-1537, this test will need
        //to be updated to test this once it is fixed.

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
        when().get(url)
                .then()
                .log()
                .all()
                .assertThat()
                // Check that the attributes about to be removed in the update are present.
                .body(hasXPath("//metacard/dateTime[@name='modified']"),
                        hasXPath("//metacard/string[@name='title']"),
                        hasXPath("//metacard/geometry[@name='location']"));

        ValidatableResponse validatableResponse = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
                .body(getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswUpdateRemoveAttributesByCqlConstraintRequest"))
                .post(CSW_PATH.getUrl())
                .then();
        validatableResponse.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted",
                is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("1")));

        when().get(url)
                .then()
                .log()
                .all()
                .assertThat()
                // Check that the updated attributes were removed.
                .body(not(hasXPath("//metacard/string[@name='title']")),
                        not(hasXPath("//metacard/geometry[@name='location']")),
                        // Check that an attribute that was not updated was not changed.
                        hasXPath("//metacard/dateTime[@name='modified']"),
                        hasXPath("//metacard/string[@name='topic.category']/value",
                                is("Hydrography--Dictionaries")));

        deleteMetacard(id);
    }

    @Test
    public void testCswNumericalQuery() throws IOException {
        //ingest test record
        String id = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/testNumerical.xml");

        //query for it by the numerical query
        String numericalQuery = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<GetRecords resultType=\"results\"\n"
                + "            outputFormat=\"application/xml\"\n"
                + "            outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "            startPosition=\"1\"\n" + "            maxRecords=\"10\"\n"
                + "            service=\"CSW\"\n" + "            version=\"2.0.2\"\n"
                + "            xmlns=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "            xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                + "  <Query typeNames=\"csw:Record\">\n"
                + "    <ElementSetName>full</ElementSetName>\n"
                + "    <Constraint version=\"1.1.0\">\n" + "      <ogc:Filter>\n"
                + "        <ogc:PropertyIsEqualTo wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\" matchCase=\"true\">\n"
                + "          <ogc:PropertyName>media.width-pixels</ogc:PropertyName>\n"
                + "          <ogc:Literal>12</ogc:Literal>\n" + "        </ogc:PropertyIsEqualTo>\n"
                + "      </ogc:Filter>\n" + "    </Constraint>\n" + "  </Query>\n"
                + "</GetRecords>\n";

        given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
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
        final String url = CSW_PATH.getUrl() + cswUrlGetRecordsParmaters.replace("placeholder_id",
                requestIds);

        final ValidatableResponse response = when().get(url)
                .then()
                .log()
                .all();

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

        final String requestXml = getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswByIdsQuery").replace(
                "placeholder_id_1",
                firstId)
                .replace("placeholder_id_2", secondId);

        final ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                MediaType.APPLICATION_XML)
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
        final String url = CSW_PATH.getUrl() + getGetRecordByIdProductRetrievalUrl().replace(
                "placeholder_id",
                metacardId);

        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(new String[] {
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

        given().get(url)
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(equalTo(200))
                .body(is(SAMPLE_DATA));

        deleteMetacard(metacardId);
    }

    @Test
    public void testUpdateContentResourceUri() throws IOException {
        String fileName = testName.getMethodName() + ".txt";
        String metacardId = ingestXmlWithProduct(fileName);
        update(metacardId, getSimpleXml("content:" + metacardId), "text/xml");
        given().header(HttpHeaders.CONTENT_TYPE, "text/xml")
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
        File tmpFile = createTemporaryFile(fileName,
                IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));

        String id = given().multiPart(tmpFile)
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

        //Get the product once
        get(url).then()
                .log()
                .headers();

        //Get again to hit the cache
        get(url).then()
                .log()
                .headers()
                .assertThat()
                .header(HttpHeaders.CONTENT_LENGTH, notNullValue());

        deleteMetacard(id);
    }

    @Test
    public void testPostGetRecordByIdProductRetrieval()
            throws IOException, XPathExpressionException {
        String fileName = testName.getMethodName() + ".txt";
        String metacardId = ingestXmlWithProduct(fileName);

        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(new String[] {
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

        final String requestXml =
                getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswByIdQuery").replace(
                        "placeholder_id_1",
                        metacardId);

        given().header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)
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

        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(new String[] {
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

        final String requestXml =
                getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswByIdQuery").replace(
                        "placeholder_id_1",
                        metacardId);

        int offset = 4;
        byte[] sampleDataByteArray = SAMPLE_DATA.getBytes();
        String partialSampleData = new String(Arrays.copyOfRange(sampleDataByteArray,
                offset,
                sampleDataByteArray.length));

        // @formatter:off
        given().headers(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML,
                CswConstants.RANGE_HEADER, String.format("bytes=%s-", offset))
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

        String productDirectory = new File(fileName).getAbsoluteFile()
                .getParent();
        urlResourceReaderConfigurator.setUrlResourceReaderRootDirs(new String[] {
                DEFAULT_URL_RESOURCE_READER_ROOT_RESOURCE_DIRS, productDirectory});

        final String requestXml =
                getFileContent(CSW_REQUEST_RESOURCE_PATH + "/CswByIdQuery").replace(
                        "placeholder_id_1",
                        metacardId);

        String invalidRange = "100";

        // @formatter:off
        given().header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_XML)
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

    private void verifyGetRecordByIdResponse(final ValidatableResponse response,
            final String... ids) {
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
            response.body(hasXPath(String.format(xPathValidateId, id)))
                    // Check the contents of the returned records.
                    .body(hasXPath(String.format(xPathValidateTitleWithId, id)))
                    .body(hasXPath(String.format(xPathValidateBboxLowerWithId, id)))
                    .body(hasXPath(String.format(xPathValidateBboxUpperWithId, id)));
        }
    }

    @Test
    public void testEnforceValidityErrorsOnly() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Configure to enforce validator
        configureEnforcedMetacardValidators(Collections.singletonList("sample-validator"), getAdminConfig());

        //Configure to enforce errors but not warnings
        configureEnforceValidityErrorsAndWarnings("true", "false", getAdminConfig());

        //Configure to show invalid metacards to show that invalid metacards were not ingested
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml", false);

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard and warning metacard should be in results but not error one
            response.body(containsString("warning metacard"));
            response.body(containsString("clean metacard"));
            response.body(not(containsString("error metacard")));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            deleteMetacard(id3, false);
            getServiceManager().stopFeature(true, "sample-validator");
            configureEnforceValidityErrorsAndWarningsReset();
            configureShowInvalidMetacardsReset();
        }
    }

    @Test
    public void testEnforceValidityWarningsOnly() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Configure to enforce validator
        configureEnforcedMetacardValidators(Collections.singletonList("sample-validator"), getAdminConfig());

        //Configure to enforce warnings but not errors
        configureEnforceValidityErrorsAndWarnings("false", "true", getAdminConfig());

        //Configure to show invalid metacards to show that invalid metacards were not ingested
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml", false);
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard and error metacard should be in results but not warning one
            response.body(not(containsString("warning metacard")));
            response.body(containsString("clean metacard"));
            response.body(containsString("error metacard"));

        } finally {
            deleteMetacard(id1, false);
            deleteMetacard(id2);
            deleteMetacard(id3);
            getServiceManager().stopFeature(true, "sample-validator");
            configureEnforceValidityErrorsAndWarningsReset();
            configureShowInvalidMetacardsReset();
        }
    }

    @Test
    public void testEnforceValidityErrorsAndWarnings() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Configure to enforce validator
        configureEnforcedMetacardValidators(Collections.singletonList("sample-validator"), getAdminConfig());

        //Configure to enforce errors and warnings
        configureEnforceValidityErrorsAndWarnings("true", "true", getAdminConfig());

        //Configure to show invalid metacards to show that invalid metacards were not ingested
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml", false);
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml", false);

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard should be in results but not invalid ones
            response.body(not(containsString("warning metacard")));
            response.body(containsString("clean metacard"));
            response.body(not(containsString("error metacard")));

        } finally {
            deleteMetacard(id1, false);
            deleteMetacard(id2);
            deleteMetacard(id3, false);
            getServiceManager().stopFeature(true, "sample-validator");
            configureEnforceValidityErrorsAndWarningsReset();
            configureShowInvalidMetacardsReset();
        }
    }

    @Test
    public void testNoEnforceValidityErrorsOrWarnings() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Configure to enforce validator
        configureEnforcedMetacardValidators(Collections.singletonList("sample-validator"), getAdminConfig());

        //Configure to enforce neither errors nor warnings
        configureEnforceValidityErrorsAndWarnings("false", "false", getAdminConfig());

        //Configure to show invalid metacards to show that invalid metacards were not ingested
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard should be in results but not invalid one
            response.body(containsString("warning metacard"));
            response.body(containsString("clean metacard"));
            response.body(containsString("error metacard"));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            deleteMetacard(id3);
            getServiceManager().stopFeature(true, "sample-validator");
            configureEnforceValidityErrorsAndWarningsReset();
            configureShowInvalidMetacardsReset();
        }
    }

    @Test
    public void testQueryByErrorFailedValidators() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Don't enforce the validator, so that it will be marked but ingested
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

        //Configure to show invalid metacards so the marked ones will appear in search
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        //Configure to not filter invalid metacards
        configureFilterInvalidMetacards("false", "false", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    Validation.FAILED_VALIDATORS_ERRORS,
                    "sample-validator")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard and warning metacard should be in results but not error one
            response.body(not(containsString("warning metacard")));
            response.body(not(containsString("clean metacard")));
            response.body(containsString("error metacard"));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            deleteMetacard(id3);
            getServiceManager().stopFeature(true, "sample-validator");
            configureShowInvalidMetacardsReset();
            configureFilterInvalidMetacardsReset();
        }
    }

    @Test
    public void testQueryByWarningFailedValidators() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Don't enforce the validator, so that it will be marked but ingested
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

        //Configure to show invalid metacards so the marked ones will appear in search
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        //Configure to not filter invalid metacards
        configureFilterInvalidMetacards("false", "false", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    Validation.FAILED_VALIDATORS_WARNINGS,
                    "sample-validator")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard and warning metacard should be in results but not error one
            response.body(not(containsString("error metacard")));
            response.body(not(containsString("clean metacard")));
            response.body(containsString("warning metacard"));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            deleteMetacard(id3);
            getServiceManager().stopFeature(true, "sample-validator");
            configureShowInvalidMetacardsReset();
            configureFilterInvalidMetacardsReset();
        }
    }

    @Test
    public void testFilterPlugin() throws Exception {
        // Ingest the metacard
        String id1 = ingestXmlFromResource("/metacard1.xml");
        String xPath = String.format(METACARD_X_PATH, id1);

        // Test without filtering
        ValidatableResponse response = executeOpenSearch("xml", "q=*");
        response.body(hasXPath(xPath));

        getServiceManager().startFeature(true, "sample-filter");

        try {
            // Configure the PDP
            PdpProperties pdpProperties = new PdpProperties();
            pdpProperties.put("matchAllMappings",
                    Arrays.asList(
                            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=point-of-contact"));
            Configuration config = configAdmin.getConfiguration("ddf.security.pdp.realm.AuthzRealm",
                    null);
            Dictionary<String, ?> configProps = new Hashtable<>(pdpProperties);
            config.update(configProps);
            getServiceManager().waitForAllBundles();

            // Test with filtering with out point-of-contact
            response = executeOpenSearch("xml", "q=*");
            response.body(not(hasXPath(xPath)));

            response = executeAdminOpenSearch("xml", "q=*");
            response.body(hasXPath(xPath));

        } finally {
            Configuration config = configAdmin.getConfiguration("ddf.security.pdp.realm.AuthzRealm",
                    null);
            Dictionary<String, ?> configProps = new Hashtable<>(new PdpProperties());
            config.update(configProps);
            getServiceManager().stopFeature(true, "sample-filter");
            deleteMetacard(id1);
        }
    }

    @Test
    public void testFilterPluginWarningsOnly() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Configure not enforcing validators so invalid metacards can ingest
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

        // Configure invalid filtering
        configureMetacardValidityFilterPlugin(Arrays.asList("invalid-state=system-admin"),
                getAdminConfig());

        // Configure query to request invalid metacards
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        //Configure to filter metacards with validation warnings but not validation errors
        configureFilterInvalidMetacards("false", "true", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard should be in results but not invalid one
            response.body(not(containsString("warning metacard")));
            response.body(containsString("clean metacard"));
            response.body(containsString("error metacard"));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            deleteMetacard(id3);
            getServiceManager().stopFeature(true, "sample-validator");
            configureFilterInvalidMetacardsReset();
            configureMetacardValidityFilterPlugin(Arrays.asList(""), getAdminConfig());
            configureShowInvalidMetacardsReset();
        }
    }

    @Test
    public void testFilterPluginErrorsOnly() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Configure not enforcing validators so invalid metacards can ingest
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

        // Configure invalid filtering
        configureMetacardValidityFilterPlugin(Arrays.asList("invalid-state=system-admin"),
                getAdminConfig());

        // Configure query to request invalid metacards
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        //Configure to filter metacards with validation errors but not validation warnings
        configureFilterInvalidMetacards("true", "false", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard should be in results but not invalid one
            response.body(not(containsString("error metacard")));
            response.body(containsString("clean metacard"));
            response.body(containsString("warning metacard"));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            deleteMetacard(id3);
            configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());
            getServiceManager().stopFeature(true, "sample-validator");
            configureFilterInvalidMetacardsReset();
            configureMetacardValidityFilterPlugin(Arrays.asList(""), getAdminConfig());
            configureShowInvalidMetacardsReset();
        }
    }

    @Test
    public void testFilterPluginWarningsAndErrors() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Configure not enforcing validators so invalid metacards can ingest
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

        // Configure invalid filtering
        configureMetacardValidityFilterPlugin(Arrays.asList("invalid-state=system-admin"),
                getAdminConfig());

        // Configure query to request invalid metacards
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        //configure to filter both metacards with validation errors and validation warnings
        configureFilterInvalidMetacards("true", "true", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard should be in results but not invalid one
            response.body(not(containsString("error metacard")));
            response.body(not(containsString("warning metacard")));
            response.body(containsString("clean metacard"));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id3);
            deleteMetacard(id2);
            configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());
            getServiceManager().stopFeature(true, "sample-validator");
            configureFilterInvalidMetacardsReset();
            configureMetacardValidityFilterPlugin(Arrays.asList(""), getAdminConfig());
            configureShowInvalidMetacardsReset();
        }
    }

    @Test
    public void testFilterPluginNoFiltering() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        //Configure not enforcing validators so invalid metacards can ingest
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

        // Configure invalid filtering
        configureMetacardValidityFilterPlugin(Arrays.asList("invalid-state=system-admin"),
                getAdminConfig());

        // Configure query to request invalid metacards
        configureShowInvalidMetacards("true", "true", getAdminConfig());

        //Configure to not filter metacard with validation errors or warnings
        configureFilterInvalidMetacards("false", "false", getAdminConfig());

        String id1 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleErrorMetacard.xml");
        String id2 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleCleanMetacard.xml");
        String id3 = ingestXmlFromResource(XML_RECORD_RESOURCE_PATH + "/sampleWarningMetacard.xml");

        try {
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();

            //clean metacard should be in results but not invalid one
            response.body(containsString("error metacard"));
            response.body(containsString("warning metacard"));
            response.body(containsString("clean metacard"));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            deleteMetacard(id3);
            configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());
            getServiceManager().stopFeature(true, "sample-validator");
            configureFilterInvalidMetacardsReset();
            configureMetacardValidityFilterPlugin(Arrays.asList(""), getAdminConfig());
            configureShowInvalidMetacardsReset();
        }
    }

    @Test
    public void testIngestPlugin() throws Exception {

        //ingest a data set to make sure we don't have any issues initially
        String id1 = ingestGeoJson(getFileContent(JSON_RECORD_RESOURCE_PATH  + "/SimpleGeoJsonRecord"));
        String xPath1 = String.format(METACARD_X_PATH, id1);

        //verify ingest by querying
        ValidatableResponse response = executeOpenSearch("xml", "q=*");
        response.body(hasXPath(xPath1));

        //change ingest plugin role to ingest
        CatalogPolicyProperties catalogPolicyProperties = new CatalogPolicyProperties();
        catalogPolicyProperties.put("createPermissions",
                new String[] {"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=ingest"});
        Configuration config = configAdmin.getConfiguration(
                "org.codice.ddf.catalog.security.CatalogPolicy",
                null);
        Dictionary<String, Object> configProps = new Hashtable<>(catalogPolicyProperties);
        config.update(configProps);
        getServiceManager().waitForAllBundles();

        //try ingesting again - it should fail this time
        given().body(getFileContent(JSON_RECORD_RESOURCE_PATH  + "/SimpleGeoJsonRecord"))
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .expect()
                .log()
                .all()
                .statusCode(400)
                .when()
                .post(REST_PATH.getUrl());

        //verify query for first id works
        response = executeOpenSearch("xml", "q=*");
        response.body(hasXPath(xPath1));

        //revert to original configuration
        configProps.put("createPermissions",
                new String[] {"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=guest"});
        config.update(configProps);
        getServiceManager().waitForAllBundles();

        deleteMetacard(id1);
    }

    @Test
    public void testVideoThumbnail() throws Exception {

        try (InputStream inputStream = IOUtils.toInputStream(getFileContent("sample.mp4"))) {
            final byte[] fileBytes = IOUtils.toByteArray(inputStream);

            final ValidatableResponse response = given().multiPart("file",
                    "sample.mp4",
                    fileBytes,
                    "video/mp4")
                    .post(REST_PATH.getUrl())
                    .then()
                    .statusCode(201);
        }
    }

    @Test
    public void testContentDirectoryMonitor() throws Exception {
        final String TMP_PREFIX = "tcdm_";
        Path tmpDir = Files.createTempDirectory(TMP_PREFIX);
        tmpDir.toFile()
                .deleteOnExit();
        Path tmpFile = Files.createTempFile(tmpDir, TMP_PREFIX, "_tmp.xml");
        tmpFile.toFile()
                .deleteOnExit();
        Files.copy(IOUtils.toInputStream(getFileContent("metacard5.xml")),
                tmpFile,
                StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> cdmProperties = new HashMap<>();
        cdmProperties.putAll(getServiceManager().getMetatypeDefaults("content-core-directorymonitor",
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor"));
        cdmProperties.put("monitoredDirectoryPath", tmpDir.toString() + "/");
        getServiceManager().createManagedService(
                "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor",
                cdmProperties);

        long startTime = System.nanoTime();
        ValidatableResponse response = null;
        do {
            response = executeOpenSearch("xml", "q=*SysAdmin*");
            if (response.extract()
                    .xmlPath()
                    .getList("metacards.metacard")
                    .size() == 1) {

                String cardId = response.extract()
                        .xmlPath()
                        .get("metacards.metacard[0].@gml:id")
                        .toString();

                if (cardId != null) {
                    deleteMetacard(cardId);
                }
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
            }
        } while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)
                < TimeUnit.MINUTES.toMillis(1));
        response.body("metacards.metacard.size()", equalTo(1));
    }

    @Test
    public void persistObjectToWorkspace() throws Exception {
        persistToWorkspace(100);
    }

    @Test
    public void testValidationEnforced() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");
        // Update metacardMarkerPlugin config with enforcedMetacardValidators
        configureEnforcedMetacardValidators(Collections.singletonList("sample-validator"), getAdminConfig());

        String id1 = ingestXmlFromResource("/metacard1.xml");
        String id2 = ingestXmlFromResource("/metacard2.xml", false);

        try {
            // Search for all entries, implicit "validation-warnings is null" and "validation-errors is null"
            // should get added by ValidationQueryFactory
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 is in results AND not Metacard2
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            // Search for all entries that have no validation warnings or errors
            query =
                    new CswQueryBuilder().addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
                            .getQuery();
            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 is in results AND not Metacard2
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            //Search for all entries that have validation-warnings from sample-validator or no validation warnings
            //Only search that will actually return all entries

            query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_EQUAL_TO,
                    Validation.VALIDATION_WARNINGS,
                    "*")
                    .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
                    .addLogicalOperator(OR)
                    .getQuery();

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 and NOT metacard2 is in results
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            // Search for all metacards that have validation-warnings
            query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_EQUAL_TO,
                    Validation.VALIDATION_WARNINGS,
                    "*")
                    .getQuery();

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 and metacard2 are NOT in results
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1))));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));
        } finally {
            deleteMetacard(id1);
            configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());
            getServiceManager().stopFeature(true, "sample-validator");
        }
    }

    @Test
    public void testValidationUnenforced() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");
        configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());

        String id1 = ingestXmlFromResource("/metacard1.xml");
        String id2 = ingestXmlFromResource("/metacard2.xml");

        try {
            // metacardMarkerPlugin has no enforcedMetacardValidators
            // Search for all entries, implicit "validation-warnings is null" and "validation-errors is null"
            // should get added by ValidationQueryFactory
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 is in results AND not Metacard2
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            // Search for all entries that have no validation warnings
            query =
                    new CswQueryBuilder().addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
                            .getQuery();
            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 is in results AND not Metacard2
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            //Search for all entries that have validation-warnings or no validation warnings
            //Only search that will actually return all entries
            query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    Validation.VALIDATION_WARNINGS,
                    "sampleWarnings")
                    .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
                    .addLogicalOperator(OR)
                    .getQuery();

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 AND Metacard2 are in results
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2)));

            // Search for all entries that are invalid
            query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    Validation.VALIDATION_WARNINGS,
                    "*")
                    .getQuery();

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard2 is in results AND not Metacard1
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1))));

            query =
                    new CswQueryBuilder().addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
                            .addLogicalOperator(NOT)
                            .getQuery();

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard2 is in results AND not Metacard1
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1))));
        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            getServiceManager().stopFeature(true, "sample-validator");
        }
    }

    @Test
    public void testValidationEnforcedUpdate() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");
        // metacardMarkerPlugin has no enforced validators so both metacards can be ingested
        final String id1 = ingestXmlFromResource("/metacard1.xml");
        final String id2 = ingestXmlFromResource("/metacard2.xml");

        try {
            // Enforce the sample metacard validator
            configureEnforcedMetacardValidators(Collections.singletonList("sample-validator"), getAdminConfig());

            String metacard2Xml = getFileContent("metacard2.xml");
            given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(metacard2Xml)
                    .put(new DynamicUrl(REST_PATH, id1).getUrl())
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_BAD_REQUEST);

            String metacard1Xml = getFileContent("metacard1.xml");
            given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(metacard1Xml)
                    .put(new DynamicUrl(REST_PATH, id2).getUrl())
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            String metacard1Path = String.format(METACARD_X_PATH, id1);
            String metacard2Path = String.format(METACARD_X_PATH, id2);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    .body(hasXPath(metacard1Path))
                    .body(hasXPath(metacard1Path + "/string[@name='title']/value",
                            is("Metacard-1")))
                    .body(not(hasXPath(metacard1Path + "/string[@name='validation-errors']")))
                    .body(not(hasXPath(metacard1Path + "/string[@name='validation-warnings']")))
                    .body(hasXPath(metacard2Path))
                    .body(hasXPath(metacard2Path + "/string[@name='title']/value",
                            is("Metacard-1")))
                    .body(not(hasXPath(metacard2Path + "/string[@name='validation-errors']")))
                    .body(not(hasXPath(metacard2Path + "/string[@name='validation-warnings']")));
        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            configureEnforcedMetacardValidators(Collections.singletonList(""), getAdminConfig());
            getServiceManager().stopFeature(true, "sample-validator");
        }
    }

    @Test
    public void testValidationUnenforcedUpdate() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");
        // metacardMarkerPlugin has no enforced validators so both metacards can be ingested
        final String id1 = ingestXmlFromResource("/metacard1.xml");
        final String id2 = ingestXmlFromResource("/metacard2.xml");

        try {
            String metacard2Xml = getFileContent("metacard2.xml");
            given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(metacard2Xml)
                    .put(new DynamicUrl(REST_PATH, id1).getUrl())
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            String metacard1Xml = getFileContent("metacard1.xml");
            given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(metacard1Xml)
                    .put(new DynamicUrl(REST_PATH, id2).getUrl())
                    .then()
                    .assertThat()
                    .statusCode(HttpStatus.SC_OK);

            configureShowInvalidMetacards("true", "true", getAdminConfig());

            String metacard1Path = String.format(METACARD_X_PATH, id1);
            String metacard2Path = String.format(METACARD_X_PATH, id2);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    .body(hasXPath(metacard1Path))
                    .body(hasXPath(metacard1Path + "/string[@name='title']/value",
                            is("Metacard-2")))
                    .body(hasXPath(
                            "count(" + metacard1Path + "/string[@name='validation-errors']/value)",
                            is("1")))
                    .body(hasXPath("count(" + metacard1Path
                            + "/string[@name='validation-warnings']/value)", is("1")))
                    .body(hasXPath(metacard2Path))
                    .body(hasXPath(metacard2Path + "/string[@name='title']/value",
                            is("Metacard-1")))
                    .body(not(hasXPath(metacard2Path + "/string[@name='validation-errors']")))
                    .body(not(hasXPath(metacard2Path + "/string[@name='validation-warnings']")));
        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            configureShowInvalidMetacardsReset();
            getServiceManager().stopFeature(true, "sample-validator");
        }
    }

    @Test
    public void testValidationFiltering() throws Exception {
        getServiceManager().startFeature(true, "catalog-security-filter", "sample-validator");

        // Update metacardMarkerPlugin config with no enforcedMetacardValidators
        configureEnforcedMetacardValidators(Arrays.asList(""), getAdminConfig());

        // Configure invalid filtering
        configureMetacardValidityFilterPlugin(Arrays.asList("invalid-state=system-admin"),
                getAdminConfig());

        // Configure the PDP
        PdpProperties pdpProperties = new PdpProperties();
        pdpProperties.put("matchOneMappings",
                Arrays.asList(
                        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=invalid-state"));
        Configuration config = configAdmin.getConfiguration("ddf.security.pdp.realm.AuthzRealm",
                null);
        Dictionary<String, ?> configProps = new Hashtable<>(pdpProperties);
        config.update(configProps);

        String id1 = ingestXmlFromResource("/metacard1.xml");
        String id2 = ingestXmlFromResource("/metacard2.xml");
        try {

            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    Validation.VALIDATION_WARNINGS,
                    "*")
                    .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
                    .addLogicalOperator(OR)
                    .getQuery();

            ValidatableResponse response = given().auth()
                    .preemptive()
                    .basic("admin", "admin")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard2 is in results AND Metacard1
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2)));

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard2 is in results Metacard1
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            // Configure invalid filtering
            configureMetacardValidityFilterPlugin(Arrays.asList("invalid-state=system-admin,guest"),
                    getAdminConfig());

            response = given().auth()
                    .preemptive()
                    .basic("admin", "admin")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard2 is in results AND Metacard1
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2)));

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard2 is in results Metacard1
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2)));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            getServiceManager().stopFeature(true, "catalog-security-filter", "sample-validator");
            config = configAdmin.getConfiguration("ddf.security.pdp.realm.AuthzRealm", null);
            configProps = new Hashtable<>(new PdpProperties());
            config.update(configProps);
        }
    }

    @Test
    public void testValidationChecker() throws Exception {
        getServiceManager().startFeature(true, "sample-validator");

        configureEnforcedMetacardValidators(Arrays.asList(""), getAdminConfig());

        // Configure invalid filtering
        configureMetacardValidityFilterPlugin(Arrays.asList(""), getAdminConfig());

        configureShowInvalidMetacards("true", "true", getAdminConfig());

        String id1 = ingestXmlFromResource("/metacard1.xml");
        String id2 = ingestXmlFromResource("/metacard2.xml");
        try {

            // Search for all entries, implicit "validation-warnings is null" and "validation-errors is null"
            // should get added by ValidationQueryFactory
            String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE,
                    "AnyText",
                    "*")
                    .getQuery();
            ValidatableResponse response = given().header(HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 is in results AND Metacard2 because showInvalidMetacards is true
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body((hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            // Search for all entries that have no validation warnings or errors
            query =
                    new CswQueryBuilder().addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
                            .getQuery();
            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 is in results AND not Metacard2
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            //Search for all entries that have validation-warnings from sample-validator or no validation warnings

            query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_EQUAL_TO,
                    Validation.VALIDATION_WARNINGS,
                    "*")
                    .addPropertyIsNullAttributeFilter(Validation.VALIDATION_WARNINGS)
                    .addLogicalOperator(OR)
                    .getQuery();

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 and NOT metacard2 is in results
            response.body(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1)));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

            // Search for all metacards that have validation-warnings
            query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_EQUAL_TO,
                    Validation.VALIDATION_WARNINGS,
                    "*")
                    .getQuery();

            response = given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                    .body(query)
                    .post(CSW_PATH.getUrl())
                    .then();
            // Assert Metacard1 and metacard2 are NOT in results
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id1))));
            response.body(not(hasXPath(String.format(
                    "/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                    id2))));

        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            getServiceManager().stopFeature(true, "sample-validator");
            configureShowInvalidMetacardsReset();
        }
    }

    private File copyFileToDefinitionsDir(String filename) throws IOException {
        Path definitionsDirPath = Paths.get(System.getProperty(DDF_HOME_PROPERTY),
                "etc/definitions");
        definitionsDirPath = Files.createDirectories(definitionsDirPath);
        definitionsDirPath.toFile()
                .deleteOnExit();

        Path tmpFile = definitionsDirPath.resolve(filename);
        tmpFile.toFile()
                .deleteOnExit();
        Files.copy(IOUtils.toInputStream(getFileContent(filename)),
                tmpFile,
                StandardCopyOption.REPLACE_EXISTING);
        return tmpFile.toFile();
    }

    private File ingestDefinitionJsonWithWaitCondition(String filename,
            Callable<Void> waitCondition) throws Exception {
        File definitionFile = copyFileToDefinitionsDir(filename);
        waitCondition.call();
        return definitionFile;
    }

    private void uninstallDefinitionJson(File definitionFile, Callable<Void> waitCondition)
            throws Exception {
        FileUtils.deleteQuietly(definitionFile);
        waitCondition.call();
    }

    @Test
    public void testMetacardDefinitionJsonFile() throws Exception {
        final String newMetacardTypeName = "new.metacard.type";
        File file = ingestDefinitionJsonWithWaitCondition("definitions.json", () -> {
            expect("Service to be available: " + MetacardType.class.getName()).within(10,
                    TimeUnit.SECONDS)
                    .until(() -> getServiceManager().getServiceReferences(MetacardType.class,
                            "(name=" + newMetacardTypeName + ")"), not(empty()));
            return null;
        });

        String ddfMetacardXml = getFileContent("metacard1.xml");

        String modifiedMetacardXml = ddfMetacardXml.replaceFirst("ddf\\.metacard",
                newMetacardTypeName)
                .replaceFirst("resource-uri", "new-attribute-required-2");
        String id = ingest(modifiedMetacardXml, "text/xml");
        configureShowInvalidMetacards("true", "true", getAdminConfig());
        try {
            String newMetacardXpath = String.format("/metacards/metacard[@id=\"%s\"]", id);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    .body(hasXPath(newMetacardXpath + "/type", is(newMetacardTypeName)))
                    .body(hasXPath("count(" + newMetacardXpath
                            + "/string[@name=\"validation-errors\"]/value)", is("2")))
                    .body(hasXPath(newMetacardXpath
                            + "/string[@name=\"validation-errors\"]/value[text()=\"point-of-contact is required\"]"))
                    .body(hasXPath(newMetacardXpath
                            + "/string[@name=\"validation-errors\"]/value[text()=\"new-attribute-required-1 is required\"]"))
                    .body(hasXPath(
                            newMetacardXpath + "/string[@name=\"new-attribute-required-2\"]/value",
                            is("\" + uri + \"")));
        } finally {
            deleteMetacard(id);
            uninstallDefinitionJson(file, () -> {
                AttributeRegistry attributeRegistry = getServiceManager().getService(
                        AttributeRegistry.class);
                expect("Attributes to be unregistered").within(10, TimeUnit.SECONDS)
                        .until(() -> attributeRegistry.lookup("new-attribute-required-2")
                                .isPresent());
                return null;
            });
            configureShowInvalidMetacards("false", "false", getAdminConfig());
        }
    }

    private String getDefaultExpirationAsString() {
        final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
        format.setTimeZone(TimeZone.getTimeZone(ZoneId.of("Z")));
        final Date defaultExpiration = Date.from(OffsetDateTime.of(2020,
                2,
                2,
                2,
                2,
                2,
                0,
                ZoneOffset.UTC)
                .toInstant());
        return format.format(defaultExpiration);
    }

    private void verifyMetacardDoesNotContainAttribute(String metacardXml, String attribute) {
        assertThat(metacardXml, not(containsString(attribute)));
    }

    @Test
    public void testDefaultValuesCreate() throws Exception {
        final String customMetacardTypeName = "custom";
        File file = ingestDefinitionJsonWithWaitCondition("defaults.json", () -> {
            expect("Service to be available: " + MetacardType.class.getName()).within(10,
                    TimeUnit.SECONDS)
                    .until(() -> getServiceManager().getServiceReferences(MetacardType.class,
                            "(name=" + customMetacardTypeName + ")"), not(empty()));
            return null;
        });

        String metacard1Xml = getFileContent("metacard1.xml");

        String metacard2Xml = getFileContent("metacard2.xml");

        metacard2Xml = metacard2Xml.replaceFirst("ddf\\.metacard", customMetacardTypeName);

        verifyMetacardDoesNotContainAttribute(metacard1Xml, Metacard.DESCRIPTION);
        verifyMetacardDoesNotContainAttribute(metacard1Xml, Metacard.EXPIRATION);
        verifyMetacardDoesNotContainAttribute(metacard2Xml, Metacard.DESCRIPTION);
        verifyMetacardDoesNotContainAttribute(metacard2Xml, Metacard.EXPIRATION);

        final String id1 = ingest(metacard1Xml, MediaType.APPLICATION_XML);
        final String id2 = ingest(metacard2Xml, MediaType.APPLICATION_XML);

        try {
            final String defaultDescription = "Default description";
            final String defaultCustomMetacardDescription = "Default custom description";
            final String defaultExpiration = getDefaultExpirationAsString();

            final String metacard1XPath = String.format(METACARD_X_PATH, id1);
            final String metacard2XPath = String.format(METACARD_X_PATH, id2);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    // The metacard had a title, so it should not have been set to the default
                    .body(hasXPath(metacard1XPath + "/string[@name='title']/value",
                            is("Metacard-1")))
                    .body(hasXPath(metacard1XPath + "/string[@name='description']/value",
                            is(defaultDescription)))
                    .body(hasXPath(metacard1XPath + "/dateTime[@name='expiration']/value",
                            is(defaultExpiration)))
                    // The metacard had a title, so it should not have been set to the default
                    .body(hasXPath(metacard2XPath + "/string[@name='title']/value",
                            is("Metacard-2")))
                    .body(hasXPath(metacard2XPath + "/string[@name='description']/value",
                            is(defaultCustomMetacardDescription)))
                    .body(hasXPath(metacard2XPath + "/dateTime[@name='expiration']/value",
                            is(defaultExpiration)));
        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            uninstallDefinitionJson(file, () -> {
                DefaultAttributeValueRegistry defaultsRegistry = getServiceManager().getService(
                        DefaultAttributeValueRegistry.class);
                expect("Defaults to be unregistered").within(10, TimeUnit.SECONDS)
                        .until(() -> !defaultsRegistry.getDefaultValue(customMetacardTypeName,
                                Metacard.DESCRIPTION)
                                .isPresent());
                return null;
            });
        }
    }

    @Test
    public void testDefaultValuesUpdate() throws Exception {
        final String customMetacardTypeName = "custom";
        File file = ingestDefinitionJsonWithWaitCondition("defaults.json", () -> {
            expect("Service to be available: " + MetacardType.class.getName()).within(30,
                    TimeUnit.SECONDS)
                    .until(() -> getServiceManager().getServiceReferences(MetacardType.class,
                            "(name=" + customMetacardTypeName + ")"), not(empty()));
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

            final String metacard1XPath = String.format(METACARD_X_PATH, id1);
            final String metacard2XPath = String.format(METACARD_X_PATH, id2);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    .body(hasXPath(metacard1XPath + "/string[@name='title']/value",
                            is(updatedTitle1)))
                    .body(hasXPath(metacard1XPath + "/string[@name='description']/value",
                            is(defaultDescription)))
                    .body(hasXPath(metacard1XPath + "/dateTime[@name='expiration']/value",
                            is(defaultExpiration)))
                    .body(hasXPath(metacard2XPath + "/string[@name='title']/value",
                            is(updatedTitle2)))
                    .body(hasXPath(metacard2XPath + "/string[@name='description']/value",
                            is(defaultCustomMetacardDescription)))
                    .body(hasXPath(metacard2XPath + "/dateTime[@name='expiration']/value",
                            is(defaultExpiration)));
        } finally {
            deleteMetacard(id1);
            deleteMetacard(id2);
            uninstallDefinitionJson(file, () -> {
                DefaultAttributeValueRegistry defaultsRegistry = getServiceManager().getService(
                        DefaultAttributeValueRegistry.class);
                expect("Defaults to be unregistered").within(10, TimeUnit.SECONDS)
                        .until(() -> !defaultsRegistry.getDefaultValue(customMetacardTypeName,
                                Metacard.DESCRIPTION)
                                .isPresent());
                return null;
            });
        }
    }

    @Test
    public void testInjectAttributesOnCreate() throws Exception {
        final File file = ingestDefinitionJsonWithWaitCondition("injections.json", () -> {
            expect("Injectable attributes to be registered").within(30, TimeUnit.SECONDS)
                    .until(() -> getServiceManager().getServiceReferences(InjectableAttribute.class,
                            null), hasSize(2));
            return null;
        });

        final String id = ingestXmlFromResource("/metacard-injections.xml");

        final String originalMetacardXml = getFileContent("metacard-injections.xml");
        final String basicMetacardTypeName = DEFAULT_METACARD_TYPE_NAME;
        final String otherMetacardTypeName = "other.metacard.type";

        final String otherMetacardXml = originalMetacardXml.replaceFirst(Pattern.quote(
                basicMetacardTypeName), otherMetacardTypeName);

        final String id2 = ingest(otherMetacardXml, MediaType.APPLICATION_XML);

        try {
            final String basicMetacardXpath = String.format(METACARD_X_PATH, id);
            final String otherMetacardXpath = String.format(METACARD_X_PATH, id2);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    .body(hasXPath(basicMetacardXpath + "/type", is(basicMetacardTypeName)))
                    .body(hasXPath(basicMetacardXpath + "/int[@name='page-count']/value", is("55")))
                    .body(not(hasXPath(basicMetacardXpath + "/double[@name='temperature']")))
                    .body(hasXPath(otherMetacardXpath + "/type", is(otherMetacardTypeName)))
                    .body(hasXPath(otherMetacardXpath + "/int[@name='page-count']/value", is("55")))
                    .body(hasXPath(otherMetacardXpath + "/double[@name='temperature']/value",
                            is("-12.541")));
        } finally {
            deleteMetacard(id);
            deleteMetacard(id2);
            uninstallDefinitionJson(file, () -> {
                expect("Injectable attributes to be unregistered").within(10, TimeUnit.SECONDS)
                        .until(() -> getServiceManager().getServiceReferences(InjectableAttribute.class,
                                null), is(empty()));
                return null;
            });
        }
    }

    @Test
    public void testInjectAttributesOnUpdate() throws Exception {
        final File file = ingestDefinitionJsonWithWaitCondition("injections.json", () -> {
            expect("Injectable attributes to be registered").within(10, TimeUnit.SECONDS)
                    .until(() -> getServiceManager().getServiceReferences(InjectableAttribute.class,
                            null), hasSize(2));
            return null;
        });

        final String id = ingestXmlFromResource("/metacard1.xml");
        final String id2 = ingestXmlFromResource("/metacard1.xml");

        try {
            final String basicMetacardTypeName = DEFAULT_METACARD_TYPE_NAME;
            final String otherMetacardTypeName = "other.metacard.type";

            final String updateBasicMetacardXml = getFileContent("metacard-injections.xml");

            final String updateOtherMetacardXml = updateBasicMetacardXml.replaceFirst(Pattern.quote(
                    basicMetacardTypeName), otherMetacardTypeName);

            update(id, updateBasicMetacardXml, MediaType.APPLICATION_XML);
            update(id2, updateOtherMetacardXml, MediaType.APPLICATION_XML);

            final String basicMetacardXpath = String.format(METACARD_X_PATH, id);
            final String otherMetacardXpath = String.format(METACARD_X_PATH, id2);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    .body(hasXPath(basicMetacardXpath + "/type", is(basicMetacardTypeName)))
                    .body(hasXPath(basicMetacardXpath + "/int[@name='page-count']/value", is("55")))
                    .body(not(hasXPath(basicMetacardXpath + "/double[@name='temperature']")))
                    .body(hasXPath(otherMetacardXpath + "/type", is(otherMetacardTypeName)))
                    .body(hasXPath(otherMetacardXpath + "/int[@name='page-count']/value", is("55")))
                    .body(hasXPath(otherMetacardXpath + "/double[@name='temperature']/value",
                            is("-12.541")));
        } finally {
            deleteMetacard(id);
            deleteMetacard(id2);
            uninstallDefinitionJson(file, () -> {
                expect("Injectable attributes to be unregistered").within(10, TimeUnit.SECONDS)
                        .until(() -> getServiceManager().getServiceReferences(InjectableAttribute.class,
                                null), is(empty()));
                return null;
            });
        }
    }

    @Test
    public void embeddedSolrProviderStarts() throws Exception {
        getServiceManager().startFeature(true, "catalog-solr-embedded-provider");
        getServiceManager().stopFeature(true, "catalog-solr-embedded-provider");
    }

    @Ignore("Ignored until DDF-1571 is addressed")
    @Test
    public void persistLargeObjectToWorkspace() throws Exception {
        persistToWorkspace(40000);
    }

    private void persistToWorkspace(int size) throws Exception {
        getServiceManager().waitForRequiredApps("search-ui-app");
        // Generate very large data block
        Map<String, String> map = Maps.newHashMap();
        for (int i = 0; i < size; i++) {
            map.put("Key-" + i, "Val-" + i);
        }

        String jsonString = new JSONObject(map).toJSONString();

        final PersistentStore pstore = getServiceManager().getService(PersistentStore.class);

        PersistentItem item = new PersistentItem();
        item.addIdProperty("itest");
        item.addProperty("user", "itest");
        item.addProperty("workspaces_json", jsonString);

        try {
            assertThat(pstore.get(PersistentStore.WORKSPACE_TYPE), is(empty()));
            pstore.add(PersistentStore.WORKSPACE_TYPE, item);

            expect("Solr core to be spun up and item to be persisted").within(5, TimeUnit.MINUTES)
                    .until(() -> pstore.get(PersistentStore.WORKSPACE_TYPE)
                            .size(), equalTo(1));

            List<Map<String, Object>> storedWs = pstore.get(PersistentStore.WORKSPACE_TYPE,
                    "id = 'itest'");
            assertThat(storedWs, hasSize(1));
            assertThat(storedWs.get(0)
                    .get("user_txt"), is("itest"));
        } finally {
            pstore.delete(PersistentStore.WORKSPACE_TYPE, "id = 'itest'");
            expect("Workspace to be empty").within(5, TimeUnit.MINUTES)
                    .until(() -> pstore.get(PersistentStore.WORKSPACE_TYPE)
                            .size(), equalTo(0));
        }
    }

    @Test
    public void testContentVersioning() throws Exception {

        Configuration config = getAdminConfig().getConfiguration("ddf.catalog.history.Historian");
        config.setBundleLocation(
                "mvn:ddf.catalog.core/catalog-core-standardframework/" + System.getProperty(
                        "ddf.version"));
        Dictionary properties = new Hashtable<>();
        properties.put("historyEnabled", true);
        config.update(properties);

        String fileName1 = "testcontent" + ".jpg";
        File tmpFile1 = createTemporaryFile(fileName1,
                IOUtils.toInputStream(getFileContent(SAMPLE_IMAGE)));
        String fileName2 = "testcontent2" + ".mp4";
        File tmpFile2 = createTemporaryFile(fileName2,
                IOUtils.toInputStream(getFileContent(SAMPLE_MP4)));

        String id = given().multiPart(tmpFile1)
                .expect()
                .log()
                .headers()
                .statusCode(201)
                .when()
                .post(REST_PATH.getUrl())
                .getHeader("id");

        final String url =
                REST_PATH.getUrl() + "sources/ddf.distribution/" + id + "?transform=resource";

        byte[] content1 = get(url).thenReturn()
                .body()
                .asByteArray();

        String metacardHistoryQuery = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_EQUAL_TO,
                "metacard.version.id",
                id)
                .addAttributeFilter(PROPERTY_IS_LIKE, Metacard.TAGS, "revision")
                .addLogicalOperator(AND)
                .getQuery("application/xml", "urn:catalog:metacard");

        given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .body(metacardHistoryQuery)
                .post(CSW_PATH.getUrl())
                .then()
                .body(hasXPath("count(/GetRecordsResponse/SearchResults/metacard)", is("0")));

        given().multiPart(tmpFile2)
                .expect()
                .log()
                .headers()
                .statusCode(200)
                .when()
                .put(REST_PATH.getUrl() + id);

        byte[] content2 = get(url).thenReturn()
                .body()
                .asByteArray();

        assertThat("The two content items should be different",
                Arrays.equals(content1, content2),
                is(false));
        given().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
                .body(metacardHistoryQuery)
                .post(CSW_PATH.getUrl())
                .then()
                .body(hasXPath("count(/GetRecordsResponse/SearchResults/metacard)", is("1")),
                        hasXPath(
                                "/GetRecordsResponse/SearchResults/metacard/string[@name='metacard.version.action']/value[text()=\"Versioned-Content\"]"));

        properties.put("historyEnabled", false);
        config.update(properties);
        deleteMetacard(id);
    }

    @Test
    public void testTypeValidation() throws Exception {
        String invalidCardId = null;
        String validCardId = null;
        try {
            final String newMetacardTypeName = "customtype1";

            ingestDefinitionJsonWithWaitCondition("customtypedefinitions.json", () -> {
                expect("Service to be available: " + MetacardType.class.getName()).within(30,
                        TimeUnit.SECONDS)
                        .until(() -> getServiceManager().getServiceReferences(MetacardType.class,
                                "(name=" + newMetacardTypeName + ")"), not(empty()));
                return null;
            });

            invalidCardId = ingestXmlFromResource("/metacard-datatype-validation.xml");

            configureShowInvalidMetacards("true", "true", getAdminConfig());
            String newMetacardXpath = String.format("/metacards/metacard[@id=\"%s\"]",
                    invalidCardId);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    .body(hasXPath("count(" + newMetacardXpath
                            + "/string[@name=\"validation-errors\"]/value)", is("1")));

            String ddfMetacardXml = getFileContent("metacard-datatype-validation.xml");

            String modifiedMetacardXml = ddfMetacardXml.replaceFirst("Invalid Type", "Image");
            validCardId = ingest(modifiedMetacardXml, "text/xml");

            String newMetacardXpath2 = String.format("/metacards/metacard[@id=\"%s\"]",
                    validCardId);

            executeOpenSearch("xml", "q=*").log()
                    .all()
                    .assertThat()
                    .body(hasXPath("count(" + newMetacardXpath2
                            + "/string[@name=\"validation-errors\"]/value)", is("0")));
        } finally {

            if (invalidCardId != null) {
                deleteMetacard(invalidCardId);
            }

            if (validCardId != null) {
                deleteMetacard(validCardId);
            }

            configureShowInvalidMetacardsReset();
        }
    }

    private ValidatableResponse executeOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH.getUrl()).append("?")
                .append("format=")
                .append(format);

        for (String term : query) {
            buffer.append("&")
                    .append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return when().get(url)
                .then();
    }

    private ValidatableResponse executeAdminOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH.getUrl()).append("?")
                .append("format=")
                .append(format);

        for (String term : query) {
            buffer.append("&")
                    .append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return given().auth()
                .preemptive()
                .basic("admin", "admin")
                .when()
                .get(url)
                .then();
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
        String fileLocation = file.toURI()
                .toURL()
                .toString();
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

    public static String getGetRecordByIdProductRetrievalUrl() {
        return "?service=CSW&version=2.0.2&request=GetRecordById&NAMESPACE=xmlns="
                + "http://www.opengis.net/cat/csw/2.0.2&"
                + "outputFormat=application/octet-stream&outputSchema="
                + "http://www.iana.org/assignments/media-types/application/octet-stream&"
                + "id=placeholder_id";
    }

    public static String getSimpleXml(String uri) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + getFileContent(
                XML_RECORD_RESOURCE_PATH + "/SimpleXmlNoDecMetacard",
                ImmutableMap.of("uri", uri));
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
}
