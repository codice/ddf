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
package org.codice.ddf.opensearch.endpoint;

import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.opensearch.OpenSearch;
import org.codice.ddf.opensearch.OpenSearchConstants;
import org.codice.ddf.opensearch.endpoint.query.OpenSearchQuery;
import org.parboiled.errors.ParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OpenSearchEndpoint implements OpenSearch {

  private static final Logger LOGGER = LoggerFactory.getLogger(OpenSearchEndpoint.class);

  private static final String UPDATE_QUERY_INTERVAL = "interval";

  private static final String DEFAULT_FORMAT = "atom";

  private static final long DEFAULT_TIMEOUT = 300000;

  private static final int DEFAULT_COUNT = 10;

  private static final int DEFAULT_START_INDEX = 1;

  private static final String DEFAULT_SORT_FIELD = OpenSearchConstants.SORT_RELEVANCE;

  private static final String DEFAULT_SORT_ORDER = OpenSearchConstants.ORDER_DESCENDING;

  private static final String DEFAULT_RADIUS = "5000";

  private static final Pattern SOURCES_PATTERN =
      Pattern.compile(OpenSearchConstants.SOURCES_DELIMITER);

  private static final Pattern SORT_PATTERN = Pattern.compile(OpenSearchConstants.SORT_DELIMITER);

  /**
   * sort=<sbfield>:<sborder>, where <sbfield> may be 'date' or 'relevance' (default is
   * 'relevance'). The conditional param <sborder> is optional but has a value of 'asc' or 'desc'
   * (default is 'desc'). When <sbfield> is 'relevance', <sborder> must be 'desc'.
   */
  private static final Pattern EXPECTED_SORT_FORMAT =
      Pattern.compile(
          String.format(
              "(%1$s(%5$s(%3$s|%4$s))?)|(%2$s(%5$s%4$s)?)",
              OpenSearchConstants.SORT_TEMPORAL,
              OpenSearchConstants.SORT_RELEVANCE,
              OpenSearchConstants.ORDER_ASCENDING,
              OpenSearchConstants.ORDER_DESCENDING,
              OpenSearchConstants.SORT_DELIMITER));

  private final CatalogFramework framework;

  private final FilterBuilder filterBuilder;

  public OpenSearchEndpoint(CatalogFramework framework, FilterBuilder filterBuilder) {
    this.framework = framework;
    this.filterBuilder = filterBuilder;
  }

  /**
   * @param searchTerms Space-delimited list of search terms.
   * @param maxResults Maximum # of results to return. If count is also specified, the count value
   *     will take precedence over the maxResults value
   * @param sources Comma-delimited list of data sources to query (default: default sources
   *     selected).
   * @param maxTimeout Maximum timeout (msec) for query to respond (default: mt=30000).
   * @param startIndex Index of first result to return. Integer >= 0 (default: start=1).
   * @param count Number of results to retrieve per page (default: count=10).
   * @param geometry WKT Geometries.
   * @param bbox Comma-delimited list of lat/lon (deg) bounding box coordinates (geo format:
   *     geo:bbox ~ West,South,East,North).
   * @param polygon Comma-delimited list of lat/lon (deg) pairs, in clockwise order around the
   *     polygon, with the last point being the same as the first in order to close the polygon.
   * @param lat Latitude in decimal degrees (typical GPS receiver WGS84 coordinates).
   * @param lon Longitude in decimal degrees (typical GPS receiver WGS84 coordinates).
   * @param radius The radius (m) parameter, used with the lat and lon parameters, specifies the
   *     search distance from this point (default: radius=5000).
   * @param dateStart Specifies the beginning of the time slice of the search on the modified time
   *     field (RFC-3339 - Date and Time format, i.e. YYYY-MM-DDTHH:mm:ssZ). Default value of
   *     "1970-01-01T00:00:00Z" is used when dtend is indicated but dtstart is not specified
   * @param dateEnd Specifies the ending of the time slice of the search on the modified time field
   *     (RFC-3339 - Date and Time format, i.e. YYYY-MM-DDTHH:mm:ssZ). Current GMT date/time is used
   *     when dtstart is specified but not dtend.
   * @param dateOffset Specifies an offset, backwards from the current time, to search on the
   *     modified time field for entries. Defined in milliseconds.
   * @param sort Specifies sort by field as sort=<sbfield>:<sborder>, where <sbfield> may be 'date'
   *     or 'relevance' (default is 'relevance'). The conditional param <sborder> is optional but
   *     has a value of 'asc' or 'desc' (default is 'desc'). When <sbfield> is 'relevance',
   *     <sborder> must be 'desc'.
   * @param format Defines the format that the return type should be in. (example:atom, html)
   * @param selectors Defines a comma-delimited list of XPath selectors to narrow the query.
   * @param type Specifies the type of data to search for. (example: nitf)
   * @param versions Specifies the versions in a comma-delimited list.
   */
  @Override
  @GET
  public Response processQuery(
      @QueryParam(OpenSearchConstants.SEARCH_TERMS) String searchTerms,
      @QueryParam(OpenSearchConstants.MAX_RESULTS) String maxResults,
      @QueryParam(OpenSearchConstants.SOURCES) String sources,
      @QueryParam(OpenSearchConstants.MAX_TIMEOUT) String maxTimeout,
      @QueryParam(OpenSearchConstants.START_INDEX) String startIndex,
      @QueryParam(OpenSearchConstants.COUNT) String count,
      @QueryParam(OpenSearchConstants.GEOMETRY) String geometry,
      @QueryParam(OpenSearchConstants.BBOX) String bbox,
      @QueryParam(OpenSearchConstants.POLYGON) String polygon,
      @QueryParam(OpenSearchConstants.LAT) String lat,
      @QueryParam(OpenSearchConstants.LON) String lon,
      @QueryParam(OpenSearchConstants.RADIUS) String radius,
      @QueryParam(OpenSearchConstants.DATE_START) String dateStart,
      @QueryParam(OpenSearchConstants.DATE_END) String dateEnd,
      @QueryParam(OpenSearchConstants.DATE_OFFSET) String dateOffset,
      @QueryParam(OpenSearchConstants.SORT) String sort,
      @QueryParam(OpenSearchConstants.FORMAT) String format,
      @QueryParam(OpenSearchConstants.SELECTORS) String selectors,
      @Context UriInfo ui,
      @QueryParam(OpenSearchConstants.TYPE) String type,
      @QueryParam(OpenSearchConstants.VERSIONS) String versions,
      @Context HttpServletRequest request) {
    Response response;
    LOGGER.trace("request url: {}", ui.getRequestUri());

    try {
      OpenSearchQuery query = createNewQuery(startIndex, count, maxResults, sort, maxTimeout);

      if (StringUtils.isNotEmpty(sources)) {
        LOGGER.trace("Received site names from client.");
        final Set<String> siteSet =
            SOURCES_PATTERN.splitAsStream(sources).collect(Collectors.toSet());

        // This code block is for backward compatibility to support src=local.
        // Since local is a magic word, not in any specification, we need to
        // eventually remove support for it.
        if (siteSet.remove(OpenSearchConstants.LOCAL_SOURCE)) {
          LOGGER.trace("Found 'local' alias, replacing with {}.", SystemInfo.getSiteName());
          siteSet.add(SystemInfo.getSiteName());
        }

        if (siteSet.contains(framework.getId()) && siteSet.size() == 1) {
          LOGGER.trace(
              "Only local site specified, saving overhead and just performing a local query on "
                  + framework.getId()
                  + ".");
        } else {
          LOGGER.trace("Querying site set: {}", siteSet);
          query.setSiteIds(siteSet);
        }

        query.setIsEnterprise(false);
      } else {
        LOGGER.trace("No sites found, defaulting to enterprise query.");
        query.setIsEnterprise(true);
      }

      // contextual
      if (StringUtils.isNotBlank(searchTerms)) {
        query.addContextualFilter(searchTerms.trim(), selectors);
      }

      if (StringUtils.isNotBlank(dateStart) || StringUtils.isNotBlank(dateEnd)) {
        // If either start date OR end date is specified and non-empty, then a temporal filter can
        // be created
        query.addStartEndTemporalFilter(dateStart, dateEnd);
      } else if (StringUtils.isNotBlank(dateOffset)) {
        query.addOffsetTemporalFilter(dateOffset);
      }

      // spatial
      // single spatial criterion per query
      addSpatialFilter(query, geometry, polygon, bbox, radius, lat, lon);

      if (StringUtils.isNotBlank(type)) {
        query.addTypeFilter(type.trim(), versions);
      }

      Map<String, Serializable> properties = new HashMap<>();
      for (Object key : request.getParameterMap().keySet()) {
        if (key instanceof String) {
          Object value = request.getParameterMap().get(key);
          if (value != null) {
            properties.put((String) key, ((String[]) value)[0]);
          }
        }
      }

      response = executeQuery(format, query, ui, properties);
    } catch (ParsingException e) {
      LOGGER.debug("Bad input found while executing a query", e);
      response =
          Response.status(Response.Status.BAD_REQUEST)
              .entity(wrapStringInPreformattedTags(e.getMessage()))
              .build();
    } catch (RuntimeException re) {
      LOGGER.debug("Exception while executing a query", re);
      response =
          Response.serverError()
              .entity(wrapStringInPreformattedTags("Exception while executing a query"))
              .build();
    }

    return response;
  }

  /**
   * Creates SpatialCriterion based on the input parameters, any null values will be ignored
   *
   * @param geometry - the geo to search over
   * @param polygon - the polygon to search over
   * @param bbox - the bounding box to search over
   * @param radius - the radius for a point radius search
   * @param lat - the latitude of the point.
   * @param lon - the longitude of the point.
   */
  private void addSpatialFilter(
      OpenSearchQuery query,
      String geometry,
      String polygon,
      String bbox,
      String radius,
      String lat,
      String lon) {
    if (StringUtils.isNotBlank(geometry)) {
      LOGGER.trace("Adding SpatialCriterion geometry: {}", geometry);
      query.addGeometrySpatialFilter(geometry.trim());
    }

    if (StringUtils.isNotBlank(bbox)) {
      LOGGER.trace("Adding SpatialCriterion bbox: {}", bbox);
      query.addBBoxSpatialFilter(bbox.trim());
    }

    if (StringUtils.isNotBlank(polygon)) {
      LOGGER.trace("Adding SpatialCriterion polygon: {}", polygon);
      query.addPolygonSpatialFilter(polygon.trim());
    }

    if (StringUtils.isNotBlank(lat) && StringUtils.isNotBlank(lon)) {
      if (StringUtils.isBlank(radius)) {
        LOGGER.trace("Adding default radius {}", DEFAULT_RADIUS);
        query.addPointRadiusSpatialFilter(lon.trim(), lat.trim(), DEFAULT_RADIUS);
      } else {
        LOGGER.trace("Using radius: {}", radius);
        query.addPointRadiusSpatialFilter(lon.trim(), lat.trim(), radius.trim());
      }
    }
  }

  /**
   * Executes the OpenSearchQuery and formulates the response
   *
   * @param format - of the results in the response
   * @param query - the query to execute
   * @param ui -the ui information to use to format the results
   * @return the response on the query
   */
  private Response executeQuery(
      String format, OpenSearchQuery query, UriInfo ui, Map<String, Serializable> properties) {
    Response response = null;
    String queryFormat = format;

    MultivaluedMap<String, String> queryParams = ui.getQueryParameters();
    List<String> subscriptionList = queryParams.get(Constants.SUBSCRIPTION_KEY);

    LOGGER.trace("Attempting to execute query: {}", query);
    try {
      Map<String, Serializable> arguments = new HashMap<>();
      String organization = framework.getOrganization();
      String url = ui.getRequestUri().toString();
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("organization: {}", organization);
        LOGGER.trace("url: {}", url);
      }

      arguments.put("organization", organization);
      arguments.put("url", url);

      // if subscription is specified, add to arguments as well as update
      // interval
      if (CollectionUtils.isNotEmpty(subscriptionList)) {
        String subscription = subscriptionList.get(0);
        LOGGER.trace("Subscription: {}", subscription);
        arguments.put(Constants.SUBSCRIPTION_KEY, subscription);
        List<String> intervalList = queryParams.get(UPDATE_QUERY_INTERVAL);
        if (CollectionUtils.isNotEmpty(intervalList)) {
          arguments.put(UPDATE_QUERY_INTERVAL, intervalList.get(0));
        }
      }

      if (StringUtils.isEmpty(queryFormat)) {
        queryFormat = DEFAULT_FORMAT;
      }

      if (query.getFilter() != null) {
        QueryRequest queryRequest =
            new QueryRequestImpl(query, query.isEnterprise(), query.getSiteIds(), properties);
        QueryResponse queryResponse;

        LOGGER.trace("Sending query");
        queryResponse = framework.query(queryRequest);

        // pass in the format for the transform
        BinaryContent content = framework.transform(queryResponse, queryFormat, arguments);
        response = Response.ok(content.getInputStream(), content.getMimeTypeValue()).build();
      } else {
        // No query was specified
        QueryRequest queryRequest =
            new QueryRequestImpl(query, query.isEnterprise(), query.getSiteIds(), null);

        // Create a dummy QueryResponse with zero results
        QueryResponseImpl queryResponseQueue =
            new QueryResponseImpl(queryRequest, new ArrayList<>(), 0);

        // pass in the format for the transform
        BinaryContent content = framework.transform(queryResponseQueue, queryFormat, arguments);
        if (null != content) {
          response = Response.ok(content.getInputStream(), content.getMimeTypeValue()).build();
        }
      }
    } catch (UnsupportedQueryException ce) {
      LOGGER.debug("Unsupported query", ce);
      response =
          Response.status(Response.Status.BAD_REQUEST)
              .entity(wrapStringInPreformattedTags("Unsupported query"))
              .build();
    } catch (CatalogTransformerException e) {
      LOGGER.debug("Error transforming response", e);
      response =
          Response.serverError()
              .entity(wrapStringInPreformattedTags("Error transforming response"))
              .build();
    } catch (FederationException e) {
      LOGGER.debug("Error executing query", e);
      response =
          Response.serverError()
              .entity(wrapStringInPreformattedTags("Error executing query"))
              .build();
    } catch (SourceUnavailableException e) {
      LOGGER.debug("Error executing query because the underlying source was unavailable.", e);
      response =
          Response.serverError()
              .entity(
                  wrapStringInPreformattedTags(
                      "Error executing query because the underlying source was unavailable."))
              .build();
    } catch (RuntimeException e) {
      // Account for any runtime exceptions and send back a server error
      // this prevents full stacktraces returning to the client
      // this allows for a graceful server error to be returned
      LOGGER.debug("RuntimeException on executing query", e);
      response =
          Response.serverError()
              .entity(wrapStringInPreformattedTags("RuntimeException on executing query"))
              .build();
    }
    return response;
  }

  /**
   * Creates a new query from the incoming parameters
   *
   * @param startIndexStr - start index for the query
   * @param countStr - number of results for the query
   * @param maxResultsStr - maximum # of results to return
   * @param sortStr - how to sort the query results
   * @param maxTimeoutStr - timeout value on the query execution
   * @return - the new query
   */
  private OpenSearchQuery createNewQuery(
      final String startIndexStr,
      final String countStr,
      final String maxResultsStr,
      final String sortStr,
      final String maxTimeoutStr) {
    final int startIndex;
    if (StringUtils.isEmpty(startIndexStr)) {
      LOGGER.trace(
          "Empty {} query parameter. Using default {} instead.",
          OpenSearchConstants.START_INDEX,
          DEFAULT_START_INDEX);
      startIndex = DEFAULT_START_INDEX;
    } else {
      final int parsedInt = Integer.parseInt(startIndexStr);

      if (parsedInt > 0) {
        startIndex = parsedInt;
      } else {
        LOGGER.debug(
            "{} query parameter must be greater than 0 but is \"{}\". Using default {} instead.",
            OpenSearchConstants.START_INDEX,
            startIndexStr,
            DEFAULT_START_INDEX);
        startIndex = DEFAULT_START_INDEX;
      }
    }

    final int count;
    if (StringUtils.isEmpty(countStr)) {
      LOGGER.trace("Empty {} query parameter", OpenSearchConstants.COUNT);

      if (StringUtils.isEmpty(maxResultsStr)) {
        LOGGER.trace(
            "Empty {} query parameter. Using default {} instead",
            OpenSearchConstants.MAX_RESULTS,
            DEFAULT_COUNT);
        count = DEFAULT_COUNT;
      } else {
        // honor maxResults if count is not specified
        count = Integer.parseInt(maxResultsStr);
      }
    } else {
      count = Integer.parseInt(countStr);
    }

    final String sortField;
    final String sortOrder;
    if (StringUtils.isEmpty(sortStr)) {
      LOGGER.trace(
          "Empty {} query parameter. Using defaults (field={}, order={}) instead.",
          OpenSearchConstants.SORT,
          DEFAULT_SORT_FIELD,
          DEFAULT_SORT_ORDER);
      sortField = DEFAULT_SORT_FIELD;
      sortOrder = DEFAULT_SORT_ORDER;
    } else {
      if (EXPECTED_SORT_FORMAT.matcher(sortStr).matches()) {
        String[] sortAry = SORT_PATTERN.split(sortStr, 2);
        sortField = sortAry[0];

        if (sortAry.length == 2) {
          sortOrder = sortAry[1];
        } else {
          LOGGER.trace(
              "Empty {} order query parameter. Using default {} instead.",
              OpenSearchConstants.SORT,
              DEFAULT_SORT_ORDER);
          sortOrder = DEFAULT_SORT_ORDER;
        }
      } else {
        LOGGER.debug(
            "Invalid {} query parameter \"{}\". See the OpenSearch Endpoint documentation for details the parameter format. Using defaults (field={}, order={}) instead.",
            OpenSearchConstants.SORT,
            sortStr,
            DEFAULT_SORT_FIELD,
            DEFAULT_SORT_ORDER);
        sortField = DEFAULT_SORT_FIELD;
        sortOrder = DEFAULT_SORT_ORDER;
      }
    }

    final long maxTimeout;
    if (StringUtils.isEmpty(maxTimeoutStr)) {
      LOGGER.trace(
          "Empty {} query parameter. Using default {} instead.",
          OpenSearchConstants.MAX_TIMEOUT,
          DEFAULT_TIMEOUT);
      maxTimeout = DEFAULT_TIMEOUT;
    } else {
      maxTimeout = Long.parseLong(maxTimeoutStr);
    }

    return new OpenSearchQuery(startIndex, count, sortField, sortOrder, maxTimeout, filterBuilder);
  }

  private String wrapStringInPreformattedTags(String stringToWrap) {
    return "<pre>" + stringToWrap + "</pre>";
  }
}
