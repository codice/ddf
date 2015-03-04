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
package org.codice.ddf.security.handler.cas.filter;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy filter used by the DDF CAS client. When a request comes in, the filter chain will initially
 * look like the following:
 * <p/>
 * ProxyFilter -> MyServlet
 * <p/>
 * We want to intercept the filter chain and add all of the required CAS filters in the correct
 * order so the filter chain looks similar to the following:
 * <p/>
 * ProxyFilter -> CAS filter 1 -> CAS filter 2 -> CAS filter n -> MyServlet
 * <p/>
 * Note: Using blueprint to define the filters and publish them as services caused the filters to be
 * added to the filter chain in an nondeterministic order. Setting service raking didn't work
 * either. CAS relies on the filters being executed in a specific order.
 *
 * @see javax.servlet.Filter
 */
public class ProxyFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFilter.class);

    private List<Filter> filters;

    private boolean initialized;

    /**
     * @param filters The CAS filters to add to the filter chain.
     */
    public ProxyFilter(List<Filter> filters) {
        this.filters = filters;
        initialized = false;
    }

    /**
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy() {
        for (Filter filter : filters) {
            LOGGER.debug("Destroying filter {}.", filter.getClass().getName());
            filter.destroy();
        }
    }

    /**
     * Adds all of the CAS filters to the filter chain and calls the filter chain so it can start
     * executing all of the CAS filters.
     *
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     * javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        StringBuffer requestUrl = httpServletRequest.getRequestURL();

        LOGGER.debug("Starting a filter chain for request {}", requestUrl);

        ProxyFilterChain filterProxyChain = new ProxyFilterChain(filterChain);

        LOGGER.debug("Adding {} filter(s) to filter chain.", filters.size());

        filterProxyChain.addFilters(filters);

        LOGGER.debug("Calling {}.doFilter({},{})", filterProxyChain.getClass().getName(), servletRequest, servletResponse);
        filterProxyChain.doFilter(servletRequest, servletResponse);

    }

    /**
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        if (!initialized) {
            for (Filter filter : filters) {
                LOGGER.debug("Calling {}.init({})", filter.getClass().getName(), filterConfig);
                filter.init(filterConfig);
            }

            initialized = true;
        }
    }

}
