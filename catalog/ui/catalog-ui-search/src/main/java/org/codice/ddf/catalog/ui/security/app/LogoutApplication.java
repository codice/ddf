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
package org.codice.ddf.catalog.ui.security.app;

import static spark.Spark.get;

import org.apache.http.HttpStatus;
import org.codice.ddf.security.logout.service.LogoutService;
import spark.servlet.SparkApplication;

public class LogoutApplication implements SparkApplication {

  private LogoutService logoutService;

  public LogoutApplication(LogoutService logoutService) {
    this.logoutService = logoutService;
  }

  @Override
  public void init() {
    get(
        "/logout/actions",
        (req, res) -> {
          String jsonString = logoutService.getActionProviders(req.raw(), res.raw());

          res.status(HttpStatus.SC_OK);
          res.header("Cache-Control", "no-cache, no-store");
          res.header("Pragma", "no-cache");
          return jsonString;
        });
  }
}
