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
package org.codice.ddf.metrics.endpoint;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Endpoint which exposes Micrometer metrics in Prometheus format. */
public class PrometheusMetricsEndpoint extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(PrometheusMetricsEndpoint.class);
  private static final PrometheusMeterRegistry REGISTRY =
      new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);

  public PrometheusMetricsEndpoint() {
    LOGGER.debug("Starting Metrics endpoint");
    Metrics.addRegistry(REGISTRY);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    LOGGER.trace("Returning metrics in Prometheus format.");

    resp.setContentType("text/plain");
    try {
      resp.getWriter().print(REGISTRY.scrape());
    } catch (IOException ex) {
      LOGGER.debug("Unable to write metrics out in Prometheus format.", ex);
    }
  }
}
