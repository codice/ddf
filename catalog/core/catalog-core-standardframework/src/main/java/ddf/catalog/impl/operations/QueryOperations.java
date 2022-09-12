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
package ddf.catalog.impl.operations;

import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.delegate.TagsFilterDelegate;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.DescribableImpl;
import ddf.catalog.util.impl.Requests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for query delegate operations for the {@code CatalogFrameworkImpl}.
 *
 * <p>This class contains two delegated query methods and methods to support them. No
 * operations/support methods should be added to this class except in support of CFI query
 * operations.
 */
public class QueryOperations extends DescribableImpl {
  private static final Logger LOGGER = LoggerFactory.getLogger(QueryOperations.class);

  private static final String MAX_PAGE_SIZE_PROPERTY = "catalog.maxPageSize";

  private static final String ZERO_PAGESIZE_COMPATIBILITY_PROPERTY =
      "catalog.zeroPageSizeCompatibility";

  private static final String QUERY_FAILURE_MSG = "Query could not be executed.";

  /**
   * Enforcing a default maximum page size of 1000 to avoid overloading the system with too many
   * records. In practice, correct paging techniques should be implemented. If needed, this property
   * can be overridden by setting "catalog.maxPageSize" in system properties.
   *
   * <p>(See DDF-2872 for more details)
   */
  private static final Integer DEFAULT_MAX_PAGE_SIZE = 1000;

  public static final Integer MAX_PAGE_SIZE = determineAndRetrieveMaxPageSize();

  private static final Supplier<Boolean> ZERO_PAGESIZE_COMPATIBILTY =
      () -> Boolean.valueOf(System.getProperty(ZERO_PAGESIZE_COMPATIBILITY_PROPERTY));

  // Inject properties
  private final FrameworkProperties frameworkProperties;

  private final SourceOperations sourceOperations;

  private final OperationsMetacardSupport opsMetacardSupport;

  private FilterAdapter filterAdapter;

  private List<String> fanoutProxyTagBlacklist = new ArrayList<>();

  private long queryTimeoutMillis = 300000;

  public QueryOperations(
      FrameworkProperties frameworkProperties,
      SourceOperations sourceOperations,
      OperationsMetacardSupport opsMetacardSupport) {
    this.frameworkProperties = frameworkProperties;
    this.sourceOperations = sourceOperations;
    this.opsMetacardSupport = opsMetacardSupport;
  }

  private static Integer determineAndRetrieveMaxPageSize() {
    return NumberUtils.toInt(System.getProperty(MAX_PAGE_SIZE_PROPERTY), DEFAULT_MAX_PAGE_SIZE);
  }

  public void setFanoutProxyTagBlacklist(List<String> fanoutProxyTagBlacklist) {
    this.fanoutProxyTagBlacklist = fanoutProxyTagBlacklist;
  }

  public void setFilterAdapter(FilterAdapter filterAdapter) {
    this.filterAdapter = filterAdapter;
  }

  public void setQueryTimeoutMillis(long queryTimeoutMillis) {
    this.queryTimeoutMillis = queryTimeoutMillis;
  }

  //
  // Delegate methods
  //
  public QueryResponse query(QueryRequest fedQueryRequest, boolean fanoutEnabled)
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {
    return query(fedQueryRequest, null, fanoutEnabled);
  }

  public QueryResponse query(
      QueryRequest queryRequest, FederationStrategy strategy, boolean fanoutEnabled)
      throws SourceUnavailableException, UnsupportedQueryException, FederationException {
    return query(queryRequest, strategy, false, fanoutEnabled);
  }

  //
  // Helper methods
  //
  QueryResponse query(
      QueryRequest queryRequest,
      FederationStrategy strategy,
      boolean overrideFanoutRename,
      boolean fanoutEnabled)
      throws UnsupportedQueryException, FederationException {

    FederationStrategy fedStrategy = strategy;
    QueryResponse queryResponse;

    queryRequest = setFlagsOnRequest(queryRequest);

    try {
      queryRequest = validateQueryRequest(queryRequest);
      queryRequest = getFanoutQuery(queryRequest, fanoutEnabled);
      queryRequest = processPreQueryPlugins(queryRequest);
      queryRequest = validateQueryRequest(queryRequest);

      if (fedStrategy == null) {
        if (frameworkProperties.getFederationStrategy() == null) {
          throw new FederationException(
              "No Federation Strategies exist.  Cannot execute federated query.");
        } else {
          LOGGER.debug(
              "FederationStrategy was not specified, using default strategy: {}",
              frameworkProperties.getFederationStrategy().getClass());
          fedStrategy = frameworkProperties.getFederationStrategy();
        }
      }

      queryResponse = doQuery(queryRequest, fedStrategy);

      // Allow callers to determine the total results returned from the query; this value
      // may differ from the number of filtered results after processing plugins have been run.
      queryResponse.getProperties().put("actualResultSize", queryResponse.getResults().size());
      LOGGER.trace("BeforePostQueryFilter result size: {}", queryResponse.getResults().size());
      queryResponse = injectAttributes(queryResponse);
      queryResponse = validateFixQueryResponse(queryResponse, overrideFanoutRename, fanoutEnabled);
      queryResponse = processPostQueryPlugins(queryResponse);

      log(queryResponse);

    } catch (RuntimeException re) {
      LOGGER.debug("Unhandled runtime exception during query", re);
      throw new UnsupportedQueryException("Exception during runtime while performing query", re);
    }

    return queryResponse;
  }

  private void log(QueryResponse queryResponse) {
    LOGGER.trace("AfterPostQueryFilter result size: {}", queryResponse.getResults().size());
    LOGGER.trace("Total Hit count: {}", queryResponse.getHits());
    if (LOGGER.isTraceEnabled() && queryResponse.getProcessingDetails() != null) {
      LOGGER.trace("Processing details count: {}", queryResponse.getProcessingDetails().size());
      for (ProcessingDetails details : queryResponse.getProcessingDetails()) {
        if (details.getException() != null) {
          LOGGER.trace(
              "Source [{}] query exception", details.getSourceId(), details.getException());
        }
        if (details.getWarnings() != null && !details.getWarnings().isEmpty()) {
          LOGGER.trace(
              "Source [{}] warnings: {}",
              details.getSourceId(),
              String.join(", ", details.getWarnings()));
        }
      }
    }
  }

  /**
   * Executes a query using the specified {@link QueryRequest} and {@link FederationStrategy}. Based
   * on the isEnterprise and sourceIds list in the query request, the federated query may include
   * the local provider and {@link ConnectedSource}s.
   *
   * @param queryRequest the {@link QueryRequest}
   * @param strategy the {@link FederationStrategy}
   * @return the {@link QueryResponse}
   * @throws FederationException
   */
  QueryResponse doQuery(QueryRequest queryRequest, FederationStrategy strategy)
      throws FederationException {
    Set<String> sourceIds = getCombinedIdSet(queryRequest);
    LOGGER.debug("source ids: {}", sourceIds);

    QuerySources querySources =
        new QuerySources(frameworkProperties)
            .initializeSources(this, queryRequest, sourceIds)
            .addConnectedSources(this, frameworkProperties)
            .addCatalogProvider(this);

    if (querySources.isEmpty()) {
      // We have nothing to query at all.
      // TODO change to SourceUnavailableException
      throw new FederationException(
          "SiteNames could not be resolved due to invalid site names or none of the sites "
              + "were available.");
    }

    LOGGER.debug("Calling strategy.federate()");

    Query originalQuery = queryRequest.getQuery();

    if (originalQuery != null && originalQuery.getTimeoutMillis() <= 0) {

      Query modifiedQuery =
          new QueryImpl(
              originalQuery,
              originalQuery.getStartIndex(),
              originalQuery.getPageSize(),
              originalQuery.getSortBy(),
              originalQuery.requestsTotalResultsCount(),
              queryTimeoutMillis);

      queryRequest =
          new QueryRequestImpl(
              modifiedQuery,
              queryRequest.isEnterprise(),
              queryRequest.getSourceIds(),
              queryRequest.getProperties());
    }

    QueryResponse response = strategy.federate(querySources.sourcesToQuery, queryRequest);
    frameworkProperties.getQueryResponsePostProcessor().processResponse(response);
    return addProcessingDetails(querySources.exceptions, response);
  }

  <T extends Request> T setFlagsOnRequest(T request) {
    if (request != null) {
      Set<String> ids = getCombinedIdSet(request);

      request
          .getProperties()
          .put(
              Constants.LOCAL_DESTINATION_KEY,
              ids.isEmpty()
                  || (sourceOperations.getCatalog() != null
                      && ids.contains(sourceOperations.getCatalog().getId())));
      request
          .getProperties()
          .put(
              Constants.REMOTE_DESTINATION_KEY,
              (Requests.isLocal(request) && ids.size() > 1)
                  || (!Requests.isLocal(request) && !ids.isEmpty()));
    }

    return request;
  }

  /**
   * Replaces the site name(s) of {@link FederatedSource}s in the {@link QueryResponse} with the
   * fanout's site name to keep info about the {@link FederatedSource}s hidden from the external
   * client.
   *
   * @param queryResponse the original {@link QueryResponse} from the query request
   * @return the updated {@link QueryResponse} with all site names replaced with fanout's site name
   */
  public QueryResponse replaceSourceId(QueryResponse queryResponse) {
    LOGGER.trace("ENTERING: replaceSourceId()");
    List<Result> results = queryResponse.getResults();
    QueryResponseImpl newResponse =
        new QueryResponseImpl(queryResponse.getRequest(), queryResponse.getProperties());
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
    LOGGER.trace("EXITING: replaceSourceId()");
    return newResponse;
  }

  /**
   * Whether this {@link ddf.catalog.CatalogFramework} is configured with a {@code CatalogProvider}.
   *
   * @return true if this has a {@code CatalogProvider} configured, false otherwise
   */
  boolean hasCatalogProvider() {
    if (sourceOperations.getCatalog() != null) {
      LOGGER.trace("hasCatalogProvider() returning true");
      return true;
    }

    LOGGER.trace("hasCatalogProvider() returning false");
    return false;
  }

  /**
   * Determines if the local catlog provider's source ID is included in the list of source IDs. A
   * source ID in the list of null or an empty string are treated the same as the local source's
   * actual ID being in the list.
   *
   * @param sourceIds the list of source IDs to examine
   * @return true if the list includes the local source's ID, false otherwise
   */
  boolean includesLocalSources(Set<String> sourceIds) {
    return sourceIds != null
        && (sourceIds.contains(getId()) || sourceIds.contains("") || sourceIds.contains(null));
  }

  private QueryResponse processPostQueryPlugins(QueryResponse queryResponse)
      throws FederationException {
    for (PostQueryPlugin service : frameworkProperties.getPostQuery()) {
      try {
        queryResponse = service.process(queryResponse);
      } catch (PluginExecutionException see) {
        LOGGER.debug("Error executing PostQueryPlugin: {}", see.getMessage(), see);
      } catch (StopProcessingException e) {
        throw new FederationException(QUERY_FAILURE_MSG, e);
      }
    }
    return queryResponse;
  }

  private QueryRequest processPreQueryPlugins(QueryRequest queryReq) throws FederationException {
    for (PreQueryPlugin service : frameworkProperties.getPreQuery()) {
      try {
        queryReq = service.process(queryReq);
      } catch (PluginExecutionException see) {
        LOGGER.debug("Error executing PreQueryPlugin: {}", see.getMessage(), see);
      } catch (StopProcessingException e) {
        throw new FederationException(QUERY_FAILURE_MSG, e);
      }
    }
    return queryReq;
  }

  private QueryRequest getFanoutQuery(QueryRequest queryRequest, boolean fanoutEnabled) {
    if (!fanoutEnabled || blockProxyFanoutQuery(queryRequest)) {
      return queryRequest;
    }

    return new QueryRequestImpl(queryRequest.getQuery(), true, null, queryRequest.getProperties());
  }

  private boolean blockProxyFanoutQuery(QueryRequest queryRequest) {
    if (filterAdapter == null) {
      return false;
    }

    try {
      return filterAdapter.adapt(
          queryRequest.getQuery(), new TagsFilterDelegate(new HashSet<>(fanoutProxyTagBlacklist)));
    } catch (UnsupportedQueryException e) {
      LOGGER.debug(
          "Error checking if fanout query should be proxied. Defaulting to yes, proxy the query");
      return false;
    }
  }

  /**
   * Validates that the {@link QueryRequest} is non-null and that the query in it is non-null. Also
   * checks that the query's page size is between 1 and the {@link #MAX_PAGE_SIZE}. If not, the
   * query's page size is set to the {@link #MAX_PAGE_SIZE}.
   *
   * @param queryRequest the {@link QueryRequest}
   * @throws UnsupportedQueryException if the {@link QueryRequest} is null or the query in it is
   *     null
   */
  private QueryRequest validateQueryRequest(QueryRequest queryRequest)
      throws UnsupportedQueryException {
    if (queryRequest == null) {
      throw new UnsupportedQueryException(
          "QueryRequest was null, either passed in from endpoint, or as output from a PreQuery Plugin");
    }

    if (queryRequest.getQuery() == null) {
      throw new UnsupportedQueryException(
          "Cannot perform query with null query, either passed in from endpoint, or as output from a PreQuery Plugin");
    }

    Query originalQuery = queryRequest.getQuery();

    int queryPageSize = originalQuery.getPageSize();

    if (originalQuery.getPageSize() < 0) {
      queryPageSize = MAX_PAGE_SIZE;
    }

    if (originalQuery.getPageSize() == 0) {
      if (ZERO_PAGESIZE_COMPATIBILTY.get()) {
        queryPageSize = MAX_PAGE_SIZE;
      }
    }

    if (originalQuery.getPageSize() > 0) {
      queryPageSize = Math.min(originalQuery.getPageSize(), MAX_PAGE_SIZE);
    }

    Query modifiedQuery =
        new QueryImpl(
            originalQuery,
            originalQuery.getStartIndex(),
            queryPageSize,
            originalQuery.getSortBy(),
            originalQuery.requestsTotalResultsCount(),
            originalQuery.getTimeoutMillis());

    return new QueryRequestImpl(
        modifiedQuery,
        queryRequest.isEnterprise(),
        queryRequest.getSourceIds(),
        queryRequest.getProperties());
  }

  private QueryResponse injectAttributes(QueryResponse response) {
    List<Result> results =
        response.getResults().stream()
            .map(
                result -> {
                  Metacard original = result.getMetacard();
                  Metacard metacard =
                      opsMetacardSupport.applyInjectors(
                          original, frameworkProperties.getAttributeInjectors());
                  ResultImpl newResult = new ResultImpl(metacard);
                  newResult.setDistanceInMeters(result.getDistanceInMeters());
                  newResult.setRelevanceScore(result.getRelevanceScore());
                  return newResult;
                })
            .collect(Collectors.toList());

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(
            response.getRequest(), results, true, response.getHits(), response.getProperties());
    queryResponse.setProcessingDetails(response.getProcessingDetails());
    return queryResponse;
  }

  /**
   * Validates that the {@link QueryResponse} has a non-null list of {@link Result}s in it, and that
   * the original {@link QueryRequest} is included in the response.
   *
   * @param queryResponse the original {@link QueryResponse} returned from the source
   * @param overrideFanoutRename
   * @param fanoutEnabled
   * @return the updated {@link QueryResponse}
   * @throws UnsupportedQueryException if the original {@link QueryResponse} is null or the results
   *     list is null
   */
  private QueryResponse validateFixQueryResponse(
      QueryResponse queryResponse, boolean overrideFanoutRename, boolean fanoutEnabled)
      throws UnsupportedQueryException {
    if (queryResponse == null) {
      throw new UnsupportedQueryException("CatalogProvider returned null QueryResponse Object.");
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

  private ProcessingDetailsImpl createUnavailableProcessingDetails(Source source) {
    ProcessingDetailsImpl exception = new ProcessingDetailsImpl();
    SourceUnavailableException sue =
        new SourceUnavailableException(
            "Source \"" + source.getId() + "\" is unavailable and will not be queried");
    exception.setException(sue);
    exception.setSourceId(source.getId());
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Source Unavailable", sue);
    }
    return exception;
  }

  /**
   * Adds any exceptions to the query response's processing details.
   *
   * @param exceptions the set of exceptions to include in the response's {@link ProcessingDetails}.
   *     Can be empty, but cannot be null.
   * @param response the {@link QueryResponse} to add the exceptions to
   * @return the modified {@link QueryResponse}
   */
  private QueryResponse addProcessingDetails(
      Set<ProcessingDetails> exceptions, QueryResponse response) {

    if (!exceptions.isEmpty()) {
      // we have exceptions to merge in
      if (response == null) {
        LOGGER.debug(
            "Could not add Query exceptions to a QueryResponse because the list of ProcessingDetails was null -- according to the API this should not happen");
      } else {
        // need to merge them together.
        Set<ProcessingDetails> sourceDetails = response.getProcessingDetails();
        sourceDetails.addAll(exceptions);
      }
    }
    return response;
  }

  private Set<String> getCombinedIdSet(Request request) {
    Set<String> ids = new HashSet<>();
    if (request != null) {
      if (request.getStoreIds() != null) {
        ids.addAll(request.getStoreIds());
      }
      if (request instanceof QueryRequest && ((QueryRequest) request).getSourceIds() != null) {
        ids.addAll(((QueryRequest) request).getSourceIds());
      }
    }
    return ids;
  }

  static class QuerySources {
    private final FrameworkProperties frameworkProperties;

    List<Source> sourcesToQuery = new ArrayList<>();

    Set<ProcessingDetails> exceptions = new HashSet<>();

    boolean needToAddConnectedSources = false;

    boolean needToAddCatalogProvider = false;

    QuerySources(FrameworkProperties frameworkProperties) {
      this.frameworkProperties = frameworkProperties;
    }

    QuerySources initializeSources(
        QueryOperations queryOps, QueryRequest queryRequest, Set<String> sourceIds) {
      if (queryRequest.isEnterprise()) { // Check if it's an enterprise query
        needToAddConnectedSources = true;
        needToAddCatalogProvider = queryOps.hasCatalogProvider();

        if (sourceIds != null && !sourceIds.isEmpty()) {
          LOGGER.debug("Enterprise Query also included specific sites which will now be ignored");
          sourceIds.clear();
        }

        // add all the federated sources
        Set<String> notPermittedSources = new HashSet<>();
        for (FederatedSource source : frameworkProperties.getFederatedSources()) {
          if (queryOps.sourceOperations.isSourceAvailable(source)) {
            sourcesToQuery.add(source);
          } else {
            exceptions.add(queryOps.createUnavailableProcessingDetails(source));
          }
        }

      } else if (CollectionUtils.isNotEmpty(sourceIds)) {
        // it's a targeted federated query
        if (queryOps.includesLocalSources(sourceIds)) {
          LOGGER.debug("Local source is included in sourceIds");
          needToAddConnectedSources =
              CollectionUtils.isNotEmpty(frameworkProperties.getConnectedSources());
          needToAddCatalogProvider = queryOps.hasCatalogProvider();
          sourceIds.remove(queryOps.getId());
          sourceIds.remove(null);
          sourceIds.remove("");
        }

        // See if we still have sources to look up by name
        if (!sourceIds.isEmpty()) {
          Set<String> notPermittedSources = new HashSet<>();
          for (String id : sourceIds) {
            LOGGER.debug("Looking up source ID = {}", id);

            Stream<FederatedSource> federatedSources =
                frameworkProperties.getFederatedSources().stream();
            Stream<CatalogStore> catalogStores = frameworkProperties.getCatalogStores().stream();

            List<FederatedSource> sources =
                new ArrayList<>(
                    Stream.concat(federatedSources, catalogStores)
                        .filter(e -> e.getId().equals(id))
                        .collect(Collectors.toList()));

            if (sources.isEmpty()) {
              exceptions.add(
                  new ProcessingDetailsImpl(
                      id, new SourceUnavailableException("Source id is not found")));
            } else {
              if (sources.size() != 1) {
                LOGGER.debug("Multiple sources found for id: {}", id);
              }
              FederatedSource source = sources.get(0);

              sourcesToQuery.add(source);
            }
          }
        }
      } else {
        // default to local sources
        needToAddConnectedSources =
            CollectionUtils.isNotEmpty(frameworkProperties.getConnectedSources());
        needToAddCatalogProvider = queryOps.hasCatalogProvider();
      }

      return this;
    }

    QuerySources addConnectedSources(
        QueryOperations queryOps, FrameworkProperties frameworkProperties) {
      if (needToAddConnectedSources) {
        // add Connected Sources
        for (ConnectedSource source : frameworkProperties.getConnectedSources()) {
          if (queryOps.sourceOperations.isSourceAvailable(source)) {
            sourcesToQuery.add(source);
          } else {
            LOGGER.debug(
                "Connected Source {} is unavailable and will not be queried.", source.getId());
          }
        }
      }

      return this;
    }

    QuerySources addCatalogProvider(QueryOperations queryOps) {
      if (needToAddCatalogProvider) {
        if (queryOps.sourceOperations.isSourceAvailable(queryOps.sourceOperations.getCatalog())) {
          sourcesToQuery.add(queryOps.sourceOperations.getCatalog());
        } else {
          exceptions.add(
              queryOps.createUnavailableProcessingDetails(queryOps.sourceOperations.getCatalog()));
        }
      }
      return this;
    }

    boolean isEmpty() {
      return sourcesToQuery.isEmpty();
    }
  }
}
