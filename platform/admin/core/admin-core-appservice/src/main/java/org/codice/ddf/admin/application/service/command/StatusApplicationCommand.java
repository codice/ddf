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

import org.apache.karaf.features.Feature;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.command.completers.AllApplicationsCompleter;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.Bundle;

/** Utilizes the OSGi Command Shell in Karaf and shows the status of an application. */
@Command(
  scope = "app",
  name = "status",
  description =
      "Shows status of an application.\n\tGives information on the current state, features within the application, what required features are not started and what required bundles are not started."
)
@Service
public class StatusApplicationCommand extends AbstractApplicationCommand {

  @Argument(
    index = 0,
    name = "appName",
    description = "Name of the application to get status on.",
    required = true,
    multiValued = false
  )
  @Completion(AllApplicationsCompleter.class)
  String appName;

  @Override
  protected void doExecute(ApplicationService applicationService)
      throws ApplicationServiceException {

    Application application = applicationService.getApplication(appName);

    if (application == null) {
      console.println("No application found with name " + appName);
      return;
    }

    ApplicationStatus appStatus = applicationService.getApplicationStatus(application);
    console.println(application.getName());
    console.println("\nCurrent State is: " + appStatus.getState());

    console.println("\nFeatures Located within this Application:");
    for (Feature curFeature : application.getFeatures()) {
      console.println("\t" + curFeature.getName());
    }

    console.println("\nRequired Features Not Started");
    if (appStatus.getErrorFeatures().isEmpty()) {
      console.print(Ansi.ansi().fg(Ansi.Color.GREEN).toString());
      console.println("\tNONE");
      console.print(Ansi.ansi().reset().toString());
    } else {
      for (Feature curFeature : appStatus.getErrorFeatures()) {
        console.print(Ansi.ansi().fg(Ansi.Color.RED).toString());
        console.println("\t" + curFeature.getName());
        console.print(Ansi.ansi().reset().toString());
      }
    }

    console.println("\nRequired Bundles Not Started");
    if (appStatus.getErrorBundles().isEmpty()) {
      console.print(Ansi.ansi().fg(Ansi.Color.GREEN).toString());
      console.println("\tNONE");
      console.print(Ansi.ansi().reset().toString());
    } else {
      for (Bundle curBundle : appStatus.getErrorBundles()) {
        console.print(Ansi.ansi().fg(Ansi.Color.RED).toString());
        console.println("\t[" + curBundle.getBundleId() + "]\t" + curBundle.getSymbolicName());
        console.print(Ansi.ansi().reset().toString());
      }
    }
  }
}
