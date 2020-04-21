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
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;

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

  protected final Counter resourceRetrival;

  private final FilterAdapter filterAdapter;

  public CatalogMetrics(
      @NotNull FilterAdapter filterAdapter, @NotNull MeterRegistryService meterRegistryService) {
    Validate.notNull(filterAdapter, "Argument filterAdapter cannot be null");
    Validate.notNull(meterRegistryService, "Argument meterRegistryService cannot be null");

    this.filterAdapter = filterAdapter;
    meterRegistry = meterRegistryService.getMeterRegistry();

    resultCount = meterRegistry.summary(METRIC_PREFIX + "." + QUERIES_SCOPE + "." + "totalresults");
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
    final String scope = getQueryScope(input);
    QueryTypeFilterDelegate queryType = new QueryTypeFilterDelegate();
    Set<String> sourceIds = input.getSourceIds();
    if (sourceIds != null) {
      try {
        filterAdapter.adapt(input.getQuery(), queryType);
        if (queryType.isComparison()) {
          sourceIds.forEach(sourceId -> pegQueryCounter(sourceId, "comparison", scope));
        }
        if (queryType.isSpatial()) {
          sourceIds.forEach(sourceId -> pegQueryCounter(sourceId, "spatial", scope));
        }
        if (queryType.isFuzzy()) {
          sourceIds.forEach(sourceId -> pegQueryCounter(sourceId, "fuzzy", scope));
        }
        if (queryType.isXpath()) {
          sourceIds.forEach(sourceId -> pegQueryCounter(sourceId, "xpath", scope));
        }
        if (queryType.isTemporal()) {
          sourceIds.forEach(sourceId -> pegQueryCounter(sourceId, "temporal", scope));
        }
        if (queryType.isFunction()) {
          sourceIds.forEach(sourceId -> pegQueryCounter(sourceId, "function", scope));
        }
        if (isNone(queryType)) {
          sourceIds.forEach(sourceId -> pegQueryCounter(sourceId, "none", scope));
        }
      } catch (UnsupportedQueryException e) {
        // ignore filters not supported by the QueryTypeFilterDelegate
      }
    }

    return input;
  }

  private String getQueryScope(QueryRequest input) {
    if (isFederated(input)) {
      return "federated";
    } else {
      return "local";
    }
  }

  private void pegQueryCounter(String sourceId, String queryType, String scope) {
    meterRegistry
        .counter(
            METRIC_PREFIX + "." + QUERIES_SCOPE,
            "type",
            queryType,
            "sourceId",
            sourceId,
            "scope",
            scope)
        .increment();
  }

  private boolean isNone(QueryTypeFilterDelegate queryType) {
    return !(queryType.isComparison()
        || queryType.isSpatial()
        || queryType.isFuzzy()
        || queryType.isXpath()
        || queryType.isTemporal()
        || queryType.isFunction());
  }

  // PostCreate
  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {
    if (Requests.isLocal(input.getRequest())) {
      meterRegistry
          .counter(METRIC_PREFIX + "." + INGEST_SCOPE, "type", "create")
          .increment(input.getCreatedMetacards().size());
    }
    return input;
  }

  // PostUpdate
  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
    if (Requests.isLocal(input.getRequest())) {
      meterRegistry
          .counter(METRIC_PREFIX + "." + INGEST_SCOPE, "type", "update")
          .increment(input.getUpdatedMetacards().size());
    }
    return input;
  }

  // PostDelete
  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
    if (Requests.isLocal(input.getRequest())) {
      meterRegistry
          .counter(METRIC_PREFIX + "." + INGEST_SCOPE, "type", "delete")
          .increment(input.getDeletedMetacards().size());
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

    if (processingDetails == null) {
      return;
    }

    for (ProcessingDetails next : processingDetails) {
      if (next != null && next.getException() != null) {
        if (next.getException() instanceof UnsupportedQueryException) {
          meterRegistry
              .counter(METRIC_PREFIX + "." + EXCEPTIONS_SCOPE, "type", "unsupportedquery")
              .increment();
        } else if (next.getException() instanceof SourceUnavailableException) {
          meterRegistry
              .counter(METRIC_PREFIX + "." + EXCEPTIONS_SCOPE, "type", "sourceunavailable")
              .increment();
        } else if (next.getException() instanceof FederationException) {
          meterRegistry
              .counter(METRIC_PREFIX + "." + EXCEPTIONS_SCOPE, "type", "federation")
              .increment();
        } else {
          meterRegistry
              .counter(METRIC_PREFIX + "." + EXCEPTIONS_SCOPE, "type", "unknown")
              .increment();
        }
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
