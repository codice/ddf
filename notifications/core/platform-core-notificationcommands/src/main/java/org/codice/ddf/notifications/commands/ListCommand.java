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
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.notifications.store.NotificationStore;
import org.codice.ddf.notifications.store.PersistentNotification;
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
    
    public static final String SERVICE_PID = "org.codice.ddf.notifications.store.NotificationStore";
    
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormat
            .forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
    
    private static final int MAX_LENGTH = 40;
    
    static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();
    
    static final String RED_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.RED).toString();

    static final String CYAN_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.CYAN).toString();
    
    private static final String USER_ID = "User ID ";
    
    private static final String TIMESTAMP = "Timestamp ";
    
    private static final String APPLICATION = "Application ";
    
    private static final String TITLE = "Title ";
    
    private static final String MESSAGE = "Message ";

    @Argument(name = "User ID", description = "User ID to search for notifications. "
            + "If an id is not provided, then all of the notifications for all users are displayed.", 
            index = 0, multiValued = false, required = false)
    String userId = null;
    
    @Override
    protected Object doExecute() throws Exception {

        PrintStream console = System.out;
        
        String formatString = "%1$-30s %2$-30s %3$-15s %4$-20s %5$-" + MAX_LENGTH + "s%n";

        List<Map<String, String>> notifications = getNotifications(userId);
        if (notifications == null || notifications.size() == 0) {
            console.println(RED_CONSOLE_COLOR + "No notifications found" + DEFAULT_CONSOLE_COLOR);
        } else {
            console.println();
            console.print("Total notifications found: " + notifications.size());
            console.println();
            console.println();
            console.printf(formatString, "", "", "", "", "");
            console.print(CYAN_CONSOLE_COLOR);
            console.printf(formatString, USER_ID, TIMESTAMP, APPLICATION, TITLE, MESSAGE);
            console.print(DEFAULT_CONSOLE_COLOR);

            for (Map<String, String> notification : notifications) {
                Long timestamp = Long.valueOf(notification.get(PersistentNotification.NOTIFICATION_KEY_TIMESTAMP));
                String dateTime = new DateTime(new Date(timestamp)).toString(DATETIME_FORMATTER);
                LOGGER.debug("id = {}, userId = {}, timestamp = {}, application = {},  title = {},  message = {}",
                        notification.get(PersistentNotification.NOTIFICATION_KEY_UUID),
                        notification.get(PersistentNotification.NOTIFICATION_KEY_USER_ID), 
                        dateTime, 
                        notification.get(PersistentNotification.NOTIFICATION_KEY_APPLICATION), 
                        notification.get(PersistentNotification.NOTIFICATION_KEY_TITLE),
                        notification.get(PersistentNotification.NOTIFICATION_KEY_MESSAGE));
                
                console.printf(
                        formatString,
                        notification.get(PersistentNotification.NOTIFICATION_KEY_USER_ID), 
                        dateTime, 
                        notification.get(PersistentNotification.NOTIFICATION_KEY_APPLICATION), 
                        notification.get(PersistentNotification.NOTIFICATION_KEY_TITLE),
                        notification.get(PersistentNotification.NOTIFICATION_KEY_MESSAGE).substring(0, 
                                Math.min(notification.get(PersistentNotification.NOTIFICATION_KEY_MESSAGE).length(), MAX_LENGTH)));
            }
        }

        return null;
    }
    
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> getNotifications(String userId) throws InvalidSyntaxException {
        List<Map<String, String>> notifications = new ArrayList<Map<String, String>>();
        
        // Get Notification service
        @SuppressWarnings("rawtypes")
        ServiceReference[] serviceReferences = bundleContext.getServiceReferences(SERVICE_PID,
                null);

        if (serviceReferences == null || serviceReferences.length != 1) {
            LOGGER.debug("Found no service references for " + SERVICE_PID);
        } else {
            LOGGER.debug("Found " + serviceReferences.length + " service references for "
                    + SERVICE_PID);
            
            NotificationStore notificationStore = (NotificationStore) bundleContext.getService(serviceReferences[0]);
            if (notificationStore != null) {
                if (StringUtils.isNotBlank(userId)) {
                    notifications = notificationStore.getNotifications(userId);
                } else {
                    notifications = notificationStore.getNotifications();
                }
            } else {
                LOGGER.debug("Unable to lookup Notification Store");
            }
        }
        
        return notifications;
    }
    
}
