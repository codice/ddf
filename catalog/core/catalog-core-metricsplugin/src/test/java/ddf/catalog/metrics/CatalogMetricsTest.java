/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.metrics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codice.ddf.configuration.ConfigurationManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

import com.codahale.metrics.MetricRegistry;

import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.ProcessingDetailsImpl;
import ddf.catalog.operation.QueryImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.QueryResponseImpl;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * Tests {@link CatalogMetrics}
 * 
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
public class CatalogMetricsTest {

    private CatalogMetrics underTest;

    private static FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();

    private static FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

    private static Filter idFilter = filterBuilder.attribute(Metacard.ID).is().equalTo()
            .text("metacardId");

    @Before
    public void setup() {
        underTest = new CatalogMetrics(filterAdapter);
    }

    @After
    public void tearDown() {

        // Remove the metrics created when setup() instantiated CatalogMetrics -
        // otherwise get lots of exceptions that metric already exists which fill
        // up the log to point of Travis CI build failing

        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.QUERIES_SCOPE, "TotalResults"));

        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.QUERIES_SCOPE));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.QUERIES_SCOPE, "Federated"));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.QUERIES_SCOPE, "Comparison"));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.QUERIES_SCOPE, "Spatial"));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.QUERIES_SCOPE, "Xpath"));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.QUERIES_SCOPE, "Fuzzy"));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.QUERIES_SCOPE, "Temporal"));

        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.EXCEPTIONS_SCOPE));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.EXCEPTIONS_SCOPE,
                "UnsupportedQuery"));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.EXCEPTIONS_SCOPE,
                "SourceUnavailable"));
        underTest.metrics
                .remove(MetricRegistry.name(CatalogMetrics.EXCEPTIONS_SCOPE, "Federation"));

        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.INGEST_SCOPE, "Created"));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.INGEST_SCOPE, "Updated"));
        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.INGEST_SCOPE, "Deleted"));

        underTest.metrics.remove(MetricRegistry.name(CatalogMetrics.RESOURCE_SCOPE));

        underTest.reporter.stop();
    }

    @Test
    public void catalogQueryMetric() throws Exception {
        QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter));
        underTest.process(query);

        assertThat(underTest.queries.getCount(), is(1L));
        assertThat(underTest.comparisonQueries.getCount(), is(1L));
    }

    @Test
    public void catalogFederatedQueryMetric() throws Exception {
        QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter), true);
        underTest.process(query);

        query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList("fedSourceId"));
        underTest.process(query);

        query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList("fedSource1Id",
                "fedSource2Id"));
        underTest.process(query);

        assertThat(underTest.federatedQueries.getCount(), is(3L));
    }

    @Test
    public void catalogFederatedQueryMetricForLocalQueries() throws Exception {
        QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList(""));
        underTest.process(query);

        query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList((String) null));
        underTest.process(query);

        setLocalCatalogId("localSourceId");
        query = new QueryRequestImpl(new QueryImpl(idFilter), Arrays.asList("localSourceId"));
        underTest.process(query);

        assertThat(underTest.federatedQueries.getCount(), is(0L));
    }

    private void setLocalCatalogId(String catalogId) {
        Map<String, String> settings = new HashMap<String, String>();
        settings.put(ConfigurationManager.SITE_NAME, catalogId);
        underTest.configurationUpdateCallback(settings);
    }

    @Test
    public void catalogSpatialQueryMetric() throws Exception {
        Filter geoFilter = filterBuilder.attribute(Metacard.ANY_GEO).within()
                .wkt("POLYGON ((1 1,2 1,2 2,1 2,1 1))");

        QueryRequest query = new QueryRequestImpl(new QueryImpl(geoFilter));
        underTest.process(query);

        assertThat(underTest.spatialQueries.getCount(), is(1L));
    }

    @Test
    public void catalogTemporalQueryMetric() throws Exception {
        Filter temporalFilter = filterBuilder.attribute(Metacard.ANY_DATE).before()
                .date(new Date());

        QueryRequest query = new QueryRequestImpl(new QueryImpl(temporalFilter));
        underTest.process(query);

        assertThat(underTest.temporalQueries.getCount(), is(1L));
    }

    @Test
    public void catalogXpathQueryMetric() throws Exception {
        Filter xpathFilter = filterBuilder.xpath("//node").exists();

        QueryRequest query = new QueryRequestImpl(new QueryImpl(xpathFilter));
        underTest.process(query);

        assertThat(underTest.xpathQueries.getCount(), is(1L));
    }

    @Test
    public void catalogFuzzyQueryMetric() throws Exception {
        Filter fuzzyFilter = filterBuilder.attribute(Metacard.ANY_TEXT).like().fuzzyText("fuzzy");

        QueryRequest query = new QueryRequestImpl(new QueryImpl(fuzzyFilter));
        underTest.process(query);

        assertThat(underTest.fuzzyQueries.getCount(), is(1L));
    }

    @Test
    public void catalogResultCountMetric() throws Exception {
        QueryRequest query = new QueryRequestImpl(new QueryImpl(idFilter));
        QueryResponse response = new QueryResponseImpl(query, new ArrayList(), 50);

        underTest.process(response);

        assertThat(underTest.resultCount.getCount(), is(1L));
        assertThat(underTest.resultCount.getSnapshot().getMean(), is(50.0));
    }

    @Test
    public void catalogExceptionMetric() throws Exception {
        QueryResponse response = new QueryResponseImpl(
                new QueryRequestImpl(new QueryImpl(idFilter)));
        Set<ProcessingDetails> details = response.getProcessingDetails();

        details.addAll(new HashSet<ProcessingDetails>() {
            {
                add(new ProcessingDetailsImpl("source1", new UnsupportedQueryException()));
                add(new ProcessingDetailsImpl("source2", new SourceUnavailableException()));
                add(new ProcessingDetailsImpl("source3", new FederationException()));
                add(new ProcessingDetailsImpl("source4", new Exception()));
            }
        });

        underTest.process(response);

        assertThat(underTest.exceptions.getCount(), is(4L));
        assertThat(underTest.unsupportedQueryExceptions.getCount(), is(1L));
        assertThat(underTest.sourceUnavailableExceptions.getCount(), is(1L));
        assertThat(underTest.federationExceptions.getCount(), is(1L));
    }

    @Test
    public void catalogCreateMetric() throws Exception {
        CreateResponse response = mock(CreateResponse.class);
        List<Metacard> createdList = mock(List.class);
        when(createdList.size()).thenReturn(100);
        when(response.getCreatedMetacards()).thenReturn(createdList);

        underTest.process(response);

        assertThat(underTest.createdMetacards.getCount(), is(100L));
    }

    @Test
    public void catalogUpdateMetric() throws Exception {
        UpdateResponse response = mock(UpdateResponse.class);
        List<Update> updatedList = mock(List.class);
        when(updatedList.size()).thenReturn(100);
        when(response.getUpdatedMetacards()).thenReturn(updatedList);

        underTest.process(response);

        assertThat(underTest.updatedMetacards.getCount(), is(100L));
    }

    @Test
    public void catalogDeleteMetric() throws Exception {
        DeleteResponse response = mock(DeleteResponse.class);
        List<Metacard> deletedList = mock(List.class);
        when(deletedList.size()).thenReturn(100);
        when(response.getDeletedMetacards()).thenReturn(deletedList);

        underTest.process(response);

        assertThat(underTest.deletedMetacards.getCount(), is(100L));
    }

    @Test
    public void catalogResourceRetrievalMetric() throws Exception {
        ResourceResponse response = mock(ResourceResponse.class);

        underTest.process(response);

        assertThat(underTest.resourceRetrival.getCount(), is(1L));
    }

}
