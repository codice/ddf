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
import java.util.Set;

import org.apache.felix.gogo.commands.Command;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationServiceException;

/**
 * Utilizes the OSGi Command Shell in Karaf and lists all available
 * applications.
 * 
 */
@Command(scope = "app", name = "tree", description = "Creates a hierarchy tree of all of the applications.")
public class TreeApplicationCommand extends AbstractApplicationCommand {

    @Override
    protected void applicationCommand() throws ApplicationServiceException {

        // node for the application tree
        Set<ApplicationNode> rootApplications = applicationService.getApplicationTree();
        for (ApplicationNode curRoot : rootApplications) {
            printNode(curRoot, "");
        }

        return;
    }

    private void printNode(ApplicationNode appNode, String appender) {
        PrintStream console = System.out;
        String appendStr = appender;

        console.println(appendStr + "+- " + appNode.getApplication().getName());
        appendStr += "|   ";
        for (ApplicationNode curChild : appNode.getChildren()) {
            printNode(curChild, appendStr);
        }

    }

}
