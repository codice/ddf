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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.command.completers.NotActiveApplicationsCompleter;

/** Utilizes the OSGi Command Shell in Karaf and starts a given application. */
@Command(scope = "app", name = "start", description = "Starts an application with the given name.")
@Service
public class StartApplicationCommand extends AbstractApplicationCommand {

  @Argument(
    index = 0,
    name = "appName",
    description = "Name of the application to start.",
    required = true,
    multiValued = false
  )
  @Completion(NotActiveApplicationsCompleter.class)
  String appName;

  @Override
  protected void doExecute(ApplicationService applicationService)
      throws ApplicationServiceException {

    applicationService.startApplication(appName);

    return;
  }
}
