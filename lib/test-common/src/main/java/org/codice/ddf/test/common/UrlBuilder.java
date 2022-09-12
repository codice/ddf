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
package org.codice.ddf.test.common;

import java.net.MalformedURLException;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Builder class for URLs. */
public class UrlBuilder {
  private static final Logger LOGGER = LoggerFactory.getLogger(UrlBuilder.class);

  private final URL rootContextUrl;

  private final URL secureRootContextUrl;

  /**
   * Creates a {@link UrlBuilder} from static values.
   *
   * @param hostname host name to use in the URL
   * @param port port to use in the URL
   * @param securePort secure port to use in the URL
   * @param rootContext root context of the URL
   * @return new {@link UrlBuilder} instance
   */
  public static UrlBuilder from(
      String hostname, String port, String securePort, String rootContext) {
    return new UrlBuilder(hostname, port, securePort, rootContext);
  }

  /**
   * Creates a {@link UrlBuilder} from system property values.
   *
   * @param hostnameSystemProperty name of the system property that contains the host name to use in
   *     the URL
   * @param portSystemProperty name of the system property that contains the port to use in the URL
   * @param securePortSystemProperty name of the system property that contains the secure port to
   *     use in the URL
   * @param rootContextSystemProperty name of the system property that contains the root context of
   *     the URL
   * @return new {@link UrlBuilder} instance
   */
  public static UrlBuilder fromSystemProperties(
      String hostnameSystemProperty,
      String portSystemProperty,
      String securePortSystemProperty,
      String rootContextSystemProperty) {

    return new UrlBuilder(
        System.getProperty(hostnameSystemProperty),
        System.getProperty(portSystemProperty),
        System.getProperty(securePortSystemProperty),
        System.getProperty(rootContextSystemProperty));
  }

  /**
   * Adds a context path to an existing URL. The method will take care of adding and/or removing any
   * extraneous {@code /} at the beginning or end of the context path provided to guarantee that the
   * URL is always valid.
   *
   * @param contextPath context path to add to the URL
   * @return this {@link UrlBuilder}
   */
  public UrlBuilder add(String contextPath) {
    String cleanContextPath = cleanContextPath(contextPath);

    try {
      return new UrlBuilder(
          new URL(String.format("%s/%s", rootContextUrl.toString(), cleanContextPath)),
          new URL(String.format("%s/%s", secureRootContextUrl.toString(), cleanContextPath)));
    } catch (MalformedURLException e) {
      String message = "Cannot build valid URL using context path " + contextPath;
      LOGGER.error(message, e);
      throw new IllegalArgumentException(message, e);
    }
  }

  /**
   * Builds an http URL from the base URL information provided and all the context paths added.
   *
   * @return http URL
   */
  public URL build() {
    return rootContextUrl;
  }

  /**
   * Builds an https URL from the base URL information provided and all the context paths added.
   *
   * @return https URL
   */
  public URL buildSecure() {
    return secureRootContextUrl;
  }

  private UrlBuilder(URL rootContextUrl, URL secureRootContextUrl) {
    this.rootContextUrl = rootContextUrl;
    this.secureRootContextUrl = secureRootContextUrl;
    LOGGER.debug("UrlBuilder created for {}, {}", rootContextUrl, secureRootContextUrl);
  }

  private UrlBuilder(String hostname, String port, String securePort, String rootContext) {

    try {
      this.rootContextUrl =
          new URL(String.format("http://%s:%s/%s", hostname, port, cleanContextPath(rootContext)));
      this.secureRootContextUrl =
          new URL(
              String.format(
                  "https://%s:%s/%s", hostname, securePort, cleanContextPath(rootContext)));
      LOGGER.debug("UrlBuilder created for {}, {}", rootContextUrl, secureRootContextUrl);
    } catch (MalformedURLException e) {
      String message =
          String.format(
              "Cannot build valid URL using hostname %s, port %s and secure port %s",
              hostname, port, securePort);
      LOGGER.error(message, e);
      throw new IllegalArgumentException(message, e);
    }
  }

  private String cleanContextPath(String contextPath) {
    String cleanContextPath = StringUtils.removeEnd(contextPath, "/");
    return StringUtils.removeStart(cleanContextPath, "/");
  }
}
