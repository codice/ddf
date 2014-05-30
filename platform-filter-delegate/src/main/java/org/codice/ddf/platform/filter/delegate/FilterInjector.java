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

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Injects the delegating filter into the servletcontext as it is being created.
 * This guarantees that all servlets will have the delegating filter added.
 * 
 */
public class FilterInjector {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterInjector.class);

    private static final String ALL_URLS = "/*";

    private static final String DELEGATING_FILTER = "delegating-filter";

    private Filter delegatingServletFilter;

    /**
     * Creates a new filter injector with the specified filter.
     * 
     * @param filter
     *            filter that should be injected.
     */
    public FilterInjector(Filter filter) {
        this.delegatingServletFilter = filter;
    }

    /**
     * Injects the filter into the passed-in servlet context. This only works if
     * the servlet has not already been initialized.
     * 
     * @param serviceReference
     *            Reference to the servlet context that the filter should be
     *            injected into.
     */
    public void injectFilter(ServiceReference<ServletContext> serviceReference) {
        Bundle refBundle = serviceReference.getBundle();
        LOGGER.debug("Adding Servlet Filter for {}", refBundle.getSymbolicName());
        BundleContext bundlectx = refBundle.getBundleContext();
        ServletContext context = bundlectx.getService(serviceReference);
        try {
            FilterRegistration filterReg = context.addFilter(DELEGATING_FILTER,
                    delegatingServletFilter);

            if (filterReg == null) {
                filterReg = context.getFilterRegistration(DELEGATING_FILTER);
            }
            filterReg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, ALL_URLS);
        } catch (IllegalStateException ise) {
            LOGGER.warn("Could not inject filter into " + refBundle.getSymbolicName()
                    + " because the servlet was already initialized.", ise);
        }
    }

}
