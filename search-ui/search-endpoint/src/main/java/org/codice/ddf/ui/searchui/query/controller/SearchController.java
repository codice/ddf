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
package org.codice.ddf.ui.searchui.query.controller;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.commons.collections.map.LRUMap;
import org.codice.ddf.ui.searchui.query.model.QueryStatus;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.ServerMessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The SearchController class handles all of the query threads for asynchronous queries.
 */
public class SearchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // TODO: just store the searches in memory for now, change this later
    private final Map<String, Search> searchMap = Collections
            .synchronizedMap(new LRUMap(1000));

    private CatalogFramework framework;

    private BayeuxServer bayeuxServer;

    /**
     * Create a new SearchController
     * 
     * @param framework
     *            - CatalogFramework that will be handling the actual queries
     */
    public SearchController(CatalogFramework framework) {
        this.framework = framework;
    }

    /**
     * Destroys this controller. This controller may not be used again after this method is called.
     */
    public void destroy() {
        executorService.shutdown();
    }

    /**
     * Push results out to clients
     * @param channel - Channel to send results on
     * @param jsonData
     * @param serverSession
     */
    public synchronized void pushResults(String channel, JSONObject jsonData, ServerSession serverSession) {
        String channelName;
        //you can't have 2 leading slashes, but if there isn't one, add it
        if (channel.startsWith("/")) {
            channelName = channel;
        } else {
            channelName = "/" + channel;
        }

        LOGGER.debug("Creating channel if it doesn't exist: {}", channelName);

        bayeuxServer.createChannelIfAbsent(channelName, new ConfigurableServerChannel.Initializer()
        {
            public void configureChannel(ConfigurableServerChannel channel) {
            channel.setPersistent(true);
            }
        });

        ServerMessage.Mutable reply = new ServerMessageImpl();
        reply.put(Search.SUCCESSFUL, true);
        reply.putAll(jsonData);

        LOGGER.debug("Sending results to subscribers on: {}", channelName);

        bayeuxServer.getChannel(channelName).publish(serverSession, reply, null);
    }

    /**
     * Execute all of the queries contained within the SearchRequest
     * 
     * @param request
     *            - SearchRequest containing a query for 1 or more sources
     * @param session
     *            - Cometd ServerSession
     */
    public void executeQuery(final SearchRequest request, final ServerSession session, final Subject subject) {

        final SearchController controller = this;

        executorService.submit(new Runnable() {
            @Override
            public void run() {
                Map<String, Serializable> properties = new HashMap<String, Serializable>();
                // check if there are any currently cached results
                properties.put("mode", "cache");
                // search cache for all sources
                QueryResponse response = executeQuery(null, request,
                        subject, properties);

                try {
                    Search search = addQueryResponseToSearch(request, response);
                    pushResults(request.getId(),
                            controller.transform(search, request),
                            session);
                } catch (InterruptedException e) {
                    LOGGER.error("Failed adding cached search results.", e);
                } catch (CatalogTransformerException e) {
                    LOGGER.error("Failed to transform cached search results.", e);
                }
            }
        });

        for (final String sourceId : request.getSourceIds()) {
            LOGGER.debug("Executing async query on: {}", sourceId);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    Map<String, Serializable> properties = new HashMap<String, Serializable>();
                    // update index from federated sources
                    properties.put("mode", "index");
                    QueryResponse indexResponse = executeQuery(sourceId, request,
                            subject, properties);

                    // query updated cache
                    properties.put("mode", "cache");
                    QueryResponse cachedResponse = executeQuery(null, request,
                            subject, properties);

                    try {
                        Search search = addQueryResponseToSearch(request, cachedResponse);
                        search.updateStatus(sourceId, indexResponse);
                        pushResults(request.getId(),
                                    controller.transform(search, request),
                                    session);
                        if (search.isFinished()) {
                            searchMap.remove(request.getId());
                        }
                    } catch (InterruptedException e) {
                        LOGGER.error("Failed adding federated search results.", e);
                    } catch (CatalogTransformerException e) {
                        LOGGER.error("Failed to transform federated search results.", e);
                    }
                }
            });
        }
    }

    private Search addQueryResponseToSearch(SearchRequest searchRequest,
            QueryResponse queryResponse) throws InterruptedException {
        Search search = null;
        if (searchMap.containsKey(searchRequest.getId())) {
            LOGGER.debug("Using previously created Search object for cache: {}",
                    searchRequest.getId());
            search = searchMap.get(searchRequest.getId());
            search.addQueryResponse(queryResponse);
        } else {
            LOGGER.debug("Creating new Search object to cache async query results: {}",
                    searchRequest.getId());
            search = new Search();
            search.setSearchRequest(searchRequest);
            search.addQueryResponse(queryResponse);
            searchMap.put(searchRequest.getId(), search);
        }
        return search;
    }

    /**
     * Executes the OpenSearchQuery and formulates the response
     * 
     * @param subject
     *            -the user subject
     * 
     * @return the response on the query
     */
    private QueryResponse executeQuery(String sourceId, SearchRequest searchRequest,
            Subject subject,
            Map<String,
            Serializable> properties) {
        Query query = searchRequest.getQuery();
        QueryResponse response = getEmptyResponse(sourceId);
        long startTime = System.currentTimeMillis();

        try {
            if (query != null) {
                List<String> sourceIds;
                if (sourceId == null) {
                    sourceIds = new ArrayList(searchRequest.getSourceIds());
                } else {
                    sourceIds = Arrays.asList(sourceId);
                }
                QueryRequest request = new QueryRequestImpl(query, false, sourceIds, properties);

                if (subject != null) {
                    LOGGER.debug("Adding {} property with value {} to request.",
                            SecurityConstants.SECURITY_SUBJECT, subject);
                    request.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
                }

                LOGGER.debug("Sending query: {}", query);
                response = framework.query(request);
            }
        } catch (UnsupportedQueryException e) {
            LOGGER.warn("Error executing query", e);
            response.getProcessingDetails().add(new ProcessingDetailsImpl(sourceId, e));
        } catch (FederationException e) {
            LOGGER.warn("Error executing query", e);
            response.getProcessingDetails().add(new ProcessingDetailsImpl(sourceId, e));
        } catch (SourceUnavailableException e) {
            LOGGER.warn("Error executing query because the underlying source was unavailable.", e);
            response.getProcessingDetails().add(new ProcessingDetailsImpl(sourceId, e));
        } catch (RuntimeException e) {
            // Account for any runtime exceptions and send back a server error
            // this prevents full stacktraces returning to the client
            // this allows for a graceful server error to be returned
            LOGGER.warn("RuntimeException on executing query", e);
            response.getProcessingDetails().add(new ProcessingDetailsImpl(sourceId, e));
        }
        long estimatedTime = System.currentTimeMillis() - startTime;
        response.getProperties().put("elapsed", estimatedTime);

        return response;
    }

    private QueryResponse getEmptyResponse(String sourceId) {
        // No query was specified
        QueryRequest queryRequest = new QueryRequestImpl(null, false,
                Arrays.asList(sourceId), null);

        // Create a dummy QueryResponse with zero results
        return new QueryResponseImpl(queryRequest, new ArrayList<Result>(), 0);
    }

    private JSONObject transform(Search search, SearchRequest searchRequest)
        throws CatalogTransformerException {

        SourceResponse upstreamResponse = search.getCompositeQueryResponse();

        if (upstreamResponse == null) {
            throw new CatalogTransformerException("Cannot transform null "
                    + SourceResponse.class.getName());
        }

        JSONObject rootObject = new JSONObject();

        addObject(rootObject, Search.HITS, search.getHits());
        addObject(rootObject, Search.ID, searchRequest.getId().toString());
        addObject(rootObject, Search.RESULTS, getResultList(upstreamResponse.getResults()));
        addObject(rootObject, Search.STATUS, getQueryStatus(search.getQueryStatus()));

        return rootObject;
    }

    private JSONArray getQueryStatus(Map<String, QueryStatus> queryStatus) {
        JSONArray statuses = new JSONArray();

        for (String key : queryStatus.keySet()) {
            QueryStatus status = queryStatus.get(key);

            JSONObject statusObject = new JSONObject();

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

    private JSONArray getResultList(List<Result> results)
            throws CatalogTransformerException {
        JSONArray resultsList = new JSONArray();

        if (results != null) {
            for (Result result : results) {
                if (result == null) {
                    throw new CatalogTransformerException("Cannot transform null "
                            + Result.class.getName());
                }
                JSONObject jsonObj = convertToJSON(result);
                if (jsonObj != null) {
                    resultsList.add(jsonObj);
                }
            }
        }
        return resultsList;
    }

    private static JSONObject convertToJSON(Result result) throws CatalogTransformerException {
        JSONObject rootObject = new JSONObject();

        addObject(rootObject, Search.DISTANCE, result.getDistanceInMeters());
        addObject(rootObject, Search.RELEVANCE, result.getRelevanceScore());
        addObject(rootObject, Search.METACARD,
                GeoJsonMetacardTransformer.convertToJSON(result.getMetacard()));

        return rootObject;
    }

    private static void addObject(JSONObject obj, String name, Object value) {
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

    public CatalogFramework getFramework() {
        return framework;
    }

    public void setBayeuxServer(BayeuxServer bayeuxServer) {
        this.bayeuxServer = bayeuxServer;
    }
}
