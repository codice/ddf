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

import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Completer that returns active applications.
 * </p>
 */
public class ActiveApplicationsCompleter extends AbstractApplicationsCompleter
        implements Completer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActiveApplicationsCompleter.class);

    public ActiveApplicationsCompleter(ApplicationService applicationService) {
        super(applicationService);
    }

    /**
     * @param buffer the beginning string typed by the user
     * @param cursor the position of the cursor
     * @param candidates the list of completions proposed to the user
     */
    public int complete(String buffer, int cursor, List candidates) {

        StringsCompleter delegate = new StringsCompleter();
        if (applicationService != null) {
            Set<Application> applications = applicationService.getApplications();
            for (Application curApp : applications) {
                if (applicationService.getApplicationStatus(curApp)
                        .getState()
                        .equals(ApplicationState.ACTIVE)) {
                    delegate.getStrings()
                            .add(curApp.getName());
                }
            }
        } else {
            LOGGER.info("No application service - cannot complete");
        }
        return delegate.complete(buffer, cursor, candidates);
    }

}
