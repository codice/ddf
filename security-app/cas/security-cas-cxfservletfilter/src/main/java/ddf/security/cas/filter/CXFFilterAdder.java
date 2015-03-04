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
package ddf.security.cas.filter;

import org.apache.cxf.transport.servlet.CXFNonSpringServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import java.util.Hashtable;

public class CXFFilterAdder {
    private Logger logger = LoggerFactory.getLogger(CXFFilterAdder.class);

    private ServiceRegistration filterService;

    private BundleContext cxfContext;

    private Hashtable<String, String> properties = new Hashtable<String, String>();

    private Filter casProxyFilter;

    private static final String URL_PATTERNS_KEY = "urlPatterns";

    private static final String FILTER_NAME_KEY = "filter-name";

    private static final String FILTER_NAME = "CXFtoCASFilter";

    private static final String DEFAULT_URL_PATTERN = "/services/catalog/*";

    public CXFFilterAdder(Filter proxyFilter) {
        casProxyFilter = proxyFilter;
        Bundle cxfBundle = FrameworkUtil.getBundle(CXFNonSpringServlet.class);
        if (cxfBundle != null) {
            logger.debug("Found CXF Servlet Bundle with id: {}", cxfBundle.getBundleId());
            cxfContext = cxfBundle.getBundleContext();
            properties.put(FILTER_NAME_KEY, FILTER_NAME);
            properties.put(URL_PATTERNS_KEY, DEFAULT_URL_PATTERN);
            registerService();
        } else {
            logger.debug("Could not find CXF Servlet Bundle");
        }
    }

    public void setUrlPattern(String urlPattern) {
        logger.trace("Unregistering filter service to reset urlPatterns");
        filterService.unregister();
        properties.put(URL_PATTERNS_KEY, urlPattern);
        registerService();
    }

    public String getUrlPattern() {
        return properties.get(URL_PATTERNS_KEY);
    }

    private void registerService() {
        logger.debug("Registering Filter with CXF Context for url {}",
                properties.get(URL_PATTERNS_KEY));
        if (cxfContext != null) {
            filterService = cxfContext.registerService("javax.servlet.Filter", casProxyFilter, properties);
        } else {
            logger.debug("Attempting to register service with null CXF context.");
        }
        logger.debug("Filter registered.");
    }

}
