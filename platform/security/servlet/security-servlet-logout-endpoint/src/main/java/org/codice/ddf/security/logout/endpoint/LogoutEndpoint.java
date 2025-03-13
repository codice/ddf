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
package org.codice.ddf.security.logout.endpoint;

import ddf.security.service.SecurityServiceException;
import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.security.logout.service.LogoutService;

public class LogoutEndpoint extends HttpServlet {

  private LogoutService logoutService;

  public LogoutEndpoint(LogoutService logoutService) {
    this.logoutService = logoutService;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) {
    try {

      String jsonString = logoutService.getActionProviders(req, res);
      res.setHeader("Cache-Control", "no-cache, no-store");
      res.setHeader("Pragma", "no-cache");
      res.getWriter().print(jsonString);
    } catch (SecurityServiceException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
