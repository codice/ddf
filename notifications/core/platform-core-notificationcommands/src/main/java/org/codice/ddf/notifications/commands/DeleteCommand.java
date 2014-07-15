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
package org.codice.ddf.notifications.commands;

import java.io.PrintStream;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.notifications.Notification;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.fusesource.jansi.Ansi;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Command(scope = "notifications", name = "delete", description = "Allows users to delete notifications for a specified user.")
public class DeleteCommand extends OsgiCommandSupport {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteCommand.class);
    
    public static final String SERVICE_PID = "org.codice.ddf.persistence.PersistentStore";
    
    static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();

    static final String CYAN_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.CYAN).toString();
 
    @Option(name = "User ID", required = false, aliases = {"-u"}, multiValued = false, description = "The user to delete notifications for.")
    String userId = null;

    @Option(name = "ID", required = false, aliases = {"-id"}, multiValued = false, description = "The id to delete notification for.")
    String id = null;

    @Override
    protected Object doExecute() throws Exception {

        PrintStream console = System.out;
        
        // Get Notification service
        @SuppressWarnings("rawtypes")
        ServiceReference[] serviceReferences = bundleContext.getServiceReferences(SERVICE_PID,
                null);

        int numDeleted = 0;
        
        if (serviceReferences == null || serviceReferences.length != 1) {
            LOGGER.debug("Found no service references for " + SERVICE_PID);
        } else {
            LOGGER.debug("Found " + serviceReferences.length + " service references for "
                    + SERVICE_PID);
            
            PersistentStore persistentStore = (PersistentStore) bundleContext.getService(serviceReferences[0]);
            if (persistentStore != null) {               
                    if (StringUtils.isNotBlank(id)) {
                        try {
                            numDeleted = persistentStore.delete(PersistentStore.NOTIFICATION_TYPE, 
                                    Notification.NOTIFICATION_KEY_ID + " = '" + id + "'");
                        } catch (PersistenceException e) {
                            LOGGER.info("PersistenceException during deletion of notifications for ID {}", id, e);
                        }
                    } else if (StringUtils.isNotBlank(userId)) {
                        try {
                            numDeleted = persistentStore.delete(PersistentStore.NOTIFICATION_TYPE, 
                                    Notification.NOTIFICATION_KEY_USER_ID + " = '" + userId + "'");
                        } catch (PersistenceException e) {
                            LOGGER.info("PersistenceException during deletion of notifications for user {}", userId, e);
                        }
                    }          
            } else {
                LOGGER.debug("Unable to lookup PersistentStore service");
            }
        }
        
        console.println(CYAN_CONSOLE_COLOR + "Deleted " + numDeleted + " notifications" + DEFAULT_CONSOLE_COLOR);
        
        return null;
    }
    
}
