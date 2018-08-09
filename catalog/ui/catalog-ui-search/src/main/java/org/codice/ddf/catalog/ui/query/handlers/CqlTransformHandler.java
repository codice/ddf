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

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.core.HttpHeaders;
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

  private static final String GZIP = "gzip";

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
    CqlQueryResponse cqlQueryResponse = util.executeCqlQuery(cqlRequest);

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

    attachFileToResponse(request, response, queryResponseTransformer, cqlQueryResponse);

    return "";
  }

  private void setHttpHeaders(Request request, Response response, BinaryContent content)
      throws MimeTypeException {
    String mimeType = content.getMimeTypeValue();
    String fileExt = getFileExtFromMimeType(mimeType);

    if (containsGzip(request)) {
      LOGGER.trace("Request header accepts gzip");
      response.header(HttpHeaders.CONTENT_ENCODING, GZIP);
    }

    response.type(mimeType);
    String attachment =
        String.format("attachment;filename=export-%s%s", Instant.now().toString(), fileExt);
    response.header(HttpHeaders.CONTENT_DISPOSITION, attachment);
  }

  private String getFileExtFromMimeType(String mimeType) throws MimeTypeException {
    MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
    return allTypes.forName(mimeType).getExtension();
  }

  private Boolean containsGzip(Request request) {
    return request.headers(HttpHeaders.ACCEPT_ENCODING).toLowerCase().contains(GZIP);
  }

  private void attachFileToResponse(
      Request request,
      Response response,
      ServiceReference<QueryResponseTransformer> queryResponseTransformer,
      CqlQueryResponse cqlQueryResponse)
      throws CatalogTransformerException, IOException, MimeTypeException {
    BinaryContent content =
        bundleContext
            .getService(queryResponseTransformer)
            .transform(cqlQueryResponse.getQueryResponse(), Collections.emptyMap());

    setHttpHeaders(request, response, content);

    try (OutputStream servletOutputStream = response.raw().getOutputStream();
        InputStream resultStream = content.getInputStream()) {
      if (containsGzip(request)) {
        try (OutputStream gzipServletOutputStream = new GZIPOutputStream(servletOutputStream)) {
          IOUtils.copy(resultStream, gzipServletOutputStream);
        }
      } else {
        IOUtils.copy(resultStream, servletOutputStream);
      }
    }

    response.status(HttpStatus.OK_200);

    LOGGER.trace(
        "Successfully output file using transformer id {}",
        queryResponseTransformer.getProperty("id"));
  }

  public void setEndpointUtil(EndpointUtil util) {
    this.util = util;
  }
}
