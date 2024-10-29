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
package ddf.catalog.impl.operations;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.opengis.filter.Filter;

public class QueryOperationsTest {

  @Test
  public void testCollectQueryMetrics() throws NullPointerException {
    Map<String, Serializable> reqMetrics = new HashMap<>();
    Map<String, Serializable> respMetrics = new HashMap<>();
    Map<String, Serializable> noQueryMetrics = new HashMap<>();
    Map<String, Serializable> noTraceIdMetrics = new HashMap<>();
    Map<String, Serializable> nullTraceIdMetrics = new HashMap<>();

    reqMetrics.put(QueryOperations.QM_TRACEID, "12345");
    reqMetrics.put(QueryOperations.QM_PREQUERY + "metric1" + QueryOperations.QM_ELAPSED, "23456");
    reqMetrics.put(QueryOperations.QM_PREQUERY + "metric2" + QueryOperations.QM_ELAPSED, "34567");

    respMetrics.put(QueryOperations.QM_TRACEID, "ABCDEF");
    respMetrics.put(QueryOperations.QM_POSTQUERY + "metric1" + QueryOperations.QM_ELAPSED, "45678");
    respMetrics.put(QueryOperations.QM_POSTQUERY + "metric2" + QueryOperations.QM_ELAPSED, "56789");
    respMetrics.put(QueryOperations.QM_POSTQUERY + "metric3" + QueryOperations.QM_ELAPSED, "98765");

    Map<String, Serializable> metrics = QueryOperations.collectQueryProperties(null, null);
    assertNotNull(metrics);
    assertThat(metrics.size(), is(0));
    String metricsString = QueryOperations.serializeMetrics(metrics, QueryOperations.QMB);
    assertThat(metricsString, is("trace-id: null"));

    metrics = QueryOperations.collectQueryProperties(reqMetrics, null);
    assertNotNull(metrics);
    assertThat(metrics.size(), is(reqMetrics.size()));
    metricsString = QueryOperations.serializeMetrics(metrics, QueryOperations.QMB);
    assertThat(metricsString, containsString("trace-id: 12345"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_PREQUERY + "metric1" + QueryOperations.QM_ELAPSED + ": 23456"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_PREQUERY + "metric2" + QueryOperations.QM_ELAPSED + ": 34567"));

    metrics = QueryOperations.collectQueryProperties(null, respMetrics);
    assertNotNull(metrics);
    assertThat(metrics.size(), is(respMetrics.size()));
    metricsString = QueryOperations.serializeMetrics(metrics, QueryOperations.QMB);
    assertThat(metricsString, containsString("trace-id: ABCDEF"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_POSTQUERY + "metric1" + QueryOperations.QM_ELAPSED + ": 45678"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_POSTQUERY + "metric2" + QueryOperations.QM_ELAPSED + ": 56789"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_POSTQUERY + "metric3" + QueryOperations.QM_ELAPSED + ": 98765"));

    metrics = QueryOperations.collectQueryProperties(reqMetrics, respMetrics);
    assertNotNull(metrics);
    // trace-id from each is merged into 1
    assertThat(metrics.size(), is(reqMetrics.size() + respMetrics.size() - 1));
    metricsString = QueryOperations.serializeMetrics(metrics, QueryOperations.QMB);
    assertThat(metricsString, containsString("trace-id: ABCDEF"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_PREQUERY + "metric1" + QueryOperations.QM_ELAPSED + ": 23456"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_PREQUERY + "metric2" + QueryOperations.QM_ELAPSED + ": 34567"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_POSTQUERY + "metric1" + QueryOperations.QM_ELAPSED + ": 45678"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_POSTQUERY + "metric2" + QueryOperations.QM_ELAPSED + ": 56789"));
    assertThat(
        metricsString,
        containsString(
            QueryOperations.QM_POSTQUERY + "metric3" + QueryOperations.QM_ELAPSED + ": 98765"));
  }

  @Test
  public void testLogQueryMetrics() throws NullPointerException {
    QueryResponse queryResponse =
        new QueryResponseImpl(new QueryRequestImpl(new QueryImpl(Filter.INCLUDE)));
    Map<String, Serializable> respProps = new HashMap<>();

    respProps.put(QueryOperations.QM_TRACEID, "12345");
    respProps.put(QueryOperations.QM_PREQUERY + "metric" + QueryOperations.QM_ELAPSED, "23456");
    respProps.put(QueryOperations.QM_DO_QUERY + "metric" + QueryOperations.QM_ELAPSED, "23456");
    respProps.put(
        QueryOperations.QM_TOTAL_ELAPSED + "metric" + QueryOperations.QM_ELAPSED, "523456");
    respProps.put(QueryOperations.QM_POSTQUERY + "metric" + QueryOperations.QM_ELAPSED, "45678");
    respProps.put("metrics-enabled", true);
    respProps.put("additional-query-metrics", new HashMap<>());

    queryResponse.getProperties().putAll(respProps);
    String queryMetricsLog = QueryOperations.getQueryMetricsLog(queryResponse.getProperties());
    assertNotNull(queryMetricsLog);
  }
}
