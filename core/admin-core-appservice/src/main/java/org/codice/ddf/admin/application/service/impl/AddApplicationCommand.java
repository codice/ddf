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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.admin.application.service.ApplicationServiceException;

/**
 * Utilizes the OSGi Command Shell in Karaf and starts a given application.
 * 
 */
@Command(scope = "app", name = "add", description = "Adds an application with the given uri.")
public class AddApplicationCommand extends AbstractApplicationCommand {

    @Argument(index = 0, name = "appName", description = "Name of the application to add.", required = true, multiValued = false)
    String appName;

    @Override
    protected void applicationCommand() throws ApplicationServiceException {

        try {
            applicationService.addApplication(new URI(appName));
        } catch (URISyntaxException use) {
            console.println(appName + " is not a valid URI.");
            return;
        }

        return;
    }

}
