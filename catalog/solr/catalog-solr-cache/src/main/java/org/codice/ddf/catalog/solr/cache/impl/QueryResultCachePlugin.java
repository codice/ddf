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
package org.codice.ddf.catalog.solr.cache.impl;

import ddf.catalog.Constants;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.util.impl.Requests;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryResultCachePlugin implements PostFederatedQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryResultCachePlugin.class);

  private final SolrCacheSource cacheSource;

  private final ExecutorService cacheExecutorService;

  private final CacheBulkProcessor cacheBulkProcessor;

  private boolean isCachingFederatedResponses = true;

  public QueryResultCachePlugin(
      SolrCache solrCache,
      SolrCacheSource solrCacheSource,
      ExecutorService solrCacheExecutorService) {

    Validate.notNull(solrCache, "Valid SolrCache required.");
    Validate.notNull(solrCacheSource, "Valid SolrCacheSource required.");
    Validate.notNull(solrCacheExecutorService, "Valid ExecutorService required.");

    cacheSource = solrCacheSource;
    cacheExecutorService = solrCacheExecutorService;

    cacheBulkProcessor = new CacheBulkProcessor(solrCache);
  }

  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {

    if (!isCachingFederatedResponses) {
      return input;
    }

    if (!"update".equals(input.getRequest().getProperties().get("mode"))) {
      return input;
    }

    if (Requests.isLocal(input.getRequest())) {
      return input;
    }

    if (cacheSource
        .getId()
        .equals(input.getRequest().getProperties().get(Constants.SERVICE_TITLE))) {
      return input;
    }

    LOGGER.debug("Adding {} federated query results to cache.", input.getResults().size());
    addToCache(input);

    return input;
  }

  public void setCachingFederatedResponses(boolean cachingFederatedResponses) {
    this.isCachingFederatedResponses = cachingFederatedResponses;
  }

  public void shutdown() {
    cacheBulkProcessor.shutdown();
  }

  private void addToCache(QueryResponse input) {
    SourceResponse clonedSourceResponse = cloneResponse(input);
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

  private SourceResponse cloneResponse(SourceResponse sourceResponse) {

    List<Result> clonedResults =
        sourceResponse
            .getResults()
            .stream()
            .map(Result::getMetacard)
            .map(m -> new MetacardImpl(m, m.getMetacardType()))
            .map(ResultImpl::new)
            .collect(Collectors.toList());

    Set<ProcessingDetails> processingDetails = new HashSet<>();
    if (clonedResults.size() > 0) {
      String sourceId = clonedResults.get(0).getMetacard().getSourceId();
      processingDetails =
          sourceResponse
              .getProcessingDetails()
              .stream()
              .map(sourceDetails -> new ProcessingDetailsImpl(sourceDetails, sourceId))
              .collect(Collectors.toSet());
    }

    return new QueryResponseImpl(
        sourceResponse.getRequest(),
        clonedResults,
        true,
        sourceResponse.getHits(),
        sourceResponse.getProperties(),
        processingDetails);
  }

  /** Phaser that forces all added metacards to commit to the cache on phase advance */
  public void setCacheStrategy(String cacheStrategy) {
    cacheBulkProcessor.setCacheStrategy(CacheStrategy.valueOf(cacheStrategy));
  }
}
