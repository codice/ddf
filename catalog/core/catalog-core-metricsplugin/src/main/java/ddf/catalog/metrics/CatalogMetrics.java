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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SlidingTimeWindowReservoir;
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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.SystemInfo;

/**
 * Catalog plug-in to capture metrics on catalog operations.
 *
 * @author Phillip Klinefelter
 */
public final class CatalogMetrics
    implements PreQueryPlugin, PostQueryPlugin, PostIngestPlugin, PostResourcePlugin {

  protected static final String EXCEPTIONS_SCOPE = "Exceptions";

  protected static final String QUERIES_SCOPE = "Queries";

  protected static final String INGEST_SCOPE = "Ingest";

  protected static final String RESOURCE_SCOPE = "Resource";

  protected final MetricRegistry metrics = new MetricRegistry();

  protected final JmxReporter reporter =
      JmxReporter.forRegistry(metrics).inDomain("ddf.metrics.catalog").build();

  protected final Histogram resultCount;

  protected final Meter exceptions;

  protected final Meter unsupportedQueryExceptions;

  protected final Meter sourceUnavailableExceptions;

  protected final Meter federationExceptions;

  protected final Meter queries;

  protected final Meter federatedQueries;

  protected final Meter comparisonQueries;

  protected final Meter spatialQueries;

  protected final Meter fuzzyQueries;

  protected final Meter functionQueries;

  protected final Meter temporalQueries;

  protected final Meter createdMetacards;

  protected final Meter updatedMetacards;

  protected final Meter deletedMetacards;

  protected final Meter resourceRetrival;

  private final FilterAdapter filterAdapter;

  public CatalogMetrics(FilterAdapter filterAdapter) {

    this.filterAdapter = filterAdapter;

    resultCount =
        metrics.register(
            MetricRegistry.name(QUERIES_SCOPE, "TotalResults"),
            new Histogram(new SlidingTimeWindowReservoir(1, TimeUnit.MINUTES)));

    queries = metrics.meter(MetricRegistry.name(QUERIES_SCOPE));
    federatedQueries = metrics.meter(MetricRegistry.name(QUERIES_SCOPE, "Federated"));
    comparisonQueries = metrics.meter(MetricRegistry.name(QUERIES_SCOPE, "Comparison"));
    spatialQueries = metrics.meter(MetricRegistry.name(QUERIES_SCOPE, "Spatial"));
    fuzzyQueries = metrics.meter(MetricRegistry.name(QUERIES_SCOPE, "Fuzzy"));
    temporalQueries = metrics.meter(MetricRegistry.name(QUERIES_SCOPE, "Temporal"));
    functionQueries = metrics.meter(MetricRegistry.name(QUERIES_SCOPE, "Function"));

    exceptions = metrics.meter(MetricRegistry.name(EXCEPTIONS_SCOPE));
    unsupportedQueryExceptions =
        metrics.meter(MetricRegistry.name(EXCEPTIONS_SCOPE, "UnsupportedQuery"));
    sourceUnavailableExceptions =
        metrics.meter(MetricRegistry.name(EXCEPTIONS_SCOPE, "SourceUnavailable"));
    federationExceptions = metrics.meter(MetricRegistry.name(EXCEPTIONS_SCOPE, "Federation"));

    createdMetacards = metrics.meter(MetricRegistry.name(INGEST_SCOPE, "Created"));
    updatedMetacards = metrics.meter(MetricRegistry.name(INGEST_SCOPE, "Updated"));
    deletedMetacards = metrics.meter(MetricRegistry.name(INGEST_SCOPE, "Deleted"));

    resourceRetrival = metrics.meter(MetricRegistry.name(RESOURCE_SCOPE));

    reporter.start();
  }

  // PostQuery
  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {
    resultCount.update(input.getHits());
    recordSourceQueryExceptions(input);

    return input;
  }

  // PreQuery
  @Override
  public QueryRequest process(QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    if (isFederated(input)) {
      federatedQueries.mark();
    }
    queries.mark();

    QueryTypeFilterDelegate queryType = new QueryTypeFilterDelegate();
    try {
      filterAdapter.adapt(input.getQuery(), queryType);
      if (queryType.isComparison()) {
        comparisonQueries.mark();
      }
      if (queryType.isSpatial()) {
        spatialQueries.mark();
      }
      if (queryType.isFuzzy()) {
        fuzzyQueries.mark();
      }
      if (queryType.isTemporal()) {
        temporalQueries.mark();
      }
      if (queryType.isFunction()) {
        functionQueries.mark();
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
      createdMetacards.mark(input.getCreatedMetacards().size());
    }
    return input;
  }

  // PostUpdate
  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
    if (Requests.isLocal(input.getRequest())) {
      updatedMetacards.mark(input.getUpdatedMetacards().size());
    }
    return input;
  }

  // PostDelete
  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
    if (Requests.isLocal(input.getRequest())) {
      deletedMetacards.mark(input.getDeletedMetacards().size());
    }
    return input;
  }

  // PostResource
  @Override
  public ResourceResponse process(ResourceResponse input)
      throws PluginExecutionException, StopProcessingException {
    resourceRetrival.mark();
    return input;
  }

  private void recordSourceQueryExceptions(QueryResponse response) {
    Set<ProcessingDetails> processingDetails =
        (Set<ProcessingDetails>) response.getProcessingDetails();

    if (processingDetails == null || processingDetails.iterator() == null) {
      return;
    }

    Iterator<ProcessingDetails> iterator = processingDetails.iterator();
    while (iterator.hasNext()) {
      ProcessingDetails next = iterator.next();
      if (next != null && next.getException() != null) {
        if (next.getException() instanceof UnsupportedQueryException) {
          unsupportedQueryExceptions.mark();
        } else if (next.getException() instanceof SourceUnavailableException) {
          sourceUnavailableExceptions.mark();
        } else if (next.getException() instanceof FederationException) {
          federationExceptions.mark();
        }
        exceptions.mark();
      }
    }

    return;
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
