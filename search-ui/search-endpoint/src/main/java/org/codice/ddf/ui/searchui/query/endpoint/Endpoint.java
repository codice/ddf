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
package org.codice.ddf.ui.searchui.query.endpoint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.codice.ddf.opensearch.query.OpenSearchQuery;
import org.codice.ddf.ui.searchui.query.controller.SearchController;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.codice.ddf.ui.searchui.query.servlet.CometdServlet;
import org.parboiled.errors.ParsingException;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.service.TokenRequestHandler;

/**
 * Created by tustisos on 12/10/13.
 */
@Path("/")
public class Endpoint implements ConfigurationWatcher {

    private final CatalogFramework framework;

    private final FilterBuilder filterBuilder;

    private final CometdServlet cometdServlet;

    private SecurityManager securityManager;

    private List<TokenRequestHandler> requestHandlerList;

    private String localSiteName = null;

    private SearchController searchController;

    private static final String DEFAULT_FORMAT = "atom";

    private static final String PHRASE = "q";

    private static final String MAX_RESULTS = "mr";

    private static final String SOURCES = "src";

    private static final String MAX_TIMEOUT = "mt";

    private static final String START_INDEX = "start";

    private static final String COUNT = "count";

    private static final String BBOX = "bbox";

    private static final String POLYGON = "polygon";

    private static final String GEOMETRY = "geometry";

    private static final String LAT = "lat";

    private static final String LON = "lon";

    private static final String RADIUS = "radius";

    private static final String DATE_START = "dtstart";

    private static final String DATE_END = "dtend";

    private static final String DATE_OFFSET = "dtoffset";

    private static final String TYPE = "type";

    private static final String VERSION = "version";

    private static final String SELECTOR = "selector";

    private static final String SORT = "sort";

    private static final String FORMAT = "format";

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(Endpoint.class));

    private static final String DEFAULT_SORT_FIELD = "relevance";

    private static final String DEFAULT_SORT_ORDER = "desc";

    private static final long DEFAULT_TIMEOUT = 300000;

    private static final int DEFAULT_COUNT = 10;

    private static final int DEFAULT_START_INDEX = 1;

    private static final String DEFAULT_RADIUS = "5000";

    private static final String LOCAL = "local";

    public Endpoint(CometdServlet cometdServlet, CatalogFramework framework, FilterBuilder filterBuilder) {
        this.cometdServlet = cometdServlet;
        this.framework = framework;
        this.filterBuilder = filterBuilder;
        this.searchController = new SearchController(framework, cometdServlet);
    }

    /**
     *
     * @param searchTerms
     *            Space delimited list of search terms.
     * @param maxResults
     *            Maximum # of results to return. If count is also specified, the count value will
     *            take precedence over the maxResults value
     * @param sources
     *            Comma delimited list of data sources to query (default: default sources selected).
     * @param maxTimeout
     *            Maximum timeout (msec) for query to respond (default: mt=30000).
     * @param startIndex
     *            Index of first result to return. Integer >= 0 (default: start=1).
     * @param count
     *            Number of results to retrieve per page (default: count=10).
     * @param geometry
     *            WKT Geometries (Support POINT and POLYGON).
     * @param bbox
     *            Comma delimited list of lat/lon (deg) bounding box coordinates (geo format:
     *            geo:bbox ~ West,South,East,North).
     * @param polygon
     *            Comma delimited list of lat/lon (deg) pairs, in clockwise order around the
     *            polygon, with the last point being the same as the first in order to close the
     *            polygon.
     * @param lat
     *            Latitude in decimal degrees (typical GPS receiver WGS84 coordinates).
     * @param lon
     *            Longitude in decimal degrees (typical GPS receiver WGS84 coordinates).
     * @param radius
     *            The radius (m) parameter, used with the lat and lon parameters, specifies the
     *            search distance from this point (default: radius=5000).
     * @param dateStart
     *            Specifies the beginning of the time slice of the search on the modified time field
     *            (RFC-3339 - Date and Time format, i.e. YYYY-MM-DDTHH:mm:ssZ). Default value of
     *            "1970-01-01T00:00:00Z" is used when dtend is indicated but dtstart is not
     *            specified
     * @param dateEnd
     *            Specifies the ending of the time slice of the search on the modified time field
     *            (RFC-3339 - Date and Time format, i.e. YYYY-MM-DDTHH:mm:ssZ). Current GMT
     *            date/time is used when dtstart is specified but not dtend.
     * @param dateOffset
     *            Specifies an offset, backwards from the current time, to search on the modified
     *            time field for entries. Defined in milliseconds.
     * @param sort
     *            Specifies sort by field as sort=<sbfield>:<sborder>, where <sbfield> may be 'date'
     *            or 'relevance' (default is 'relevance'). The conditional param <sborder> is
     *            optional but has a value of 'asc' or 'desc' (default is 'desc'). When <sbfield> is
     *            'relevance', <sborder> must be 'desc'.
     * @param format
     *            Defines the format that the return type should be in. (example:atom, html)
     * @param selector
     *            Defines a comma delimited list of XPath selectors to narrow the query.
     * @param type
     *            Specifies the type of data to search for. (example: nitf)
     * @param versions
     *            Specifies the versions in a comma delimited list.
     * @return
     */
    @GET
    public Response processQuery(@QueryParam(PHRASE)
                                 String searchTerms, @QueryParam(MAX_RESULTS)
                                 String maxResults, @QueryParam(SOURCES)
                                 String sources, @QueryParam(MAX_TIMEOUT)
                                 String maxTimeout, @QueryParam(START_INDEX)
                                 String startIndex, @QueryParam(COUNT)
                                 String count, @QueryParam(GEOMETRY)
                                 String geometry, @QueryParam(BBOX)
                                 String bbox, @QueryParam(POLYGON)
                                 String polygon, @QueryParam(LAT)
                                 String lat, @QueryParam(LON)
                                 String lon, @QueryParam(RADIUS)
                                 String radius, @QueryParam(DATE_START)
                                 String dateStart, @QueryParam(DATE_END)
                                 String dateEnd, @QueryParam(DATE_OFFSET)
                                 String dateOffset, @QueryParam(SORT)
                                 String sort, @QueryParam(FORMAT)
                                 String format, @QueryParam(SELECTOR)
                                 String selector, @Context
                                 UriInfo ui, @QueryParam(TYPE)
                                 String type, @QueryParam(VERSION)
                                 String versions, @Context
                                 HttpServletRequest request) {

        final String methodName = "processQuery";
        LOGGER.entry(methodName);
        Response response;
        String localCount = count;
        Subject subject;
        LOGGER.debug("request url: " + ui.getRequestUri());

        subject = getSubject(request);
        if (subject == null) {
            LOGGER.debug("Could not set security attributes for user, performing query with no permissions set.");
        }

        // honor maxResults if count is not specified
        if ((StringUtils.isEmpty(localCount)) && (!(StringUtils.isEmpty(maxResults)))) {
            LOGGER.debug("setting count to: " + maxResults);
            localCount = maxResults;
        }

        try {
            String queryFormat = format;
            OpenSearchQuery query = createNewQuery(startIndex, localCount, sort, maxTimeout);
            //no asynchronous queries will be enterprise, this will be handled internally
            query.setIsEnterprise(false);

            // contextual
            if (searchTerms != null && !searchTerms.trim().isEmpty()) {
                try {
                    query.addContextualFilter(searchTerms, selector);
                } catch (ParsingException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }

            // temporal
            // single temporal criterion per query
            if ((dateStart != null && !dateStart.trim().isEmpty())
                    || (dateEnd != null && !dateEnd.trim().isEmpty())
                    || (dateOffset != null && !dateOffset.trim().isEmpty())) {
                query.addTemporalFilter(dateStart, dateEnd, dateOffset);
            }

            // spatial
            // single spatial criterion per query
            addSpatialFilter(query, geometry, polygon, bbox, radius, lat, lon);

            if (type != null && !type.trim().isEmpty()) {
                query.addTypeFilter(type, versions);
            }

            //right now we will perform only the local query to immediately return results
            //other sites (if specified) will be queried asynchronously
            Set<String> siteSet = new HashSet<String>();
            siteSet.add(framework.getId());
            query.setSiteIds(siteSet);

            List<OpenSearchQuery> remoteQueryList = new ArrayList<OpenSearchQuery>();

//            query.setGuid(UUID.randomUUID());

            Set<String> federatedSet = null;
            if (!(StringUtils.isEmpty(sources))) {
                LOGGER.debug("Received site names from client.");
                federatedSet = new HashSet<String>(Arrays.asList(StringUtils.stripAll(sources.split(","))));
            } else {
                federatedSet = framework.getSourceIds();
            }

            if (federatedSet != null && !federatedSet.isEmpty()) {
               //make sure no one is including this "local" magic word
                federatedSet.remove(LOCAL);
                federatedSet.remove(framework.getId());

                OpenSearchQuery fedQuery;
                Set<String> fedSet;
                for(String siteId : federatedSet) {
                    fedQuery = createNewQuery(startIndex, localCount, sort, maxTimeout);

                    // contextual
                    if (searchTerms != null && !searchTerms.trim().isEmpty()) {
                        try {
                            fedQuery.addContextualFilter(searchTerms, selector);
                        } catch (ParsingException e) {
                            throw new IllegalArgumentException(e.getMessage());
                        }
                    }

                    // temporal
                    // single temporal criterion per query
                    if ((dateStart != null && !dateStart.trim().isEmpty())
                            || (dateEnd != null && !dateEnd.trim().isEmpty())
                            || (dateOffset != null && !dateOffset.trim().isEmpty())) {
                        fedQuery.addTemporalFilter(dateStart, dateEnd, dateOffset);
                    }

                    // spatial
                    // single spatial criterion per query
                    addSpatialFilter(fedQuery, geometry, polygon, bbox, radius, lat, lon);

                    if (type != null && !type.trim().isEmpty()) {
                        fedQuery.addTypeFilter(type, versions);
                    }

                    fedSet = new HashSet<String>();
                    fedSet.add(siteId);
                    fedQuery.setSiteIds(fedSet);
                    fedQuery.setIsEnterprise(false);

                    remoteQueryList.add(fedQuery);
                }

            } else {
                LOGGER.debug("No sites found, defaulting to asynchronous enterprise query.");

            }

            if (StringUtils.isEmpty(queryFormat)) {
                queryFormat = DEFAULT_FORMAT;
            }

            SearchRequest searchRequest = new SearchRequest(query, remoteQueryList, ui, subject, queryFormat, UUID.randomUUID());

            response = searchController.executeQuery(searchRequest);
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Bad input found while executing a query", iae);
            response = Response.status(Response.Status.BAD_REQUEST)
                    .entity(searchController.wrapStringInPreformattedTags(iae.getMessage())).build();
        } catch (RuntimeException re) {
            LOGGER.warn("Exception while executing a query", re);
            response = Response.serverError().entity(searchController.wrapStringInPreformattedTags(re.getMessage()))
                    .build();
        } catch (InterruptedException e) {
            LOGGER.warn("Exception while executing a query", e);
            response = Response.serverError().entity(searchController.wrapStringInPreformattedTags(e.getMessage()))
                    .build();
        }
        LOGGER.exit(methodName);

        return response;
    }

    /**
     * Creates SpatialCriterion based on the input parameters, any null values will be ignored
     *
     * @param geometry
     *            - the geo to search over
     * @param polygon
     *            - the polygon to search over
     * @param bbox
     *            - the bounding box to search over
     * @param radius
     *            - the radius for a point radius search
     * @param lat
     *            - the latitude of the point.
     * @param lon
     *            - the longitude of the point.
     * @return - the spatialCriterion created, can be null
     */
    private void addSpatialFilter(OpenSearchQuery query, String geometry, String polygon,
                                  String bbox, String radius, String lat, String lon) {
        if (geometry != null && !geometry.trim().isEmpty()) {
            LOGGER.debug("Adding SpatialCriterion geometry: " + geometry);
            query.addGeometrySpatialFilter(geometry);
        } else if (bbox != null && !bbox.trim().isEmpty()) {
            LOGGER.debug("Adding SpatialCriterion bbox: " + bbox);
            query.addBBoxSpatialFilter(bbox);
        } else if (polygon != null && !polygon.trim().isEmpty()) {
            LOGGER.debug("Adding SpatialCriterion polygon: " + polygon);
            query.addPolygonSpatialFilter(polygon);
        } else if (lat != null && !lat.trim().isEmpty() && lon != null && !lon.trim().isEmpty()) {
            if (radius == null || radius.trim().isEmpty()) {
                LOGGER.debug("Adding default radius");
                query.addSpatialDistanceFilter(lon, lat, DEFAULT_RADIUS);
            } else {
                LOGGER.debug("Using radius: " + radius);
                query.addSpatialDistanceFilter(lon, lat, radius);
            }
        }
    }

    protected Subject getSubject(HttpServletRequest request) {
        Subject subject = null;
        if (request != null) {
            if (securityManager != null) {
                for (TokenRequestHandler curHandler : requestHandlerList) {
                    try {
                        subject = securityManager.getSubject(curHandler.createToken(request));
                        LOGGER.debug("Able to get populated subject from incoming request.");
                        break;
                    } catch (SecurityServiceException sse) {
                        LOGGER.warn(
                                "Could not create subject from request handler, trying other handlers if available.",
                                sse);
                    }
                }
            } else {
                LOGGER.debug("No security manager was passed in, cannot obtain security credentials for user.");
            }
        } else {
            LOGGER.debug("No servlet request found, cannot obtain user credentials.");
        }
        return subject;
    }



    /**
     * Creates a new query from the incoming parameters
     *
     * @param startIndexStr
     *            - Start index for the query
     * @param countStr
     *            - number of results for the query
     * @param sortStr
     *            - How to sort the query results
     * @param maxTimeoutStr
     *            - timeout value on the query execution
     * @return - the new query
     */
    private OpenSearchQuery createNewQuery(String startIndexStr, String countStr, String sortStr,
                                           String maxTimeoutStr) {
        // default values
        String sortField = DEFAULT_SORT_FIELD;
        String sortOrder = DEFAULT_SORT_ORDER;
        Integer startIndex = DEFAULT_START_INDEX;
        Integer count = DEFAULT_COUNT;
        long maxTimeout = DEFAULT_TIMEOUT;

        // Updated to use the passed in index if valid (=> 1)
        // and to use the default if no value, or an invalid value (< 1)
        // is specified
        if (!(StringUtils.isEmpty(startIndexStr)) && (Integer.parseInt(startIndexStr) > 0)) {
            startIndex = Integer.parseInt(startIndexStr);
        }
        if (!(StringUtils.isEmpty(countStr))) {
            count = Integer.parseInt(countStr);
        }
        if (!(StringUtils.isEmpty(sortStr))) {
            String[] sortAry = sortStr.split(":");
            if (sortAry.length > 1) {
                sortField = sortAry[0];
                sortOrder = sortAry[1];
            }
        }
        if (!(StringUtils.isEmpty(maxTimeoutStr))) {
            maxTimeout = Long.parseLong(maxTimeoutStr);
        }
        LOGGER.debug("Retrieved query settings: \n" + "sortField:" + sortField + "\nsortOrder:"
                + sortOrder);
        return new OpenSearchQuery(null, startIndex, count, sortField, sortOrder, maxTimeout,
                filterBuilder);
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> ddfProperties) {
        String methodName = "configurationUpdateCallback";
        LOGGER.trace("ENTERING: " + methodName);

        // Need the id aka sitename property for the query

        if (ddfProperties != null && !ddfProperties.isEmpty()) {

            String siteName = ddfProperties.get(ConfigurationManager.SITE_NAME);
            if (StringUtils.isNotBlank(siteName)) {

                this.localSiteName = siteName;
            }

        } else {
            LOGGER.debug("properties are NULL or empty");
        }

        LOGGER.trace("EXITING: " + methodName);
    }



    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public void setSecurityManager(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    public void setRequestHandlers(List<TokenRequestHandler> requestHandlerList) {
        this.requestHandlerList = requestHandlerList;
 }
}
