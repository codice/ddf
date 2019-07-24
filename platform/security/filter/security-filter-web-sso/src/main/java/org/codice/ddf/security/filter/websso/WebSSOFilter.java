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

import static ddf.security.SecurityConstants.AUTHENTICATION_TOKEN_KEY;

import com.google.common.hash.Hashing;
import ddf.security.SecurityConstants;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.http.SessionFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.codice.ddf.platform.filter.AuthenticationChallengeException;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.HandlerResult.Status;
import org.codice.ddf.security.handler.api.SessionToken;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves as the main security filter that works in conjunction with a number of handlers to protect
 * a variety of contexts each using different authentication schemes and policies. The basic premise
 * is that this filter is installed on any registered http context and it handles delegating the
 * authentication to the specified handlers in order to normalize and consolidate a session token.
 */
public class WebSSOFilter implements SecurityFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(WebSSOFilter.class);

  /** Dynamic list of handlers that are registered to provide authentication services. */
  private List<AuthenticationHandler> handlerList = new ArrayList<>();

  private ContextPolicyManager contextPolicyManager;

  private SessionFactory sessionFactory;

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
   * Provides filtering for every registered http context. Checks for an existing session. If it
   * doesn't exist, it then looks up the current context and determines the proper handlers to
   * include in the chain. Each handler is given the opportunity to locate their specific tokens if
   * they exist or to go off and obtain them. Once a token has been received that we know how to
   * process , we attach them to the request and continue down the chain.
   *
   * @param servletRequest incoming http request
   * @param servletResponse response stream for returning the response
   * @param filterChain chain of filters to be invoked following this filter
   * @throws IOException
   * @throws AuthenticationException
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

    boolean isWhiteListed = false;

    if (contextPolicyManager != null) {
      isWhiteListed = contextPolicyManager.isWhiteListed(path);
    }

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
      LOGGER.debug("Handling request for {}.", path);
      handleRequest(httpRequest, httpResponse, filterChain, getHandlerList(path));
    }
  }

  private void handleRequest(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      FilterChain filterChain,
      List<AuthenticationHandler> handlers)
      throws AuthenticationException, IOException {
    HandlerResult result = null;

    // First pass, see if anyone can come up with proper security token from the get-go
    LOGGER.debug("Checking for existing tokens in request.");

    final String path = httpRequest.getRequestURI();
    String ipAddress = httpRequest.getHeader("X-FORWARDED-FOR");

    if (ipAddress == null) {
      ipAddress = httpRequest.getRemoteAddr();
    }

    if (contextPolicyManager.getSessionAccess()) {
      result = checkForPreviousResultOnSession(httpRequest, ipAddress);
    }

    // no result found on session, try and get result from handlers
    if (result == null) {
      if (!handlers.isEmpty()) {
        result = getResultFromHandlers(httpRequest, httpResponse, filterChain, handlers);
      } else { // no configured handlers
        if (contextPolicyManager.getGuestAccess()) {
          LOGGER.info(
              "No configured handlers found, but guest access is enabled. Continuing with an empty handler result for guest login.");
          result = new HandlerResult(Status.NO_ACTION, null);
          result.setSource("default");
        } else {
          LOGGER.warn(
              "No configured handler found and guest access is disabled. Returning status code 503, Service Unavailable. Check system configuration and bundle state.");
          returnSimpleResponse(HttpServletResponse.SC_SERVICE_UNAVAILABLE, httpResponse);
          return;
        }
      }
    }

    handleResultStatus(httpRequest, httpResponse, result, path, ipAddress);

    // If we got here, we've received our tokens to continue
    LOGGER.debug("Invoking the rest of the filter chain");
    try {
      filterChain.doFilter(httpRequest, httpResponse);
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

  private HandlerResult checkForPreviousResultOnSession(HttpServletRequest httpRequest, String ip) {
    HandlerResult result = null;
    HttpSession session = httpRequest.getSession(false);
    String requestedSessionId = httpRequest.getRequestedSessionId();
    if (requestedSessionId != null && !httpRequest.isRequestedSessionIdValid()) {
      SecurityLogger.audit(
          "Incoming HTTP Request contained possible unknown session ID [{}] for this server.",
          Hashing.sha256().hashString(requestedSessionId, StandardCharsets.UTF_8).toString());
    }
    if (session == null && requestedSessionId != null) {
      session = sessionFactory.getOrCreateSession(httpRequest);
    }
    if (session != null) {
      SecurityTokenHolder savedToken =
          (SecurityTokenHolder) session.getAttribute(SecurityConstants.SECURITY_TOKEN_KEY);
      if (savedToken != null && savedToken.getPrincipals() != null) {
        Collection<SecurityAssertion> assertions =
            savedToken.getPrincipals().byType(SecurityAssertion.class);
        SessionToken sessionToken = null;
        if (!assertions.isEmpty()) {
          sessionToken =
              new SessionToken(savedToken.getPrincipals(), savedToken.getPrincipals(), ip);
        }
        if (sessionToken != null) {
          result = new HandlerResult();
          result.setToken(sessionToken);
          result.setStatus(HandlerResult.Status.COMPLETED);
        } else {
          savedToken.remove();
        }
      } else {
        LOGGER.trace("No principals located in session - returning with no results");
      }
    } else {
      LOGGER.trace("No HTTP Session - returning with no results");
    }
    return result;
  }

  private void handleResultStatus(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      HandlerResult result,
      String path,
      String ipAddress)
      throws AuthenticationChallengeException, AuthenticationFailureException {
    if (result != null) {
      switch (result.getStatus()) {
        case REDIRECTED:
          // handler handled the response - it is redirecting or whatever
          // necessary to get their tokens
          LOGGER.debug("Stopping filter chain - handled by plugins");
          throw new AuthenticationChallengeException("Stopping filter chain - handled by plugins");
        case NO_ACTION:
          if (!contextPolicyManager.getGuestAccess()) {
            LOGGER.warn(
                "No handlers were able to determine required credentials, returning bad request to {}. Check policy configuration for path: {}",
                ipAddress,
                path);
            returnSimpleResponse(HttpServletResponse.SC_BAD_REQUEST, httpResponse);
            throw new AuthenticationFailureException(
                "No handlers were able to determine required credentials");
          }
          result = new HandlerResult(Status.COMPLETED, new GuestAuthenticationToken(ipAddress));
          result.setSource("default");
          // fall through
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
          result.getToken().setAllowGuest(contextPolicyManager.getGuestAccess());
          httpRequest.setAttribute(AUTHENTICATION_TOKEN_KEY, result);
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
  }

  private HandlerResult getResultFromHandlers(
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse,
      FilterChain filterChain,
      List<AuthenticationHandler> handlers)
      throws AuthenticationException {
    HandlerResult result = new HandlerResult();
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
    if (result.getStatus() == HandlerResult.Status.NO_ACTION) {
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
    return result;
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

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }
}
