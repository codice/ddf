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
package org.codice.security.filter.websso;

import org.codice.security.handler.api.AuthenticationHandler;
import org.codice.security.handler.api.HandlerResult;
import org.codice.security.handler.api.HandlerResult.FilterStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

/**
 * Serves as the main security filter that works in conjunction with a number of handlers
 * to protect a variety of contexts each using different authentication schemes and policies.
 * The basic premise is that this filter is installed on any registered http context
 * and it handles delegating the authentication to the specified handlers in order to
 * normalize and consolidate a session token (the SAML assertion).
 */
public class WebSSOFilter implements Filter {
    private static final String DDF_SECURITY_TOKEN = "ddf.security.securityToken";

    private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSSOFilter.class);

    /**
     * Dynamic list of handlers that are registered to provide authentication services.
     */
    List<AuthenticationHandler> handlerList;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    /**
     * Provides filtering for every registered http context. Checks for an existing session (via the SAML assertion included as a cookie).
     * If it doesn't exist, it then looks up the current context and determines the proper handlers to include in the chain.
     * Each handler is given the opportunity to locate their specific tokens if they exist or to go off and obtain them.
     * Once a token has been received that we know how to convert to a SAML assertion, we attach them to the request and continue
     * down the chain.
     *
     * @param servletRequest incoming http request
     * @param servletResponse response stream for returning the response
     * @param filterChain chain of filters to be invoked following this filter
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public synchronized void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String path = httpRequest.getServletPath();
        LOGGER.debug("Handling request for path {}", path);

        // @TODO lookup the authentication context based on the context path and adjust the handlers accordingly.
        // for now they are automatically added to the handler list ordered by service rank

        // First pass, see if anyone can come up with proper security token from the git-go
        HandlerResult result = null;
        for (AuthenticationHandler auth : handlerList) {
            result = auth.getNormalizedToken(servletRequest, servletResponse, filterChain, false);
            if (result.getStatus() != FilterStatus.NO_ACTION) {
                break;
            }
        }

        // If we haven't received usable credentials yet, go get some
        if (result == null || result.getStatus() == FilterStatus.NO_ACTION) {
            LOGGER.debug("First pass with no tokens found - requesting tokens");
            // This pass, tell each handler to do whatever it takes to get a SecurityToken
            for (AuthenticationHandler auth : handlerList) {
                result = auth.getNormalizedToken(servletRequest, servletResponse, filterChain, true);
                if (result.getStatus() != FilterStatus.NO_ACTION) {
                    break;
                }
            }
        }

        switch (result.getStatus()) {
            case REDIRECTED:
                // handler handled the response - it is redirecting or whatever necessary to get their tokens
                LOGGER.debug("Stopping filter chain - handled by plugins");
                return;
            case NO_ACTION: // should never occur - one of the handlers should have returned a token
            case COMPLETED:
                // set the appropriate request attribute
                if (result.hasSecurityToken()) {
                    LOGGER.debug("Attaching SecurityToken to http request");
                    httpRequest.setAttribute(DDF_SECURITY_TOKEN, result.getCredentials());
                } else {
                    LOGGER.debug("Attaching AuthenticationToken to http request");
                    httpRequest.setAttribute(DDF_AUTHENTICATION_TOKEN, result);
                }
                break;
        }

        // If we got here, we've received our tokens to continue
        LOGGER.debug("Invoking the rest of the filter chain");
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {

    }

    public List<AuthenticationHandler> getHandlerList() {
        return handlerList;
    }

    public void setHandlerList(List<AuthenticationHandler> handlerList) {
        this.handlerList = handlerList;
    }
}

