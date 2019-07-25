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
package org.codice.ddf.security.handler.oidc;

import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.session.J2ESessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OidcCallbackEndpoint {

  private static final Logger LOGGER = LoggerFactory.getLogger(OidcCallbackEndpoint.class);

  @GET
  @Path("/logout")
  public Response logout(
      @Context HttpServletRequest request, @Context HttpServletResponse response) {
    if (request == null) {
      throw new IllegalArgumentException("Passed in request cannot be null.");
    }
    if (response == null) {
      throw new IllegalArgumentException("Passed in response cannot be null.");
    }

    if (request.getSession(false) == null) {
      throw new IllegalArgumentException(
          "Passed in request must have a corresponding session to logout.");
    }

    J2ESessionStore sessionStore = new J2ESessionStore();

    J2EContext j2EContext = new J2EContext(request, response, sessionStore);

    sessionStore.destroySession(j2EContext);

    try {
      return Response.temporaryRedirect(new URI(SystemBaseUrl.EXTERNAL.constructUrl("/logout")))
          .build();
    } catch (URISyntaxException e) {
      LOGGER.debug("Unable to create logout response URL for OIDC logout.", e);
    }
    return Response.serverError().build();
  }
}
