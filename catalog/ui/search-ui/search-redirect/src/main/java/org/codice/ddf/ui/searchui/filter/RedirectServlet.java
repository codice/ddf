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
package org.codice.ddf.ui.searchui.filter;

import static org.apache.commons.lang3.Validate.notBlank;

import java.io.IOException;
import java.net.URI;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.configuration.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet for forcing a redirect from the default /search location to the actual web app location.
 *
 * <p>This exists because in some instances we may want to redirect to the Simple Search UI, rather
 * than Intrigue, or even to a load balancer.
 */
public class RedirectServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectServlet.class);

  private static final long serialVersionUID = 1L;

  @SuppressWarnings("squid:S2226" /* Lifecycle managed by blueprint */)
  private transient String defaultUriString;

  /**
   * @throws NullPointerException if {@code defaultUriString} is {@code null}
   * @throws IllegalArgumentException if {@code defaultUriString} is blank or violates RFC 2396
   */
  public RedirectServlet(final String defaultDefaultUriString) {
    setDefaultUri(defaultDefaultUriString);
  }

  @Override
  public void service(
      final HttpServletRequest servletRequest, final HttpServletResponse servletResponse)
      throws IOException {
    servletResponse.sendRedirect(defaultUriString);
  }

  /**
   * @throws NullPointerException if {@code defaultUriStringWithUnresolvedProperties} is {@code
   *     null}
   * @throws IllegalArgumentException if {@code defaultUriStringWithUnresolvedProperties} is blank
   *     or violates RFC 2396
   */
  public void setDefaultUri(final String defaultUriStringWithUnresolvedProperties) {
    final String defaultUriString =
        PropertyResolver.resolveProperties(defaultUriStringWithUnresolvedProperties);

    final URI defaultUri = URI.create(notBlank(defaultUriString));
    if (defaultUri.isAbsolute()) {
      LOGGER.debug("/search will be redirected to an absolute URI: {}", defaultUri);
    } else {
      LOGGER.debug("/search will be redirected to a relative URI: {}", defaultUri);
    }

    this.defaultUriString = defaultUriString;
  }
}
