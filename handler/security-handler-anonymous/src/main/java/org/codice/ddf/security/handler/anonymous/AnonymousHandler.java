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

import org.apache.cxf.common.util.StringUtils;
import org.codice.ddf.security.handler.api.AuthenticationHandler;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
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

    protected static final String GUEST_USER = "guest";

    protected static final String GUEST_PW = "guest";

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
        UPAuthenticationToken usernameToken = getUsernameToken((HttpServletRequest) request);

        result.setSource(BaseAuthenticationToken.DEFAULT_REALM + "-AnonymousHandler");
        result.setStatus(HandlerResult.Status.COMPLETED);
        result.setToken(usernameToken);
        return result;
    }

    /**
     * This method uses the data passed in the HttpServletRequest to generate
     * and return UsernameTokenType.
     *
     * @param request http request to obtain attributes from and to pass into any local filter chains required
     * @return UsernameTokenType
     */
    private UPAuthenticationToken getUsernameToken(HttpServletRequest request) {
        String username = GUEST_USER;
        String password = GUEST_PW;

        /**
         * Parse the header data and extract the username and password.
         *
         * Change the username and password if request contains values.
         */
        String header = request.getHeader("Authorization");
        if (!StringUtils.isEmpty(header)) {
            String headerData[] = header.split(" ");
            if (headerData.length == 2) {
                String decodedHeader = new String(Base64.decodeBase64(headerData[1].getBytes()));
                String decodedHeaderData[] = decodedHeader.split(":");
                if (decodedHeaderData.length == 2) {
                    username = decodedHeaderData[0];
                    password = decodedHeaderData[1];
                }
            }
        }

        UPAuthenticationToken token = new UPAuthenticationToken(username, password);
        return token;
    }

    @Override
    public HandlerResult handleError(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
        httpResponse.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        try {
            httpResponse.getWriter().write("Username/Password is invalid.");
            httpResponse.flushBuffer();
        } catch (IOException e) {
            logger.debug("Failed to send auth response: {}", e);
        }

        HandlerResult result = new HandlerResult();
        result.setSource(BaseAuthenticationToken.DEFAULT_REALM + "-AnonymousHandler");
        logger.debug("In error handler for anonymous - returning action completed.");
        result.setStatus(HandlerResult.Status.COMPLETED);  // we handled the error
        return result;
    }
}
