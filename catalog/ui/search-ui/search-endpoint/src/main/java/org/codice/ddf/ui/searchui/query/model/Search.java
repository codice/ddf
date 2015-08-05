/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.ui.searchui.query.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.Bag;
import org.apache.commons.collections.bag.HashBag;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.ui.searchui.query.model.QueryStatus.State;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;

/**
 * This class represents the cached asynchronous query response from all sources.
 */
public class Search {

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

    private static final Logger LOGGER = LoggerFactory.getLogger(Search.class);

    private static final DateTimeFormatter ISO_8601_DATE_FORMAT = DateTimeFormat
            .forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").withZoneUTC();

    private ActionRegistry actionRegistry;

    private SearchRequest searchRequest;

    private QueryResponse compositeQueryResponse;

    private Map<String, QueryStatus> queryStatus = new HashMap<String, QueryStatus>();

    private long hits = 0;

    private long responseNum = 0;

    private Search() {

    }

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
     *
     * @throws InterruptedException
     */
    public synchronized void update(String sourceId, QueryResponse queryResponse) {
        if (queryResponse != null) {
            compositeQueryResponse = queryResponse;
            updateResultStatus(queryResponse.getResults());
            updateStatus(sourceId, queryResponse);
        }
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
        status.setState((isSuccessful(queryResponse.getProcessingDetails()) ?
                State.SUCCEEDED :
                State.FAILED));
        responseNum++;
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

        for (String sourceId : queryStatus.keySet()) {
            queryStatus.get(sourceId).setResultCount(0);
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

    public QueryResponse getCompositeQueryResponse() {
        return compositeQueryResponse;
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

    public Map<String, Object> transform(String searchRequestId) throws
            CatalogTransformerException {

        SourceResponse upstreamResponse = this.getCompositeQueryResponse();
        Map<String, MetacardType> metaTypes = new HashMap<>();
        if (upstreamResponse == null) {
            throw new CatalogTransformerException(
                    "Cannot transform null " + SourceResponse.class.getName());
        }

        Map<String, Object> result = new HashMap<>();

        addObject(result, Search.HITS, this.getHits());
        addObject(result, Search.ID, searchRequestId);
        addObject(result, Search.RESULTS,
                getResultList(upstreamResponse.getResults(), metaTypes));
        addObject(result, Search.STATUS, getQueryStatus(this.getQueryStatus()));
        addObject(result, Search.METACARD_TYPES, getMetacardTypes(metaTypes.values()));

        return result;
    }

    private List<Map<String, Object>> getQueryStatus(Map<String, QueryStatus> queryStatus) {
        List<Map<String, Object>> statuses = new ArrayList<>();

        for (String key : queryStatus.keySet()) {
            QueryStatus status = queryStatus.get(key);

            Map<String, Object> statusObject = new HashMap<>();

            addObject(statusObject, Search.ID, status.getSourceId());
            if (status.isDone()) {
                addObject(statusObject, Search.RESULTS, status.getResultCount());
                addObject(statusObject, Search.HITS, status.getHits());
                addObject(statusObject, Search.ELAPSED, status.getElapsed());
            }
            addObject(statusObject, Search.STATE, status.getState());

            statuses.add(statusObject);
        }

        return statuses;
    }

    private List<Map<String, Object>> getResultList(List<Result> results,
            Map<String, MetacardType> metaTypes) throws CatalogTransformerException {
        List<Map<String, Object>> resultsList = new ArrayList<>();
        if (results != null) {
            for (Result result : results) {
                if (result == null) {
                    throw new CatalogTransformerException(
                            "Cannot transform null " + Result.class.getName());
                }
                Map<String, Object> resultItem = getResultItem(result, metaTypes);
                if (resultItem != null) {
                    resultsList.add(resultItem);
                }
            }
        }
        return resultsList;
    }

    private Map<String, Object> getResultItem(Result result, Map<String, MetacardType> metaTypes) throws
            CatalogTransformerException {
        Map<String, Object> transformedResult = new HashMap<>();

        addObject(transformedResult, Search.DISTANCE, result.getDistanceInMeters());
        addObject(transformedResult, Search.RELEVANCE, result.getRelevanceScore());

        @SuppressWarnings("unchecked")
        Map<String, Object> metacard = (Map<String, Object>) GeoJsonMetacardTransformer
                .convertToJSON(result.getMetacard());
        metacard.put(Search.ACTIONS, getActions(result.getMetacard()));

        Attribute cachedDate = result.getMetacard().getAttribute(Search.CACHED);
        if (cachedDate != null && cachedDate.getValue() != null) {
            metacard.put(Search.CACHED,
                    ISO_8601_DATE_FORMAT.print(new DateTime(cachedDate.getValue())));
        } else {
            metacard.put(Search.CACHED, ISO_8601_DATE_FORMAT.print(new DateTime()));
        }

        addObject(transformedResult, Search.METACARD, metacard);

        if (result.getMetacard().getMetacardType() != null && !StringUtils
                .isBlank(result.getMetacard().getMetacardType().getName())) {
            metaTypes.put(result.getMetacard().getMetacardType().getName(),
                    result.getMetacard().getMetacardType());
        }
        return transformedResult;
    }

    private List<Map<String, Object>> getActions(Metacard metacard) {
        List<Map<String, Object>> actionsJson = new ArrayList<>();

        List<Action> actions = actionRegistry.list(metacard);
        for (Action action : actions) {
            Map<String, Object> actionJson = new HashMap<>();
            actionJson.put(Search.ACTIONS_ID, action.getId() + action.getTitle());
            actionJson.put(Search.ACTIONS_TITLE, action.getTitle());
            actionJson.put(Search.ACTIONS_DESCRIPTION, action.getDescription());
            actionJson.put(Search.ACTIONS_URL, action.getUrl());
            actionsJson.add(actionJson);
        }
        return actionsJson;
    }

    private Map<String, Object> getMetacardTypes(Collection<MetacardType> types) throws
            CatalogTransformerException {
        Map<String, Object> typesObject = new HashMap<>();

        for (MetacardType type : types) {
            Map<String, Object> typeObj = getResultItem(type);
            if (typeObj != null) {
                typesObject.put(type.getName(), typeObj);
            }
        }

        return typesObject;
    }

    private Map<String, Object> getResultItem(MetacardType metacardType) throws CatalogTransformerException {
        Map<String, Object> fields = new HashMap<>();

        for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
            Map<String, Object> description = new HashMap<>();
            description.put("format", descriptor.getType().getAttributeFormat().toString());
            description.put("indexed", descriptor.isIndexed());

            fields.put(descriptor.getName(), description);
        }
        return fields;
    }
}
