/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl.operations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.DescribableImpl;
import ddf.catalog.util.impl.Requests;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;

public class QueryOperations extends DescribableImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueryOperations.class);

    // Inject properties
    private final FrameworkProperties frameworkProperties;

    private final SourceOperations sourceOperations;

    private final OperationsSecuritySupport opsSecuritySupport;

    private final OperationsMetacardSupport opsMetacardSupport;

    public QueryOperations(FrameworkProperties frameworkProperties,
            SourceOperations sourceOperations, OperationsSecuritySupport opsSecuritySupport,
            OperationsMetacardSupport opsMetacardSupport) {
        this.frameworkProperties = frameworkProperties;
        this.sourceOperations = sourceOperations;
        this.opsSecuritySupport = opsSecuritySupport;
        this.opsMetacardSupport = opsMetacardSupport;
    }

    //
    // Delegate methods
    //
    public QueryResponse query(QueryRequest fedQueryRequest, boolean fanoutEnabled)
            throws UnsupportedQueryException, SourceUnavailableException, FederationException {
        return query(fedQueryRequest, null, fanoutEnabled);
    }

    public QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy,
            boolean fanoutEnabled)
            throws SourceUnavailableException, UnsupportedQueryException, FederationException {
        return query(queryRequest, strategy, false, fanoutEnabled);
    }

    //
    // Helper methods
    //
    QueryResponse query(QueryRequest queryRequest, FederationStrategy strategy,
            boolean overrideFanoutRename, boolean fanoutEnabled)
            throws UnsupportedQueryException, FederationException {

        FederationStrategy fedStrategy = strategy;
        QueryResponse queryResponse;

        queryRequest = setFlagsOnRequest(queryRequest);

        try {
            queryRequest = validateQueryRequest(queryRequest, fanoutEnabled);
            queryRequest = getFanoutQuery(queryRequest, fanoutEnabled);
            queryRequest = populateQueryRequestPolicyMap(queryRequest);
            queryRequest = processPreQueryAccessPlugins(queryRequest);
            queryRequest = processPreQueryPlugins(queryRequest);
            queryRequest = validateQueryRequest(queryRequest, fanoutEnabled);

            if (fedStrategy == null) {
                if (frameworkProperties.getFederationStrategy() == null) {
                    throw new FederationException(
                            "No Federation Strategies exist.  Cannot execute federated query.");
                } else {
                    LOGGER.debug("FederationStrategy was not specified, using default strategy: "
                            + frameworkProperties.getFederationStrategy()
                            .getClass());
                    fedStrategy = frameworkProperties.getFederationStrategy();
                }
            }

            queryResponse = doQuery(queryRequest, fedStrategy, fanoutEnabled);
            queryResponse = injectAttributes(queryResponse);
            queryResponse = validateFixQueryResponse(queryResponse,
                    overrideFanoutRename,
                    fanoutEnabled);
            queryResponse = populateQueryResponsePolicyMap(queryResponse);
            queryResponse = processPostQueryAccessPlugins(queryResponse);
            queryResponse = processPostQueryPlugins(queryResponse);

        } catch (RuntimeException re) {
            throw new UnsupportedQueryException("Exception during runtime while performing query", re);
        }

        return queryResponse;
    }

    /**
     * Executes a query using the specified {@link QueryRequest} and {@link FederationStrategy}.
     * Based on the isEnterprise and sourceIds list in the query request, the federated query may
     * include the local provider and {@link ConnectedSource}s.
     *
     * @param queryRequest  the {@link QueryRequest}
     * @param strategy      the {@link FederationStrategy}
     * @param fanoutEnabled
     * @return the {@link QueryResponse}
     * @throws FederationException
     */
    QueryResponse doQuery(QueryRequest queryRequest, FederationStrategy strategy,
            boolean fanoutEnabled) throws FederationException {

        Set<ProcessingDetails> exceptions = new HashSet<>();
        Set<String> sourceIds = getCombinedIdSet(queryRequest);
        LOGGER.debug("source ids: {}", sourceIds);
        List<Source> sourcesToQuery = new ArrayList<>();
        boolean addConnectedSources = false;
        boolean addCatalogProvider = false;
        boolean sourceFound;

        if (queryRequest.isEnterprise()) { // Check if it's an enterprise query
            addConnectedSources = true;
            addCatalogProvider = hasCatalogProvider(fanoutEnabled);

            if (sourceIds != null && !sourceIds.isEmpty()) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "Enterprise Query also included specific sites which will now be ignored");
                }
                sourceIds.clear();
            }

            // add all the federated sources
            for (FederatedSource source : frameworkProperties.getFederatedSources()
                    .values()) {
                if (sourceOperations.isSourceAvailable(source) && canAccessSource(source,
                        queryRequest)) {
                    sourcesToQuery.add(source);
                } else {
                    exceptions.add(createUnavailableProcessingDetails(source));
                }
            }

        } else if (sourceIds != null && !sourceIds.isEmpty()) {
            // it's a targeted federated query
            if (includesLocalSources(sourceIds)) {
                LOGGER.debug("Local source is included in sourceIds");
                addConnectedSources = connectedSourcesExist();
                addCatalogProvider = hasCatalogProvider(fanoutEnabled);
                sourceIds.remove(getId());
                sourceIds.remove(null);
                sourceIds.remove("");
            }

            // See if we still have sources to look up by name
            if (!sourceIds.isEmpty()) {
                for (String id : sourceIds) {
                    LOGGER.debug("Looking up source ID = {}", id);
                    sourceFound = false;
                    if (frameworkProperties.getFederatedSources()
                            .containsKey(id)) {
                        sourceFound = true;
                        if (frameworkProperties.getFederatedSources()
                                .get(id)
                                .isAvailable()
                                && canAccessSource(frameworkProperties.getFederatedSources()
                                .get(id), queryRequest)) {
                            sourcesToQuery.add(frameworkProperties.getFederatedSources()
                                    .get(id));
                        } else {
                            exceptions.add(createUnavailableProcessingDetails(frameworkProperties.getFederatedSources()
                                    .get(id)));
                        }
                    }

                    if (!sourceFound) {
                        exceptions.add(new ProcessingDetailsImpl(id,
                                new SourceUnavailableException("Source id is not found")));
                    }
                }
            }
        } else {
            // default to local sources
            addConnectedSources = connectedSourcesExist();
            addCatalogProvider = hasCatalogProvider(fanoutEnabled);
        }

        if (addConnectedSources) {
            // add Connected Sources
            for (ConnectedSource source : frameworkProperties.getConnectedSources()) {
                if (sourceOperations.isSourceAvailable(source)) {
                    sourcesToQuery.add(source);
                } else {
                    LOGGER.debug("Connected Source {} is unavailable and will not be queried.",
                            source.getId());
                }
            }
        }

        if (addCatalogProvider) {
            if (sourceOperations.isSourceAvailable(sourceOperations.getCatalog())) {
                sourcesToQuery.add(sourceOperations.getCatalog());
            } else {
                exceptions.add(createUnavailableProcessingDetails(sourceOperations.getCatalog()));
            }
        }

        if (sourcesToQuery.isEmpty()) {
            // We have nothing to query at all.
            // TODO change to SourceUnavailableException
            throw new FederationException(
                    "SiteNames could not be resolved due to  invalid site names, none of the sites were available, or the current subject doesn't have permission to access the sites.");
        }

        LOGGER.debug("Calling strategy.federate()");

        QueryResponse response = strategy.federate(sourcesToQuery, queryRequest);
        frameworkProperties.getQueryResponsePostProcessor()
                .processResponse(response);
        return addProcessingDetails(exceptions, response);
    }

    <T extends Request> T setFlagsOnRequest(T request) {
        if (request != null) {
            Set<String> ids = getCombinedIdSet(request);

            request.getProperties()
                    .put(Constants.LOCAL_DESTINATION_KEY,
                            ids.isEmpty() || (sourceOperations.getCatalog() != null && ids.contains(
                                    sourceOperations.getCatalog()
                                            .getId())));
            request.getProperties()
                    .put(Constants.REMOTE_DESTINATION_KEY,
                            (Requests.isLocal(request) && ids.size() > 1) || (!Requests.isLocal(
                                    request) && !ids.isEmpty()));
        }

        return request;
    }

    Filter getFilterWithAdditionalFilters(List<Filter> originalFilter) {
        return frameworkProperties.getFilterBuilder()
                .allOf(getTagsQueryFilter(),
                        frameworkProperties.getValidationQueryFactory()
                                .getFilterWithValidationFilter(),
                        frameworkProperties.getFilterBuilder()
                                .anyOf(originalFilter));
    }

    /**
     * Replaces the site name(s) of {@link FederatedSource}s in the {@link QueryResponse} with the
     * fanout's site name to keep info about the {@link FederatedSource}s hidden from the external
     * client.
     *
     * @param queryResponse the original {@link QueryResponse} from the query request
     * @return the updated {@link QueryResponse} with all site names replaced with fanout's site
     * name
     */
    public QueryResponse replaceSourceId(QueryResponse queryResponse) {
        LOGGER.debug("ENTERING: replaceSourceId()");
        List<Result> results = queryResponse.getResults();
        QueryResponseImpl newResponse = new QueryResponseImpl(queryResponse.getRequest(),
                queryResponse.getProperties());
        for (Result result : results) {
            MetacardImpl newMetacard = new MetacardImpl(result.getMetacard());
            newMetacard.setSourceId(this.getId());
            ResultImpl newResult = new ResultImpl(newMetacard);
            // Copy over scores
            newResult.setDistanceInMeters(result.getDistanceInMeters());
            newResult.setRelevanceScore(result.getRelevanceScore());
            newResponse.addResult(newResult, false);
        }
        newResponse.setHits(queryResponse.getHits());
        newResponse.closeResultQueue();
        LOGGER.debug("EXITING: replaceSourceId()");
        return newResponse;
    }

    private QueryResponse processPostQueryPlugins(QueryResponse queryResponse)
            throws FederationException {
        for (PostQueryPlugin service : frameworkProperties.getPostQuery()) {
            try {
                queryResponse = service.process(queryResponse);
            } catch (PluginExecutionException see) {
                LOGGER.warn("Error executing PostQueryPlugin: {}", see.getMessage(), see);
            } catch (StopProcessingException e) {
                throw new FederationException("Query could not be executed.", e);
            }
        }
        return queryResponse;
    }

    private QueryResponse processPostQueryAccessPlugins(QueryResponse queryResponse)
            throws FederationException {
        for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
            try {
                queryResponse = plugin.processPostQuery(queryResponse);
            } catch (StopProcessingException e) {
                throw new FederationException("Query could not be executed.", e);
            }
        }
        return queryResponse;
    }

    private QueryResponse populateQueryResponsePolicyMap(QueryResponse queryResponse)
            throws FederationException {
        HashMap<String, Set<String>> responsePolicyMap = new HashMap<>();
        Map<String, Serializable> unmodifiableProperties =
                Collections.unmodifiableMap(queryResponse.getProperties());
        for (Result result : queryResponse.getResults()) {
            HashMap<String, Set<String>> itemPolicyMap = new HashMap<>();
            for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
                try {
                    PolicyResponse policyResponse = plugin.processPostQuery(result,
                            unmodifiableProperties);
                    opsSecuritySupport.buildPolicyMap(itemPolicyMap,
                            policyResponse.itemPolicy()
                                    .entrySet());
                    opsSecuritySupport.buildPolicyMap(responsePolicyMap,
                            policyResponse.operationPolicy()
                                    .entrySet());
                } catch (StopProcessingException e) {
                    throw new FederationException("Query could not be executed.", e);
                }
            }
            result.getMetacard()
                    .setAttribute(new AttributeImpl(Metacard.SECURITY, itemPolicyMap));
        }
        queryResponse.getProperties()
                .put(PolicyPlugin.OPERATION_SECURITY, responsePolicyMap);

        return queryResponse;
    }

    private QueryRequest processPreQueryPlugins(QueryRequest queryReq) throws FederationException {
        for (PreQueryPlugin service : frameworkProperties.getPreQuery()) {
            try {
                queryReq = service.process(queryReq);
            } catch (PluginExecutionException see) {
                LOGGER.warn("Error executing PreQueryPlugin: {}", see.getMessage(), see);
            } catch (StopProcessingException e) {
                throw new FederationException("Query could not be executed.", e);
            }
        }
        return queryReq;
    }

    private QueryRequest processPreQueryAccessPlugins(QueryRequest queryReq)
            throws FederationException {
        for (AccessPlugin plugin : frameworkProperties.getAccessPlugins()) {
            try {
                queryReq = plugin.processPreQuery(queryReq);
            } catch (StopProcessingException e) {
                throw new FederationException("Query could not be executed.", e);
            }
        }
        return queryReq;
    }

    private QueryRequest populateQueryRequestPolicyMap(QueryRequest queryReq)
            throws FederationException {
        HashMap<String, Set<String>> requestPolicyMap = new HashMap<>();
        Map<String, Serializable> unmodifiableProperties =
                Collections.unmodifiableMap(queryReq.getProperties());
        for (PolicyPlugin plugin : frameworkProperties.getPolicyPlugins()) {
            try {
                PolicyResponse policyResponse = plugin.processPreQuery(queryReq.getQuery(),
                        unmodifiableProperties);
                opsSecuritySupport.buildPolicyMap(requestPolicyMap,
                        policyResponse.operationPolicy()
                                .entrySet());
            } catch (StopProcessingException e) {
                throw new FederationException("Query could not be executed.", e);
            }
        }
        queryReq.getProperties()
                .put(PolicyPlugin.OPERATION_SECURITY, requestPolicyMap);

        return queryReq;
    }

    private QueryRequest getFanoutQuery(QueryRequest queryRequest, boolean fanoutEnabled) {
        if (!fanoutEnabled) {
            return queryRequest;
        }
        return new QueryRequestImpl(queryRequest.getQuery(),
                true,
                null,
                queryRequest.getProperties());
    }

    /**
     * Validates that the {@link QueryRequest} is non-null and that the query in it is non-null.
     *
     * @param queryRequest  the {@link QueryRequest}
     * @param fanoutEnabled
     * @throws UnsupportedQueryException if the {@link QueryRequest} is null or the query in it is null
     */
    private QueryRequest validateQueryRequest(QueryRequest queryRequest, boolean fanoutEnabled)
            throws UnsupportedQueryException {
        if (queryRequest == null) {
            throw new UnsupportedQueryException(
                    "QueryRequest was null, either passed in from endpoint, or as output from a PreQuery Plugin");
        }

        if (queryRequest.getQuery() == null) {
            throw new UnsupportedQueryException(
                    "Cannot perform query with null query, either passed in from endpoint, or as output from a PreQuery Plugin");
        }

        if (fanoutEnabled) {
            Set<String> sources = queryRequest.getSourceIds();
            if (sources != null) {
                for (String querySourceId : sources) {
                    LOGGER.debug("validating requested sourceId {}", querySourceId);
                    if (!querySourceId.equals(this.getId())) {
                        UnsupportedQueryException unsupportedQueryException =
                                new UnsupportedQueryException("Unknown source: " + querySourceId);
                        LOGGER.debug(
                                "Throwing unsupportedQueryException due to unknown sourceId: {}",
                                querySourceId,
                                unsupportedQueryException);
                        throw unsupportedQueryException;
                    }
                }
            }
        }

        return queryRequest;
    }

    private QueryResponse injectAttributes(QueryResponse response) {
        List<Result> results = response.getResults()
                .stream()
                .map(result -> {
                    Metacard original = result.getMetacard();
                    Metacard metacard = opsMetacardSupport.applyInjectors(original,
                            frameworkProperties.getAttributeInjectors());
                    ResultImpl newResult = new ResultImpl(metacard);
                    newResult.setDistanceInMeters(result.getDistanceInMeters());
                    newResult.setRelevanceScore(result.getRelevanceScore());
                    return newResult;
                })
                .collect(Collectors.toList());

        return new QueryResponseImpl(response.getRequest(),
                results,
                true,
                response.getHits(),
                response.getProperties());
    }

    /**
     * Validates that the {@link QueryResponse} has a non-null list of {@link Result}s in it, and
     * that the original {@link QueryRequest} is included in the response.
     *
     * @param queryResponse        the original {@link QueryResponse} returned from the source
     * @param overrideFanoutRename
     * @param fanoutEnabled
     * @return the updated {@link QueryResponse}
     * @throws UnsupportedQueryException if the original {@link QueryResponse} is null or the results list is null
     */
    private QueryResponse validateFixQueryResponse(QueryResponse queryResponse,
            boolean overrideFanoutRename, boolean fanoutEnabled) throws UnsupportedQueryException {
        if (queryResponse == null) {
            throw new UnsupportedQueryException(
                    "CatalogProvider returned null QueryResponse Object.");
        }
        if (queryResponse.getResults() == null) {
            throw new UnsupportedQueryException(
                    "CatalogProvider returned null list of results from query method.");
        }

        if (fanoutEnabled && !overrideFanoutRename) {
            queryResponse = replaceSourceId(queryResponse);
        }

        return queryResponse;
    }

    private boolean canAccessSource(FederatedSource source, QueryRequest request) {
        Map<String, Set<String>> securityAttributes = source.getSecurityAttributes();
        if (securityAttributes.isEmpty()) {
            return true;
        }

        Object requestSubject = request.getProperties()
                .get(SecurityConstants.SECURITY_SUBJECT);
        if (requestSubject instanceof ddf.security.Subject) {
            Subject subject = (Subject) requestSubject;

            KeyValueCollectionPermission kvCollection = new KeyValueCollectionPermission(
                    CollectionPermission.READ_ACTION,
                    securityAttributes);
            boolean isPermitted = subject.isPermitted(kvCollection);
            if (isPermitted) {
                SecurityLogger.audit("Subject is permitted to access source {}", source.getId());
            } else {
                SecurityLogger.audit("Subject is not permitted to access source {}",
                        source.getId());
            }
            return isPermitted;
        }
        return false;
    }

    private ProcessingDetailsImpl createUnavailableProcessingDetails(Source source) {
        ProcessingDetailsImpl exception = new ProcessingDetailsImpl();
        SourceUnavailableException sue = new SourceUnavailableException(
                "Source \"" + source.getId() + "\" is unavailable and will not be queried");
        exception.setException(sue);
        exception.setSourceId(source.getId());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Source Unavailable", sue);
        }
        return exception;
    }

    /**
     * Determines if this catalog framework has any {@link ConnectedSource}s configured.
     *
     * @return true if this framework has any connected sources configured, false otherwise
     */
    private boolean connectedSourcesExist() {
        return CollectionUtils.isNotEmpty(frameworkProperties.getConnectedSources());
    }

    /**
     * Adds any exceptions to the query response's processing details.
     *
     * @param exceptions the set of exceptions to include in the response's {@link ProcessingDetails}. Can
     *                   be empty, but cannot be null.
     * @param response   the {@link QueryResponse} to add the exceptions to
     * @return the modified {@link QueryResponse}
     */
    private QueryResponse addProcessingDetails(Set<ProcessingDetails> exceptions,
            QueryResponse response) {

        if (!exceptions.isEmpty()) {
            // we have exceptions to merge in
            if (response == null) {
                LOGGER.warn(
                        "Could not add Query exceptions to a QueryResponse because the list of ProcessingDetails was null -- according to the API this should not happen");
            } else {
                // need to merge them together.
                Set<ProcessingDetails> sourceDetails = response.getProcessingDetails();
                sourceDetails.addAll(exceptions);
            }
        }
        return response;
    }

    /**
     * Determines if the local catlog provider's source ID is included in the list of source IDs. A
     * source ID in the list of null or an empty string are treated the same as the local source's
     * actual ID being in the list.
     *
     * @param sourceIds the list of source IDs to examine
     * @return true if the list includes the local source's ID, false otherwise
     */
    private boolean includesLocalSources(Set<String> sourceIds) {
        return sourceIds != null && (sourceIds.contains(getId()) || sourceIds.contains("")
                || sourceIds.contains(null));
    }

    private Set<String> getCombinedIdSet(Request request) {
        Set<String> ids = new HashSet<>();
        if (request != null) {
            if (request.getStoreIds() != null) {
                ids.addAll(request.getStoreIds());
            }
            if (request instanceof QueryRequest
                    && ((QueryRequest) request).getSourceIds() != null) {
                ids.addAll(((QueryRequest) request).getSourceIds());
            }
        }
        return ids;
    }

    /**
     * Whether this {@link ddf.catalog.CatalogFramework} is configured with a {@code CatalogProvider}.
     *
     * @param fanoutEnabled
     * @return true if this has a {@code CatalogProvider} configured,
     * false otherwise
     */
    private boolean hasCatalogProvider(boolean fanoutEnabled) {
        if (!fanoutEnabled && sourceOperations.getCatalog() != null) {
            LOGGER.trace("hasCatalogProvider() returning true");
            return true;
        }

        LOGGER.trace("hasCatalogProvider() returning false");
        return false;
    }

    private Filter getTagsQueryFilter() {
        return frameworkProperties.getFilterBuilder()
                .anyOf(frameworkProperties.getFilterBuilder()
                                .attribute(Metacard.TAGS)
                                .is()
                                .like()
                                .text(FilterDelegate.WILDCARD_CHAR),
                        frameworkProperties.getFilterBuilder()
                                .attribute(Metacard.TAGS)
                                .empty());
    }
}
