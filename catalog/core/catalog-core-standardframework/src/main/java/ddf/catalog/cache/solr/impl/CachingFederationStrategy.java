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
package ddf.catalog.cache.solr.impl;

import com.google.common.collect.ImmutableList;
import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.RelevanceResultComparator;
import ddf.catalog.util.impl.Requests;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a {@link ddf.catalog.federation.FederationStrategy} based on sorting {@link
 * ddf.catalog.data.Metacard}s. The sorting is based on the {@link ddf.catalog.operation.Query}'s
 * {@link org.opengis.filter.sort.SortBy} propertyName. The possible sorting values are
 *
 * <ul>
 *   <li>{@link ddf.catalog.data.Metacard#EFFECTIVE}
 *   <li>{@link ddf.catalog.data.Result#TEMPORAL}
 *   <li>{@link ddf.catalog.data.Result#DISTANCE}
 *   <li>{@link ddf.catalog.data.Result#RELEVANCE}
 * </ul>
 *
 * The supported ordering includes {@link org.opengis.filter.sort.SortOrder#DESCENDING} and {@link
 * org.opengis.filter.sort.SortOrder#ASCENDING}. For this class to function properly a sort value
 * and sort order must be provided.
 *
 * @see ddf.catalog.data.Metacard
 * @see ddf.catalog.operation.Query
 * @see org.opengis.filter.sort.SortBy
 */
public class CachingFederationStrategy implements FederationStrategy, PostIngestPlugin {

  /**
   * The default comparator for sorting by {@link ddf.catalog.data.Result#RELEVANCE}, {@link
   * org.opengis.filter.sort.SortOrder#DESCENDING}
   */
  protected static final Comparator<Result> DEFAULT_COMPARATOR =
      new RelevanceResultComparator(SortOrder.DESCENDING);

  protected static final String QUERY_MODE = "mode";

  /** Query the cache */
  protected static final String CACHE_QUERY_MODE = "cache";

  /** Query without updating the cache */
  protected static final String NATIVE_QUERY_MODE = "native";

  /** Query and update the cache but block until done indexing */
  protected static final String INDEX_QUERY_MODE = "index";

  /** Query and update the cache without blocking */
  protected static final String UPDATE_QUERY_MODE = "update";

  /** package-private to allow for unit testing */
  static final int DEFAULT_MAX_START_INDEX = 50000;

  private static final Logger LOGGER = LoggerFactory.getLogger(CachingFederationStrategy.class);

  private final SolrCache cache;

  private final SolrCacheSource cacheSource;

  private final ExecutorService cacheExecutorService;

  /**
   * The {@link List} of pre-federated query plugins to execute on the query request before the
   * query is executed on the {@link Source}.
   */
  protected List<PreFederatedQueryPlugin> preQuery;

  /**
   * The {@link List} of post-federated query plugins to execute on the query request after the
   * query is executed on the {@link Source}.
   */
  protected List<PostFederatedQueryPlugin> postQuery;

  private SortedQueryMonitorFactory sortedQueryMonitorFactory = new SortedQueryMonitorFactory(this);

  private ExecutorService queryExecutorService;

  private int maxStartIndex;

  private CacheCommitPhaser cacheCommitPhaser;

  private CacheBulkProcessor cacheBulkProcessor;

  private boolean isCachingEverything = false;

  private boolean cacheRemoteIngests = false;

  private CacheQueryFactory cacheQueryFactory;

  /**
   * Instantiates an {@code AbstractFederationStrategy} with the provided {@link ExecutorService}.
   *
   * @param queryExecutorService the {@link ExecutorService} for queries
   */
  public CachingFederationStrategy(
      ExecutorService queryExecutorService,
      List<PreFederatedQueryPlugin> preQuery,
      List<PostFederatedQueryPlugin> postQuery,
      SolrCache cache,
      ExecutorService cacheExecutorService,
      CacheQueryFactory cacheQueryFactory) {

    Validate.notNull(queryExecutorService, "Valid queryExecutorService required.");
    Validate.notNull(preQuery, "Valid List<PreFederatedQueryPlugin> required.");
    Validate.noNullElements(preQuery, "preQuery cannot contain null elements.");
    Validate.notNull(postQuery, "Valid List<PostFederatedQueryPlugin> required.");
    Validate.noNullElements(postQuery, "postQuery cannot contain null elements.");
    Validate.notNull(cache, "Valid SolrCache required.");
    Validate.notNull(cacheExecutorService, "Valid cacheExecutorService required.");
    Validate.notNull(cacheQueryFactory, "Valid CacheQueryFactory required.");

    this.queryExecutorService = queryExecutorService;
    this.preQuery = preQuery;
    this.postQuery = postQuery;
    this.maxStartIndex = DEFAULT_MAX_START_INDEX;
    this.cache = cache;
    this.cacheExecutorService = cacheExecutorService;
    cacheCommitPhaser = new CacheCommitPhaser(cache);
    cacheBulkProcessor = new CacheBulkProcessor(cache);
    this.cacheQueryFactory = cacheQueryFactory;
    cacheSource = new SolrCacheSource(cache);
  }

  void setSortedQueryMonitorFactory(SortedQueryMonitorFactory sortedQueryMonitorFactory) {
    this.sortedQueryMonitorFactory = sortedQueryMonitorFactory;
  }

  void setCacheCommitPhaser(CacheCommitPhaser cacheCommitPhaser) {
    this.cacheCommitPhaser = cacheCommitPhaser;
  }

  void setCacheBulkProcessor(CacheBulkProcessor cacheBulkProcessor) {
    this.cacheBulkProcessor = cacheBulkProcessor;
  }

  @Override
  public QueryResponse federate(List<Source> sources, QueryRequest queryRequest) {
    Validate.noNullElements(sources, "Cannot federate with null sources.");
    Validate.notNull(queryRequest, "Cannot federate with null QueryRequest.");
    Set<String> sourceIds = new HashSet<>();
    for (Source source : sources) {
      sourceIds.add(source.getId());
    }
    QueryRequest modifiedQueryRequest =
        new QueryRequestImpl(
            queryRequest.getQuery(),
            queryRequest.isEnterprise(),
            sourceIds,
            queryRequest.getProperties());

    if (CACHE_QUERY_MODE.equals(queryRequest.getProperties().get(QUERY_MODE))) {
      return queryCache(modifiedQueryRequest);
    } else {
      return sourceFederate(sources, modifiedQueryRequest);
    }
  }

  QueryResponse queryCache(QueryRequest queryRequest) {
    return sourceFederate(
        ImmutableList.of(cacheSource),
        cacheQueryFactory.getQueryRequestWithSourcesFilter(queryRequest));
  }

  private QueryResponse sourceFederate(List<Source> sources, final QueryRequest queryRequest) {
    if (LOGGER.isDebugEnabled()) {
      for (Source source : sources) {
        if (source != null) {
          LOGGER.debug("source to query: {}", source.getId());
        }
      }
    }

    Query originalQuery = queryRequest.getQuery();

    int offset = originalQuery.getStartIndex();
    final int pageSize = originalQuery.getPageSize();

    // limit offset to max value
    if (offset > this.maxStartIndex) {
      offset = this.maxStartIndex;
    }

    final QueryResponseImpl queryResponseQueue = new QueryResponseImpl(queryRequest, null);

    Map<Future<SourceResponse>, QueryRequest> futures = new HashMap<>();

    Query modifiedQuery = getModifiedQuery(originalQuery, sources.size(), offset, pageSize);
    QueryRequest modifiedQueryRequest =
        new QueryRequestImpl(
            modifiedQuery,
            queryRequest.isEnterprise(),
            queryRequest.getSourceIds(),
            queryRequest.getProperties());

    CompletionService<SourceResponse> queryCompletion =
        new ExecutorCompletionService<>(queryExecutorService);

    // Do NOT call source.isAvailable() when checking sources
    for (final Source source : sources) {
      if (source != null) {
        LOGGER.debug("running query on source: {}", source.getId());

        QueryRequest sourceQueryRequest =
            new QueryRequestImpl(
                modifiedQuery,
                queryRequest.isEnterprise(),
                Collections.singleton(source.getId()),
                new HashMap<>(queryRequest.getProperties()));
        try {
          for (PreFederatedQueryPlugin service : preQuery) {
            try {
              sourceQueryRequest = service.process(source, sourceQueryRequest);
            } catch (PluginExecutionException e) {
              LOGGER.info("Error executing PreFederatedQueryPlugin", e);
            }
          }
        } catch (StopProcessingException e) {
          LOGGER.info("Plugin stopped processing", e);
        }

        futures.put(
            queryCompletion.submit(
                new CallableSourceResponse(new TimedSource(source), sourceQueryRequest)),
            sourceQueryRequest);
      }
    }

    QueryResponseImpl offsetResults = null;
    // If there are offsets and more than one source, we have to get all the
    // results back and then
    // transfer them into a different Queue. That is what the
    // OffsetResultHandler does.
    if (offset > 1 && sources.size() > 1) {
      offsetResults = new QueryResponseImpl(queryRequest, null);
      queryExecutorService.submit(
          new OffsetResultHandler(queryResponseQueue, offsetResults, pageSize, offset));
    }

    queryExecutorService.submit(
        sortedQueryMonitorFactory.createMonitor(
            queryCompletion, futures, queryResponseQueue, modifiedQueryRequest, postQuery));

    QueryResponse queryResponse;
    if (offset > 1 && sources.size() > 1) {
      queryResponse = offsetResults;
      LOGGER.debug("returning offsetResults");
    } else {
      queryResponse = queryResponseQueue;
      LOGGER.debug("returning returnResults: {}", queryResponse);
    }

    LOGGER.debug("returning Query Results: {}", queryResponse);
    return queryResponse;
  }

  private Query getModifiedQuery(
      Query originalQuery, int numberOfSources, int offset, int pageSize) {

    Query query;

    // If offset is not specified, our offset is 1
    if (offset > 1 && numberOfSources > 1) {

      final int modifiedOffset = 1;
      int modifiedPageSize = computeModifiedPageSize(offset, pageSize);

      LOGGER.debug(
          "Creating new query for federated sources to query each source from {} " + "to {}.",
          modifiedOffset,
          modifiedPageSize);
      LOGGER.debug("original offset: {}", offset);
      LOGGER.debug("original page size: {}", pageSize);
      LOGGER.debug("modified offset: {}", modifiedOffset);
      LOGGER.debug("modified page size: {}", modifiedPageSize);

      /**
       * Federated sources always query from offset of 1. When all query results are received from
       * all federated sources and merged together - then the offset is applied.
       */
      query =
          new QueryImpl(
              originalQuery,
              modifiedOffset,
              modifiedPageSize,
              originalQuery.getSortBy(),
              originalQuery.requestsTotalResultsCount(),
              originalQuery.getTimeoutMillis());
    } else {
      query = originalQuery;
    }

    return query;
  }

  /** Base 1 offset, hence page size is one less. */
  private int computeModifiedPageSize(int offset, int pageSize) {
    return offset + pageSize - 1;
  }

  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {
    return input;
  }

  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {

    LOGGER.debug("Post ingest processing of UpdateResponse.");
    if (!isCacheRemoteIngests() && !Requests.isLocal(input.getRequest())) {
      return input;
    }

    if (cacheSource
        .getId()
        .equals(input.getRequest().getProperties().get(Constants.SERVICE_TITLE))) {
      return input;
    }

    List<Metacard> metacards = new ArrayList<>(input.getUpdatedMetacards().size());

    for (Update update : input.getUpdatedMetacards()) {
      metacards.add(update.getNewMetacard());
    }

    LOGGER.debug("Updating metacard(s) in cache.");
    cache.put(metacards);
    LOGGER.debug("Updating metacard(s) in cache complete.");

    return input;
  }

  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {

    LOGGER.debug("Post ingest processing of DeleteResponse.");
    if (!isCacheRemoteIngests() && !Requests.isLocal(input.getRequest())) {
      return input;
    }

    if (cacheSource
        .getId()
        .equals(input.getRequest().getProperties().get(Constants.SERVICE_TITLE))) {
      return input;
    }

    LOGGER.debug("Deleting metacard(s) in cache.");
    cache.delete(input.getRequest());
    LOGGER.debug("Deletion of metacard(s) in cache complete.");

    return input;
  }

  int getMaxStartIndex() {
    return maxStartIndex;
  }

  /**
   * To be set via Spring/Blueprint
   *
   * @param maxStartIndex the new default max start index value
   */
  public void setMaxStartIndex(int maxStartIndex) {
    this.maxStartIndex = DEFAULT_MAX_START_INDEX;

    if (maxStartIndex > 0) {
      this.maxStartIndex = maxStartIndex;
    } else {
      LOGGER.debug("Invalid max start index input. Reset to default value: {}", this.maxStartIndex);
    }
  }

  public void setExpirationIntervalInMinutes(long expirationIntervalInMinutes) {
    cache.setExpirationIntervalInMinutes(expirationIntervalInMinutes);
  }

  public void setExpirationAgeInMinutes(long expirationAgeInMinutes) {
    cache.setExpirationAgeInMinutes(expirationAgeInMinutes);
  }

  public void setCachingEverything(boolean cachingEverything) {
    this.isCachingEverything = cachingEverything;
  }

  public boolean isCacheRemoteIngests() {
    return cacheRemoteIngests;
  }

  public void setCacheRemoteIngests(boolean cacheRemoteIngests) {
    this.cacheRemoteIngests = cacheRemoteIngests;
  }

  public void shutdown() {
    cacheCommitPhaser.shutdown();
    cacheBulkProcessor.shutdown();
  }

  static class OffsetResultHandler implements Runnable {

    private QueryResponseImpl originalResults = null;

    private QueryResponseImpl offsetResultQueue = null;

    private int pageSize = 0;

    private int offset = 1;

    OffsetResultHandler(
        QueryResponseImpl originalResults,
        QueryResponseImpl offsetResultQueue,
        int pageSize,
        int offset) {
      this.originalResults = originalResults;
      this.offsetResultQueue = offsetResultQueue;
      this.pageSize = pageSize;
      this.offset = offset;
    }

    @Override
    public void run() {
      int queryResultIndex = 1;
      int resultsSent = 0;
      Result result;

      while (resultsSent < pageSize
          && originalResults.hasMoreResults()
          && (result = originalResults.take()) != null) {
        if (queryResultIndex >= offset) {
          offsetResultQueue.addResult(result, false);
          resultsSent++;
        }
        queryResultIndex++;
      }

      LOGGER.debug("Closing Queue and setting the total count");
      offsetResultQueue.setHits(originalResults.getHits());
      offsetResultQueue.closeResultQueue();
    }
  }

  private class CallableSourceResponse implements Callable<SourceResponse> {

    private final QueryRequest request;

    private final Source source;

    public CallableSourceResponse(Source source, QueryRequest request) {
      this.source = source;
      this.request = request;
    }

    @Override
    public SourceResponse call() throws Exception {
      QueryRequest queryRequest = getQueryRequest();
      return getSourceResponse(queryRequest);
    }

    @SuppressWarnings("squid:S1181" /*Catching throwable intentionally*/)
    private SourceResponse getSourceResponse(QueryRequest queryRequest)
        throws UnsupportedQueryException {
      final SourceResponse sourceResponse = source.query(queryRequest);
      final SourceResponse clonedSourceResponse = cloneResponse(sourceResponse);

      if (INDEX_QUERY_MODE.equals(request.getPropertyValue(QUERY_MODE))) {
        cacheCommitPhaser.add(clonedSourceResponse.getResults());
      } else if (!NATIVE_QUERY_MODE.equals(request.getPropertyValue(QUERY_MODE))) {
        if (isCachingEverything || UPDATE_QUERY_MODE.equals(request.getPropertyValue(QUERY_MODE))) {
          cacheExecutorService.submit(
              () -> {
                try {
                  cacheBulkProcessor.add(clonedSourceResponse.getResults());
                } catch (VirtualMachineError vme) {
                  throw vme;
                } catch (Throwable throwable) {
                  LOGGER.warn("Unable to add results for bulk processing", throwable);
                }
              });
        }
      }
      return sourceResponse;
    }

    private QueryRequest getQueryRequest() {
      QueryRequest queryRequest;
      if (CACHE_QUERY_MODE.equals(request.getPropertyValue(QUERY_MODE))
          || INDEX_QUERY_MODE.equals(request.getPropertyValue(QUERY_MODE))) {
        queryRequest =
            new QueryRequestImpl(
                request.getQuery(), false, request.getSourceIds(), request.getProperties());
      } else {
        queryRequest = new QueryRequestImpl(request.getQuery(), request.getProperties());
      }
      return queryRequest;
    }

    private SourceResponse cloneResponse(SourceResponse sourceResponse) {
      List<Result> clonedResults =
          sourceResponse
              .getResults()
              .stream()
              .map(Result::getMetacard)
              .map(m -> new MetacardImpl(m, m.getMetacardType()))
              .map(ResultImpl::new)
              .collect(Collectors.toList());

      return new QueryResponseImpl(
          sourceResponse.getRequest(),
          clonedResults,
          true,
          sourceResponse.getHits(),
          sourceResponse.getProperties());
    }
  }

  /** Phaser that forces all added metacards to commit to the cache on phase advance */
  public void setCacheStrategy(String cacheStrategy) {
    cacheBulkProcessor.setCacheStrategy(CacheStrategy.valueOf(cacheStrategy));
  }
}
