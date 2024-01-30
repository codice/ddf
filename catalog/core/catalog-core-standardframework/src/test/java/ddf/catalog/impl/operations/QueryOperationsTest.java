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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

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

    noQueryMetrics.put("some.metrics", "123");
    noQueryMetrics.put("other.metrics", "456");

    noTraceIdMetrics.put(
        QueryOperations.QM_POSTQUERY + "metric1" + QueryOperations.QM_ELAPSED, "45678");
    noTraceIdMetrics.put(
        QueryOperations.QM_POSTQUERY + "metric2" + QueryOperations.QM_ELAPSED, "56789");
    noTraceIdMetrics.put(
        QueryOperations.QM_POSTQUERY + "metric3" + QueryOperations.QM_ELAPSED, "98765");

    nullTraceIdMetrics.put(QueryOperations.QM_TRACEID, null);
    nullTraceIdMetrics.put(
        QueryOperations.QM_POSTQUERY + "metric1" + QueryOperations.QM_ELAPSED, "45678");
    nullTraceIdMetrics.put(
        QueryOperations.QM_POSTQUERY + "metric2" + QueryOperations.QM_ELAPSED, "56789");
    nullTraceIdMetrics.put(
        QueryOperations.QM_POSTQUERY + "metric3" + QueryOperations.QM_ELAPSED, "98765");

    Map<String, Serializable> metrics = QueryOperations.collectQueryMetrics(null, null);
    assertNotNull(metrics);
    assertThat(metrics.size(), is(0));
    String metricsString = QueryOperations.serializeMetrics(metrics, QueryOperations.QMB);
    assertThat(metricsString, is("trace-id: null"));

    metrics = QueryOperations.collectQueryMetrics(noQueryMetrics, null);
    assertNotNull(metrics);
    assertThat(metrics.size(), is(0));

    metrics = QueryOperations.collectQueryMetrics(noTraceIdMetrics, null);
    assertNotNull(metrics);

    metrics = QueryOperations.collectQueryMetrics(nullTraceIdMetrics, null);
    assertNotNull(metrics);
    assertThat(metrics.get(QueryOperations.QM_TRACEID), is(QueryOperations.NIL_UUID));

    metrics = QueryOperations.collectQueryMetrics(reqMetrics, null);
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

    metrics = QueryOperations.collectQueryMetrics(null, respMetrics);
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

    metrics = QueryOperations.collectQueryMetrics(reqMetrics, respMetrics);
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
}
