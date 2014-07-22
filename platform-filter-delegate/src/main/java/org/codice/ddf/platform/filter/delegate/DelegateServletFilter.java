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
package org.codice.ddf.platform.filter.delegate;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * {@link DelegateServletFilter} is meant to detect any ServletFilters
 * and call their {@link FilterChain}. If none are found it becomes a pass
 * through filter.
 *
 */
public class DelegateServletFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegateServletFilter.class);

    private FilterConfig filterConfig;

    private BundleContext context;

    public DelegateServletFilter(BundleContext context) {
        this.context = context;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        Collection<ServiceReference<Filter>> referenceCollection = null;

        try {
            referenceCollection = context.getServiceReferences(Filter.class, null);
        } catch (InvalidSyntaxException ise) {
            LOGGER.debug("Should not get this exception as there is no filter being passed.");
        }

        if (referenceCollection != null && !referenceCollection.isEmpty()) {
            LOGGER.debug("Found {} filter(s), now filtering...", referenceCollection.size());

            ProxyFilterChain chain = new ProxyFilterChain(filterChain);

            LinkedList<ServiceReference<Filter>> sortedFilters = new LinkedList<ServiceReference<Filter>>(referenceCollection);
            // natural ordering of service references is to sort by service ranking
            Collections.sort(sortedFilters);

            // because we're inserting these one at a time (inserting at index 0),
            // we insert them from lowest to highest in order to end up with the highest at index 0.
            Iterator<ServiceReference<Filter>> iterator = sortedFilters.iterator();
            while (iterator.hasNext()) {
                ServiceReference<Filter> curService = iterator.next();
                Filter curFilter = context.getService(curService);
                curFilter.init(filterConfig);
                if (!curFilter.getClass().toString().equals(this.getClass().toString())) {
                    LOGGER.debug("Adding filter that has a service ranking of {}", curService.getProperty(Constants.SERVICE_RANKING));
                    chain.addFilter(curFilter);
                }
                iterator.remove();
            }

            chain.doFilter(servletRequest, servletResponse);
        } else {
            LOGGER.debug("Did not find any filters");
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        this.filterConfig = filterConfig;
    }

}
