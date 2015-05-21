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
package org.codice.ddf.platform.http.proxy;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.codice.proxy.http.HttpProxyService;
import org.codice.proxy.http.HttpProxyServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpProxy.class);

    public static final String KARAF_HOME = "karaf.home";

    public static final String PAX_CONFIG = "org.ops4j.pax.web.cfg";

    public static final String CONFIG_DIR = "etc";

    public static final String SECURE_PORT_PROPERTY = "org.osgi.service.http.port.secure";

    public static final String PORT_PROPERTY = "org.osgi.service.http.port";

    public static final String SECURE_ENABLED_PROPERTY = "org.osgi.service.http.secure.enabled";

    public static final String HTTP_ENABLED_PROPERTY = "org.osgi.service.http.enabled";

    private final HttpProxyService httpProxyService;

    private String endpointName;

    private String hostname;

    /**
     * Constructor
     * @param httpProxyService - proxy service to use to start a camel proxy
     */
    public HttpProxy(HttpProxyService httpProxyService) {
        this.httpProxyService = httpProxyService;
    }

    /**
     * Starts the HTTP -> HTTPS proxy
     */
    public void startProxy() throws Exception {
        Properties properties = getProperties();
        stopProxy();

        boolean isSecureEnabled = Boolean
                .valueOf(properties.getProperty(SECURE_ENABLED_PROPERTY));
        boolean isHttpEnabled = Boolean.valueOf(properties.getProperty(HTTP_ENABLED_PROPERTY));
        if (isSecureEnabled && !isHttpEnabled) {
            String httpPort = properties.getProperty(PORT_PROPERTY);
            String httpsPort = properties.getProperty(SECURE_PORT_PROPERTY);
            String host;
            try {
                host = StringUtils.isNotBlank(hostname) ?
                        hostname :
                        InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LOGGER.warn("Unable to determine hostname, using localhost instead.", e);
                host = "localhost";
            }
            endpointName = ((HttpProxyServiceImpl) httpProxyService)
                    .start("0.0.0.0:" + httpPort, "https://" + host + ":" + httpsPort, 1000, true);
        }
    }

    /**
     * Returns the pax web properties.
     * @return Properties - contains pax web properties
     */
    Properties getProperties() {
        File paxConfig = new File(
                System.getProperty(KARAF_HOME) + File.separator + CONFIG_DIR + File.separator
                        + PAX_CONFIG);
        Properties properties = new Properties();
        if (paxConfig.exists()) {
            try {
                properties.load(new FileReader(paxConfig));
            } catch (IOException e) {
                LOGGER.error("Error reading file {}, not starting proxy.", PAX_CONFIG, e);
            }
        }
        return properties;
    }

    /**
     * Stops the HTTP -> HTTPS proxy
     */
    public void stopProxy() throws Exception {
        if (endpointName != null) {
            httpProxyService.stop(endpointName);
        }
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
