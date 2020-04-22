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
package org.codice.ddf.itests.common.catalog;

import static com.jayway.restassured.RestAssured.given;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.CSW_PATH;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.REST_PATH;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.getFileContent;
import static org.codice.ddf.itests.common.csw.CswQueryBuilder.PROPERTY_IS_LIKE;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswInsertRequest;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getMetacardIdFromCswInsertResponse;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.xml.HasXPath.hasXPath;

import com.jayway.restassured.filter.log.LogDetail;
import com.jayway.restassured.response.Response;
import com.jayway.restassured.response.ValidatableResponse;
import java.io.IOException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.xpath.XPathExpressionException;
import org.apache.http.HttpStatus;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.csw.CswQueryBuilder;

public class CatalogTestCommons {

  public static final String TRANSFORMER_XML = "xml";

  private CatalogTestCommons() {}

  /**
   * Ingests the provided metacard
   *
   * @param data - body of the message containing metacard to be ingested
   * @param mimeType - content type header value
   * @return id of ingested metacard
   */
  public static String ingest(String data, String mimeType) {
    return ingest(data, mimeType, HttpStatus.SC_CREATED);
  }

  /**
   * Ingests the provided metacard
   *
   * @param data - body of the message containing metacard to be ingested
   * @param mimeType - content type header value
   * @param checkResponse - assert status code is 201
   * @return id of ingested metacard
   */
  public static String ingest(String data, String mimeType, boolean checkResponse) {
    if (checkResponse) {
      return ingest(data, mimeType, HttpStatus.SC_CREATED);
    } else {
      return given()
          .body(data)
          .header(HttpHeaders.CONTENT_TYPE, mimeType)
          .when()
          .post(REST_PATH.getUrl())
          .getHeader("id");
    }
  }

  /**
   * @param data - body of the message containing metacard to be ingested
   * @param mimeType - content type header value
   * @param expectedStatusCode - expected status code to check for
   * @return id of ingested metacard
   */
  public static String ingest(String data, String mimeType, int expectedStatusCode) {
    return given()
        .body(data)
        .header(HttpHeaders.CONTENT_TYPE, mimeType)
        .expect()
        .statusCode(expectedStatusCode)
        .log()
        .ifValidationFails(LogDetail.ALL)
        .post(REST_PATH.getUrl())
        .getHeader("id");
  }

  /**
   * @param data - body of the message containing metacard to be ingested
   * @param mimeType - content type header value
   * @param username - username of auth user
   * @param password - password of auth user
   * @return id of ingested metacard
   */
  public static String ingestWithBasicAuth(
      String data, String mimeType, String username, String password) {
    return ingestWithBasicAuth(data, mimeType, username, password, HttpStatus.SC_CREATED);
  }

  /**
   * @param data - body of the message containing metacard to be ingested
   * @param mimeType - content type header value
   * @param username - username of auth user
   * @param password - password of auth user
   * @param expectedStatusCode - expected status code to check for
   * @return id of ingested metacard
   */
  public static String ingestWithBasicAuth(
      String data, String mimeType, String username, String password, int expectedStatusCode) {
    return given()
        .body(data)
        .header(HttpHeaders.CONTENT_TYPE, mimeType)
        .auth()
        .preemptive()
        .basic(username, password)
        .expect()
        .statusCode(expectedStatusCode)
        .log()
        .ifValidationFails(LogDetail.ALL)
        .post(REST_PATH.getUrl())
        .getHeader("id");
  }

  /**
   * Ingests an xml resource by name. Does not return until resource has been verified to be in the
   * catalog
   *
   * @param resourceName - The relative path of the resource file
   * @return metacard id
   */
  public static String ingestXmlFromResourceAndWait(String resourceName) {
    String content = getFileContent(resourceName);
    String[] id = new String[1];
    // ingest might not succeed the first time due to the async nature of some configurations
    // Will try several times before considering it failed.
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, SECONDS)
        .ignoreExceptions()
        .until(
            () -> {
              id[0] = ingest(content, "text/xml", true);
              return true;
            });
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, SECONDS)
        .ignoreExceptions()
        .until(() -> doesMetacardExist(id[0]));
    return id[0];
  }

  /**
   * Does a wildcard search and verifies that one of the results is a metacard with the given id.
   * This doesn't query directly on the metacard id because that query can return the metacard
   * before it has been committed to the catalog. Metacards that have not been committed to the
   * catalog will not be returned in queries unless that query is a metacard id query.
   *
   * @param id The metacard id to look up
   * @return returns true if the metacard is in the catalog, false otherwise.
   */
  private static boolean doesMetacardExist(String id) {
    try {
      String query =
          new CswQueryBuilder().addAttributeFilter(PROPERTY_IS_LIKE, "AnyText", "*").getQuery();
      ValidatableResponse response =
          given()
              .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
              .body(query)
              .post(CSW_PATH.getUrl())
              .then();
      response.body(
          hasXPath(format("/GetRecordsResponse/SearchResults/Record[identifier=\"%s\"]", id)));
      return true;
    } catch (AssertionError e) {
      return false;
    }
  }

  public static String ingestGeoJson(String json) {
    return ingest(json, "application/json");
  }

  public static String ingestCswRecord(String cswRecord) {
    String transactionRequest = getCswInsertRequest("csw:Record", cswRecord);

    ValidatableResponse response =
        given()
            .body(transactionRequest)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML)
            .when()
            .post(CSW_PATH.getUrl())
            .then()
            .assertThat()
            .statusCode(equalTo(HttpStatus.SC_OK));

    return response
        .extract()
        .body()
        .xmlPath()
        .get("Transaction.InsertResult.BriefRecord.identifier")
        .toString();
  }

  /**
   * @param id - id of metacard to query
   * @param transformer - transformer to use for response
   */
  public static String query(String id, String transformer) {
    return query(id, transformer, HttpStatus.SC_OK);
  }

  /**
   * @param id - id of metacard to query
   * @param transformer - transformer to use for response
   * @param expectedStatusCode - expected status code to check for
   */
  public static String query(String id, String transformer, int expectedStatusCode) {
    ValidatableResponse response =
        given()
            .get(REST_PATH.getUrl() + id + "?transform=" + transformer)
            .then()
            .assertThat()
            .statusCode(equalTo(expectedStatusCode));
    return response.extract().body().asString();
  }

  /**
   * @param id - id of metacard to query
   * @param transformer - transformer to use for response
   * @param username - username of auth user
   * @param password - password of auth user
   */
  public static String queryWithBasicAuth(
      String id, String transformer, String username, String password) {
    return queryWithBasicAuth(id, transformer, username, password, HttpStatus.SC_OK);
  }

  /**
   * @param id - id of metacard to query
   * @param transformer - transformer to use for response
   * @param username - username of auth user
   * @param password - password of auth user
   * @param expectedStatusCode - expected status code to check for
   */
  public static String queryWithBasicAuth(
      String id, String transformer, String username, String password, int expectedStatusCode) {
    ValidatableResponse response =
        given()
            .auth()
            .preemptive()
            .basic(username, password)
            .get(REST_PATH.getUrl() + id + "?transform=" + transformer)
            .then()
            .assertThat()
            .statusCode(equalTo(expectedStatusCode));
    return response.extract().body().asString();
  }

  /**
   * @param id - id of metacard to update
   * @param data - body of request to update with
   * @param mimeType - content type header value
   */
  public static void update(String id, String data, String mimeType) {
    update(id, data, mimeType, HttpStatus.SC_OK);
  }

  /**
   * @param id - id of metacard to update
   * @param data - body of request to update with
   * @param mimeType - content type header value
   * @param expectedStatusCode - expected status code to check for
   */
  public static void update(String id, String data, String mimeType, int expectedStatusCode) {
    given()
        .header(HttpHeaders.CONTENT_TYPE, mimeType)
        .body(data)
        .expect()
        .log()
        .ifValidationFails(LogDetail.ALL)
        .statusCode(expectedStatusCode)
        .put(new AbstractIntegrationTest.DynamicUrl(REST_PATH, id).getUrl());
  }

  /**
   * @param id - id of metacard to update
   * @param mimeType - content type header value
   * @param username - username of auth user
   * @param password - password of auth user
   */
  public static void updateWithBasicAuth(
      String id, String data, String mimeType, String username, String password) {
    updateWithBasicAuth(id, data, mimeType, username, password, HttpStatus.SC_OK);
  }

  /**
   * @param id - id of metacard to update
   * @param mimeType - content type header value
   * @param username - username of auth user
   * @param password - password of auth user
   * @param expectedStatusCode - expected status code to check for
   */
  public static void updateWithBasicAuth(
      String id,
      String data,
      String mimeType,
      String username,
      String password,
      int expectedStatusCode) {
    given()
        .header(HttpHeaders.CONTENT_TYPE, mimeType)
        .auth()
        .preemptive()
        .basic(username, password)
        .body(data)
        .expect()
        .log()
        .ifValidationFails(LogDetail.ALL)
        .statusCode(expectedStatusCode)
        .put(new AbstractIntegrationTest.DynamicUrl(REST_PATH, id).getUrl());
  }

  /** @param id - id of metacard to update */
  public static void delete(String id) {
    delete(id, HttpStatus.SC_OK);
  }

  /**
   * @param id - id of metacard to update
   * @param expectedStatusCode - expected status code to check for
   */
  public static void delete(String id, int expectedStatusCode) {
    given().delete(REST_PATH + id).then().assertThat().statusCode(expectedStatusCode);
  }

  /**
   * @param id - id of metacard to update
   * @param username - username of auth user
   * @param password - password of auth user
   */
  public static void deleteWithBasicAuth(String id, String username, String password) {
    deleteWithBasicAuth(id, username, password, HttpStatus.SC_OK);
  }

  /**
   * @param id - id of metacard to update
   * @param username - username of auth user
   * @param password - password of auth user
   * @param expectedStatusCode - expected status code to check for
   */
  public static void deleteWithBasicAuth(
      String id, String username, String password, int expectedStatusCode) {
    given()
        .auth()
        .preemptive()
        .basic(username, password)
        .when()
        .delete(REST_PATH + id)
        .then()
        .assertThat()
        .statusCode(expectedStatusCode);
  }

  /**
   * Deletes a metacard by id and then waits until it has been removed from the catalog before
   * returning.
   *
   * @param id metacard id to delete
   */
  public static void deleteMetacardAndWait(String id) {
    delete(id);
    with()
        .pollInterval(1, SECONDS)
        .await()
        .atMost(AbstractIntegrationTest.GENERIC_TIMEOUT_SECONDS, SECONDS)
        .ignoreExceptions()
        .until(() -> !doesMetacardExist(id));
  }

  /**
   * Uses ids within the responses to delete
   *
   * @param response - response with ids of metacards to delete
   */
  public static void deleteMetacardUsingCswResponseId(Response response)
      throws IOException, XPathExpressionException {
    String id = getMetacardIdFromCswInsertResponse(response);
    delete(id);
  }
}
