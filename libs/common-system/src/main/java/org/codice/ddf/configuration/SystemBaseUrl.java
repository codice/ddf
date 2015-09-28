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

    public static final String ROOT_CONTEXT = "org.codice.ddf.system.rootContext";

    public static final String DEFAULT_HTTP_PORT = "8181";

    public static final String DEFAULT_HTTPS_PORT = "8993";

    public static final String DEFAULT_HOST = "localhost";

    public static final String DEFAULT_PROTOCOL = "https://";

    public SystemBaseUrl() {

    }

    /**
     * Gets the port number based on the the system protocol
     *
     * @return
     */
    public String getPort() {
        return getPort(getProtocol());
    }

    /**
     * Constructs and returns the base url for the system using the system default protocol
     *
     * @return
     */
    public String getBaseUrl() {
        return getBaseUrl(getProtocol());
    }

    /**
     * Constructs and returns the base url for the system given a protocol
     *
     * @param proto Protocol to use during url construction. A null value will
     *              cause the system default protocol to be used
     * @return
     */
    public String getBaseUrl(String proto) {
        return constructUrl(proto, null);
    }

    /**
     * Construct a url for the given context
     *
     * @param context The context path to be appened to the end of the base url
     * @return
     */
    public String constructUrl(String context) {
        return constructUrl(getProtocol(), context);
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
        StringBuilder sb = new StringBuilder();
        String protocol = proto;
        if (protocol == null) {
            protocol = getProtocol();
        }
        sb.append(protocol);

        if (!sb.toString().endsWith("://")) {
            sb.append("://");
        }
        sb.append(getHost());
        sb.append(":");
        sb.append(getPort(protocol));

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
     * @param proto protocol (http or https)
     * @return
     */
    public String getPort(String proto) {
        if (proto != null && !proto.startsWith("https")) {
            return getHttpPort();
        } else {
            return getHttpsPort();
        }
    }

    public String getHttpPort() {
        return System.getProperty(HTTP_PORT, DEFAULT_HTTP_PORT);
    }

    public String getHttpsPort() {
        return System.getProperty(HTTPS_PORT, DEFAULT_HTTPS_PORT);
    }

    public String getHost() {
        return System.getProperty(HOST, DEFAULT_HOST);
    }

    public String getProtocol() {
        return System.getProperty(PROTOCOL, DEFAULT_PROTOCOL);
    }

    public String getRootContext() {
        return System.getProperty(ROOT_CONTEXT, "");
    }
}
