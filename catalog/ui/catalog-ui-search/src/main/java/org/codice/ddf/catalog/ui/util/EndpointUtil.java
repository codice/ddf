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
package org.codice.ddf.catalog.ui.util;

import static ddf.catalog.Constants.ADDITIONAL_SORT_BYS;
import static ddf.catalog.util.impl.ResultIterable.resultIterable;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.action.ActionRegistry;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.impl.SortByImpl;
import ddf.catalog.impl.filter.GeoToolsFunctionFactory;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.QueryFunction;
import ddf.catalog.util.impl.ResultIterable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.ui.config.ConfigurationApplication;
import org.codice.ddf.catalog.ui.metacard.EntityTooLargeException;
import org.codice.ddf.catalog.ui.query.cql.CqlQueryResponse;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.transformer.TransformerDescriptors;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.FactoryIteratorProvider;
import org.geotools.factory.GeoTools;
import org.geotools.filter.FunctionFactory;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

public class EndpointUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(EndpointUtil.class);
  public static final String MESSAGE = "message";

  private final List<MetacardType> metacardTypes;

  private final CatalogFramework catalogFramework;

  private final FilterBuilder filterBuilder;

  private final FilterAdapter filterAdapter;

  private final ActionRegistry actionRegistry;

  private final List<InjectableAttribute> injectableAttributes;

  private final AttributeRegistry attributeRegistry;

  private final ConfigurationApplication config;

  private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  private static final String APPLICATION_JSON = "application/json";

  private static final String TYPE_KEY = "type";

  private static final String MULTIVALUED_KEY = "multivalued";

  private static final String ID_KEY = "id";

  private static final String ISINJECTED_KEY = "isInjected";

  private static int pageSize = 250;

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .setDateFormat(ISO_8601_DATE_FORMAT)
          .create();

  private final Random random = new Random();

  private List<String> whiteListedMetacardTypes = Collections.emptyList();

  private TransformerDescriptors descriptors;

  public EndpointUtil(
      List<MetacardType> metacardTypes,
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      FilterAdapter filterAdapter,
      ActionRegistry actionRegistry,
      List<InjectableAttribute> injectableAttributes,
      AttributeRegistry attributeRegistry,
      ConfigurationApplication config) {
    this.metacardTypes = metacardTypes;
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.filterAdapter = filterAdapter;
    this.actionRegistry = actionRegistry;
    this.injectableAttributes = injectableAttributes;
    this.attributeRegistry = attributeRegistry;
    this.config = config;
    registerGeoToolsFunctionFactory();
  }

  public List<String> getWhiteListedMetacardTypes() {
    return whiteListedMetacardTypes;
  }

  public void setWhiteListedMetacardTypes(List<String> whiteListedMetacardTypes) {
    this.whiteListedMetacardTypes = whiteListedMetacardTypes;
  }

  @SuppressWarnings("squid:S1604") // generics cannot be lambdas
  private void registerGeoToolsFunctionFactory() {
    GeoTools.addFactoryIteratorProvider(
        new FactoryIteratorProvider() {
          @Override
          public <T> Iterator<T> iterator(Class<T> category) {
            if (FunctionFactory.class == category) {
              List<FunctionFactory> functionFactories = new LinkedList<>();
              functionFactories.add(new GeoToolsFunctionFactory());
              return (Iterator<T>) functionFactories.iterator();
            }
            return null;
          }
        });
    CommonFactoryFinder.reset();
  }

  public Metacard getMetacardById(String id)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    Filter idFilter = filterBuilder.attribute(Core.ID).is().equalTo().text(id);
    Filter tagsFilter = filterBuilder.attribute(Core.METACARD_TAGS).is().like().text("*");
    Filter filter = filterBuilder.allOf(idFilter, tagsFilter);

    QueryResponse queryResponse =
        catalogFramework.query(new QueryRequestImpl(new QueryImpl(filter), false));

    if (queryResponse.getResults().isEmpty()) {
      throw new NotFoundException("Could not find metacard for id: " + id);
    }

    Result result = queryResponse.getResults().get(0);

    return result.getMetacard();
  }

  public String getResponseWrapper(String responseType, Object response) {
    Map<String, Object> result = new HashMap<>();
    result.put("responseType", responseType);
    result.put("response", response);
    return getJson(result);
  }

  @Override
  public Map<String, Result> getMetacardsByTag(String tagStr) {
    Filter filter = filterBuilder.attribute(Core.METACARD_TAGS).is().like().text(tagStr);

    ResultIterable resultIterable =
        resultIterable(
            catalogFramework,
            new QueryRequestImpl(
                new QueryImpl(
                    filter,
                    1,
                    pageSize,
                    new SortByImpl(Core.MODIFIED, SortOrder.DESCENDING),
                    false,
                    TimeUnit.SECONDS.toMillis(10)),
                false,
                null,
                additionalSort(new HashMap<>(), Core.ID, SortOrder.ASCENDING)));
    return resultIterable
        .stream()
        .collect(
            Collectors.toMap(
                result -> result.getMetacard().getId(),
                Function.identity(),
                EndpointUtil::firstInWinsMerge));
  }

  public List<Metacard> getMetacardListByTag(String tagStr) {
    Filter filter = filterBuilder.attribute(Core.METACARD_TAGS).is().like().text(tagStr);

    return resultIterable(
            catalogFramework,
            new QueryRequestImpl(
                new QueryImpl(
                    filter,
                    1,
                    pageSize,
                    new SortByImpl(Core.MODIFIED, SortOrder.DESCENDING),
                    false,
                    TimeUnit.SECONDS.toMillis(10)),
                false,
                null,
                null))
        .stream()
        .filter(Objects::nonNull)
        .map(Result::getMetacard)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  public Map<String, Result> getMetacardsWithTagByLikeAttributes(
      Map<String, Collection<String>> attributeMap, String tagStr) {
    return getMetacardsWithTagByLikeAttributes(
        attributeMap, filterBuilder.attribute(Core.METACARD_TAGS).is().like().text(tagStr));
  }

  public Map<String, Result> getMetacardsWithTagByLikeAttributes(
      Map<String, Collection<String>> attributeMap, Filter tagFilter) {

    List<Filter> attributeFilters = new ArrayList<>();

    for (String attributeName : attributeMap.keySet()) {
      attributeFilters.add(
          buildAttributeFilter(attributeName, attributeMap.get(attributeName), false));
    }

    Filter filter = filterBuilder.allOf(filterBuilder.anyOf(attributeFilters), tagFilter);

    ResultIterable resultIterable =
        resultIterable(
            catalogFramework,
            new QueryRequestImpl(
                new QueryImpl(
                    filter,
                    1,
                    pageSize,
                    new SortByImpl(Core.MODIFIED, SortOrder.DESCENDING),
                    false,
                    TimeUnit.SECONDS.toMillis(10)),
                false,
                null,
                additionalSort(new HashMap<>(), Core.ID, SortOrder.ASCENDING)));
    return resultIterable
        .stream()
        .collect(
            Collectors.toMap(
                result -> result.getMetacard().getId(),
                Function.identity(),
                EndpointUtil::firstInWinsMerge));
  }

  public Map<String, Result> getMetacardsWithTagById(Collection<String> ids, String tagFilter) {
    return getMetacardsWithTagByAttributes(Core.ID, ids, tagFilter);
  }

  public Map<String, Result> getMetacardsWithTagById(Collection<String> ids, Filter tagFilter) {
    return getMetacardsWithTagByAttributes(Core.ID, ids, tagFilter);
  }

  public Map<String, Result> getMetacardsWithTagByAttributes(
      String attributeName, Collection<String> attributeValues, String tag) {
    return getMetacardsWithTagByAttributes(
        attributeName,
        attributeValues,
        filterBuilder.attribute(Core.METACARD_TAGS).is().like().text(tag));
  }

  public Map<String, Result> getMetacardsWithTagByAttributes(
      String attributeName, Collection<String> attributeValues, Filter tagFilter) {
    if (attributeValues.isEmpty()) {
      return new HashMap<>();
    }

    Filter attributeFilter = buildAttributeFilter(attributeName, attributeValues, true);
    Filter queryFilter = filterBuilder.allOf(attributeFilter, tagFilter);

    ResultIterable resultIterable =
        resultIterable(
            catalogFramework,
            new QueryRequestImpl(
                new QueryImpl(
                    queryFilter,
                    1,
                    pageSize,
                    new SortByImpl(Core.MODIFIED, SortOrder.DESCENDING),
                    false,
                    TimeUnit.SECONDS.toMillis(10)),
                false,
                null,
                additionalSort(new HashMap<>(), Core.ID, SortOrder.ASCENDING)));

    return resultIterable
        .stream()
        .collect(
            Collectors.toMap(
                result -> result.getMetacard().getId(),
                Function.identity(),
                EndpointUtil::firstInWinsMerge));
  }

  private Filter buildAttributeFilter(
      String attributeName, Collection<String> attributeValues, boolean isExactMatch) {
    if (CollectionUtils.isEmpty(attributeValues)) {
      return null;
    }

    List<Filter> filters = new ArrayList<>();
    for (String value : attributeValues) {
      if (isExactMatch) {
        filters.add(filterBuilder.attribute(attributeName).is().equalTo().text(value));
      } else {
        filters.add(filterBuilder.attribute(attributeName).is().like().text(value));
      }
    }

    if (filters.size() == 1) {
      return filters.get(0);
    } else {
      return filterBuilder.anyOf(filters);
    }
  }

  private Map<String, Serializable> additionalSort(
      Map<String, Serializable> properties, String propertyName, SortOrder order) {
    SortBy[] additionalSorts = {new SortByImpl(propertyName, order)};
    properties.put(ADDITIONAL_SORT_BYS, additionalSorts);
    return properties;
  }

  private List<MetacardType> getFilteredMetacardTypes() {
    if (!whiteListedMetacardTypes.isEmpty()) {
      return metacardTypes
          .stream()
          .filter(metacardType -> whiteListedMetacardTypes.contains(metacardType.getName()))
          .collect(Collectors.toList());
    }
    return metacardTypes;
  }

  public Map<String, Object> getMetacardTypeMap() {
    Map<String, Object> resultTypes = new HashMap<>();
    for (MetacardType metacardType : getFilteredMetacardTypes()) {
      Map<String, Object> attributes = new HashMap<>();
      for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
        Map<String, Object> attributeProperties = new HashMap<>();
        attributeProperties.put(TYPE_KEY, descriptor.getType().getAttributeFormat().name());
        attributeProperties.put(MULTIVALUED_KEY, descriptor.isMultiValued());
        attributeProperties.put(ID_KEY, descriptor.getName());
        attributeProperties.put(ISINJECTED_KEY, false);
        attributes.put(descriptor.getName(), attributeProperties);
      }
      resultTypes.put(metacardType.getName(), attributes);
    }
    for (InjectableAttribute attribute : injectableAttributes) {
      Optional<AttributeDescriptor> lookup = attributeRegistry.lookup(attribute.attribute());
      if (!lookup.isPresent()) {
        continue;
      }

      AttributeDescriptor descriptor = lookup.get();
      Map<String, Object> attributeProperties = new HashMap<>();
      attributeProperties.put(TYPE_KEY, descriptor.getType().getAttributeFormat().name());
      attributeProperties.put(MULTIVALUED_KEY, descriptor.isMultiValued());
      attributeProperties.put(ID_KEY, descriptor.getName());
      attributeProperties.put(ISINJECTED_KEY, true);
      Set<String> types =
          attribute.metacardTypes().isEmpty() ? resultTypes.keySet() : attribute.metacardTypes();

      types
          .stream()
          .filter(type -> isAttributeMissing(resultTypes, attribute, type))
          .forEach(
              type ->
                  mergeMetacardTypeIntoResults(resultTypes, attribute, attributeProperties, type));
    }

    return resultTypes;
  }

  @SuppressWarnings("unchecked")
  private void mergeMetacardTypeIntoResults(
      Map<String, Object> resultTypes,
      InjectableAttribute attribute,
      Map<String, Object> attributeProperties,
      String type) {
    Map<String, Object> attributes =
        (Map) resultTypes.getOrDefault(type, new HashMap<String, Object>());
    attributes.put(attribute.attribute(), attributeProperties);
    resultTypes.put(type, attributes);
  }

  @SuppressWarnings("unchecked")
  private boolean isAttributeMissing(
      Map<String, Object> resultTypes, InjectableAttribute attribute, String type) {
    if (!resultTypes.containsKey(type)) {
      return true;
    }

    Map<String, Object> attributes = (Map<String, Object>) resultTypes.get(type);

    return !attributes.containsKey(attribute.attribute());
  }

  @SuppressWarnings("squid:S1319") // needs to match signature of AttributeImpl
  public ArrayList<String> getStringList(List<Serializable> list) {
    if (list == null) {
      return new ArrayList<>();
    }
    return list.stream().map(String::valueOf).collect(Collectors.toCollection(ArrayList::new));
  }

  public Map<String, Object> transformToMap(Metacard metacard) {
    return transformToMap(Collections.singletonList(metacard));
  }

  public Map<String, Object> transformToMap(List<Metacard> metacards) {
    List<Map<String, Object>> metacardJsons =
        metacards.stream().map(this::getMetacardMap).collect(Collectors.toList());

    Set<String> types =
        metacards
            .stream()
            .map(Metacard::getMetacardType)
            .map(MetacardType::getName)
            .collect(Collectors.toSet());

    List<Map<String, Object>> typesList = new ArrayList<>();
    for (String type : types) {
      Map<String, Object> typeMap = new HashMap<>();
      typeMap.put("type-name", type);
      typeMap.put("type", getMetacardTypeMap().get(type));

      typeMap.put(
          "ids",
          metacards
              .stream()
              .filter(mc -> type.equals(mc.getMetacardType().getName()))
              .map(Metacard::getId)
              .collect(Collectors.toList()));
      typesList.add(typeMap);
    }

    Map<String, Object> outerMap = new HashMap<>();
    outerMap.put("metacards", metacardJsons);
    outerMap.put("metacard-types", typesList);

    return outerMap;
  }

  public String metacardToJson(String id)
      throws SourceUnavailableException, UnsupportedQueryException, FederationException {
    return metacardToJson(getMetacardById(id));
  }

  public String metacardToJson(Metacard metacard) {
    return getJson(transformToMap(metacard));
  }

  public String metacardsToJson(List<Metacard> metacards) {
    return getJson(transformToMap(metacards));
  }

  public String getJson(Object result) {
    return GSON.toJson(result);
  }

  public CqlQueryResponse executeCqlQuery(CqlRequest cqlRequest)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    QueryRequest request = cqlRequest.createQueryRequest(catalogFramework.getId(), filterBuilder);
    Stopwatch stopwatch = Stopwatch.createStarted();

    List<QueryResponse> responses = Collections.synchronizedList(new ArrayList<>());

    List<Result> results;
    if (cqlRequest.getCount() == 0) {
      results = retrieveHitCount(request, responses);
    } else {
      results = retrieveResults(cqlRequest, request, responses);
    }

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
                .orElse(-1L),
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
        cqlRequest.getSourceResponseString(),
        stopwatch.elapsed(TimeUnit.MILLISECONDS),
        cqlRequest.isNormalize(),
        filterAdapter,
        actionRegistry,
        descriptors);
  }

  private List<Result> retrieveHitCount(QueryRequest request, List<QueryResponse> responses)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    QueryResponse queryResponse = catalogFramework.query(request);
    responses.add(queryResponse);
    return queryResponse.getResults();
  }

  private List<Result> retrieveResults(
      CqlRequest cqlRequest, QueryRequest request, List<QueryResponse> responses) {
    QueryFunction queryFunction =
        (queryRequest) -> {
          QueryResponse queryResponse = catalogFramework.query(queryRequest);
          responses.add(queryResponse);
          return queryResponse;
        };
    return ResultIterable.resultIterable(queryFunction, request, cqlRequest.getCount())
        .stream()
        .collect(Collectors.toList());
  }

  public Map<String, Object> getMetacardMap(Metacard metacard) {
    Set<AttributeDescriptor> attributeDescriptors =
        metacard.getMetacardType().getAttributeDescriptors();
    Map<String, Object> result = new HashMap<>();
    for (AttributeDescriptor descriptor : attributeDescriptors) {
      if (handleNullName(metacard, result, descriptor)
          || handleThumbnail(metacard, result, descriptor)) {
        continue;
      }
      handleDate(metacard, result, descriptor);
      handleMultivalued(metacard, result, descriptor);
    }
    return result;
  }

  private void handleMultivalued(
      Metacard metacard, Map<String, Object> result, AttributeDescriptor descriptor) {
    if (descriptor.isMultiValued()) {
      result.put(descriptor.getName(), metacard.getAttribute(descriptor.getName()).getValues());
    } else {
      result.put(descriptor.getName(), metacard.getAttribute(descriptor.getName()).getValue());
    }
  }

  private void handleDate(
      Metacard metacard, Map<String, Object> result, AttributeDescriptor descriptor) {
    if (descriptor.getType().getAttributeFormat().equals(AttributeType.AttributeFormat.DATE)) {
      Attribute attribute = metacard.getAttribute(descriptor.getName());
      if (descriptor.isMultiValued()) {
        result.put(
            descriptor.getName(),
            attribute.getValues().stream().map(this::parseDate).collect(Collectors.toList()));
      } else {
        result.put(descriptor.getName(), parseDate(attribute.getValue()));
      }
    }
  }

  private boolean handleThumbnail(
      Metacard metacard, Map<String, Object> result, AttributeDescriptor descriptor) {
    if (Core.THUMBNAIL.equals(descriptor.getName())) {
      if (metacard.getThumbnail() != null) {
        result.put(
            descriptor.getName(), Base64.getEncoder().encodeToString(metacard.getThumbnail()));
      } else {
        result.put(descriptor.getName(), null);
      }
      return true;
    }
    return false;
  }

  private boolean handleNullName(
      Metacard metacard, Map<String, Object> result, AttributeDescriptor descriptor) {
    if (metacard.getAttribute(descriptor.getName()) == null) {
      if (descriptor.isMultiValued()) {
        result.put(descriptor.getName(), Collections.emptyList());
      } else {
        result.put(descriptor.getName(), null);
      }
      return true;
    }
    return false;
  }

  public Instant parseToDate(Serializable value) {
    if (value instanceof Instant) {
      return ((Instant) value);
    }
    if (value instanceof Date) {
      return ((Date) value).toInstant();
    }
    SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    try {
      return dateFormat.parse(value.toString()).toInstant();
    } catch (ParseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  @SuppressWarnings("squid:S2293") // results in class cast error
  public Map.Entry<String, Object> convertDateEntries(Map.Entry<String, Object> entry) {
    if (entry == null || entry.getKey() == null || entry.getValue() == null) {
      return entry;
    }

    return attributeRegistry
        .lookup(entry.getKey())
        .filter(ad -> AttributeType.AttributeFormat.DATE.equals(ad.getType().getAttributeFormat()))
        .map(
            attributeDescriptor -> {
              Serializable date = parseDate((Serializable) entry.getValue());
              if (date instanceof Instant) {
                // must be Date object for solr to parse correctly
                date = Date.from((Instant) date);
              }
              return (Map.Entry<String, Object>)
                  new AbstractMap.SimpleEntry<String, Object>(entry.getKey(), date);
            })
        .orElse(entry);
  }

  private Pattern jsonDefault =
      Pattern.compile("[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s\\d+\\s[0-9:]+\\s(\\w+\\s)?\\d+");

  private Pattern iso8601Z = Pattern.compile("\\d+-?\\d+-?\\d+T\\d+:?\\d+:?\\d+(\\.\\d+)?Z");

  private Pattern iso8601Offset =
      Pattern.compile("\\d+-?\\d+-?\\d+T\\d+:?\\d+:?\\d+(\\.\\d+)?[+\\-]\\d+:\\d+");

  public Instant parseDate(Serializable value) {

    if (value instanceof Date) {
      return ((Date) value).toInstant();
    }

    if (value instanceof Long) {
      return Instant.ofEpochMilli((Long) value);
    }

    if (!(value instanceof String)) {
      return null;
    }

    String string = String.valueOf(value);

    if (StringUtils.isBlank(string)) {
      return null;
    }

    if (StringUtils.isNumeric(string)) {
      try {
        return Instant.ofEpochMilli(Long.parseLong(string));
      } catch (NumberFormatException ex) {
        LOGGER.debug("Failed to create Epoch time from a numeric: {}", string, ex);
        return null;
      }
    }

    if (iso8601Z.matcher(string).matches()) {
      return Instant.parse(string);
    }

    if (iso8601Offset.matcher(string).matches()) {
      return OffsetDateTime.parse(string).toInstant();
    }

    SimpleDateFormat dateFormat;
    if (jsonDefault.matcher(string).matches()) {
      dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss zzz yyyy");
    } else {
      dateFormat = new SimpleDateFormat();
    }
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    try {
      return dateFormat.parse(string).toInstant();
    } catch (ParseException e) {
      LOGGER.debug("Failed to parse as a dateFormat time from a date string: {}", string, e);
      return null;
    }
  }

  public String safeGetBody(Request req) throws IOException {
    if (req.contentLength() > config.getMaximumUploadSize()) {
      throw new EntityTooLargeException(req.ip(), req.userAgent(), req.url(), random.nextInt());
    }
    byte[] bytes =
        IOUtils.toByteArray(
            new BoundedInputStream(req.raw().getInputStream(), config.getMaximumUploadSize() + 1L));
    if (bytes.length > config.getMaximumUploadSize()) {
      throw new EntityTooLargeException(req.ip(), req.userAgent(), req.url(), random.nextInt());
    }

    return new String(bytes, Charset.defaultCharset());
  }

  @SuppressWarnings("squid:S1172") // needed for compilation
  public void handleRuntimeException(Exception ex, Request req, Response res) {
    LOGGER.debug("Exception occurred", ex);
    res.status(404);
    res.header(CONTENT_TYPE, APPLICATION_JSON);
    res.body(getJson(ImmutableMap.of(MESSAGE, "Could not find what you were looking for")));
  }

  @SuppressWarnings("squid:S1172") // needed for compilation
  public void handleEntityTooLargeException(Exception ex, Request req, Response res) {
    LOGGER.info(
        "User uploaded object greater than maximum size ({} bytes). If this is a valid request then you may consider increasing the maximum allowed request size under: <system>/admin/index.html > Search UI > Catalog UI Search > Maximum Endpoint Upload Size. Please consider the constraints of system memory before adjusting this value.  It is roughly 3 * (max number concurrent system users) * (maximum endpoint upload bytes size) just for this endpoint, not considering the rest of the system. ",
        config.getMaximumUploadSize(),
        ex);
    String errorId = null;
    if (ex instanceof EntityTooLargeException) {
      errorId = ((EntityTooLargeException) ex).getStringId();
    }
    res.status(413);
    res.header(CONTENT_TYPE, APPLICATION_JSON);
    res.body(
        getJson(
            ImmutableMap.of(
                MESSAGE,
                "The data sent was too large. Please contact your Systems Administrator. Error Code: "
                    + errorId)));
  }

  @SuppressWarnings("squid:S1172") // needed for compilation
  public void handleIOException(Exception ex, Request req, Response res) {
    LOGGER.debug("Exception occurred", ex);
    res.status(500);
    res.header(CONTENT_TYPE, APPLICATION_JSON);
    res.body(
        getJson(
            ImmutableMap.of(
                MESSAGE,
                "Something went wrong, please retry. If the problem persists please contact your Systems Administrator.")));
  }

  public FilterBuilder getFilterBuilder() {
    return filterBuilder;
  }

  private static Result firstInWinsMerge(Result current, Result incoming) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "While collecting metacards into map, there were metacards found with a duplicate key.\nOld: {}\nNew: {}",
          current,
          incoming);
    }
    return current;
  }

  /**
   * Copy the attributes from a metacard to another metacard.
   *
   * @param sourceMetacard the source metacard
   * @param metacardType copy all attributes represented by this metacard type
   * @param destinationMetacard the destination metacard
   */
  public void copyAttributes(
      Metacard sourceMetacard, MetacardType metacardType, Metacard destinationMetacard) {
    metacardType
        .getAttributeDescriptors()
        .stream()
        .filter(descriptor -> sourceMetacard.getAttribute(descriptor.getName()) != null)
        .map(descriptor -> copyAttribute(descriptor, sourceMetacard))
        .forEach(destinationMetacard::setAttribute);
  }

  private Attribute copyAttribute(AttributeDescriptor attributeDescriptor, Metacard metacard) {
    String name = attributeDescriptor.getName();
    if (attributeDescriptor.isMultiValued()) {
      List<Serializable> values = new ArrayList<>(metacard.getAttribute(name).getValues());
      return new AttributeImpl(name, values);
    }
    return metacard.getAttribute(name);
  }

  /**
   * Find the workspace metacard based on the workspace identifier. If the workspsace cannot be
   * found, then return <code>null</code>.
   *
   * @param workspaceId the workspace identifier
   * @return workspace metacard
   */
  public Metacard findWorkspace(String workspaceId) {
    try {
      return getMetacardById(workspaceId);
    } catch (NotFoundException
        | UnsupportedQueryException
        | SourceUnavailableException
        | FederationException e) {
      return null;
    }
  }

  @SuppressWarnings("WeakerAccess" /* setter must be public for blueprint access */)
  public void setDescriptors(TransformerDescriptors descriptors) {
    this.descriptors = descriptors;
  }
}
