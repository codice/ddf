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

import java.io.IOException;
import java.util.Map;
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
import org.codice.ddf.security.handler.api.OidcHandlerConfiguration;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.Pac4jConstants;
import org.pac4j.core.context.session.J2ESessionStore;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.http.callback.QueryParameterCallbackUrlResolver;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.credentials.OidcCredentials;
import org.pac4j.oidc.credentials.extractor.OidcExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OidcHandler implements AuthenticationHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(OidcHandler.class);

  private static final String SOURCE = "OidcHandler";
  private static final String AUTH_TYPE = "OIDC";

  private static HandlerResult noActionResult;
  private static HandlerResult redirectedResult;

  static {
    noActionResult = new HandlerResult(Status.NO_ACTION, null);
    noActionResult.setSource(SOURCE);

    redirectedResult = new HandlerResult(Status.REDIRECTED, null);
    redirectedResult.setSource(SOURCE);
  }

  private OidcHandlerConfiguration configuration;

  public OidcHandler(OidcHandlerConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }

  /**
   * Handler implementing OIDC authentication.
   *
   * @param request http request to obtain attributes from and to pass into any local filter chains
   *     required
   * @param response http response to return http responses or redirects
   * @param chain original filter chain (should not be called from your handler)
   * @param resolve flag with true implying that credentials should be obtained, false implying
   *     return if no credentials are found.
   * @return result of handling this request - status and optional tokens
   * @throws AuthenticationFailureException
   */
  @Override
  public HandlerResult getNormalizedToken(
      ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve)
      throws AuthenticationFailureException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;
    if (httpRequest.getMethod().equals("HEAD")) {
      return processHeadRequest(httpResponse);
    }

    LOGGER.debug(
        "Doing Oidc authentication and authorization for path {}.", httpRequest.getContextPath());

    J2ESessionStore sessionStore = new J2ESessionStore();
    J2EContext j2EContext = new J2EContext(httpRequest, httpResponse, sessionStore);

    StringBuffer requestUrlBuffer = httpRequest.getRequestURL();
    requestUrlBuffer.append(
        httpRequest.getQueryString() == null ? "" : "?" + httpRequest.getQueryString());
    String requestUrl = requestUrlBuffer.toString();
    String ipAddress = httpRequest.getRemoteAddr();

    OidcClient oidcClient = configuration.getOidcClient(requestUrl);

    OidcCredentials credentials;
    boolean isMachine = userAgentIsNotBrowser(httpRequest);
    if (isMachine) {
      LOGGER.debug(
          "The Oidc Handler does not handle machine to machine requests. Continuing to other handlers.");
      return noActionResult;
    } else { // check for Authorization Code Flow, Implicit Flow, or Hybrid Flow credentials
      try {
        credentials = getCredentialsFromRequest(oidcClient, j2EContext);
      } catch (IllegalArgumentException e) {
        LOGGER.debug(e.getMessage(), e);
        LOGGER.error(
            "Problem with the Oidc Handler's configuration. "
                + "Check the Oidc Handler configuration in the admin console.");
        return noActionResult;
      } catch (TechnicalException e) {
        LOGGER.debug("Problem extracting Oidc credentials from incoming user request.", e);
        return redirectForCredentials(oidcClient, j2EContext, requestUrl);
      }
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
    } else { // the user agent request didn't have credentials, redirect and go get some
      LOGGER.info(
          "No credentials found on user-agent request. "
              + "Redirecting user-agent to IdP for credentials.");
      return redirectForCredentials(oidcClient, j2EContext, requestUrl);
    }
  }

  @Override
  public HandlerResult handleError(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
    LOGGER.debug("In error handler for Oidc - no action taken.");
    return noActionResult;
  }

  public OidcHandlerConfiguration getConfiguration() {
    return configuration;
  }

  private HandlerResult processHeadRequest(HttpServletResponse httpResponse)
      throws AuthenticationFailureException {
    httpResponse.setStatus(HttpServletResponse.SC_OK);
    try {
      httpResponse.flushBuffer();
    } catch (IOException e) {
      throw new AuthenticationFailureException(
          "Unable to send response to HEAD message from OIDC client.");
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

  private OidcCredentials getCredentialsFromRequest(OidcClient oidcClient, J2EContext j2EContext) {
    // Check that the request contains a code, an access token or an id token
    Map<String, String[]> requestParams = j2EContext.getRequestParameters();
    if (!requestParams.containsKey("code")
        && !requestParams.containsKey("access_token")
        && !requestParams.containsKey("id_token")) {
      return new OidcCredentials();
    }
    oidcClient.setCallbackUrlResolver(new QueryParameterCallbackUrlResolver());

    OidcExtractor oidcExtractor = new OidcExtractor(oidcClient.getConfiguration(), oidcClient);
    return oidcExtractor.extract(j2EContext);
  }

  private HandlerResult redirectForCredentials(
      OidcClient oidcClient, J2EContext j2EContext, String requestUrl) {
    j2EContext.getSessionStore().set(j2EContext, Pac4jConstants.REQUESTED_URL, requestUrl);
    oidcClient.redirect(j2EContext);
    return redirectedResult;
  }
}
