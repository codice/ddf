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
package org.codice.ddf.platform.http.proxy;

import ddf.security.PropertiesLoader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.proxy.http.HttpProxyService;
import org.codice.proxy.http.HttpProxyServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpProxy {

  public static final String KARAF_HOME = "karaf.home";

  public static final String PAX_CONFIG = "org.ops4j.pax.web.cfg";

  public static final String CXF_CONFIG = "org.apache.cxf.osgi.cfg";

  public static final String CXF_CONTEXT = "org.apache.cxf.servlet.context";

  public static final String CONFIG_DIR = "etc";

  public static final String SECURE_PORT_PROPERTY = "org.osgi.service.http.port.secure";

  public static final String PORT_PROPERTY = "org.osgi.service.http.port";

  public static final String SECURE_ENABLED_PROPERTY = "org.osgi.service.http.secure.enabled";

  public static final String HTTP_ENABLED_PROPERTY = "org.osgi.service.http.enabled";

  public static final String GZIP_ENCODING = "gzip";

  private static final String UNKNOWN_TARGET = "0.0.0.0";

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpProxy.class);

  private final HttpProxyService httpProxyService;

  private String endpointName;

  private String hostname;

  /**
   * Constructor
   *
   * @param httpProxyService - proxy service to use to start a camel proxy
   */
  public HttpProxy(HttpProxyService httpProxyService) {
    this.httpProxyService = httpProxyService;
  }

  /** Starts the HTTP -> HTTPS proxy */
  public void startProxy() throws Exception {
    Properties properties = getProperties();
    stopProxy();

    boolean isSecureEnabled = Boolean.parseBoolean(properties.getProperty(SECURE_ENABLED_PROPERTY));
    boolean isHttpEnabled = Boolean.parseBoolean(properties.getProperty(HTTP_ENABLED_PROPERTY));
    if (isSecureEnabled && !isHttpEnabled) {
      String httpPort = properties.getProperty(PORT_PROPERTY);
      String httpsPort = properties.getProperty(SECURE_PORT_PROPERTY);
      String cxfContext = properties.getProperty(CXF_CONTEXT);
      String host;
      try {
        host =
            StringUtils.isNotBlank(hostname) ? hostname : InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException e) {
        LOGGER.warn(
            "Unable to determine hostname, using localhost instead. Check the configuration of the system. Set logging to DEBUG for more details.");
        LOGGER.debug(
            "Unable to determine hostname, using localhost instead. Check the configuration of the system.",
            e);
        host = "localhost";
      }
      endpointName =
          ((HttpProxyServiceImpl) httpProxyService)
              .start(
                  UNKNOWN_TARGET + ":" + httpPort,
                  "https://" + host + ":" + httpsPort,
                  120000,
                  true,
                  new PolicyRemoveBean(httpPort, httpsPort, host, cxfContext));
    }
  }

  /**
   * Returns the pax web properties.
   *
   * @return Properties - contains pax web properties
   */
  Properties getProperties() throws IOException {
    File paxConfig =
        new File(
            System.getProperty(KARAF_HOME)
                + File.separator
                + CONFIG_DIR
                + File.separator
                + PAX_CONFIG);
    File cxfConfig =
        new File(
            System.getProperty(KARAF_HOME)
                + File.separator
                + CONFIG_DIR
                + File.separator
                + CXF_CONFIG);
    Properties properties = new Properties();
    if (paxConfig.exists() && cxfConfig.exists()) {
      properties.putAll(PropertiesLoader.loadProperties(paxConfig.getAbsolutePath()));
      properties.putAll(PropertiesLoader.loadProperties(cxfConfig.getAbsolutePath()));
    }
    return properties;
  }

  /** Stops the HTTP -> HTTPS proxy */
  public void stopProxy() throws Exception {
    if (endpointName != null) {
      httpProxyService.stop(endpointName);
    }
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public static class PolicyRemoveBean {

    private final String httpPort;

    private final String httpsPort;

    private final String host;

    private final String cxfContext;

    public PolicyRemoveBean(String httpPort, String httpsPort, String host, String cxfContext) {
      this.httpPort = httpPort;
      this.httpsPort = httpsPort;
      this.host = host;
      this.cxfContext = cxfContext;
    }

    public void rewrite(Exchange exchange) throws IOException {
      try {
        Message httpMessage = exchange.getIn();
        if (httpMessage != null) {
          String encoding = (String) httpMessage.getHeader(Exchange.CONTENT_ENCODING);
          if (encoding == null || !encoding.contains(GZIP_ENCODING)) {
            Object body = httpMessage.getBody();
            if (body != null) {
              String bodyStr;
              if (body instanceof InputStream) {
                bodyStr = IOUtils.toString((InputStream) body, StandardCharsets.UTF_8);
                IOUtils.closeQuietly((InputStream) body);
              } else if (body instanceof String) {
                bodyStr = (String) body;
              } else {
                bodyStr = body.toString();
              }

              String queryString = (String) httpMessage.getHeader(Exchange.HTTP_QUERY);
              String httpUri = (String) httpMessage.getHeader(Exchange.HTTP_URI);
              boolean isWsdlOrWadl =
                  "wsdl".equalsIgnoreCase(queryString)
                      || "_wadl".equalsIgnoreCase(queryString)
                      || httpUri.equals(cxfContext);

              if (isWsdlOrWadl || (queryString != null && queryString.endsWith(".xsd"))) {
                bodyStr =
                    bodyStr.replaceAll(
                        "https://" + host + ":" + httpsPort, "http://" + host + ":" + httpPort);
              }
              exchange.getIn().setBody(bodyStr);
            }
          }
        }
      } catch (Exception e) {
        LOGGER.debug("Exception occurred while processing policy removal bean.", e);
      }
    }
  }
}
