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
 **/
package ddf.catalog.test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.jayway.restassured.response.ValidatableResponse;

import ddf.common.test.BeforeExam;

/**
 * Tests the Catalog framework components. Includes helper methods at the Catalog level.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalog extends AbstractIntegrationTest {

    private static final String CSW_ENDPOINT = SERVICE_ROOT + "/csw";

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
        response.body(hasXPath("/metacards/metacard[@id='" + id1 + "']"))
                .body(hasXPath("/metacards/metacard[@id='" + id2 + "']"))
                .body(hasXPath("/metacards/metacard[@id='" + id3 + "']"))
                .body(hasXPath("/metacards/metacard[@id='" + id4 + "']"));

        // Execute a text search against a value in an indexed field (metadata)
        response = executeOpenSearch("xml", "q=dunder*");
        response.body(hasXPath("/metacards/metacard[@id='" + id3 + "']"))
                .body(not(hasXPath("/metacards/metacard[@id='" + id1 + "']")))
                .body(not(hasXPath("/metacards/metacard[@id='" + id2 + "']")))
                .body(not(hasXPath("/metacards/metacard[@id='" + id4 + "']")));

        // Execute a text search against a value that isn't in any indexed fields
        response = executeOpenSearch("xml", "q=whatisthedealwithairlinefood");
        response.body("metacards.metacard.size()", equalTo(0));

        // Execute a geo search that should match a point card
        response = executeOpenSearch("xml", "lat=40.689", "lon=-74.045", "radius=250");
        response.body(hasXPath("/metacards/metacard[@id='" + id1 + "']"))
                .body(not(hasXPath("/metacards/metacard[@id='" + id2 + "']")))
                .body(not(hasXPath("/metacards/metacard[@id='" + id3 + "']")))
                .body(not(hasXPath("/metacards/metacard[@id='" + id4 + "']")));

        // Execute a geo search...this should match two cards, both polygons around the Space Needle
        response = executeOpenSearch("xml", "lat=47.62", "lon=-122.356", "radius=500");
        response.body(hasXPath("/metacards/metacard[@id='" + id2 + "']"))
                .body(hasXPath("/metacards/metacard[@id='" + id4 + "']"))
                .body(not(hasXPath("/metacards/metacard[@id='" + id1 + "']")))
                .body(not(hasXPath("/metacards/metacard[@id='" + id3 + "']")));

        deleteMetacard(id1);
        deleteMetacard(id2);
        deleteMetacard(id3);
        deleteMetacard(id4);
    }

    @Test
    public void testCswIngest() {
        ValidatableResponse response = given().header("Content-Type", "application/xml")
                .body(Library.getCswIngest()).post(CSW_ENDPOINT).then();
        response.body(hasXPath("//TransactionResponse/TransactionSummary/totalInserted", is("1")),
                hasXPath("//TransactionResponse/TransactionSummary/totalUpdated", is("0")),
                hasXPath("//TransactionResponse/TransactionSummary/totalDeleted", is("0")),
                hasXPath("//TransactionResponse/InsertResult/BriefRecord/title",
                        is("Aliquam fermentum purus quis arcu")),
                hasXPath("//TransactionResponse/InsertResult/BriefRecord/BoundingBox"));
    }

    private ValidatableResponse executeOpenSearch(String format, String... query) {
        StringBuilder buffer = new StringBuilder(OPENSEARCH_PATH).append("?").append("format=")
                .append(format);

        for (String term : query) {
            buffer.append("&");
            buffer.append(term);
        }

        String url = buffer.toString();
        LOGGER.info("Getting response to {}", url);

        return when().get(url).then();
    }

    protected String ingestXmlFromResource(String resourceName) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(getClass().getResourceAsStream(resourceName), writer);
        return ingest(writer.toString(), "text/xml");
    }

}
