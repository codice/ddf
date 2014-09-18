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
package org.codice.ddf.security.handler.anonymous;

import org.codice.ddf.security.handler.api.AnonymousAuthenticationToken;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.handler.basic.BasicAuthenticationHandler;
import org.codice.ddf.security.handler.pki.PKIHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Handler that allows anonymous user access via a guest user account. The guest/guest account
 * must be present in the user store for this handler to work correctly.
 */
public class AnonymousHandler implements AuthenticationHandler {
    public static final Logger logger = LoggerFactory.getLogger(AnonymousHandler.class.getName());

    /**
     * Anonymous type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "ANON";

    public static final String INVALID_MESSAGE = "Username/Password is invalid.";

    private String realm;

    private PKIAuthenticationTokenFactory tokenFactory;

    @Override
    public String getAuthenticationType() {
        return AUTH_TYPE;
    }

    /**
     * This method takes an anonymous request and attaches a username token
     * to the HTTP request to allow access. The method also allows the user to
     * sign-in and authenticate.
     *
     * @param request  http request to obtain attributes from and to pass into any local filter chains required
     * @param response http response to return http responses or redirects
     * @param chain    original filter chain (should not be called from your handler)
     * @param resolve  flag with true implying that credentials should be obtained, false implying return if no credentials are found.
     * @return HandlerResult
     */
    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve) {
        HandlerResult result = new HandlerResult();

        // For anonymous - if credentials were provided, return them, if not, then return guest credentials
        BaseAuthenticationToken authToken = getAuthToken((HttpServletRequest) request,
                (HttpServletResponse) response, chain);

        result.setSource(realm + "-AnonymousHandler");
        result.setStatus(HandlerResult.Status.COMPLETED);
        result.setToken(authToken);
        return result;
    }

    /**
     * Returns BSTAuthenticationToken for the HttpServletRequest
     *
     * @param request http request to obtain attributes from and to pass into any local filter chains required
     * @return BSTAuthenticationToken
     */
    private BaseAuthenticationToken getAuthToken(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        //check for basic auth first
        BasicAuthenticationHandler basicAuthenticationHandler = new BasicAuthenticationHandler();
        basicAuthenticationHandler.setRealm(realm);
        HandlerResult handlerResult = basicAuthenticationHandler.getNormalizedToken(request, response, chain, false);
        if(handlerResult.getStatus().equals(HandlerResult.Status.COMPLETED)) {
            return handlerResult.getToken();
        }
        //if basic fails, check for PKI
        PKIHandler pkiHandler = new PKIHandler();
        pkiHandler.setRealm(realm);
        pkiHandler.setTokenFactory(tokenFactory);
        try {
            handlerResult = pkiHandler.getNormalizedToken(request, response, chain, false);
            if(handlerResult.getStatus().equals(HandlerResult.Status.COMPLETED)) {
                return handlerResult.getToken();
            }
        } catch (ServletException e) {
            logger.warn("Encountered an exception while checking for PKI auth info.", e);
        }

        //if everything fails, the user is anonymous, log in as such
        AnonymousAuthenticationToken token = new AnonymousAuthenticationToken(realm);


        return token;
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        try {
            httpResponse.getWriter().write(INVALID_MESSAGE);
            httpResponse.flushBuffer();
        } catch (IOException e) {
            logger.debug("Failed to send auth response: {}", e);
        }

        HandlerResult result = new HandlerResult();
        result.setSource(realm + "-AnonymousHandler");
        logger.debug("In error handler for anonymous - returning action completed.");
        result.setStatus(HandlerResult.Status.REDIRECTED);
        return result;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public PKIAuthenticationTokenFactory getTokenFactory() {
        return tokenFactory;
    }

    public void setTokenFactory(PKIAuthenticationTokenFactory tokenFactory) {
        this.tokenFactory = tokenFactory;
    }
}
