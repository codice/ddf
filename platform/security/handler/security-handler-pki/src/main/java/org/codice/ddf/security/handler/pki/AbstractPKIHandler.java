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
package org.codice.ddf.security.handler.pki;

import java.security.cert.X509Certificate;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPKIHandler implements AuthenticationHandler {

  public static final String SOURCE = "PKIHandler";

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractPKIHandler.class);

  protected PKIAuthenticationTokenFactory tokenFactory;

  protected CrlChecker crlChecker;

  public AbstractPKIHandler() {
    crlChecker = new CrlChecker();
  }

  @Override
  public abstract String getAuthenticationType();

  /**
   * Handler implementing PKI authentication. Returns the {@link
   * org.codice.ddf.security.handler.api.HandlerResult} containing a BinarySecurityToken if the
   * operation was successful.
   *
   * @param request http request to obtain attributes from and to pass into any local filter chains
   *     required
   * @param response http response to return http responses or redirects
   * @param chain original filter chain (should not be called from your handler)
   * @param resolve flag with true implying that credentials should be obtained, false implying
   *     return if no credentials are found.
   * @return result of handling this request - status and optional tokens
   */
  @Override
  public HandlerResult getNormalizedToken(
      ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve) {
    String realm = (String) request.getAttribute(ContextPolicy.ACTIVE_REALM);
    HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
    handlerResult.setSource(realm + "-" + SOURCE);

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String path = httpRequest.getServletPath();
    LOGGER.debug("Doing PKI authentication and authorization for path {}", path);

    // doesn't matter what the resolve flag is set to, we do the same action
    BaseAuthenticationToken token =
        extractAuthenticationInfo(
            realm,
            (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate"));

    X509Certificate[] certs =
        (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

    HttpServletResponse httpResponse =
        response instanceof HttpServletResponse ? (HttpServletResponse) response : null;

    // The httpResponse was null, return no action and try to process with other handlers
    if (httpResponse == null && resolve) {
      LOGGER.debug("HTTP Response was null for request {}", path);
      return handlerResult;
    }

    // No auth info was extracted, return NO_ACTION
    if (token == null) {
      return handlerResult;
    }

    // CRL was specified, check against CRL and return the result or throw a ServletException to the
    // WebSSOFilter
    if (crlChecker.passesCrlCheck(certs)) {
      handlerResult.setToken(token);
      handlerResult.setStatus(HandlerResult.Status.COMPLETED);
    } else {

      if (httpResponse == null) {
        LOGGER.error(
            "Error returning revoked certificate request because the HTTP response object is invalid.");
      } else {

        try {
          httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Your certificate is revoked.");
          httpResponse.flushBuffer();
          LOGGER.info("The certificate used to complete the request has been revoked.");
        } catch (Exception e) {
          LOGGER.error("Error returning revoked certificate request.");
        }
      }
      handlerResult.setStatus(HandlerResult.Status.REDIRECTED);
    }

    return handlerResult;
  }

  @Override
  public HandlerResult handleError(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) {
    String realm = (String) servletRequest.getAttribute(ContextPolicy.ACTIVE_REALM);
    HandlerResult result = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
    result.setSource(realm + "-" + SOURCE);
    LOGGER.debug("In error handler for pki - no action taken.");
    return result;
  }

  protected abstract BaseAuthenticationToken extractAuthenticationInfo(
      String realm, X509Certificate[] certs);

  public void setTokenFactory(PKIAuthenticationTokenFactory factory) {
    tokenFactory = factory;
  }
}
