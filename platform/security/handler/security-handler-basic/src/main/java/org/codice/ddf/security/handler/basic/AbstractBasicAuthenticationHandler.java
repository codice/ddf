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
package org.codice.ddf.security.handler.basic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBasicAuthenticationHandler implements AuthenticationHandler {

  public static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

  public static final String SOURCE = "BasicHandler";

  protected static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractBasicAuthenticationHandler.class);

  @Override
  public abstract String getAuthenticationType();

  /**
   * Processes the incoming request to retrieve the username/password tokens. Handles responding to
   * the client that authentication is needed if they are not present in the request. Returns the
   * {@link org.codice.ddf.security.handler.api.HandlerResult} for the HTTP Request.
   *
   * @param request http request to obtain attributes from and to pass into any local filter chains
   *     required
   * @param response http response to return http responses or redirects
   * @param chain original filter chain (should not be called from your handler)
   * @param resolve flag with true implying that credentials should be obtained, false implying
   *     return if no credentials are found.
   * @return
   */
  @Override
  public HandlerResult getNormalizedToken(
      ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve) {

    String realm = (String) request.getAttribute(ContextPolicy.ACTIVE_REALM);
    HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
    handlerResult.setSource(realm + "-" + SOURCE);

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String path = httpRequest.getServletPath();
    LOGGER.debug("Handling request for path {}", path);

    LOGGER.debug("Doing authentication and authorization for path {}", path);

    BaseAuthenticationToken token = extractAuthenticationInfo(httpRequest);

    // we found credentials, attach to result and return with completed status
    if (token != null) {
      handlerResult.setToken(token);
      handlerResult.setStatus(HandlerResult.Status.COMPLETED);
      return handlerResult;
    }

    // we didn't find the credentials, see if we are to do anything or not
    if (resolve) {
      doAuthPrompt(realm, (HttpServletResponse) response);
      handlerResult.setStatus(HandlerResult.Status.REDIRECTED);
    }

    return handlerResult;
  }

  @Override
  public HandlerResult handleError(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
    String realm = (String) servletRequest.getAttribute(ContextPolicy.ACTIVE_REALM);
    doAuthPrompt(realm, (HttpServletResponse) servletResponse);
    HandlerResult result = new HandlerResult(HandlerResult.Status.REDIRECTED, null);
    result.setSource(realm + "-" + SOURCE);
    LOGGER.debug("In error handler for basic auth - prompted for auth credentials.");
    return result;
  }

  /**
   * Return a 401 response back to the web browser to prompt for basic auth.
   *
   * @param realm
   * @param response
   */
  private void doAuthPrompt(String realm, HttpServletResponse response) {
    try {
      response.setHeader(
          HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + realm + "\"");
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentLength(0);
      response.flushBuffer();
    } catch (IOException ioe) {
      LOGGER.debug("Failed to send auth response: {}", ioe);
    }
  }

  protected BaseAuthenticationToken extractAuthenticationInfo(HttpServletRequest request) {

    String realm = (String) request.getAttribute(ContextPolicy.ACTIVE_REALM);
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

    if (StringUtils.isEmpty(authHeader)) {
      return null;
    }

    return extractAuthInfo(authHeader, realm);
  }

  /**
   * Extract the Authorization header and parse into a username/password token.
   *
   * @param authHeader the authHeader string from the HTTP request
   * @return the initialized UPAuthenticationToken for this username, password, realm combination
   *     (or null)
   */
  protected BaseAuthenticationToken extractAuthInfo(String authHeader, String realm) {
    BaseAuthenticationToken token = null;
    authHeader = authHeader.trim();
    String[] parts = authHeader.split(" ");
    if (parts.length == 2) {
      String authType = parts[0];
      String authInfo = parts[1];

      if (authType.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
        byte[] decode = Base64.getDecoder().decode(authInfo);
        if (decode != null) {
          String userPass = new String(decode, StandardCharsets.UTF_8);
          String[] authComponents = userPass.split(":");
          if (authComponents.length == 2) {
            token = getBaseAuthenticationToken(realm, authComponents[0], authComponents[1]);
          } else if ((authComponents.length == 1) && (userPass.endsWith(":"))) {
            token = getBaseAuthenticationToken(realm, authComponents[0], "");
          }
        }
      }
    }
    return token;
  }

  protected abstract BaseAuthenticationToken getBaseAuthenticationToken(
      String realm, String username, String password);
}
