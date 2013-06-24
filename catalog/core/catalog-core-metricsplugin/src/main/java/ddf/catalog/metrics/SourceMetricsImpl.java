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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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
import ddf.catalog.source.Source;

public class SourceMetricsImpl implements SourceMetrics {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SourceMetricsImpl.class);
	
    private static final String JMX_COLLECTOR_FACTORY_PID = "ddf.metrics.reporting.internal.rrd4j.JmxCollector";
    
    private Map<String, SourceMetric> metrics = new HashMap<String, SourceMetric>();
	
	private final MetricRegistry metricsRegistry = new MetricRegistry(
			MBEAN_PACKAGE_NAME);
	
    private final JmxReporter reporter = JmxReporter.forRegistry(metricsRegistry)
            .build();
    
    private ConfigurationAdmin configurationAdmin;

	public SourceMetricsImpl(ConfigurationAdmin configurationAdmin) {
		this.configurationAdmin = configurationAdmin;
		LOGGER.trace("INSIDE: SourceTracker constructor");
	}
	
	public void init() {
		LOGGER.trace("INSIDE: init");
		
		reporter.start();
	}
	
	public void destroy() {
		LOGGER.trace("INSIDE: destroy");
		
		reporter.stop();
	}
	
	public void addingSource( Source source, Map props ) {
		LOGGER.trace("ENTERING: addingService");
		
		if (source == null || StringUtils.isBlank(source.getId())) {
			LOGGER.info("Not adding metrics for NULL or blank source");
			return;
		}
		
		LOGGER.debug("sourceId = {}", source.getId());
		
		String keyPrefix = source.getId() + ".";
		
		// Create source-specific metrics for this source
		// (Must be done prior to creating metrics collector so that
		// JMX MBean exists for collector to detect).
		String key = keyPrefix + QUERIES_TOTAL_RESULTS_SCOPE;
		
		// Do not create metric and collector if they already exist for this source.
		// (This can happen for ConnectedSources because they have the same sourceId
		// as the local catalog provider).
		if (!metrics.containsKey(key)) {
			Histogram resultCount = metricsRegistry.histogram(MetricRegistry.name(source.getId(), QUERIES_SCOPE,
	                "TotalResults"));
			String pid = createMetricsCollector(source.getId(), QUERIES_TOTAL_RESULTS_SCOPE);
			metrics.put(key, new SourceMetric(resultCount, pid, true));
		} else {
			LOGGER.debug("Metric " + key + " already exists - not creating again");
		}

		key = keyPrefix + QUERIES_SCOPE;
		if (!metrics.containsKey(key)) {
			Meter queries = metricsRegistry.meter(MetricRegistry.name(source.getId(), QUERIES_SCOPE));
			String pid = createMetricsCollector(source.getId(), QUERIES_SCOPE);
			metrics.put(key, new SourceMetric(queries, pid));
		} else {
			LOGGER.debug("Metric " + key + " already exists - not creating again");
		}
		
		key = keyPrefix + EXCEPTIONS_SCOPE;
		if (!metrics.containsKey(key)) {
			Meter exceptions = metricsRegistry.meter(MetricRegistry.name(source.getId(), EXCEPTIONS_SCOPE));
			String pid = createMetricsCollector(source.getId(), EXCEPTIONS_SCOPE);
			metrics.put(key, new SourceMetric(exceptions, pid));
		} else {
			LOGGER.debug("Metric " + key + " already exists - not creating again");
		}
		
		LOGGER.trace("EXITING: addingService");
	}
	
    public void deletingSource( Source source, Map props ) {
    	LOGGER.trace("ENTERING: deletingService");
    	
    	if (source == null) return;
    	
    	LOGGER.debug("sourceId = {},    props = {}", source.getId(), props);
    	    	
    	String keyPrefix = source.getId() + ".";
    	
    	String key = keyPrefix + QUERIES_TOTAL_RESULTS_SCOPE;
    	if (metrics.containsKey(key)) {
	    	metricsRegistry.remove(MetricRegistry.name(source.getId(), QUERIES_TOTAL_RESULTS_SCOPE));    	
	    	deleteCollector(source.getId(), QUERIES_TOTAL_RESULTS_SCOPE);
	    	metrics.remove(key);
    	} else {
    		LOGGER.debug("Did not remove metric " + key + " because it was not in metrics map");
    	}
        
    	key = keyPrefix + QUERIES_SCOPE;
    	if (metrics.containsKey(key)) {
	        metricsRegistry.remove(MetricRegistry.name(source.getId(), QUERIES_SCOPE));
	        deleteCollector(source.getId(), QUERIES_SCOPE);
	        metrics.remove(key);
    	} else {
    		LOGGER.debug("Did not remove metric " + key + " because it was not in metrics map");
    	}
        
    	key = keyPrefix + EXCEPTIONS_SCOPE;
    	if (metrics.containsKey(key)) {
	        metricsRegistry.remove(MetricRegistry.name(source.getId(), EXCEPTIONS_SCOPE));
	        deleteCollector(source.getId(), EXCEPTIONS_SCOPE);
	        metrics.remove(key);
    	} else {
    		LOGGER.debug("Did not remove metric " + key + " because it was not in metrics map");
    	}
    	
    	LOGGER.trace("EXITING: deletingService");
	}
    
    private String createMetricsCollector(String sourceId, String collectorName) {
    	LOGGER.trace("ENTERING: createMetricsCollector - sourceId = {}", sourceId);
    	String pid = null;
    	
    	String[] sourceIdParts = sourceId.split("[^a-zA-Z0-9]");
    	String newSourceId = "";
    	for (String part : sourceIdParts) {
    		newSourceId += StringUtils.capitalize(part);
    	}
    	String rrdPath = "source" + newSourceId + collectorName;
    	LOGGER.debug("BEFORE: rrdPath = " + rrdPath);
    	
    	// Sterilize RRD path name by removing any non-alphanumeric characters - this would confuse the
    	// URL being generated for this RRD path in the Metrics tab of Admin console.
    	rrdPath = rrdPath.replaceAll("[^a-zA-Z0-9]", "");
    	rrdPath += ".rrd";
    	LOGGER.debug("AFTER: rrdPath = " + rrdPath);
    	
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
    
    private void deleteCollector(String sourceId, String metricName) {
    	String mapKey = sourceId + "." + metricName;
    	SourceMetric sourceMetric = metrics.get(mapKey);
    	try {
			Configuration config = configurationAdmin.getConfiguration(sourceMetric.getPid());
			if (config != null) {
				LOGGER.debug("Deleting " + metricName + " JmxCollector for source " + sourceId);
				config.delete();
			}
//    		String filter = "(mbeanName=" + MBEAN_PACKAGE_NAME + ":name=" + sourceId + "." + metricName + ")";
//    		LOGGER.debug("filter = " + filter);
//    		Configuration[] configs = configurationAdmin.listConfigurations(filter);
//    		if (configs != null && configs.length == 1) {
//    			LOGGER.debug("Deleting JmxCollector for source " + sourceId);
//    			configs[0].delete();
//    		} else {
//    			LOGGER.debug("Unable to delete " + metricName + " JmxCollector for source " + sourceId);
//    		}
		} catch (IOException e) {
			LOGGER.warn("Unable to delete " + metricName + " JmxCollector for source " + sourceId, e);
//		} catch (InvalidSyntaxException e) {
//			LOGGER.warn("Unable to delete " + metricName + " JmxCollector for source " + sourceId, e);
		}
      metrics.remove(mapKey);
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
    		LOGGER.debug("Metric not found for " + mapKey);
    		return;
    	}
		
    	if (sourceMetric.isHistogram()) {
			Histogram metric = (Histogram) sourceMetric.getMetric();
			LOGGER.debug("Updating histogram metric " + name + " by amount of " + incrementAmount);
			metric.update(incrementAmount);
		} else {
			Meter metric = (Meter) sourceMetric.getMetric();
			LOGGER.debug("Updating metric " + name + " by amount of " + incrementAmount);
			metric.mark(incrementAmount);
		}
	}
    
    
    private class SourceMetric {
    	
    	private Metric metric;
    	private String pid;
    	private boolean isHistogram = false;
    	
    	public SourceMetric(Metric metric, String pid) {
    		this(metric, pid, false);
    	}
    	
    	public SourceMetric(Metric metric, String pid, boolean isHistogram) {
    		this.metric = metric;
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
