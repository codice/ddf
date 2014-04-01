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
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;

/**
 * 
 * Abstract application command. Retrieves an instance of the application
 * service for child classes to use.
 * 
 */
public abstract class AbstractApplicationCommand extends OsgiCommandSupport {

    protected ApplicationService applicationService;

    protected PrintStream console = System.out;

    @Override
    protected Object doExecute() throws ApplicationServiceException {

        PrintStream console = System.out;
        List<ApplicationService> appServiceList = new ArrayList<ApplicationService>();

        try {
            appServiceList = getAllServices(ApplicationService.class, null);
        } catch (Exception e) {
            console.println("Could not obtain find Application Service due to error: "
                    + e.getMessage());
        }

        if (appServiceList.isEmpty()) {
            console.println("Application Service is unavailable.");
            return null;
        }

        applicationService = appServiceList.get(0);

        applicationCommand();

        return null;
    }

    /**
     * Command code that operates with an application service. All output should
     * be sent to the console and nothing should be returned.
     * 
     * @throws ApplicationServiceException
     *             On any error from the application service.
     */
    abstract void applicationCommand() throws ApplicationServiceException;

}
