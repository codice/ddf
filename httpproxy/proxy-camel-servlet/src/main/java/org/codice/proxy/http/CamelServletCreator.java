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
package org.codice.proxy.http;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a registered Camel Servlet
 * 
 * @author ddf
 * 
 */
public class CamelServletCreator {
    BundleContext bundleContext = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(CamelServletCreator.class);

    public static final String SERVLET_PATH = "/proxy";

    public static final String SERVLET_NAME = "CamelServlet";

    public CamelServletCreator(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Register the Camel Servlet with the HTTP Service
     */
    public void registerServlet() {

        Dictionary props = new Hashtable();
        props.put("alias", SERVLET_PATH);
        props.put("servlet-name", SERVLET_NAME);
        bundleContext.registerService("javax.servlet.Servlet",
                new HttpProxyCamelHttpTransportServlet(), props);
    }
}
