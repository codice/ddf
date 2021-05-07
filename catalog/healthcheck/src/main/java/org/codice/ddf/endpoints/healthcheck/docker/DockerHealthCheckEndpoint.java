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
package org.codice.ddf.endpoints.healthcheck.docker;

import org.codice.ddf.endpoints.healthcheck.HealthChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/** Docker Health Check */
@Path("/")
public class DockerHealthCheckEndpoint implements DockerHealthCheck {
  private HealthChecker healthChecker;
  private static final Logger LOGGER = LoggerFactory.getLogger(DockerHealthCheckEndpoint.class);

  public DockerHealthCheckEndpoint(HealthChecker healthChecker) {
    this.healthChecker = healthChecker;
  }

  @GET
  @Path("/docker")
  public Response getDocument() {
    LOGGER.info("in /healthcheck");
    return healthChecker.handle();
  }
}
