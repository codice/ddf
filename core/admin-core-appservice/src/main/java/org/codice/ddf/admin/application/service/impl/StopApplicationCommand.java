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
package org.codice.ddf.admin.application.service.impl;

import java.io.PrintStream;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.osgi.framework.ServiceReference;

/**
 * Utilizes the OSGi Command Shell in Karaf and stops a specific application.
 * 
 */
@Command(scope = "app", name = "stop", description = "Stops an application with the given name.")
public class StopApplicationCommand extends OsgiCommandSupport {

    @Argument(index = 0, name = "appName", description = "Name of the application to stop.", required = true, multiValued = false)
    String appName;

    @Override
    protected Object doExecute() throws ApplicationServiceException {

        PrintStream console = System.out;

        ServiceReference<ApplicationService> ref = getBundleContext().getServiceReference(
                ApplicationService.class);

        if (ref == null) {
            console.println("Application Status service is unavailable.");
            return null;
        }
        try {
            ApplicationService appService = getBundleContext().getService(ref);
            if (appService == null) {
                console.println("Application Status service is unavailable.");
                return null;
            }

            appService.stopApplication(appName);

        } finally {
            getBundleContext().ungetService(ref);
        }
        return null;
    }

}
