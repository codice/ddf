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
package org.codice.ddf.admin.application.service.command;

import java.io.PrintStream;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract application command. Retrieves an instance of the application
 * service for child classes to use.
 */
public abstract class AbstractApplicationCommand implements Action {

    @Reference
    private ApplicationService applicationService;

    protected PrintStream console = System.out;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Object execute() throws Exception {

        if (applicationService == null) {
            throw new IllegalStateException("ApplicationService not found");
        }
        try {
            doExecute(applicationService);
        } catch (ApplicationServiceException ase) {
            console.println(
                    "Encountered error while trying to perform command. Check log for more details.");
            logger.error("Error while performing command.", ase);
        }
        return null;
    }

    protected void setApplicationService(ApplicationService appSvc) {
        this.applicationService = appSvc;
    }

    /**
     * Command code that operates with an application service. All output should
     * be sent to the console and nothing should be returned.
     *
     * @param applicationService
     * @throws ApplicationServiceException On any error from the application service.
     */
    abstract void doExecute(ApplicationService applicationService)
            throws ApplicationServiceException;
}
