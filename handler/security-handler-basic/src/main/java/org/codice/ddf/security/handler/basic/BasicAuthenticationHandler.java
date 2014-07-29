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
package org.codice.ddf.security.handler.basic;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.Base64;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

/**
 * Checks for basic authentication credentials in the http request header. If they exist, they are retrieved and
 * returned in the HandlerResult.
 */
public class BasicAuthenticationHandler implements AuthenticationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicAuthenticationHandler.class);

    /**
     * Basic type to use when configuring context policy.
     */
    public static final String AUTH_TYPE = "BASIC";

    public static final String AUTHENTICATION_SCHEME_BASIC = "Basic";

    public static final String SOURCE = "BasicHandler";

    protected String authenticationType = AUTH_TYPE;
    protected String realm = BaseAuthenticationToken.DEFAULT_REALM;

    public BasicAuthenticationHandler() {
        LOGGER.debug("Creating basic username/token bst handler.");
    }

    @Override
    public String getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(String authType) {
        this.authenticationType = authType;
    }
    /**
     * Processes the incoming request to retrieve the username/password tokens. Handles responding
     * to the client that authentication is needed if they are not present in the request.
     * Returns the {@link org.codice.ddf.security.handler.api.HandlerResult} for the HTTP Request.
     *
     * @param request  http request to obtain attributes from and to pass into any local filter chains required
     * @param response http response to return http responses or redirects
     * @param chain    original filter chain (should not be called from your handler)
     * @param resolve  flag with true implying that credentials should be obtained, false implying return if no credentials are found.
     * @return
     */
    @Override
    public HandlerResult getNormalizedToken(ServletRequest request, ServletResponse response, FilterChain chain, boolean resolve) {

        HandlerResult handlerResult = new HandlerResult(HandlerResult.Status.NO_ACTION, null);
        handlerResult.setSource(realm + "-" + SOURCE);

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath();
        LOGGER.debug("Handling request for path {}", path);

        LOGGER.debug("Doing authentication and authorization for path {}", path);

        UPAuthenticationToken token = extractAuthenticationInfo(httpRequest);

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
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws ServletException {
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
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME_BASIC + " realm=\"" + realm + "\"");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentLength(0);
            response.flushBuffer();
        } catch (IOException ioe) {
            LOGGER.debug("Failed to send auth response: {}", ioe);
        }
    }

    /**
     * Updates the given credentials with the username and password from the http authentication
     * header of the http request. The authorization request is expected to be of the following form:
     * <p/>
     * Authorization: Basic <base64-encoded string of the form username:password>>
     * <p/>
     *
     * @param request the http request containing the credential information
     * @return string representation of the BinarySecurityTokenType, or null if username and password not supplied
     */
    protected UPAuthenticationToken extractAuthenticationInfo(HttpServletRequest request) {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || authHeader.equals("")) {
            return null;
        }

        UPAuthenticationToken token = extractAuthInfo(authHeader);

        return token;
    }

    /**
     * Extract the Authorization header and parse into a username/password token.
     *
     * @param authHeader the authHeader string from the HTTP request
     * @return the initialize UPAuthenticationToken for this username, password, realm combination (or null)
     */
    protected UPAuthenticationToken extractAuthInfo(String authHeader) {
        UPAuthenticationToken token = null;
        authHeader = authHeader.trim();
        String[] parts = authHeader.split(" ");
        if (parts.length == 2) {
            String authType = parts[0];
            String authInfo = parts[1];

            if (authType.equalsIgnoreCase(AUTHENTICATION_SCHEME_BASIC)) {
                String decoded = null;
                try {
                    decoded = new String(Base64.decode(authInfo));
                    parts = decoded.split(":");
                    if (parts.length == 2) {
                        token = new UPAuthenticationToken(parts[0], parts[1], realm);
                    } else if ((parts.length == 1) && (decoded.endsWith(":"))) {
                        token = new UPAuthenticationToken(parts[0], "", realm);
                    }
                } catch (WSSecurityException e) {
                    LOGGER.warn("Unexpected error decoding username/password: " + e.getMessage(), e);
                }
            }
        }
        return token;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}