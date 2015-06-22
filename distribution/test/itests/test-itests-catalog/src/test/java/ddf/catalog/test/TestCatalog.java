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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ddf.common.test.BeforeExam;

/**
 * Tests the Catalog framework components. Includes helper methods at the Catalog level.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestCatalog extends AbstractIntegrationTest {

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
        when().get(url).then().log().all().assertThat().body(
                hasXPath("/metacard[@id='" + id + "']"));

        deleteMetacard(id);
    }

    @Test
    public void testOpenSearchQuery() {
        String id = ingestGeoJson(Library.getSimpleGeoJson());

        String url = OPENSEARCH_PATH + "?q=*&format=xml";
        LOGGER.info("Getting response to {}", url);
        expect().body(containsString(id)).when().get(url).prettyPrint();

        deleteMetacard(id);
    }

    public static void deleteMetacard(String id) {
        LOGGER.info("Deleteing metacard {}", id);
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

}
