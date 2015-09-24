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
package org.codice.ddf.configuration;

/**
 * An immutable utility class for getting system url information. Information is pulled from
 * the system properties.
 */
public final class SystemBaseUrl {

    public static final String HTTP_PORT = "org.codice.ddf.system.httpPort";

    public static final String HTTPS_PORT = "org.codice.ddf.system.httpsPort";

    public static final String HOST = "org.codice.ddf.system.hostname";

    public static final String PROTOCOL = "org.codice.ddf.system.protocol";

    public static final String EXT_HTTP_PORT = "org.codice.ddf.system.externalHttpPort";

    public static final String EXT_HTTPS_PORT = "org.codice.ddf.system.externalHttpsPort";

    public static final String EXT_HOST = "org.codice.ddf.system.externalHostname";

    public static final String ROOT_CONTEXT = "org.codice.ddf.system.rootContext";

    public static final String DEFAULT_HTTP_PORT = "8181";

    public static final String DEFAULT_HTTPS_PORT = "8993";

    public static final String DEFAULT_HOST = "localhost";

    public static final String DEFAULT_PROTOCOL = "https://";

    public static final String DEFAULT_EXT_HTTP_PORT = DEFAULT_HTTP_PORT;

    public static final String DEFAULT_EXT_HTTPS_PORT = DEFAULT_HTTPS_PORT;

    public static final String DEFAULT_EXT_HOST = DEFAULT_HOST;

    public static final String DEFAULT_ROOT_CONTEXT = "/services";

    public SystemBaseUrl() {

    }

    /**
     * Gets the port number based on the the system protocol
     *
     * @return
     */
    public String getPort() {
        return getPort(getProtocol(), false);
    }

    /**
     * Gets the port number based on the the system protocol
     *
     * @return
     */
    public String getExternalPort() {
        return getPort(getProtocol(), true);
    }

    /**
     * Constructs and returns the base url for the system using the system default protocol
     *
     * @return
     */
    public String getBaseUrl() {
        return getBaseUrl(false);
    }

    /**
     * Constructs and returns the base url for the system using the system default protocol
     *
     * @param external If true will return the external url
     * @return
     */
    public String getBaseUrl(boolean external) {
        return getBaseUrl(getProtocol(), external);
    }

    /**
     * Constructs and returns the base url for the system given a protocol
     *
     * @param proto    Protocol to use during url construction. A null value will
     *                 cause the system default protocol to be used
     * @param external If true will return the external url
     * @return
     */
    public String getBaseUrl(String proto, boolean external) {
        return constructUrl(proto, null, external);
    }

    /**
     * Construct a url for the given context
     *
     * @param context The context path to be appened to the end of the base url
     * @return
     */
    public String constructUrl(String context) {
        return constructUrl(getProtocol(), context, false);
    }

    /**
     * Construct a url based on the protocol and context
     *
     * @param proto   Protocol to use during url construction. A null value will
     *                cause the system default protocol to be used
     * @param context The context path to be appened to the end of the base url
     * @return
     */
    public String constructUrl(String proto, String context) {
        return constructUrl(proto, context, false);
    }

    /**
     * Construct a url based on the protocol and context
     *
     * @param proto    Protocol to use during url construction. A null value will
     *                 cause the system default protocol to be used
     * @param context  The context path to be appened to the end of the base url
     * @param external If true will return the external url
     * @return
     */
    public String constructUrl(String proto, String context, boolean external) {
        StringBuilder sb = new StringBuilder();
        if (proto == null) {
            sb.append(getProtocol());
        } else {
            sb.append(proto);
        }
        if (!sb.toString().endsWith("://")) {
            sb.append("://");
        }
        sb.append(external ? getExternalHost() : getHost());
        sb.append(":");
        sb.append(getPort(proto, external));

        if (context != null) {
            if (!context.startsWith("/")) {
                sb.append("/");
            }
            sb.append(context);
        }

        return sb.toString();
    }

    /**
     * Gets the port number for the given protocol. If the protocol is null or not one of
     * http or https returns the https port number.
     *
     * @param proto    protocol (http or https)
     * @param external return external port
     * @return
     */
    public String getPort(String proto, boolean external) {
        if (proto != null && !proto.startsWith("https")) {
            return external ? getExternalHttpPort() : getHttpPort();
        } else {
            return external ? getExternalHttpsPort() : getHttpsPort();
        }
    }

    public String getExternalHost() {
        return getProperty(EXT_HOST, DEFAULT_EXT_HOST);
    }

    public String getHttpPort() {
        return getProperty(HTTP_PORT, DEFAULT_EXT_HTTP_PORT);
    }

    public String getHttpsPort() {
        return getProperty(HTTPS_PORT, DEFAULT_HTTPS_PORT);
    }

    public String getHost() {
        return getProperty(HOST, DEFAULT_HOST);
    }

    public String getProtocol() {
        return getProperty(PROTOCOL, DEFAULT_PROTOCOL);
    }

    public String getExternalHttpPort() {
        return getProperty(EXT_HTTP_PORT, DEFAULT_EXT_HTTP_PORT);
    }

    public String getExternalHttpsPort() {
        return getProperty(EXT_HTTPS_PORT, DEFAULT_EXT_HTTPS_PORT);
    }

    public String getRootContext() {
        return getProperty(ROOT_CONTEXT, DEFAULT_ROOT_CONTEXT);
    }

    private String getProperty(String propName, String defaultValue) {
        String prop = System.getProperty(propName);
        if (prop == null) {
            prop = defaultValue;
        }
        return prop;
    }
}
