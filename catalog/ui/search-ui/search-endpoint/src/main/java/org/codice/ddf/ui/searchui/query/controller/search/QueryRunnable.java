/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.ui.searchui.query.controller.search;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.ui.searchui.query.controller.SearchController;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.codice.ddf.ui.searchui.query.solr.FilteringDynamicSchemaResolver;
import org.cometd.bayeux.server.ServerSession;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.spatial4j.core.context.jts.JtsSpatialContext;
import com.spatial4j.core.context.jts.JtsSpatialContextFactory;
import com.spatial4j.core.distance.DistanceUtils;
import com.spatial4j.core.shape.Shape;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.DistanceResultComparator;
import ddf.catalog.util.impl.RelevanceResultComparator;
import ddf.catalog.util.impl.TemporalResultComparator;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

public abstract class QueryRunnable implements Runnable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(QueryRunnable.class);

    protected static final Map<String, ? extends Serializable> CACHE_PROPERTIES = ImmutableMap.of(
            "mode",
            "cache");

    protected static final Map<String, ? extends Serializable> UPDATE_PROPERTIES = ImmutableMap.of(
            "mode",
            "update");

    protected static final JtsSpatialContextFactory JTS_SPATIAL_CONTEXT_FACTORY =
            new JtsSpatialContextFactory();

    protected static final JtsSpatialContext SPATIAL_CONTEXT =
            JTS_SPATIAL_CONTEXT_FACTORY.newSpatialContext();

    protected static final int FUTURE_TIMEOUT_SECONDS = 60;

    public static final int METERS_IN_KILOMETERS = 1000;

    protected final SearchController searchController;

    protected final SearchRequest request;

    protected final Subject subject;

    protected final Search search;

    protected final ServerSession session;

    protected final Map<String, Result> results;

    public QueryRunnable(SearchController searchController, SearchRequest request, Subject subject,
            Search search, ServerSession session, Map<String, Result> results) {
        this.searchController = searchController;
        this.request = request;
        this.subject = subject;
        this.search = search;
        this.session = session;
        this.results = results;
    }

    public abstract void run();

    protected String extractQueryWkt(Filter filter) {
        String wkt = "";
        try {
            wkt = searchController.getFilterAdapter()
                    .adapt(filter, new WktExtractionFilterDelegate());
        } catch (UnsupportedQueryException e) {
            LOGGER.debug("Unable to extract wkt", e);
        }
        return wkt;
    }

    protected void normalizeDistances(Filter query, Map<String, Result> results) {
        String wkt = extractQueryWkt(query);

        if (StringUtils.isNotBlank(wkt)) {
            Shape queryShape;
            try {
                queryShape = SPATIAL_CONTEXT.readShapeFromWkt(wkt);
            } catch (ParseException e) {
                LOGGER.debug("Unable to parse query WKT to calculate distance", e);
                return;
            }
            for (Map.Entry<String, Result> entry : results.entrySet()) {
                Result result = entry.getValue();
                if (result.getMetacard() != null && StringUtils.isNotBlank(result.getMetacard()
                        .getLocation())) {
                    try {
                        Shape locationShape = SPATIAL_CONTEXT.readShapeFromWkt(result.getMetacard()
                                .getLocation());

                        double distance = DistanceUtils.degrees2Dist(SPATIAL_CONTEXT.calcDistance(
                                locationShape.getCenter(),
                                queryShape.getCenter()), DistanceUtils.EARTH_MEAN_RADIUS_KM)
                                * METERS_IN_KILOMETERS;

                        ResultImpl updatedResult =
                                new ResultImpl(new MetacardImpl(result.getMetacard()));
                        updatedResult.setDistanceInMeters(distance);
                        results.put(entry.getKey(), updatedResult);
                    } catch (ParseException e) {
                        LOGGER.debug("Unable to parse metacard WKT to calculate distance", e);
                    }
                }
            }
        }
    }

    protected void normalizeRelevance(List<Result> indexResults, Map<String, Result> results) {
        for (Result indexResult : indexResults) {
            String resultKey = indexResult.getMetacard()
                    .getAttribute(FilteringDynamicSchemaResolver.SOURCE_ID)
                    .getValue() + indexResult.getMetacard()
                    .getId();

            if (results.containsKey(resultKey)) {
                MetacardImpl metacard = new MetacardImpl(results.get(resultKey)
                        .getMetacard());
                metacard.setAttribute(Search.CACHED, null);

                ResultImpl result = new ResultImpl(metacard);

                result.setRelevanceScore(indexResult.getRelevanceScore());

                results.put(resultKey, result);
            }
        }
    }

    protected int getMaxResults(SearchRequest request) {
        return request.getQuery()
                .getPageSize() > 0 ?
                request.getQuery()
                        .getPageSize() :
                Integer.MAX_VALUE;
    }

    protected void addResults(Collection<Result> responseResults) {
        results.putAll(Maps.uniqueIndex(responseResults, new Function<Result, String>() {
            @Override
            public String apply(Result result) {
                return getResultKey(result.getMetacard());
            }
        }));
    }

    protected String getResultKey(Metacard metacard) {
        return metacard.getSourceId() + ":" + metacard.getId();
    }

    protected Comparator<Result> getResultComparator(Query query) {
        Comparator<Result> sortComparator = new RelevanceResultComparator(SortOrder.DESCENDING);
        SortBy sortBy = query.getSortBy();

        if (sortBy != null && sortBy.getPropertyName() != null) {
            PropertyName sortingProp = sortBy.getPropertyName();
            String sortType = sortingProp.getPropertyName();
            SortOrder sortOrder =
                    (sortBy.getSortOrder() == null) ? SortOrder.DESCENDING : sortBy.getSortOrder();

            // Temporal searches are currently sorted by the effective time
            if (Metacard.EFFECTIVE.equals(sortType) || Result.TEMPORAL.equals(sortType)) {
                sortComparator = new TemporalResultComparator(sortOrder);
            } else if (Metacard.CREATED.equals(sortType) || Metacard.MODIFIED.equals(sortType)) {
                sortComparator = new TemporalResultComparator(sortOrder, sortType);
            } else if (Result.DISTANCE.equals(sortType)) {
                sortComparator = new DistanceResultComparator(sortOrder);
            } else if (Result.RELEVANCE.equals(sortType)) {
                sortComparator = new RelevanceResultComparator(sortOrder);
            }
        }
        return sortComparator;
    }

    protected QueryResponse queryCatalog(String sourceId, SearchRequest searchRequest,
            Subject subject, Map<String, Serializable> properties) {
        Query query = searchRequest.getQuery();
        QueryResponse response = getEmptyResponse(sourceId);
        long startTime = System.currentTimeMillis();

        try {
            if (query != null) {
                List<String> sourceIds;
                if (sourceId == null) {
                    sourceIds = new ArrayList<>(searchRequest.getSourceIds());
                } else {
                    sourceIds = Collections.singletonList(sourceId);
                }
                QueryRequest request = new QueryRequestImpl(query, false, sourceIds, properties);

                if (subject != null) {
                    LOGGER.debug("Adding {} property with value {} to request.",
                            SecurityConstants.SECURITY_SUBJECT,
                            subject);
                    request.getProperties()
                            .put(SecurityConstants.SECURITY_SUBJECT, subject);
                }

                LOGGER.debug("Sending query: {}", query);
                response = searchController.getFramework()
                        .query(request);
            }
        } catch (UnsupportedQueryException | FederationException e) {
            LOGGER.warn("Error executing query", e);
            response.getProcessingDetails()
                    .add(new ProcessingDetailsImpl(sourceId, e));
        } catch (SourceUnavailableException e) {
            LOGGER.warn("Error executing query because the underlying source was unavailable.", e);
            response.getProcessingDetails()
                    .add(new ProcessingDetailsImpl(sourceId, e));
        } catch (RuntimeException e) {
            // Account for any runtime exceptions and send back a server error
            // this prevents full stacktraces returning to the client
            // this allows for a graceful server error to be returned
            LOGGER.warn("RuntimeException on executing query", e);
            response.getProcessingDetails()
                    .add(new ProcessingDetailsImpl(sourceId, e));
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        response.getProperties()
                .put("elapsed", estimatedTime);

        return response;
    }

    private QueryResponse getEmptyResponse(String sourceId) {
        // No query was specified
        QueryRequest queryRequest = new QueryRequestImpl(null,
                false,
                Collections.singletonList(sourceId),
                null);

        // Create a dummy QueryResponse with zero results
        return new QueryResponseImpl(queryRequest, new ArrayList<Result>(), 0);
    }

}
