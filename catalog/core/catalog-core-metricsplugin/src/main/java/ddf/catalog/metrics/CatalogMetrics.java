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

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.Response;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.PreResourcePlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.UnsupportedQueryException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.Validate;
import org.codice.ddf.configuration.SystemInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Catalog plug-in to capture metrics on catalog operations. */
public final class CatalogMetrics
    implements PreQueryPlugin,
        PostQueryPlugin,
        PreIngestPlugin,
        PostIngestPlugin,
        PreResourcePlugin,
        PostResourcePlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogMetrics.class);

  private static final String METRIC_PREFIX = "ddf.catalog";

  private static final String EXCEPTIONS_SCOPE = "exceptions";

  private static final String QUERY_SCOPE = "query";

  private static final String CREATE_SCOPE = "create";

  private static final String UPDATE_SCOPE = "update";

  private static final String DELETE_SCOPE = "delete";

  private static final String RESOURCE_SCOPE = "resource";

  private static final String METRICS_OPERATION_START = "metrics.catalog.operation.start";

  private final FilterAdapter filterAdapter;

  private final DistributionSummary hits;

  private final Counter createdMetacards;

  private final Counter updatedMetacards;

  private final Counter deletedMetacards;

  public CatalogMetrics(FilterAdapter filterAdapter) {
    Validate.notNull(filterAdapter, "Argument filterAdapter cannot be null");

    this.filterAdapter = filterAdapter;

    hits = Metrics.summary(metricName(METRIC_PREFIX, QUERY_SCOPE, "hits"));
    createdMetacards = Metrics.counter(metricName(METRIC_PREFIX, CREATE_SCOPE));
    updatedMetacards = Metrics.counter(metricName(METRIC_PREFIX, UPDATE_SCOPE));
    deletedMetacards = Metrics.counter(metricName(METRIC_PREFIX, DELETE_SCOPE));
  }

  // PreQuery
  @Override
  public QueryRequest process(QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    QueryTypeFilterDelegate queryType = new QueryTypeFilterDelegate();

    Set<String> sourceIds = getSourceIds(input);

    try {
      filterAdapter.adapt(input.getQuery(), queryType);
      if (queryType.isComparison()) {
        sourceIds.forEach(sourceId -> incrementCounter(sourceId, "comparison"));
      }
      if (queryType.isSpatial()) {
        sourceIds.forEach(sourceId -> incrementCounter(sourceId, "spatial"));
      }
      if (queryType.isFuzzy()) {
        sourceIds.forEach(sourceId -> incrementCounter(sourceId, "fuzzy"));
      }
      if (queryType.isTemporal()) {
        sourceIds.forEach(sourceId -> incrementCounter(sourceId, "temporal"));
      }
      if (queryType.isFunction()) {
        sourceIds.forEach(sourceId -> incrementCounter(sourceId, "function"));
      }
      if (isNone(queryType)) {
        sourceIds.forEach(sourceId -> incrementCounter(sourceId, "none"));
      }
    } catch (UnsupportedQueryException e) {
      LOGGER.debug("Unable to detect query type due to unsupported query.", e);
      sourceIds.forEach(sourceId -> incrementCounter(sourceId, "unsupported"));
    }

    addStartTime(input);
    return input;
  }

  // PostQuery
  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {
    recordLatency(input, QUERY_SCOPE);
    hits.record(input.getHits());
    recordExceptions(input.getProcessingDetails(), QUERY_SCOPE);

    return input;
  }

  // PreCreate
  @Override
  public CreateRequest process(CreateRequest input)
      throws PluginExecutionException, StopProcessingException {
    addStartTime(input);
    return input;
  }

  // PostCreate
  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {
    recordLatency(input, CREATE_SCOPE);
    createdMetacards.increment(input.getCreatedMetacards().size());
    recordExceptions(input.getProcessingErrors(), CREATE_SCOPE);

    return input;
  }

  // PreUpdate
  @Override
  public UpdateRequest process(UpdateRequest input)
      throws PluginExecutionException, StopProcessingException {
    addStartTime(input);
    return input;
  }

  // PostUpdate
  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
    recordLatency(input, UPDATE_SCOPE);
    updatedMetacards.increment(input.getUpdatedMetacards().size());
    recordExceptions(input.getProcessingErrors(), UPDATE_SCOPE);

    return input;
  }

  // PreDelete
  @Override
  public DeleteRequest process(DeleteRequest input)
      throws PluginExecutionException, StopProcessingException {
    addStartTime(input);
    return input;
  }

  // PostDelete
  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
    recordLatency(input, DELETE_SCOPE);
    deletedMetacards.increment(input.getDeletedMetacards().size());
    recordExceptions(input.getProcessingErrors(), DELETE_SCOPE);

    return input;
  }

  // PreResource
  @Override
  public ResourceRequest process(ResourceRequest input)
      throws PluginExecutionException, StopProcessingException {
    addStartTime(input);
    return input;
  }

  // PostResource
  @Override
  public ResourceResponse process(ResourceResponse input)
      throws PluginExecutionException, StopProcessingException {
    recordLatency(input, RESOURCE_SCOPE);
    recordExceptions(input.getProcessingErrors(), RESOURCE_SCOPE);

    return input;
  }

  private void recordExceptions(Set<ProcessingDetails> processingDetails, String operation) {
    if (processingDetails == null) {
      return;
    }

    for (ProcessingDetails next : processingDetails) {
      if (next != null && next.getException() != null) {
        String exceptionName = rootCauseExceptionName(next.getException());
        Metrics.counter(
                metricName(METRIC_PREFIX, operation, EXCEPTIONS_SCOPE),
                "type",
                exceptionName,
                "source",
                next.getSourceId())
            .increment();
      }
    }
  }

  private String rootCauseExceptionName(Exception exception) {
    Throwable rootCause = exception;
    while (rootCause.getCause() != null && rootCause.getCause() != rootCause) {
      rootCause = rootCause.getCause();
    }
    return rootCause.getClass().getName();
  }

  private String metricName(String... parts) {
    return String.join(".", parts);
  }

  private Set<String> getSourceIds(QueryRequest query) {
    if (query.isEnterprise()) {
      return Collections.singleton("enterprise");
    }

    if (query.getSourceIds() == null || query.getSourceIds().isEmpty()) {
      return Collections.singleton(SystemInfo.getSiteName());
    }

    return query.getSourceIds();
  }

  private void incrementCounter(String sourceId, String queryType) {
    Metrics.counter(metricName(METRIC_PREFIX, QUERY_SCOPE, queryType), "source", sourceId)
        .increment();
  }

  private boolean isNone(QueryTypeFilterDelegate queryType) {
    return !(queryType.isComparison()
        || queryType.isSpatial()
        || queryType.isFuzzy()
        || queryType.isTemporal()
        || queryType.isFunction());
  }

  private void addStartTime(Request request) {
    request.getProperties().put(METRICS_OPERATION_START, System.currentTimeMillis());
  }

  private void recordLatency(Response<? extends Request> response, String operation) {
    Serializable start = response.getRequest().getPropertyValue(METRICS_OPERATION_START);
    long latency = calculateLatency((Long) start);

    if (latency <= 0) {
      LOGGER.debug("Detected an invalid latency [{}] for given start time [{}]", latency, start);
      return;
    }

    DistributionSummary.builder(metricName(METRIC_PREFIX, operation, "latency"))
        .baseUnit("milliseconds")
        .tags("successful", Boolean.toString(response.getProcessingErrors().isEmpty()))
        .publishPercentiles(0.5, 0.95)
        .register(Metrics.globalRegistry)
        .record(latency);
  }

  private long calculateLatency(Long start) {
    if (start == null) {
      return 0;
    }

    long end = System.currentTimeMillis();
    return end - start;
  }
}
