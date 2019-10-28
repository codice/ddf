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
package ddf.catalog.metrics.source;

import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import io.micrometer.core.instrument.MeterRegistry;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static ddf.catalog.source.SourceMetrics.*;

public class SourceMetricsImpl implements PreFederatedQueryPlugin, PostFederatedQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceMetricsImpl.class);

  private final MeterRegistry meterRegistry;

  public SourceMetricsImpl(MeterRegistryService meterRegistryService) {
    if (meterRegistryService == null) {
      LOGGER.warn("Meter Registry Service is not available");
    }
    meterRegistry = meterRegistryService.getMeterRegistry();
  }

  // PreFederatedQuery
  @Override
  public QueryRequest process(Source source, QueryRequest input)
      throws PluginExecutionException, StopProcessingException {

//    A DistributionSummary can be instead by replacing counter() by summary() and increment() by record. This applies
//    equally to any other metric set in this class and retrieved in SourceMetricImplTest
    meterRegistry.counter(source.getId() + "." + METRICS_PREFIX + "." +
            QUERY_SCOPE + "." + REQUEST_TYPE)
            .increment();
    return input;
  }

  // PostFederatedQuery
  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {

    LOGGER.trace("ENTERING: process (for PostFederatedQueryPlugin)");

    if (null != input) {
      Set<ProcessingDetails> processingDetails = input.getProcessingDetails();
      List<Result> results = input.getResults();

      Iterator<ProcessingDetails> iterator = processingDetails.iterator();
      while (iterator.hasNext()) {
        ProcessingDetails next = iterator.next();
        if (next != null && next.getException() != null) {
          String sourceId = next.getSourceId();
          meterRegistry.counter(sourceId + "." + METRICS_PREFIX + "." + QUERY_SCOPE + "."+ EXCEPTION_TYPE).increment();
        }
      }

      Map<String, Integer> totalHitsPerSource = new HashMap<String, Integer>();
      for (Result result : results) {
        String sourceId = result.getMetacard().getSourceId();
        totalHitsPerSource.put(sourceId, totalHitsPerSource.getOrDefault(sourceId, 0));
      }
      for (Map.Entry<String, Integer> source : totalHitsPerSource.entrySet()) {
        meterRegistry.counter(source.getKey() + "." + METRICS_PREFIX + "." + QUERY_SCOPE + "." + RESPONSE_TYPE).increment();
      }
    }

    LOGGER.trace("EXITING: process (for PostFederatedQueryPlugin)");

    return input;
  }
}
