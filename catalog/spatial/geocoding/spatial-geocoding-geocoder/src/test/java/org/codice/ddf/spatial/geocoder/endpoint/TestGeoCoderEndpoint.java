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

package org.codice.ddf.spatial.geocoder.endpoint;

import static org.hamcrest.CoreMatchers.is;

import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.codice.ddf.spatial.geocoder.GeoCoder;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoder.GeoResultCreator;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.util.impl.ServiceSelector;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class TestGeoCoderEndpoint {

    private ServiceSelector<GeoCoder> mockGeoCoderFactory;
    private GeoCoder mockGeoCoder;
    private GeoResult geoResult;
    private GeoCoderEndpoint geoCoderEndpoint;
    private NearbyLocation nearbyLocation;

    private JSONParser jsonParser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);

    @Before
    public void setUp() {
        this.mockGeoCoderFactory = buildMockGeoCoderFactory();
        this.mockGeoCoder = buildMockGeoCoder();
        this.geoResult = buildGeoResult("Phoenix", 0, 0.389, "ADM3", 100000);
        this.geoCoderEndpoint = new GeoCoderEndpoint(mockGeoCoderFactory);

        when(mockGeoCoderFactory.getService()).thenReturn(mockGeoCoder);
        when(mockGeoCoder.getLocation(anyString())).thenReturn(geoResult);

        nearbyLocation = mock(NearbyLocation.class);
        when(nearbyLocation.getName()).thenReturn("Phoenix");
        when(nearbyLocation.getDistance()).thenReturn(23.45);
        when(nearbyLocation.getCardinalDirection()).thenReturn("N");

        when(mockGeoCoder.getNearbyCity(anyString())).thenReturn(nearbyLocation);
        when(mockGeoCoder.getNearbyCity(null)).thenReturn(null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testConstructorException() {
        new GeoCoderEndpoint(null);
    }

    @Test
    public void testQuery() {
        JSONObject jsonObject = this.geoCoderEndpoint.doQuery("Phoenix");
        JSONArray resourceSets = (JSONArray) jsonObject.get("resourceSets");
        assertThat(resourceSets.size(), is(1));

        JSONObject resources = (JSONObject) resourceSets.get(0);
        JSONArray resourceElements = (JSONArray) resources.get("resources");
        assertThat(resourceElements.size(), is(1));

        JSONObject resource = (JSONObject) resourceElements.get(0);
        JSONObject point = (JSONObject) resource.get("point");
        JSONArray bbox = (JSONArray) resource.get("bbox");
        String name = (String) resource.get("name");

        assertThat(name, is("Phoenix"));
        assertThat(bbox.size(), is(4));

        String type = (String) point.get("type");
        assertThat(type, is("Point"));

        JSONArray coordinates = (JSONArray) point.get("coordinates");
        assertThat(coordinates.size(), is(2));
    }

    @Test
    public void testNearbyLocation() throws ParseException {
        Response response = geoCoderEndpoint.getNearbyCities("POINT(10 30)");

        if (response != null) {
            String responseString = (String)response.getEntity();
            if (responseString != null) {
                JSONObject jsonObject = (JSONObject)jsonParser.parse(responseString);

                assertThat(jsonObject.get("name"), is("Phoenix"));
                assertThat(jsonObject.get("direction"), is("N"));
                assertThat(jsonObject.get("distance"), is(23.45));
            }
        }
    }

    @Test
    public void testNullNearbyLocation() {
        Response response = geoCoderEndpoint.getNearbyCities(null);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }


    private GeoCoder buildMockGeoCoder() {
        return mock(GeoCoder.class);
    }

    private ServiceSelector<GeoCoder> buildMockGeoCoderFactory() {
        ServiceSelector<GeoCoder> geoCoderFactory = mock(ServiceSelector.class);
        return geoCoderFactory;
    }

    private GeoResult buildGeoResult(final String name, final double latitude, final double longitude,
                                     final String featureCode, final long population) {
        GeoResult geoResult = GeoResultCreator.createGeoResult(name, latitude, longitude,
                featureCode, population);
        return geoResult;
    }


}
