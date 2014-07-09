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

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.notifications.Notification;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "notifications", name = "send", description = "Send notification(s).")
public class SendCommand extends OsgiCommandSupport {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);
    
    public static final String SERVICE_PID = "org.osgi.service.event.EventAdmin";
    
    private static final int DEFAULT_NUMBER_OF_NOTIFICATIONS = 1;
    
    private static final String DEFAULT_NOTIFICATION_MESSAGE = "Default notification message";
    
    private static final String DEFAULT_NOTIFICATION_APPLICATION = "console-send-cmd";
    
    private static final String DEFAULT_NOTIFICATION_TITLE = "Default Title";
    
    private static final int DEFAULT_WAIT_TIME_BETWEEN_NOTIFICATIONS = 0;

    
    @Argument(name = "User ID", description = "User ID to send notifications to. ", 
            index = 0, multiValued = false, required = true)
    String userId = null;
    
    @Argument(name = "NUMBER_OF_ITEMS", description = "Number of notifications to send.", index = 1, multiValued = false, required = false)
    int numberOfNotifications = DEFAULT_NUMBER_OF_NOTIFICATIONS;
    
    @Option(name = "Application", required = false, aliases = {"-a"}, multiValued = false, description = "The name of application that sent the notification.")
    String application = DEFAULT_NOTIFICATION_APPLICATION;
    
    @Option(name = "Title", required = false, aliases = {"-t"}, multiValued = false, description = "The title of the notification.")
    String title = DEFAULT_NOTIFICATION_TITLE;
    
    @Option(name = "Message", required = false, aliases = {"-m"}, multiValued = false, description = "The detailed message to be sent in the notification.")
    String message = DEFAULT_NOTIFICATION_MESSAGE;
    
    @Option(name = "Wait Time", required = false, aliases = {"-w"}, multiValued = false, description = "FOR TESTING ONLY: The amount of seconds to wait between sending each notification.")
    int waitTime = DEFAULT_WAIT_TIME_BETWEEN_NOTIFICATIONS;

    
    @Override
    protected Object doExecute() throws Exception {
        
        for (int i=0; i < numberOfNotifications; i++) {
            sendNotification();
            if (waitTime > 0) {
                try {
                    Thread.sleep(waitTime * 1000);
                } catch (InterruptedException e) {
                    
                }
            }
        }

        return null;
    }

    private void sendNotification() throws Exception {
        Long sysTimeMillis = System.currentTimeMillis();
        Notification notification = new Notification(application,
                title,
                message,
                sysTimeMillis,
                userId);

        notification.put("status", "Started");
        notification.put("bytes", "12345");

        Event event = new Event(Notification.NOTIFICATION_TOPIC_DOWNLOADS,
                notification);

        // Get OSGi Event Admin service
        EventAdmin eventAdmin = null;
        @SuppressWarnings("rawtypes")
        ServiceReference[] serviceReferences = bundleContext.getServiceReferences(SERVICE_PID,
                null);

        if (serviceReferences == null || serviceReferences.length != 1) {
            LOGGER.debug("Found no service references for " + SERVICE_PID);
        } else {
            LOGGER.debug("Found " + serviceReferences.length + " service references for "
                    + SERVICE_PID);
            
            eventAdmin = (EventAdmin) bundleContext.getService(serviceReferences[0]);
            if (eventAdmin != null) {
                eventAdmin.postEvent(event);   
            }
        }
    }
}
