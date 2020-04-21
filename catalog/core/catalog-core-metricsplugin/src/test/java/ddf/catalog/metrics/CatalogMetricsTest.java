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
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.federation.FederationException;
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
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.opengis.filter.Filter;

public class CatalogMetricsTest {

  private static FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();

  private static FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  private MeterRegistryService meterRegistryService;

  private MeterRegistry meterRegistry;

  private static Filter idFilter =
      filterBuilder.attribute(Metacard.ID).is().equalTo().text("metacardId");

  private CatalogMetrics catalogMetrics;

  @Rule public ExpectedException exception = ExpectedException.none();

  @Before
  public void setup() {
    meterRegistry = new SimpleMeterRegistry();
    meterRegistryService = mock(MeterRegistryService.class);
    when(meterRegistryService.getMeterRegistry()).thenReturn(meterRegistry);
    catalogMetrics = new CatalogMetrics(filterAdapter, meterRegistryService);
    System.setProperty(SystemInfo.SITE_NAME, "testSite");
  }

  @Test
  public void testNullFilterAdapter() {
    exception.expect(NullPointerException.class);
    exception.expectMessage("filterAdapter");
    new CatalogMetrics(null, meterRegistryService);
  }

  @Test
  public void testNullMeterRegistryService() {
    exception.expect(NullPointerException.class);
    exception.expectMessage("meterRegistryService");
    new CatalogMetrics(filterAdapter, null);
  }

  @Test
  public void testLocalComparisonQuery() throws Exception {
    Iterable<Tag> tags = getTags("comparison", "testSite", "local");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(idFilter), Collections.singleton("testSite"));
    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testEnterpriseComparisonQuery() throws Exception {
    Iterable<Tag> tags = getTags("comparison", "testSite", "federated");
    QueryRequest query =
        new QueryRequestImpl(
            new QueryImpl(idFilter),
            true,
            Collections.singleton("testSite"),
            Collections.emptyMap());

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteSourceComparisonQuery() throws Exception {
    Iterable<Tag> tags = getTags("comparison", "remoteSiteId", "federated");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList("remoteSiteId"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteAndLocalSourceComparisonQuery() throws Exception {
    Iterable<Tag> testSiteTags = getTags("comparison", "testSite", "federated");
    Iterable<Tag> remoteSiteTags = getTags("comparison", "remoteSite", "federated");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList("testSite", "remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", testSiteTags).count(), is(1.0));
    assertThat(meterRegistry.counter("ddf.catalog.queries", remoteSiteTags).count(), is(1.0));
  }

  @Test
  public void testEnterpriseSpatialQuery() throws Exception {
    Iterable<Tag> tags = getTags("spatial", "testSite", "local");
    Filter geoFilter =
        filterBuilder.attribute(Metacard.ANY_GEO).within().wkt("POLYGON ((1 1,2 1,2 2,1 2,1 1))");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(geoFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteSourceSpatialQuery() throws Exception {
    Iterable<Tag> tags = getTags("spatial", "remoteSite", "federated");
    Filter geoFilter =
        filterBuilder.attribute(Metacard.ANY_GEO).within().wkt("POLYGON ((1 1,2 1,2 2,1 2,1 1))");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(geoFilter), Collections.singleton("remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteAndLocalSourceSpatialQuery() throws Exception {
    Iterable<Tag> testSiteTags = getTags("spatial", "testSite", "federated");
    Iterable<Tag> remoteSiteTags = getTags("spatial", "remoteSite", "federated");
    Filter geoFilter =
        filterBuilder.attribute(Metacard.ANY_GEO).within().wkt("POLYGON ((1 1,2 1,2 2,1 2,1 1))");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(geoFilter), Arrays.asList("testSite", "remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", testSiteTags).count(), is(1.0));
    assertThat(meterRegistry.counter("ddf.catalog.queries", remoteSiteTags).count(), is(1.0));
  }

  @Test
  public void testEnterpriseFuzzyQuery() throws Exception {
    Iterable<Tag> tags = getTags("fuzzy", "testSite", "local");
    Filter fuzzyFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like().fuzzyText("fuzzy");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(fuzzyFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteSourceFuzzyQuery() throws Exception {
    Iterable<Tag> tags = getTags("fuzzy", "remoteSite", "federated");
    Filter fuzzyFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like().fuzzyText("fuzzy");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(fuzzyFilter), Collections.singleton("remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteAndLocalSourceFuzzyQuery() throws Exception {
    Iterable<Tag> testSiteTags = getTags("fuzzy", "testSite", "federated");
    Iterable<Tag> remoteSiteTags = getTags("fuzzy", "remoteSite", "federated");
    Filter fuzzyFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like().fuzzyText("fuzzy");
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(fuzzyFilter), Arrays.asList("testSite", "remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", testSiteTags).count(), is(1.0));
    assertThat(meterRegistry.counter("ddf.catalog.queries", remoteSiteTags).count(), is(1.0));
  }

  @Test
  public void testEnterpriseXpathQuery() throws Exception {
    Iterable<Tag> tags = getTags("xpath", "testSite", "local");
    Filter xpathFilter = filterBuilder.xpath("//node").exists();
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(xpathFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteSourceXpathQuery() throws Exception {
    Iterable<Tag> tags = getTags("xpath", "remoteSite", "federated");
    Filter xpathFilter = filterBuilder.xpath("//node").exists();
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(xpathFilter), Collections.singleton("remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteAndLocalSourceXpathQuery() throws Exception {
    Iterable<Tag> testSiteTags = getTags("xpath", "testSite", "federated");
    Iterable<Tag> remoteSiteTags = getTags("xpath", "remoteSite", "federated");
    Filter xpathFilter = filterBuilder.xpath("//node").exists();
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(xpathFilter), Arrays.asList("testSite", "remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", testSiteTags).count(), is(1.0));
    assertThat(meterRegistry.counter("ddf.catalog.queries", remoteSiteTags).count(), is(1.0));
  }

  @Test
  public void testEnterpriseTemporalQuery() throws Exception {
    Iterable<Tag> tags = getTags("temporal", "testSite", "local");
    Filter temporalFilter = filterBuilder.attribute(Metacard.ANY_DATE).before().date(new Date());
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(temporalFilter), Collections.singleton("testSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteSourceTemporalQuery() throws Exception {
    Iterable<Tag> tags = getTags("temporal", "remoteSite", "federated");
    Filter temporalFilter = filterBuilder.attribute(Metacard.ANY_DATE).before().date(new Date());
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(temporalFilter), Collections.singleton("remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteAndLocalSourceTemporalQuery() throws Exception {
    Iterable<Tag> testSiteTags = getTags("temporal", "testSite", "federated");
    Iterable<Tag> remoteSiteTags = getTags("temporal", "remoteSite", "federated");
    Filter temporalFilter = filterBuilder.attribute(Metacard.ANY_DATE).before().date(new Date());
    QueryRequest query =
        new QueryRequestImpl(
            new QueryImpl(temporalFilter), Arrays.asList("testSite", "remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", testSiteTags).count(), is(1.0));
    assertThat(meterRegistry.counter("ddf.catalog.queries", remoteSiteTags).count(), is(1.0));
  }

  @Test
  public void testEnterpriseFunctionQuery() throws Exception {
    Iterable<Tag> tags = getTags("function", "testSite", "local");
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

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteSourceFunctionQuery() throws Exception {
    Iterable<Tag> tags = getTags("function", "remoteSite", "federated");
    Filter functionFilter =
        filterBuilder
            .function("proximity")
            .attributeArg(Core.TITLE)
            .numberArg(1)
            .textArg("Mary little")
            .equalTo()
            .bool(true);
    QueryRequest query =
        new QueryRequestImpl(new QueryImpl(functionFilter), Collections.singleton("remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", tags).count(), is(1.0));
  }

  @Test
  public void testRemoteAndLocalSourceFunctionQuery() throws Exception {
    Iterable<Tag> testSiteTags = getTags("function", "testSite", "federated");
    Iterable<Tag> remoteSiteTags = getTags("function", "remoteSite", "federated");
    Filter functionFilter =
        filterBuilder
            .function("proximity")
            .attributeArg(Core.TITLE)
            .numberArg(1)
            .textArg("Mary little")
            .equalTo()
            .bool(true);
    QueryRequest query =
        new QueryRequestImpl(
            new QueryImpl(functionFilter), Arrays.asList("testSite", "remoteSite"));

    catalogMetrics.process(query);

    assertThat(meterRegistry.counter("ddf.catalog.queries", testSiteTags).count(), is(1.0));
    assertThat(meterRegistry.counter("ddf.catalog.queries", remoteSiteTags).count(), is(1.0));
  }

  @Test
  public void catalogResultCountMetric() throws Exception {
    QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter));
    QueryResponse response = new QueryResponseImpl(query, new ArrayList(), 50);

    catalogMetrics.process(response);

    assertThat(catalogMetrics.resultCount.count(), is(1L));
    assertThat(catalogMetrics.resultCount.mean(), is(50.0));
  }

  @Test
  public void catalogExceptionMetric() throws Exception {
    Iterable<Tag> unsupportedQueryExceptionTags = Tags.of("type", "unsupportedquery");
    Iterable<Tag> sourceUnavailableExceptionTags = Tags.of("type", "unsupportedquery");
    Iterable<Tag> federationExceptionTags = Tags.of("type", "unsupportedquery");
    Iterable<Tag> unknownExceptionTags = Tags.of("type", "unsupportedquery");
    QueryResponse response = new QueryResponseImpl(new QueryRequestImpl(new QueryImpl(idFilter)));
    Set<ProcessingDetails> details = response.getProcessingDetails();

    details.addAll(
        new HashSet<ProcessingDetails>() {
          {
            add(new ProcessingDetailsImpl("source1", new UnsupportedQueryException()));
            add(new ProcessingDetailsImpl("source2", new SourceUnavailableException()));
            add(new ProcessingDetailsImpl("source3", new FederationException()));
            add(new ProcessingDetailsImpl("source4", new Exception()));
          }
        });

    catalogMetrics.process(response);

    assertThat(
        meterRegistry.counter("ddf.catalog.exceptions", unsupportedQueryExceptionTags).count(),
        is(1.0));
    assertThat(
        meterRegistry.counter("ddf.catalog.exceptions", sourceUnavailableExceptionTags).count(),
        is(1.0));
    assertThat(
        meterRegistry.counter("ddf.catalog.exceptions", federationExceptionTags).count(), is(1.0));
    assertThat(
        meterRegistry.counter("ddf.catalog.exceptions", unknownExceptionTags).count(), is(1.0));
  }

  @Test
  public void catalogCreateMetric() throws Exception {
    Iterable<Tag> tags = Tags.of("type", "create");
    CreateRequest request = mock(CreateRequest.class);
    CreateResponse response = mock(CreateResponse.class);
    List<Metacard> createdList = mock(List.class);
    when(createdList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getCreatedMetacards()).thenReturn(createdList);

    catalogMetrics.process(response);

    assertThat(meterRegistry.counter("ddf.catalog.ingest", tags).count(), is(100.0));
  }

  @Test
  public void catalogUpdateMetric() throws Exception {
    Iterable<Tag> tags = Tags.of("type", "update");
    UpdateRequest request = mock(UpdateRequest.class);
    UpdateResponse response = mock(UpdateResponse.class);
    List<Update> updatedList = mock(List.class);
    when(updatedList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getUpdatedMetacards()).thenReturn(updatedList);

    catalogMetrics.process(response);

    assertThat(meterRegistry.counter("ddf.catalog.ingest", tags).count(), is(100.0));
  }

  @Test
  public void catalogDeleteMetric() throws Exception {
    Iterable<Tag> tags = Tags.of("type", "delete");
    DeleteRequest request = mock(DeleteRequest.class);
    DeleteResponse response = mock(DeleteResponse.class);
    List<Metacard> deletedList = mock(List.class);
    when(deletedList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getDeletedMetacards()).thenReturn(deletedList);

    catalogMetrics.process(response);

    assertThat(meterRegistry.counter("ddf.catalog.ingest", tags).count(), is(100.0));
  }

  @Test
  public void catalogResourceRetrievalMetric() throws Exception {
    ResourceResponse response = mock(ResourceResponse.class);

    catalogMetrics.process(response);

    assertThat(catalogMetrics.resourceRetrival.count(), is(1.0));
  }

  private Iterable<Tag> getTags(String type, String sourceId, String scope) {
    return Tags.of(Tag.of("type", type), Tag.of("sourceId", sourceId), Tag.of("scope", scope));
  }
}
