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
package ddf.catalog.federation.impl;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import ddf.catalog.util.impl.RelevanceResultComparator;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import org.apache.commons.lang3.Validate;
import org.apache.shiro.util.ThreadContext;
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
public class SortedFederationStrategy implements FederationStrategy {

  /**
   * The default comparator for sorting by {@link ddf.catalog.data.Result#RELEVANCE}, {@link
   * org.opengis.filter.sort.SortOrder#DESCENDING}
   */
  protected static final Comparator<Result> DEFAULT_COMPARATOR =
      new RelevanceResultComparator(SortOrder.DESCENDING);

  /** package-private to allow for unit testing */
  static final int DEFAULT_MAX_START_INDEX = 50000;

  private static final Logger LOGGER = LoggerFactory.getLogger(SortedFederationStrategy.class);

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

  private final SortedQueryMonitorFactory sortedQueryMonitorFactory;

  private final ExecutorService queryExecutorService;

  private int maxStartIndex;

  /**
   * Instantiates an {@code AbstractFederationStrategy} with the provided {@link ExecutorService}.
   *
   * @param queryExecutorService the {@link ExecutorService} for queries
   */
  public SortedFederationStrategy(
      ExecutorService queryExecutorService,
      List<PreFederatedQueryPlugin> preQuery,
      List<PostFederatedQueryPlugin> postQuery) {

    this(queryExecutorService, preQuery, postQuery, new SortedQueryMonitorFactory());
  }

  @VisibleForTesting
  public SortedFederationStrategy(
      ExecutorService queryExecutorService,
      List<PreFederatedQueryPlugin> preQuery,
      List<PostFederatedQueryPlugin> postQuery,
      SortedQueryMonitorFactory sortedQueryMonitorFactory) {

    Validate.notNull(queryExecutorService, "Valid queryExecutorService required.");
    Validate.notNull(preQuery, "Valid List<PreFederatedQueryPlugin> required.");
    Validate.noNullElements(preQuery, "preQuery cannot contain null elements.");
    Validate.notNull(postQuery, "Valid List<PostFederatedQueryPlugin> required.");
    Validate.noNullElements(postQuery, "postQuery cannot contain null elements.");
    Validate.notNull(sortedQueryMonitorFactory, "Valid SortedQueryMonitorFactory required.");

    this.queryExecutorService = queryExecutorService;
    this.preQuery = preQuery;
    this.postQuery = postQuery;
    this.maxStartIndex = DEFAULT_MAX_START_INDEX;
    this.sortedQueryMonitorFactory = sortedQueryMonitorFactory;
  }

  @Override
  public QueryResponse federate(List<Source> sources, QueryRequest queryRequest) {
    Validate.noNullElements(sources, "Cannot federate with null sources.");
    Validate.notNull(queryRequest, "Cannot federate with null QueryRequest.");

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

    final Map<String, Serializable> properties = Collections.synchronizedMap(new HashMap<>());
    final QueryResponseImpl queryResponseQueue = new QueryResponseImpl(queryRequest, properties);

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

        QueryRequest finalSourceQueryRequest = sourceQueryRequest;
        Map<Object, Object> originalThreadResources = ThreadContext.getResources();
        futures.put(
            queryCompletion.submit(
                () ->
                    new TimedSource(source, originalThreadResources)
                        .query(finalSourceQueryRequest)),
            sourceQueryRequest);
      }
    }

    QueryResponseImpl offsetResults = null;
    // If there are offsets and more than one source, we have to get all the
    // results back and then
    // transfer them into a different Queue. That is what the
    // OffsetResultHandler does.
    if (offset > 1 && sources.size() > 1) {
      offsetResults = new QueryResponseImpl(queryRequest, properties);
      queryExecutorService.submit(
          new QueryResponseRunnableMonitor(
              new OffsetResultHandler(queryResponseQueue, offsetResults, pageSize, offset),
              offsetResults));
    }

    queryExecutorService.submit(
        new QueryResponseRunnableMonitor(
            sortedQueryMonitorFactory.createMonitor(
                queryCompletion, futures, queryResponseQueue, modifiedQueryRequest, postQuery),
            queryResponseQueue));

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

      /*
       Federated sources always query from offset of 1. When all query results are received from
       all federated sources and merged together - then the offset is applied.
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

  /**
   * Logs unhandled Throwable, adds processing details, and closes the result queue when Errors
   * (e.g. NoClassDefFoundError) and RuntimeExceptions are thrown from the wrapped runnable.
   */
  static class QueryResponseRunnableMonitor implements Runnable {

    final Runnable wrapped;

    final QueryResponseImpl queryResponse;

    QueryResponseRunnableMonitor(Runnable runnable, QueryResponseImpl queryResponse) {
      wrapped = runnable;
      this.queryResponse = queryResponse;
    }

    @Override
    public void run() {
      try {
        wrapped.run();
      } catch (Throwable t) {
        LOGGER.debug("Unhandled exception while watching query response runnable.", t);
        queryResponse
            .getProcessingDetails()
            .add(
                new ProcessingDetailsImpl(
                    "unknown", new FederationException("Failed to apply federation strategy.", t)));
        queryResponse.closeResultQueue();
        throw t;
      }
    }
  }
}
