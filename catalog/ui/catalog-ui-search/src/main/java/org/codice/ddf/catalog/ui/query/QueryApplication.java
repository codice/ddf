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

import com.google.common.collect.ImmutableMap;
import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.boon.json.implementation.ObjectMapperImpl;
import org.codice.ddf.catalog.ui.metacard.EntityTooLargeException;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.query.geofeature.FeatureService;
import org.codice.ddf.catalog.ui.query.handlers.CqlTransformHandler;
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

  private CqlTransformHandler cqlTransformHandler;

  private ObjectMapper mapper =
      new ObjectMapperImpl(
          new JsonParserFactory().usePropertyOnly(),
          new JsonSerializerFactory()
              .includeEmpty()
              .includeNulls()
              .includeDefaultValues()
              .setJsonFormatForDates(false)
              .useAnnotations());

  private EndpointUtil util;

  public QueryApplication(CqlTransformHandler cqlTransformHandler) {
    this.cqlTransformHandler = cqlTransformHandler;
  }

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
          CqlQueryResponse cqlQueryResponse = util.executeCqlQuery(cqlRequest);
          return mapper.toJson(cqlQueryResponse);
        });

    after(
        "/cql",
        (req, res) -> {
          res.header("Content-Encoding", "gzip");
        });

    post("/cql/transform/:transformerId", cqlTransformHandler, mapper::toJson);

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
      return JsonRpc.invalidParams("params not list", req);
    }

    List params = (List) req;

    if (params.size() != 1) {
      return JsonRpc.invalidParams("must pass exactly 1 param", params);
    }

    Object param = params.get(0);

    if (!(param instanceof String)) {
      return JsonRpc.invalidParams("param not string", param);
    }

    CqlRequest cqlRequest;

    try {
      cqlRequest = mapper.readValue((String) param, CqlRequest.class);
    } catch (RuntimeException e) {
      return JsonRpc.invalidParams("param not valid json", param);
    }

    try {
      return util.executeCqlQuery(cqlRequest);
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
