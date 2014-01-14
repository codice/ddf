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
package org.codice.ddf.ui.searchui.query.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.Subject;
import ddf.security.service.SecurityServiceException;
import ddf.security.service.TokenRequestHandler;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.opensearch.query.OpenSearchQuery;
import org.codice.ddf.ui.searchui.query.controller.SearchController;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.codice.ddf.ui.searchui.query.endpoint.CometdEndpoint;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.AbstractService;
import org.cometd.server.ServerMessageImpl;
import org.parboiled.errors.ParsingException;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by tustisos on 12/10/13.
 */
public class SearchService extends AbstractService {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(CometdEndpoint.class));

    private static final String DEFAULT_FORMAT = "geojson";

    private static final String GUID = "guid";

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

    private static final String LOCAL = "local";

    private static final String DEFAULT_SORT_FIELD = "relevance";

    private static final String DEFAULT_SORT_ORDER = "desc";

    private static final long DEFAULT_TIMEOUT = 300000;

    private static final int DEFAULT_COUNT = 10;

    private static final int DEFAULT_START_INDEX = 1;

    private static final String DEFAULT_RADIUS = "5000";

    private final FilterBuilder filterBuilder;

    private final SearchController searchController;

    public SearchService(BayeuxServer bayeux, String name, FilterBuilder filterBuilder, SearchController searchController) {
        super(bayeux, name);
        this.filterBuilder = filterBuilder;
        this.searchController = searchController;

        addService("/service/async/query", "processQuery");
    }

    public void processQuery(final ServerSession remote, Message message) {

        ServerMessage.Mutable reply = new ServerMessageImpl();

        Map<String, Object> queryMessage = message.getDataAsMap();

        if(queryMessage.containsKey("guid")) {
            getBayeux().createChannelIfAbsent("/"+queryMessage.get("guid"), new ConfigurableServerChannel.Initializer()
            {
                public void configureChannel(ConfigurableServerChannel channel)
                {
                    channel.setPersistent(true);
                }
            });

            reply.putAll(executeQuery(queryMessage));
            reply.put("successful", true);
        } else {
            reply.put("status", "ERROR: unable to return results, no guid in query request");
            remote.deliver(getServerSession(), reply);
        }

        remote.deliver(getServerSession(), "/"+queryMessage.get("guid"), reply, null);
    }

    public Map<? extends String, ?> executeQuery(Map<String, Object> queryMessage) {
        final String methodName = "executeQuery";
        LOGGER.entry(methodName);
        String searchTerms = (String) queryMessage.get(PHRASE);
        Long maxResults = (Long) queryMessage.get(MAX_RESULTS);
        String sources = (String) queryMessage.get(SOURCES);
        Long maxTimeout = (Long) queryMessage.get(MAX_TIMEOUT);
        Long startIndex = (Long) queryMessage.get(START_INDEX);
        Long count = (Long) queryMessage.get(COUNT);
        String geometry = (String) queryMessage.get(GEOMETRY);
        String bbox = (String) queryMessage.get(BBOX);
        String polygon = (String) queryMessage.get(POLYGON);
        String lat = (String) queryMessage.get(LAT);
        String lon = (String) queryMessage.get(LON);
        String radius = (String) queryMessage.get(RADIUS);
        String dateStart = (String) queryMessage.get(DATE_START);
        String dateEnd = (String) queryMessage.get(DATE_END);
        String dateOffset = (String) queryMessage.get(DATE_OFFSET);
        String sort = (String) queryMessage.get(SORT);
        String format = (String) queryMessage.get(FORMAT);
        String selector = (String) queryMessage.get(SELECTOR);
        String type = (String) queryMessage.get(TYPE);
        String versions = (String) queryMessage.get(VERSION);
        String guid = (String) queryMessage.get(GUID);

        Map<String, Object> response = null;

        Long localCount = count;

        //Build the SearchRequest and then hand off to the controller for the actual query

        // honor maxResults if count is not specified
        if (localCount == null && maxResults != null) {
            LOGGER.debug("setting count to: " + maxResults);
            localCount = maxResults;
        }

        try {
            String queryFormat = format;
            OpenSearchQuery query = createNewQuery(startIndex, localCount, sort, maxTimeout);
            //no asynchronous queries will be enterprise, this will be handled internally
            query.setIsEnterprise(false);

            // contextual
            addContextualFilter(searchTerms, selector, query);

            // temporal
            // single temporal criterion per query
            addTemporalFilter(dateStart, dateEnd, dateOffset, query);

            // spatial
            // single spatial criterion per query
            addSpatialFilter(query, geometry, polygon, bbox, radius, lat, lon);

            if (type != null && !type.trim().isEmpty()) {
                query.addTypeFilter(type, versions);
            }

            //right now we will perform only the local query to immediately return results
            //other sites (if specified) will be queried asynchronously
            Set<String> siteSet = new HashSet<String>();
            siteSet.add(searchController.getFramework().getId());
            query.setSiteIds(siteSet);

            List<OpenSearchQuery> remoteQueryList = new ArrayList<OpenSearchQuery>();

            Set<String> federatedSet = null;
            if (!(StringUtils.isEmpty(sources))) {
                LOGGER.debug("Received site names from client.");
                federatedSet = new HashSet<String>(Arrays.asList(StringUtils.stripAll(sources.split(","))));
            } else {
                federatedSet = searchController.getFramework().getSourceIds();
            }

            if (federatedSet != null && !federatedSet.isEmpty()) {
                //make sure no one is including this "local" magic word
                federatedSet.remove(LOCAL);
                federatedSet.remove(searchController.getFramework().getId());

                OpenSearchQuery fedQuery;
                Set<String> fedSet;
                for(String siteId : federatedSet) {
                    fedQuery = createNewQuery(startIndex, localCount, sort, maxTimeout);

                    // contextual
                    addContextualFilter(searchTerms, selector, fedQuery);

                    // temporal
                    // single temporal criterion per query
                    addTemporalFilter(dateStart, dateEnd, dateOffset, fedQuery);

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

            //Now we can build the request
            SearchRequest searchRequest = new SearchRequest(query, remoteQueryList, queryFormat, guid);

            //Hand off to the search controller for the actual query
            response = searchController.executeQuery(searchRequest, getServerSession());
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Bad input found while executing a query", iae);
        } catch (RuntimeException re) {
            LOGGER.warn("Exception while executing a query", re);
        } catch (InterruptedException e) {
            LOGGER.warn("Exception while executing a query", e);
        } catch (CatalogTransformerException e) {
            LOGGER.error("Unable to transform query result.", e);
        }
        LOGGER.exit(methodName);

        return response;
    }

    /**
     * Create temporal filter
     *
     * @param dateStart
     * @param dateEnd
     * @param dateOffset
     * @param query
     */
    private void addTemporalFilter(String dateStart, String dateEnd, String dateOffset, OpenSearchQuery query) {
        if ((dateStart != null && !dateStart.trim().isEmpty())
                || (dateEnd != null && !dateEnd.trim().isEmpty())
                || (dateOffset != null && !dateOffset.trim().isEmpty())) {
            query.addTemporalFilter(dateStart, dateEnd, dateOffset);
        }
    }

    /**
     * Create contextual filter
     *
     * @param searchTerms
     * @param selector
     * @param query
     */
    private void addContextualFilter(String searchTerms, String selector, OpenSearchQuery query) {
        if (searchTerms != null && !searchTerms.trim().isEmpty()) {
            try {
                query.addContextualFilter(searchTerms, selector);
            } catch (ParsingException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
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

    /**
     * Creates a new query from the incoming parameters
     *
     * @param startIndexLng
     *            - Start index for the query
     * @param countLng
     *            - number of results for the query
     * @param sortStr
     *            - How to sort the query results
     * @param maxTimeoutLng
     *            - timeout value on the query execution
     * @return - the new query
     */
    private OpenSearchQuery createNewQuery(Long startIndexLng, Long countLng, String sortStr,
                                           Long maxTimeoutLng) {
        // default values
        String sortField = DEFAULT_SORT_FIELD;
        String sortOrder = DEFAULT_SORT_ORDER;
        Long startIndex = startIndexLng == null ? DEFAULT_START_INDEX : startIndexLng;
        Long count = countLng == null ? DEFAULT_COUNT : countLng;
        long maxTimeout = maxTimeoutLng == null ? DEFAULT_TIMEOUT : maxTimeoutLng;

        // Updated to use the passed in index if valid (=> 1)
        // and to use the default if no value, or an invalid value (< 1)
        // is specified
        if (!(StringUtils.isEmpty(sortStr))) {
            String[] sortAry = sortStr.split(":");
            if (sortAry.length > 1) {
                sortField = sortAry[0];
                sortOrder = sortAry[1];
            }
        }
        LOGGER.debug("Retrieved query settings: \n" + "sortField:" + sortField + "\nsortOrder:"
                + sortOrder);
        return new OpenSearchQuery(null, startIndex.intValue(), count.intValue(), sortField, sortOrder, maxTimeout,
                filterBuilder);
    }
}
