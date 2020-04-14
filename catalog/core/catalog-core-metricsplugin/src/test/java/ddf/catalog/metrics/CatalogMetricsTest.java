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
import ddf.catalog.operation.*;
import ddf.catalog.operation.impl.ProcessingDetailsImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.*;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

/**
 * Tests {@link CatalogMetrics}
 *
 * @author Phillip Klinefelter
 */
public class CatalogMetricsTest {

  private static FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();

  private static FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  private MeterRegistryService meterRegistryService;

  private MeterRegistry meterRegistry;

  private static Filter idFilter =
      filterBuilder.attribute(Metacard.ID).is().equalTo().text("metacardId");

  private CatalogMetrics underTest;

  @Before
  public void setup() {
    meterRegistry = new SimpleMeterRegistry();
    meterRegistryService = mock(MeterRegistryService.class);
    when(meterRegistryService.getMeterRegistry()).thenReturn(meterRegistry);
    underTest = new CatalogMetrics(filterAdapter, meterRegistryService);
    System.setProperty(SystemInfo.SITE_NAME, "testSite");
  }

  @Test
  public void catalogQueryMetric() throws Exception {
    QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter));
    underTest.process(query);

    assertThat(underTest.queries.count(), is(1.0));
    assertThat(underTest.comparisonQueries.count(), is(1.0));
  }

  @Test
  public void catalogFederatedQueryMetric() throws Exception {
    QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter), true);
    underTest.process(query);

    query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList("fedSourceId"));
    underTest.process(query);

    query =
        new QueryRequestImpl(
            new QueryImpl(idFilter), Arrays.asList("fedSource1Id", "fedSource2Id"));
    underTest.process(query);

    assertThat(underTest.federatedQueries.count(), is(3.0));
  }

  @Test
  public void catalogFederatedQueryMetricForLocalQueries() throws Exception {
    QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList(""));
    underTest.process(query);

    query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList((String) null));
    underTest.process(query);

    System.setProperty(SystemInfo.SITE_NAME, "localSourceId");
    query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList("localSourceId"));
    underTest.process(query);

    assertThat(underTest.federatedQueries.count(), is(0.0));
  }

  @Test
  public void catalogSpatialQueryMetric() throws Exception {
    Filter geoFilter =
        filterBuilder.attribute(Metacard.ANY_GEO).within().wkt("POLYGON ((1 1,2 1,2 2,1 2,1 1))");

    QueryRequest query = new QueryRequestImpl(new QueryImpl(geoFilter));
    underTest.process(query);

    assertThat(underTest.spatialQueries.count(), is(1.0));
  }

  @Test
  public void catalogTemporalQueryMetric() throws Exception {
    Filter temporalFilter = filterBuilder.attribute(Metacard.ANY_DATE).before().date(new Date());

    QueryRequest query = new QueryRequestImpl(new QueryImpl(temporalFilter));
    underTest.process(query);

    assertThat(underTest.temporalQueries.count(), is(1.0));
  }

  @Test
  public void catalogFunctionQueryMetric() throws Exception {
    Filter functionFilter =
        filterBuilder
            .function("proximity")
            .attributeArg(Core.TITLE)
            .numberArg(1)
            .textArg("Mary little")
            .equalTo()
            .bool(true);

    QueryRequest query = new QueryRequestImpl(new QueryImpl(functionFilter));
    underTest.process(query);

    assertThat(underTest.functionQueries.count(), is(1.0));
  }

  @Test
  public void catalogXpathQueryMetric() throws Exception {
    Filter xpathFilter = filterBuilder.xpath("//node").exists();

    QueryRequest query = new QueryRequestImpl(new QueryImpl(xpathFilter));
    underTest.process(query);

    assertThat(underTest.xpathQueries.count(), is(1.0));
  }

  @Test
  public void catalogFuzzyQueryMetric() throws Exception {
    Filter fuzzyFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like().fuzzyText("fuzzy");

    QueryRequest query = new QueryRequestImpl(new QueryImpl(fuzzyFilter));
    underTest.process(query);

    assertThat(underTest.fuzzyQueries.count(), is(1.0));
  }

  @Test
  public void catalogResultCountMetric() throws Exception {
    QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter));
    QueryResponse response = new QueryResponseImpl(query, new ArrayList(), 50);

    underTest.process(response);

    assertThat(underTest.resultCount.count(), is(1L));
    assertThat(underTest.resultCount.mean(), is(50.0));
  }

  @Test
  public void catalogExceptionMetric() throws Exception {
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

    underTest.process(response);

    assertThat(underTest.exceptions.count(), is(4.0));
    assertThat(underTest.unsupportedQueryExceptions.count(), is(1.0));
    assertThat(underTest.sourceUnavailableExceptions.count(), is(1.0));
    assertThat(underTest.federationExceptions.count(), is(1.0));
  }

  @Test
  public void catalogCreateMetric() throws Exception {
    CreateRequest request = mock(CreateRequest.class);
    CreateResponse response = mock(CreateResponse.class);
    List<Metacard> createdList = mock(List.class);
    when(createdList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getCreatedMetacards()).thenReturn(createdList);

    underTest.process(response);

    assertThat(underTest.createdMetacards.count(), is(100.0));
  }

  @Test
  public void catalogUpdateMetric() throws Exception {
    UpdateRequest request = mock(UpdateRequest.class);
    UpdateResponse response = mock(UpdateResponse.class);
    List<Update> updatedList = mock(List.class);
    when(updatedList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getUpdatedMetacards()).thenReturn(updatedList);

    underTest.process(response);

    assertThat(underTest.updatedMetacards.count(), is(100.0));
  }

  @Test
  public void catalogDeleteMetric() throws Exception {
    DeleteRequest request = mock(DeleteRequest.class);
    DeleteResponse response = mock(DeleteResponse.class);
    List<Metacard> deletedList = mock(List.class);
    when(deletedList.size()).thenReturn(100);
    when(response.getRequest()).thenReturn(request);
    when(response.getDeletedMetacards()).thenReturn(deletedList);

    underTest.process(response);

    assertThat(underTest.deletedMetacards.count(), is(100.0));
  }

  @Test
  public void catalogResourceRetrievalMetric() throws Exception {
    ResourceResponse response = mock(ResourceResponse.class);

    underTest.process(response);

    assertThat(underTest.resourceRetrival.count(), is(1.0));
  }
}
