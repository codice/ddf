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
package org.codice.ddf.catalog.ui.session;

import static spark.Spark.get;

import java.net.URI;
import org.codice.ddf.security.session.management.service.SessionManagementService;
import spark.servlet.SparkApplication;

public class SessionManagementApplication implements SparkApplication {

  private SessionManagementService sessionManagement;

  public SessionManagementApplication(SessionManagementService sessionManagementService) {
    sessionManagement = sessionManagementService;
  }

  @Override
  public void init() {
    get(
        "/session/expiry",
        (req, res) -> {
          String body = sessionManagement.getExpiry(req.raw());
          res.status(200);
          return body;
        });

    get(
        "/session/invalidate",
        (req, res) -> {
          URI uri = sessionManagement.getInvalidate(req.raw());
          res.status(200);
          return uri.toString();
        });
  }
}
