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
 */
package org.codice.ddf.catalog.ui.query;

import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.halt;
import static spark.Spark.post;
import static spark.route.RouteOverview.enableRouteOverview;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.catalog.ui.query.cql.response.CqlQueryResponse;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import spark.servlet.SparkApplication;

public class QueryApplication implements SparkApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryApplication.class);

    private static final String LOCAL_SOURCE = "local";

    private static final String CACHE_SOURCE = "cache";

    private static final String ID = "id";

    private static final String SOURCE = "src";

    private static final String MAX_TIMEOUT = "timeout";

    private static final String START_INDEX = "start";

    private static final String COUNT = "count";

    private static final String CQL_FILTER = "cql";

    private static final String SORT = "sort";

    private static final String DEFAULT_SORT_ORDER = "desc";

    private static final long DEFAULT_TIMEOUT = 300000;

    private static final long DEFAULT_COUNT = 10;

    private static final long DEFAULT_START_INDEX = 1;

    public static final String APPLICATION_JSON = "application/json";

    private CatalogFramework catalogFramework;

    private FilterBuilder filterBuilder;

    private FilterAdapter filterAdapter;

    private ActionRegistry actionRegistry;

    @Override
    public void init() {

        post("/cql", APPLICATION_JSON, (req, res) -> {
            ObjectMapper mapper = JsonFactory.createUseAnnotations(true);
            Map body =  mapper.readValue(req.body(), Map.class);

            String source = parseSource(body.get(SOURCE));
            Long maxTimeout = parseLong(body.get(MAX_TIMEOUT), DEFAULT_TIMEOUT);
            Long startIndex = parseLong(body.get(START_INDEX), DEFAULT_START_INDEX);
            Long count = parseLong(body.get(COUNT), DEFAULT_COUNT);
            Filter filter = parseCql(body.get(CQL_FILTER));
            SortBy sort = parseSort(body.get(SORT));
            String id = castObject(String.class, body.get(ID));

            Query query = new QueryImpl(filter,
                    startIndex.intValue(),
                    count.intValue(),
                    sort,
                    true,
                    maxTimeout);

            QueryRequest request;
            if (CACHE_SOURCE.equals(source)) { // TODO check if cache disabled
                request = new QueryRequestImpl(query, true);
                request.getProperties().put("mode", "cache");
            } else {
                request = new QueryRequestImpl(query, Collections.singleton(source));
                request.getProperties().put("mode", "update");
            }

            Stopwatch stopwatch = Stopwatch.createStarted();
            QueryResponse response = catalogFramework.query(request);
            stopwatch.stop();

            CqlQueryResponse cqlQueryResponse = new CqlQueryResponse(id, response, source, stopwatch.elapsed(
                    TimeUnit.MILLISECONDS), filterAdapter, actionRegistry);

            res.type(APPLICATION_JSON);
            res.header("Content-Encoding", "gzip");
            return mapper.toJson(cqlQueryResponse);
        });

        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            response.body(e.toString());
            LOGGER.error("Query endpoint failed", e);
        });

        enableRouteOverview();



        // TODO remove test methods
        get("/name/:name", (req, res) -> "Hello " + req.params(":name"));

        get("/world", (req, res) -> "Hello World");

        get("/test/*", (req, res) -> "Test");

    }

    private Long parseLong(Object value, Long defaultValue) {
        Long timeout = castObject(Long.class, value);

        if (timeout == null) {
            timeout = defaultValue;
        }

        return timeout;
    }

    private String parseSource(Object value) {
        String src = castObject(String.class, value);

        if (StringUtils.equalsIgnoreCase(src, LOCAL_SOURCE) || StringUtils.isBlank(src)) {
            src = catalogFramework.getId();
        }

        return src;
    }

    private Filter parseCql(Object value) {
        String cql = castObject(String.class, value);

        Filter filter = null;
        try {
            filter = ECQL.toFilter(cql);
        } catch (CQLException e) {
            halt(400, "Unable to parse CQL filter");
        }

        if (filter == null) {
            LOGGER.debug("Received an empty filter. Using a wildcard contextual filter instead.");
            filter = filterBuilder.attribute(Metacard.ANY_TEXT)
                    .is()
                    .like()
                    .text(FilterDelegate.WILDCARD_CHAR);
        }

        return filter;
    }

    private SortBy parseSort(Object value) {
        String sortStr = castObject(String.class, value);

        // default values
        String sortField = Result.TEMPORAL;
        String sortOrder = DEFAULT_SORT_ORDER;

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
            sort = new SortByImpl(sortField, SortOrder.DESCENDING);
        } else {
            throw new IllegalArgumentException(
                    "Incorrect sort order received, must be 'asc' or 'desc'");
        }

        LOGGER.debug("Retrieved query settings: \n sortField: {} \nsortOrder: {}",
                sortField,
                sortOrder);

        return sort;
    }

    @SuppressWarnings("unchecked")
    private <T> T castObject(Class<T> targetClass, Object o) {
        if (o != null) {
            if (o instanceof Number) {
                if (targetClass.equals(Double.class)) {
                    return (T) Double.valueOf(((Number) o).doubleValue());
                } else if (targetClass.equals(Long.class)) {
                    return (T) Long.valueOf(((Number) o).longValue());
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

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setFilterAdapter(FilterAdapter filterAdapter) {
        this.filterAdapter = filterAdapter;
    }

    public void setActionRegistry(ActionRegistry actionRegistry) {
        this.actionRegistry = actionRegistry;
    }
}