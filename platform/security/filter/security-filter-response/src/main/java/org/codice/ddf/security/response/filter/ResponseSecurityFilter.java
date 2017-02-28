/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.response.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet filter that adds security information to the http response header.
 */
public class ResponseSecurityFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseSecurityFilter.class);

    public static final String X_XSS_PROTECTION = "X-XSS-Protection";

    public static final String X_FRAME_OPTIONS = "X-Frame-Options";

    public static final String X_CONTENT_SECURITY_POLICY = "X-Content-Security-Policy";

    public static final String DEFAULT_XSS_PROTECTION_VALUE = "1; mode=block";

    public static final String DEFAULT_X_FRAME_OPTIONS_VALUE = "SAMEORIGIN";

    public static final String DEFAULT_CONTENT_SECURITY_POLICY =
            "default-src 'none'; connect-src 'self'; script-src 'self'; style-src 'self'; img-src 'self'";

    private String xXssProtection = DEFAULT_XSS_PROTECTION_VALUE;

    private String xFrameOptions = DEFAULT_X_FRAME_OPTIONS_VALUE;

    private String xContentSecurityPolicy = DEFAULT_CONTENT_SECURITY_POLICY;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOGGER.debug("Initializing Response Security Filter.");
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        response.setHeader(X_CONTENT_SECURITY_POLICY, xContentSecurityPolicy);
        response.setHeader(X_XSS_PROTECTION, xXssProtection);
        response.setHeader(X_FRAME_OPTIONS, xFrameOptions);

        filterChain.doFilter(servletRequest, response);
    }

    @Override
    public void destroy() {
        LOGGER.debug("Destroying Response Security Filter.");
    }

    public void setXXssProtection(String xXssProtection) {
        this.xXssProtection = xXssProtection;
    }

    public void setXContentSecurityPolicy(String xContentSecurityPolicy) {
        this.xContentSecurityPolicy = xContentSecurityPolicy;
    }

    public void setXFrameOptions(String xFrameOptions) {
        this.xFrameOptions = xFrameOptions;
    }
}
