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
package ddf.catalog.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.xml.sax.InputSource;

import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;

import ddf.common.test.BeforeExam;

/**
 * Tests the Catalog framework components. Includes helper methods at the Catalog level.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalog extends AbstractIntegrationTest {
    private static final String METACARD_X_PATH = "/metacards/metacard[@id='%s']";

    public static void deleteMetacard(String id) {
        LOGGER.info("Deleting metacard {}", id);
        delete(REST_PATH + id).then().assertThat().statusCode(200).log().all();
    }

    public static String ingestGeoJson(String json) {
        return ingest(json, "application/json");
    }

    public static String ingest(String data, String mimeType) {
        LOGGER.info("Ingesting data of type {}:\n{}", mimeType, data);
        return given().body(data).header("Content-Type", mimeType).expect().log().all()
                .statusCode(201).when().post(REST_PATH).getHeader("id");
    }

    @BeforeExam
    public void beforeExam() throws Exception {
        setLogLevels();
        waitForAllBundles();
        waitForCatalogProvider();
        waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
    }

    @Test
    public void testMetacardTransformersFromRest() {
        String id = ingestGeoJson(Library.getSimpleGeoJson());

        String url = REST_PATH + id;
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
                .body(Library.getCswIngest()).post(CSW_PATH);
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
                .body(Library.getCswFilterDelete()).post(CSW_PATH).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswDeleteOneWithCQL() {
        ingestCswRecord();

        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswCqlDelete()).post(CSW_PATH).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswDeleteNone() {
        Response response = ingestCswRecord();

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswCqlDeleteNone()).post(CSW_PATH).then();
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
                .body(Library.getCombinedCswInsertAndDelete()).post(CSW_PATH);
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
                .body(Library.getCswFilterDelete()).post(CSW_PATH).then();
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
                .header("Content-Type", MediaType.APPLICATION_XML).body(requestXml).post(CSW_PATH)
                .then();
        validatableResponse
                .body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                        hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("1")));

        String url = REST_PATH + id;
        when().get(url).then().log().all().assertThat()
                .body(hasXPath("//metacard/dateTime[@name='date']/value", startsWith("2015-08-10")),
                        hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
                        hasXPath("//metacard/string[@name='subject']/value", is("Updated Subject")),
                        hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[1]",
                                is("2.0 1.0")), hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[2]",
                                is("4.0 1.0")), hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[3]",
                                is("4.0 3.0")), hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[4]",
                                is("2.0 3.0")), hasXPath(
                                "(//metacard/geometry[@name='location']/value/Polygon/exterior/LinearRing/pos)[5]",
                                is("2.0 1.0")));

        deleteMetacard(id);
    }

    @Test
    public void testCswUpdateByNewRecordNoExistingMetacards() {
        ValidatableResponse response = given().header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswUpdateByNewRecord()).post(CSW_PATH).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")));
    }

    @Test
    public void testCswUpdateByNewRecordNoMetacardFound() {
        Response response = ingestCswRecord();

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswUpdateByNewRecord()).post(CSW_PATH).then();
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
                .body(Library.getCswUpdateByFilterConstraint()).post(CSW_PATH).then();
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

        String firstUrl = REST_PATH + firstId;
        when().get(firstUrl).then().log().all().assertThat()
                // Check that the updated attributes were changed.
                .body(hasXPath("//metacard/dateTime[@name='date']/value", startsWith("2015-08-25")),
                        hasXPath("//metacard/string[@name='title']/value", is("Updated Title")),
                        hasXPath("//metacard/string[@name='format']/value", is("")),
                        // Check that an attribute that was not updated was not changed.
                        hasXPath("//metacard/string[@name='subject']/value",
                                is("Hydrography--Dictionaries")));

        String secondUrl = REST_PATH + secondId;
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
                .body(Library.getCswUpdateByFilterConstraint()).post(CSW_PATH).then();
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
                .post(CSW_PATH).then();
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
    public void testCswUpdateRemoveAttributesByCqlConstraint() {
        Response response = ingestCswRecord();

        String id;

        try {
            id = getMetacardIdFromCswInsertResponse(response);
        } catch (IOException | XPathExpressionException e) {
            fail("Could not retrieve the ingested record's ID from the response.");
            return;
        }

        String url = REST_PATH + id;
        when().get(url).then().log().all().assertThat()
                // Check that the attributes about to be removed in the update are present.
                .body(hasXPath("//metacard/dateTime[@name='date']"),
                        hasXPath("//metacard/string[@name='title']"),
                        hasXPath("//metacard/geometry[@name='location']"));

        ValidatableResponse validatableResponse = given()
                .header("Content-Type", MediaType.APPLICATION_XML)
                .body(Library.getCswUpdateRemoveAttributesByCqlConstraint()).post(CSW_PATH).then();
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
        final String url =
                CSW_PATH + Library.getGetRecordByIdUrl().replace("placeholder_id", requestIds);

        final ValidatableResponse response = when().get(url).then();

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
                .header("Content-Type", MediaType.APPLICATION_XML).body(requestXml).post(CSW_PATH)
                .then();

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
                xPathGetRecordWithId + "/BoundingBox/LowerCorner[text()=\"-6.171 44.792\"]";
        final String xPathValidateBboxUpperWithId =
                xPathGetRecordWithId + "/BoundingBox/UpperCorner[text()=\"-2.228 51.126\"]";

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

            getSecurityPolicy().configureRestForAnonymous();

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
                .log().all().statusCode(500).when().post(REST_PATH);

        //verify query for first id works
        response = executeOpenSearch("xml", "q=*");
        response.body(hasXPath(xPath1));

        //revert to original configuration
        configProps.put("permissionStrings", new String[] {
                "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=anonymous"});
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
        response.body("metcards.metacard.size()", equalTo(1));
    }

    private ValidatableResponse executeOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH).append("?").append("format=")
                .append(format);

        for (String term : query) {
            buffer.append("&").append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return when().get(url).then();
    }

    private ValidatableResponse executeAdminOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH).append("?").append("format=")
                .append(format);

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
