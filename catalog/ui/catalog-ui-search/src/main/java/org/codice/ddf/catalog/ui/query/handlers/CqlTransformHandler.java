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
package org.codice.ddf.catalog.ui.query.handlers;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.util.impl.QueryFunction;
import ddf.catalog.util.impl.ResultIterable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.boon.json.JsonParserFactory;
import org.boon.json.JsonSerializerFactory;
import org.boon.json.ObjectMapper;
import org.boon.json.implementation.ObjectMapperImpl;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.utils.IOUtils;

public class CqlTransformHandler implements Route {

  private static final Logger LOGGER = LoggerFactory.getLogger(CqlTransformHandler.class);

  private List<ServiceReference> queryResponseTransformers;

  private BundleContext bundleContext;

  private FilterBuilder filterBuilder;

  private FilterAdapter filterAdapter;

  private CatalogFramework catalogFramework;

  private ActionRegistry actionRegistry;

  private final String GZIP = "gzip";

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

  public CqlTransformHandler(
      List<ServiceReference> queryResponseTransformers, BundleContext bundleContext) {
    this.queryResponseTransformers = queryResponseTransformers;
    this.bundleContext = bundleContext;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    String transformerId = request.params(":transformerId");
    String body = util.safeGetBody(request);
    CqlRequest cqlRequest = mapper.readValue(body, CqlRequest.class);
    CqlQueryResponse cqlQueryResponse = executeCqlQuery(cqlRequest);

    LOGGER.trace("Finding transformer to transform query response.");

    ServiceReference<QueryResponseTransformer> queryResponseTransformer =
        queryResponseTransformers
            .stream()
            .filter(transformer -> transformer.getProperty("id").equals(transformerId))
            .findFirst()
            .orElse(null);

    if (queryResponseTransformer == null) {
      LOGGER.debug("Could not find transformer with id: {}", transformerId);
      response.status(HttpStatus.NOT_FOUND_404);
      return mapper.toJson(ImmutableMap.of("message", "Service not found"));
    }

    attachFileWithTransformer(queryResponseTransformer, cqlQueryResponse, request, response);

    return "";
  }

  private void attachFileWithTransformer(
      ServiceReference<QueryResponseTransformer> queryResponseTransformer,
      CqlQueryResponse cqlQueryResponse,
      Request request,
      Response response)
      throws CatalogTransformerException, MimeTypeException, IOException {

    BinaryContent content;

    content =
        bundleContext
            .getService(queryResponseTransformer)
            .transform(cqlQueryResponse.getQueryResponse(), new HashMap<>());

    String mimeType = (String) queryResponseTransformer.getProperty("mime-type");
    MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
    String fileExt;

    fileExt = allTypes.forName(mimeType).getExtension();

    String acceptEncoding = request.headers(HttpHeaders.ACCEPT_ENCODING);

    boolean shouldGzip =
        StringUtils.isNotBlank(acceptEncoding) && acceptEncoding.toLowerCase().contains(GZIP);

    response.type(mimeType);
    String attachment =
        String.format("attachment;filename=export-%s%s", Instant.now().toString(), fileExt);
    response.header(HttpHeaders.CONTENT_DISPOSITION, attachment);

    try (OutputStream servletOutputStream = response.raw().getOutputStream();
        InputStream resultStream = content.getInputStream()) {
      if (shouldGzip) {
        response.header(HttpHeaders.CONTENT_ENCODING, GZIP);
        LOGGER.trace("Request header accepts gzip");
        try (OutputStream gzipServletOutputStream = new GZIPOutputStream(servletOutputStream)) {
          IOUtils.copy(resultStream, gzipServletOutputStream);
        }
      } else {
        IOUtils.copy(resultStream, servletOutputStream);
      }
    }

    response.status(HttpStatus.OK_200);
    LOGGER.trace(
        "Response sent to transformer id {} in {} file format.",
        queryResponseTransformer.getProperty("id"),
        fileExt);
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

  public void setEndpointUtil(EndpointUtil util) {
    this.util = util;
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
