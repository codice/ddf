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
package org.codice.ddf.ui.searchui.security;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * {@link SecurityServletFilter} is meant to detect any Security ServletFilters and call their
 * {@link FilterChain}. If none are found it becomes a pass through filter.
 * 
 * @author kcwire
 * 
 */
public class SecurityServletFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityServletFilter.class);

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain)
        throws IOException, ServletException {

        BundleContext ctx = FrameworkUtil.getBundle(SecurityServletFilter.class).getBundleContext();
        ServiceReference<?>[] serviceRefs = null;
        try {
            // TODO - Would be benenficial if we had in our Security API a "SecurityFitler" that
            // implements Filter. Then we could look up all "SecurityFilter"s and just use the
            // highest ranking one.
          serviceRefs = ctx.getServiceReferences(Filter.class.getName(), null);
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Lookup failed.", e);
        }

        if (serviceRefs != null && serviceRefs.length != 0) {
            LOGGER.debug("Found filter, now filtering...");

            ProxyFilterChain chain = new ProxyFilterChain(filterChain);

            for(int i=serviceRefs.length - 1; i>=0; i--) {
                chain.addFilter((Filter) ctx.getService(serviceRefs[i]));
            }

//            LOGGER.debug("Filter class: {}", proxyFilter.getClass().getName());
            chain.doFilter(servletRequest, servletResponse);
        } else {
            LOGGER.debug("Did not find filter");
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

    public static class ProxyFilterChain implements FilterChain {
        private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFilterChain.class);

        private FilterChain filterChain;

        private List<Filter> filters;

        private Iterator<Filter> iterator;

        /**
         * @param filterChain
         *            The filter chain from the web container.
         */
        public ProxyFilterChain(FilterChain filterChain) {
            this.filterChain = filterChain;
            filters = new ArrayList<Filter>();
        }

        /**
         * @param filter
         *            The servlet filter to add.
         */
        public void addFilter(Filter filter) {
            if (iterator != null) {
                throw new IllegalStateException();
            }

            LOGGER.debug("Adding filter " + filter.getClass().getName() + " to filter chain.");
            filters.add(0, filter);
        }

        /**
         * @param filters
         *            The servlet filters to add.
         */
        public void addFilters(List<Filter> filters) {
            if (iterator != null) {
                throw new IllegalStateException();
            }

            this.filters.addAll(filters);

            if (LOGGER.isDebugEnabled()) {
                for (Filter filter : filters) {
                    LOGGER.debug("Added filter " + filter.getClass().getName() + " to filter chain.");
                }
            }
        }

        /**
         * Calls the next filter in the chain.
         *
         * @see javax.servlet.FilterChain#doFilter(javax.servlet.ServletRequest,
         *      javax.servlet.ServletResponse)
         */
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IOException, ServletException {
            if (iterator == null) {
                iterator = filters.iterator();
            }

            if (iterator.hasNext()) {
                Filter filter = iterator.next();
                LOGGER.debug("Calling filter " + filter.getClass().getName() + ".doFilter(" + servletRequest
                        + ", " + servletResponse + ", " + this + ")");
                filter.doFilter(servletRequest, servletResponse, this);
            } else {
                LOGGER.debug("Calling filterChain " + filterChain.getClass().getName() + ".doFilter("
                        + servletRequest + ", " + servletResponse + ")");
                filterChain.doFilter(servletRequest, servletResponse);
            }
        }

    }

}
