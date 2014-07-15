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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.notifications.Notification;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Command(scope = "notifications", name = "list", description = "Allows users to view notifications.")
public class ListCommand extends OsgiCommandSupport {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);
    
    public static final String SERVICE_PID = "org.codice.ddf.persistence.PersistentStore";
    
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat
            .forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
    
    private static final int MAX_LENGTH = 40;
    
    static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();
    
    static final String RED_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.RED).toString();

    static final String CYAN_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.CYAN).toString();
    
    private static final String ID = "ID ";
    
    private static final String USER_ID = "User ID ";
    
    private static final String TIMESTAMP = "Timestamp ";
    
    private static final String APPLICATION = "Application ";
    
    private static final String TITLE = "Title ";
    
    private static final String MESSAGE = "Message ";

    @Argument(name = "User ID", description = "User ID to search for notifications. "
            + "If an id is not provided, then all of the notifications for all users are displayed.", 
            index = 0, multiValued = false, required = false)
    String userId = null;
    
    @Option(name = "ECQL", required = false, aliases = {"-ecql"}, multiValued = false, description = "ECQL query to get notifications.")
    String ecql = null;
    
    @Override
    protected Object doExecute() throws Exception {

        PrintStream console = System.out;
        
        String formatString = "%1$-33s %2$-30s %3$-30s %4$-15s %5$-20s %6$-" + MAX_LENGTH + "s%n";

        List<Map<String, Object>> notifications = getNotifications(userId);
        if (notifications == null || notifications.size() == 0) {
            console.println(RED_CONSOLE_COLOR + "No notifications found" + DEFAULT_CONSOLE_COLOR);
        } else {
            console.println();
            console.print("Total notifications found: " + notifications.size());
            console.println();
            console.println();
            console.printf(formatString, "", "", "", "", "", "");
            console.print(CYAN_CONSOLE_COLOR);
            console.printf(formatString, ID, USER_ID, TIMESTAMP, APPLICATION, TITLE, MESSAGE);
            console.print(DEFAULT_CONSOLE_COLOR);

            for (Map<String, Object> notification : notifications) {
                Long timestamp = Long.valueOf((String) notification.get(Notification.NOTIFICATION_KEY_TIMESTAMP));
                String dateTime = new DateTime(new Date(timestamp)).toString(DATETIME_FORMATTER);
                String message = (String) notification.get(Notification.NOTIFICATION_KEY_MESSAGE);
                LOGGER.debug("id = {}, userId = {}, timestamp = {}, application = {},  title = {},  message = {}",
                        notification.get(Notification.NOTIFICATION_KEY_ID),
                        notification.get(Notification.NOTIFICATION_KEY_USER_ID),
                        dateTime, 
                        notification.get(Notification.NOTIFICATION_KEY_APPLICATION), 
                        notification.get(Notification.NOTIFICATION_KEY_TITLE),
                        message);
                
                console.printf(
                        formatString,
                        notification.get(Notification.NOTIFICATION_KEY_ID),
                        notification.get(Notification.NOTIFICATION_KEY_USER_ID), 
                        dateTime, 
                        notification.get(Notification.NOTIFICATION_KEY_APPLICATION), 
                        notification.get(Notification.NOTIFICATION_KEY_TITLE),
                        message.substring(0, Math.min(message.length(), MAX_LENGTH)));
            }
        }

        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getNotifications(String userId) throws InvalidSyntaxException {
        List<Map<String, Object>> notifications = new ArrayList<Map<String, Object>>();
        
        // Get Notification service
        @SuppressWarnings("rawtypes")
        ServiceReference[] serviceReferences = bundleContext.getServiceReferences(SERVICE_PID,
                null);

        if (serviceReferences == null || serviceReferences.length != 1) {
            LOGGER.debug("Found no service references for " + SERVICE_PID);
        } else {
            LOGGER.debug("Found " + serviceReferences.length + " service references for "
                    + SERVICE_PID);
            
            PersistentStore persistentStore = (PersistentStore) bundleContext.getService(serviceReferences[0]);
            if (persistentStore != null) {
                try {
                    List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
                    if (StringUtils.isNotBlank(ecql)) {
                        results = persistentStore.get(PersistentStore.NOTIFICATION_TYPE, ecql);
                    } else if (StringUtils.isNotBlank(userId)) {
                        results = persistentStore.get(PersistentStore.NOTIFICATION_TYPE, 
                                Notification.NOTIFICATION_KEY_USER_ID + " = '" + userId + "'");
                    } else {
                        results = persistentStore.get(PersistentStore.NOTIFICATION_TYPE);
                    }
                    
                    for (Map<String, Object> result : results) {
                        notifications.add(PersistentItem.stripSuffixes(result));
                    }
                } catch (PersistenceException e) {
                    LOGGER.info("PersistenceException during retrieval of notifications", e);
                }
            } else {
                LOGGER.debug("Unable to lookup PersistentStore service");
            }
        }
        
        return notifications;
    }
    
}
