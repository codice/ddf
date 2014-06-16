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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.ui.searchui.query.controller.SearchController;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.BayeuxContext;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.ServerMessageImpl;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class performs the searches when a client communicates with the cometd endpoint
 */
@Service
public class SearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

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

    private static final String TIME_TYPE = "timeType";

    private static final String TYPE = "type";

    private static final String VERSION = "version";

    private static final String SELECTOR = "selector";

    private static final String SORT = "sort";

    private static final String FORMAT = "format";

    private static final String DEFAULT_SORT_ORDER = "desc";

    private static final long DEFAULT_TIMEOUT = 300000;

    private static final int DEFAULT_COUNT = 10;

    private static final int DEFAULT_START_INDEX = 1;

    public static final String LOCAL_SOURCE = "local";

    private static DateTimeFormatter dateFormatter;

    static {
        DateTimeParser[] parsers = {ISODateTimeFormat.dateTime().getParser(),
                ISODateTimeFormat.dateTimeNoMillis().getParser(),
                ISODateTimeFormat.basicDateTime().getParser(),
                ISODateTimeFormat.basicDateTimeNoMillis().getParser()};
        dateFormatter = new DateTimeFormatterBuilder().append(null, parsers).toFormatter();
    }

    private final FilterBuilder filterBuilder;

    private final SearchController searchController;
    
    @Inject
    private BayeuxServer bayeux;
    
    @Session
    private ServerSession serverSession;

    /**
     * Creates a new SearchService
     *
     * @param filterBuilder
     *            - FilterBuilder to use for queries
     * @param searchController
     *            - SearchController to handle async queries
     */
    public SearchService(FilterBuilder filterBuilder, 
            SearchController searchController) {
        this.filterBuilder = filterBuilder;
        this.searchController = searchController;
    }

    /**
     * Service method called by Cometd when something arrives on the service channel
     * 
     * @param remote
     *            - Client session
     * @param message
     *            - JSON message
     */
    @Listener("/service/query")
    public void processQuery(final ServerSession remote, Message message) {

        ServerMessage.Mutable reply = new ServerMessageImpl();

        Map<String, Object> queryMessage = message.getDataAsMap();

        if (queryMessage.containsKey(Search.GUID)) {
            bayeux.createChannelIfAbsent("/" + queryMessage.get(Search.GUID),
                    new ConfigurableServerChannel.Initializer() {
                        public void configureChannel(ConfigurableServerChannel channel) {
                            channel.setPersistent(true);
                        }
                    });

            BayeuxContext context = bayeux.getContext();
            Subject subject = null;
            if(context != null) {
                subject = (Subject) context.getRequestAttribute(SecurityConstants.SECURITY_SUBJECT);
            }

            // kick off the query
            executeQuery(queryMessage, subject);

            reply.put(Search.SUCCESSFUL, true);
            remote.deliver(serverSession, reply);
        } else {
            reply.put(Search.SUCCESSFUL, false);
            reply.put("status", "ERROR: unable to return results, no guid in query request");
            remote.deliver(serverSession, reply);
        }

    }

    @SuppressWarnings("unchecked")
    private <T> T castObject(Class<T> targetClass, Object o) {
        if (o != null) {
            if (o instanceof Number) {
                if (targetClass.equals(Double.class)) {
                    return (T) new Double(((Number) o).doubleValue());
                } else if (targetClass.equals(Long.class)) {
                    return (T) new Long(((Number) o).longValue());
                } else {
                    // unhandled conversion so trying best effort
                    return (T) o;
                }
            } else {
                return (T) o.toString();
            }
        } else {
            return null;
        }
    }

    /**
     * Creates the query requests for each source and hands off the query to the Search Controller
     * 
     * @param queryMessage
     *            - JSON message received from cometd
     */
    public void executeQuery(Map<String, Object> queryMessage, Subject subject) {
        final String methodName = "executeQuery";
        LOGGER.debug("ENTERING {}", methodName);

        String searchTerms = castObject(String.class, queryMessage.get(PHRASE));
        Long maxResults = castObject(Long.class, queryMessage.get(MAX_RESULTS));
        String sources = castObject(String.class, queryMessage.get(SOURCES));
        Long maxTimeout = castObject(Long.class, queryMessage.get(MAX_TIMEOUT));
        Long startIndex = castObject(Long.class, queryMessage.get(START_INDEX));
        Long count = castObject(Long.class, queryMessage.get(COUNT));
        String geometry = castObject(String.class, queryMessage.get(GEOMETRY));
        String bbox = castObject(String.class, queryMessage.get(BBOX));
        String polygon = castObject(String.class, queryMessage.get(POLYGON));
        String lat = castObject(String.class, queryMessage.get(LAT));
        String lon = castObject(String.class, queryMessage.get(LON));
        Double radius = castObject(Double.class, queryMessage.get(RADIUS));
        String dateStart = castObject(String.class, queryMessage.get(DATE_START));
        String dateEnd = castObject(String.class, queryMessage.get(DATE_END));
        Long dateOffset = castObject(Long.class, queryMessage.get(DATE_OFFSET));
        String sort = castObject(String.class, queryMessage.get(SORT));
        String format = castObject(String.class, queryMessage.get(FORMAT));
        String selector = castObject(String.class, queryMessage.get(SELECTOR));
        String type = castObject(String.class, queryMessage.get(TYPE));
        String versions = castObject(String.class, queryMessage.get(VERSION));
        String guid = castObject(String.class, queryMessage.get(GUID));
        String timeType = castObject(String.class, queryMessage.get(TIME_TYPE));

        Long localCount = count;

        // Build the SearchRequest and then hand off to the controller for the actual query

        // honor maxResults if count is not specified
        if (localCount == null && maxResults != null) {
            LOGGER.debug("setting count to: {}", maxResults);
            localCount = maxResults;
        }

        Set<String> sourceIds = getSourceIds(sources);
        List<Filter> filters = new ArrayList<Filter>();

        addContextualFilter(filters, searchTerms);
        addTemporalFilter(filters, dateStart, dateEnd, dateOffset, timeType);
        addSpatialFilter(filters, bbox, radius, lat, lon);
        addTypeFilters(filters, type);

        Query query = createQuery(andFilters(filters), startIndex, localCount, sort, maxTimeout);
        SearchRequest searchRequest = new SearchRequest(sourceIds, query, guid);

        try {
            // Hand off to the search controller for the actual query
            searchController.executeQuery(searchRequest, serverSession, subject);
        } catch (RuntimeException re) {
            LOGGER.warn("Exception while executing a query", re);
        }

        LOGGER.debug("EXITING {}", methodName);

    }

    private Set<String> getSourceIds(String sources) {
        Set<String> sourceIds;
        if (StringUtils.equalsIgnoreCase(sources, LOCAL_SOURCE)) {
            LOGGER.debug("Received local query");
            sourceIds = new HashSet<String>(
                    Arrays.asList(searchController.getFramework().getId()));
        } else if (!(StringUtils.isEmpty(sources))) {
            LOGGER.debug("Received source names from client: {}", sources);
            sourceIds = new HashSet<String>(Arrays.asList(StringUtils.stripAll(sources
                    .split(","))));
        } else {
            LOGGER.debug("Received enterprise query");
            SourceInfoResponse sourceInfo = null;
            try {
                sourceInfo = searchController.getFramework()
                        .getSourceInfo(new SourceInfoRequestEnterprise(true));
            } catch (SourceUnavailableException e) {
                LOGGER.debug(
                        "Exception while getting source status. Defaulting to all sources. " +
                                "This could include unavailable sources.", e
                );
            }

            if (sourceInfo != null) {
                sourceIds = new HashSet<String>();
                for (SourceDescriptor source : sourceInfo.getSourceInfo()) {
                    if (source.isAvailable()) {
                        sourceIds.add(source.getSourceId());
                    }
                }
            } else {
                sourceIds = searchController.getFramework().getSourceIds();
            }
        }
        return sourceIds;
    }

    private Filter andFilters(List<Filter> filters) {
        if (filters.size() > 1) {
            return filterBuilder.allOf(filters);
        } else if (filters.size() == 1) {
            return filters.get(0);
        } else {
            LOGGER.warn("Unable to create filter for given input");
            return null;
        }
    }

    /**
     * Create temporal filter
     *
     * @param filters
     * @param dateStart
     * @param dateEnd
     * @param dateOffset
     */
    private void addTemporalFilter(List<Filter> filters, String dateStart, String dateEnd,
            Long dateOffset, String timeType) {
        if (timeType.equals(Metacard.MODIFIED) || timeType.equals(Metacard.EFFECTIVE)) {
            if (StringUtils.isNotBlank(dateStart) || StringUtils.isNotBlank(dateEnd)) {
                filters.add(filterBuilder.attribute(timeType).is().during().dates(
                        parseDate(dateStart), parseDate(dateEnd)));
            } else if (dateOffset != null) {
                filters.add(filterBuilder.attribute(timeType).is().during().last(dateOffset));
            }
        }
        else {
            LOGGER.warn("Invalid timeType passed. Expect {} {} instead got {}", Metacard.MODIFIED, Metacard.EFFECTIVE, timeType);
        }
        
    }

    public static Date parseDate(String date) {
        Date parsedDate = null;
        if (StringUtils.isNotBlank(date)) {
            try {
                parsedDate = dateFormatter.parseDateTime(date).toDate();
            } catch (IllegalArgumentException iae) {
                LOGGER.warn("Could not parse given date: {}", date);
            }
        }
        return parsedDate;
    }

    /**
     * Create contextual filter
     *
     * @param filters
     * @param searchTerms
     */
    private void addContextualFilter(List<Filter> filters, String searchTerms) {
        if (StringUtils.isNotBlank(searchTerms)) {
            filters.add(filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(searchTerms));
        }
    }

    /**
     * Creates SpatialCriterion based on the input parameters, any null values will be ignored
     *
     * @param filters
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
    private void addSpatialFilter(List<Filter> filters, String bbox, Double radius, String lat,
            String lon) {
        if (StringUtils.isNotBlank(bbox)) {
            String wkt = getBboxWkt(bbox);
            if (wkt != null) {
                filters.add(filterBuilder.attribute(Metacard.ANY_GEO).intersecting().wkt(wkt));
            }
        } else if (StringUtils.isNotBlank(lat) && StringUtils.isNotBlank(lon) && StringUtils
                .isNotBlank(radius.toString())) {
            String wkt = getPointWkt(lat, lon);
            filters.add(filterBuilder.attribute(Metacard.ANY_GEO).withinBuffer().wkt(wkt, radius));
        }
    }

    private String getPointWkt(String lat, String lon) {
        return "POINT(" + lon + " " + lat + ")";
    }

    private String getBboxWkt(String bbox) {
        String wkt = null;
        String[] bboxParts = bbox.split(",");

        if (bboxParts.length == 4) {
            double minX = Double.parseDouble(bboxParts[0]);
            double minY = Double.parseDouble(bboxParts[1]);
            double maxX = Double.parseDouble(bboxParts[2]);
            double maxY = Double.parseDouble(bboxParts[3]);

            wkt = "POLYGON((" +
                    minX + " " + minY +
                    "," + minX + " " + maxY +
                    "," + maxX + " " + maxY +
                    "," + maxX + " " + minY +
                    "," + minX + " " + minY +
                    "))";
        }

        return wkt;
    }

    private void addTypeFilters(List<Filter> queryFilters, String types) {
        if (StringUtils.isNotBlank(types)) {
            LOGGER.debug("Received Types: {}", types);
            
            List<Filter> contentTypeFilters = new ArrayList<Filter>();
            
            for (String type : StringUtils.stripAll(types.split(","))) {
                contentTypeFilters.add(filterBuilder.attribute(
                        Metacard.CONTENT_TYPE).is().text(type));
            }   
            
            queryFilters.add(filterBuilder.anyOf(contentTypeFilters));
        } else {
            LOGGER.debug("Received empty content types list");
        }
    }

    /**
     * Creates a new query from the incoming parameters
     *
     * @param filter
     *            - Filter to query
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
    private Query createQuery(Filter filter, Long startIndexLng, Long countLng, String sortStr,
            Long maxTimeoutLng) {
        // default values
        String sortField = Result.TEMPORAL;
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

        // Query must specify a valid sort order if a sort field was specified, i.e., query
        // cannot specify just "date:", must specify "date:asc"
        SortBy sort;
        if ("asc".equalsIgnoreCase(sortOrder)) {
            sort = new SortByImpl(sortField, SortOrder.ASCENDING);
        } else if ("desc".equalsIgnoreCase(sortOrder)) {
            sort = new SortByImpl(sortField, SortOrder.ASCENDING);
        } else {
            throw new IllegalArgumentException(
                    "Incorrect sort order received, must be 'asc' or 'desc'");
        }

        LOGGER.debug("Retrieved query settings: \n sortField: {} \nsortOrder: {}", sortField,
                sortOrder);
        return new QueryImpl(filter, startIndex.intValue(), count.intValue(), sort, true,
                maxTimeout);
    }
}
