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
package org.codice.ddf.catalog.ui.configuration;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.protocol.HTTP.CONTENT_TYPE;
import static spark.Spark.get;

import org.codice.ddf.configuration.service.PlatformUiConfigurationService;
import spark.servlet.SparkApplication;

public class PlatformUiConfigurationApplication implements SparkApplication {

  private PlatformUiConfigurationService platformUiConfigurationService;

  public PlatformUiConfigurationApplication(PlatformUiConfigurationService configurationService) {
    platformUiConfigurationService = configurationService;
  }

  @Override
  public void init() {
    get(
        "/platform/config/ui",
        APPLICATION_JSON,
        (req, res) -> {
          String response = platformUiConfigurationService.getConfigAsJsonString();
          res.status(200);
          res.header(CONTENT_TYPE, APPLICATION_JSON);
          return response;
        });
  }
}
