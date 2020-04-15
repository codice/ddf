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
package ddf.catalog.metrics;

import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.Requests;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Catalog plug-in to capture metrics on catalog operations.
 *
 * @author Phillip Klinefelter
 */
public final class CatalogMetrics
    implements PreQueryPlugin, PostQueryPlugin, PostIngestPlugin, PostResourcePlugin {

  protected static final String METRIC_PREFIX = "ddf.catalog";

  protected static final String EXCEPTIONS_SCOPE = "exceptions";

  protected static final String QUERIES_SCOPE = "queries";

  protected static final String INGEST_SCOPE = "ingest";

  protected static final String RESOURCE_SCOPE = "resource";

  protected final MeterRegistry meterRegistry;

  protected final DistributionSummary resultCount;

  protected final Counter exceptions;

  protected final Counter unsupportedQueryExceptions;

  protected final Counter sourceUnavailableExceptions;

  protected final Counter federationExceptions;

  protected final Counter queries;

  protected final Counter federatedQueries;

  protected final Counter comparisonQueries;

  protected final Counter spatialQueries;

  protected final Counter xpathQueries;

  protected final Counter fuzzyQueries;

  protected final Counter functionQueries;

  protected final Counter temporalQueries;

  protected final Counter createdMetacards;

  protected final Counter updatedMetacards;

  protected final Counter deletedMetacards;

  protected final Counter resourceRetrival;

  private final FilterAdapter filterAdapter;

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogMetrics.class);

  public CatalogMetrics(FilterAdapter filterAdapter, MeterRegistryService meterRegistryService) {
    this.filterAdapter = filterAdapter;

    if (meterRegistryService == null) {
      LOGGER.warn("Meter Registry Service is not available");
    }

    meterRegistry = meterRegistryService.getMeterRegistry();
    resultCount = meterRegistry.summary(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "totalresults");

    queries = meterRegistry.counter(METRIC_PREFIX + "." + QUERIES_SCOPE);
    federatedQueries =
        meterRegistry.counter(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "federated");
    comparisonQueries =
        meterRegistry.counter(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "comparison");
    spatialQueries = meterRegistry.counter(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "spatial");
    xpathQueries = meterRegistry.counter(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "xpath");
    fuzzyQueries = meterRegistry.counter(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "fuzzy");
    temporalQueries = meterRegistry.counter(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "temporal");
    functionQueries = meterRegistry.counter(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "function");

    exceptions = meterRegistry.counter(METRIC_PREFIX + "." + EXCEPTIONS_SCOPE);
    unsupportedQueryExceptions =
        meterRegistry.counter(METRIC_PREFIX + "." + EXCEPTIONS_SCOPE + "." + "unsupportedquery");
    sourceUnavailableExceptions =
        meterRegistry.counter(METRIC_PREFIX + "." + EXCEPTIONS_SCOPE + "." + "sourceunavailable");
    federationExceptions =
        meterRegistry.counter(METRIC_PREFIX + "." + EXCEPTIONS_SCOPE + "." + "federation");

    createdMetacards = meterRegistry.counter(METRIC_PREFIX + "." + INGEST_SCOPE + "." + "created");
    updatedMetacards = meterRegistry.counter(METRIC_PREFIX + "." + INGEST_SCOPE + "." + "updated");
    deletedMetacards = meterRegistry.counter(METRIC_PREFIX + "." + INGEST_SCOPE + "." + "deleted");

    resourceRetrival = meterRegistry.counter(METRIC_PREFIX + "." + RESOURCE_SCOPE);
  }

  // PostQuery
  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {
    resultCount.record(input.getHits());
    recordSourceQueryExceptions(input);

    return input;
  }

  // PreQuery
  @Override
  public QueryRequest process(QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    if (isFederated(input)) {
      federatedQueries.increment();
    }
    queries.increment();

    QueryTypeFilterDelegate queryType = new QueryTypeFilterDelegate();
    try {
      filterAdapter.adapt(input.getQuery(), queryType);
      if (queryType.isComparison()) {
        comparisonQueries.increment();
      }
      if (queryType.isSpatial()) {
        spatialQueries.increment();
      }
      if (queryType.isFuzzy()) {
        fuzzyQueries.increment();
      }
      if (queryType.isXpath()) {
        xpathQueries.increment();
      }
      if (queryType.isTemporal()) {
        temporalQueries.increment();
      }
      if (queryType.isFunction()) {
        functionQueries.increment();
      }
    } catch (UnsupportedQueryException e) {
      // ignore filters not supported by the QueryTypeFilterDelegate
    }

    return input;
  }

  // PostCreate
  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {
    if (Requests.isLocal(input.getRequest())) {
      createdMetacards.increment(input.getCreatedMetacards().size());
    }
    return input;
  }

  // PostUpdate
  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
    if (Requests.isLocal(input.getRequest())) {
      updatedMetacards.increment(input.getUpdatedMetacards().size());
    }
    return input;
  }

  // PostDelete
  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
    if (Requests.isLocal(input.getRequest())) {
      deletedMetacards.increment(input.getDeletedMetacards().size());
    }
    return input;
  }

  // PostResource
  @Override
  public ResourceResponse process(ResourceResponse input)
      throws PluginExecutionException, StopProcessingException {
    resourceRetrival.increment();
    return input;
  }

  private void recordSourceQueryExceptions(QueryResponse response) {
    Set<ProcessingDetails> processingDetails = response.getProcessingDetails();

    if (processingDetails == null || processingDetails.iterator() == null) {
      return;
    }

    Iterator<ProcessingDetails> iterator = processingDetails.iterator();
    while (iterator.hasNext()) {
      ProcessingDetails next = iterator.next();
      if (next != null && next.getException() != null) {
        if (next.getException() instanceof UnsupportedQueryException) {
          unsupportedQueryExceptions.increment();
        } else if (next.getException() instanceof SourceUnavailableException) {
          sourceUnavailableExceptions.increment();
        } else if (next.getException() instanceof FederationException) {
          federationExceptions.increment();
        }
        exceptions.increment();
      }
    }
  }

  private boolean isFederated(QueryRequest queryRequest) {
    Set<String> sourceIds = queryRequest.getSourceIds();

    if (queryRequest.isEnterprise()) {
      return true;
    } else if (sourceIds == null) {
      return false;
    } else {
      return (sourceIds.size() > 1)
          || (sourceIds.size() == 1
              && sourceIds.stream().noneMatch(StringUtils::isEmpty)
              && !sourceIds.contains(SystemInfo.getSiteName()));
    }
  }
}
