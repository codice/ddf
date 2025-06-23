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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
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
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.codice.ddf.configuration.SystemInfo;
import org.geotools.api.filter.Filter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CatalogMetricsTest {

  private static FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();

  private static FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  private MeterRegistry meterRegistry;

  private static Filter idFilter =
      filterBuilder.attribute(Metacard.ID).is().equalTo().text("metacardId");

  private CatalogMetrics catalogMetrics;

  @Rule public ExpectedException exception = ExpectedException.none();

  @Before
  public void setup() {
    meterRegistry = new SimpleMeterRegistry();
    Metrics.addRegistry(meterRegistry);
    catalogMetrics = new CatalogMetrics(filterAdapter);
    System.setProperty(SystemInfo.SITE_NAME, "testSite");
  }

  @Test
  public void testNullFilterAdapter() {
    exception.expect(NullPointerException.class);
    exception.expectMessage("filterAdapter");
    new CatalogMetrics(null);
  }

  @Test
  public void testSourceComparisonQuery() throws Exception {
    Iterable<Tag> tags = Tags.of("source", "testSite");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(idFilter), Collections.singleton("testSite"));
    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.query.comparison", tags).count(), is(1.0));
  }

  @Test
  public void testSpatialQuery() throws Exception {
    Iterable<Tag> tags = Tags.of("source", "testSite");
    Filter geoFilter =
        filterBuilder.attribute(Metacard.ANY_GEO).within().wkt("POLYGON ((1 1,2 1,2 2,1 2,1 1))");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(geoFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.query.spatial", tags).count(), is(1.0));
  }

  @Test
  public void testFuzzyQuery() throws Exception {
    Iterable<Tag> tags = Tags.of("source", "testSite");
    Filter fuzzyFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like().fuzzyText("fuzzy");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(fuzzyFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.query.fuzzy", tags).count(), is(1.0));
  }

  @Test
  public void testTemporalQuery() throws Exception {
    Iterable<Tag> tags = Tags.of("source", "testSite");
    Filter temporalFilter = filterBuilder.attribute(Metacard.ANY_DATE).before().date(new Date());
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(temporalFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.query.temporal", tags).count(), is(1.0));
  }

  @Test
  public void testFunctionQuery() throws Exception {
    Iterable<Tag> tags = Tags.of("source", "testSite");
    Filter functionFilter =
        filterBuilder
            .function("proximity")
            .attributeArg(Core.TITLE)
            .numberArg(1)
            .textArg("Mary little")
            .equalTo()
            .bool(true);
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(functionFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.query.function", tags).count(), is(1.0));
  }

  @Test
  public void testUnsupportedQuery() throws Exception {
    FilterAdapter adapter = mock(FilterAdapter.class);
    when(adapter.adapt(any(), any()))
        .thenThrow(new UnsupportedQueryException("Unsupported test query"));
    CatalogMetrics catalogMetrics = new CatalogMetrics(adapter);

    Iterable<Tag> tags = Tags.of("source", "testSite");
    Filter unsupportedFilter = filterBuilder.attribute("unsupported").is().text("unsupported");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(unsupportedFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.query.unsupported", tags).count(), is(1.0));
  }

  @Test
  public void catalogHitsCountMetric() throws Exception {
    QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter));
    QueryResponse response = new QueryResponseImpl(query, new ArrayList(), 50);

    catalogMetrics.process(response);

    assertThat(meterRegistry.summary("ddf.catalog.query.hits").count(), is(1L));
    assertThat(meterRegistry.summary("ddf.catalog.query.hits").mean(), is(50.0));
  }

  @Test
  public void catalogQueryExceptionMetric() throws Exception {
    Iterable<Tag> unsupportedQueryExceptionTags =
        Tags.of("type", UnsupportedQueryException.class.getName(), "source", "source1");

    QueryResponse response =
        new QueryResponseImpl(
            new QueryRequestImpl(new QueryImpl(idFilter)),
            Collections.emptyList(),
            true,
            0,
            Collections.emptyMap(),
            Collections.singleton(
                new ProcessingDetailsImpl(
                    "source1", new ExecutionException(new UnsupportedQueryException()))));

    catalogMetrics.process(response);

    assertThat(
        meterRegistry
            .counter("ddf.catalog.query.exceptions", unsupportedQueryExceptionTags)
            .count(),
        is(1.0));
  }

  @Test
  public void catalogCreateExceptionMetric() throws Exception {
    Iterable<Tag> createExceptionTags =
        Tags.of("type", IngestException.class.getName(), "source", "source2");
    CreateRequest request = mock(CreateRequest.class);
    CreateResponse response = mock(CreateResponse.class);
    mockCatalogResponseExceptions(
        request, response, new ProcessingDetailsImpl("source2", new IngestException()));

    catalogMetrics.process(response);

    assertThat(
        meterRegistry.counter("ddf.catalog.create.exceptions", createExceptionTags).count(),
        is(1.0));
  }

  @Test
  public void catalogUpdateExceptionMetric() throws Exception {
    Iterable<Tag> updateExceptionTags =
        Tags.of("type", IngestException.class.getName(), "source", "source3");

    UpdateRequest request = mock(UpdateRequest.class);
    UpdateResponse response = mock(UpdateResponse.class);
    mockCatalogResponseExceptions(
        request, response, new ProcessingDetailsImpl("source3", new IngestException()));

    catalogMetrics.process(response);

    assertThat(
        meterRegistry.counter("ddf.catalog.update.exceptions", updateExceptionTags).count(),
        is(1.0));
  }

  @Test
  public void catalogDeleteExceptionMetric() throws Exception {
    Iterable<Tag> deleteExceptionTags =
        Tags.of("type", Exception.class.getName(), "source", "source4");

    DeleteRequest request = mock(DeleteRequest.class);
    DeleteResponse response = mock(DeleteResponse.class);
    mockCatalogResponseExceptions(
        request, response, new ProcessingDetailsImpl("source4", new Exception()));

    catalogMetrics.process(response);

    assertThat(
        meterRegistry.counter("ddf.catalog.delete.exceptions", deleteExceptionTags).count(),
        is(1.0));
  }

  @Test
  public void catalogResourceExceptionMetric() throws Exception {
    Iterable<Tag> ioExceptionTags =
        Tags.of("type", IOException.class.getName(), "source", "source5");

    ResourceRequest request = mock(ResourceRequest.class);
    ResourceResponse response = mock(ResourceResponse.class);
    mockCatalogResponseExceptions(
        request, response, new ProcessingDetailsImpl("source5", new IOException()));

    catalogMetrics.process(response);

    assertThat(
        meterRegistry.counter("ddf.catalog.resource.exceptions", ioExceptionTags).count(), is(1.0));
  }

  private void mockCatalogResponseExceptions(
      Request request, Response response, ProcessingDetails details) {
    when(response.getRequest()).thenReturn(request);
    when(request.getProperties()).thenReturn(Collections.emptyMap());
    when(response.getProcessingErrors()).thenReturn(Collections.singleton(details));
  }

  @Test
  public void catalogCreateMetric() throws Exception {
    CreateRequest request = mock(CreateRequest.class);
    CreateResponse response = mock(CreateResponse.class);
    List<Metacard> createdList = mock(List.class);
    when(createdList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getCreatedMetacards()).thenReturn(createdList);

    catalogMetrics.process(response);

    assertThat(meterRegistry.counter("ddf.catalog.create").count(), is(100.0));
  }

  @Test
  public void catalogUpdateMetric() throws Exception {
    UpdateRequest request = mock(UpdateRequest.class);
    UpdateResponse response = mock(UpdateResponse.class);
    List<Update> updatedList = mock(List.class);
    when(updatedList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getUpdatedMetacards()).thenReturn(updatedList);

    catalogMetrics.process(response);

    assertThat(meterRegistry.counter("ddf.catalog.update").count(), is(100.0));
  }

  @Test
  public void catalogDeleteMetric() throws Exception {
    DeleteRequest request = mock(DeleteRequest.class);
    DeleteResponse response = mock(DeleteResponse.class);
    List<Metacard> deletedList = mock(List.class);
    when(deletedList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getDeletedMetacards()).thenReturn(deletedList);

    catalogMetrics.process(response);

    assertThat(meterRegistry.counter("ddf.catalog.delete").count(), is(100.0));
  }

  @Test
  public void catalogPreQueryLatencyProperty() throws Exception {
    Map<String, Serializable> props = new HashMap<>();
    props.put("metrics.catalog.operation.start", System.currentTimeMillis() - 1000);
    QueryRequest request = new QueryRequestImpl(new QueryImpl(idFilter), props);

    QueryRequest output = catalogMetrics.process(request);

    assertThat(
        (Long) output.getPropertyValue("metrics.catalog.operation.start"),
        lessThanOrEqualTo(System.currentTimeMillis()));
  }

  @Test
  public void catalogPostQueryLatencyMetric() throws Exception {
    Iterable<Tag> tags = Tags.of("successful", "true");
    QueryRequest request = mock(QueryRequest.class);
    QueryResponse response = mock(QueryResponse.class);
    when(response.getRequest()).thenReturn(request);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    catalogMetrics.process(response);

    assertThat(meterRegistry.summary("ddf.catalog.query.latency", tags).count(), is(1L));
    assertThat(
        meterRegistry.summary("ddf.catalog.query.latency", tags).max(),
        greaterThanOrEqualTo(1000.0));
  }

  @Test
  public void catalogPreCreateLatencyProperty() throws Exception {
    CreateRequest request = mock(CreateRequest.class);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    CreateRequest output = catalogMetrics.process(request);

    assertThat(
        (Long) output.getPropertyValue("metrics.catalog.operation.start"),
        lessThanOrEqualTo(System.currentTimeMillis()));
  }

  @Test
  public void catalogPostCreateLatencyMetric() throws Exception {
    Iterable<Tag> tags = Tags.of("successful", "true");
    CreateRequest request = mock(CreateRequest.class);
    CreateResponse response = mock(CreateResponse.class);
    when(response.getRequest()).thenReturn(request);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    catalogMetrics.process(response);

    assertThat(meterRegistry.summary("ddf.catalog.create.latency", tags).count(), is(1L));
    assertThat(
        meterRegistry.summary("ddf.catalog.create.latency", tags).max(),
        greaterThanOrEqualTo(1000.0));
  }

  @Test
  public void catalogPreUpdateLatencyProperty() throws Exception {
    UpdateRequest request = mock(UpdateRequest.class);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    UpdateRequest output = catalogMetrics.process(request);

    assertThat(
        (Long) output.getPropertyValue("metrics.catalog.operation.start"),
        lessThanOrEqualTo(System.currentTimeMillis()));
  }

  @Test
  public void catalogPostUpdateLatencyMetric() throws Exception {
    Iterable<Tag> tags = Tags.of("successful", "true");
    UpdateRequest request = mock(UpdateRequest.class);
    UpdateResponse response = mock(UpdateResponse.class);
    when(response.getRequest()).thenReturn(request);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    catalogMetrics.process(response);

    assertThat(meterRegistry.summary("ddf.catalog.update.latency", tags).count(), is(1L));
    assertThat(
        meterRegistry.summary("ddf.catalog.update.latency", tags).max(),
        greaterThanOrEqualTo(1000.0));
  }

  @Test
  public void catalogPreDeleteLatencyProperty() throws Exception {
    DeleteRequest request = mock(DeleteRequest.class);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    DeleteRequest output = catalogMetrics.process(request);

    assertThat(
        (Long) output.getPropertyValue("metrics.catalog.operation.start"),
        lessThanOrEqualTo(System.currentTimeMillis()));
  }

  @Test
  public void catalogPostDeleteLatencyMetric() throws Exception {
    Iterable<Tag> tags = Tags.of("successful", "true");
    DeleteRequest request = mock(DeleteRequest.class);
    DeleteResponse response = mock(DeleteResponse.class);
    when(response.getRequest()).thenReturn(request);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    catalogMetrics.process(response);

    assertThat(meterRegistry.summary("ddf.catalog.delete.latency", tags).count(), is(1L));
    assertThat(
        meterRegistry.summary("ddf.catalog.delete.latency", tags).max(),
        greaterThanOrEqualTo(1000.0));
  }

  @Test
  public void catalogPreResourceLatencyProperty() throws Exception {
    ResourceRequest request = mock(ResourceRequest.class);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    ResourceRequest output = catalogMetrics.process(request);

    assertThat(
        (Long) output.getPropertyValue("metrics.catalog.operation.start"),
        lessThanOrEqualTo(System.currentTimeMillis()));
  }

  @Test
  public void catalogPostResourceLatencyMetric() throws Exception {
    Iterable<Tag> tags = Tags.of("successful", "true");
    ResourceRequest request = mock(ResourceRequest.class);
    ResourceResponse response = mock(ResourceResponse.class);
    when(response.getRequest()).thenReturn(request);
    when(request.getPropertyValue("metrics.catalog.operation.start"))
        .thenReturn(System.currentTimeMillis() - 1000);

    catalogMetrics.process(response);

    assertThat(meterRegistry.summary("ddf.catalog.resource.latency", tags).count(), is(1L));
    assertThat(
        meterRegistry.summary("ddf.catalog.resource.latency", tags).max(),
        greaterThanOrEqualTo(1000.0));
  }
}
