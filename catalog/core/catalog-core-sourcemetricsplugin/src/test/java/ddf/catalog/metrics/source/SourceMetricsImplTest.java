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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.metrics.source.SourceMetricsImpl.SourceMetric;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import java.util.Collections;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceMetricsImplTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceMetricsImplTest.class);

  private SourceMetricsImpl sourceMetrics;

  private CatalogProvider catalogProvider;

  private FederatedSource fedSource;

  @Test
  public void testAddDeleteSource() throws Exception {
    String sourceId = "cp-1";
    String metricName = SourceMetrics.QUERIES_SCOPE;

    sourceMetrics = configureSourceMetrics(sourceId);
    addSource();

    String key = sourceId + "." + metricName;
    SourceMetric sourceMetric = sourceMetrics.metrics.get(key);
    assertThat(sourceMetric, not(nullValue()));

    sourceMetrics.deletingSource(catalogProvider, null);
    sourceMetric = sourceMetrics.metrics.get(key);
    assertThat(sourceMetric, is(nullValue()));
  }

  @Test
  public void testNewSource() throws Exception {
    String sourceId = "cp-1";
    String metricName = SourceMetrics.QUERIES_SCOPE;

    sourceMetrics = configureSourceMetrics(sourceId);
    sourceMetrics.updateMetric(sourceId, metricName, 1);

    assertMetricCount(sourceId, metricName, 1);
  }

  @Test
  public void testSourceIdChanged() throws Exception {
    String sourceId = "cp-1";
    String metricName = SourceMetrics.QUERIES_SCOPE;

    sourceMetrics = configureSourceMetrics(sourceId);

    // Simulate initial creation of Source
    addSource();

    // Simulate changing Source's name
    String newSourceId = "cp-new";
    when(catalogProvider.getId()).thenReturn(newSourceId);

    sourceMetrics.updateMetric(newSourceId, metricName, 1);

    assertMetricCount(newSourceId, metricName, 1);
  }

  @Test
  public void testSourceCreatedWithNullId() throws Exception {
    String sourceId = null;
    String metricName = SourceMetrics.QUERIES_TOTAL_RESULTS_SCOPE;

    sourceMetrics = configureSourceMetrics(sourceId);

    // Simulate initial creation of Source
    addSource();

    // Simulate changing Source's name
    String newSourceId = "cp-1";
    when(catalogProvider.getId()).thenReturn(newSourceId);

    sourceMetrics.updateMetric(newSourceId, metricName, 1);

    assertMetricCount(newSourceId, metricName, 1);
  }

  @Test
  public void testUpdateNonExistingSourceMetric() throws Exception {
    String sourceId = "existing-source";
    String metricName = SourceMetrics.QUERIES_SCOPE;

    sourceMetrics = configureSourceMetrics(sourceId);

    String nonExistingSourceId = "non-existing-source-id";
    sourceMetrics.updateMetric(nonExistingSourceId, metricName, 1);

    String key = nonExistingSourceId + "." + metricName;
    SourceMetric sourceMetric = sourceMetrics.metrics.get(key);
    assertThat(sourceMetric, is(nullValue()));
  }

  @Test
  public void testUpdateMetricForEmptySourceId() throws Exception {
    String sourceId = "existing-source";
    String metricName = SourceMetrics.QUERIES_SCOPE;

    sourceMetrics = configureSourceMetrics(sourceId);

    sourceMetrics.updateMetric("", metricName, 1);

    assertThat(sourceMetrics.metrics.size(), is(0));
  }

  @Test
  public void testUpdateEmptyMetricForSourceId() throws Exception {
    String sourceId = "existing-source";

    sourceMetrics = configureSourceMetrics(sourceId);

    sourceMetrics.updateMetric(sourceId, "", 1);

    assertThat(sourceMetrics.metrics.size(), is(0));
  }

  @Test
  public void testUpdateNonExistentMetricForExistingSourceId() throws Exception {
    String sourceId = "existing-source";

    sourceMetrics = configureSourceMetrics(sourceId);

    addSource();

    sourceMetrics.updateMetric(sourceId, "invalid-metric", 1);

    // Verify none of the valid metrics were updated (but they were
    // created since this is first time this sourceId was detected by
    // the SourceMetricsImpl)
    assertMetricCount(sourceId, SourceMetrics.QUERIES_TOTAL_RESULTS_SCOPE, 0);
    assertMetricCount(sourceId, SourceMetrics.QUERIES_SCOPE, 0);
    assertMetricCount(sourceId, SourceMetrics.EXCEPTIONS_SCOPE, 0);
  }

  @Test
  public void testUpdateNonExistentMetricForNewSourceId() throws Exception {
    String sourceId = "new-source";
    String metricName = "invalid-metric";

    SourceMetricsImpl sourceMetrics = configureSourceMetrics(sourceId);

    sourceMetrics.updateMetric(sourceId, metricName, 1);

    // Verify none of the valid metrics were updated (but they were
    // created since this is first time this sourceId was detected by
    // the SourceMetricsImpl)
    assertMetricCount(sourceId, SourceMetrics.QUERIES_TOTAL_RESULTS_SCOPE, 0);
    assertMetricCount(sourceId, SourceMetrics.QUERIES_SCOPE, 0);
    assertMetricCount(sourceId, SourceMetrics.EXCEPTIONS_SCOPE, 0);
  }

  @Test
  public void testDeleteSourceBlankSourceId() throws Exception {
    String sourceId = "cp-1";
    String metricName = SourceMetrics.QUERIES_SCOPE;

    sourceMetrics = configureSourceMetrics(sourceId);
    addSource();

    // Simulate Source returning empty sourceId
    when(catalogProvider.getId()).thenReturn("");

    sourceMetrics.deletingSource(catalogProvider, null);

    String key = sourceId + "." + metricName;
    SourceMetric sourceMetric = sourceMetrics.metrics.get(key);
    assertThat(sourceMetric, not(nullValue()));

    sourceMetrics.deletingSource(null, null);

    key = sourceId + "." + metricName;
    sourceMetric = sourceMetrics.metrics.get(key);
    assertThat(sourceMetric, not(nullValue()));
  }

  /** ********************************************************************************* */
  private SourceMetricsImpl configureSourceMetrics(String sourceId) throws Exception {

    catalogProvider = mock(CatalogProvider.class);
    when(catalogProvider.getId()).thenReturn(sourceId);

    fedSource = mock(FederatedSource.class);
    when(fedSource.getId()).thenReturn("fs-1");

    sourceMetrics = new SourceMetricsImpl(mock(MeterRegistryService.class));
    sourceMetrics.setCatalogProviders(Collections.singletonList(catalogProvider));
    sourceMetrics.setFederatedSources(Collections.singletonList(fedSource));

    assertThat(sourceMetrics, not(nullValue()));

    return sourceMetrics;
  }

  private void addSource() throws Exception {
    // Do not call addingSource() because it starts createSourceMetrics()
    // in a separate thread hence predictable results are not assured
    // sourceMetrics.addingSource(catalogProvider, null);

    sourceMetrics.createSourceMetrics(catalogProvider);
  }

  private void assertMetricCount(String sourceId, String metricName, int expectedCount) {
    String key = sourceId + "." + metricName;
    SourceMetric sourceMetric = sourceMetrics.metrics.get(key);

    if (sourceMetric.isHistogram()) {
      DistributionSummary histogram = (DistributionSummary) sourceMetric.getMetric();
      assertThat(histogram.count(), is((long) expectedCount));
    } else {
      Counter meter = (Counter) sourceMetric.getMetric();
      assertThat(meter.count(), is((long) expectedCount));
    }
  }
}
