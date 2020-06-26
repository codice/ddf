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

import static ddf.catalog.metrics.source.SourceMetricsImpl.EXCEPTION_TYPE;
import static ddf.catalog.metrics.source.SourceMetricsImpl.LATENCY_TYPE;
import static ddf.catalog.metrics.source.SourceMetricsImpl.METRICS_PREFIX;
import static ddf.catalog.metrics.source.SourceMetricsImpl.METRICS_SOURCE_ELAPSED_PREFIX;
import static ddf.catalog.metrics.source.SourceMetricsImpl.QUERY_SCOPE;
import static ddf.catalog.metrics.source.SourceMetricsImpl.REQUEST_TYPE;
import static ddf.catalog.metrics.source.SourceMetricsImpl.RESULTS_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.Source;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;

public class SourceMetricsImplTest {

  private SourceMetricsImpl sourceMetricsImpl;

  private MeterRegistry meterRegistry;

  @Before
  public void init() {
    meterRegistry = new SimpleMeterRegistry();
    Metrics.addRegistry(meterRegistry);
    sourceMetricsImpl = new SourceMetricsImpl();
  }

  @Test
  public void testNullInput() throws PluginExecutionException, StopProcessingException {
    QueryResponse output = sourceMetricsImpl.process(null);
    assertThat(output, is(nullValue(QueryResponse.class)));
  }

  @Test
  public void testRequestCounterForQueryRequest()
      throws PluginExecutionException, StopProcessingException {
    Source source = mock(Source.class);
    when(source.getId()).thenReturn("testSource");
    QueryRequest queryRequest = mock(QueryRequest.class);
    sourceMetricsImpl.process(source, queryRequest);
    String suffix = METRICS_PREFIX + "." + QUERY_SCOPE + "." + REQUEST_TYPE;
    assertThat(meterRegistry.counter(suffix, "source", "testSource").count(), is(1.0));
  }

  @Test
  public void testExceptionCounterForQueryResponse()
      throws PluginExecutionException, StopProcessingException {
    QueryResponse queryResponse = mock(QueryResponse.class);
    Set<ProcessingDetails> processingDetails =
        Stream.of(new ProcessingDetailsImpl("testSource", new Exception()))
            .collect(Collectors.toSet());
    when(queryResponse.getProcessingDetails()).thenReturn(processingDetails);
    sourceMetricsImpl.process(queryResponse);
    String suffix = METRICS_PREFIX + "." + QUERY_SCOPE + "." + EXCEPTION_TYPE;
    assertThat(meterRegistry.counter(suffix, "source", "testSource").count(), is(1.0));
  }

  @Test
  public void testResponseCounterForQueryResponse()
      throws PluginExecutionException, StopProcessingException {
    Metacard metacard = mock(Metacard.class);
    when(metacard.getSourceId()).thenReturn("testSource");
    Result result = mock(Result.class);
    when(result.getMetacard()).thenReturn(metacard);
    List<Result> results = Stream.of(result).collect(Collectors.toList());
    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(results);
    sourceMetricsImpl.process(queryResponse);
    String suffix = METRICS_PREFIX + "." + QUERY_SCOPE + "." + RESULTS_TYPE;
    assertThat(meterRegistry.counter(suffix, "source", "testSource").count(), is(1.0));
  }

  @Test
  public void testLatencyForQueryResponse()
      throws PluginExecutionException, StopProcessingException {
    String src = "testSource";
    QueryResponse queryResponse = mock(QueryResponse.class);
    Map<String, Serializable> properties =
        Collections.singletonMap(METRICS_SOURCE_ELAPSED_PREFIX + src, 100);
    when(queryResponse.getProperties()).thenReturn(properties);
    sourceMetricsImpl.process(queryResponse);
    String suffix = METRICS_PREFIX + "." + LATENCY_TYPE;
    assertThat(meterRegistry.summary(suffix, "source", src).max(), is(100.0));
  }
}
