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
package org.codice.ddf.metrics.servlet;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Metrics;
import java.io.IOException;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.filter.http.HttpFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServletMetrics implements HttpFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServletMetrics.class);

  private static final String METRICS_PREFIX = "ddf.platform.http";

  private static final String HISTOGRAM_NAME = "latency";

  @Override
  public void doFilter(
      HttpServletRequest request, HttpServletResponse response, HttpFilterChain filterChain)
      throws IOException, ServletException {
    boolean hadException = false;
    long startTime = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } catch (Exception ex) {
      hadException = true;
      throw ex;
    } finally {
      if (!hadException && request.isAsyncStarted()) {
        request.getAsyncContext().addListener(new AsyncResponseListener(startTime));
      } else {
        record(request, response, startTime, hadException, false);
      }
    }
  }

  private static void record(
      HttpServletRequest request,
      HttpServletResponse response,
      long startTime,
      boolean hadException,
      boolean hadTimeout) {
    long endTime = System.currentTimeMillis();
    long latency = endTime - startTime;

    DistributionSummary.builder(METRICS_PREFIX + "." + HISTOGRAM_NAME)
        .baseUnit("milliseconds")
        .tags(
            "method",
            request.getMethod(),
            "status",
            getStatusCode(response, hadException, hadTimeout))
        .publishPercentiles(0.5, 0.95)
        .register(Metrics.globalRegistry)
        .record(latency);
  }

  private static String getStatusCode(
      HttpServletResponse response, boolean hadException, boolean hadTimeout) {
    String result = String.valueOf(response.getStatus());
    if (hadException && response.getStatus() != 500) {
      LOGGER.trace(
          "Returned a status code of [{}] but caught exception. Recording status of 500 instead.",
          response.getStatus());
      result = "500";
    }

    if (hadTimeout && response.getStatus() != 408) {
      LOGGER.trace(
          "Returned a status code of [{}] but request timed out. Recording status of 408 instead.",
          response.getStatus());
      result = "408";
    }

    return result;
  }

  private static final class AsyncResponseListener implements AsyncListener {

    private final long startTime;

    private boolean hadException = false;

    private boolean hadTimeout = false;

    public AsyncResponseListener(long startTime) {
      this.startTime = startTime;
    }

    @Override
    public void onComplete(AsyncEvent event) throws IOException {
      record(
          (HttpServletRequest) event.getSuppliedRequest(),
          (HttpServletResponse) event.getSuppliedResponse(),
          startTime,
          hadException,
          hadTimeout);
    }

    @Override
    public void onTimeout(AsyncEvent event) throws IOException {
      hadTimeout = true;
    }

    @Override
    public void onError(AsyncEvent event) throws IOException {
      hadException = true;
    }

    @Override
    public void onStartAsync(AsyncEvent event) throws IOException {
      LOGGER.debug("Started async listen for servlet metrics");
    }
  }
}
