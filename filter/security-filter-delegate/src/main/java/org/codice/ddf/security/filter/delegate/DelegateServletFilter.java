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
package org.codice.ddf.security.filter.delegate;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link DelegateServletFilter} is meant to detect any Security ServletFilters
 * and call their {@link FilterChain}. If none are found it becomes a pass
 * through filter.
 *
 */
public class DelegateServletFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegateServletFilter.class);

    private BundleContext ctx;

    private ContextPolicyManager contextPolicyManager = null;

    public DelegateServletFilter(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;

        if (contextPolicyManager != null) {
            String contextPath = !StringUtils.isBlank(httpRequest.getContextPath()) ? httpRequest
                    .getContextPath() : httpRequest.getServletPath() + httpRequest.getPathInfo();
            ContextPolicy policy = contextPolicyManager.getContextPolicy(contextPath);
            LOGGER.debug("Got policy for ({}).", contextPath);
            if (policy.getAuthenticationMethods().isEmpty()) {
                LOGGER.debug(
                        "Current Context path {} has been white listed by the local policy, no authentication or authorization filters will be applied.",
                        contextPath);
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        }

        LinkedList<ServiceReference<Filter>> serviceRefs = new LinkedList<ServiceReference<Filter>>();
        try {
            serviceRefs.addAll(ctx.getServiceReferences(Filter.class, null));
        } catch (InvalidSyntaxException e) {
            LOGGER.warn("Could not lookup service references.", e);
        }

        if (!serviceRefs.isEmpty()) {
            LOGGER.debug("Found {} filter, now filtering...", serviceRefs.size());

            ProxyFilterChain chain = new ProxyFilterChain(filterChain);

            Iterator<ServiceReference<Filter>> reverseIterator = serviceRefs.descendingIterator();
            while (reverseIterator.hasNext()) {
                ServiceReference<Filter> curReference = reverseIterator.next();
                Filter curFilter = ctx.getService(curReference);
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

    /**
     * Sets the context policy manager that will be used to check if the
     * incoming request should be whitelisted (no filters called).
     *
     * @param policyManager
     *            Manager that contains policies for contexts.
     */
    public void setContextPolicyManager(ContextPolicyManager policyManager) {
        this.contextPolicyManager = policyManager;
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
    }

}
