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
package org.codice.ddf.admin.application.service.command;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;

/** Utilizes the OSGi Command Shell in Karaf and adds a given application to the system. */
@Command(scope = "app", name = "add", description = "Adds an application with the given uri.")
@Service
public class AddApplicationCommand extends AbstractApplicationCommand {

  @Argument(
    index = 0,
    name = "appName",
    description = "URL of the application to add.",
    required = true,
    multiValued = false
  )
  String appName;

  @Override
  protected void doExecute(ApplicationService applicationService)
      throws ApplicationServiceException {

    try {
      applicationService.addApplication(new URI(appName));
    } catch (URISyntaxException use) {
      console.println(appName + " is not a valid URI.");
      return;
    }

    return;
  }
}
