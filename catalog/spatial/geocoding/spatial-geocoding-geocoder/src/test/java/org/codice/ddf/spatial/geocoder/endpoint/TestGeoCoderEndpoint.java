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

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.codice.ddf.spatial.geocoder.GeoCoder;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoder.GeoResultCreator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import static org.mockito.Mockito.*;

public class TestGeoCoderEndpoint {
    private GeoCoderFactory mockGeoCoderFactory;
    private GeoCoder mockGeoCoder;
    private GeoResult geoResult;
    private GeoCoderEndpoint geoCoderEndpoint;

    @Before
    public void testSetup() {
        this.mockGeoCoderFactory = buildMockGeoCoderFactory();
        this.mockGeoCoder = buildMockGeoCoder();
        this.geoResult = buildGeoResult("Phoenix", 0, 0.389, "ADM3", 100000);
        this.geoCoderEndpoint = new GeoCoderEndpoint(mockGeoCoderFactory);

        when(mockGeoCoderFactory.getService()).thenReturn(mockGeoCoder);
        when(mockGeoCoder.getLocation(anyString())).thenReturn(geoResult);
    }


    @Test(expected=IllegalArgumentException.class)
    public void testConstructorException() {
        new GeoCoderEndpoint(null);
    }

    @Test
    public void testQuery() {
        JSONObject jsonObject = this.geoCoderEndpoint.doQuery("Phoenix");
        JSONArray resourceSets = (JSONArray) jsonObject.get("resourceSets");
        assertEquals(1, resourceSets.size());

        JSONObject resources = (JSONObject) resourceSets.get(0);
        JSONArray resourceElements = (JSONArray) resources.get("resources");
        assertEquals(1, resourceElements.size());

        JSONObject resource = (JSONObject) resourceElements.get(0);
        JSONObject point = (JSONObject) resource.get("point");
        JSONArray bbox = (JSONArray) resource.get("bbox");
        String name = (String) resource.get("name");

        assertEquals("Phoenix", name);
        assertEquals(4, bbox.size());

        String type = (String) point.get("type");
        assertEquals("Point", type);

        JSONArray coordinates = (JSONArray) point.get("coordinates");
        assertEquals(2, coordinates.size());
    }

    private GeoCoder buildMockGeoCoder() {
        return mock(GeoCoder.class);
    }

    private GeoCoderFactory buildMockGeoCoderFactory() {
        GeoCoderFactory geoCoderFactory = mock(GeoCoderFactory.class);
        return geoCoderFactory;
    }

    private GeoResult buildGeoResult(final String name, final double latitude, final double longitude,
                                     final String featureCode, final long population) {
        GeoResult geoResult = GeoResultCreator.createGeoResult(name, latitude, longitude,
                featureCode, population);
        return geoResult;
    }
}
