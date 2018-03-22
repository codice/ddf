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
package ddf.catalog.source.opensearch.impl;

import ddf.catalog.data.Result;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.opensearch.OpenSearchParser;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchParserImpl implements OpenSearchParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchParserImpl.class);

  private static final String START_INDEX = "start";

  // constants for Vincenty's formula
  // length of semi-major axis of the Earth (radius at equator) = 6378137.0 metres in WGS-84
  private static final double LENGTH_OF_SEMI_MAJOR_AXIS_IN_METERS = 6378137.0;

  // flattening of the Earth = 1/298.257223563 in WGS-84
  private static final double FLATTENING = 1 / 298.257223563;

  // length of semi-minor axis of the Earth (radius at the poles) = 6356752.314245 meters in WGS-84
  private static final double LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS =
      (1 - FLATTENING) * LENGTH_OF_SEMI_MAJOR_AXIS_IN_METERS;

  private static final int MAXIMUM_VINCENTYS_FORMULA_ITERATIONS = 100;
  // end constants for Vincenty's formula

  private static final double MAX_LAT = 90;

  private static final double MIN_LAT = -90;

  private static final double MAX_LON = 180;

  private static final double MIN_LON = -180;

  private static final double FULL_LON_ROTATION = MAX_LON - MIN_LON;

  private static final String ORDER_ASCENDING = "asc";

  private static final String ORDER_DESCENDING = "desc";

  private static final String SORT_DELIMITER = ":";

  private static final String SORT_RELEVANCE = "relevance";

  private static final String SORT_TEMPORAL = "date";

  // OpenSearch defined parameters
  public static final String SEARCH_TERMS = "q";

  // temporal
  static final String TIME_START = "dtstart";

  static final String TIME_END = "dtend";

  static final String TIME_NAME = "dateName";

  // geospatial
  static final String GEO_LAT = "lat";

  static final String GEO_LON = "lon";

  static final String GEO_RADIUS = "radius";

  static final String GEO_POLY = "polygon";

  static final String GEO_BBOX = "bbox";

  // general options
  static final String SRC = "src";

  static final String MAX_RESULTS = "mr";

  static final String COUNT = "count";

  static final String MAX_TIMEOUT = "mt";

  static final String USER_DN = "dn";

  static final String SORT = "sort";

  static final String FILTER = "filter";

  static final Integer DEFAULT_TOTAL_MAX = 1000;

  @Override
  public void populateSearchOptions(
      WebClient client, QueryRequest queryRequest, Subject subject, List<String> parameters) {
    String maxTotalSize = null;
    String maxPerPage = null;
    String routeTo = "";
    String timeout = null;
    String start = "1";
    String dn = null;
    String filterStr = "";
    String sortStr = null;

    if (queryRequest != null) {
      Query query = queryRequest.getQuery();

      if (query != null) {
        maxPerPage = String.valueOf(query.getPageSize());
        if (query.getPageSize() > DEFAULT_TOTAL_MAX) {
          maxTotalSize = maxPerPage;
        } else if (query.getPageSize() <= 0) {
          maxTotalSize = String.valueOf(DEFAULT_TOTAL_MAX);
        }

        start = Integer.toString(query.getStartIndex());

        timeout = Long.toString(query.getTimeoutMillis());

        sortStr = translateToOpenSearchSort(query.getSortBy());

        if (subject != null
            && subject.getPrincipals() != null
            && !subject.getPrincipals().isEmpty()) {
          List principals = subject.getPrincipals().asList();
          for (Object principal : principals) {
            if (principal instanceof SecurityAssertion) {
              SecurityAssertion assertion = (SecurityAssertion) principal;
              Principal assertionPrincipal = assertion.getPrincipal();
              if (assertionPrincipal != null) {
                dn = assertionPrincipal.getName();
              }
            }
          }
        }
      }
    }

    checkAndReplace(client, start, START_INDEX, parameters);
    checkAndReplace(client, maxPerPage, COUNT, parameters);
    checkAndReplace(client, maxTotalSize, MAX_RESULTS, parameters);
    checkAndReplace(client, routeTo, SRC, parameters);
    checkAndReplace(client, timeout, MAX_TIMEOUT, parameters);
    checkAndReplace(client, dn, USER_DN, parameters);
    checkAndReplace(client, filterStr, FILTER, parameters);
    checkAndReplace(client, sortStr, SORT, parameters);
  }

  @Override
  public void populateContextual(
      WebClient client, Map<String, String> searchPhraseMap, List<String> parameters) {
    if (searchPhraseMap != null) {
      String queryStr = searchPhraseMap.get(SEARCH_TERMS);
      if (queryStr != null) {
        try {
          queryStr = URLEncoder.encode(queryStr, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
          LOGGER.debug("Could not encode contextual string", uee);
        }
      }
      checkAndReplace(client, queryStr, SEARCH_TERMS, parameters);
    }
  }

  @Override
  public void populateTemporal(WebClient client, TemporalFilter temporal, List<String> parameters) {
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    String start = "";
    String end = "";
    String name = "";
    if (temporal != null) {
      long startLng = (temporal.getStartDate() != null) ? temporal.getStartDate().getTime() : 0;
      start = fmt.print(startLng);
      long endLng =
          (temporal.getEndDate() != null)
              ? temporal.getEndDate().getTime()
              : System.currentTimeMillis();
      end = fmt.print(endLng);
    }
    checkAndReplace(client, start, TIME_START, parameters);
    checkAndReplace(client, end, TIME_END, parameters);
    checkAndReplace(client, name, TIME_NAME, parameters);
  }

  @Override
  public void populateGeospatial(
      WebClient client,
      SpatialDistanceFilter spatial,
      boolean shouldConvertToBBox,
      List<String> parameters) {
    String lat = "";
    String lon = "";
    String radiusStr = "";
    StringBuilder bbox = new StringBuilder();

    if (spatial != null) {
      String wktStr = spatial.getGeometryWkt();
      double radius = spatial.getDistanceInMeters();

      if (wktStr.contains("POINT")) {
        String[] latLon = createLatLonAryFromWKT(wktStr);
        lon = latLon[0];
        lat = latLon[1];
        radiusStr = Double.toString(radius);
        if (shouldConvertToBBox) {
          final Optional<double[]> bboxCoords =
              createBBoxFromPointRadius(Double.parseDouble(lon), Double.parseDouble(lat), radius);

          if (bboxCoords.isPresent()) {
            bbox.append(createBboxParamString(bboxCoords.get()));
          } else {
            LOGGER.debug(
                "Unable to calculate a bounding box for lon={} degrees, lat={} degrees, search radius={} meters. Not including {} parameter in search.",
                lon,
                lat,
                radius,
                GEO_BBOX);
          }

          lon = "";
          lat = "";
          radiusStr = "";
        }
      } else {
        LOGGER.debug("WKT ({}) not supported for POINT-RADIUS search, use POINT.", wktStr);
      }
    }

    checkAndReplace(client, lat, GEO_LAT, parameters);
    checkAndReplace(client, lon, GEO_LON, parameters);
    checkAndReplace(client, radiusStr, GEO_RADIUS, parameters);
    checkAndReplace(client, "", GEO_POLY, parameters);
    checkAndReplace(client, bbox.toString(), GEO_BBOX, parameters);
  }

  @Override
  public void populateGeospatial(
      WebClient client,
      SpatialFilter spatial,
      boolean shouldConvertToBBox,
      List<String> parameters) {
    String lat = "";
    String lon = "";
    String radiusStr = "";
    StringBuilder bbox = new StringBuilder();
    StringBuilder poly = new StringBuilder();

    if (spatial != null) {
      String wktStr = spatial.getGeometryWkt();
      if (wktStr.contains("POLYGON")) {
        String[] polyAry = createPolyAryFromWKT(wktStr);
        if (shouldConvertToBBox) {
          bbox.append(createBboxParamString(createBBoxFromPolygon(polyAry)));
        } else {
          for (int i = 0; i < polyAry.length - 1; i += 2) {
            if (i != 0) {
              poly.append(",");
            }
            poly.append(polyAry[i + 1]);
            poly.append(",");
            poly.append(polyAry[i]);
          }
        }
      } else {
        LOGGER.debug("WKT ({}) not supported for SPATIAL search, use POLYGON.", wktStr);
      }
    }

    checkAndReplace(client, lat, GEO_LAT, parameters);
    checkAndReplace(client, lon, GEO_LON, parameters);
    checkAndReplace(client, radiusStr, GEO_RADIUS, parameters);
    checkAndReplace(client, poly.toString(), GEO_POLY, parameters);
    checkAndReplace(client, bbox.toString(), GEO_BBOX, parameters);
  }

  /**
   * Parses a WKT polygon string and returns a string array containing the lon and lat.
   *
   * @param wkt WKT String in the form of POLYGON((Lon Lat, Lon Lat...))
   * @return Lon on even # and Lat on odd #
   */
  private static String[] createPolyAryFromWKT(String wkt) {
    String lonLat = wkt.substring(wkt.indexOf("((") + 2, wkt.indexOf("))"));
    return lonLat.split(" |,\\p{Space}?");
  }

  /**
   * Parses a WKT Point string and returns a string array containing the lon and lat.
   *
   * @param wkt WKT String in the form of POINT( Lon Lat)
   * @return Lon at position 0, Lat at position 1
   */
  private static String[] createLatLonAryFromWKT(String wkt) {
    String lonLat = wkt.substring(wkt.indexOf('(') + 1, wkt.indexOf(')'));
    return lonLat.split(" ");
  }

  /**
   * Checks the input and replaces the items inside of the url.
   *
   * @param client The URL to do the replacement on. <b>NOTE:</b> replacement is done directly on
   *     this object.
   * @param inputStr Item to put into the URL.
   * @param definition Area inside of the URL to be replaced by.
   */
  private static void checkAndReplace(
      WebClient client, String inputStr, String definition, List<String> parameters) {
    if (hasParameter(definition, parameters) && StringUtils.isNotEmpty(inputStr)) {
      client.replaceQueryParam(definition, inputStr);
    }
  }

  private static boolean hasParameter(String parameter, List<String> parameters) {
    for (String param : parameters) {
      if (param != null && param.equalsIgnoreCase(parameter)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Takes in a point radius search and converts it to a (rough approximation) bounding box using
   * Vincenty's formula (direct) and the WGS-84 approximation of the Earth.
   *
   * @param lonInDegrees longitude in decimal degrees (WGS-84)
   * @param latInDegrees longitude in decimal degrees (WGS-84)
   * @param searchRadiusInMeters search radius in meters
   * @return optional array of bounding box coordinates in the following order: West South East
   *     North. Also described as minX, minY, maxX, maxY (where longitude is the X-axis, and
   *     latitude is the Y-axis). Returns {@link Optional#empty()} if unable to calculate a bounding
   *     box.
   */
  private static Optional<double[]> createBBoxFromPointRadius(
      final double lonInDegrees, final double latInDegrees, final double searchRadiusInMeters) {
    final double latDifferenceInDegrees =
        Math.toDegrees(searchRadiusInMeters / LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS);

    double minLat = latInDegrees - latDifferenceInDegrees; // south
    double maxLat = latInDegrees + latDifferenceInDegrees; // north

    double minLon; // west
    double maxLon; // east

    if (minLat > MIN_LAT && maxLat < MAX_LAT) {
      final double latInRadians = Math.toRadians(latInDegrees);

      final double tanU1 = (1 - FLATTENING) * Math.tan(latInRadians);
      final double cosU1 = 1 / Math.sqrt((1 + tanU1 * tanU1));
      final double sigma1 = Math.atan2(tanU1, 0);
      final double cosSquaredAlpha = 1 - cosU1 * cosU1;
      final double uSq =
          cosSquaredAlpha
              * (LENGTH_OF_SEMI_MAJOR_AXIS_IN_METERS * LENGTH_OF_SEMI_MAJOR_AXIS_IN_METERS
                  - LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS * LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS)
              / (LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS * LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS);
      final double A = 1 + uSq / 16384 * (4096 + uSq * (-768 + uSq * (320 - 175 * uSq)));
      final double B = uSq / 1024 * (256 + uSq * (-128 + uSq * (74 - 47 * uSq)));

      double cos2sigmaM;
      double sinSigma;
      double cosSigma;
      double deltaSigma;

      double sigma = searchRadiusInMeters / (LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS * A);
      double oldSigma;

      int iterationCount = 0;
      do {
        if (iterationCount > MAXIMUM_VINCENTYS_FORMULA_ITERATIONS) {
          LOGGER.debug(
              "Vincenty's formula failed to converge after {} iterations.",
              MAXIMUM_VINCENTYS_FORMULA_ITERATIONS);
          return Optional.empty();
        }

        cos2sigmaM = Math.cos(2 * sigma1 + sigma);
        sinSigma = Math.sin(sigma);
        cosSigma = Math.cos(sigma);
        deltaSigma =
            B
                * sinSigma
                * (cos2sigmaM
                    + B
                        / 4
                        * (cosSigma * (-1 + 2 * cos2sigmaM * cos2sigmaM)
                            - B
                                / 6
                                * cos2sigmaM
                                * (-3 + 4 * sinSigma * sinSigma)
                                * (-3 + 4 * cos2sigmaM * cos2sigmaM)));
        oldSigma = sigma;
        sigma = searchRadiusInMeters / (LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS * A) + deltaSigma;

        iterationCount++;
      } while (Math.abs(sigma - oldSigma) > 1e-12);

      final double lambda = Math.atan2(sinSigma, cosU1 * cosSigma);
      final double C =
          FLATTENING / 16 * cosSquaredAlpha * (4 + FLATTENING * (4 - 3 * cosSquaredAlpha));
      final double L =
          lambda
              - (1 - C)
                  * FLATTENING
                  * cosU1
                  * (sigma
                      + C
                          * sinSigma
                          * (cos2sigmaM + C * cosSigma * (-1 + 2 * cos2sigmaM * cos2sigmaM)));

      final double xDifferenceInDegrees = Math.toDegrees(L);

      minLon = lonInDegrees - xDifferenceInDegrees;
      if (minLon < MIN_LON) {
        minLon += FULL_LON_ROTATION;
      }

      maxLon = lonInDegrees + xDifferenceInDegrees;
      if (maxLon > MAX_LON) {
        maxLon -= FULL_LON_ROTATION;
      }
    } else {
      // The search area overlaps one of the poles.
      minLat = Math.max(minLat, MIN_LAT);
      maxLat = Math.min(maxLat, MAX_LAT);
      minLon = MIN_LON;
      maxLon = MAX_LON;
    }

    // west, south, east, north
    return Optional.of(new double[] {minLon, minLat, maxLon, maxLat});
  }

  /**
   * Takes in an array of coordinates and converts it to a (rough approximation) bounding box.
   *
   * <p>Note: Searches being performed where the polygon goes through the international date line
   * may return a bad bounding box.
   *
   * @param polyAry array of coordinates (lon,lat,lon,lat,lon,lat..etc)
   * @return Array of bounding box coordinates in the following order: West South East North. Also
   *     described as minX, minY, maxX, maxY (where longitude is the X-axis, and latitude is the
   *     Y-axis).
   */
  private static double[] createBBoxFromPolygon(final String[] polyAry) {
    double minX = Double.POSITIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY;
    double maxX = Double.NEGATIVE_INFINITY;
    double maxY = Double.NEGATIVE_INFINITY;

    double curX;
    double curY;
    for (int i = 0; i < polyAry.length - 1; i += 2) {
      final String lon = polyAry[i];
      final String lat = polyAry[i + 1];
      LOGGER.debug("polyToBBox: lon - {} lat - {}", lon, lat);
      curX = Double.parseDouble(lon);
      curY = Double.parseDouble(lat);
      if (curX < minX) {
        minX = curX;
      }
      if (curX > maxX) {
        maxX = curX;
      }
      if (curY < minY) {
        minY = curY;
      }
      if (curY > maxY) {
        maxY = curY;
      }
    }
    return new double[] {minX, minY, maxX, maxY};
  }

  private static String createBboxParamString(double[] bboxCoords) {
    return Arrays.stream(bboxCoords).mapToObj(Double::toString).collect(Collectors.joining(","));
  }

  private static String translateToOpenSearchSort(SortBy ddfSort) {
    String openSearchSortStr = null;
    String orderType;

    if (ddfSort == null || ddfSort.getSortOrder() == null) {
      return null;
    }

    if (ddfSort.getSortOrder().equals(SortOrder.ASCENDING)) {
      orderType = ORDER_ASCENDING;
    } else {
      orderType = ORDER_DESCENDING;
    }

    PropertyName sortByField = ddfSort.getPropertyName();

    switch (sortByField.getPropertyName()) {
      case Result.RELEVANCE:
        // asc relevance not supported by spec
        openSearchSortStr = SORT_RELEVANCE + SORT_DELIMITER + ORDER_DESCENDING;
        break;
      case Result.TEMPORAL:
        openSearchSortStr = SORT_TEMPORAL + SORT_DELIMITER + orderType;
        break;
      default:
        LOGGER.debug(
            "Couldn't determine sort policy, not adding sorting in request to federated site.");
        break;
    }

    return openSearchSortStr;
  }
}
