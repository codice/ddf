/**
 * Copyright (c) Codice Foundation
<<<<<<< HEAD
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
=======
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
>>>>>>> master
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.geocoder.geonames;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
<<<<<<< HEAD
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
=======
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
>>>>>>> master
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

<<<<<<< HEAD
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.junit.Before;
import org.junit.Test;

import com.spatial4j.core.shape.Point;

public class TestGeoNamesWebService {
    private static final String QUERY_TEST_RESPONSE =
=======
import java.io.IOException;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.spatial4j.shape.Point;

public class TestGeoNamesWebService {
    private static final String NEARBY_CITY_QUERY_TEST_RESPONSE =
>>>>>>> master
            "{ \"geonames\": [ " + "              { \"countryId\": \"357994\", "
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
                    + "                \"population\": 0 " + "              } " + "           ] "
                    + "} ";

<<<<<<< HEAD
=======
    private static final String COUNRTY_CODE_QUERY_TEST_RESPONSE =
            "{\"languages\":\"ca\",\"distance\":\"0\",\"countryCode\":\"AD\",\"countryName\":\"Andorra\"}";

>>>>>>> master
    private static final String QUERY_TEST_LOCATION = "POINT(32 25)";

    private static final String CREATE_POINT_FROM_WKT_POINT = "POINT(-1.0 22)";

    private static final String CREATE_POINT_FROM_WKT_POLYGON =
            "POLYGON((30 10, 10 20, 20 40, 40 40, 30 10))";

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
<<<<<<< HEAD
    public void testGetNearbyCity() {
        WebClient webClientMock = mock(WebClient.class);
        when(webClientMock.acceptEncoding(anyString())).thenReturn(webClientMock);
        when(webClientMock.accept(anyString())).thenReturn(webClientMock);
        when(webClientMock.get(String.class)).thenReturn(QUERY_TEST_RESPONSE);
        doReturn(webClientMock).when(webServiceSpy)
                .createWebClient(anyString());
=======
    public void testCreateGeoPointFromWktParseException() {
        String pointWkt = "i am not well known";

        Point p = webService.createPointFromWkt(pointWkt);
        assertThat(p, nullValue());
    }

    @Test
    public void testGetLocation() throws IOException {
        String response = IOUtils.toString(TestGeoNamesWebService.class.getClassLoader()
                .getResourceAsStream("getLocationTestResponse.json"));

        WebClient webClientMock = mock(WebClient.class);
        prepareWebClient(webClientMock, response);

        GeoResult result = webServiceSpy.getLocation("Phoenix");
        assertThat(result.getFullName(), is("Phoenix"));
    }

    @Test
    public void testGetNearbyCity() {
        WebClient webClientMock = mock(WebClient.class);
        prepareWebClient(webClientMock, NEARBY_CITY_QUERY_TEST_RESPONSE);
>>>>>>> master

        NearbyLocation nearbyLocation = webServiceSpy.getNearbyCity(QUERY_TEST_LOCATION);
        assertThat(nearbyLocation.getCardinalDirection(), equalTo("W"));
        assertThat(nearbyLocation.getDistance(), greaterThan(14.0));
        assertThat(nearbyLocation.getDistance(), lessThan(15.0));
        assertThat(nearbyLocation.getName(), equalTo("Qaryat Wādī ‘Abbādī 2"));
    }
<<<<<<< HEAD
=======

    @Test
    public void testGetCountryCode() {
        WebClient webClientMock = mock(WebClient.class);
        prepareWebClient(webClientMock, COUNRTY_CODE_QUERY_TEST_RESPONSE);

        Optional<String> countryCode = webServiceSpy.getCountryCode(QUERY_TEST_LOCATION, 10);
        assertThat(countryCode.get(), is("AND"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetCountryCodeNoLocation() {
        WebClient webClientMock = mock(WebClient.class);
        prepareWebClient(webClientMock, COUNRTY_CODE_QUERY_TEST_RESPONSE);

        webServiceSpy.getCountryCode(null, 10);
    }

    private void prepareWebClient(WebClient mockedWebClient, String webResponse) {
        when(mockedWebClient.acceptEncoding(anyString())).thenReturn(mockedWebClient);
        when(mockedWebClient.accept(anyString())).thenReturn(mockedWebClient);
        when(mockedWebClient.get(String.class)).thenReturn(webResponse);
        doReturn(mockedWebClient).when(webServiceSpy)
                .createWebClient(anyString());
    }
>>>>>>> master
}
