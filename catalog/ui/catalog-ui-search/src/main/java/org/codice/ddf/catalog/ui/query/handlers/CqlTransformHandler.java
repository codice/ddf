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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.codice.ddf.catalog.ui.metacard.transform.CsvTransform;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.gsonsupport.GsonTypeAdapters.DateLongFormatTypeAdapter;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
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

  public static final String TRANSFORMER_ID_PROPERTY = "id";

  private static final Logger LOGGER = LoggerFactory.getLogger(CqlTransformHandler.class);
  private static final String GZIP = "gzip";

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .registerTypeAdapter(Date.class, new DateLongFormatTypeAdapter())
          .create();

  private EndpointUtil util;
  private List<ServiceReference> queryResponseTransformers;
  private BundleContext bundleContext;

  public CqlTransformHandler(
      List<ServiceReference> queryResponseTransformers,
      BundleContext bundleContext,
      EndpointUtil endpointUtil) {
    this.queryResponseTransformers = queryResponseTransformers;
    this.bundleContext = bundleContext;
    this.util = endpointUtil;
  }

  public class Arguments {
    private Map<String, Object> args;

    public Arguments() {
      this.args = Collections.emptyMap();
    }

    public void setArguments(Map<String, Object> arguments) {
      this.args = arguments;
    }

    public Map<String, Object> getArguments() {
      return this.args;
    }

    public Map<String, Serializable> getSerializableArguments() {
      if (this.args != null) {
        return this.args
            .entrySet()
            .stream()
            .map(
                entry ->
                    entry.getValue() instanceof CharSequence
                        ? new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().toString())
                        : entry)
            .filter(entry -> entry.getValue() instanceof Serializable)
            .collect(Collectors.toMap(Map.Entry::getKey, e -> (Serializable) e.getValue()));
      }
      return Collections.emptyMap();
    }
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    String transformerId = request.params(":transformerId");
    String body = util.safeGetBody(request);
    CqlRequest cqlRequest;

    try {
      cqlRequest = GSON.fromJson(body, CqlRequest.class);
    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Error fetching cql request");
      response.status(HttpStatus.BAD_REQUEST_400);
      return ImmutableMap.of("message", "Error retrieving cql request");
    }

    if (cqlRequest.getCql() == null) {
      LOGGER.debug("Cql not found in request");
      return ImmutableMap.of("message", "Cql not found in request");
    }

    Map<String, Serializable> arguments =
        GSON.fromJson(body, Arguments.class).getSerializableArguments();

    LOGGER.trace("Finding transformer to transform query response.");

    ServiceReference<QueryResponseTransformer> queryResponseTransformer =
        queryResponseTransformers
            .stream()
            .filter(
                transformer ->
                    transformerId.equals(transformer.getProperty(TRANSFORMER_ID_PROPERTY)))
            .findFirst()
            .orElse(null);

    if (queryResponseTransformer == null) {
      LOGGER.debug("Could not find transformer with id: {}", transformerId);
      response.status(HttpStatus.NOT_FOUND_404);
      return ImmutableMap.of("message", "Service not found");
    }

    CqlQueryResponse cqlQueryResponse = util.executeCqlQuery(cqlRequest);

    Object schema = queryResponseTransformer.getProperty("schema");

    List<String> mimeTypeServiceProperty =
        queryResponseTransformer.getProperty("mime-type") instanceof List
            ? (List) queryResponseTransformer.getProperty("mime-type")
            : Collections.emptyList();

    if (mimeTypeServiceProperty.contains("text/csv")) {
      arguments = csvTransformArgumentsAdapter(arguments);
    } else if (schema != null && schema.toString().equals(CswConstants.CSW_NAMESPACE_URI)) {
      arguments = cswTransformArgumentsAdapter();
    }

    attachFileToResponse(request, response, queryResponseTransformer, cqlQueryResponse, arguments);

    return "";
  }

  public List<ServiceReference> getQueryResponseTransformers() {
    return queryResponseTransformers;
  }

  private void setHttpHeaders(Request request, Response response, BinaryContent content)
      throws MimeTypeException {
    String mimeType = content.getMimeTypeValue();

    if (mimeType == null) {
      LOGGER.debug("Failure to fetch file extension, mime-type is empty");
      throw new IllegalArgumentException("Binary Content contains null mime-type value.");
    }

    String fileExt = getFileExtFromMimeType(mimeType);

    if (containsGzip(request)) {
      LOGGER.trace("Request header accepts gzip");
      response.header(HttpHeaders.CONTENT_ENCODING, GZIP);
    }

    response.type(mimeType);
    String attachment =
        String.format("attachment;filename=\"export-%s%s\"", Instant.now().toString(), fileExt);
    response.header(HttpHeaders.CONTENT_DISPOSITION, attachment);
  }

  private String getFileExtFromMimeType(String mimeType) throws MimeTypeException {
    MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
    String fileExt = allTypes.forName(mimeType).getExtension();
    if (StringUtils.isEmpty(fileExt)) {
      LOGGER.debug("Null or empty file extension from mime-type {}", mimeType);
    }
    return fileExt;
  }

  private Boolean containsGzip(Request request) {
    String acceptEncoding = request.headers(HttpHeaders.ACCEPT_ENCODING);
    if (acceptEncoding != null) {
      return acceptEncoding.toLowerCase().contains(GZIP);
    }
    LOGGER.debug("Request header Accept-Encoding is null");
    return false;
  }

  private void attachFileToResponse(
      Request request,
      Response response,
      ServiceReference<QueryResponseTransformer> queryResponseTransformer,
      CqlQueryResponse cqlQueryResponse,
      Map<String, Serializable> arguments)
      throws CatalogTransformerException, IOException, MimeTypeException {
    BinaryContent content =
        bundleContext
            .getService(queryResponseTransformer)
            .transform(cqlQueryResponse.getQueryResponse(), arguments);

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

  private Map<String, Serializable> cswTransformArgumentsAdapter() {
    Map<String, Serializable> args = new HashMap<>();
    args.put(CswConstants.IS_BY_ID_QUERY, true);
    return args;
  }

  private Map<String, Serializable> csvTransformArgumentsAdapter(
      Map<String, Serializable> arguments) {
    String columnOrder = "\"columnOrder\":" + GSON.toJson(arguments.get("columnOrder"));
    String hiddenFields = "\"hiddenFields\":" + GSON.toJson(arguments.get("hiddenFields"));
    String columnAliasMap = "\"columnAliasMap\":" + GSON.toJson(arguments.get("columnAliasMap"));
    String csvBody = String.format("{%s,%s,%s}", columnOrder, columnAliasMap, hiddenFields);

    CsvTransform queryTransform = GSON.fromJson(csvBody, CsvTransform.class);

    Set<String> hiddenFieldsSet =
        queryTransform.getHiddenFields() != null
            ? queryTransform.getHiddenFields()
            : Collections.emptySet();

    List<String> columnOrderList =
        queryTransform.getColumnOrder() != null
            ? queryTransform.getColumnOrder()
            : Collections.emptyList();

    Map<String, String> aliasMap =
        queryTransform.getColumnAliasMap() != null
            ? queryTransform.getColumnAliasMap()
            : Collections.emptyMap();

    return ImmutableMap.<String, Serializable>builder()
        .put("hiddenFields", (Serializable) hiddenFieldsSet)
        .put("columnOrder", (Serializable) columnOrderList)
        .put("aliases", (Serializable) aliasMap)
        .build();
  }
}
