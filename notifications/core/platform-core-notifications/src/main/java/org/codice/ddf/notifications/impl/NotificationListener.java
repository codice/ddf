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
package org.codice.ddf.notifications.impl;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.notifications.Notification;
import org.codice.ddf.notifications.NotificationStore;
import org.codice.ddf.notifications.PersistentNotification;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationListener implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListener.class);
    
    private NotificationStore notificationStore;
    
    
    public NotificationListener(NotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }
    
    @Override
    public void handleEvent(Event event) throws IllegalArgumentException {
        LOGGER.debug("Received notification on topic {}", event.getTopic());

        String application = (String) event.getProperty(Notification.NOTIFICATION_KEY_APPLICATION);
        String message = (String) event.getProperty(Notification.NOTIFICATION_KEY_MESSAGE);
        String timestamp = (String) event.getProperty(Notification.NOTIFICATION_KEY_TIMESTAMP);
        String title = (String) event.getProperty(Notification.NOTIFICATION_KEY_TITLE);
        String userId = (String) event.getProperty(Notification.NOTIFICATION_KEY_USER_ID);
        if (StringUtils.isBlank(userId)) {
            throw new IllegalArgumentException("Event \"" + Notification.NOTIFICATION_KEY_USER_ID
                    + "\" property is null or empty");
        }
        
        //TODO: Dp we need to get extra properties out of event for Notification, i.e., STATUS and BYTES?
        
        //Notification notification = new Notification(application, title, message, timestamp, userId);
        PersistentNotification notification = new PersistentNotification(application, title, message, timestamp, userId);
        notificationStore.putNotification(notification);
    }
}
