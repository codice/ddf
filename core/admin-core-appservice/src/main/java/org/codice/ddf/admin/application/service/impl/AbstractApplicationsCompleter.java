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
import java.util.List;

import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.console.Completer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.codice.ddf.admin.application.service.ApplicationService;

/**
 * <p>
 * Abstract Completer - base class for all Application completer implementations.
 * </p>
 */
public abstract class AbstractApplicationsCompleter implements Completer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApplicationsCompleter.class);

    protected PrintStream console = System.out;

    protected ApplicationService getApplicationService() {
        ApplicationService applicationService = null;
        ServiceReference<ApplicationService> appServiceRef = null;
        BundleContext bundleContext = null;

        try {
            bundleContext = FrameworkUtil.getBundle(AllApplicationsCompleter.class).getBundleContext();
            appServiceRef = bundleContext.getServiceReference(ApplicationService.class);
            if (appServiceRef != null) {
                applicationService = bundleContext.getService(appServiceRef);
            } else {
                console.println("Could not obtain reference to Application Service. Cannot perform operation.");
            }
        } finally {
            if (appServiceRef != null) {
                try {
                    bundleContext.ungetService(appServiceRef);
                } catch (IllegalStateException ise) {
                    LOGGER.debug("Bundle Context was closed, service reference already removed.");
                }
            }
        }
        return applicationService;
    }
}
