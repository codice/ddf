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
package org.codice.ddf.spatial.geocoder.geonames;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.WebApplicationException;

import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.spatial.geocoder.GeoCoder;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoder.GeoResultCreator;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.codice.ddf.spatial.geocoding.context.impl.NearbyLocationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.context.SpatialContextFactory;
import com.spatial4j.core.context.jts.JtsSpatialContextFactory;
import com.spatial4j.core.shape.Point;
import com.spatial4j.core.shape.Shape;
import com.spatial4j.core.shape.impl.PointImpl;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class GeoNamesWebService implements GeoCoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesWebService.class);

    //geonames requires an application username, this is the default name for DDF
    protected String username = "ddf_ui";

    protected String geoNamesApiServer = "api.geonames.org";

    protected String geoNamesProtocol = "http";

    @Override
    public GeoResult getLocation(String location) {

        location = getUrlEncodedLocation(location);

        String urlStr = String.format("%s://%s/searchJSON?q=%s&username=%s", geoNamesProtocol,
                geoNamesApiServer, location, username);

        Object result = query(urlStr);

        if (result != null) {
            if (result instanceof JSONObject) {
                JSONObject jsonResult = (JSONObject) result;
                JSONArray geonames = (JSONArray) jsonResult.get("geonames");
                if (geonames != null && geonames.size() > 0) {
                    JSONObject firstResult = (JSONObject) geonames.get(0);
                    if (firstResult != null) {
                        double lat = Double.valueOf((String) firstResult.get("lat"));
                        double lon = Double.valueOf((String) firstResult.get("lng"));

                        Long population = (Long) firstResult.get("population");
                        String adminCode = (String) firstResult.get("fcode");

                        return  GeoResultCreator
                                .createGeoResult((String)firstResult.get("name"), lat, lon, adminCode, population);
                    }
                }
            }
        }

        return null;
    }

    private Object query(String urlStr) {
        String response = null;

        try {
            WebClient client = createWebClient(urlStr);
            response = client.acceptEncoding(StandardCharsets.UTF_8.name()).accept("application/json").get(
                    String.class);
        } catch (WebApplicationException e) {
            LOGGER.error("Error while making geonames request.", e);
            return null;
        }

        Object result = null;

        try {
            JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
            result = parser.parse(response);
        } catch (ParseException e) {
            LOGGER.error("Error while parsing JSON message from Geonames service.", e);
        }

        return result;
    }

    WebClient createWebClient(String urlStr) {
        return WebClient.create(urlStr);
    }

    String getUrlEncodedLocation(String location) {

        try {
            location = URLEncoder.encode(location, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("Unable to encode location.", e);
        }

        return location;
    }

    public NearbyLocation getNearbyCity(String locationWkt) {
        if (locationWkt == null) {
            throw new IllegalArgumentException("argument 'locationWkt' may not be null.");
        }

        Point wktCenterPoint = createPointFromWkt(locationWkt);

        String urlStr = String.format("%s://%s/findNearbyPlaceNameJSON?lat=%f&lng=%f&maxRows=1&username=%s",
                geoNamesProtocol, geoNamesApiServer, wktCenterPoint.getY(), wktCenterPoint.getX(), username);

        Object result = query(urlStr);

        if (result instanceof JSONObject) {
            JSONObject jsonResult = (JSONObject) result;
            JSONArray geonames = (JSONArray) jsonResult.get("geonames");
            if (geonames != null && geonames.size() > 0) {
                JSONObject firstResult = (JSONObject) geonames.get(0);
                if (firstResult != null) {
                    double lat = Double.valueOf((String) firstResult.get("lat"));
                    double lon = Double.valueOf((String) firstResult.get("lng"));
                    String cityName = (String) firstResult.get("adminName1");
                    Point cityPoint = new PointImpl(lon, lat, SpatialContext.GEO);

                    return new NearbyLocationImpl(wktCenterPoint, cityPoint, cityName);
                }
            }
        }

        return null;
    }

    Point createPointFromWkt(String wkt) {
        try {
            SpatialContextFactory contextFactory = new JtsSpatialContextFactory();
            SpatialContext spatialContext = contextFactory.newSpatialContext();
            Shape shape = (Shape) spatialContext.readShapeFromWkt(wkt);
            Point center = shape.getCenter();
            return center;
        } catch (java.text.ParseException parseException) {
            LOGGER.error(parseException.getMessage(), parseException);
        }

        return null;
    }
}
