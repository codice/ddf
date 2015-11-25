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
 **/
package ddf.test.itests.catalog;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin.VALIDATION_WARNINGS;
import static ddf.common.test.WaitCondition.expect;
import static ddf.test.itests.common.CswQueryBuilder.NOT;
import static ddf.test.itests.common.CswQueryBuilder.OR;
import static ddf.test.itests.common.CswQueryBuilder.PROPERTY_IS_EQUAL_TO;
import static ddf.test.itests.common.CswQueryBuilder.PROPERTY_IS_LIKE;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.json.simple.JSONObject;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.xml.sax.InputSource;

import com.google.common.collect.Maps;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

import ddf.common.test.BeforeExam;
import ddf.test.itests.AbstractIntegrationTest;
import ddf.test.itests.common.CswQueryBuilder;
import ddf.test.itests.common.Library;

/**
 * Tests the Catalog framework components. Includes helper methods at the Catalog level.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalog extends AbstractIntegrationTest {
    private static final String METACARD_X_PATH = "/metacards/metacard[@id='%s']";

    public static void deleteMetacard(String id) {
        LOGGER.info("Deleting metacard {}", id);
        delete(REST_PATH.getUrl() + id).then().assertThat().statusCode(200).log().all();
    }

    public static String ingestGeoJson(String json) {
        return ingest(json, "application/json");
    }

    public static String ingest(String data, String mimeType) {
        LOGGER.info("Ingesting data of type {}:\n{}", mimeType, data);
        return given().body(data).header("Content-Type", mimeType).expect().log().all()
                .statusCode(201).when().post(REST_PATH.getUrl()).getHeader("id");
    }

    public static String ingest(String data, String mimeType, boolean checkResponse) {
        if (checkResponse) {
            return ingest(data, mimeType);
        } else {
            LOGGER.info("Ingesting data of type {}:\n{}", mimeType, data);
            return given().body(data).header("Content-Type", mimeType).when()
                    .post(REST_PATH.getUrl()).getHeader("id");
        }
    }

    @BeforeExam
    public void beforeExam() throws Exception {
        basePort = getBasePort();
        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
        getCatalogBundle().waitForCatalogProvider();
        getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
    }

    @Test
    public void testMetacardTransformersFromRest() {
        String id = ingestGeoJson(Library.getSimpleGeoJson());

        String url = REST_PATH.getUrl() + id;
        LOGGER.info("Getting response to {}", url);
        when().get(url).then().log().all().assertThat()
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
        return given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswIngest()).post(CSW_PATH.getUrl());
    }

    private String getMetacardIdFromCswInsertResponse(Response response)
            throws IOException, XPathExpressionException {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String idPath = "//*[local-name()='identifier']/text()";
        InputSource xml = new InputSource(IOUtils.toInputStream(response.getBody().asString(),
                StandardCharsets.UTF_8.name()));
        return xPath.compile(idPath).evaluate(xml);
    }

    private void deleteMetacard(Response response) throws IOException, XPathExpressionException {
        String id = getMetacardIdFromCswInsertResponse(response);
        deleteMetacard(id);
    }

    @Test
    public void testCswIngest() {
        Response response = ingestCswRecord();
        ValidatableResponse validatableResponse = response.then();

        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/InsertResult/BriefRecord/title",
                                is("Aliquam fermentum purus quis arcu")),
                        hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));

        try {
            deleteMetacard(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
        }
    }

    @Test
    public void testCswDeleteOneWithFilter() {
        ingestCswRecord();

        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswFilterDelete()).post(CSW_PATH.getUrl()).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswDeleteOneWithCQL() {
        ingestCswRecord();

        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswCqlDelete()).post(CSW_PATH.getUrl()).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswDeleteNone() {
        Response response = ingestCswRecord();

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswCqlDeleteNone()).post(CSW_PATH.getUrl()).then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

        try {
            deleteMetacard(response);
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
        Response response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCombinedCswInsertAndDelete()).post(CSW_PATH.getUrl());
        ValidatableResponse validatableResponse = response.then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

        try {
            deleteMetacard(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
        }
    }

    @Test
    public void testCswDeleteMultiple() {
        ingestCswRecord();
        ingestCswRecord();

        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswFilterDelete()).post(CSW_PATH.getUrl()).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("2")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswUpdateByNewRecord() {
        Response response = ingestCswRecord();

        String requestXml = Library.getCswUpdateByNewRecord();

        String id;

        try {
            id = getMetacardIdFromCswInsertResponse(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
            return;
        }

        requestXml = requestXml.replace("identifier placeholder", id);

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML).body(requestXml)
                .post(CSW_PATH.getUrl()).then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("1")));

        String url = REST_PATH.getUrl() + id;
        when().get(url).then().log().all().assertThat()
                .body(hasXPath("//metacard/dateTime[@name='date']/value", startsWith("2015-08-10")),
                        hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
                        hasXPath("//metacard/string[@name='subject']/value", is("Updated Subject")),
                        hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[1]",
                                is("1.0 2.0")), hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[2]",
                                is("3.0 2.0")), hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[3]",
                                is("3.0 4.0")), hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[4]",
                                is("1.0 4.0")), hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[5]",
                                is("1.0 2.0")));

        deleteMetacard(id);
    }

    @Test
    public void testCswUpdateByNewRecordNoExistingMetacards() {
        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswUpdateByNewRecord()).post(CSW_PATH.getUrl()).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswUpdateByNewRecordNoMetacardFound() {
        Response response = ingestCswRecord();

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswUpdateByNewRecord()).post(CSW_PATH.getUrl()).then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

        try {
            deleteMetacard(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
        }
    }

    @Test
    public void testCswUpdateByFilterConstraint() {
        Response firstResponse = ingestCswRecord();
        Response secondResponse = ingestCswRecord();

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswUpdateByFilterConstraint()).post(CSW_PATH.getUrl()).then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
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
        when().get(firstUrl).then().log().all().assertThat()
                // Check that the updated attributes were changed.
                .body(hasXPath("//metacard/dateTime[@name='date']/value", startsWith("2015-08-25")),
                        hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
                        hasXPath("//metacard/string[@name='format']/value", is("")),
                        // Check that an attribute that was not updated was not changed.
                        hasXPath("//metacard/string[@name='subject']/value",
                                is("Hydrography--Dictionaries")));

        String secondUrl = REST_PATH.getUrl() + secondId;
        when().get(secondUrl).then().log().all().assertThat()
                // Check that the updated attributes were changed.
                .body(hasXPath("//metacard/dateTime[@name='date']/value", startsWith("2015-08-25")),
                        hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
                        hasXPath("//metacard/string[@name='format']/value", is("")),
                        // Check that an attribute that was not updated was not changed.
                        hasXPath("//metacard/string[@name='subject']/value",
                                is("Hydrography--Dictionaries")));

        deleteMetacard(firstId);
        deleteMetacard(secondId);
    }

    @Test
    public void testCswUpdateByFilterConstraintNoExistingMetacards() {
        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswUpdateByFilterConstraint()).post(CSW_PATH.getUrl()).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswUpdateByFilterConstraintNoMetacardsFound() {
        Response response = ingestCswRecord();

        String updateRequest = Library.getCswUpdateByFilterConstraint();

        // Change the <Filter> property being searched for so no results will be found.
        updateRequest = updateRequest.replace("title", "subject");

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML).body(updateRequest)
                .post(CSW_PATH.getUrl()).then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));

        try {
            deleteMetacard(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
        }
    }

    @Test
    public void testCswGetRecordsWithHitsResultType() {

        Response response = ingestCswRecord();

        String query = Library.getCswQuery("AnyText", "*");

        String id;

        try {
            id = getMetacardIdFromCswInsertResponse(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
            return;
        }

        //test with resultType="results" first
        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();

        validatableResponse.body(hasXPath("/GetRecordsResponse/SearchResults/Record"));

        //test with resultType="hits"
        query = query.replace("results", "hits");
        validatableResponse = given().header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();
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
        when().get(url).then().log().all().assertThat()
                // Check that the attributes about to be removed in the update are present.
                .body(hasXPath("//metacard/dateTime[@name='date']"),
                        hasXPath("//metacard/string[@name='title']"),
                        hasXPath("//metacard/geometry[@name='location']"));

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswUpdateRemoveAttributesByCqlConstraint()).post(CSW_PATH.getUrl())
                .then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("1")));

        when().get(url).then().log().all().assertThat()
                // Check that the updated attributes were removed.
                .body(not(hasXPath("//metacard/dateTime[@name='date']")),
                        not(hasXPath("//metacard/string[@name='title']")),
                        not(hasXPath("//metacard/geometry[@name='location']")),
                        // Check that an attribute that was not updated was not changed.
                        hasXPath("//metacard/string[@name='subject']/value",
                                is("Hydrography--Dictionaries")));

        deleteMetacard(id);
    }

    @Test
    public void testGetRecordById() throws IOException, XPathExpressionException {
        final Response firstResponse = ingestCswRecord();
        final Response secondResponse = ingestCswRecord();

        final String firstId = getMetacardIdFromCswInsertResponse(firstResponse);
        final String secondId = getMetacardIdFromCswInsertResponse(secondResponse);
        final String requestIds = firstId + "," + secondId;

        // Request the records we just added.
        final String url = CSW_PATH.getUrl() + Library.getGetRecordByIdUrl()
                .replace("placeholder_id", requestIds);

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

        final String requestXml = Library.getGetRecordByIdXml().replace("placeholder_id_1", firstId)
                .replace("placeholder_id_2", secondId);

        final ValidatableResponse response = given()
                .header("Content-Type", MediaType.APPLICATION_XML).body(requestXml)
                .post(CSW_PATH.getUrl()).then();

        verifyGetRecordByIdResponse(response, firstId, secondId);

        deleteMetacard(firstId);
        deleteMetacard(secondId);
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
    public void testFilterPlugin() {
        try {
            // Ingest the metacard
            String id1 = ingestXmlFromResource("/metacard1.xml");
            String xPath = String.format(METACARD_X_PATH, id1);

            // Test without filtering
            ValidatableResponse response = executeOpenSearch("xml", "q=*");
            response.body(hasXPath(xPath));

            startFeature(true, "sample-filter");
            startFeature(true, "filter-plugin");

            // Configure the PDP
            PdpProperties pdpProperties = new PdpProperties();
            pdpProperties.put("matchAllMappings",
                    "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=point-of-contact");
            Configuration config = configAdmin
                    .getConfiguration("ddf.security.pdp.realm.SimpleAuthzRealm", null);
            Dictionary<String, ?> configProps = new Hashtable<>(pdpProperties);
            config.update(configProps);
            waitForAllBundles();

            // Test with filtering with out point-of-contact
            response = executeOpenSearch("xml", "q=*");
            response.body(not(hasXPath(xPath)));

            // Test filtering with point of contact
            getSecurityPolicy().configureRestForBasic();

            response = executeAdminOpenSearch("xml", "q=*");
            response.body(hasXPath(xPath));

            getSecurityPolicy().configureRestForGuest();

            stopFeature(true, "sample-filter");
            stopFeature(true, "filter-plugin");

            deleteMetacard(id1);
        } catch (Exception e) {
            LOGGER.error("Couldn't start filter plugin");
        }
    }

    @Test
    public void testIngestPlugin() throws Exception {

        //ingest a data set to make sure we don't have any issues initially
        String id1 = ingestGeoJson(Library.getSimpleGeoJson());
        String xPath1 = String.format(METACARD_X_PATH, id1);

        //verify ingest by querying
        ValidatableResponse response = executeOpenSearch("xml", "q=*");
        response.body(hasXPath(xPath1));

        //change ingest plugin role to ingest
        IngestProperties ingestProperties = new IngestProperties();
        ingestProperties.put("permissionStrings",
                new String[] {"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=ingest"});
        Configuration config = configAdmin
                .getConfiguration("ddf.catalog.security.ingest.IngestPlugin", null);
        Dictionary<String, Object> configProps = new Hashtable<>(ingestProperties);
        config.update(configProps);
        waitForAllBundles();

        //try ingesting again - it should fail this time
        given().body(Library.getSimpleGeoJson()).header("Content-Type", "application/json").expect()
                .log().all().statusCode(500).when().post(REST_PATH.getUrl());

        //verify query for first id works
        response = executeOpenSearch("xml", "q=*");
        response.body(hasXPath(xPath1));

        //revert to original configuration
        configProps.put("permissionStrings",
                new String[] {"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=guest"});
        config.update(configProps);
        waitForAllBundles();

        deleteMetacard(id1);
    }

    @Test
    public void testContentDirectoryMonitor() throws Exception {
        startFeature(true, "content-core-directorymonitor");
        final String TMP_PREFIX = "tcdm_";
        Path tmpDir = Files.createTempDirectory(TMP_PREFIX);
        tmpDir.toFile().deleteOnExit();
        Path tmpFile = Files.createTempFile(tmpDir, TMP_PREFIX, "_tmp.xml");
        tmpFile.toFile().deleteOnExit();
        Files.copy(this.getClass().getClassLoader().getResourceAsStream("metacard5.xml"), tmpFile,
                StandardCopyOption.REPLACE_EXISTING);

        Map<String, Object> cdmProperties = new HashMap<>();
        cdmProperties.putAll(getMetatypeDefaults("content-core-directorymonitor",
                "ddf.content.core.directorymonitor.ContentDirectoryMonitor"));
        cdmProperties.put("monitoredDirectoryPath", tmpDir.toString() + "/"); // Must end with /
        cdmProperties.put("directive", "STORE_AND_PROCESS");
        createManagedService("ddf.content.core.directorymonitor.ContentDirectoryMonitor",
                cdmProperties);

        long startTime = System.nanoTime();
        ValidatableResponse response = null;
        do {
            response = executeOpenSearch("xml", "q=*SysAdmin*");
            if (response.extract().xmlPath().getList("metacards.metacard").size() == 1) {
                break;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException e) {
            }
        } while (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime) < TimeUnit.MINUTES
                .toMillis(1));
        response.body("metacards.metacard.size()", equalTo(1));
    }

    @Test
    public void persistObjectToWorkspace() throws Exception {
        persistToWorkspace(100);
    }

    @Test
    public void testValidationEnforced() throws Exception {
        getServiceManager().startFeature(true, "catalog-plugin-metacard-validation");

        // Update metacardMarkerPlugin config with enforcedMetacardValidators
        Configuration metacardMarker = configAdmin
                .getConfiguration("ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin",
                        null);
        Dictionary<String, Object> properties = new Hashtable<>();
        List<String> enforcedMetacardValidators = new ArrayList<>();
        enforcedMetacardValidators.add("sample-validator");
        properties.put("enforcedMetacardValidators", enforcedMetacardValidators);
        metacardMarker.update(properties);

        String id1 = ingestXmlFromResource("/metacard1.xml");
        String id2 = ingestXmlFromResource("/metacard2.xml", false);

        // Search for all entries, implicit "validation-warnings is null" and "validation-errors is null"
        // should get added by MetacardValidityCheckerPlugin
        String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*")
                .getQuery();
        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(query).post(CSW_PATH.getUrl()).then();
        // Assert Metacard1 is in results AND not Metacard2
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id2))));

        // Search for all entries that have no validation warnings or errors
        query = new CswQueryBuilder().addPropertyIsNullAttributeFilter(VALIDATION_WARNINGS)
                .getQuery();
        response = given().header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();
        // Assert Metacard1 is in results AND not Metacard2
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id2))));

        //Search for all entries that have validation-warnings from sample-validator or no validation warnings
        //Only search that will actually return all entries

        query = new CswQueryBuilder()
                .addAttributeFilter(PROPERTY_IS_EQUAL_TO, VALIDATION_WARNINGS, "*")
                .addPropertyIsNullAttributeFilter(VALIDATION_WARNINGS).addLogicalOperator(OR)
                .getQuery();

        response = given().header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();
        // Assert Metacard1 and NOT metacard2 is in results
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id2))));

        // Search for all metacards that have validation-warnings
        query = new CswQueryBuilder()
                .addAttributeFilter(PROPERTY_IS_EQUAL_TO, VALIDATION_WARNINGS, "*").getQuery();

        response = given().header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();
        // Assert Metacard1 and metacard2 are NOT in results
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id1))));
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id2))));

        deleteMetacard(id1);
        deleteMetacard(id2);
        getServiceManager().stopFeature(true, "catalog-plugin-metacard-validation");

    }

    @Test
    public void testValidationUnenforced() throws Exception {
        getServiceManager().startFeature(true, "catalog-plugin-metacard-validation");

        // Update metacardMarkerPlugin config with no enforcedMetacardValidators
        Configuration metacardMarker = configAdmin
                .getConfiguration("ddf.catalog.metacard.validation.MetacardValidityMarkerPlugin",
                        null);
        Dictionary<String, Object> properties = new Hashtable<>();
        List<String> enforcedMetacardValidators = new ArrayList<>();
        enforcedMetacardValidators.add("");
        properties.put("enforcedMetacardValidators", enforcedMetacardValidators);
        metacardMarker.update(properties);

        String id1 = ingestXmlFromResource("/metacard1.xml");
        String id2 = ingestXmlFromResource("/metacard2.xml");

        // Search for all entries, implicit "validation-warnings is null" and "validation-errors is null"
        // should get added by MetacardValidityCheckerPlugin
        String query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*")
                .getQuery();
        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(query).post(CSW_PATH.getUrl()).then();
        // Assert Metacard1 is in results AND not Metacard2
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id2))));

        // Search for all entries that have no validation warnings
        query = new CswQueryBuilder().addPropertyIsNullAttributeFilter(VALIDATION_WARNINGS)
                .getQuery();
        response = given().header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();
        // Assert Metacard1 is in results AND not Metacard2
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id2))));

        //Search for all entries that have validation-warnings or no validation warnings
        //Only search that will actually return all entries
        query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, VALIDATION_WARNINGS, "*")
                .addPropertyIsNullAttributeFilter(VALIDATION_WARNINGS).addLogicalOperator(OR)
                .getQuery();

        response = given().header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();
        // Assert Metacard2 is in results AND not Metacard1
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id1)));
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));

        // Search for all entries that are invalid
        query = new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, VALIDATION_WARNINGS, "*")
                .getQuery();

        response = given().header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();
        // Assert Metacard2 is in results AND not Metacard1
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id1))));

        query = new CswQueryBuilder().addPropertyIsNullAttributeFilter(VALIDATION_WARNINGS)
                .addLogicalOperator(NOT).getQuery();

        response = given().header("Content-Type", MediaType.APPLICATION_XML).body(query)
                .post(CSW_PATH.getUrl()).then();
        // Assert Metacard2 is in results AND not Metacard1
        response.body(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id2)));
        response.body(not(hasXPath(
                String.format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]",
                        id1))));

        deleteMetacard(id1);
        deleteMetacard(id2);
        getServiceManager().stopFeature(true, "catalog-plugin-metacard-validation");

    }

    @Test
    @Ignore
    // Ignored until DDF-1571 is addressed
    public void persistLargeObjectToWorkspace() throws Exception {
        persistToWorkspace(40000);
    }

    private void persistToWorkspace(int size) throws Exception {
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
                    .until(() -> pstore.get(PersistentStore.WORKSPACE_TYPE).size(), equalTo(1));

            List<Map<String, Object>> storedWs = pstore
                    .get(PersistentStore.WORKSPACE_TYPE, "id = 'itest'");
            assertThat(storedWs, hasSize(1));
            assertThat(storedWs.get(0).get("user_txt"), is("itest"));
        } finally {
            pstore.delete(PersistentStore.WORKSPACE_TYPE, "id = 'itest'");
            expect("Workspace to be empty").within(5, TimeUnit.MINUTES)
                    .until(() -> pstore.get(PersistentStore.WORKSPACE_TYPE).size(), equalTo(0));
        }
    }

    private ValidatableResponse executeOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH.getUrl()).append("?")
                .append("format=").append(format);

        for (String term : query) {
            buffer.append("&").append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return when().get(url).then();
    }

    private ValidatableResponse executeAdminOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH.getUrl()).append("?")
                .append("format=").append(format);

        for (String term : query) {
            buffer.append("&").append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return given().auth().basic("admin", "admin").when().get(url).then();
    }

    protected String ingestXmlFromResource(String resourceName) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream(resourceName), writer);
        return ingest(writer.toString(), "text/xml");
    }

    protected String ingestXmlFromResource(String resourceName, boolean checkResponse)
            throws IOException {
        if (checkResponse) {
            return ingestXmlFromResource(resourceName);
        } else {
            StringWriter writer = new StringWriter();
            IOUtils.copy(getClass().getResourceAsStream(resourceName), writer);
            return ingest(writer.toString(), "text/xml", checkResponse);
        }
    }

    public class PdpProperties extends HashMap<String, Object> {

        public static final String SYMBOLIC_NAME = "security-pdp-authzrealm";

        public static final String FACTORY_PID = "ddf.security.pdp.realm.SimpleAuthzRealm";

        public PdpProperties() {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
        }

    }

    public class IngestProperties extends HashMap<String, Object> {
        public static final String SYMBOLIC_NAME = "catalog-security-ingestplugin";

        public static final String FACTORY_PID = "ddf.catalog.security.ingest.IngestPlugin";

        public IngestProperties() {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
        }
    }
}
