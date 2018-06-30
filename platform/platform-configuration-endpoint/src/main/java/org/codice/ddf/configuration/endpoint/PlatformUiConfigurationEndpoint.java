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
package org.codice.ddf.configuration.endpoint;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.codice.ddf.configuration.service.PlatformUiConfigurationService;

@Path("/")
public class PlatformUiConfigurationEndpoint {

  private PlatformUiConfigurationService platformUiConfigurationService;

  public PlatformUiConfigurationEndpoint(PlatformUiConfigurationService configurationService) {
    platformUiConfigurationService = configurationService;
  }

  @GET
  @Path("/config/ui")
  @Produces("application/json")
  public String getConfig() {
    return platformUiConfigurationService.getConfigAsJsonString();
  }
}
