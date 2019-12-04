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
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.plugin.OAuthPluginException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import org.codice.ddf.catalog.ui.metacard.EntityTooLargeException;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.query.geofeature.FeatureService;
import org.codice.ddf.catalog.ui.query.handlers.CqlTransformHandler;
import org.codice.ddf.catalog.ui.query.suggestion.DmsCoordinateProcessor;
import org.codice.ddf.catalog.ui.query.suggestion.LatLonCoordinateProcessor;
import org.codice.ddf.catalog.ui.query.suggestion.MgrsCoordinateProcessor;
import org.codice.ddf.catalog.ui.query.suggestion.UtmUpsCoordinateProcessor;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.catalog.ui.ws.JsonRpc;
import org.codice.ddf.spatial.geocoding.Suggestion;
import org.codice.gsonsupport.GsonTypeAdapters.DateLongFormatTypeAdapter;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.geotools.geojson.feature.FeatureJSON;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.servlet.SparkApplication;

public class QueryApplication implements SparkApplication, Function {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryApplication.class);

  private static final String APPLICATION_JSON = "application/json";

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .registerTypeAdapter(Date.class, new DateLongFormatTypeAdapter())
          .create();

  private static final String MESSAGE = "message";

  private static final String QUERY_ENDPOINT_FAILED = "Query endpoint failed";

  private static final String ID_KEY = "id";

  private static final String URL_KEY = "url";

  private final LatLonCoordinateProcessor latLonCoordinateProcessor;

  private final DmsCoordinateProcessor dmsCoordinateProcessor;

  private final MgrsCoordinateProcessor mgrsCoordinateProcessor;

  private final UtmUpsCoordinateProcessor utmUpsCoordinateProcessor;

  private FeatureService featureService;

  private CqlTransformHandler cqlTransformHandler;

  private EndpointUtil util;

  public QueryApplication(
      CqlTransformHandler cqlTransformHandler,
      LatLonCoordinateProcessor latLonCoordinateProcessor,
      DmsCoordinateProcessor dmsCoordinateProcessor,
      MgrsCoordinateProcessor mgrsCoordinateProcessor,
      UtmUpsCoordinateProcessor utmUpsCoordinateProcessor) {
    this.latLonCoordinateProcessor = latLonCoordinateProcessor;
    this.dmsCoordinateProcessor = dmsCoordinateProcessor;
    this.mgrsCoordinateProcessor = mgrsCoordinateProcessor;
    this.utmUpsCoordinateProcessor = utmUpsCoordinateProcessor;
    this.cqlTransformHandler = cqlTransformHandler;
  }

  @Override
  public void init() {
    before((req, res) -> res.type(APPLICATION_JSON));

    post(
        "/cql",
        APPLICATION_JSON,
        (req, res) -> {
          try {
            CqlRequest cqlRequest = GSON.fromJson(util.safeGetBody(req), CqlRequest.class);
            CqlQueryResponse cqlQueryResponse = util.executeCqlQuery(cqlRequest);
            return GSON.toJson(cqlQueryResponse);
          } catch (OAuthPluginException e) {
            res.status(e.getErrorType().getStatusCode());
            return GSON.toJson(ImmutableMap.of(ID_KEY, e.getSourceId(), URL_KEY, e.getUrl()));
          }
        });

    post("/cql/transform/:transformerId", cqlTransformHandler, GSON::toJson);

    get(
        "/geofeature/suggestions",
        (req, res) -> {
          String query = req.queryParams("q");
          List<Suggestion> results = this.featureService.getSuggestedFeatureNames(query, 10);
          List<Suggestion> efficientPrependingResults = new LinkedList<>(results);
          this.utmUpsCoordinateProcessor.enhanceResults(efficientPrependingResults, query);
          this.mgrsCoordinateProcessor.enhanceResults(efficientPrependingResults, query);
          this.dmsCoordinateProcessor.enhanceResults(efficientPrependingResults, query);
          this.latLonCoordinateProcessor.enhanceResults(efficientPrependingResults, query);
          return GSON.toJson(efficientPrependingResults);
        });

    get(
        "/geofeature",
        (req, res) -> {
          String id = req.queryParams("id");
          SimpleFeature feature = this.featureService.getFeatureById(id);
          if (feature == null) {
            res.status(404);
            return GSON.toJson(ImmutableMap.of(MESSAGE, "Feature not found"));
          }
          return new FeatureJSON().toString(feature);
        });

    exception(
        UnsupportedQueryException.class,
        (e, request, response) -> {
          response.status(400);
          response.header(CONTENT_TYPE, APPLICATION_JSON);
          response.body(GSON.toJson(ImmutableMap.of(MESSAGE, "Unsupported query request.")));
          LOGGER.error(QUERY_ENDPOINT_FAILED, e);
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
              GSON.toJson(ImmutableMap.of(MESSAGE, "Error while processing query request.")));
          LOGGER.error(QUERY_ENDPOINT_FAILED, e);
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
      cqlRequest = GSON.fromJson((String) param, CqlRequest.class);
    } catch (RuntimeException e) {
      return JsonRpc.invalidParams("parameter not valid json", param);
    }

    try {
      return util.executeCqlQuery(cqlRequest);
    } catch (OAuthPluginException e) {
      return JsonRpc.error(
          e.getErrorType().getStatusCode(),
          GSON.toJson(ImmutableMap.of(ID_KEY, e.getSourceId(), URL_KEY, e.getUrl())));
    } catch (UnsupportedQueryException e) {
      LOGGER.error(QUERY_ENDPOINT_FAILED, e);
      return JsonRpc.error(400, "Unsupported query request.");
    } catch (RuntimeException e) {
      LOGGER.debug("Exception occurred", e);
      return JsonRpc.error(404, "Could not find what you were looking for");
    } catch (Exception e) {
      LOGGER.error(QUERY_ENDPOINT_FAILED, e);
      return JsonRpc.error(500, "Error while processing query request.");
    }
  }

  public void setFeatureService(FeatureService featureService) {
    this.featureService = featureService;
  }

  public void setEndpointUtil(EndpointUtil util) {
    this.util = util;
  }
}
