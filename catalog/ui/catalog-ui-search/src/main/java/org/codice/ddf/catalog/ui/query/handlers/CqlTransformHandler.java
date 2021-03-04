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

import static org.codice.ddf.catalog.ui.transformer.TransformerDescriptors.REQUIRED_ATTR;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.util.impl.CollectionResultComparator;
import ddf.catalog.util.impl.DistanceResultComparator;
import ddf.catalog.util.impl.RelevanceResultComparator;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.codice.ddf.catalog.ui.metacard.transform.CsvTransformImpl;
import org.codice.ddf.catalog.ui.query.cql.CqlRequestImpl;
import org.codice.ddf.catalog.ui.query.utility.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.utility.CqlRequest;
import org.codice.ddf.catalog.ui.util.CqlQueriesImpl;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.gsonsupport.GsonTypeAdapters.DateLongFormatTypeAdapter;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.eclipse.jetty.http.HttpStatus;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.sort.SortOrder;
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
  public static final String COLUMN_ALIAS_MAP = "columnAliasMap";
  public static final String FAILED_TRANSFORM_IDS = "failedTransformIds";
  public static final String COLUMN_ALIASES = "aliases";

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
  private CqlQueriesImpl cqlQueryUtil;

  public CqlTransformHandler(
      List<ServiceReference> queryResponseTransformers,
      BundleContext bundleContext,
      EndpointUtil endpointUtil,
      CqlQueriesImpl cqlQueryUtil) {
    this.queryResponseTransformers = queryResponseTransformers;
    this.bundleContext = bundleContext;
    this.util = endpointUtil;
    this.cqlQueryUtil = cqlQueryUtil;
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

  public class CqlTransformRequest {
    private List<CqlRequestImpl> searches = Collections.emptyList();
    private int count = 0;
    private List<CqlRequest.Sort> sorts = Collections.emptyList();
    private List<String> hiddenResults = Collections.emptyList();

    public void setSearches(List<CqlRequestImpl> searches) {
      this.searches = searches;
    }

    public List<CqlRequestImpl> getSearches() {
      return this.searches;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public int getCount() {
      return this.count;
    }

    public void setSorts(List<CqlRequest.Sort> sorts) {
      this.sorts = sorts;
    }

    public List<CqlRequest.Sort> getSorts() {
      return this.sorts;
    }

    public void setHiddenResults(List<String> hiddenResults) {
      this.hiddenResults = hiddenResults;
    }

    public List<String> getHiddenResults() {
      return this.hiddenResults;
    }
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    String transformerId = request.params(":transformerId");
    String body = util.safeGetBody(request);
    CqlTransformRequest cqlTransformRequest;

    try {
      cqlTransformRequest = GSON.fromJson(body, CqlTransformRequest.class);
    } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
      LOGGER.debug("Error fetching cql request");
      response.status(HttpStatus.BAD_REQUEST_400);
      return ImmutableMap.of("message", "Error retrieving cql request");
    }

    List<CqlRequest> cqlRequests =
        cqlTransformRequest
            .getSearches()
            .stream()
            .filter(
                cqlRequest ->
                    cqlRequest.getCql() != null
                        && (cqlRequest.getSrc() != null
                            || CollectionUtils.isNotEmpty(cqlRequest.getSrcs())))
            .collect(Collectors.toList());

    cqlRequests.forEach(
        cqlRequest -> {
          cqlRequest.setSorts(cqlTransformRequest.getSorts());
        });

    if (CollectionUtils.isEmpty(cqlRequests)) {
      LOGGER.debug("Cql not found in request");
      response.status(HttpStatus.BAD_REQUEST_400);
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

    List<Result> results =
        cqlRequests
            .stream()
            .map(
                cqlRequest -> {
                  CqlQueryResponse cqlQueryResponse = null;
                  try {
                    cqlQueryResponse = cqlQueryUtil.executeCqlQuery(cqlRequest);
                  } catch (UnsupportedQueryException
                      | SourceUnavailableException
                      | FederationException e) {
                    LOGGER.debug("Error fetching cql request for {}", cqlRequest.getSrc());
                    return null;
                  }
                  return cqlQueryResponse.getQueryResponse().getResults();
                })
            .filter(cqlResults -> CollectionUtils.isNotEmpty(cqlResults))
            .flatMap(cqlResults -> cqlResults.stream())
            .collect(Collectors.toList());

    results.sort(getResultComparators(cqlTransformRequest.getSorts()));

    results =
        results.size() > cqlTransformRequest.getCount()
            ? results.subList(0, cqlTransformRequest.getCount())
            : results;

    results =
        CollectionUtils.isEmpty(cqlTransformRequest.getHiddenResults())
            ? results
            : results
                .stream()
                .filter(
                    result ->
                        !cqlTransformRequest
                            .getHiddenResults()
                            .contains(result.getMetacard().getId()))
                .collect(Collectors.toList());

    List<String> resultsNotToExport = new ArrayList<>();
    results =
        queryResponseTransformer.getProperty(REQUIRED_ATTR) == null
            ? results
            : results
                .stream()
                .filter(
                    result -> {
                      Metacard metacard = result.getMetacard();
                      for (String attr :
                          (List<String>) queryResponseTransformer.getProperty(REQUIRED_ATTR)) {
                        if (metacard.getAttribute(attr) == null) {
                          resultsNotToExport.add(metacard.getId());
                          return false;
                        }
                      }
                      return true;
                    })
                .collect(Collectors.toList());

    QueryResponse combinedResponse =
        new QueryResponseImpl(
            new QueryRequestImpl(new QueryImpl(ECQL.toFilter(cqlRequests.get(0).getCql()))),
            results,
            results.size());

    Object schema = queryResponseTransformer.getProperty("schema");

    List<String> mimeTypeServiceProperty =
        queryResponseTransformer.getProperty("mime-type") instanceof List
            ? (List) queryResponseTransformer.getProperty("mime-type")
            : Collections.emptyList();

    if (mimeTypeServiceProperty.contains("text/csv")
        || "csv".equals(arguments.get("transformerId"))) {
      arguments = csvTransformArgumentsAdapter(arguments);
    } else if (schema != null && schema.toString().equals(CswConstants.CSW_NAMESPACE_URI)) {
      arguments = cswTransformArgumentsAdapter();
    }

    String warning = "";
    if (resultsNotToExport.size() > 0) {
      List<String> requiredAttrList = getRequiredAttributes(queryResponseTransformer, arguments);
      if (results.size() == 0) {
        LOGGER.debug("0 Results to export due to missing required field(s): {}", requiredAttrList);
        response.status(HttpStatus.BAD_REQUEST_400);
        return ImmutableMap.of(
            "message", String.format("Result(s) missing required field(s): %s", requiredAttrList));
      }
      warning =
          getWarningMessage(
              String.format(
                  "Following not exported, missing required field(s): %s", requiredAttrList),
              resultsNotToExport,
              arguments);
    }

    return attachFileToResponse(
        request, response, queryResponseTransformer, combinedResponse, arguments, warning);
  }

  private Map<String, Serializable> getAliasMap(Map<String, Serializable> arguments) {
    if (arguments.get(COLUMN_ALIAS_MAP) != null) {
      return (Map<String, Serializable>) arguments.get(COLUMN_ALIAS_MAP);
    }
    return (Map<String, Serializable>) arguments.get(COLUMN_ALIASES);
  }

  private String getWarningMessage(
      String warningMsg, List<String> resultsNotToExport, Map<String, Serializable> arguments) {
    Map<String, Serializable> columnAliasMap = getAliasMap(arguments);
    String idName =
        columnAliasMap == null || Strings.isBlank((String) columnAliasMap.get(Metacard.ID))
            ? Metacard.ID
            : (String) columnAliasMap.get(Metacard.ID);
    int count = 0;
    int maxID = Math.min(resultsNotToExport.size(), 10);
    for (String resultId : resultsNotToExport) {
      warningMsg += String.format("\\n%s) %s: %s", ++count, idName, resultId);
      if (maxID == count) {
        warningMsg += String.format("\\n.... Total not exported: %s", resultsNotToExport.size());
        break;
      }
    }
    return warningMsg + System.lineSeparator();
  }

  private List<String> getRequiredAttributes(
      ServiceReference<QueryResponseTransformer> queryResponseTransformer,
      Map<String, Serializable> arguments) {
    List<String> requiredAttrList = new ArrayList<>();
    Map<String, Serializable> columnAliasMap = getAliasMap(arguments);
    if (columnAliasMap != null) {
      for (String attribute : (List<String>) queryResponseTransformer.getProperty(REQUIRED_ATTR)) {
        if (StringUtils.isNotBlank((String) columnAliasMap.get(attribute))) {
          requiredAttrList.add((String) columnAliasMap.get(attribute));
        } else {
          requiredAttrList.add(attribute);
        }
      }
    }
    if (requiredAttrList.isEmpty()) {
      requiredAttrList.addAll((List<String>) queryResponseTransformer.getProperty(REQUIRED_ATTR));
    }
    return requiredAttrList;
  }

  public List<ServiceReference> getQueryResponseTransformers() {
    return queryResponseTransformers;
  }

  private void setHttpHeaders(
      Request request, Response response, BinaryContent content, String warning)
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

    if (StringUtils.isNotBlank(warning)) {
      LOGGER.trace("Response has warning: {}", warning);
      response.header("warning", warning);
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

  private Map<String, String> attachFileToResponse(
      Request request,
      Response response,
      ServiceReference<QueryResponseTransformer> queryResponseTransformer,
      QueryResponse cqlQueryResponse,
      Map<String, Serializable> arguments,
      String warning)
      throws CatalogTransformerException, IOException, MimeTypeException {
    BinaryContent content =
        bundleContext.getService(queryResponseTransformer).transform(cqlQueryResponse, arguments);

    if (content.getInputStream() == null) {
      LOGGER.debug("Unable to transform content for mimeType: {}", content.getMimeTypeValue());
      response.status(HttpStatus.BAD_REQUEST_400);
      return ImmutableMap.of(
          "message",
          "Unable to transform content for mimeType: "
              + content.getMimeTypeValue()
              + ", may be missing required field(s)");
    }

    if (CollectionUtils.isNotEmpty(
        (Collection) arguments.get(CqlTransformHandler.FAILED_TRANSFORM_IDS))) {
      warning +=
          getWarningMessage(
              "Unable to transform following(s):",
              (List<String>) arguments.get(FAILED_TRANSFORM_IDS),
              arguments);
    }
    setHttpHeaders(request, response, content, warning);

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

    return Collections.emptyMap();
  }

  private CollectionResultComparator getResultComparators(List<CqlRequest.Sort> sorts) {
    CollectionResultComparator resultComparator = new CollectionResultComparator();
    for (CqlRequest.Sort sort : sorts) {
      Comparator<Result> comparator;

      String sortType = sort.getAttribute();
      SortOrder sortOrder =
          (sort.getDirection() != null && sort.getDirection().equals("descending"))
              ? SortOrder.DESCENDING
              : SortOrder.ASCENDING;

      if (Result.RELEVANCE.equals(sortType)) {
        comparator = new RelevanceResultComparator(sortOrder);
      } else if (Result.DISTANCE.equals(sortType)) {
        comparator = new DistanceResultComparator(sortOrder);
      } else {
        comparator =
            Comparator.comparing(
                r -> getAttributeValue((Result) r, sortType),
                ((sortOrder == SortOrder.ASCENDING)
                    ? Comparator.nullsLast(Comparator.<Comparable>naturalOrder())
                    : Comparator.nullsLast(Comparator.<Comparable>reverseOrder())));
      }
      resultComparator.addComparator(comparator);
    }
    return resultComparator;
  }

  private static Comparable getAttributeValue(Result result, String attributeName) {
    final Attribute attribute = result.getMetacard().getAttribute(attributeName);

    if (attribute == null) {
      return null;
    }

    AttributeType.AttributeFormat format =
        result
            .getMetacard()
            .getMetacardType()
            .getAttributeDescriptor(attributeName)
            .getType()
            .getAttributeFormat();

    switch (format) {
      case STRING:
        return attribute.getValue() != null ? attribute.getValue().toString().toLowerCase() : "";
      case DATE:
      case BOOLEAN:
      case INTEGER:
      case FLOAT:
        return attribute.getValue() instanceof Comparable
            ? (Comparable) attribute.getValue()
            : null;
      default:
        return "";
    }
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
    String columnAliasMap =
        "\"" + COLUMN_ALIAS_MAP + "\":" + GSON.toJson(arguments.get(COLUMN_ALIAS_MAP));
    String csvBody = String.format("{%s,%s,%s}", columnOrder, columnAliasMap, hiddenFields);

    CsvTransformImpl queryTransform = GSON.fromJson(csvBody, CsvTransformImpl.class);

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

    Serializable transformerId =
        arguments.get("transformerId") != null ? arguments.get("transformerId") : "";

    return ImmutableMap.<String, Serializable>builder()
        .put("hiddenFields", (Serializable) hiddenFieldsSet)
        .put("columnOrder", (Serializable) columnOrderList)
        .put(COLUMN_ALIASES, (Serializable) aliasMap)
        .put("transformerId", transformerId)
        .build();
  }
}
