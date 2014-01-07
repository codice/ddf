/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.platform.status.impl;

import java.io.PrintStream;
import java.util.Set;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.features.Feature;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.platform.status.Application;
import org.codice.ddf.platform.status.ApplicationService;
import org.codice.ddf.platform.status.ApplicationStatus;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * Utilizes the OSGi Command Shell in Karaf and shows the status of an
 * application.
 *
 */
@Command(scope = "app", name = "status", description = "Shows status of an application.\n\tGives information on the current state, features within the application, what required features are not started and what required bundles are not started.")
public class StatusApplicationCommand extends OsgiCommandSupport {

    @Argument(index = 0, name = "appName", description = "Name of the application to get status on.", required = true, multiValued = false)
    String appName;

    @Override
    protected Object doExecute() throws Exception {

        PrintStream console = System.out;

        ServiceReference ref = getBundleContext().getServiceReference(
                ApplicationService.class.getName());
        if (ref == null) {
            console.println("ApplicationService service is unavailable.");
            return null;
        }
        try {
            ApplicationService appService = (ApplicationService) getBundleContext().getService(ref);
            if (appService == null) {
                console.println("ApplicationService service is unavailable.");
                return null;
            }

            Set<Application> applications = appService.getApplications();
            for (Application curApp : applications) {
                if (curApp.getName().equals(appName)) {
                    ApplicationStatus appStatus = appService.getApplicationStatus(curApp);
                    console.println(curApp.getName());
                    console.println("\nCurrent State is: " + appStatus.getState().toString());

                    console.println("\nFeatures Located within this Application:");
                    for (Feature curFeature : curApp.getFeatures()) {
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
                            console.println("\t" + "[" + curBundle.getBundleId() + "]\t"
                                    + curBundle.getSymbolicName());
                            console.print(Ansi.ansi().reset().toString());
                        }
                    }
                }
            }
        } finally {
            getBundleContext().ungetService(ref);
        }
        return null;
    }

}
