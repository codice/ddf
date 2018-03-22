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

import com.google.common.annotations.VisibleForTesting;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
import ddf.catalog.data.Result;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.opensearch.OpenSearchParser;
import ddf.catalog.source.opensearch.PointRadiusSearch;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.opensearch.OpenSearchConstants;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchParserImpl implements OpenSearchParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchParserImpl.class);

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

  @VisibleForTesting static final String USER_DN = "dn";

  @VisibleForTesting static final String FILTER = "filter";

  @VisibleForTesting static final Integer DEFAULT_TOTAL_MAX = 1000;

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

    checkAndReplace(client, start, OpenSearchConstants.START_INDEX, parameters);
    checkAndReplace(client, maxPerPage, OpenSearchConstants.COUNT, parameters);
    checkAndReplace(client, maxTotalSize, OpenSearchConstants.MAX_RESULTS, parameters);
    checkAndReplace(client, routeTo, OpenSearchConstants.SOURCES, parameters);
    checkAndReplace(client, timeout, OpenSearchConstants.MAX_TIMEOUT, parameters);
    checkAndReplace(client, dn, USER_DN, parameters);
    checkAndReplace(client, filterStr, FILTER, parameters);
    checkAndReplace(client, sortStr, OpenSearchConstants.SORT, parameters);
  }

  @Override
  public void populateContextual(
      WebClient client, Map<String, String> searchPhraseMap, List<String> parameters) {
    if (searchPhraseMap != null) {
      String queryStr = searchPhraseMap.get(OpenSearchConstants.SEARCH_TERMS);
      if (queryStr != null) {
        try {
          queryStr = URLEncoder.encode(queryStr, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
          LOGGER.debug("Could not encode contextual string", uee);
        }
      }
      checkAndReplace(client, queryStr, OpenSearchConstants.SEARCH_TERMS, parameters);
    }
  }

  @Override
  public void populateTemporal(WebClient client, TemporalFilter temporal, List<String> parameters) {
    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    String start = "";
    String end = "";
    if (temporal != null) {
      long startLng = (temporal.getStartDate() != null) ? temporal.getStartDate().getTime() : 0;
      start = fmt.print(startLng);
      long endLng =
          (temporal.getEndDate() != null)
              ? temporal.getEndDate().getTime()
              : System.currentTimeMillis();
      end = fmt.print(endLng);
    }
    checkAndReplace(client, start, OpenSearchConstants.DATE_START, parameters);
    checkAndReplace(client, end, OpenSearchConstants.DATE_END, parameters);
  }

  @Override
  public void populatePointRadiusParameters(
      WebClient client,
      PointRadiusSearch pointRadiusSearch,
      boolean shouldConvertToBBox,
      List<String> parameters) {
    if (pointRadiusSearch == null) {
      return;
    }

    // point-radius search
    final double longitude = pointRadiusSearch.getLon();
    final double latitude = pointRadiusSearch.getLat();
    final double radius = pointRadiusSearch.getRadius();

    if (shouldConvertToBBox) {
      checkAndReplace(
          client,
          createBBoxStringFromPointRadius(longitude, latitude, radius),
          OpenSearchConstants.BBOX,
          parameters);
    } else {
      checkAndReplace(client, String.valueOf(latitude), OpenSearchConstants.LAT, parameters);
      checkAndReplace(client, String.valueOf(longitude), OpenSearchConstants.LON, parameters);
      checkAndReplace(client, String.valueOf(radius), OpenSearchConstants.RADIUS, parameters);
    }
  }

  @Override
  public void populatePolygonParameter(
      WebClient client, Polygon polygon, boolean shouldConvertToBBox, List<String> parameters) {
    if (polygon == null) {
      return;
    }

    if (shouldConvertToBBox) {
      checkAndReplace(
          client, createBBoxStringFromPolygon(polygon), OpenSearchConstants.BBOX, parameters);
    } else {
      checkAndReplace(
          client,
          Arrays.stream(polygon.getCoordinates())
              .flatMap(coordinate -> Stream.of(coordinate.y, coordinate.x))
              .map(String::valueOf)
              .collect(Collectors.joining(OpenSearchConstants.POLYGON_LON_LAT_DELIMITER)),
          OpenSearchConstants.POLYGON,
          parameters);
    }
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
   * @param searchRadiusInMeters
   * @return {@link String} of the bounding box coordinates in the format "west,south,east,north".
   *     Returns an empty {@link String} if unable to calculate a bounding box.
   */
  private static String createBBoxStringFromPointRadius(
      final double lonInDegrees, final double latInDegrees, final double searchRadiusInMeters) {
    final double latDifferenceInDegrees =
        Math.toDegrees(searchRadiusInMeters / LENGTH_OF_SEMI_MINOR_AXIS_IN_METERS);

    double west;
    double south = latInDegrees - latDifferenceInDegrees;
    double east;
    double north = latInDegrees + latDifferenceInDegrees;

    if (south > MIN_LAT && north < MAX_LAT) {
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
              "Vincenty's formula failed to converge after {} iterations. Unable to calculate a bounding box for lon={} degrees, lat={} degrees, search radius={} meters.",
              MAXIMUM_VINCENTYS_FORMULA_ITERATIONS,
              lonInDegrees,
              latInDegrees,
              searchRadiusInMeters);
          return null;
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

      west = lonInDegrees - xDifferenceInDegrees;
      if (west < MIN_LON) {
        west += FULL_LON_ROTATION;
      }

      east = lonInDegrees + xDifferenceInDegrees;
      if (east > MAX_LON) {
        east -= FULL_LON_ROTATION;
      }
    } else {
      // The search area overlaps one of the poles.
      west = MIN_LON;
      south = Math.max(south, MIN_LAT);
      east = MAX_LON;
      north = Math.min(north, MAX_LAT);
    }

    return createBboxString(west, south, east, north);
  }

  /**
   * Takes in a {@link Polygon} and converts it to a (rough approximation) bounding box.
   *
   * <p>Note: Searches being performed where the polygon goes through the antimeridian will return
   * an incorrect bounding box. TODO DDF-3742
   *
   * @param polygon A polygon search area
   * @return {@link String} of the bounding box coordinates in the format "west,south,east,north".
   */
  private static String createBBoxStringFromPolygon(final Polygon polygon) {
    double west = Double.POSITIVE_INFINITY;
    double south = Double.POSITIVE_INFINITY;
    double east = Double.NEGATIVE_INFINITY;
    double north = Double.NEGATIVE_INFINITY;

    for (Coordinate coordinate : polygon.getCoordinates()) {
      final double lon = coordinate.x;
      final double lat = coordinate.y;

      if (lon < west) {
        west = lon;
      }
      if (lon > east) {
        east = lon;
      }
      if (lat < south) {
        south = lat;
      }
      if (lat > north) {
        north = lat;
      }
    }

    return createBboxString(west, south, east, north);
  }

  private static String createBboxString(double west, double south, double east, double north) {
    return Stream.of(west, south, east, north)
        .map(String::valueOf)
        .collect(Collectors.joining(OpenSearchConstants.BBOX_DELIMITER));
  }

  private static String translateToOpenSearchSort(SortBy ddfSort) {
    String openSearchSortStr = null;
    String orderType;

    if (ddfSort == null || ddfSort.getSortOrder() == null) {
      return null;
    }

    if (ddfSort.getSortOrder().equals(SortOrder.ASCENDING)) {
      orderType = OpenSearchConstants.ORDER_ASCENDING;
    } else {
      orderType = OpenSearchConstants.ORDER_DESCENDING;
    }

    PropertyName sortByField = ddfSort.getPropertyName();

    switch (sortByField.getPropertyName()) {
      case Result.RELEVANCE:
        // asc relevance not supported by spec
        openSearchSortStr =
            OpenSearchConstants.SORT_RELEVANCE
                + OpenSearchConstants.SORT_DELIMITER
                + OpenSearchConstants.ORDER_DESCENDING;
        break;
      case Result.TEMPORAL:
        openSearchSortStr =
            OpenSearchConstants.SORT_TEMPORAL + OpenSearchConstants.SORT_DELIMITER + orderType;
        break;
      default:
        LOGGER.debug(
            "Couldn't determine sort policy, not adding sorting in request to federated site.");
        break;
    }

    return openSearchSortStr;
  }
}
