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

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        // Find the cas-client bundleContext
        BundleContext ctx = FrameworkUtil.getBundle(SecurityServletFilter.class).getBundleContext();
        ServiceReference<?>[] serviceRefs = null;
        try {
            // TODO - Would be benenficial if we had in our Security API a "SecurityFitler" that
            // implements Filter. Then we could look up all "SecurityFilter"s and just use the
            // highest ranking one.
            serviceRefs = ctx.getServiceReferences(Filter.class.getName(),
                    "(filter-name=cas-client)");
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Unable to lookup cas-client", e);
        }

        if (serviceRefs != null && serviceRefs.length != 0) {
            LOGGER.debug("Found the cas-client, now filtering...");
            Filter proxyFilter = (Filter) ctx.getService(serviceRefs[0]);
            proxyFilter.doFilter(servletRequest, servletResponse, filterChain);
        } else {
            LOGGER.debug("Did not find cas-client");
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

}
