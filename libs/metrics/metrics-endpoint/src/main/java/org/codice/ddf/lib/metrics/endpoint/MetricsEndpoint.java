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
package org.codice.ddf.lib.metrics.endpoint;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.Validate;
import org.codice.ddf.lib.metrics.registry.MeterRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Endpoint which exposes Micrometer metrics. */
@Path("/")
public class MetricsEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetricsEndpoint.class);
  private final MeterRegistryService meterRegistryService;

  /**
   * Creates a new MetricsEndpoint.
   *
   * @param meterRegistryService service for accessing the {@link
   *     io.micrometer.core.instrument.MeterRegistry}
   */
  public MetricsEndpoint(MeterRegistryService meterRegistryService) {
    LOGGER.debug("Starting Metrics endpoint...");
    Validate.notNull(meterRegistryService, "Argument meterRegistryService cannot be null");
    this.meterRegistryService = meterRegistryService;
  }

  @GET
  public Response getMetrics(
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    PrometheusMeterRegistry metrics =
        (PrometheusMeterRegistry) meterRegistryService.getMeterRegistry();
    return Response.status(200).entity(metrics.scrape()).type(MediaType.TEXT_PLAIN).build();
  }
}
