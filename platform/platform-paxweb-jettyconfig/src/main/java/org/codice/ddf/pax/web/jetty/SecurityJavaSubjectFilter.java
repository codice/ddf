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

import ddf.security.SecurityConstants;
import java.io.IOException;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.slf4j.LoggerFactory;

public class SecurityJavaSubjectFilter implements Filter {
  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(SecurityJavaSubjectFilter.class);

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    LOGGER.debug("Starting SecurityJavaSubjectFilter...");
  }

  @Override
  public void doFilter(
      ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
      throws IOException, ServletException {

    javax.security.auth.Subject subject =
        (javax.security.auth.Subject)
            servletRequest.getAttribute(SecurityConstants.SECURITY_JAVA_SUBJECT);

    if (subject != null) {
      if (filterChain != null) {
        PrivilegedExceptionAction<Void> action =
            () -> {
              filterChain.doFilter(servletRequest, servletResponse);
              return null;
            };
        try {
          javax.security.auth.Subject.doAs(subject, action);
        } catch (PrivilegedActionException e) {
          LOGGER.error("Could not attach java subject to thread for rest of filters.");
        }
      }
    } else {
      filterChain.doFilter(servletRequest, servletResponse);
      LOGGER.debug("No java subject found to attach to thread.");
    }
  }

  @Override
  public void destroy() {
    // not needed
  }
}
