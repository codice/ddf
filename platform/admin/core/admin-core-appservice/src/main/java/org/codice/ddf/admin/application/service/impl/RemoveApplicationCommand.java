/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.application.service.impl;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.admin.application.service.ApplicationServiceException;

/**
 * Utilizes the OSGi Command Shell in Karaf and removes a given application from
 * the system.
 *
 */
@Command(scope = "app", name = "remove", description = "Removes an application with the given name.")
public class RemoveApplicationCommand extends AbstractApplicationCommand {

    @Argument(index = 0, name = "appName", description = "Name of the application to remove.", required = true, multiValued = false)
    String appName;

    @Override
    protected void applicationCommand() throws ApplicationServiceException {

        applicationService.removeApplication(appName);

        return;
    }

}
