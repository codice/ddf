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

import static spark.Spark.after;
import static spark.Spark.exception;
import static spark.Spark.post;
import static spark.route.RouteOverview.enableRouteOverview;

import java.util.concurrent.TimeUnit;

import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.boon.json.implementation.ObjectMapperImpl;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import spark.servlet.SparkApplication;

public class QueryApplication implements SparkApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryApplication.class);

    private static final String APPLICATION_JSON = "application/json";

    private CatalogFramework catalogFramework;

    private FilterBuilder filterBuilder;

    private FilterAdapter filterAdapter;

    private ActionRegistry actionRegistry;

    private ObjectMapper mapper = new ObjectMapperImpl(new JsonParserFactory().usePropertyOnly(),
            new JsonSerializerFactory().includeEmpty()
                    .includeNulls()
                    .includeDefaultValues());

    @Override
    public void init() {

        post("/cql", APPLICATION_JSON, (req, res) -> {
            CqlRequest cqlRequest = mapper.readValue(req.body(), CqlRequest.class);

            CqlQueryResponse cqlQueryResponse = executeCqlQuery(cqlRequest);

            return mapper.toJson(cqlQueryResponse);
        });

        after((req, res) -> {
            res.type(APPLICATION_JSON);
            res.header("Content-Encoding", "gzip");
        });

        exception(UnsupportedQueryException.class, (e, request, response) -> {
            response.status(400);
            response.body("Unsupported query request.");
            LOGGER.error("Query endpoint failed", e);
        });

        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            response.body("Error while processing query request.");
            LOGGER.error("Query endpoint failed", e);
        });

        enableRouteOverview();

    }

    private CqlQueryResponse executeCqlQuery(CqlRequest cqlRequest)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        QueryRequest request = cqlRequest.createQueryRequest(catalogFramework.getId(),
                filterBuilder);

        Stopwatch stopwatch = Stopwatch.createStarted();
        QueryResponse response = catalogFramework.query(request);
        stopwatch.stop();

        return new CqlQueryResponse(cqlRequest.getId(),
                request,
                response,
                cqlRequest.getSource(),
                stopwatch.elapsed(TimeUnit.MILLISECONDS),
                cqlRequest.isNormalize(),
                filterAdapter,
                actionRegistry);
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