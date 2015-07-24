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
package ddf.sdk.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.UnsupportedQueryException;

public class SampleMetrics implements PreQueryPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleMetrics.class);

    private final MetricRegistry metrics = new MetricRegistry();

    private final JmxReporter reporter = JmxReporter.forRegistry(metrics)
            .inDomain("sdk.metrics.sample").build();

    protected final Meter pointRadiusQueries;

    private FilterAdapter filterAdapter;

    public SampleMetrics(FilterAdapter filterAdapter) {
        LOGGER.trace("ENTERING: SampleMetrics constructor");

        this.filterAdapter = filterAdapter;

        // Maps to the MBean's ObjectName, i.e., sdk.metrics.sample:name=Queries.PointRadius
        // NOTE: Also look in the sdk-app project's blueprint.xml file for how the configuration
        // of the JmxCollector for this MBean is setup
        pointRadiusQueries = metrics.meter(MetricRegistry.name("Queries", "PointRadius"));

        reporter.start();

        LOGGER.trace("EXITING: SampleMetrics constructor");
    }

    // Pre-Query plugin
    @Override
    public QueryRequest process(QueryRequest input) throws PluginExecutionException,
        StopProcessingException {
        LOGGER.trace("ENTERING: process");

        // Run the query request through this metric's filter delegate to check if this is
        // a point radius type of query - if it is, mark (increment) the metric's counter, which
        // results in incrementing the MBean's Count attribute
        QueryTypeFilterDelegate queryType = new QueryTypeFilterDelegate();
        try {
            filterAdapter.adapt(input.getQuery(), queryType);
            if (queryType.isPointRadius()) {
                pointRadiusQueries.mark();
            }
        } catch (UnsupportedQueryException e) {
            // ignore filters not supported by the QueryTypeFilterDelegate
        }

        LOGGER.trace("EXITING: process");

        return input;
    }

}
