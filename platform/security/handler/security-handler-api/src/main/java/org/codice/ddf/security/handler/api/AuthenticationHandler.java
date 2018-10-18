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
package org.codice.ddf.security.handler.api;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.FilterChain;

public interface AuthenticationHandler {

  /**
   * Authentication type String used to match this handler with the auth types configured for a
   * specific context.
   *
   * @return String representing the authentication type
   */
  String getAuthenticationType();

  /**
   * Determine if all the required information exists in the request to generate a token and move on
   * to perform authentication and/or authorization for the requested context. If 'resolve' is set
   * to false and the required information is missing, do not attempt to obtain it and return a
   * status of NO_ACTION. If 'resolve' is set to true and the required information is missing, do
   * whatever it takes to obtain it (redirects, apply your own filters, etc.) and return a status of
   * REDIRECTED. In any case, if the required credentials are present (including the successful
   * conclusion of any redirects, etc.) place the credentials into the HandlerResult and return a
   * status of COMPLETED.
   *
   * @param request http request to obtain attributes from and to pass into any local filter chains
   *     required
   * @param response http response to return http responses or redirects
   * @param chain original filter chain (should not be called from your handler)
   * @param resolve flag with true implying that credentials should be obtained, false implying
   *     return if no credentials are found.
   * @return result containing a status and the credentials to be placed into the http request
   */
  HandlerResult getNormalizedToken(
      ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve)
      throws AuthenticationException;

  /**
   * Called when downstream authentication fails. Should attempt to re-acquire credentials if
   * appropriate. Returns a status indicating if appropriate action has been taken.
   *
   * @param servletRequest htt http response to return http responses or redirects
   * @return result containing a status indicating if further action is necessary
   */
  HandlerResult handleError(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
      throws AuthenticationException;
}
