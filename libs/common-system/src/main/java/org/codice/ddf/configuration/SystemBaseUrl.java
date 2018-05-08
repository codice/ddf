/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration;

/**
 * An immutable utility class for getting system url information. Information is pulled from the
 * system properties.
 */
public final class SystemBaseUrl {

  /* Internal Property Keys */
  public static final String HTTP_PORT = "org.codice.ddf.system.httpPort";

  public static final String HTTPS_PORT = "org.codice.ddf.system.httpsPort";

  public static final String PORT = "org.codice.ddf.system.port";

  public static final String HOST = "org.codice.ddf.system.hostname";

  public static final String PROTOCOL = "org.codice.ddf.system.protocol";

  /* External Property Keys */
  public static final String EXTERNAL_HTTP_PORT = "org.codice.ddf.external.httpPort";

  public static final String EXTERNAL_HTTPS_PORT = "org.codice.ddf.external.httpsPort";

  public static final String EXTERNAL_PORT = "org.codice.ddf.external.port";

  public static final String EXTERNAL_HOST = "org.codice.ddf.external.hostname";

  public static final String EXTERNAL_PROTOCOL = "org.codice.ddf.external.protocol";

  /* Shared Property Keys */
  public static final String ROOT_CONTEXT = "org.codice.ddf.system.rootContext";

  /* Defaults */
  public static final String DEFAULT_HTTP_PORT = "8181";

  public static final String DEFAULT_HTTPS_PORT = "8993";

  public static final String DEFAULT_HOST = "localhost";

  public static final String DEFAULT_PROTOCOL = "https://";

  /* Accessor Fields to be set based on External or Internal*/
  private String httpPort;

  private String httpsPort;

  private String port;

  private String host;

  private String protocol;

  /* EXTERNAL and INTERNAL singleton objects */
  public static final SystemBaseUrl EXTERNAL = new SystemBaseUrl(true);
  public static final SystemBaseUrl INTERNAL = new SystemBaseUrl(false);

  private SystemBaseUrl(boolean external) {
    if (external) externalMode();
    else internalMode();
  }

  private void externalMode() {
    httpPort = EXTERNAL_HTTP_PORT;
    httpsPort = EXTERNAL_HTTPS_PORT;
    port = EXTERNAL_PORT;
    host = EXTERNAL_HOST;
    protocol = EXTERNAL_PROTOCOL;
  }

  private void internalMode() {
    httpPort = HTTP_PORT;
    httpsPort = HTTPS_PORT;
    port = PORT;
    host = HOST;
    protocol = PROTOCOL;
  }

  /**
   * Gets the port number based on the the system protocol
   *
   * @return
   */
  public String getPortString() {
    String port = System.getProperty(this.port);
    if (port == null) {
      port = getPortString(getProtocolString());
    }
    return port;
  }

  /**
   * Constructs and returns the base url for the system using the system default protocol
   *
   * @return
   */
  public String getBaseUrlString() {
    return getBaseUrlString(getProtocolString());
  }

  /**
   * Constructs and returns the base url for the system given a protocol
   *
   * @param proto Protocol to use during url construction. A null value will cause the system
   *     default protocol to be used
   * @return
   */
  public String getBaseUrlString(String proto) {
    return constructUrlString(proto, null);
  }

  /**
   * Construct a url for the given context
   *
   * @param context The context path to be appened to the end of the base url
   * @return
   */
  public String constructUrlString(String context) {
    return constructUrlString(getProtocolString(), context);
  }

  /**
   * Construct a url for the given context
   *
   * @param context The context path to be appened to the end of the base url
   * @param includeRootContext Flag to indicated whether the rootcontext should be included in the
   *     url.
   * @return
   */
  public String constructUrlString(String context, boolean includeRootContext) {
    return constructUrlString(getProtocolString(), context, includeRootContext);
  }

  /**
   * Construct a url based on the protocol and context
   *
   * @param proto Protocol to use during url construction. A null value will cause the system
   *     default protocol to be used
   * @param context The context path to be appened to the end of the base url
   * @return
   */
  public String constructUrlString(String proto, String context) {
    return constructUrlString(proto, context, false);
  }

  /**
   * Construct a url based on the protocol and context
   *
   * @param proto Protocol to use during url construction. A null value will cause the system
   *     default protocol to be used
   * @param context The context path to be appened to the end of the base url
   * @param includeRootContext Flag to indicated whether the rootcontext should be included in the
   *     url.
   * @return
   */
  public String constructUrlString(String proto, String context, boolean includeRootContext) {
    StringBuilder sb = new StringBuilder();
    String protocol = proto;
    if (protocol == null) {
      protocol = getProtocolString();
    }
    sb.append(protocol);

    if (!sb.toString().endsWith("://")) {
      sb.append("://");
    }
    sb.append(getHostString());
    sb.append(":");

    sb.append(getPortString(protocol));

    if (includeRootContext) {
      if (!getRootContextString().startsWith("/")) {
        sb.append("/");
      }
      sb.append(getRootContextString());
    }

    if (context != null) {
      if (!context.startsWith("/")) {
        sb.append("/");
      }
      sb.append(context);
    }

    return sb.toString();
  }

  /**
   * Gets the port number for the given protocol. If the protocol is null or not one of http or
   * https returns the https port number.
   *
   * @param proto protocol (http or https)
   * @return
   */
  public String getPortString(String proto) {
    if (proto != null && !proto.startsWith("https")) {
      return getHttpPortString();
    } else {
      return getHttpsPortString();
    }
  }

  public String getHttpPortString() {
    return System.getProperty(this.httpPort, DEFAULT_HTTP_PORT);
  }

  public String getHttpsPortString() {
    return System.getProperty(this.httpsPort, DEFAULT_HTTPS_PORT);
  }

  /**
   * Gets the current host name or IP address from the system properties, or {@link #DEFAULT_HOST}
   * if not set.
   *
   * @return host name, IP address or {@link #DEFAULT_HOST}
   */
  public String getHostString() {
    return System.getProperty(this.host, DEFAULT_HOST);
  }

  public String getProtocolString() {
    return System.getProperty(this.protocol, DEFAULT_PROTOCOL);
  }

  public String getRootContextString() {
    return System.getProperty(this.ROOT_CONTEXT, "");
  }

  /*Backwards compatible static calls assumes INTERNAL*/
  public static String getPort() {
    return INTERNAL.getPortString();
  }

  public static String getBaseUrl() {
    return INTERNAL.getBaseUrlString();
  }

  public static String getBaseUrl(String proto) {
    return INTERNAL.getBaseUrlString(proto);
  }

  public static String constructUrl(String context) {
    return INTERNAL.constructUrlString(context);
  }

  public static String constructUrl(String context, boolean includeRootContext) {
    return INTERNAL.constructUrlString(context, includeRootContext);
  }

  public static String constructUrl(String proto, String context) {
    return INTERNAL.constructUrlString(proto, context);
  }

  public static String constructUrl(String proto, String context, boolean includeRootContext) {
    return INTERNAL.constructUrlString(proto, context, includeRootContext);
  }

  public static String getPort(String proto) {
    return INTERNAL.getPortString(proto);
  }

  public static String getHttpPort() {
    return INTERNAL.getHttpPortString();
  }

  public static String getHttpsPort() {
    return INTERNAL.getHttpsPortString();
  }

  public static String getHost() {
    return INTERNAL.getHostString();
  }

  public static String getProtocol() {
    return INTERNAL.getProtocolString();
  }

  public static String getRootContext() {
    return INTERNAL.getRootContextString();
  }
}
