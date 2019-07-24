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
package org.codice.ddf.security.handler.oauth;

import java.io.IOException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.codice.ddf.security.handler.api.OidcAuthenticationToken;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.session.J2ESessionStore;
import org.pac4j.oauth.exception.OAuthCredentialsException;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthHandler implements AuthenticationHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(OAuthHandler.class);

  private static final String SOURCE = "OAuthHandler";
  private static final String AUTH_TYPE = "OIDC"; // OIDC covers both oidc and oauth

  private static HandlerResult noActionResult;

  static {
    noActionResult = new HandlerResult(Status.NO_ACTION, null);
    noActionResult.setSource(SOURCE);
  }

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }

  @Override
  public HandlerResult getNormalizedToken(
      ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve)
      throws AuthenticationFailureException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    if (httpRequest.getMethod().equals("HEAD")) {
      return processHeadRequest(httpResponse);
    }

    J2ESessionStore sessionStore = new J2ESessionStore();
    J2EContext j2EContext = new J2EContext(httpRequest, httpResponse, sessionStore);

    // time to try and pull credentials off of the request
    LOGGER.debug(
        "Doing OAuth authentication and authorization for path {}.", httpRequest.getContextPath());

    OidcCredentials credentials;

    StringBuffer requestUrlBuffer = httpRequest.getRequestURL();
    requestUrlBuffer.append(
        httpRequest.getQueryString() == null ? "" : "?" + httpRequest.getQueryString());
    String ipAddress = httpRequest.getRemoteAddr();

    boolean isMachine = userAgentIsNotBrowser(httpRequest);

    // machine to machine, check for Client Credentials Flow credentials
    if (isMachine) {
      try {
        credentials = getCredentialsFromRequest(j2EContext);
      } catch (IllegalArgumentException e) {
        LOGGER.error(
            "Problem with the OAuth Handler's OAuthHandlerConfiguration. "
                + "Check the OAuth Handler Configuration in the admin console.",
            e);
        return noActionResult;
      } catch (OAuthCredentialsException e) {
        LOGGER.error(
            "Problem extracting credentials from machine to machine request. "
                + "See OAuth2's \"Client Credential Flow\" for more information.",
            e);
        return noActionResult;
      }
    } else {
      LOGGER.info(
          "The OAuth Handler does not handle user agent requests. Continuing to other handlers.");
      return noActionResult;
    }

    // if the request has credentials, process it
    if (credentials.getCode() != null
        || credentials.getAccessToken() != null
        || credentials.getIdToken() != null) {
      LOGGER.info(
          "Oidc credentials found/retrieved. Saving to session and continuing filter chain.");

      OidcAuthenticationToken token =
          new OidcAuthenticationToken(credentials, j2EContext, ipAddress);

      HandlerResult handlerResult = new HandlerResult(Status.COMPLETED, token);
      handlerResult.setSource(SOURCE);
      return handlerResult;
    } else {
      LOGGER.info(
          "No credentials found on user-agent request. "
              + "This handler does not support the acquisition of user agent credentials. Continuing to other handlers.");
      return noActionResult;
    }
  }

  @Override
  public HandlerResult handleError(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
    LOGGER.debug("In error handler for OAuth - no action taken.");
    return noActionResult;
  }

  private HandlerResult processHeadRequest(HttpServletResponse httpResponse)
      throws AuthenticationFailureException {
    httpResponse.setStatus(HttpServletResponse.SC_OK);
    try {
      httpResponse.flushBuffer();
    } catch (IOException e) {
      throw new AuthenticationFailureException(
          "Unable to send response to HEAD message from OAUTH client.");
    }
    return noActionResult;
  }

  private boolean userAgentIsNotBrowser(HttpServletRequest httpRequest) {
    String userAgentHeader = httpRequest.getHeader("User-Agent");
    // basically all browsers support the "Mozilla" way of operating, so they all have "Mozilla"
    // in the string. I just added the rest in case that ever changes for existing browsers.
    // New browsers should contain "Mozilla" as well, though.
    return userAgentHeader == null
        || !(userAgentHeader.contains("Mozilla")
            || userAgentHeader.contains("Safari")
            || userAgentHeader.contains("OPR")
            || userAgentHeader.contains("MSIE")
            || userAgentHeader.contains("Edge")
            || userAgentHeader.contains("Chrome"));
  }

  private OidcCredentials getCredentialsFromRequest(J2EContext j2EContext) {
    CustomOAuthCredentialsExtractor credentialsExtractor = new CustomOAuthCredentialsExtractor();
    return credentialsExtractor.getOauthCredentialsAsOidcCredentials(j2EContext);
  }
}
