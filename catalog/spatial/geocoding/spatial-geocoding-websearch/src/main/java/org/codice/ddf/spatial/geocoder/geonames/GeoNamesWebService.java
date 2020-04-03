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

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.Validate.notNull;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.codice.ddf.spatial.geocoding.context.impl.NearbyLocationImpl;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.impl.PointImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoNamesWebService implements GeoEntryQueryable {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesWebService.class);

  // geonames requires an application username, this is the default name for DDF
  private static final String USERNAME = "ddf_ui";

  private static final String GEONAMES_API_ADDRESS = "api.geonames.org";

  private static final String GEONAMES_PROTOCOL = "http";

  private static final String GEONAMES_KEY = "geonames";

  private static final String GEONAMES_COUNTRYCODE = "countryCode";

  private static final int GEONAMES_MAX_ROWS = 1000;

  private static final String LAT_KEY = "lat";

  private static final String LON_KEY = "lng";

  private static final String POPULATION_KEY = "population";

  private static final String ADMIN_CODE_KEY = "fcode";

  private static final String PLACENAME_KEY = "name";

  @Override
  public List<GeoEntry> query(String queryString, int maxResults) throws GeoEntryQueryException {

    String location = getUrlEncodedLocation(queryString);

    String urlStr =
        String.format(
            "%s://%s/searchJSON?q=%s&username=%s&maxRows=%d",
            GEONAMES_PROTOCOL, GEONAMES_API_ADDRESS, location, USERNAME, limitMaxRows(maxResults));

    Object result = webQuery(urlStr);

    if (result != null) {
      if (result instanceof JSONObject) {
        JSONObject jsonResult = (JSONObject) result;
        JSONArray geoNames = (JSONArray) jsonResult.get(GEONAMES_KEY);
        if (geoNames != null) {
          return geoNames.stream()
              .map(JSONObject.class::cast)
              .map(
                  obj ->
                      new GeoEntry.Builder()
                          .name((String) obj.get(PLACENAME_KEY))
                          .population((Long) obj.get(POPULATION_KEY))
                          .featureCode((String) obj.get(ADMIN_CODE_KEY))
                          .latitude(Double.valueOf((String) obj.get(LAT_KEY)))
                          .longitude(Double.valueOf((String) obj.get(LON_KEY)))
                          .build())
              .collect(toList());
        }
      }
    }

    return Collections.emptyList();
  }

  @Override
  public GeoEntry queryById(String id) throws GeoEntryQueryException {
    LOGGER.debug("Query by ID not query not implemented.");
    return null;
  }

  @Override
  public List<Suggestion> getSuggestedNames(String queryString, int maxResults)
      throws GeoEntryQueryException {
    LOGGER.debug("Suggestion query not implemented.");
    return Collections.emptyList();
  }

  @Override
  public List<NearbyLocation> getNearestCities(String locationWkt, int radiusInKm, int maxResults)
      throws java.text.ParseException, GeoEntryQueryException {
    notNull(locationWkt, "argument locationWkt may not be null");

    Point wktCenterPoint = createPointFromWkt(locationWkt);

    String urlStr =
        String.format(
            "%s://%s/findNearbyPlaceNameJSON?lat=%f&lng=%f&maxRows=%d&radius=%d&username=%s&cities=cities5000",
            GEONAMES_PROTOCOL,
            GEONAMES_API_ADDRESS,
            wktCenterPoint.getY(),
            wktCenterPoint.getX(),
            limitMaxRows(maxResults),
            radiusInKm,
            USERNAME);

    Object result = webQuery(urlStr);

    if (result instanceof JSONObject) {
      JSONObject jsonResult = (JSONObject) result;
      JSONArray geoNames = (JSONArray) jsonResult.get(GEONAMES_KEY);
      if (geoNames != null) {
        return geoNames.stream()
            .map(JSONObject.class::cast)
            .map(
                obj -> {
                  double lat = Double.parseDouble((String) obj.get(LAT_KEY));
                  double lon = Double.parseDouble((String) obj.get(LON_KEY));
                  String cityName = (String) obj.get(PLACENAME_KEY);
                  Point cityPoint = new PointImpl(lon, lat, SpatialContext.GEO);
                  return new NearbyLocationImpl(wktCenterPoint, cityPoint, cityName);
                })
            .collect(toList());
      }
    }

    return Collections.emptyList();
  }

  @Override
  public Optional<String> getCountryCode(String locationWkt, int radius) {
    notNull(locationWkt, "argument locationWkt may not be null");

    Point wktCenterPoint = createPointFromWkt(locationWkt);
    String urlStr =
        String.format(
            "%s://%s/countryCode?lat=%f&lng=%f&radius=%d&type=JSON&username=%s",
            GEONAMES_PROTOCOL,
            GEONAMES_API_ADDRESS,
            wktCenterPoint.getY(),
            wktCenterPoint.getX(),
            radius,
            USERNAME);

    Object result = webQuery(urlStr);

    if (result instanceof JSONObject) {
      JSONObject jsonResult = (JSONObject) result;
      Object countryCode = jsonResult.get(GEONAMES_COUNTRYCODE);
      if (countryCode != null) {
        String alpha2CountryCode = (String) countryCode;
        if (StringUtils.isNotEmpty(alpha2CountryCode)) {
          try {
            String alpha3CountryCode =
                new Locale(Locale.ENGLISH.getLanguage(), alpha2CountryCode).getISO3Country();
            return Optional.of(alpha3CountryCode);
          } catch (MissingResourceException e) {
            LOGGER.debug(
                "Failed to convert country code {} to alpha-3 format. Returning " + "empty value",
                alpha2CountryCode);
          }
        }
      }
    }

    return Optional.empty();
  }

  Point createPointFromWkt(String wkt) {
    try {
      JtsSpatialContextFactory contextFactory = new JtsSpatialContextFactory();
      contextFactory.allowMultiOverlap = true;
      SpatialContext spatialContext = contextFactory.newSpatialContext();
      Shape shape = (Shape) spatialContext.readShapeFromWkt(wkt);
      Point center = shape.getCenter();
      return center;
    } catch (java.text.ParseException parseException) {
      LOGGER.debug(parseException.getMessage(), parseException);
    }

    return null;
  }

  private int limitMaxRows(int value) {
    return value > GEONAMES_MAX_ROWS ? GEONAMES_MAX_ROWS : value;
  }

  private Object webQuery(String urlStr) {
    final String response;

    try {
      WebClient client = createWebClient(urlStr);
      response =
          client
              .acceptEncoding(StandardCharsets.UTF_8.name())
              .accept(MediaType.APPLICATION_JSON)
              .get(String.class);
    } catch (WebApplicationException | ProcessingException e) {
      LOGGER.debug("Error while making GeoNames request: {}", urlStr, e);
      return null;
    }

    try {
      JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
      return parser.parse(response);
    } catch (ParseException e) {
      LOGGER.debug("Error while parsing JSON message from GeoNames service.", e);
      return null;
    }
  }

  WebClient createWebClient(String urlStr) {
    return WebClient.create(urlStr);
  }

  String getUrlEncodedLocation(String location) {

    try {
      location = URLEncoder.encode(location, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      LOGGER.debug("Unable to encode location: {}", location, e);
    }

    return location;
  }
}
