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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.platform.filter.http.HttpFilter;
import org.codice.ddf.platform.filter.http.HttpFilterChain;

public class DoPrivilegedFilter implements HttpFilter {

  @Override
  public void doFilter(
      HttpServletRequest request, HttpServletResponse response, HttpFilterChain filterChain)
      throws IOException, ServletException {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                filterChain.doFilter(request, response);
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
}
