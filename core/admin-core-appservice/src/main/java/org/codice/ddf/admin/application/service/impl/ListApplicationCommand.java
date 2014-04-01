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

import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.fusesource.jansi.Ansi;

/**
 * Utilizes the OSGi Command Shell in Karaf and lists all available
 * applications.
 * 
 */
@Command(scope = "app", name = "list", description = "Lists the applications that are in the system and gives their current state. \n\tThere are four possible states:\n\t\tACTIVE: no errors, started successfully\n\t\tFAILED: errors occurred while trying to start\n\t\tINACTIVE: nothing from the app is installed\n\t\tUNKNOWN: could not determine status.")
public class ListApplicationCommand extends AbstractApplicationCommand {

    private static final int STATUS_COLUMN_LENGTH;
    static {
        int size = 0;
        for (ApplicationState curState : ApplicationStatus.ApplicationState.values()) {
            if (curState.name().length() > size) {
                size = curState.name().length();
            }
        }
        STATUS_COLUMN_LENGTH = size;
    }

    @Override
    protected void applicationCommand() throws ApplicationServiceException {

        Set<Application> applications = applicationService.getApplications();
        console.printf("%s%10s%n", "State", "Name");
        for (Application curApp : applications) {
            ApplicationStatus appStatus = applicationService.getApplicationStatus(curApp);
            // only show applications that have features (gets rid of repo
            // aggregator 'apps')
            if (!curApp.getFeatures().isEmpty()) {
                console.print("[");
                switch (appStatus.getState()) {
                case ACTIVE:
                    console.print(Ansi.ansi().fg(Ansi.Color.GREEN).toString());
                    break;
                case FAILED:
                    console.print(Ansi.ansi().fg(Ansi.Color.RED).toString());
                    break;
                case INACTIVE:
                    // don't set a color
                    break;
                case UNKNOWN:
                    console.print(Ansi.ansi().fg(Ansi.Color.YELLOW).toString());
                    break;
                default:
                    break;
                }
                console.print(StringUtils.rightPad(appStatus.getState().toString(),
                        STATUS_COLUMN_LENGTH));
                console.print(Ansi.ansi().reset().toString());
                console.println("] " + curApp.getName());
            }
        }
        return;
    }

}
