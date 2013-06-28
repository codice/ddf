/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.metrics;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.Histogram;
import com.yammer.metrics.JmxReporter;
import com.yammer.metrics.Meter;
import com.yammer.metrics.Metric;
import com.yammer.metrics.MetricRegistry;

import ddf.catalog.metrics.internal.SourceMetrics;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;

/**
 * This class manages the metrics for individual {@link CatalogProvider} and {@link FederatedSource}
 * {@link Source}s. These metrics currently include the count of queries, results per query, 
 * and exceptions per {@link Source}.
 * 
 * The metrics and their associated {@link JmxCollector}s are created when the {@link Source} is created
 * and deleted when the {@link Source} is deleted. (The associated RRD file remains available indefinitely
 * and accessible from the Metrics tab in the Web Admin console unless an administrator manually deletes it). 
 * 
 * If a {@link Source} is renamed, i.e., its ID changed, then the {@link Source}'s existing metrics' MBeans 
 * and {@link JmxCollector}s are deleted and new metrics created using the new {@link Source}'s ID. 
 * However, the RRD file for the {@link Source}'s previous source ID remains available and accessible from 
 * the Metrics tab in the Web Admin console unless an administrator manually deletes it.
 * 
 * @author rodgersh
 * @author ddf.isgs@lmco.com
 *
 */
public class SourceMetricsImpl implements SourceMetrics {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceMetricsImpl.class);
	
    public static final String JMX_COLLECTOR_FACTORY_PID = "MetricsJmxCollector";
    
    private static final String ALPHA_NUMERIC_REGEX = "[^a-zA-Z0-9]";
    
    private static final String RRD_FILENAME_EXTENSION = ".rrd";
	
	private final MetricRegistry metricsRegistry = new MetricRegistry(
			MBEAN_PACKAGE_NAME);
	
    private final JmxReporter reporter = JmxReporter.forRegistry(metricsRegistry)
            .build();
    
    // The types of Yammer Metrics supported
    private enum MetricType { HISTOGRAM, METER };
    
    private ConfigurationAdmin configurationAdmin;
    
    // Injected list of CatalogProviders and FederatedSources 
    // that is kept updated by container, e.g., with latest sourceIds
    private List<CatalogProvider> catalogProviders = new ArrayList<CatalogProvider>();
    private List<FederatedSource> federatedSources = new ArrayList<FederatedSource>();
    
    // Map of Source to sourceId - used to detect if sourceId has been changed since last metric update
    private Map<Source, String> sourceToSourceIdMap = new HashMap<Source, String>();
    
    // Map of sourceId to Source's metric data
    protected Map<String, SourceMetric> metrics = new HashMap<String, SourceMetric>();
    

	/**
	 * 
	 * @param configurationAdmin
	 */
	public SourceMetricsImpl(ConfigurationAdmin configurationAdmin) {
		
		LOGGER.trace("INSIDE: SourceTracker constructor");
		
		this.configurationAdmin = configurationAdmin;
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

	public void init() {
		LOGGER.trace("INSIDE: init");
		
		reporter.start();
	}
	
	public void destroy() {
		LOGGER.trace("INSIDE: destroy");
		
		reporter.stop();
	}

	@Override
	public void updateMetric(String sourceId, String name, int incrementAmount) {
				
		LOGGER.debug("sourceId = {},   name = {}", sourceId, name);
		
		if (StringUtils.isBlank(sourceId) || StringUtils.isBlank(name)) {
			return;
		}
		
		String mapKey = sourceId + "." + name;
    	SourceMetric sourceMetric = metrics.get(mapKey);
    	
    	if (sourceMetric == null) {
    		// Loop through list of all sources until find the sourceId whose metric is being updated
    		boolean created = createMetric(catalogProviders, sourceId);
    		if (!created) {
    			createMetric(federatedSources, sourceId);
    		}
    		sourceMetric = metrics.get(mapKey);
    	}
    	
    	// If this metric already exists, then just update its MBean
    	if (sourceMetric != null) {
    		LOGGER.debug("CASE 1: Metric already exists for " + mapKey);
    		if (sourceMetric.isHistogram()) {
    			Histogram metric = (Histogram) sourceMetric.getMetric();
    			LOGGER.debug("Updating histogram metric " + name + " by amount of " + incrementAmount);
    			metric.update(incrementAmount);
    		} else {
    			Meter metric = (Meter) sourceMetric.getMetric();
    			LOGGER.debug("Updating metric " + name + " by amount of " + incrementAmount);
    			metric.mark(incrementAmount);
    		}
    		return;
    	}
	}
	
	private boolean createMetric(List<? extends Source> sources, String sourceId) {
		for (Source source : sources) {
			if (source.getId().equals(sourceId)) {
				LOGGER.debug("Found sourceId = " + sourceId + " in sources list");
				if (sourceToSourceIdMap.containsKey(source)) {
					// Source's ID must have changed since it is in this map but not in the metrics map
					// Delete SourceMetrics for Source's "old" sourceId
					String oldSourceId = sourceToSourceIdMap.get(source);
					LOGGER.debug("CASE 2: source " + sourceId + " exists but has oldSourceId = " + oldSourceId);
					deleteMetric(oldSourceId, QUERIES_TOTAL_RESULTS_SCOPE);
			    	deleteMetric(oldSourceId, QUERIES_SCOPE);
			    	deleteMetric(oldSourceId, EXCEPTIONS_SCOPE);
			    	
			    	// Create metrics for Source with new sourceId
			    	createMetric(sourceId, QUERIES_TOTAL_RESULTS_SCOPE, MetricType.HISTOGRAM);
					createMetric(sourceId, QUERIES_SCOPE, MetricType.METER);
					createMetric(sourceId, EXCEPTIONS_SCOPE, MetricType.METER);
					
					// Add Source to map with its new sourceId
					sourceToSourceIdMap.put(source, sourceId);
				} else {
					// This is a brand new Source - create metrics for it
					// (Should rarely happen since Sources typically have their metrics created
					// when the Source itself is created via the addingSource() method. This could
					// happen if sourceId = null when Source originally created and then its metric
					// needs updating because client, e.g., SortedFederationStrategy, knows the 
					// Source exists.)
					LOGGER.debug("CASE 3: New source " + sourceId + " detected - creating metrics");
					createMetric(sourceId, QUERIES_TOTAL_RESULTS_SCOPE, MetricType.HISTOGRAM);
					createMetric(sourceId, QUERIES_SCOPE, MetricType.METER);
					createMetric(sourceId, EXCEPTIONS_SCOPE, MetricType.METER);
					
					sourceToSourceIdMap.put(source, sourceId);
				}
				return true;
			}
		}
		
		LOGGER.debug("Did not find source " + sourceId + " in Sources - cannot create metrics");
		
		return false;
	}
	
	/**
	 * Creates metrics for new CatalogProvider or FederatedSource when they
	 * are initially created. Metrics creation includes the JMX MBeans and 
	 * associated JmxCollector.
	 * 
	 * @param source
	 * @param props
	 */
	public void addingSource(Source source, Map props) {
		LOGGER.trace("ENTERING: addingService");
		
		if (source == null || StringUtils.isBlank(source.getId())) {
			LOGGER.info("Not adding metrics for NULL or blank source");
			return;
		}
		
		String sourceId = source.getId();
		
		LOGGER.debug("sourceId = {}", sourceId);
		
		createMetric(sourceId, QUERIES_TOTAL_RESULTS_SCOPE, MetricType.HISTOGRAM);
		createMetric(sourceId, QUERIES_SCOPE, MetricType.METER);
		createMetric(sourceId, EXCEPTIONS_SCOPE, MetricType.METER);
		
		// Add new source to internal map used when updating metrics by sourceId
		sourceToSourceIdMap.put(source, sourceId);
		
		LOGGER.trace("EXITING: addingService");
	}
	
    /**
     * Deletes metrics for existing CatalogProvider or FederatedSource when they
	 * are deleted. Metrics deletion includes the JMX MBeans and 
	 * associated JmxCollector.
	 * 
     * @param source
     * @param props
     */
    public void deletingSource(Source source, Map props) {
    	LOGGER.trace("ENTERING: deletingService");
    	
    	if (source == null || StringUtils.isBlank(source.getId())) {
			LOGGER.info("Not deleting metrics for NULL or blank source");
			return;
		}
		
		String sourceId = source.getId();
    	
    	LOGGER.debug("sourceId = {},    props = {}", sourceId, props);
    	 
    	deleteMetric(sourceId, QUERIES_TOTAL_RESULTS_SCOPE);
    	deleteMetric(sourceId, QUERIES_SCOPE);
    	deleteMetric(sourceId, EXCEPTIONS_SCOPE);
    	
    	// Delete source from internal map used when updating metrics by sourceId
    	sourceToSourceIdMap.remove(source);
    	
    	LOGGER.trace("EXITING: deletingService");
	}
    
    private void createMetric(String sourceId, String mbeanName, MetricType type) {
        
		// Create source-specific metrics for this source
		// (Must be done prior to creating metrics collector so that
		// JMX MBean exists for collector to detect).
		String key = sourceId + "." + mbeanName;
		
		// Do not create metric and collector if they already exist for this source.
		// (This can happen for ConnectedSources because they have the same sourceId
		// as the local catalog provider).
		if (!metrics.containsKey(key)) {
			if (type == MetricType.HISTOGRAM){ 
				Histogram histogram = metricsRegistry.histogram(MetricRegistry.name(sourceId, mbeanName));
				String pid = createMetricsCollector(sourceId, mbeanName);
				metrics.put(key, new SourceMetric(histogram, sourceId, pid, true));
			} else if (type == MetricType.METER) {
				Meter meter = metricsRegistry.meter(MetricRegistry.name(sourceId, mbeanName));
				String pid = createMetricsCollector(sourceId, mbeanName);
				metrics.put(key, new SourceMetric(meter, sourceId, pid));
			} else {
				LOGGER.debug("Metric " + key
						+ " not created because unknown metric type " + type
						+ " specified.");
			}
		} else {
			LOGGER.debug("Metric " + key + " already exists - not creating again");
		}
    }
    
    /**
     * Creates the JMX Collector for an associated metric's JMX MBean.
     * 
     * @param sourceId
     * @param collectorName
     * @return the PID of the JmxCollector Managed Service Factory created
     */
    private String createMetricsCollector(String sourceId, String collectorName) {
    	
		LOGGER.trace(
				"ENTERING: createMetricsCollector - sourceId = {},   collectorName = {}",
				sourceId, collectorName);
		
    	String pid = null;
    	
    	String rrdPath = getRrdFilename(sourceId, collectorName);
    	    	
    	// Create the Managed Service Factory for the JmxCollector, setting its service properties
    	// to the MBean it will poll and the RRD file it will populate
    	try {
			Configuration config = configurationAdmin.createFactoryConfiguration(JMX_COLLECTOR_FACTORY_PID, null);
			Dictionary<String,String> props = new Hashtable<String,String>();
			props.put("mbeanName", MBEAN_PACKAGE_NAME + ":name=" + sourceId + "." + collectorName);
			props.put("mbeanAttributeName", "Count");
			props.put("rrdPath", rrdPath);
			props.put("rrdDataSourceName", "data");
			props.put("rrdDataSourceType", "COUNTER");
			config.update(props);
			pid = config.getPid();
			LOGGER.debug("JmxCollector pid = {} for sourceId = {}", pid, sourceId);
		} catch (IOException e) {
			LOGGER.warn("Unable to create " + collectorName + " JmxCollector for source " + sourceId, e);
		}
    	
    	LOGGER.trace("EXITING: createMetricsCollector");
    	
    	return pid;
    }
    
    protected String getRrdFilename(String sourceId, String collectorName) {
    	
    	// Based on the sourceId and collectorName, generate the name of the RRD file.
    	// This RRD file will be of the form "source<sourceId><collectorName>.rrd" with
    	// the non-alphanumeric characters stripped out and the next character after any
    	// non-alphanumeric capitalized.
    	// Example:
    	//     Given sourceId = dib30rhel-58 and collectorName = Queries.TotalResults
    	//     The resulting RRD filename would be: sourceDib30rhel58QueriesTotalResults.rrd
    	String[] sourceIdParts = sourceId.split(ALPHA_NUMERIC_REGEX);
    	String newSourceId = "";
    	for (String part : sourceIdParts) {
    		newSourceId += StringUtils.capitalize(part);
    	}
    	String rrdPath = "source" + newSourceId + collectorName;
    	LOGGER.debug("BEFORE: rrdPath = " + rrdPath);
    	
    	// Sterilize RRD path name by removing any non-alphanumeric characters - this would confuse the
    	// URL being generated for this RRD path in the Metrics tab of Admin console.
    	rrdPath = rrdPath.replaceAll(ALPHA_NUMERIC_REGEX, "");
    	rrdPath += RRD_FILENAME_EXTENSION;
    	LOGGER.debug("AFTER: rrdPath = " + rrdPath);

    	return rrdPath;
    }
    
    /**
     * Delete the metric's MBean for the specified Source.
     * 
     * @param sourceId
     * @param mbeanName
     */
    private void deleteMetric(String sourceId, String mbeanName) {

    	String key = sourceId + "." + mbeanName;
    	if (metrics.containsKey(key)) {
	    	metricsRegistry.remove(MetricRegistry.name(sourceId, mbeanName));    	
	    	deleteCollector(sourceId, mbeanName);
	    	metrics.remove(key);
    	} else {
    		LOGGER.debug("Did not remove metric " + key + " because it was not in metrics map");
    	}
    }
    
    /**
     * Delete the JmxCollector Managed Service Factory for the specified Source and
     * MBean.
     * 
     * @param sourceId
     * @param metricName
     */
    private void deleteCollector(String sourceId, String metricName) {
    	String mapKey = sourceId + "." + metricName;
    	SourceMetric sourceMetric = metrics.get(mapKey);
    	try {
			Configuration config = configurationAdmin.getConfiguration(sourceMetric.getPid());
			if (config != null) {
				LOGGER.debug("Deleting " + metricName + " JmxCollector for source " + sourceId);
				config.delete();
			}
		} catch (IOException e) {
			LOGGER.warn("Unable to delete " + metricName + " JmxCollector for source " + sourceId, e);
		}
      metrics.remove(mapKey);
    }    
    
    /**
     * Inner class POJO to maintain details of each metric for each Source.
     * 
     * @author rodgersh
     *
     */
    public class SourceMetric {
    	
    	// The Yammer Metric
    	private Metric metric;
    	
    	// The ID of the Source (CatalogProvider or Federated Source) this metric
    	// is affiliated with
    	private String sourceId;
    	
    	// The PID of the JmxCollector Managed Service Factory polling this metric's
    	// MBean
    	private String pid;
    	
    	// Whether this metric is a Histogram or Meter
    	private boolean isHistogram = false;
    	
    	public SourceMetric(Metric metric, String sourceId, String pid) {
    		this(metric, sourceId, pid, false);
    	}
    	
    	public SourceMetric(Metric metric, String sourceId, String pid, boolean isHistogram) {
    		this.metric = metric;
    		this.sourceId = sourceId;
    		this.pid = pid;
    		this.isHistogram = isHistogram;
    	}
    	
		public Metric getMetric() {
			return metric;
		}
				
		public String getPid() {
			return pid;
		}
		
		public boolean isHistogram() {
			return isHistogram;
		}
    	
    }

}
