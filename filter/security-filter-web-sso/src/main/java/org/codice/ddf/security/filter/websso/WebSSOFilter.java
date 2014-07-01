/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.security.filter.websso;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Serves as the main security filter that works in conjunction with a number of
 * handlers to protect a variety of contexts each using different authentication
 * schemes and policies. The basic premise is that this filter is installed on
 * any registered http context and it handles delegating the authentication to
 * the specified handlers in order to normalize and consolidate a session token
 * (the SAML assertion).
 */
public class WebSSOFilter implements Filter {
    private static final String DEFAULT_REALM = "DDF";

    private static final String DDF_SECURITY_TOKEN = "ddf.security.securityToken";

    private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSSOFilter.class);

    /**
     * Dynamic list of handlers that are registered to provide authentication
     * services.
     */
    List<AuthenticationHandler> handlerList = new ArrayList<AuthenticationHandler>();

    ContextPolicyManager contextPolicyManager;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    /**
     * Provides filtering for every registered http context. Checks for an
     * existing session (via the SAML assertion included as a cookie). If it
     * doesn't exist, it then looks up the current context and determines the
     * proper handlers to include in the chain. Each handler is given the
     * opportunity to locate their specific tokens if they exist or to go off
     * and obtain them. Once a token has been received that we know how to
     * convert to a SAML assertion, we attach them to the request and continue
     * down the chain.
     *
     * @param servletRequest
     *            incoming http request
     * @param servletResponse
     *            response stream for returning the response
     * @param filterChain
     *            chain of filters to be invoked following this filter
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        LOGGER.debug("Performing doFilter() on WebSSOFilter");
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String path = StringUtils.isNotBlank(httpRequest.getContextPath()) ? httpRequest
                .getContextPath() : httpRequest.getServletPath()
                + StringUtils.defaultString(httpRequest.getPathInfo());
        LOGGER.debug("Handling request for path {}", path);

        String realm = DEFAULT_REALM;
        boolean isWhiteListed = false;
        if (contextPolicyManager != null) {
            ContextPolicy policy = contextPolicyManager.getContextPolicy(path);
            if (policy != null) {
                realm = policy.getRealm();
            }

            isWhiteListed = contextPolicyManager.isWhiteListed(path);
        }

        if (isWhiteListed) {
            LOGGER.debug(
                    "Context of {} has been whitelisted, adding a NO_AUTH_POLICY attribute to the header.",
                    path);
            servletRequest.setAttribute(ContextPolicy.ACTIVE_REALM, realm);
            servletRequest.setAttribute(ContextPolicy.NO_AUTH_POLICY, true);
            filterChain.doFilter(httpRequest, httpResponse);
        } else {
            // now handle the request and set the authentication token
            LOGGER.debug("Handling request for {} in security realm {}.", path, realm);
            handleRequest(httpRequest, httpResponse, filterChain, getHandlerList(path));
        }

    }

    private void handleRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            FilterChain filterChain, List<AuthenticationHandler> handlerList) throws IOException,
            ServletException {

        // First pass, see if anyone can come up with proper security token from
        // the git-go
        HandlerResult result = null;
        LOGGER.debug("Checking for existing tokens in request.");
        for (AuthenticationHandler auth : handlerList) {
            result = auth.getNormalizedToken(httpRequest, httpResponse, filterChain, false);
            if (result.getStatus() != HandlerResult.Status.NO_ACTION) {
                break;
            }
        }

        // If we haven't received usable credentials yet, go get some
        if (result == null || result.getStatus() == HandlerResult.Status.NO_ACTION) {
            LOGGER.debug("First pass with no tokens found - requesting tokens");
            // This pass, tell each handler to do whatever it takes to get a
            // SecurityToken
            for (AuthenticationHandler auth : handlerList) {
                result = auth.getNormalizedToken(httpRequest, httpResponse, filterChain, true);
                if (result.getStatus() != HandlerResult.Status.NO_ACTION) {
                    break;
                }
            }
        }

        if (result != null) {
            switch (result.getStatus()) {
            case REDIRECTED:
                // handler handled the response - it is redirecting or whatever
                // necessary to get their tokens
                LOGGER.debug("Stopping filter chain - handled by plugins");
                return;
            case NO_ACTION: // should never occur - one of the handlers should
                            // have returned a token
            case COMPLETED:
                // set the appropriate request attribute
                if (result.hasSecurityToken()) {
                    LOGGER.debug("Attaching security token to http request");
                    httpRequest.setAttribute(DDF_SECURITY_TOKEN, result.getCredentials());
                } else {
                    LOGGER.debug("Attaching authentication credentials to http request");
                    httpRequest.setAttribute(DDF_AUTHENTICATION_TOKEN, result);
                }
                break;
            }
        } else {
            LOGGER.warn("Expected login credentials - didn't find any. Returning a forbidden response.");
            returnSimpleResponse(HttpServletResponse.SC_FORBIDDEN, httpResponse);
            return;
        }

        // If we got here, we've received our tokens to continue
        LOGGER.debug("Invoking the rest of the filter chain");
        try {
            filterChain.doFilter(httpRequest, httpResponse);
        } catch (Exception e) {
            // First pass, see if anyone can come up with proper security token
            // from the git-go
            result = null;
            for (AuthenticationHandler auth : handlerList) {
                result = auth.handleError(httpRequest, httpResponse, filterChain);
                if (result.getStatus() != HandlerResult.Status.NO_ACTION) {
                    break;
                }
            }
            if (result.getStatus() == HandlerResult.Status.NO_ACTION) {
                LOGGER.debug("Error during authentication - no error recovery attempted - returning unauthorized.");
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.flushBuffer();
            }
        }
    }

    private List<AuthenticationHandler> getHandlerList(String path) {
        List<AuthenticationHandler> handlerList = new ArrayList<AuthenticationHandler>();
        if (contextPolicyManager != null) {
            ContextPolicy policy = contextPolicyManager.getContextPolicy(path);
            if (policy != null) {
                Collection<String> authMethods = policy.getAuthenticationMethods();
                for (String authMethod : authMethods) {
                    for (AuthenticationHandler handler : this.handlerList) {
                        if (handler.getAuthenticationType().equalsIgnoreCase(authMethod)) {
                            handlerList.add(handler);
                        }
                    }
                }
            }
        } else {
            // if no manager, get a list of all the handlers.
            handlerList.addAll(this.handlerList);
        }
        return handlerList;
    }

    /**
     * Sends the given response code back to the caller.
     *
     * @param code
     *            HTTP response code for this request
     * @param response
     *            the servlet response object
     */
    private void returnSimpleResponse(int code, HttpServletResponse response) {
        try {
            response.setStatus(code);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            LOGGER.debug("Failed to send auth response", ioe);
        }
    }

    @Override
    public void destroy() {

    }

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
