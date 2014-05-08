package ddf.security.filter;

import ddf.security.filter.handlers.AnonymousHandler;
import ddf.security.filter.handlers.AuthenticationHandler;
import ddf.security.filter.handlers.SAMLAssertionHandler;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.authc.AuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
public class TokenNormalizer implements Filter
{
    public static final String DDF_COOKIE_NAME = "ddfCookie";
    private static final String SAML_COOKIE_NAME = "org.codice.websso.saml.token";
    private static final String DDF_SECURITY_TOKEN = "ddf.security.securityToken";
    private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenNormalizer.class);

    ArrayList<AuthenticationHandler> authenticationHandlers = new ArrayList<AuthenticationHandler>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        authenticationHandlers.add(new SAMLAssertionHandler());
        authenticationHandlers.add(new AnonymousHandler());
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException
    {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String path = httpRequest.getServletPath();
        LOGGER.debug("Handling request for path {}", path);

        // read configuration for this path - get the authentication type and the roles required
        // for now...
        Map<String, Cookie> cookies = FilterUtils.getCookieMap(httpRequest);

        // First pass, see if anyone can come up with proper security token from the git-go
        Object securityToken = null;
        for (AuthenticationHandler auth : authenticationHandlers) {
            securityToken = auth.getNormalizedToken(servletRequest, servletResponse, filterChain, false);
            if (securityToken != null) {
                break;
            }
        }

        if (securityToken == null) {
            // This pass, tell each handler to do whatever it takes to get a SecurityToken
            for (AuthenticationHandler auth : authenticationHandlers) {
                securityToken = auth.getNormalizedToken(servletRequest, servletResponse, filterChain, true);
                if (securityToken != null) {
                    break;
                }
            }
        }

        if (securityToken != null) {
            if (securityToken instanceof SecurityToken) {
                httpRequest.setAttribute(DDF_SECURITY_TOKEN, securityToken);
            } else if (securityToken instanceof AuthenticationToken) {
                httpRequest.setAttribute(DDF_AUTHENTICATION_TOKEN, securityToken);
            }
        }
    }

    @Override
    public void destroy()
    {

    }

}

