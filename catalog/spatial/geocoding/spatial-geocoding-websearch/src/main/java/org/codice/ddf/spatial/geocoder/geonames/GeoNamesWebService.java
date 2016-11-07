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

import static org.apache.commons.lang.Validate.notNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;

import javax.ws.rs.WebApplicationException;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.spatial.geocoder.GeoCoder;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoder.GeoResultCreator;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.codice.ddf.spatial.geocoding.context.impl.NearbyLocationImpl;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.SpatialContextFactory;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.impl.PointImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class GeoNamesWebService implements GeoCoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesWebService.class);

    //geonames requires an application username, this is the default name for DDF
    private static final String USERNAME = "ddf_ui";

    private static final String GEONAMES_API_ADDRESS = "api.geonames.org";

    private static final String GEONAMES_PROTOCOL = "http";

    private static final String GEONAMES_KEY = "geonames";

    private static final String GEONAMES_COUNTRYCODE = "countryCode";

    private static final String LAT_KEY = "lat";

    private static final String LON_KEY = "lng";

    private static final String POPULATION_KEY = "population";

    private static final String ADMIN_CODE_KEY = "fcode";

    private static final String PLACENAME_KEY = "name";

    @Override
    public GeoResult getLocation(String location) {

        location = getUrlEncodedLocation(location);

        String urlStr = String.format("%s://%s/searchJSON?q=%s&username=%s",
                GEONAMES_PROTOCOL,
                GEONAMES_API_ADDRESS,
                location,
                USERNAME);

        Object result = query(urlStr);

        if (result != null) {
            if (result instanceof JSONObject) {
                JSONObject jsonResult = (JSONObject) result;
                JSONArray geonames = (JSONArray) jsonResult.get(GEONAMES_KEY);
                if (geonames != null && geonames.size() > 0) {
                    JSONObject firstResult = (JSONObject) geonames.get(0);
                    if (firstResult != null) {
                        double lat = Double.valueOf((String) firstResult.get(LAT_KEY));
                        double lon = Double.valueOf((String) firstResult.get(LON_KEY));

                        Long population = (Long) firstResult.get(POPULATION_KEY);
                        String adminCode = (String) firstResult.get(ADMIN_CODE_KEY);

                        return GeoResultCreator.createGeoResult((String) firstResult.get(
                                PLACENAME_KEY), lat, lon, adminCode, population);
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
            response = client.acceptEncoding(StandardCharsets.UTF_8.name())
                    .accept("application/json")
                    .get(String.class);
        } catch (WebApplicationException e) {
            LOGGER.debug("Error while making geonames request.", e);
            return null;
        }

        Object result = null;

        try {
            JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
            result = parser.parse(response);
        } catch (ParseException e) {
            LOGGER.debug("Error while parsing JSON message from Geonames service.", e);
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
            LOGGER.debug("Unable to encode location.", e);
        }

        return location;
    }

    @Override
    public NearbyLocation getNearbyCity(String locationWkt) {
        notNull(locationWkt, "argument locationWkt may not be null");

        Point wktCenterPoint = createPointFromWkt(locationWkt);

        String urlStr = String.format(
                "%s://%s/findNearbyPlaceNameJSON?lat=%f&lng=%f&maxRows=1&username=%s&cities=cities5000",
                GEONAMES_PROTOCOL,
                GEONAMES_API_ADDRESS,
                wktCenterPoint.getY(),
                wktCenterPoint.getX(),
                USERNAME);

        Object result = query(urlStr);

        if (result instanceof JSONObject) {
            JSONObject jsonResult = (JSONObject) result;
            JSONArray geonames = (JSONArray) jsonResult.get(GEONAMES_KEY);
            if (geonames != null && geonames.size() > 0) {
                JSONObject firstResult = (JSONObject) geonames.get(0);
                if (firstResult != null) {
                    double lat = Double.valueOf((String) firstResult.get(LAT_KEY));
                    double lon = Double.valueOf((String) firstResult.get(LON_KEY));
                    String cityName = (String) firstResult.get(PLACENAME_KEY);
                    Point cityPoint = new PointImpl(lon, lat, SpatialContext.GEO);

                    return new NearbyLocationImpl(wktCenterPoint, cityPoint, cityName);
                }
            }
        }

        return null;
    }

    @Override
    public Optional<String> getCountryCode(String locationWkt, int radius) {
        notNull(locationWkt, "argument locationWkt may not be null");

        Point wktCenterPoint = createPointFromWkt(locationWkt);
        String urlStr = String.format(
                "%s://%s/countryCode?lat=%f&lng=%f&radius=%d&type=JSON&username=%s",
                GEONAMES_PROTOCOL,
                GEONAMES_API_ADDRESS,
                wktCenterPoint.getY(),
                wktCenterPoint.getX(),
                radius,
                USERNAME);

        Object result = query(urlStr);

        if (result instanceof JSONObject) {
            JSONObject jsonResult = (JSONObject) result;
            Object countryCode = jsonResult.get(GEONAMES_COUNTRYCODE);
            if (countryCode != null) {
                String alpha2CountryCode = (String) countryCode;
                if (StringUtils.isNotEmpty(alpha2CountryCode)) {
                    try {
                        String alpha3CountryCode = new Locale(Locale.ENGLISH.getLanguage(),
                                alpha2CountryCode).getISO3Country();
                        return Optional.of(alpha3CountryCode);
                    } catch (MissingResourceException e) {
                        LOGGER.debug(
                                "Failed to convert country code {} to alpha-3 format. Returning "
                                        + "empty value",
                                alpha2CountryCode);
                    }
                }
            }
        }

        return Optional.empty();
    }

    Point createPointFromWkt(String wkt) {
        try {
            SpatialContextFactory contextFactory = new JtsSpatialContextFactory();
            SpatialContext spatialContext = contextFactory.newSpatialContext();
            Shape shape = (Shape) spatialContext.readShapeFromWkt(wkt);
            Point center = shape.getCenter();
            return center;
        } catch (java.text.ParseException parseException) {
            LOGGER.debug(parseException.getMessage(), parseException);
        }

        return null;
    }
}
