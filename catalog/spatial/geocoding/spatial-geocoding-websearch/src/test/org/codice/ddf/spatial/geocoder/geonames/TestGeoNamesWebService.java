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

package org.codice.ddf.spatial.geocoder.geonames;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.junit.Before;
import org.junit.Test;

import com.spatial4j.core.shape.Point;

public class TestGeoNamesWebService {
    private static final String QUERY_TEST_RESPONSE =
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

    private static final String QUERY_TEST_LOCATION = "POINT(32 25)";

    private static final String CREATE_POINT_FROM_WKT_POINT = "POINT(-1.0 22)";

    private static final String CREATE_POINT_FROM_WKT_POLYGON = "POLYGON((30 10, 10 20, 20 40, 40 40, 30 10))";

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
    public void testQuery() {
        WebClient webClientMock = mock(WebClient.class);
        when(webClientMock.acceptEncoding(anyString())).thenReturn(webClientMock);
        when(webClientMock.accept(anyString())).thenReturn(webClientMock);
        when(webClientMock.get(String.class)).thenReturn(QUERY_TEST_RESPONSE);
        doReturn(webClientMock).when(webServiceSpy).createWebClient(anyString());

        NearbyLocation nearbyLocation = webServiceSpy.getNearbyCity(QUERY_TEST_LOCATION);
        assertThat(nearbyLocation.getCardinalDirection(), equalTo("W"));
        assertThat(nearbyLocation.getDistance(), greaterThan(14.0));
        assertThat(nearbyLocation.getDistance(), lessThan(15.0));
        assertThat(nearbyLocation.getName(), equalTo("Aswān"));
    }
}
