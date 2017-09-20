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
package ddf.metrics.reporting.internal;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Exception thrown when a {@link ddf.metrics.reporting.internal.rest.MetricsEndpoint} encounters
 * problems during its graphing.
 *
 * @since 2.1.0
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class MetricsEndpointException extends WebApplicationException {
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = 1L;

  public MetricsEndpointException(String message, Status status) {
    super(Response.status(status).entity(message).build());
  }

  public MetricsEndpointException(Throwable cause) {
    super(cause);
  }
}
