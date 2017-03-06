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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.ws.rs.NotAcceptableException;

import org.apache.commons.lang3.StringUtils;
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

    private static Pattern commaSpace = Pattern.compile(",\\s?");

    private static Pattern semicolon = Pattern.compile(";\\s?");

    @Override
    public void init() {

        post("/cql", APPLICATION_JSON, (req, res) -> {
            CqlRequest cqlRequest = mapper.readValue(req.body(), CqlRequest.class);

            CqlQueryResponse cqlQueryResponse = executeCqlQuery(cqlRequest);

            return mapper.toJson(cqlQueryResponse);
        });

        after("/cql", (req, res) -> {
            res.type(APPLICATION_JSON);

            Map<String, String> acceptEncodings =
                    parseAcceptEncodings(req.headers("Accept-Encoding"));

            if (acceptEncodingSupports(acceptEncodings, "gzip")) {
                res.header("Content-Encoding", "gzip");
            } else if (acceptEncodingSupports(acceptEncodings, "identity")) {
                //do nothing, send as identity
            } else {
                throw new NotAcceptableException();
            }
        });

        exception(NotAcceptableException.class, (e, request, response) -> {
            response.status(406);
            response.body("Unsupported encoding");
            LOGGER.debug("Client asked for unsupported encoding", e);
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

    /**
     * returns a Map of the parsed Accept-Encoding header according to RFC-2616-14.2
     * </br>
     * https://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
     *
     * @param header Accept-Encoding header to parse
     * @return Map of {Encoding, Q value}
     */
    private Map<String, String> parseAcceptEncodings(String header) {
        if (StringUtils.isEmpty(header)) {
            return Collections.emptyMap();
        }
        String[] encodings = commaSpace.split(header.trim());
        return Arrays.stream(encodings)
                .map(semicolon::split)
                .collect(Collectors.toMap(v -> v[0],
                        // strip off 'q=' to get q value, or default to 1
                        v -> v.length > 1 ? v[1].substring(2) : "1"));
    }

    private boolean acceptEncodingSupports(Map<String, String> acceptEncodings, String encoding) {
        // If encoding is present and has a nonzero Q value, accepted
        if (!acceptEncodings.getOrDefault(encoding, "0")
                .equals("0")) {
            return true;
        }

        // If encoding is present and has a zero Q value, denied
        if (acceptEncodings.containsKey(encoding) && acceptEncodings.get(encoding)
                .equals("0")) {
            return false;
        }

        // if '*' is present and has nonzero Q value, accepted
        if (!acceptEncodings.getOrDefault("*", "0")
                .equals("0")) {
            return true;
        }

        if (encoding.equals("identity")) {
            // explicitly denies identity with identity;q=0
            if (acceptEncodings.containsKey("identity") && acceptEncodings.get("identity")
                    .equals("0")) {
                return false;
            }
            // '*' is denied and identity not explicitly included
            if (acceptEncodings.containsKey("*") && acceptEncodings.get("*")
                    .equals("0") && !acceptEncodings.containsKey("identity")) {
                return false;
            }
            // otherwise identity is always allowed
            return true;
        }
        return false;
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