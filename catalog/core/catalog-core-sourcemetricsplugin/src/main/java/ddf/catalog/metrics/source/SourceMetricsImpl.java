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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceMetricsImpl implements PreFederatedQueryPlugin, PostFederatedQueryPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceMetricsImpl.class);

  static final String METRICS_SOURCE_ELAPSED_PREFIX = "metrics.source.elapsed.";

  static final String METRICS_PREFIX = "ddf.catalog.source";

  static final String QUERY_SCOPE = "query";

  static final String REQUEST_TYPE = "request";

  static final String RESULTS_TYPE = "results";

  static final String EXCEPTION_TYPE = "exception";

  static final String LATENCY_TYPE = "latency";

  private static final String SOURCE_TAG = "source";

  private static final Map<String, DistributionSummary> LATENCY_SUMMARIES =
      new ConcurrentHashMap<>();

  // PreFederatedQuery
  @Override
  public QueryRequest process(Source source, QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    Metrics.counter(
            METRICS_PREFIX + "." + QUERY_SCOPE + "." + REQUEST_TYPE,
            Tags.of(SOURCE_TAG, source.getId()))
        .increment();
    return input;
  }

  // PostFederatedQuery
  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {

    if (input == null) {
      LOGGER.debug("Unable to process source metrics due to null input.");
      return null;
    }

    Set<ProcessingDetails> processingDetails = input.getProcessingDetails();
    List<Result> results = input.getResults();
    Map<String, Serializable> properties = input.getProperties();

    processingDetails.stream()
        .filter(ProcessingDetails::hasException)
        .map(ProcessingDetails::getSourceId)
        .forEach(
            id ->
                Metrics.counter(
                        METRICS_PREFIX + "." + QUERY_SCOPE + "." + EXCEPTION_TYPE,
                        Tags.of(SOURCE_TAG, id))
                    .increment());

    results.stream()
        .map(Result::getMetacard)
        .map(Metacard::getSourceId)
        .forEach(
            id ->
                Metrics.counter(
                        METRICS_PREFIX + "." + QUERY_SCOPE + "." + RESULTS_TYPE,
                        Tags.of(SOURCE_TAG, id))
                    .increment());

    properties.entrySet().stream()
        .filter(e -> e.getKey() != null && e.getKey().startsWith(METRICS_SOURCE_ELAPSED_PREFIX))
        .forEach(SourceMetricsImpl::updateLatencyMetric);

    return input;
  }

  private static void updateLatencyMetric(Map.Entry<String, Serializable> property) {
    String key = property.getKey();
    String source = key.substring(METRICS_SOURCE_ELAPSED_PREFIX.length());
    DistributionSummary latency =
        LATENCY_SUMMARIES.computeIfAbsent(
            source,
            src ->
                DistributionSummary.builder(METRICS_PREFIX + "." + LATENCY_TYPE)
                    .description("Latency of catalog source requests.")
                    .tag(SOURCE_TAG, src)
                    .baseUnit("milliseconds")
                    .publishPercentiles(0.5, 0.95)
                    .register(Metrics.globalRegistry));
    latency.record((int) property.getValue());
  }
}
