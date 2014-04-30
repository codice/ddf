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

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Abstract application command. Retrieves an instance of the application
 * service for child classes to use.
 * 
 */
public abstract class AbstractApplicationCommand extends OsgiCommandSupport {

    protected ApplicationService applicationService;

    protected PrintStream console = System.out;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected Object doExecute() throws ApplicationServiceException {

        ServiceReference<ApplicationService> appServiceRef = null;

        try {
            appServiceRef = getBundleContext().getServiceReference(ApplicationService.class);
            applicationService = getBundleContext().getService(appServiceRef);
            applicationCommand();
        } catch (Exception e) {
            console.println("Could not obtain find Application Service due to error: "
                    + e.getMessage());
        } finally {
            if (appServiceRef != null) {
                try {
                    getBundleContext().ungetService(appServiceRef);
                } catch (IllegalStateException ise) {
                    logger.debug("Bundle Context was closed, service reference already removed.");
                }
            }
        }

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
