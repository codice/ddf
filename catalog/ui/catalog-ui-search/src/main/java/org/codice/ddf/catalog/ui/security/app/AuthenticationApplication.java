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

import static spark.Spark.post;

import org.apache.http.HttpStatus;
import org.codice.ddf.security.rest.authentication.service.AuthenticationService;
import spark.servlet.SparkApplication;

public class AuthenticationApplication implements SparkApplication {

  private AuthenticationService authenticationService;

  public AuthenticationApplication(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @Override
  public void init() {
    post(
        "/login",
        (req, res) -> {
          authenticationService.login(
              req.raw(),
              req.queryParams("username"),
              req.queryParams("password"),
              req.queryParams("prevurl"));

          // Redirect to the previous url
          res.redirect(req.queryParams("prevurl"), HttpStatus.SC_SEE_OTHER);
          return "";
        });
  }
}
