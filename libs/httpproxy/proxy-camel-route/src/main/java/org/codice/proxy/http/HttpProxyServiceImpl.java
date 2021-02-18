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
package org.codice.proxy.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.servlet.ServletComponent;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Http Proxy service which creates a Camel based http proxy
 *
 * @author ddf
 */
public class HttpProxyServiceImpl implements HttpProxyService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpProxyServiceImpl.class);

  public static final String SERVLET_NAME = "CamelServlet";

  private static final String SERVLET_COMPONENT = "servlet";

  public static final String GENERIC_ENDPOINT_NAME = "endpoint";

  public static final String HTTP_PROXY_KEY = "http.";

  public static final String HTTPS_PROXY_KEY = "https.";

  public static final String HTTP_PROXY_HOST_KEY = "proxyHost";

  public static final String HTTP_PROXY_PORT_KEY = "proxyPort";

  public static final String HTTP_PROXY_AUTH_METHOD_KEY = "proxyAuthMethod";

  public static final String HTTP_PROXY_AUTH_USERNAME_KEY = "proxyAuthUsername";

  @SuppressWarnings("squid:S2068" /* Default password only */)
  public static final String HTTP_PROXY_AUTH_PASSWORD_KEY = "proxyAuthPassword";

  public static final String HTTP_PROXY_AUTH_DOMAIN_KEY = "proxyAuthDomain";

  public static final String HTTP_PROXY_AUTH_HOST_KEY = "proxyAuthHost";

  private static final int DEFAULT_TIMEOUT_MS = 5000;

  private static final String SERVLET = "servlet";

  int incrementer = 0;

  private CamelContext camelContext = null;

  private String routeEndpointType = SERVLET;

  private final Set<String> endpointIds = Collections.synchronizedSet(new HashSet<>());

  public HttpProxyServiceImpl(CamelContext camelContext, String endpointType)
      throws java.io.IOException {
    this(camelContext);
    this.routeEndpointType = endpointType;
    if (!this.routeEndpointType.equals(SERVLET)) {
      this.camelContext.removeComponent(SERVLET_COMPONENT);
    }
  }

  public HttpProxyServiceImpl(CamelContext camelContext) throws java.io.IOException {
    this.camelContext = camelContext;

    // Add servlet to the Camel Context
    try (ServletComponent servlet = new ServletComponent()) {
      servlet.setCamelContext(camelContext);
      servlet.setServletName(SERVLET_NAME);
      if (camelContext != null) {
        this.camelContext.addComponent(SERVLET_COMPONENT, servlet);
      }
    } catch (IOException e) {
      LOGGER.debug("Could not get servlet for context", e);
    }
  }

  @Override
  public synchronized String start(String targetUri, Integer timeout) throws Exception {
    String endpointName = GENERIC_ENDPOINT_NAME + incrementer;
    if (timeout <= 0) {
      timeout = DEFAULT_TIMEOUT_MS;
    }
    start(endpointName, targetUri, timeout);
    incrementer++;

    return endpointName;
  }

  @Override
  public String start(final String endpointName, final String targetUri, final Integer timeout)
      throws Exception {
    return start(endpointName, targetUri, timeout, false, null);
  }

  @SuppressWarnings("squid:CallToDeprecatedMethod" /* Calling deprecated CTOR for Protocol. */)
  public String start(
      final String endpointName,
      final String targetUri,
      final Integer timeout,
      final boolean matchOnPrefix,
      final Object bean)
      throws Exception {

    // Enable proxy settings for the external target
    enableProxySettings();

    final String matchPrefix = (matchOnPrefix) ? "?matchOnUriPrefix=true" : "";

    final String protocolDelimiter = (routeEndpointType.equals(SERVLET)) ? ":///" : "://";

    // Create Camel route
    RouteBuilder routeBuilder;
    if (bean == null) {
      routeBuilder =
          new RouteBuilder() {
            @Override
            public void configure() throws Exception {
              from(routeEndpointType + protocolDelimiter + endpointName + matchPrefix)
                  .removeHeader("Authorization")
                  .removeHeader("Cookie")
                  .to(
                      targetUri
                          + "?bridgeEndpoint=true&throwExceptionOnFailure=false&httpClient.socketTimeout="
                          + timeout
                          + "&httpClient.connectionRequestTimeout="
                          + timeout)
                  .routeId(endpointName);
            }
          };
    } else {
      routeBuilder =
          new RouteBuilder() {
            @Override
            public void configure() throws Exception {
              from(routeEndpointType + protocolDelimiter + endpointName + matchPrefix)
                  .removeHeader("Authorization")
                  .removeHeader("Cookie")
                  .to(
                      targetUri
                          + "?bridgeEndpoint=true&throwExceptionOnFailure=false&httpClient.socketTimeout="
                          + timeout
                          + "&httpClient.connectionRequestTimeout="
                          + timeout)
                  .routeId(endpointName)
                  .bean(bean);
            }
          };
    }
    camelContext.addRoutes(routeBuilder);
    camelContext.start();
    LOGGER.debug(
        "Started proxy route at servlet endpoint: {}, routing to: {}", endpointName, targetUri);
    endpointIds.add(endpointName);
    return endpointName;
  }

  /** Enable external proxy settings */
  private void enableProxySettings() {

    // Fetch all proxy settings and add settings to Camel context if not null, whitespace or
    // empty
    ArrayList<String> sysProxyConfigs = new ArrayList<>();
    sysProxyConfigs.add(HTTP_PROXY_KEY + HTTP_PROXY_HOST_KEY);
    sysProxyConfigs.add(HTTP_PROXY_KEY + HTTP_PROXY_PORT_KEY);
    sysProxyConfigs.add(HTTP_PROXY_KEY + HTTP_PROXY_AUTH_METHOD_KEY);
    sysProxyConfigs.add(HTTP_PROXY_KEY + HTTP_PROXY_AUTH_USERNAME_KEY);
    sysProxyConfigs.add(HTTP_PROXY_KEY + HTTP_PROXY_AUTH_PASSWORD_KEY);
    sysProxyConfigs.add(HTTP_PROXY_KEY + HTTP_PROXY_AUTH_DOMAIN_KEY);
    sysProxyConfigs.add(HTTP_PROXY_KEY + HTTP_PROXY_AUTH_HOST_KEY);
    sysProxyConfigs.add(HTTPS_PROXY_KEY + HTTP_PROXY_HOST_KEY);
    sysProxyConfigs.add(HTTPS_PROXY_KEY + HTTP_PROXY_PORT_KEY);
    sysProxyConfigs.add(HTTPS_PROXY_KEY + HTTP_PROXY_AUTH_METHOD_KEY);
    sysProxyConfigs.add(HTTPS_PROXY_KEY + HTTP_PROXY_AUTH_USERNAME_KEY);
    sysProxyConfigs.add(HTTPS_PROXY_KEY + HTTP_PROXY_AUTH_PASSWORD_KEY);
    sysProxyConfigs.add(HTTPS_PROXY_KEY + HTTP_PROXY_AUTH_DOMAIN_KEY);
    sysProxyConfigs.add(HTTPS_PROXY_KEY + HTTP_PROXY_AUTH_HOST_KEY);

    for (String sysProxyConfig : sysProxyConfigs) {
      String prop = System.getProperty(sysProxyConfig);
      if (StringUtils.isNotBlank(prop)) {
        LOGGER.debug("Enabling Proxy Property: {}", sysProxyConfig);
        camelContext.getGlobalOptions().put(sysProxyConfig, prop);
      }
    }
  }

  @Override
  public void stop(String endpointName) throws Exception {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Stopping proxy route at endpoint: {}", endpointName);
      LOGGER.debug("Route list before = {}", Arrays.toString(camelContext.getRoutes().toArray()));
    }
    camelContext.getRouteController().stopRoute(endpointName);
    camelContext.removeRoute(endpointName);
    endpointIds.remove(endpointName);
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Route list after = {}", Arrays.toString(camelContext.getRoutes().toArray()));
    }
  }

  public void destroy() {
    try {
      Object[] objects = endpointIds.toArray();
      for (Object endpointId : objects) {
        stop((String) endpointId);
      }
      camelContext.stop();
    } catch (Exception e) {
      LOGGER.debug(e.getMessage());
    }
    camelContext.removeComponent(SERVLET_COMPONENT);
  }
}
