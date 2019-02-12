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
package org.codice.ddf.catalog.ui.query;

import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.QueryFunction;
import ddf.catalog.util.impl.ResultIterable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.boon.json.implementation.ObjectMapperImpl;
import org.codice.ddf.catalog.ui.metacard.EntityTooLargeException;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.query.geofeature.FeatureService;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.catalog.ui.ws.JsonRpc;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class QueryApplication implements SparkApplication, Function {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryApplication.class);

  private static final String APPLICATION_JSON = "application/json";

  private CatalogFramework catalogFramework;

  private FilterBuilder filterBuilder;

  private FilterAdapter filterAdapter;

  private ActionRegistry actionRegistry;

  private FeatureService featureService;

  private ObjectMapper mapper =
      new ObjectMapperImpl(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .includeEmpty()
              .includeNulls()
              .includeDefaultValues()
              .setJsonFormatForDates(false));

  private EndpointUtil util;

  @Override
  public void init() {
    before(
        (req, res) -> {
          res.type(APPLICATION_JSON);
        });

    post(
        "/cql",
        APPLICATION_JSON,
        (req, res) -> {
          CqlRequest cqlRequest = mapper.readValue(util.safeGetBody(req), CqlRequest.class);
          CqlQueryResponse cqlQueryResponse = executeCqlQuery(cqlRequest);
          return mapper.toJson(cqlQueryResponse);
        });

    after(
        "/cql",
        (req, res) -> {
          res.header("Content-Encoding", "gzip");
        });

    get(
        "/geofeature/suggestions",
        (req, res) -> {
          String query = req.queryParams("q");
          List<Suggestion> results = this.featureService.getSuggestedFeatureNames(query, 10);
          return mapper.toJson(results);
        });

    get(
        "/geofeature",
        (req, res) -> {
          String id = req.queryParams("id");
          SimpleFeature feature = this.featureService.getFeatureById(id);
          if (feature == null) {
            res.status(404);
            return mapper.toJson(ImmutableMap.of("message", "Feature not found"));
          }
          return new FeatureJSON().toString(feature);
        });

    exception(
        UnsupportedQueryException.class,
        (e, request, response) -> {
          response.status(400);
          response.header(CONTENT_TYPE, APPLICATION_JSON);
          response.body(mapper.toJson(ImmutableMap.of("message", "Unsupported query request.")));
          LOGGER.error("Query endpoint failed", e);
        });

    exception(IOException.class, util::handleIOException);

    exception(EntityTooLargeException.class, util::handleEntityTooLargeException);

    exception(RuntimeException.class, util::handleRuntimeException);

    exception(
        Exception.class,
        (e, request, response) -> {
          response.status(500);
          response.header(CONTENT_TYPE, APPLICATION_JSON);
          response.body(
              mapper.toJson(ImmutableMap.of("message", "Error while processing query request.")));
          LOGGER.error("Query endpoint failed", e);
        });
  }

  @Override
  public Object apply(Object req) {
    if (!(req instanceof List)) {
      return JsonRpc.invalidParams("parameters not a list", req);
    }

    List params = (List) req;

    if (params.size() != 1) {
      return JsonRpc.invalidParams("must pass exactly 1 parameter", params);
    }

    Object param = params.get(0);

    if (!(param instanceof String)) {
      return JsonRpc.invalidParams("parameter not a string", param);
    }

    CqlRequest cqlRequest;

    try {
      cqlRequest = mapper.readValue((String) param, CqlRequest.class);
    } catch (RuntimeException e) {
      return JsonRpc.invalidParams("parameter not valid json", param);
    }

    try {
      return executeCqlQuery(cqlRequest);
    } catch (UnsupportedQueryException e) {
      LOGGER.error("Query endpoint failed", e);
      return JsonRpc.error(400, "Unsupported query request.");
    } catch (RuntimeException e) {
      LOGGER.debug("Exception occurred", e);
      return JsonRpc.error(404, "Could not find what you were looking for");
    } catch (Exception e) {
      LOGGER.error("Query endpoint failed", e);
      return JsonRpc.error(500, "Error while processing query request.");
    }
  }

  private CqlQueryResponse executeCqlQuery(CqlRequest cqlRequest)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    QueryRequest request = cqlRequest.createQueryRequest(catalogFramework.getId(), filterBuilder);
    Stopwatch stopwatch = Stopwatch.createStarted();

    List<QueryResponse> responses = Collections.synchronizedList(new ArrayList<>());
    QueryFunction queryFunction =
        (queryRequest) -> {
          QueryResponse queryResponse = catalogFramework.query(queryRequest);
          responses.add(queryResponse);
          return queryResponse;
        };

    List<Result> results =
        ResultIterable.resultIterable(queryFunction, request, cqlRequest.getCount())
            .stream()
            .collect(Collectors.toList());

    QueryResponse response =
        new QueryResponseImpl(
            request,
            results,
            true,
            responses
                .stream()
                .filter(Objects::nonNull)
                .map(QueryResponse::getHits)
                .findFirst()
                .orElse(-1l),
            responses
                .stream()
                .filter(Objects::nonNull)
                .map(QueryResponse::getProperties)
                .findFirst()
                .orElse(Collections.emptyMap()));

    stopwatch.stop();

    return new CqlQueryResponse(
        cqlRequest.getId(),
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

  public void setFeatureService(FeatureService featureService) {
    this.featureService = featureService;
  }

  public void setEndpointUtil(EndpointUtil util) {
    this.util = util;
  }
}
