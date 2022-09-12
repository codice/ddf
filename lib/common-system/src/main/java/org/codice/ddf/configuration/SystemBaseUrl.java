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

import org.apache.commons.lang.StringUtils;

/**
 * An immutable utility class for getting system url information. Information is pulled from the
 * system properties.
 */
public final class SystemBaseUrl {

  /* Internal Property Keys */
  public static final String INTERNAL_HTTP_PORT = "org.codice.ddf.system.httpPort";

  public static final String INTERNAL_HTTPS_PORT = "org.codice.ddf.system.httpsPort";

  public static final String INTERNAL_PORT = "org.codice.ddf.system.port";

  public static final String INTERNAL_HOST = "org.codice.ddf.system.hostname";

  public static final String INTERNAL_PROTOCOL = "org.codice.ddf.system.protocol";

  public static final String INTERNAL_ROOT_CONTEXT = "org.codice.ddf.system.rootContext";

  /* External Property Keys */
  public static final String EXTERNAL_HTTP_PORT = "org.codice.ddf.external.httpPort";

  public static final String EXTERNAL_HTTPS_PORT = "org.codice.ddf.external.httpsPort";

  public static final String EXTERNAL_PORT = "org.codice.ddf.external.port";

  public static final String EXTERNAL_HOST = "org.codice.ddf.external.hostname";

  public static final String EXTERNAL_PROTOCOL = "org.codice.ddf.external.protocol";

  public static final String EXTERNAL_CONTEXT = "org.codice.ddf.external.context";

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

  private String context;

  /* EXTERNAL and INTERNAL singleton objects */
  public static final SystemBaseUrl EXTERNAL =
      new SystemBaseUrl(
          EXTERNAL_HTTP_PORT,
          EXTERNAL_HTTPS_PORT,
          EXTERNAL_PORT,
          EXTERNAL_HOST,
          EXTERNAL_PROTOCOL,
          EXTERNAL_CONTEXT);
  public static final SystemBaseUrl INTERNAL =
      new SystemBaseUrl(
          INTERNAL_HTTP_PORT,
          INTERNAL_HTTPS_PORT,
          INTERNAL_PORT,
          INTERNAL_HOST,
          INTERNAL_PROTOCOL,
          INTERNAL_ROOT_CONTEXT);

  private SystemBaseUrl(
      String httpPortKey,
      String httpsPortKey,
      String portKey,
      String hostKey,
      String protocolKey,
      String contextKey) {
    httpPort = httpPortKey;
    httpsPort = httpsPortKey;
    port = portKey;
    host = hostKey;
    protocol = protocolKey;
    context = contextKey;
  }

  /**
   * Gets the port number based on the the system protocol
   *
   * @return
   */
  public String getPort() {
    return System.getProperty(this.port, getPort(getProtocol()));
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
   * @param proto Protocol to use during url construction. A null value will cause the system
   *     default protocol to be used
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
   * Construct a url for the given context
   *
   * @param context The context path to be appened to the end of the base url
   * @param includeRootContext Flag to indicated whether the rootcontext should be included in the
   *     url.
   * @return
   */
  public String constructUrl(String context, boolean includeRootContext) {
    return constructUrl(getProtocol(), context, includeRootContext);
  }

  /**
   * Construct a url based on the protocol and context
   *
   * @param proto Protocol to use during url construction. A null value will cause the system
   *     default protocol to be used
   * @param context The context path to be appened to the end of the base url
   * @return
   */
  public String constructUrl(String proto, String context) {
    return constructUrl(proto, context, false);
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
  @SuppressWarnings("squid:HiddenFieldCheck" /* Specific proto might be needed by caller */)
  public String constructUrl(String proto, String context, boolean includeRootContext) {
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

    String externalContext = EXTERNAL.getRootContext();
    if (this.equals(EXTERNAL) && StringUtils.isNotEmpty(externalContext)) {
      if (!externalContext.startsWith("/")) {
        sb.append("/");
      }
      sb.append(externalContext);
    }

    if (includeRootContext) {
      String internalContext = INTERNAL.getRootContext();
      if (!internalContext.startsWith("/")) {
        sb.append("/");
      }
      sb.append(internalContext);
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
  public String getPort(String proto) {
    if (proto != null && !proto.startsWith("https")) {
      return getHttpPort();
    } else {
      return getHttpsPort();
    }
  }

  public String getHttpPort() {
    return System.getProperty(this.httpPort, DEFAULT_HTTP_PORT);
  }

  public String getHttpsPort() {
    return System.getProperty(this.httpsPort, DEFAULT_HTTPS_PORT);
  }

  /**
   * Gets the current host name or IP address from the system properties, or {@link #DEFAULT_HOST}
   * if not set.
   *
   * @return host name, IP address or {@link #DEFAULT_HOST}
   */
  public String getHost() {
    return System.getProperty(this.host, DEFAULT_HOST);
  }

  public String getProtocol() {
    return System.getProperty(this.protocol, DEFAULT_PROTOCOL);
  }

  public String getRootContext() {
    return System.getProperty(this.context, "");
  }
}
