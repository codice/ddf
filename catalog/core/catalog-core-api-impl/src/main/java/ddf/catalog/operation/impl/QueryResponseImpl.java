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
package ddf.catalog.operation.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceProcessingDetails;
import ddf.catalog.operation.SourceResponse;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResponseImpl extends ResponseImpl<QueryRequest> implements QueryResponse {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryResponseImpl.class);

  protected static final Result POISON_PILL_RESULT = new POISON_PILL_RESULT();

  protected AtomicLong hits = new AtomicLong(0);

  private Set<ProcessingDetails> details = new HashSet<ProcessingDetails>();

  protected AtomicBoolean isQueueClosed = new AtomicBoolean(false);

  LinkedBlockingQueue<Result> queue = null;

  List<Result> resultList = null;

  private long timeoutMillis = 300000;

  /**
   * Instantiates a new QueryResponseImpl with a $(@link QueryRequest)
   *
   * @param request the request
   */
  public QueryResponseImpl(QueryRequest request) {
    this(request, new HashMap<String, Serializable>());
  }

  /**
   * Instantiates a new QueryResponseImpl with a $(@link QueryRequest) and and a ${@link Map} of
   * properties
   *
   * @param request the request
   * @param properties
   */
  public QueryResponseImpl(QueryRequest request, Map<String, Serializable> properties) {
    this(request, null, false, 0, properties, null);
  }

  /**
   * Instantiates a new QueryResponseImpl with a $(@link QueryRequest) and and a ${@link List} of
   * results
   *
   * @param request the request
   * @param results the results
   */
  public QueryResponseImpl(QueryRequest request, List<Result> results, long totalHits) {
    this(request, results, true, totalHits, null, null);
  }

  /**
   * Instantiates a new QueryResponseImpl with a $(@link QueryRequest), a ${@link List} of results,
   * a closeResultQueue indicator, and a number of hits to return
   *
   * @param request the request
   * @param results the results
   * @param hits the hits
   */
  public QueryResponseImpl(
      QueryRequest request, List<Result> results, boolean closeResultQueue, long hits) {
    this(request, results, closeResultQueue, hits, null, null);
  }

  /**
   * Instantiates a new QueryResponseImpl with a $(@link QueryRequest), a ${@link List} of results,
   * a closeResultQueue indicator, a number of hits to return, and a ${@link Map} of properties
   *
   * @param request the request
   * @param results the results
   * @param hits the hits
   * @param properties the properties
   */
  public QueryResponseImpl(
      QueryRequest request,
      List<Result> results,
      boolean closeResultQueue,
      long hits,
      Map<String, Serializable> properties) {
    this(request, results, closeResultQueue, hits, properties, null);
  }

  /**
   * Instantiates a new {@code QueryResponseImpl} with: a {@link QueryRequest}, a {@link List} of
   * {@link Result}s, the indicator of whether to close the {@link #queue}, the number of {@link
   * Result}s, properties, and a {@link Set} of {@link ProcessingDetails}
   *
   * @param request the {@link QueryRequest} used to elicit this {@code QueryResponseImpl}
   * @param results the {@link List} of {@link Result}s which the execution of the request has
   *     returned
   * @param shouldCloseResultQueue the indicator of whether to close this {@code
   *     QueryResponseImpl}'s {@link #queue} of {@link Result}s or to leave the {@link #queue} open
   *     so that it may continue to add {@link Result}s as they become available
   * @param hits the number of distinct {@link Result}s in the list of results
   * @param properties the {@link Map} from each key of this {@code QueryResponseImpl}'s properties
   *     to its value
   * @param processingDetails the {@link Set} of {@link ProcessingDetails} which the execution of
   *     the request generated
   */
  public QueryResponseImpl(
      QueryRequest request,
      List<Result> results,
      boolean shouldCloseResultQueue,
      long hits,
      Map<String, Serializable> properties,
      Set<ProcessingDetails> processingDetails) {
    super(request, properties);

    if (request != null && request.getQuery() != null) {
      timeoutMillis = request.getQuery().getTimeoutMillis();
    }

    this.hits.set(hits);

    queue = results == null ? new LinkedBlockingQueue<>() : new LinkedBlockingQueue<>(results);

    resultList = new ArrayList<>();

    if (shouldCloseResultQueue) {
      closeResultQueue();
    }

    if (processingDetails != null && !processingDetails.isEmpty()) {
      details.addAll(
          processingDetails.stream().filter(Objects::nonNull).collect(Collectors.toSet()));
    }
  }

  /**
   * Construct from an underlying {@link SourceResponse}
   *
   * @param response
   * @param sourceId
   */
  public QueryResponseImpl(SourceResponse response, String sourceId) {
    this(
        response == null ? null : response.getRequest(),
        response == null ? null : response.getResults(),
        response == null ? -1 : response.getHits());
    Set<? extends SourceProcessingDetails> sourceDetails = null;
    if (response != null) {
      sourceDetails = response.getProcessingDetails();
      this.setProperties(response.getProperties());
    } else {
      setProperties(new HashMap<String, Serializable>());
    }
    // Not every response will contain details
    if (sourceDetails != null) {
      for (SourceProcessingDetails detail : sourceDetails) {
        this.details.add(new ProcessingDetailsImpl(detail, sourceId));
      }
    }
  }

  // /**
  // * Wrap a {@link QueryResponse} and add additional {@link
  // ProcessingDetails}
  // *
  // * @param response
  // * @param exceptions
  // */
  // public QueryResponseImpl(QueryResponse response,
  // Set<ProcessingDetails> exceptions) {
  //
  //
  // }

  @Override
  public long getHits() {
    return hits.get();
  }

  public void setHits(long hits) {
    this.hits.set(hits);
  }

  @Override
  public Set<ProcessingDetails> getProcessingDetails() {
    return details;
  }

  public void setProcessingDetails(Set<ProcessingDetails> details) {
    this.details = details == null ? new HashSet<>() : details;
  }

  @Override
  public List<Result> getResults() {
    Result result = null;

    while (hasMoreResults() && (result = take()) != null) {
      resultList.add(result);
    }

    return resultList;
  }

  /**
   * Adds a ${@link Result} to this QueryResponse, and specifies whether or not to close the queue
   *
   * @param result the result
   * @param closeQueue the indicator for closing of the queue
   */
  public void addResult(Result result, boolean closeQueue) {
    if (result != null) {
      if (isQueueClosed.get()) {
        throw new IllegalStateException("Cannot add new Results after the Queue has been closed");
      } else {
        if (closeQueue) {
          queue.add(result);
          closeResultQueue();
        } else {
          queue.add(result);
        }
      }
    } else {
      throw new IllegalArgumentException("Result cannot be null");
    }
  }

  /**
   * Adds a ${@link List} of ${@link Result}s to this QueryResponse, and specifies whether or not to
   * close the queue
   *
   * @param results the results
   * @param closeQueue the indicator for closing of the queue
   */
  public void addResults(List<Result> results, boolean closeQueue) {
    if (results != null) {
      if (isQueueClosed.get()) {
        throw new IllegalStateException("Cannot add new Results after the Queue has been closed");
      } else {
        if (closeQueue) {
          queue.addAll(results);
          closeResultQueue();
        } else {
          queue.addAll(results);
        }
      }
    }
  }

  @Override
  public boolean hasMoreResults() {
    return !queue.isEmpty() || !isQueueClosed.get();
  }

  public synchronized void closeResultQueue() {
    isQueueClosed.set(true);
    queue.add(POISON_PILL_RESULT);
  }

  @Override
  public Result poll() {
    return hasMoreResults() ? queue.poll() : null;
  }

  @Override
  public Result poll(long timeout) {
    Result result = null;
    if (hasMoreResults()) {
      try {
        result = queue.poll(timeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        LOGGER.debug(
            "QueryResponseImpl queue thread was interrputed.  Returning null for last result");
        Thread.currentThread().interrupt();
      }
    }
    return result;
  }

  @Override
  public Result take() {
    return hasMoreResults() ? handleTake() : null;
  }

  @Override
  public List<Result> take(long size) {
    List<Result> results = new ArrayList<Result>();
    if (hasMoreResults()) {
      Result result = null;
      for (int i = 0; i < size && (result = handleTake()) != null; i++) {
        results.add(result);
      }
    }
    return results;
  }

  /**
   * Returns a result off of the queue
   *
   * @return result the result
   */
  private Result handleTake() {
    Result result = null;
    try {
      result = queue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
      if (POISON_PILL_RESULT.equals(result)) {
        result = null;
      }
    } catch (InterruptedException e) {
      LOGGER.debug(
          "QueryResponseImpl queue thread was interrputed.  Returning null for last result");
      Thread.currentThread().interrupt();
    }
    return result;
  }

  protected static class POISON_PILL_RESULT implements Result {

    @Override
    public Double getRelevanceScore() {
      return null;
    }

    @Override
    public Double getDistanceInMeters() {
      return null;
    }

    @Override
    public Metacard getMetacard() {
      return null;
    }
  }
}
