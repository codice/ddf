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
import ddf.catalog.plugin.OauthPluginException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.apache.http.HttpStatus;
import org.codice.ddf.catalog.ui.metacard.EntityTooLargeException;
import org.codice.ddf.catalog.ui.query.cql.CqlRequestImpl;
import org.codice.ddf.catalog.ui.query.cql.SourceWarningsFilterManager;
import org.codice.ddf.catalog.ui.query.geofeature.FeatureService;
import org.codice.ddf.catalog.ui.query.handlers.CqlTransformHandler;
import org.codice.ddf.catalog.ui.query.suggestion.DmsCoordinateProcessor;
import org.codice.ddf.catalog.ui.query.suggestion.LatLonCoordinateProcessor;
import org.codice.ddf.catalog.ui.query.suggestion.MgrsCoordinateProcessor;
import org.codice.ddf.catalog.ui.query.suggestion.UtmUpsCoordinateProcessor;
import org.codice.ddf.catalog.ui.query.utility.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.validate.CqlValidationHandler;
import org.codice.ddf.catalog.ui.util.CqlQueriesImpl;
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

  private final SourceWarningsFilterManager sourceWarningsFilterManager;

  private FeatureService featureService;

  private CqlTransformHandler cqlTransformHandler;

  private CqlValidationHandler cqlValidationHandler;

  private EndpointUtil util;

  private CqlQueriesImpl cqlQueryUtil;

  public QueryApplication(
      CqlTransformHandler cqlTransformHandler,
      CqlValidationHandler cqlValidationHandler,
      LatLonCoordinateProcessor latLonCoordinateProcessor,
      DmsCoordinateProcessor dmsCoordinateProcessor,
      MgrsCoordinateProcessor mgrsCoordinateProcessor,
      UtmUpsCoordinateProcessor utmUpsCoordinateProcessor,
      SourceWarningsFilterManager sourceWarningsFilterManager) {
    this.latLonCoordinateProcessor = latLonCoordinateProcessor;
    this.dmsCoordinateProcessor = dmsCoordinateProcessor;
    this.mgrsCoordinateProcessor = mgrsCoordinateProcessor;
    this.utmUpsCoordinateProcessor = utmUpsCoordinateProcessor;
    this.cqlTransformHandler = cqlTransformHandler;
    this.cqlValidationHandler = cqlValidationHandler;
    this.sourceWarningsFilterManager = sourceWarningsFilterManager;
  }

  @Override
  public void init() {
    before((req, res) -> res.type(APPLICATION_JSON));

    post(
        "/cql",
        APPLICATION_JSON,
        (req, res) -> {
          try {
            CqlRequestImpl cqlRequest = GSON.fromJson(util.safeGetBody(req), CqlRequestImpl.class);
            CqlQueryResponse cqlQueryResponse = cqlQueryUtil.executeCqlQuery(cqlRequest);
            if (sourceWarningsFilterManager != null
                && cqlQueryResponse != null
                && cqlQueryResponse.getQueryResponse() != null) {
              cqlQueryResponse
                  .getQueryResponse()
                  .getProcessingDetails()
                  .stream()
                  .map(sourceWarningsFilterManager::getFilteredWarningsFrom)
                  .forEach(cqlQueryResponse::addToWarnings);
            }
            return GSON.toJson(cqlQueryResponse);
          } catch (OauthPluginException e) {
            res.status(HttpStatus.SC_UNAUTHORIZED);
            Map<String, String> responseMap =
                ImmutableMap.of(ID_KEY, e.getSourceId(), URL_KEY, e.getProviderUrl());
            return GSON.toJson(responseMap);
          }
        });

    post("/cql/transform/:transformerId", cqlTransformHandler, GSON::toJson);

    post("/cql/validator/:validatorId", cqlValidationHandler, GSON::toJson);

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

    CqlRequestImpl cqlRequest;

    try {
      cqlRequest = GSON.fromJson((String) param, CqlRequestImpl.class);
    } catch (RuntimeException e) {
      return JsonRpc.invalidParams("parameter not valid json", param);
    }

    try {
      return cqlQueryUtil.executeCqlQuery(cqlRequest);
    } catch (OauthPluginException e) {
      Map<String, String> responseMap =
          ImmutableMap.of(ID_KEY, e.getSourceId(), URL_KEY, e.getProviderUrl());
      return JsonRpc.error(HttpStatus.SC_UNAUTHORIZED, GSON.toJson(responseMap));
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

  public void setCqlQueryUtil(CqlQueriesImpl cqlQueryUtil) {
    this.cqlQueryUtil = cqlQueryUtil;
  }
}
