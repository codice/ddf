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
package org.codice.ddf.opensearch.source;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.data.Result;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.codice.ddf.opensearch.OpenSearchConstants;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenSearchParserImpl implements OpenSearchParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchParserImpl.class);

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTWriter::new);

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
        checkAndReplace(client, queryStr, OpenSearchConstants.SEARCH_TERMS, parameters);
      }
    }
  }

  @Override
  public void populateTemporal(WebClient client, TemporalFilter temporal, List<String> parameters) {
    if (temporal == null) {
      return;
    }

    DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
    long startLng = (temporal.getStartDate() != null) ? temporal.getStartDate().getTime() : 0;
    final String start = fmt.print(startLng);
    long endLng =
        (temporal.getEndDate() != null)
            ? temporal.getEndDate().getTime()
            : System.currentTimeMillis();
    final String end = fmt.print(endLng);

    checkAndReplace(client, start, OpenSearchConstants.DATE_START, parameters);
    checkAndReplace(client, end, OpenSearchConstants.DATE_END, parameters);
  }

  @Override
  public void populateSpatial(
      WebClient client,
      @Nullable Geometry geometry,
      @Nullable BoundingBox boundingBox,
      @Nullable Polygon polygon,
      @Nullable PointRadius pointRadius,
      List<String> parameters) {

    if (geometry != null) {
      checkAndReplace(
          client,
          WKT_WRITER_THREAD_LOCAL.get().write(geometry),
          OpenSearchConstants.GEOMETRY,
          parameters);
    }

    if (boundingBox != null) {
      checkAndReplace(
          client,
          Stream.of(
                  boundingBox.getWest(),
                  boundingBox.getSouth(),
                  boundingBox.getEast(),
                  boundingBox.getNorth())
              .map(String::valueOf)
              .collect(Collectors.joining(OpenSearchConstants.BBOX_DELIMITER)),
          OpenSearchConstants.BBOX,
          parameters);
    }

    if (polygon != null) {
      checkAndReplace(
          client,
          Arrays.stream(polygon.getCoordinates())
              .flatMap(coordinate -> Stream.of(coordinate.y, coordinate.x))
              .map(String::valueOf)
              .collect(Collectors.joining(OpenSearchConstants.POLYGON_LON_LAT_DELIMITER)),
          OpenSearchConstants.POLYGON,
          parameters);
    }

    if (pointRadius != null) {
      checkAndReplace(
          client, String.valueOf(pointRadius.getLat()), OpenSearchConstants.LAT, parameters);
      checkAndReplace(
          client, String.valueOf(pointRadius.getLon()), OpenSearchConstants.LON, parameters);
      checkAndReplace(
          client, String.valueOf(pointRadius.getRadius()), OpenSearchConstants.RADIUS, parameters);
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
  protected static void checkAndReplace(
      WebClient client, String inputStr, String definition, List<String> parameters) {
    if (hasParameter(definition, parameters) && StringUtils.isNotEmpty(inputStr)) {
      client.replaceQueryParam(definition, inputStr);
    }
  }

  protected static boolean hasParameter(String parameter, List<String> parameters) {
    for (String param : parameters) {
      if (param != null && param.equalsIgnoreCase(parameter)) {
        return true;
      }
    }
    return false;
  }

  protected static String translateToOpenSearchSort(SortBy ddfSort) {
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

    final String sortByField = ddfSort.getPropertyName().getPropertyName();
    switch (sortByField) {
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
            "The OpenSearch Source only supports a sort policy of \"{}\" or \"{}\", but the sort policy is \"{}\". Not adding the sort query parameter in the request to the federated site.",
            Result.RELEVANCE,
            Result.TEMPORAL,
            sortByField);
        break;
    }

    return openSearchSortStr;
  }
}
