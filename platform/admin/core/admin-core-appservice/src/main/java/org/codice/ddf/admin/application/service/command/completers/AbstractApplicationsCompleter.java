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
package org.codice.ddf.admin.application.service.command.completers;

import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Abstract Completer - base class for all Application completer implementations.
 * </p>
 */
public abstract class AbstractApplicationsCompleter implements Completer {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AbstractApplicationsCompleter.class);

    @Reference
    protected ApplicationService applicationService;

    /**
     * @param session     the beginning string typed by the user
     * @param commandLine the position of the cursor
     * @param candidates  the list of completions proposed to the user
     */
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        if (applicationService != null) {
            Set<Application> applications = applicationService.getApplications();
            for (Application currentApp : applications) {
                addAppNames(currentApp, delegate);
            }
        } else {
            LOGGER.info(
                    "No application service - cannot complete. Check application service bundles.");
        }
        return delegate.complete(session, commandLine, candidates);
    }

    protected void setApplicationService(ApplicationService appSvc) {
        this.applicationService = appSvc;
    }

    protected abstract void addAppNames(Application currentApp, StringsCompleter delegate);

}
