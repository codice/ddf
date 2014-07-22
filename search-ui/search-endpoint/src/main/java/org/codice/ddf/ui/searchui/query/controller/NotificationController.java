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
package org.codice.ddf.ui.searchui.query.controller;


import net.minidev.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.notifications.Notification;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * The {@code NotificationController} handles the processing and routing of notifications.
 */
@Service
public class NotificationController extends AbstractEventController {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationController.class);

    // CometD requires prepending the topic name with a '/' character, whereas
    // the OSGi Event Admin doesn't allow it.
    protected static final String NOTIFICATION_TOPIC_DOWNLOADS_COMETD = "/"
            + Notification.NOTIFICATION_TOPIC_DOWNLOADS;

    public NotificationController(PersistentStore persistentStore, BundleContext bundleContext, EventAdmin eventAdmin) {
        super(persistentStore, bundleContext, eventAdmin);

    }

    /**
     * Implementation of {@link EventHandler#handleEvent(Event)} that receives notifications
     * published on the {@link #NOTIFICATIONS_TOPIC_NAME} topic from the OSGi eventing framework and
     * forwards them to their intended recipients.
     *
     * @throws IllegalArgumentException when any of the following required properties are either missing from the Event
     *                                  or contain empty values:
     *                                  <p/>
     *                                  <ul>
     *                                  <li>{@link Notification#NOTIFICATION_KEY_APPLICATION}</li>
     *                                  <li>{@link Notification#NOTIFICATION_KEY_MESSAGE}</li>
     *                                  <li>{@link Notification#NOTIFICATION_KEY_TIMESTAMP}</li
     *                                  <li>{@link Notification#NOTIFICATION_KEY_TITLE}</li>
     *                                  <li>{@link Notification#NOTIFICATION_KEY_USER_ID}</li>
     *                                  </ul>
     */
    @Override
    public void handleEvent(Event event) throws IllegalArgumentException {

        if (null == event.getProperty(Notification.NOTIFICATION_KEY_APPLICATION)
                || event.getProperty(Notification.NOTIFICATION_KEY_APPLICATION).toString()
                .isEmpty()) {
            throw new IllegalArgumentException("Event \""
                    + Notification.NOTIFICATION_KEY_APPLICATION + "\" property is null or empty");
        }

        if (null == event.getProperty(Notification.NOTIFICATION_KEY_MESSAGE)
                || event.getProperty(Notification.NOTIFICATION_KEY_MESSAGE).toString().isEmpty()) {
            throw new IllegalArgumentException("Event \"" + Notification.NOTIFICATION_KEY_MESSAGE
                    + "\" property is null or empty");
        }

        if (null == event.getProperty(Notification.NOTIFICATION_KEY_TIMESTAMP)) {
            throw new IllegalArgumentException("Event \"" + Notification.NOTIFICATION_KEY_TIMESTAMP
                    + "\" property is null");
        }

        if (null == event.getProperty(Notification.NOTIFICATION_KEY_TITLE)
                || event.getProperty(Notification.NOTIFICATION_KEY_TITLE).toString().isEmpty()) {
            throw new IllegalArgumentException("Event \"" + Notification.NOTIFICATION_KEY_TITLE
                    + "\" property is null or empty");
        }

        String sessionId = (String) event.getProperty(Notification.NOTIFICATION_KEY_SESSION_ID);
        if (StringUtils.isBlank(sessionId)) {
            throw new IllegalArgumentException("Event \"" + Notification.NOTIFICATION_KEY_SESSION_ID
                    + "\" property is null or empty");
        }

        String userId = (String) event.getProperty(Notification.NOTIFICATION_KEY_USER_ID);
        // Blank user ID is allowed as this indicates the anonymous user
        if (null == userId) {
            throw new IllegalArgumentException("Event \"" + Notification.NOTIFICATION_KEY_USER_ID
                    + "\" property is null or empty");
        }

        ServerSession recipient = null;
        if (StringUtils.isNotBlank(userId)) {
            LOGGER.debug("Getting ServerSession for userId {}", userId);
            recipient = getSessionByUserId(userId);
        } else {
            LOGGER.debug("Getting ServerSession for sessionId {}", sessionId);
            recipient = getSessionByUserId(sessionId);
        }

        if (null != recipient) {
            JSONObject jsonPropMap = new JSONObject();

            for (String key : event.getPropertyNames()) {
                jsonPropMap.put(key, event.getProperty(key));
            }

            LOGGER.debug("Sending the following property map \"{}\": ", jsonPropMap.toJSONString());

            recipient.deliver(controllerServerSession, NOTIFICATION_TOPIC_DOWNLOADS_COMETD,
                    jsonPropMap.toJSONString(), null);

        } else {
            LOGGER.debug("Session with ID \"{}\" is not connected to the server. "
                    + "Ignnoring notification", sessionId);
        }
    }

    @Override
    public String getControllerRootTopic() {
        return Notification.NOTIFICATION_TOPIC_ROOT + "/*";
    }

    @Listener("/notification/action")
    public void deletePersistentNotification(ServerSession serverSession,
            ServerMessage serverMessage) {
        LOGGER.debug("\nServerSession: {}\nServerMessage: {}", serverSession, serverMessage);

        if (null == serverSession) {
            throw new IllegalArgumentException("ServerSession is null");
        }
        if (null == serverMessage) {
            throw new IllegalArgumentException("ServerMessage is null");
        }

        Subject subject = null;
        try {
            subject = SecurityUtils.getSubject();
        } catch (Exception e) {
            LOGGER.debug("Couldn't grab user subject from Shiro.", e);
        }

        String userId = getUserId(serverSession, subject);

        Object[] notifications = (Object[]) serverMessage.getDataAsMap().get("data");

        for (Object notificationObject : notifications) {
            Map notification = (Map) notificationObject;
            String id = (String) notification.get("id");
            String action = (String) notification.get("action");

            if (action != null) {
                if ("remove".equals(action)) {
                    //You can have a blank id for anonymous
                    if (id != null) {
                        try {
                            this.persistentStore.delete(PersistentStore.NOTIFICATION_TYPE,
                                    "id = '" + id + "'");
                        } catch (PersistenceException e) {
                            throw new IllegalArgumentException("Unable to delete notification with id = " + id);
                        }
                    } else {
                        throw new IllegalArgumentException("Message id is null");
                    }
                }
            } else {
                throw new IllegalArgumentException("Message action is null.");
            }
        }
    }
}
