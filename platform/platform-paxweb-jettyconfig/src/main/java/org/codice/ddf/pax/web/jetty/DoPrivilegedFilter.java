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
package org.codice.ddf.pax.web.jetty;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoPrivilegedFilter implements Filter {

  private static final Logger LOGGER = LoggerFactory.getLogger(DoPrivilegedFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    LOGGER.debug("Starting DoPrivilegedFilter...");
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                chain.doFilter(request, response);
                return null;
              });
    } catch (PrivilegedActionException e) {
      if (e.getException() instanceof IOException) {
        throw (IOException) e.getException();
      } else if (e.getException() instanceof ServletException) {
        throw (ServletException) e.getException();
      } else {
        throw new ServletException(e.getException());
      }
    }
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroying DoPrivilegedFilter...");
  }
}
