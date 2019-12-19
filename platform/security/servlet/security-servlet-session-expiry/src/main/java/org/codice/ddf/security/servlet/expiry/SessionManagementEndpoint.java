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
package org.codice.ddf.security.servlet.expiry;

import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.codice.ddf.security.session.management.service.SessionManagementService;

@Path("/")
public class SessionManagementEndpoint {

  private SessionManagementService sessionManagementService;

  public SessionManagementEndpoint(SessionManagementService sessionManagementService) {
    this.sessionManagementService = sessionManagementService;
  }

  @GET
  @Path("/expiry")
  public Response getExpiry(@Context HttpServletRequest request) {
    String body = sessionManagementService.getExpiry(request);
    return Response.ok(body).build();
  }

  @GET
  @Path("/invalidate")
  public Response getInvalidate(@Context HttpServletRequest request) {
    URI uri = sessionManagementService.getInvalidate(request);
    return Response.seeOther(uri).build();
  }
}
