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
package org.codice.ddf.spatial.geocoder.geonames;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.ParseException;
import java.util.Optional;
import javax.ws.rs.WebApplicationException;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.spatial4j.shape.Point;

public class GeoNamesWebServiceTest {
  private static final String NEARBY_CITY_QUERY_TEST_RESPONSE =
      "{ \"geonames\": [ "
          + "              { \"countryId\": \"357994\", "
          + "                \"adminCode1\": \"16\", "
          + "                \"countryName\": \"Egypt\", "
          + "                \"fclName\": \"city, village,...\", "
          + "                \"countryCode\": \"EG\", "
          + "                \"lng\": \"32.14722\", "
          + "                \"fcodeName\": \"populated place\", "
          + "                \"distance\": \"14.89021\", "
          + "                \"toponymName\": \"Qaryat Wādī ‘Abbādī 2\", "
          + "                \"fcl\": \"P\", "
          + "                \"name\": \"Qaryat Wādī ‘Abbādī 2\", "
          + "                \"fcode\": \"PPL\", "
          + "                \"geonameId\": 445095, "
          + "                \"lat\": \"25.00833\", "
          + "                \"adminName1\": \"Aswān\", "
          + "                \"population\": 0 "
          + "              } "
          + "           ] "
          + "} ";

  private static final String COUNRTY_CODE_QUERY_TEST_RESPONSE =
      "{\"languages\":\"ca\",\"distance\":\"0\",\"countryCode\":\"AD\",\"countryName\":\"Andorra\"}";

  private static final String QUERY_TEST_LOCATION = "POINT(32 25)";

  private static final String CREATE_POINT_FROM_WKT_POINT = "POINT(-1.0 22)";

  private static final String CREATE_POINT_FROM_WKT_POLYGON =
      "POLYGON((30 10, 10 20, 20 40, 40 40, 30 10))";

  private static final String CREATE_POINT_FROM_WKT_GEOMETRY_COLLECTION =
      "GEOMETRYCOLLECTION (MULTIPOLYGON (((56 9, 64 9, 60 14, 56 9)), ((61 9, 69 9, 65 14, 61 9)), ((51 9, 59 9, 55 14, 51 9))), LINESTRING (50 8, 50 15, 70 15, 70 8, 50 8), MULTIPOINT ((62.5 14), (67.5 14), (57.5 14), (52.5 14)))";

  private GeoNamesWebService webService;

  private GeoNamesWebService webServiceSpy;

  @Before
  public void setUp() {
    this.webService = new GeoNamesWebService();
    this.webServiceSpy = spy(webService);
  }

  @Test
  public void testCreateGeoPointFromWkt() {
    String pointWkt = CREATE_POINT_FROM_WKT_POINT;

    Point p = webService.createPointFromWkt(pointWkt);

    assertThat(p, notNullValue());
    assertThat(p.getX(), equalTo(-1.0));
    assertThat(p.getY(), equalTo(22.0));

    String polygonWkt = CREATE_POINT_FROM_WKT_POLYGON;

    p = webService.createPointFromWkt(polygonWkt);

    assertThat(p, notNullValue());
  }

  @Test
  public void testIntersectingGeometryCollection() {
    assertThat(
        webService.createPointFromWkt(CREATE_POINT_FROM_WKT_GEOMETRY_COLLECTION), notNullValue());
  }

  @Test
  public void testCreateGeoPointFromWktParseException() {
    String pointWkt = "i am not well known";

    Point p = webService.createPointFromWkt(pointWkt);
    assertThat(p, nullValue());
  }

  @Test
  public void testGetLocation() throws IOException, GeoEntryQueryException {
    String response =
        IOUtils.toString(
            GeoNamesWebServiceTest.class
                .getClassLoader()
                .getResourceAsStream("getLocationTestResponse.json"));

    prepareWebClient(response);

    GeoEntry result = webServiceSpy.query("Phoenix", 0).get(0);
    assertThat(result.getName(), is("Phoenix"));
  }

  @Test
  public void testGetNearbyCity() throws ParseException, GeoEntryQueryException {
    prepareWebClient(NEARBY_CITY_QUERY_TEST_RESPONSE);

    NearbyLocation nearbyLocation =
        webServiceSpy.getNearestCities(QUERY_TEST_LOCATION, 0, 1).get(0);
    assertThat(nearbyLocation.getCardinalDirection(), equalTo("W"));
    assertThat(nearbyLocation.getDistance(), greaterThan(14.0));
    assertThat(nearbyLocation.getDistance(), lessThan(15.0));
    assertThat(nearbyLocation.getName(), equalTo("Qaryat Wādī ‘Abbādī 2"));
  }

  @Test
  public void testGetCountryCode() {
    prepareWebClient(COUNRTY_CODE_QUERY_TEST_RESPONSE);

    Optional<String> countryCode = webServiceSpy.getCountryCode(QUERY_TEST_LOCATION, 10);
    assertThat(countryCode.get(), is("AND"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetCountryCodeNoLocation() {
    prepareWebClient(COUNRTY_CODE_QUERY_TEST_RESPONSE);

    webServiceSpy.getCountryCode(null, 10);
  }

  @Test
  public void testGeoNamesQueryNoInternetConnection() {
    WebClient mockWebClient = mock(WebClient.class);
    when(mockWebClient.acceptEncoding(anyString())).thenReturn(mockWebClient);
    when(mockWebClient.accept(anyString())).thenReturn(mockWebClient);
    doThrow(WebApplicationException.class).when(mockWebClient).get(String.class);
    doReturn(mockWebClient).when(webServiceSpy).createWebClient(anyString());

    Optional<String> countryCode = webServiceSpy.getCountryCode(QUERY_TEST_LOCATION, 10);
    assertThat(countryCode, is(Optional.empty()));
  }

  private void prepareWebClient(String webResponse) {
    WebClient mockWebClient = mock(WebClient.class);
    when(mockWebClient.acceptEncoding(anyString())).thenReturn(mockWebClient);
    when(mockWebClient.accept(anyString())).thenReturn(mockWebClient);
    when(mockWebClient.get(String.class)).thenReturn(webResponse);
    doReturn(mockWebClient).when(webServiceSpy).createWebClient(anyString());
  }
}
