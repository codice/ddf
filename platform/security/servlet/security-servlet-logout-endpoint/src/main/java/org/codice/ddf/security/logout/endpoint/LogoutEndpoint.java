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
package org.codice.ddf.security.logout.endpoint;

import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.codice.ddf.security.logout.service.LogoutService;

@Path("/")
public class LogoutEndpoint {

  private LogoutService logoutService;

  public LogoutEndpoint(LogoutService logoutService) {
    this.logoutService = logoutService;
  }

  @GET
  @Path("/actions")
  public Response getActionProviders(
      @Context HttpServletRequest request, @Context HttpServletResponse response)
      throws SecurityServiceException {

    String jsonString = logoutService.getActionProviders(request, response);

    return Response.ok(new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8)))
        .header("Cache-Control", "no-cache, no-store")
        .header("Pragma", "no-cache")
        .build();
  }
}
