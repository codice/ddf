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
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.post;
import static spark.route.RouteOverview.enableRouteOverview;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import com.google.common.collect.ImmutableMap;

import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import spark.Request;
import spark.Response;
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

    // @formatter:off
    private static Map<String, Consumer<Response>> acceptedEncodings = ImmutableMap.of(
            "identity", (res) -> {},
            "gzip", (res) -> res.header("Content-Encoding", "gzip")
    );
    // @formatter:on

    @Override
    public void init() {

        post("/cql", APPLICATION_JSON, (req, res) -> {
            CqlRequest cqlRequest = mapper.readValue(req.body(), CqlRequest.class);

            CqlQueryResponse cqlQueryResponse = executeCqlQuery(cqlRequest);

            return mapper.toJson(cqlQueryResponse);
        });

        before("/cql", (req, res) -> {
            res.type(APPLICATION_JSON);

            // Must manually check and set header for gzip because of spark issue
            // https://github.com/perwendel/spark/issues/691
            final Map<String, String> REQ_ACCEPT_ENCODINGS = parseAcceptEncodings(req.headers(
                    "Accept-Encoding"));

            /*
             * An example of this stream in action.
             * Accept-Encoding: gzip;q=1,identity;q=0.5,notsupportedencoding;q=0.8
             * take each entry in REQ_ACCEPT_ENCODINGS and sort it according to the q value:
             *   (gzip, 1), (notsupportedencoding, 0.8), (identity, 0.5)
             * filter out any encodings we dont support (by checking acceptEncodings):
             *   (gzip, 1), (identity, 0.5)
             * make sure the client actually requested this header (acceptEncodingSupports method):
             *   gzip -> true, identity -> true.
             * get the encoding function from the acceptedEncodings map
             * find the first one (which should be ordered by highest priority first)
             * and finally apply the consumer to the result.
             */

            // Sort Values by descending Q value
            Stream<Map.Entry<String, String>> values = REQ_ACCEPT_ENCODINGS.entrySet()
                    .stream()
                    .sorted(Comparator.comparingDouble((Map.Entry<String, String> a) -> Double.parseDouble(
                            a.getValue()))
                            .reversed());

            // Add on identity as a fallback at the very end of the list if identity isn't already
            // defined and "*" is not explicitly restricted.
            if (!REQ_ACCEPT_ENCODINGS.containsKey("identity")
                    && !"0".equals(REQ_ACCEPT_ENCODINGS.get("*"))) {
                values = Stream.concat(values,
                        Stream.of(new AbstractMap.SimpleEntry<String, String>("identity", "1")));
            }

            values.filter((entry) -> acceptedEncodings.containsKey(entry.getKey()) || "*".equals(
                    entry.getKey()))
                    .filter((entry) -> acceptEncodingSupports(REQ_ACCEPT_ENCODINGS, entry.getKey()))
                    .map((entry) -> acceptedEncodings.get(entry.getKey()))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(NotAcceptableException::new)
                    .accept(res);
        });

        after("/cql", (Request req, Response res) -> {
            // post route logic
        });

        exception(NotAcceptableException.class, (e, request, response) -> {
            response.status(406);
            response.body("Unsupported encoding");
            LOGGER.debug("Client asked for unsupported encoding", e);
        });

        exception(NumberFormatException.class, (e, request, response) -> {
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
     * returns a Map of the parsed Accept-Encoding header according to RFC-7231#5.3.2
     * </br>
     * https://tools.ietf.org/html/rfc7231#section-5.3.2
     *
     * @param header Accept-Encoding header to parse
     * @return Map of {Encoding, Q value}
     */
    private Map<String, String> parseAcceptEncodings(String header) {
        if (StringUtils.isBlank(header)) {
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