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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.codice.ddf.opensearch.query.OpenSearchQuery;
import org.codice.ddf.ui.searchui.query.model.Search;
import org.codice.ddf.ui.searchui.query.model.SearchRequest;
import org.codice.ddf.ui.searchui.query.servlet.CometdServlet;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
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
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;
import ddf.security.SecurityConstants;
import ddf.security.Subject;

/**
 * Created by tustisos on 12/11/13.
 */
public class SearchController {

    private static final XLogger LOGGER = new XLogger(
            LoggerFactory.getLogger(SearchController.class));

    public static final String ID = "geojson";

    public static MimeType DEFAULT_MIME_TYPE = null;

    static {
        try {
            DEFAULT_MIME_TYPE = new MimeType("application/json");
        } catch (MimeTypeParseException e) {
            LOGGER.warn("", e);
        }
    }

    private static final String UPDATE_QUERY_INTERVAL = "interval";

    private final CometdServlet cometdServlet;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    // TODO: just store the searches in memory for now, change this later
    private final Map<UUID, Search> searchMap = Collections
            .synchronizedMap(new HashMap<UUID, Search>());

    List<Future<Response>> futureList = new ArrayList<Future<Response>>();

    CatalogFramework framework;

    public SearchController(CatalogFramework framework, CometdServlet cometdServlet) {
        this.framework = framework;
        this.cometdServlet = cometdServlet;
    }

    public Response executeQuery(final SearchRequest searchRequest) throws InterruptedException {

        final SearchController controller = this;

        QueryResponse localQueryResponse = executeQuery(searchRequest.getLocalQueryRequest(),
                searchRequest.getSubject());
        addQueryResponseToSearch(searchRequest, localQueryResponse);

        for (final OpenSearchQuery fedQuery : searchRequest.getRemoteQueryRequests()) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    QueryResponse fedResponse = executeQuery(fedQuery, searchRequest.getSubject());
                    try {
                        boolean changed = addQueryResponseToSearch(searchRequest, fedResponse);
                        if (changed) {
                            cometdServlet.getSearchResultService().pushResults(
                                    searchRequest.getGuid(),
                                    controller.transformResponseCometd(
                                            searchMap.get(searchRequest.getGuid())
                                                    .getCompositeQueryResponse(), searchRequest));
                        }
                    } catch (InterruptedException e) {
                        LOGGER.error("Failed adding federated search results.", e);
                    }
                }
            });
        }

        return transformResponseJaxrs(localQueryResponse, searchRequest);
    }

    private boolean addQueryResponseToSearch(SearchRequest searchRequest,
            QueryResponse queryResponse) throws InterruptedException {
        boolean changed = false;
        if (searchMap.containsKey(searchRequest.getGuid())) {
            Search search = searchMap.get(searchRequest.getGuid());
            changed = search.addQueryResponse(queryResponse);
        } else {
            Search search = new Search();
            changed = search.addQueryResponse(queryResponse);
            search.setSearchRequest(searchRequest);
            searchMap.put(searchRequest.getGuid(), search);
        }
     return changed;
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
                    LOGGER.debug("Adding " + SecurityConstants.SECURITY_SUBJECT
                            + " property with value " + subject + " to request.");
                    queryRequest.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
                }

                LOGGER.debug("Sending query");
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

    private Response transformResponseJaxrs(QueryResponse queryResponse, SearchRequest searchRequest) {
        BinaryContent content;
        Response response;
        String organization = framework.getOrganization();
        UriInfo ui = searchRequest.getUi();
        String url = ui.getRequestUri().toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("organization: " + organization);
            LOGGER.debug("url: " + url);
        }

        if (queryResponse != null) {
            try {
                content = transform(queryResponse, searchRequest);
                response = Response.ok(content.getInputStream(), content.getMimeTypeValue())
        .build();
            } catch (CatalogTransformerException e) {
                LOGGER.warn("Error tranforming response", e);
                response = Response.serverError()
                        .entity(wrapStringInPreformattedTags(e.getMessage())).build();
            }
        } else {
            response = Response.serverError()
                    .entity(wrapStringInPreformattedTags("Server error, unable to query.")).build();
        }
        return response;
    }

    private String transformResponseCometd(QueryResponse queryResponse, SearchRequest searchRequest) {
        BinaryContent content = null;

        if (queryResponse != null) {
            try {
                content = transform(queryResponse, searchRequest);
            } catch (CatalogTransformerException e) {
                LOGGER.warn("Error tranforming response", e);
            }
        }

        String response = "";

        if (content != null) {
            try {
                response = new String(content.getByteArray());
            } catch (IOException e) {
                LOGGER.error("Unable to stringify query response.", e);
            }
        }
        return response;
    }

    public String wrapStringInPreformattedTags(String stringToWrap) {
        return "<pre>" + stringToWrap + "</pre>";
    }

    public BinaryContent transform(SourceResponse upstreamResponse, SearchRequest searchRequest)
        throws CatalogTransformerException {
        if (upstreamResponse == null) {
            throw new CatalogTransformerException("Cannot transform null "
                    + SourceResponse.class.getName());
        }

        JSONObject rootObject = new JSONObject();

        addNonNullObject(rootObject, "hits", upstreamResponse.getHits());
        addNonNullObject(rootObject, "guid", searchRequest.getGuid().toString());

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
        addNonNullObject(rootObject, "results", resultsList);

        String jsonText = JSONValue.toJSONString(rootObject);

        return new ddf.catalog.data.BinaryContentImpl(
                new ByteArrayInputStream(jsonText.getBytes()), DEFAULT_MIME_TYPE);
    }

    public static JSONObject convertToJSON(Result result) throws CatalogTransformerException {
        JSONObject rootObject = new JSONObject();

        addNonNullObject(rootObject, "distance", result.getDistanceInMeters());
        addNonNullObject(rootObject, "relevance", result.getRelevanceScore());
        addNonNullObject(rootObject, "metacard",
                GeoJsonMetacardTransformer.convertToJSON(result.getMetacard()));

        return rootObject;
    }

    private static void addNonNullObject(JSONObject obj, String name, Object value) {
        if (value != null) {
            obj.put(name, value);
        }
    }

    @Override
    public String toString() {
        return MetacardTransformer.class.getName() + " {Impl=" + this.getClass().getName()
                + ", id=" + ID + ", MIME Type=" + DEFAULT_MIME_TYPE + "}";
    }
}
