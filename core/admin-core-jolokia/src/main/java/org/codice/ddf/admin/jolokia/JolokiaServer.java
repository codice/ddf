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
package org.codice.ddf.admin.jolokia;

import static org.jolokia.config.ConfigKey.AGENT_CONTEXT;

import java.util.Dictionary;
import java.util.Hashtable;

import javax.servlet.ServletException;

import org.jolokia.config.ConfigKey;
import org.jolokia.osgi.servlet.JolokiaServlet;
import org.jolokia.util.NetworkUtil;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JolokiaServer {

    private static final String CONFIG_PREFIX = "org.jolokia";

    private BundleContext bundleContext;

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Registers the Jolokia servlet using the passed-in classes.
     * 
     * @param jolokiaServlet
     *            Servlet instance to register.
     * @param httpService
     *            HTTPService used to register servlet to.
     * @param httpContext
     *            HTTPContext to use with registration.
     * @param bundleContext
     *            Current bundlecontext
     * @throws ServletException
     * @throws NamespaceException
     */
    public JolokiaServer(JolokiaServlet jolokiaServlet, HttpService httpService,
            HttpContext httpContext, BundleContext bundleContext) throws ServletException,
            NamespaceException {
        this.bundleContext = bundleContext;

        httpService.registerServlet(getConfiguration(AGENT_CONTEXT), jolokiaServlet,
                getConfiguration(), httpContext);
        logger.debug("Jolokia servlet has been registered with the http service.");
    }

    /**
     * Retrieves all of the configurations for jolokia.
     * 
     * @see <a
     *      href="https://github.com/rhuss/jolokia/blob/v1.2.1/agent/osgi/src/main/java/org/jolokia/osgi/JolokiaActivator.java">From
     *      Jolokia source</a>
     * 
     * @return all configurations for Jolokia.
     */
    private Dictionary<String, String> getConfiguration() {
        Dictionary<String, String> config = new Hashtable<String, String>();
        for (ConfigKey key : ConfigKey.values()) {
            String value = getConfiguration(key);
            if (value != null) {
                config.put(key.getKeyValue(), value);
            }
        }
        String jolokiaId = config.get(ConfigKey.AGENT_ID.getKeyValue());
        if (jolokiaId == null) {
            config.put(ConfigKey.AGENT_ID.getKeyValue(), NetworkUtil.getAgentId(hashCode(), "osgi"));
        }
        config.put(ConfigKey.AGENT_TYPE.getKeyValue(), "osgi");
        return config;
    }

    /**
     * Get a specific configuration.
     * 
     * @param pKey
     *            Key for the configuration wanted.
     * @see <a
     *      href="https://github.com/rhuss/jolokia/blob/v1.2.1/agent/osgi/src/main/java/org/jolokia/osgi/JolokiaActivator.java">From
     *      Jolokia source</a>
     * @return String of the configuration value.
     */
    private String getConfiguration(ConfigKey pKey) {
        String value = bundleContext.getProperty(CONFIG_PREFIX + "." + pKey.getKeyValue());
        if (value == null) {
            value = pKey.getDefaultValue();
        }
        return value;
    }

}
