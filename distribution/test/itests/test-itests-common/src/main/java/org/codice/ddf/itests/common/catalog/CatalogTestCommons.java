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
package org.codice.ddf.itests.common.catalog;

import static org.codice.ddf.itests.common.AbstractIntegrationTest.CSW_PATH;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.REST_PATH;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.getFileContent;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswInsertRequest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getMetacardIdFromCswInsertResponse;
import static org.hamcrest.Matchers.equalTo;
import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.given;
import java.io.IOException;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;


public class CatalogTestCommons {

    private static final String GEOJSON_NEAR_METACARD = "GeoJson near";

    private static final String GEOJSON_FAR_METACARD = "GeoJson far";

    private static final String PLAINXML_NEAR_METACARD = "PlainXml near";

    private static final String PLAINXML_FAR_METACARD = "PlainXml far";

    private static final String CSW_RESOURCE_ROOT = "/TestSpatial/";

    private static final String CSW_METACARD = "CswRecord.xml";

    /**
     * Ingests the provided metacard
     * @param data - body of the message containing metacard to be ingested
     * @param mimeType - content type header value
     * @return id of ingested metacard
     */
    public static String ingest(String data, String mimeType) {

        return given().body(data)
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .expect()
                .log()
                .all()
                .statusCode(HttpStatus.SC_CREATED)
                .when()
                .post(REST_PATH.getUrl())
                .getHeader("id");
    }

    /**
     * Ingests the provided metacard
     * @param data - body of the message containing metacard to be ingested
     * @param mimeType - content type header value
     * @param checkResponse - assert status code is 201
     * @return id of ingested metacard
     */
    public static String ingest(String data, String mimeType, boolean checkResponse) {
        if (checkResponse) {
            return ingest(data, mimeType);
        } else {
            return given().body(data)
                    .header(HttpHeaders.CONTENT_TYPE, mimeType)
                    .when()
                    .post(REST_PATH.getUrl())
                    .getHeader("id");
        }
    }

    public static String ingestGeoJson(String json) {
        return ingest(json, "application/json");
    }

    public static Map<String, String> ingestMetacards(Map<String, String> metacardsIds) {
        //ingest csw
        String cswRecordId = ingestCswRecord(getFileContent(
                CSW_RESOURCE_ROOT + "csw/record/CswRecord.xml"));
        metacardsIds.put(CSW_METACARD, cswRecordId);

        //ingest xml
        String plainXmlNearId = ingest(getFileContent(
                CSW_RESOURCE_ROOT + "xml/PlainXmlNear.xml"), MediaType.TEXT_XML);
        String plainXmlFarId = ingest(getFileContent(
                CSW_RESOURCE_ROOT + "xml/PlainXmlFar.xml"), MediaType.TEXT_XML);
        metacardsIds.put(PLAINXML_NEAR_METACARD, plainXmlNearId);
        metacardsIds.put(PLAINXML_FAR_METACARD, plainXmlFarId);

        //ingest json
        String geoJsonNearId = ingestGeoJson(getFileContent(
                CSW_RESOURCE_ROOT + "json/GeoJsonNear.json"));
        String geoJsonFarId = ingestGeoJson(getFileContent(
                CSW_RESOURCE_ROOT + "json/GeoJsonFar.json"));
        metacardsIds.put(GEOJSON_NEAR_METACARD, geoJsonNearId);
        metacardsIds.put(GEOJSON_FAR_METACARD, geoJsonFarId);

        return metacardsIds;
    }

    public static String ingestCswRecord(String cswRecord) {

        String transactionRequest = getCswInsertRequest("csw:Record", cswRecord);

        ValidatableResponse response = given().log()
                .all()
                .body(transactionRequest)
                .header("Content-Type", MediaType.APPLICATION_XML)
                .when()
                .post(CSW_PATH.getUrl())
                .then()
                .log()
                .all()
                .assertThat()
                .statusCode(equalTo(HttpStatus.SC_OK));

        return response.extract()
                .body()
                .xmlPath()
                .get("Transaction.InsertResult.BriefRecord.identifier")
                .toString();
    }

    /**
     *
     * @param id - id of metacard to update
     * @param data - body of request to update with
     * @param mimeType - content type header value
     */
    public static void update(String id, String data, String mimeType) {
        given().header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(data)
                .expect()
                .log()
                .all()
                .statusCode(HttpStatus.SC_OK)
                .when()
                .put(new AbstractIntegrationTest.DynamicUrl(REST_PATH, id).getUrl());
    }

    /**
     * Performs a delete request on the given metacard id
     * @param id - id of metacard to delete
     */
    public static void deleteMetacard(String id) {
        deleteMetacard(id, true);
    }

    /**
     * Performs a delete request on the given metacard id
     * @param id - id of metacard to delete
     * @param checkResponse
     */
    public static void deleteMetacard(String id, boolean checkResponse) {
        if (checkResponse) {
            delete(REST_PATH.getUrl() + id).then()
                    .assertThat()
                    .statusCode(200)
                    .log()
                    .all();
        } else {
            delete(REST_PATH.getUrl() + id).then()
                    .log()
                    .all();
        }
    }

    /**
     * Uses ids within the responses to delete
     * @param response - response with ids of metacards to delete
     */
    public static void deleteMetacardUsingCswResponseId(Response response) throws IOException,
            XPathExpressionException {
        String id = getMetacardIdFromCswInsertResponse(response);
        CatalogTestCommons.deleteMetacard(id);
    }
}
