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
package ddf.catalog.source;

/**
 * The interface {@link SourceMetrics} used by the {@link FederationStrategy} to update metrics on
 * individual {@link Source}s as queries and exceptions occur when the {@link Source} is accessed.
 * 
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 * 
 * @author rodgersh
 * 
 */
public interface SourceMetrics {

    /**
     * Package name for the JMX MBean where metrics for {@link Source}s are stored.
     */
    public static final String MBEAN_PACKAGE_NAME = "ddf.metrics.catalog.source";

    /**
     * Name of the JMX MBean scope for source-level metrics tracking exceptions while querying a
     * specific {@link Source}
     */
    public static final String EXCEPTIONS_SCOPE = "Exceptions";

    /**
     * Name of the JMX MBean scope for source-level metrics tracking query count while querying a
     * specific {@link Source}
     */
    public static final String QUERIES_SCOPE = "Queries";

    /**
     * Name of the JMX MBean scope for source-level metrics tracking total results returned while
     * querying a specific {@link Source}
     */
    public static final String QUERIES_TOTAL_RESULTS_SCOPE = "Queries.TotalResults";

    /**
     * Update a source-level metric.
     * 
     * @param sourceId
     *            ID of the {@link Source} to update metrics for
     * @param metricName
     *            name of the metric to update
     * @param incrementAmount
     *            amount to increment the metric's count by
     */
    public void updateMetric(String sourceId, String metricName, int incrementAmount);

}
