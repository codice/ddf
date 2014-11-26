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

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;
import java.lang.reflect.Field;
import java.util.EnumSet;

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

        //Jetty will place non-programmatically added filters (filters added via web.xml) in front of programmatically
        //added filters. This is probably OK in most instances, however, this security filter must ALWAYS be first.
        //This reflection hack basically tricks Jetty into believing that this filter is a web.xml filter so that it always ends up first.
        //In order for this to work correctly, the delegating filter must always be added before any other programmatically added filters.
        Field field = null;
        try {
            //this grabs the enclosing instance class, which is actually a private class
            //this is the only way to do this in Java
            field = context.getClass().getDeclaredField("this$0");
            field.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LOGGER.warn("Unable to find enclosing class of ServletContext for delegating filter. Security may not work correctly", e);
        }
        Field matchAfterField = null;
        Object matchAfterValue = null;
        ServletHandler handler = null;
        if (field != null) {
            //need to grab the servlet context handler so we can get down to the handler, which is what we really need
            ServletContextHandler httpServiceContext = null;
            try {
                httpServiceContext = (ServletContextHandler) field.get(context);
            } catch (IllegalAccessException e) {
                LOGGER.warn("Unable to get the ServletContextHandler for {}. The delegating filter may not work properly.", refBundle.getSymbolicName(), e);
            }

            if (httpServiceContext != null) {
                //now that we have the handler, we can muck with the filters and state variables
                handler = httpServiceContext.getServletHandler();

                if (handler != null) {
                    try {
                        matchAfterField = handler.getClass().getSuperclass().getDeclaredField("_matchAfterIndex");
                        matchAfterField.setAccessible(true);
                    } catch (NoSuchFieldException e) {
                        LOGGER.warn("Unable to find the matchAfterIndex value for the ServletHandler. The delegating filter may not work properly.", e);
                    }

                    if (matchAfterField != null) {
                        try {
                            //this value is initialized to -1 and only changes after a programmatic filter has been added
                            //so basically we are grabbing this value (should be -1) and then setting the field back to that value
                            //after we add our delegating filter to the mix
                            matchAfterValue = matchAfterField.get(handler);
                        } catch (IllegalAccessException e) {
                            LOGGER.warn("Unable to get the value of the match after field. The delegating filter may not work properly.", e);
                        }
                    }
                }
            }
        }

        try {
            //This causes the value of "_matchAfterIndex" to jump to 0 which means all web.xml filters will be added in front of it
            //this isn't what we want, so we need to reset it back to what it was before
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

        if (matchAfterField != null && matchAfterValue != null) {
            try {
                //Reset the value back to what it was before we added our delegating filter, this should cause Jetty to behave as if
                //this was a filter added via web.xml
                matchAfterField.set(handler, matchAfterValue);
            } catch (IllegalAccessException e) {
                LOGGER.warn("Unable to set the match after field back to the original value. The delegating filter might be out of order", e);
            }
        } else {
            LOGGER.warn("Unable to set the match after field back to the original value. The delegating filter might be out of order.");
        }
    }

}
