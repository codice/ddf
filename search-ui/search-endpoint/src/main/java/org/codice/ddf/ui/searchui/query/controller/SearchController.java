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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

import org.apache.commons.collections.map.LRUMap;
import org.codice.ddf.opensearch.query.OpenSearchQuery;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.cometd.bayeux.server.BayeuxServer;
import org.cometd.bayeux.server.ConfigurableServerChannel;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.cometd.server.ServerMessageImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

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
     * @param searchRequest
     *            - SearchRequest containing 1 or more query requests
     * @param serverSession
     *            - Cometd ServerSession
     * @throws InterruptedException
     * @throws CatalogTransformerException
     */
    public void executeQuery(final SearchRequest searchRequest, final ServerSession serverSession) throws InterruptedException, CatalogTransformerException {

        final SearchController controller = this;

        for (final OpenSearchQuery fedQuery : searchRequest.getQueryRequests()) {
            LOGGER.debug("Executing async query on: {}", fedQuery.getSiteIds());
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    QueryResponse fedResponse = executeQuery(fedQuery, null);
                    try {
                        addQueryResponseToSearch(searchRequest, fedResponse);
                        //send full response if it changed, otherwise send empty one
                        Search search = searchMap.get(searchRequest.getGuid());
                        pushResults(
                                searchRequest.getGuid(),
                                controller.transform(
                                        search.getCompositeQueryResponse(), searchRequest), serverSession);
                        if(search.isFinished()) {
                            searchMap.remove(searchRequest.getGuid());
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

    private void addQueryResponseToSearch(SearchRequest searchRequest,
            QueryResponse queryResponse) throws InterruptedException {
        if (searchMap.containsKey(searchRequest.getGuid())) {
            LOGGER.debug("Using previously created Search object for cache: {}",
                    searchRequest.getGuid());
            Search search = searchMap.get(searchRequest.getGuid());
            search.addQueryResponse(queryResponse);
        } else {
            LOGGER.debug("Creating new Search object to cache async query results: {}",
                    searchRequest.getGuid());
            Search search = new Search();
            search.addQueryResponse(queryResponse);
            search.setSearchRequest(searchRequest);
            searchMap.put(searchRequest.getGuid(), search);
        }
    }

    /**
     * Executes the OpenSearchQuery and formulates the response
     * 
     * @param query
     *            - the query to execute
     * 
     * @param subject
     *            -the user subject
     * 
     * @return the response on the query
     */
    private QueryResponse executeQuery(OpenSearchQuery query, Subject subject) {
        QueryResponse queryResponse = null;

        try {

            if (query.getFilter() != null) {
                QueryRequest queryRequest = new QueryRequestImpl(query, query.isEnterprise(),
                        query.getSiteIds(), null);

                if (subject != null) {
                    LOGGER.debug("Adding {} property with value {} to request.",
                            SecurityConstants.SECURITY_SUBJECT, subject);
                    queryRequest.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
                }

                LOGGER.debug("Sending query: {}", query);
                queryResponse = framework.query(queryRequest);

            } else {
                // No query was specified
                QueryRequest queryRequest = new QueryRequestImpl(query, query.isEnterprise(),
                        query.getSiteIds(), null);

                // Create a dummy QueryResponse with zero results
                queryResponse = new QueryResponseImpl(queryRequest, new ArrayList<Result>(), 0);
            }
        } catch (UnsupportedQueryException ce) {
            LOGGER.warn("Error executing query", ce);
        } catch (FederationException e) {
            LOGGER.warn("Error executing query", e);
        } catch (SourceUnavailableException e) {
            LOGGER.warn("Error executing query because the underlying source was unavailable.", e);
        } catch (RuntimeException e) {
            // Account for any runtime exceptions and send back a server error
            // this prevents full stacktraces returning to the client
            // this allows for a graceful server error to be returned
            LOGGER.warn("RuntimeException on executing query", e);
        }
        return queryResponse;
    }

    private JSONObject transform(SourceResponse upstreamResponse, SearchRequest searchRequest)
        throws CatalogTransformerException {
        if (upstreamResponse == null) {
            throw new CatalogTransformerException("Cannot transform null "
                    + SourceResponse.class.getName());
        }

        JSONObject rootObject = new JSONObject();

        addObject(rootObject, Search.HITS, upstreamResponse.getHits());
        addObject(rootObject, Search.GUID, searchRequest.getGuid().toString());

        JSONArray resultsList = new JSONArray();

        if (upstreamResponse.getResults() != null) {
            for (Result result : upstreamResponse.getResults()) {
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
        addObject(rootObject, Search.RESULTS, resultsList);

        return rootObject;
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
