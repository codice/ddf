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
package org.codice.ddf.security.filter.websso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.AuthenticationChallengeException;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.InvalidSAMLReceivedException;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves as the main security filter that works in conjunction with a number of handlers to protect
 * a variety of contexts each using different authentication schemes and policies. The basic premise
 * is that this filter is installed on any registered http context and it handles delegating the
 * authentication to the specified handlers in order to normalize and consolidate a session token
 * (the SAML assertion).
 */
public class WebSSOFilter implements SecurityFilter {

  public static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSSOFilter.class);

  /** Dynamic list of handlers that are registered to provide authentication services. */
  List<AuthenticationHandler> handlerList = new ArrayList<>();

  ContextPolicyManager contextPolicyManager;

  @Override
  public void init() {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("handlerList size is {}", handlerList.size());

      for (AuthenticationHandler authenticationHandler : handlerList) {
        LOGGER.debug(
            "AuthenticationHandler type: {}, class: {}",
            authenticationHandler.getAuthenticationType(),
            authenticationHandler.getClass().getSimpleName());
      }
    }
  }

  /**
   * Provides filtering for every registered http context. Checks for an existing session (via the
   * SAML assertion included as a cookie). If it doesn't exist, it then looks up the current context
   * and determines the proper handlers to include in the chain. Each handler is given the
   * opportunity to locate their specific tokens if they exist or to go off and obtain them. Once a
   * token has been received that we know how to convert to a SAML assertion, we attach them to the
   * request and continue down the chain.
   *
   * @param servletRequest incoming http request
   * @param servletResponse response stream for returning the response
   * @param filterChain chain of filters to be invoked following this filter
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, AuthenticationException {
    LOGGER.debug("Performing doFilter() on WebSSOFilter");
    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

    final String path = httpRequest.getRequestURI();

    LOGGER.debug("Handling request for path {}", path);

    String realm = BaseAuthenticationToken.DEFAULT_REALM;
    boolean isWhiteListed = false;

    if (contextPolicyManager != null) {
      ContextPolicy policy = contextPolicyManager.getContextPolicy(path);
      if (policy != null) {
        realm = policy.getRealm();
      }

      isWhiteListed = contextPolicyManager.isWhiteListed(path);
    }

    // set this so the login filter can easily determine the realm
    servletRequest.setAttribute(ContextPolicy.ACTIVE_REALM, realm);

    if (isWhiteListed) {
      LOGGER.debug(
          "Context of {} has been whitelisted, adding a NO_AUTH_POLICY attribute to the header.",
          path);
      servletRequest.setAttribute(ContextPolicy.NO_AUTH_POLICY, true);
      filterChain.doFilter(httpRequest, httpResponse);
    } else {
      // make sure request didn't come in with NO_AUTH_POLICY set
      servletRequest.setAttribute(ContextPolicy.NO_AUTH_POLICY, null);

      // now handle the request and set the authentication token
      LOGGER.debug("Handling request for {} in security realm {}.", path, realm);
      handleRequest(httpRequest, httpResponse, filterChain, getHandlerList(path));
    }
  }

  private void handleRequest(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      FilterChain filterChain,
      List<AuthenticationHandler> handlers)
      throws AuthenticationException, IOException {

    if (handlers.size() == 0) {
      LOGGER.warn(
          "Handlers not ready. Returning status code 503, Service Unavailable. Check system configuration and bundle state.");
      returnSimpleResponse(HttpServletResponse.SC_SERVICE_UNAVAILABLE, httpResponse);
      return;
    }

    // First pass, see if anyone can come up with proper security token from the get-go
    HandlerResult result = null;
    LOGGER.debug("Checking for existing tokens in request.");

    for (AuthenticationHandler auth : handlers) {
      result = auth.getNormalizedToken(httpRequest, httpResponse, filterChain, false);
      if (result.getStatus() != HandlerResult.Status.NO_ACTION) {
        LOGGER.debug(
            "Handler {} set the result status to {}",
            auth.getAuthenticationType(),
            result.getStatus());
        break;
      }
    }

    // If we haven't received usable credentials yet, go get some
    if (result == null || result.getStatus() == HandlerResult.Status.NO_ACTION) {
      LOGGER.debug("First pass with no tokens found - requesting tokens");
      // This pass, tell each handler to do whatever it takes to get a SecurityToken
      for (AuthenticationHandler auth : handlers) {
        result = auth.getNormalizedToken(httpRequest, httpResponse, filterChain, true);
        if (result.getStatus() != HandlerResult.Status.NO_ACTION) {
          LOGGER.debug(
              "Handler {} set the result status to {}",
              auth.getAuthenticationType(),
              result.getStatus());
          break;
        }
      }
    }

    final String path = httpRequest.getRequestURI();
    String ipAddress = httpRequest.getHeader("X-FORWARDED-FOR");

    if (ipAddress == null) {
      ipAddress = httpRequest.getRemoteAddr();
    }

    if (result != null) {
      switch (result.getStatus()) {
        case REDIRECTED:
          // handler handled the response - it is redirecting or whatever
          // necessary to get their tokens
          LOGGER.debug("Stopping filter chain - handled by plugins");
          throw new AuthenticationChallengeException("Stopping filter chain - handled by plugins");
        case NO_ACTION:
          // should never occur - one of the handlers should have returned a token
          LOGGER.warn(
              "No handlers were able to determine required credentials, returning bad request to {}. Check policy configuration for path: {}",
              ipAddress,
              path);
          returnSimpleResponse(HttpServletResponse.SC_BAD_REQUEST, httpResponse);
          throw new AuthenticationFailureException(
              "No handlers were able to determine required credentials");
        case COMPLETED:
          if (result.getToken() == null) {
            LOGGER.warn(
                "Completed without credentials for {} - check context policy configuration for path: {}",
                ipAddress,
                path);
            returnSimpleResponse(HttpServletResponse.SC_BAD_REQUEST, httpResponse);
            throw new AuthenticationFailureException("Completed without credentials");
          }
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                "Attaching result handler to the http request - token is instance of {} from classloader {}",
                result.getToken().getClass().getName(),
                result.getToken().getClass().getClassLoader());
          }
          httpRequest.setAttribute(DDF_AUTHENTICATION_TOKEN, result);
          break;
        default:
          LOGGER.warn(
              "Unexpected response from handler - ignoring. Remote IP: {}, Path: {}",
              ipAddress,
              path);
          throw new AuthenticationFailureException("Unexpected response from handler");
      }
    } else {
      LOGGER.warn(
          "Expected login credentials from {} - didn't find any. Returning a bad request for path: {}",
          ipAddress,
          path);
      returnSimpleResponse(HttpServletResponse.SC_BAD_REQUEST, httpResponse);
      throw new AuthenticationFailureException("Didn't find any login credentials");
    }

    // If we got here, we've received our tokens to continue
    LOGGER.debug("Invoking the rest of the filter chain");
    try {
      filterChain.doFilter(httpRequest, httpResponse);
    } catch (InvalidSAMLReceivedException e) {
      // we tried to process an invalid or missing SAML assertion
      returnSimpleResponse(HttpServletResponse.SC_UNAUTHORIZED, httpResponse);
      throw new AuthenticationFailureException(e);
    } catch (Exception e) {
      LOGGER.debug(
          "Exception in filter chain - passing off to handlers. Msg: {}", e.getMessage(), e);

      // First pass, see if anyone can come up with proper security token
      // from the git-go
      result = null;
      for (AuthenticationHandler auth : handlers) {
        result = auth.handleError(httpRequest, httpResponse, filterChain);
        if (result.getStatus() != HandlerResult.Status.NO_ACTION) {
          LOGGER.debug(
              "Handler {} set the status to {}", auth.getAuthenticationType(), result.getStatus());
          break;
        }
      }
      if (result == null || result.getStatus() == HandlerResult.Status.NO_ACTION) {
        LOGGER.debug(
            "Error during authentication - no error recovery attempted - returning bad request.");
        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
        httpResponse.flushBuffer();
      }
      throw new AuthenticationFailureException(e);
    }
  }

  private List<AuthenticationHandler> getHandlerList(String path) {
    List<AuthenticationHandler> handlers = new ArrayList<>();
    String handlerAuthMethod;
    if (contextPolicyManager != null) {
      ContextPolicy policy = contextPolicyManager.getContextPolicy(path);
      if (policy != null) {
        Collection<String> authMethods = policy.getAuthenticationMethods();
        for (String authMethod : authMethods) {
          for (AuthenticationHandler handler : this.handlerList) {
            handlerAuthMethod = handler.getAuthenticationType();
            LOGGER.trace(
                "Handler auth method: {} - desired auth method {}", handlerAuthMethod, authMethod);
            if (handler.getAuthenticationType().equalsIgnoreCase(authMethod)) {
              handlers.add(handler);
            }
          }
        }
      }
    } else {
      // if no manager, get a list of all the handlers.
      handlers.addAll(this.handlerList);
    }
    LOGGER.trace(
        "Returning {} handlers that support desired auth methods for path {}",
        handlers.size(),
        path);
    return handlers;
  }

  /**
   * Sends the given response code back to the caller.
   *
   * @param code HTTP response code for this request
   * @param response the servlet response object
   */
  private void returnSimpleResponse(int code, HttpServletResponse response) {
    try {
      LOGGER.debug("Sending response code {}", code);
      response.setStatus(code);
      if (code >= 400) {
        response.sendError(code);
      } else {
        response.setContentLength(0);
      }
      response.flushBuffer();
    } catch (IOException ioe) {
      LOGGER.debug("Failed to send auth response", ioe);
    }
  }

  @Override
  public void destroy() {}

  @Override
  public String toString() {
    return WebSSOFilter.class.getName();
  }

  public List<AuthenticationHandler> getHandlerList() {
    return handlerList;
  }

  public void setHandlerList(List<AuthenticationHandler> handlerList) {
    this.handlerList = handlerList;
  }

  public ContextPolicyManager getContextPolicyManager() {
    return contextPolicyManager;
  }

  public void setContextPolicyManager(ContextPolicyManager contextPolicyManager) {
    this.contextPolicyManager = contextPolicyManager;
  }
}
