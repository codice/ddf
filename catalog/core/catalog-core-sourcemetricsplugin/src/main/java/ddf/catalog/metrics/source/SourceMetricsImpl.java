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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the metrics for individual {@link CatalogProvider} and {@link FederatedSource}
 * {@link Source}s. These metrics currently include the count of queries, results per query, and
 * exceptions per {@link Source}.
 *
 * <p>The metrics are created when the {@link Source} is created and deleted when the {@link Source}
 * is deleted.
 *
 * @author rodgersh
 */
public class SourceMetricsImpl implements PreFederatedQueryPlugin, PostFederatedQueryPlugin {

  /** Package name for the JMX MBean where metrics for {@link Source}s are stored. */
  public static final String MBEAN_PACKAGE_NAME = "ddf.metrics.catalog.source";

  /** Prefix for metrics */
  public static final String METRICS_PREFIX = "ddf.catalog.source";

  /**
   * Name of the JMX MBean scope for source-level metrics tracking exceptions while querying a
   * specific {@link Source}
   */
  public static final String EXCEPTIONS_SCOPE = "exceptions";

  /**
   * Name of the JMX MBean scope for source-level metrics tracking query count while querying a
   * specific {@link Source}
   */
  public static final String QUERIES_SCOPE = "queries";

  /**
   * Name of the JMX MBean scope for source-level metrics tracking total results returned while
   * querying a specific {@link Source}
   */
  public static final String QUERIES_TOTAL_RESULTS_SCOPE = "queries.totalresults";

  public static final String DERIVE_DATA_SOURCE_TYPE = "DERIVE";

  public static final String GAUGE_DATA_SOURCE_TYPE = "GAUGE";

  public static final String COUNT_MBEAN_ATTRIBUTE_NAME = "Count";

  public static final String MEAN_MBEAN_ATTRIBUTE_NAME = "Mean";

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceMetricsImpl.class);

  private static final String ALPHA_NUMERIC_REGEX = "[^a-zA-Z0-9]";

  private final MeterRegistry meterRegistry;

  // Map of sourceId to Source's metric data
  protected Map<String, SourceMetric> metrics = new HashMap<String, SourceMetric>();

  // Injected list of CatalogProviders and FederatedSources
  // that is kept updated by container, e.g., with latest sourceIds
  private List<CatalogProvider> catalogProviders = new ArrayList<CatalogProvider>();

  private List<FederatedSource> federatedSources = new ArrayList<FederatedSource>();

  // Map of Source to sourceId - used to detect if sourceId has been changed since last metric
  // update
  private Map<Source, String> sourceToSourceIdMap = new HashMap<Source, String>();

  private ExecutorService executorPool;

  public SourceMetricsImpl(MeterRegistryService meterRegistryService) {
    if (meterRegistryService == null) {
      LOGGER.warn("Meter Registry Service is not available");
    }

    meterRegistry = meterRegistryService.getMeterRegistry();
  }

  public List<CatalogProvider> getCatalogProviders() {
    return catalogProviders;
  }

  public void setCatalogProviders(List<CatalogProvider> catalogProviders) {
    this.catalogProviders = catalogProviders;
  }

  public List<FederatedSource> getFederatedSources() {
    return federatedSources;
  }

  public void setFederatedSources(List<FederatedSource> federatedSources) {
    this.federatedSources = federatedSources;
  }

  // PreFederatedQuery
  @Override
  public QueryRequest process(Source source, QueryRequest input)
      throws PluginExecutionException, StopProcessingException {

    LOGGER.trace("ENTERING: process (for PreFederatedQueryPlugin)");

    // Number of Queries metric per Source
    updateMetric(source.getId(), QUERIES_SCOPE, 1);

    LOGGER.trace("EXITING: process (for PreFederatedQueryPlugin)");

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

      // Total Exceptions metric per Source
      Iterator<ProcessingDetails> iterator = processingDetails.iterator();
      while (iterator.hasNext()) {
        ProcessingDetails next = iterator.next();
        if (next != null && next.getException() != null) {
          String sourceId = next.getSourceId();
          updateMetric(sourceId, EXCEPTIONS_SCOPE, 1);
        }
      }

      Map<String, Integer> totalHitsPerSource = new HashMap<String, Integer>();

      for (Result result : results) {
        String sourceId = result.getMetacard().getSourceId();
        if (totalHitsPerSource.containsKey(sourceId)) {
          totalHitsPerSource.put(sourceId, totalHitsPerSource.get(sourceId) + 1);
        } else {
          // First detection of this new source ID in the results list -
          // initialize the Total Query Result Count for this Source
          totalHitsPerSource.put(sourceId, 1);
        }
      }

      // Total Query Results metric per Source
      for (Map.Entry<String, Integer> source : totalHitsPerSource.entrySet()) {
        updateMetric(source.getKey(), QUERIES_TOTAL_RESULTS_SCOPE, source.getValue());
      }
    }

    LOGGER.trace("EXITING: process (for PostFederatedQueryPlugin)");

    return input;
  }

  public void updateMetric(String sourceId, String name, int incrementAmount) {

    LOGGER.debug("sourceId = {},   name = {}", sourceId, name);

    if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(name)) {
      return;
    }

    String mapKey = sourceId + "." + name;
    SourceMetric sourceMetric = metrics.get(mapKey);

    if (sourceMetric == null) {
      LOGGER.debug("sourceMetric is null for {} - creating metric now", mapKey);
      // Loop through list of all sources until find the sourceId whose metric is being
      // updated
      boolean created = createMetric(catalogProviders, sourceId);
      if (!created) {
        createMetric(federatedSources, sourceId);
      }
      sourceMetric = metrics.get(mapKey);
    }

    // If this metric already exists, then just update its MBean
    if (sourceMetric != null) {
      LOGGER.debug("CASE 1: Metric already exists for {}", mapKey);
      if (sourceMetric.isHistogram()) {
        DistributionSummary metric = (DistributionSummary) sourceMetric.getMetric();
        LOGGER.debug("Updating histogram metric {} by amount of {}", name, incrementAmount);
        metric.record(incrementAmount);
      } else {
        Counter metric = (Counter) sourceMetric.getMetric();
        LOGGER.debug("Updating metric {} by amount of {}", name, incrementAmount);
        metric.increment(incrementAmount);
      }
      return;
    }
  }

  private boolean createMetric(List<? extends Source> sources, String sourceId) {
    for (Source source : sources) {
      if (source.getId().equals(sourceId)) {
        LOGGER.debug("Found sourceId = {} in sources list", sourceId);
        if (sourceToSourceIdMap.containsKey(source)) {
          // Source's ID must have changed since it is in this map but not in the metrics
          // map
          // Delete SourceMetrics for Source's "old" sourceId
          String oldSourceId = sourceToSourceIdMap.get(source);
          LOGGER.debug("CASE 2: source {} exists but has oldSourceId = {}", sourceId, oldSourceId);

          // Create metrics for Source with new sourceId
          createMetric(
              sourceId, METRICS_PREFIX + "." + QUERIES_TOTAL_RESULTS_SCOPE, MetricType.HISTOGRAM);
          createMetric(sourceId, METRICS_PREFIX + "." + QUERIES_SCOPE, MetricType.COUNTER);
          createMetric(sourceId, METRICS_PREFIX + "." + EXCEPTIONS_SCOPE, MetricType.COUNTER);

          // Add Source to map with its new sourceId
          sourceToSourceIdMap.put(source, sourceId);
        } else {
          // This is a brand new Source - create metrics for it
          // (Should rarely happen since Sources typically have their metrics created
          // when the Source itself is created via the addingSource() method. This could
          // happen if sourceId = null when Source originally created and then its metric
          // needs updating because client, e.g., SortedFederationStrategy, knows the
          // Source exists.)
          LOGGER.debug("CASE 3: New source {} detected - creating metrics", sourceId);
          createMetric(
              sourceId, METRICS_PREFIX + "." + QUERIES_TOTAL_RESULTS_SCOPE, MetricType.HISTOGRAM);
          createMetric(sourceId, METRICS_PREFIX + "." + QUERIES_SCOPE, MetricType.COUNTER);
          createMetric(sourceId, METRICS_PREFIX + "." + EXCEPTIONS_SCOPE, MetricType.COUNTER);

          sourceToSourceIdMap.put(source, sourceId);
        }
        return true;
      }
    }

    LOGGER.debug("Did not find source {} in Sources - cannot create metrics", sourceId);

    return false;
  }

  /**
   * Creates metrics for new CatalogProvider or FederatedSource when they are initially created.
   * Metrics creation includes the JMX MBeans and associated ddf.metrics.collector.JmxCollector.
   *
   * @param source
   * @param props
   */
  public void addingSource(final Source source, Map props) {
    LOGGER.trace("ENTERING: addingSource");

    if (executorPool == null) {
      executorPool =
          Executors.newCachedThreadPool(
              StandardThreadFactoryBuilder.newThreadFactory("sourceMetricThread"));
    }

    // Creating JmxCollectors for all of the source metrics can be time consuming,
    // so do this in a separate thread to prevent blacklisting by EventAdmin
    final Runnable metricsCreator =
        new Runnable() {
          public void run() {
            createSourceMetrics(source);
          }
        };

    LOGGER.debug("Start metricsCreator thread for Source {}", source.getId());
    executorPool.execute(metricsCreator);

    LOGGER.trace("EXITING: addingSource");
  }

  /**
   * Deletes metrics for existing CatalogProvider or FederatedSource when they are deleted. Metrics
   * deletion includes the JMX MBeans and associated ddf.metrics.collector.JmxCollector.
   *
   * @param source
   * @param props
   */
  public void deletingSource(final Source source, final Map props) {
    LOGGER.trace("ENTERING: deletingSource");

    if (source == null || StringUtils.isBlank(source.getId())) {
      LOGGER.debug("Not deleting metrics for NULL or blank source");
      return;
    }

    String sourceId = source.getId();

    LOGGER.debug("sourceId = {},    props = {}", sourceId, props);

    // Delete source from internal map used when updating metrics by sourceId
    sourceToSourceIdMap.remove(source);

    LOGGER.trace("EXITING: deletingSource");
  }

  // Separate, package-scope method to allow unit testing
  void createSourceMetrics(final Source source) {

    if (source == null || StringUtils.isBlank(source.getId())) {
      LOGGER.debug("Not adding metrics for NULL or blank source");
      return;
    }

    String sourceId = source.getId();

    LOGGER.debug("sourceId = {}", sourceId);

    createMetric(sourceId, METRICS_PREFIX + QUERIES_TOTAL_RESULTS_SCOPE, MetricType.HISTOGRAM);
    createMetric(sourceId, METRICS_PREFIX + QUERIES_SCOPE, MetricType.COUNTER);
    createMetric(sourceId, METRICS_PREFIX + EXCEPTIONS_SCOPE, MetricType.COUNTER);

    // Add new source to internal map used when updating metrics by sourceId
    sourceToSourceIdMap.put(source, sourceId);
  }

  private void createMetric(String sourceId, String mbeanName, MetricType type) {

    // Create source-specific metrics for this source
    String key = sourceId + "." + mbeanName;

    // Do not create metric and collector if they already exist for this source.
    // (This can happen for ConnectedSources because they have the same sourceId
    // as the local catalog provider).
    if (!metrics.containsKey(key)) {
      if (type == MetricType.HISTOGRAM) {
        DistributionSummary histogram = meterRegistry.summary(mbeanName);
        metrics.put(key, new SourceMetric(histogram, true));
      } else if (type == MetricType.COUNTER) {
        Counter counter = meterRegistry.counter(mbeanName);
        metrics.put(key, new SourceMetric(counter));
      } else {
        LOGGER.debug("Metric {} not created because unknown metric type {} specified.", key, type);
      }
    } else {
      LOGGER.debug("Metric {} already exists - not creating again", key);
    }
  }

  // The types of Micrometer Metrics supported
  private enum MetricType {
    HISTOGRAM,
    COUNTER
  }

  /**
   * Inner class POJO to maintain details of each metric for each Source.
   *
   * @author rodgersh
   */
  public static class SourceMetric {

    // The Micrometer Meter
    private Meter metric;

    // Whether this metric is a Histogram or Counter
    private boolean isHistogram = false;

    public SourceMetric(Meter metric) {
      this(metric, false);
    }

    public SourceMetric(Meter metric, boolean isHistogram) {
      this.metric = metric;
      this.isHistogram = isHistogram;
    }

    public Meter getMetric() {
      return metric;
    }

    public boolean isHistogram() {
      return isHistogram;
    }
  }
}
