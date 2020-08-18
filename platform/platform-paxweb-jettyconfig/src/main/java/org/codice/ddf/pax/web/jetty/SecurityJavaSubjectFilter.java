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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.filter.http.HttpFilterChain;
import org.slf4j.LoggerFactory;

public class SecurityJavaSubjectFilter implements HttpFilter {
  private static final org.slf4j.Logger LOGGER =
      LoggerFactory.getLogger(SecurityJavaSubjectFilter.class);

  @Override
  public void doFilter(
      HttpServletRequest request, HttpServletResponse response, HttpFilterChain filterChain)
      throws IOException, ServletException {
    javax.security.auth.Subject subject =
        (javax.security.auth.Subject) request.getAttribute(SecurityConstants.SECURITY_JAVA_SUBJECT);

    if (subject != null) {
      if (filterChain != null) {
        PrivilegedExceptionAction<Void> action =
            () -> {
              filterChain.doFilter(request, response);
              return null;
            };
        try {
          javax.security.auth.Subject.doAs(subject, action);
        } catch (PrivilegedActionException e) {
          LOGGER.error("Could not attach Java subject to thread for rest of filters.");
          LOGGER.debug(
              "Encountered exception while attaching Java subject to thread for rest of filters.",
              e);
        }
      }
    } else {
      filterChain.doFilter(request, response);
      LOGGER.debug("No java subject found to attach to thread.");
    }
  }
}
