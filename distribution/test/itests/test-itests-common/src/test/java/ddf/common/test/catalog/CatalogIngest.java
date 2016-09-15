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
package ddf.common.test.catalog;

import static com.jayway.restassured.RestAssured.delete;
import static com.jayway.restassured.RestAssured.given;

import javax.ws.rs.core.HttpHeaders;

import com.jayway.restassured.filter.log.LogDetail;

public class CatalogIngest {

    public static void deleteMetacard(String id, String url, int expectedStatusCode) {
        delete(url + id).then()
                .assertThat()
                .statusCode(expectedStatusCode)
                .log()
                .ifValidationFails(LogDetail.ALL);
    }

    public static void deleteMetacard(String id, String url) {
        delete(url + id);
    }

    public static String ingestGeoJson(String json, String url) {
        return ingest(json, "application/json", url);
    }

    public static String ingest(String data, String mimeType, String url, int expectedStatusCode) {
        return given().body(data)
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .expect()
                .statusCode(expectedStatusCode)
                .log()
                .ifValidationFails(LogDetail.ALL)
                .post(url)
                .getHeader("id");
    }

    public static String ingest(String data, String mimeType, String url) {
        return given().body(data)
                .header(HttpHeaders.CONTENT_TYPE, mimeType)
                .post(url)
                .getHeader("id");
    }

    public static void update(String data, String mimeType, String url, int expectedStatusCode) {
        given().header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(data)
                .expect()
                .log()
                .ifValidationFails(LogDetail.ALL)
                .statusCode(expectedStatusCode)
                .put(url);
    }

    public static void update(String data, String mimeType, String url) {
        given().header(HttpHeaders.CONTENT_TYPE, mimeType)
                .body(data)
                .put(url);
    }
}
