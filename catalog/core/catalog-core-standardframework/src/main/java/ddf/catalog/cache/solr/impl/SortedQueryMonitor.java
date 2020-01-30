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

import static ddf.catalog.Constants.ADDITIONAL_SORT_BYS;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.Federatable;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceProcessingDetails;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.impl.CollectionResultComparator;
import ddf.catalog.util.impl.DistanceResultComparator;
import ddf.catalog.util.impl.RelevanceResultComparator;
import ddf.catalog.util.impl.TemporalResultComparator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SortedQueryMonitor implements Runnable {
  private static final Logger LOGGER = LoggerFactory.getLogger(SortedQueryMonitor.class);

  private final QueryRequest request;

  private final CompletionService<SourceResponse> completionService;

  private CachingFederationStrategy cachingFederationStrategy;

  private QueryResponseImpl returnResults;

  private Map<Future<SourceResponse>, QueryRequest> futures;

  private List<PostFederatedQueryPlugin> postQuery;

  private Query query;

  private long deadline;

  public SortedQueryMonitor(
      CachingFederationStrategy cachingFederationStrategy,
      CompletionService<SourceResponse> completionService,
      Map<Future<SourceResponse>, QueryRequest> futures,
      QueryResponseImpl returnResults,
      QueryRequest request,
      List<PostFederatedQueryPlugin> postQuery) {
    this.cachingFederationStrategy = cachingFederationStrategy;
    this.completionService = completionService;
    this.returnResults = returnResults;
    this.request = request;
    this.query = request.getQuery();
    this.futures = futures;
    this.postQuery = postQuery;
    deadline = System.currentTimeMillis() + query.getTimeoutMillis();
  }

  @Override
  public void run() {
    List<SortBy> sortBys = new ArrayList<>();
    SortBy sortBy = query.getSortBy();
    if (sortBy != null && sortBy.getPropertyName() != null) {
      sortBys.add(sortBy);
    }
    Serializable sortBySer = request.getPropertyValue(ADDITIONAL_SORT_BYS);
    if (sortBySer instanceof SortBy[]) {
      SortBy[] extSortBys = (SortBy[]) sortBySer;
      if (extSortBys.length > 0) {
        sortBys.addAll(Arrays.asList(extSortBys));
      }
    }

    // Prepare the Comparators that we will use
    CollectionResultComparator resultComparator = new CollectionResultComparator();
    if (!sortBys.isEmpty()) {
      for (SortBy sort : sortBys) {
        Comparator<Result> comparator = null;

        PropertyName sortingProp = sort.getPropertyName();
        String sortType = sortingProp.getPropertyName();
        SortOrder sortOrder =
            (sort.getSortOrder() == null) ? SortOrder.DESCENDING : sort.getSortOrder();
        LOGGER.debug("Sorting type: {}", sortType);
        LOGGER.debug("Sorting order: {}", sortOrder);

        // Temporal searches are currently sorted by the effective time
        if (Metacard.EFFECTIVE.equals(sortType) || Result.TEMPORAL.equals(sortType)) {
          comparator = new TemporalResultComparator(sortOrder);
        } else if (Result.DISTANCE.equals(sortType)) {
          comparator = new DistanceResultComparator(sortOrder);
        } else if (Result.RELEVANCE.equals(sortType)) {
          comparator = new RelevanceResultComparator(sortOrder);
        } else {
          comparator =
              Comparator.comparing(
                  r -> getAttributeValue((Result) r, sortType),
                  ((sortOrder == SortOrder.ASCENDING)
                      ? Comparator.nullsFirst(Comparator.<Comparable>naturalOrder())
                      : Comparator.nullsLast(Comparator.<Comparable>reverseOrder())));
        }
        resultComparator.addComparator(comparator);
      }
    } else {
      Comparator<Result> coreComparator = CachingFederationStrategy.DEFAULT_COMPARATOR;
      resultComparator.addComparator(coreComparator);
    }

    List<Result> resultList = new ArrayList<>();
    long totalHits = 0;
    Set<ProcessingDetails> detailsOfReturnResults = returnResults.getProcessingDetails();

    Map<String, Serializable> returnProperties = returnResults.getProperties();
    HashMap<String, Long> hitsPerSource = new HashMap<>();

    for (int i = futures.size(); i > 0; i--) {
      String sourceId = "Unknown Source";
      QueryRequest queryRequest = null;
      SourceResponse sourceResponse = null;
      try {
        Future<SourceResponse> future;
        if (query.getTimeoutMillis() < 1) {
          future = completionService.take();
        } else {
          future = completionService.poll(getTimeRemaining(deadline), TimeUnit.MILLISECONDS);
          if (future == null) {
            timeoutRemainingSources(detailsOfReturnResults);
            break;
          }
        }

        queryRequest = futures.remove(future);
        if (queryRequest == null) {
          LOGGER.debug("Couldn't get completed federated query. Skipping {}", sourceId);
          continue;
        }
        sourceId = getSourceIdFromRequest(queryRequest);

        sourceResponse = future.get();
        if (sourceResponse == null) {
          LOGGER.debug("Source {} returned null response", sourceId);
          executePostFederationQueryPluginsWithSourceError(
              queryRequest, sourceId, new NullPointerException(), detailsOfReturnResults);
        } else {
          sourceResponse = executePostFederationQueryPlugins(sourceResponse, queryRequest);
          resultList.addAll(sourceResponse.getResults());
          long hits = sourceResponse.getHits();
          totalHits += hits;
          hitsPerSource.merge(sourceId, hits, (l1, l2) -> l1 + l2);

          Map<String, Serializable> properties = sourceResponse.getProperties();
          returnProperties.putAll(properties);

          for (SourceProcessingDetails detailsOfSourceResponse :
              sourceResponse.getProcessingDetails()) {
            detailsOfReturnResults.add(
                new ProcessingDetailsImpl(detailsOfSourceResponse, sourceId));
          }
        }
      } catch (InterruptedException e) {
        if (queryRequest != null) {
          // First, add interrupted processing detail for this source
          LOGGER.debug("Search interrupted for {}", sourceId);
          executePostFederationQueryPluginsWithSourceError(
              queryRequest, sourceId, e, detailsOfReturnResults);
        }

        // Then add the interrupted exception for the remaining sources
        interruptRemainingSources(detailsOfReturnResults, e);
        Thread.currentThread().interrupt();
        break;
      } catch (ExecutionException e) {
        LOGGER.info(
            "Couldn't get results from completed federated query for sourceId = {}", sourceId, e);
        executePostFederationQueryPluginsWithSourceError(
            queryRequest, sourceId, e, detailsOfReturnResults);
      }
    }
    returnProperties.put("hitsPerSource", hitsPerSource);
    LOGGER.debug("All sources finished returning results: {}", resultList.size());

    returnResults.setHits(totalHits);
    if (CachingFederationStrategy.INDEX_QUERY_MODE.equals(
        request.getPropertyValue(CachingFederationStrategy.QUERY_MODE))) {
      QueryResponse result = cachingFederationStrategy.queryCache(request);
      returnResults.addResults(result.getResults(), true);
    } else {
      returnResults.addResults(sortedResults(resultList, resultComparator), true);
    }
  }

  List<Result> sortedResults(List<Result> results, Comparator<? super Result> comparator) {
    Collections.sort(results, comparator);

    int maxResults = Integer.MAX_VALUE;
    if (query.getPageSize() > 0) {
      maxResults = query.getPageSize();
    }

    return results.size() > maxResults ? results.subList(0, maxResults) : results;
  }

  private static Comparable getAttributeValue(Result r, String attributeName) {
    if (r == null) {
      return null;
    }
    final Attribute a = r.getMetacard().getAttribute(attributeName);

    return (a != null && a.getValue() instanceof Comparable) ? (Comparable) a.getValue() : null;
  }

  private void timeoutRemainingSources(Set<ProcessingDetails> processingDetails) {
    for (QueryRequest expiredSource : futures.values()) {
      if (expiredSource != null) {
        String sourceId = getSourceIdFromRequest(expiredSource);
        LOGGER.info("Search timed out for {}", sourceId);
        processingDetails.add(new ProcessingDetailsImpl(sourceId, new TimeoutException()));
      }
    }
  }

  private void interruptRemainingSources(
      Set<ProcessingDetails> processingDetails, InterruptedException interruptedException) {
    for (QueryRequest interruptedSource : futures.values()) {
      if (interruptedSource != null) {
        String sourceId = getSourceIdFromRequest(interruptedSource);
        LOGGER.info("Search interrupted for {}", sourceId);
        processingDetails.add(new ProcessingDetailsImpl(sourceId, interruptedException));
      }
    }
  }

  private long getTimeRemaining(long deadline) {
    long timeLeft;
    if (System.currentTimeMillis() > deadline) {
      timeLeft = 0;
    } else {
      timeLeft = deadline - System.currentTimeMillis();
    }
    return timeLeft;
  }

  private String getSourceIdFromRequest(QueryRequest queryRequest) {
    String unkSource = "Unknown Source";
    if (queryRequest == null) {
      return unkSource;
    }

    return Stream.of(queryRequest)
        .map(Federatable::getSourceIds)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .findFirst()
        .orElse(unkSource);
  }

  private void executePostFederationQueryPluginsWithSourceError(
      QueryRequest queryRequest,
      String sourceId,
      Exception e,
      Set<ProcessingDetails> processingDetails) {

    ProcessingDetails processingDetail = new ProcessingDetailsImpl(sourceId, e);
    SourceResponse sourceResponse = new SourceResponseImpl(queryRequest, new ArrayList<>());
    sourceResponse.getProcessingErrors().add(processingDetail);
    processingDetails.add(processingDetail);
    executePostFederationQueryPlugins(sourceResponse, queryRequest);
  }

  private SourceResponse executePostFederationQueryPlugins(
      SourceResponse sourceResponse, QueryRequest queryRequest) {

    QueryResponse queryResponse =
        new QueryResponseImpl(
            queryRequest,
            sourceResponse.getResults(),
            true,
            sourceResponse.getHits(),
            sourceResponse.getProperties());
    try {
      for (PostFederatedQueryPlugin service : postQuery) {
        try {
          queryResponse = service.process(queryResponse);
        } catch (PluginExecutionException e) {
          LOGGER.info("Error executing PostFederatedQueryPlugin", e);
        }
      }
    } catch (StopProcessingException e) {
      LOGGER.info("Plugin stopped processing", e);
    }
    Set<SourceProcessingDetails> detailsOfResponseAfterPlugins =
        new HashSet<>(queryResponse.getProcessingDetails());
    detailsOfResponseAfterPlugins.addAll(sourceResponse.getProcessingDetails());
    return new SourceResponseImpl(
        queryRequest,
        sourceResponse.getProperties(),
        queryResponse.getResults(),
        queryResponse.getHits(),
        detailsOfResponseAfterPlugins);
  }
}
