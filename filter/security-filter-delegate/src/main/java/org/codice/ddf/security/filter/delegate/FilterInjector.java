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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Filter;
import javax.servlet.ServletContext;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FilterInjector {

    private Map<Bundle, ServiceRegistration<Filter>> filterRegistrations;

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterInjector.class);

    private static final String URL_PATTERNS = "urlPatterns";

    private static final String ALL_URLS = "/*";

    private static final String FILTER_NAME = "filter-name";

    private static final String DELEGATING_FILTER = "delegating-filter";
    
    

    private Filter delegatingServletFilter;

    public FilterInjector(Filter filter) {
        this.delegatingServletFilter = filter;
        filterRegistrations = new HashMap<Bundle, ServiceRegistration<Filter>>();
    }

    public void injectFilter(ServiceReference<ServletContext> serviceReference) {
        Bundle refBundle = serviceReference.getBundle();
        LOGGER.debug("Adding Servlet Filter for {}", refBundle.getSymbolicName());
        BundleContext bundlectx = refBundle.getBundleContext();
        ServletContext context = bundlectx.getService(serviceReference);
        Dictionary<String, Object> properties = new Hashtable<String, Object>();
        properties.put(URL_PATTERNS, ALL_URLS);
        properties.put(FILTER_NAME, DELEGATING_FILTER);
        if (!filterRegistrations.containsKey(refBundle)) {
            ServiceRegistration<Filter> reg = bundlectx.registerService(Filter.class,
                    delegatingServletFilter, properties);
            filterRegistrations.put(refBundle, reg);
        }
    }

    public void removeFilter(ServiceReference<ServletContext> serviceReference) {
        Bundle bundle = serviceReference.getBundle();
        if (bundle != null) {
            ServiceRegistration<Filter> registration = filterRegistrations.get(bundle);
            if (registration != null) {
                LOGGER.debug("Unregistering Fitler for {}", bundle.getSymbolicName());
                registration.unregister();
                filterRegistrations.remove(bundle);
            }
        }
    }

    /*
     * When this class is destroyed, remove all the registered filters.
     */
    public void cleanup() {
        LOGGER.debug("Removing all ServiceRegistrations");
        for (Entry<Bundle, ServiceRegistration<Filter>> registration : filterRegistrations
                .entrySet()) {
            registration.getValue().unregister();
        }
    }

}
