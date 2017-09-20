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
package org.codice.ddf.ui.searchui.query.model;

import com.google.common.collect.ImmutableMap;
import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.ui.searchui.query.model.QueryStatus.State;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/** This class represents the cached asynchronous query response from all sources. */
public class Search {
  private static final Map<Class, String> SAFE_ERROR_MESSAGES =
      ImmutableMap.of(
          UnsupportedQueryException.class,
          "Query was invalid",
          SourceUnavailableException.class,
          "Query hit a source that was unavailable",
          InterruptedException.class,
          "Query was interrupted",
          TimeoutException.class,
          "Query timed out");

  private static final String REASON_INTERNAL = "Internal error";

  public static final String REASONS = "reasons";

  public static final String HITS = "hits";

  public static final String DISTANCE = "distance";

  public static final String RELEVANCE = "relevance";

  public static final String METACARD = "metacard";

  public static final String ACTIONS = "actions";

  public static final String ACTIONS_ID = "id";

  public static final String ACTIONS_TITLE = "title";

  public static final String ACTIONS_DESCRIPTION = "description";

  public static final String ACTIONS_URL = "url";

  public static final String RESULTS = "results";

  public static final String METACARD_TYPES = "metacard-types";

  public static final String SUCCESSFUL = "successful";

  public static final String STATUS = "status";

  public static final String STATE = "state";

  public static final String ID = "id";

  public static final String DONE = "done";

  public static final String ELAPSED = "elapsed";

  public static final String CACHED = "cached";

  private static final String INTERNAL_LOCAL_RESOURCE = "internal.local-resource";

  private static final String IS_RESOURCE_LOCAL = "is-resource-local";

  private static final DateTimeFormatter ISO_8601_DATE_FORMAT =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC();

  public static final String ATTRIBUTE_FORMAT = "format";

  public static final String ATTRIBUTE_INDEXED = "indexed";

  private static final int MAX_EXCEPTION_SCAN_DEPTH = 10;

  private ActionRegistry actionRegistry;

  private SearchRequest searchRequest;

  private Map<String, QueryStatus> queryStatus = new HashMap<String, QueryStatus>();

  private long hits = 0;

  private long responseNum = 0;

  private List<Result> results = new ArrayList<>();

  private Search() {}

  public Search(SearchRequest request, ActionRegistry registry) {
    setSearchRequest(request);
    actionRegistry = registry;
  }

  private void setSearchRequest(SearchRequest request) {
    searchRequest = request;

    for (String sourceId : searchRequest.getSourceIds()) {
      queryStatus.put(sourceId, new QueryStatus(sourceId));
    }
  }

  public synchronized void update(QueryResponse queryResponse) {
    update(null, queryResponse);
  }

  /**
   * Adds a query response to the cached set of results.
   *
   * @param queryResponse - Query response to add
   * @throws InterruptedException
   */
  public synchronized void update(String sourceId, QueryResponse queryResponse) {
    if (queryResponse != null) {
      results = queryResponse.getResults();
      updateResultStatus(queryResponse.getResults());
      updateStatus(sourceId, queryResponse);
    }
  }

  public synchronized void failSource(String sourceId, Exception cause) {
    QueryResponseImpl failedResponse = new QueryResponseImpl(null);
    failedResponse.closeResultQueue();
    failedResponse.setHits(0);
    failedResponse.getProcessingDetails().add(new ProcessingDetailsImpl(sourceId, cause));
    failedResponse.getProperties().put("elapsed", -1L);

    updateStatus(sourceId, failedResponse);
  }

  private void updateStatus(String sourceId, QueryResponse queryResponse) {
    if (StringUtils.isBlank(sourceId)) {
      return;
    }

    if (!queryStatus.containsKey(sourceId)) {
      queryStatus.put(sourceId, new QueryStatus(sourceId));
    }

    QueryStatus status = queryStatus.get(sourceId);
    status.setDetails(queryResponse.getProcessingDetails());
    status.setHits(queryResponse.getHits());
    hits += queryResponse.getHits();
    status.setElapsed((Long) queryResponse.getProperties().get("elapsed"));
    status.setState(
        (isSuccessful(queryResponse.getProcessingDetails()) ? State.SUCCEEDED : State.FAILED));
    responseNum++;

    queryResponse
        .getProcessingDetails()
        .stream()
        .filter(ProcessingDetails::hasException)
        .forEach(
            details -> {
              status.addReason(generateSanitizedErrorMessage(details.getException(), 0));
            });
  }

  private String generateSanitizedErrorMessage(Throwable e, int depth) {
    if (depth > MAX_EXCEPTION_SCAN_DEPTH || e == null) {
      return REASON_INTERNAL;
    }

    if (SAFE_ERROR_MESSAGES.containsKey(e.getClass())) {
      return SAFE_ERROR_MESSAGES.get(e.getClass());
    }

    return generateSanitizedErrorMessage(e.getCause(), depth + 1);
  }

  private boolean isSuccessful(final Set<ProcessingDetails> details) {
    for (ProcessingDetails detail : details) {
      if (detail.hasException()) {
        return false;
      }
    }
    return true;
  }

  private void updateResultStatus(List<Result> results) {
    Bag hitSourceCount = new HashBag();

    for (QueryStatus status : queryStatus.values()) {
      status.setResultCount(0);
    }

    for (Result result : results) {
      hitSourceCount.add(result.getMetacard().getSourceId());
    }

    for (Object sourceId : hitSourceCount.uniqueSet()) {
      if (queryStatus.containsKey(sourceId)) {
        queryStatus.get(sourceId).setResultCount(hitSourceCount.getCount(sourceId));
      }
    }
  }

  public boolean isFinished() {
    return responseNum >= searchRequest.getSourceIds().size();
  }

  public SearchRequest getSearchRequest() {
    return searchRequest;
  }

  public Map<String, QueryStatus> getQueryStatus() {
    return queryStatus;
  }

  public long getHits() {
    return hits;
  }

  public void setHits(long hits) {
    this.hits = hits;
  }

  public List<Result> getResults() {
    return results;
  }

  private void addObject(Map<String, Object> obj, String name, Object value) {
    if (value instanceof Number) {
      if (value instanceof Double) {
        if (((Double) value).isInfinite()) {
          obj.put(name, null);
        } else {
          obj.put(name, value);
        }
      } else if (value instanceof Float) {
        if (((Float) value).isInfinite()) {
          obj.put(name, null);
        } else {
          obj.put(name, value);
        }
      } else {
        obj.put(name, value);
      }
    } else if (value != null) {
      obj.put(name, value);
    }
  }

  public Map<String, Object> transform(String searchRequestId) throws CatalogTransformerException {

    Map<String, Object> result = new HashMap<>();

    addObject(result, HITS, this.getHits());
    addObject(result, ID, searchRequestId);
    addObject(result, RESULTS, getResultList(this.getResults()));
    addObject(result, STATUS, getQueryStatus(this.getQueryStatus()));
    addObject(result, METACARD_TYPES, getMetacardTypes(this.getResults()));

    return result;
  }

  private List<Map<String, Object>> getQueryStatus(Map<String, QueryStatus> queryStatus) {
    List<Map<String, Object>> statuses = new ArrayList<>();

    for (QueryStatus status : queryStatus.values()) {
      Map<String, Object> statusObject = new HashMap<>();

      addObject(statusObject, ID, status.getSourceId());
      if (status.isDone()) {
        addObject(statusObject, RESULTS, status.getResultCount());
        addObject(statusObject, HITS, status.getHits());
        addObject(statusObject, ELAPSED, status.getElapsed());
        addObject(statusObject, REASONS, status.getReasons());
      }
      addObject(statusObject, STATE, status.getState());

      statuses.add(statusObject);
    }

    return statuses;
  }

  private List<Map<String, Object>> getResultList(List<Result> results)
      throws CatalogTransformerException {
    List<Map<String, Object>> resultsList = new ArrayList<>();
    if (results != null) {
      for (Result result : results) {
        if (result == null) {
          throw new CatalogTransformerException("Cannot transform null " + Result.class.getName());
        }
        Map<String, Object> resultItem = getResultItem(result);
        if (resultItem != null) {
          resultsList.add(resultItem);
        }
      }
    }
    return resultsList;
  }

  private Map<String, Object> getResultItem(Result result) throws CatalogTransformerException {
    Map<String, Object> transformedResult = new HashMap<>();

    addObject(transformedResult, DISTANCE, result.getDistanceInMeters());
    addObject(transformedResult, RELEVANCE, result.getRelevanceScore());

    @SuppressWarnings("unchecked")
    Map<String, Object> metacard =
        (Map<String, Object>) GeoJsonMetacardTransformer.convertToJSON(result.getMetacard());
    metacard.put(ACTIONS, getActions(result.getMetacard()));

    Attribute cachedDate = result.getMetacard().getAttribute(CACHED);
    if (cachedDate != null && cachedDate.getValue() != null) {
      metacard.put(CACHED, ISO_8601_DATE_FORMAT.print(new DateTime(cachedDate.getValue())));
    } else {
      metacard.put(CACHED, ISO_8601_DATE_FORMAT.print(new DateTime()));
    }

    metacard.put(
        IS_RESOURCE_LOCAL,
        Optional.ofNullable(result.getMetacard().getAttribute(INTERNAL_LOCAL_RESOURCE))
            .map(Attribute::getValue)
            .orElse(Boolean.FALSE));

    addObject(transformedResult, METACARD, metacard);

    return transformedResult;
  }

  private List<Map<String, Object>> getActions(Metacard metacard) {
    List<Map<String, Object>> actionsJson = new ArrayList<>();

    List<Action> actions = actionRegistry.list(metacard);
    if (actions != null) {
      for (Action action : actions) {
        Map<String, Object> actionJson = new HashMap<>();
        actionJson.put(ACTIONS_ID, action.getId() + action.getTitle());
        actionJson.put(ACTIONS_TITLE, action.getTitle());
        actionJson.put(ACTIONS_DESCRIPTION, action.getDescription());
        // user were seeing an issue where the json url was not quoted, we were not able to
        // reproduce the issue but resolved it by converting the url to a string
        actionJson.put(ACTIONS_URL, action.getUrl() != null ? action.getUrl().toString() : null);
        actionsJson.add(actionJson);
      }
    }
    return actionsJson;
  }

  private Map<String, Object> getMetacardTypes(List<Result> results)
      throws CatalogTransformerException {
    Map<String, Object> typesObject = new HashMap<>();

    for (Result result : results) {
      MetacardType type = result.getMetacard().getMetacardType();
      if (type != null
          && !StringUtils.isBlank(type.getName())
          && !typesObject.containsKey(type.getName())) {
        Map<String, Object> typeObj = getType(type);
        if (typeObj != null) {
          typesObject.put(type.getName(), typeObj);
        }
      }
    }
    return typesObject;
  }

  private Map<String, Object> getType(MetacardType metacardType)
      throws CatalogTransformerException {
    Map<String, Object> fields = new HashMap<>();

    for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
      Map<String, Object> description = new HashMap<>();
      description.put(ATTRIBUTE_FORMAT, descriptor.getType().getAttributeFormat().toString());
      description.put(ATTRIBUTE_INDEXED, descriptor.isIndexed());

      fields.put(descriptor.getName(), description);
    }
    return fields;
  }
}
