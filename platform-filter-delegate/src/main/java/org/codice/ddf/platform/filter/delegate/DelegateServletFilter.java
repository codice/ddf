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

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DelegateServletFilter} is meant to detect any ServletFilters
 * and call their {@link FilterChain}. If none are found it becomes a pass
 * through filter.
 *
 */
public class DelegateServletFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegateServletFilter.class);

    private FilterConfig filterConfig;

    private List<Filter> filters;

    public DelegateServletFilter(List<Filter> filters) {
        this.filters = filters;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        if (!filters.isEmpty()) {
            LOGGER.debug("Found {} filter(s), now filtering...", filters.size());

            ProxyFilterChain chain = new ProxyFilterChain(filterChain);

            LinkedList<Filter> sortedFilters = new LinkedList<Filter>(filters);
            Collections.sort(sortedFilters, new Comparator<Filter>() {
                @Override
                public int compare(Filter o1, Filter o2) {
                    return 0;
                }
            });
            Iterator<Filter> reverseIterator = new LinkedList<Filter>(filters).descendingIterator();
            while (reverseIterator.hasNext()) {
                Filter curFilter = reverseIterator.next();
                curFilter.init(filterConfig);
                if (!curFilter.getClass().toString().equals(this.getClass().toString())) {
                    chain.addFilter(curFilter);
                }
                reverseIterator.remove();
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
