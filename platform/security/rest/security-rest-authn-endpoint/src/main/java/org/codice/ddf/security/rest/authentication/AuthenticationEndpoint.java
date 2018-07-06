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
package org.codice.ddf.security.rest.authentication;

import ddf.security.service.SecurityServiceException;
import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.codice.ddf.security.rest.authentication.service.AuthenticationService;

@Path("/")
public class AuthenticationEndpoint {

  @Context UriInfo uriInfo;

  private AuthenticationService authenticationService;

  public AuthenticationEndpoint(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @POST
  public Response login(
      @Context HttpServletRequest request,
      @FormParam("username") String username,
      @FormParam("password") String password,
      @FormParam("prevurl") String prevurl)
      throws SecurityServiceException {

    authenticationService.login(request, username, password, prevurl);

    // Redirect to the previous url
    URI redirect = uriInfo.getBaseUriBuilder().replacePath(prevurl).build();
    return Response.seeOther(redirect).build();
  }
}
