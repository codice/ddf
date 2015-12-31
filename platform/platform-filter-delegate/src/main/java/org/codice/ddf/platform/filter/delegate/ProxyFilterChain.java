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
package org.codice.ddf.platform.filter.delegate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of filter chain that allows the ability to add new filters to
 * a chain.
 *
 */
public class ProxyFilterChain implements FilterChain {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFilterChain.class);

    private FilterChain filterChain;

    private LinkedList<Filter> filters;

    private Iterator<Filter> iterator;

    /**
     * Creates a new ProxyFilterChain with the specified filter chain included.
     *
     * @param filterChain
     *            The filter chain from the web container.
     */
    public ProxyFilterChain(FilterChain filterChain) {
        this.filterChain = filterChain;
        filters = new LinkedList<Filter>();
    }

    /**
     * Adds a single filter to the start of the local filter chain.
     *
     * @param filter
     *            The servlet filter to add.
     */
    public void addFilter(Filter filter) {
        if (filter != null) {
            addFilters(Arrays.asList(filter));
        } else {
            throw new IllegalArgumentException("Cannot add null filter to chain.");
        }
    }

    /**
     * Adds a list of filters to the start of the local filter chain but before
     * the initialized chain.
     *
     * @param filters
     *            The servlet filters to add.
     */
    public void addFilters(List<Filter> filters) {
        if (iterator != null) {
            throw new IllegalStateException("Cannot add filter to current running chain.");
        }

        if (filters != null) {
            this.filters.addAll(0, filters);
        } else {
            throw new IllegalArgumentException("Cannot add null filter list to chain.");
        }

        if (LOGGER.isDebugEnabled()) {
            for (Filter filter : filters) {
                LOGGER.debug("Added filter {} to filter chain.",
                        filter.getClass()
                                .getName());
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IOException, ServletException {
        if (iterator == null) {
            iterator = filters.iterator();
        }

        if (iterator.hasNext()) {
            Filter filter = iterator.next();
            LOGGER.debug("Calling filter {}.doFilter({}, {}, {})",
                    filter.getClass()
                            .getName(),
                    servletRequest,
                    servletResponse,
                    this);
            filter.doFilter(servletRequest, servletResponse, this);
        } else {
            LOGGER.debug("Calling filterChain {}.doFilter({}, {})",
                    filterChain.getClass()
                            .getName(),
                    servletRequest,
                    servletResponse);
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

}
