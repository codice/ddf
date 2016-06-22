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
 **/
package org.codice.ddf.ui.searchui.query.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.notifications.Notification;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.bayeux.Message;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code NotificationController} handles the processing and routing of notifications.
 */
@Service
public class NotificationController extends AbstractEventController {
    // CometD requires prepending the topic name with a '/' character, whereas
    // the OSGi Event Admin doesn't allow it.
    protected static final String NOTIFICATION_TOPIC_DOWNLOADS_COMETD =
            "/" + Notification.NOTIFICATION_TOPIC_DOWNLOADS;

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationController.class);

    public NotificationController(PersistentStore persistentStore, BundleContext bundleContext,
            EventAdmin eventAdmin) {
        super(persistentStore, bundleContext, eventAdmin);

    }

    /**
     * Implementation of {@link org.osgi.service.event.EventHandler#handleEvent(Event)} that receives notifications
     * published on the {@link #NOTIFICATIONS_TOPIC_NAME} topic from the OSGi eventing framework and
     * forwards them to their intended recipients.
     *
     * @throws IllegalArgumentException when any of the following required properties are either missing from the Event
     *                                  or contain empty values:
     *                                  <p>
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
                || event.getProperty(Notification.NOTIFICATION_KEY_APPLICATION)
                .toString()
                .isEmpty()) {
            throw new IllegalArgumentException(
                    "Event \"" + Notification.NOTIFICATION_KEY_APPLICATION
                            + "\" property is null or empty");
        }

        if (null == event.getProperty(Notification.NOTIFICATION_KEY_MESSAGE) || event.getProperty(
                Notification.NOTIFICATION_KEY_MESSAGE)
                .toString()
                .isEmpty()) {
            throw new IllegalArgumentException("Event \"" + Notification.NOTIFICATION_KEY_MESSAGE
                    + "\" property is null or empty");
        }

        if (null == event.getProperty(Notification.NOTIFICATION_KEY_TIMESTAMP)) {
            throw new IllegalArgumentException(
                    "Event \"" + Notification.NOTIFICATION_KEY_TIMESTAMP + "\" property is null");
        }

        if (null == event.getProperty(Notification.NOTIFICATION_KEY_TITLE) || event.getProperty(
                Notification.NOTIFICATION_KEY_TITLE)
                .toString()
                .isEmpty()) {
            throw new IllegalArgumentException("Event \"" + Notification.NOTIFICATION_KEY_TITLE
                    + "\" property is null or empty");
        }

        String sessionId = (String) event.getProperty(Notification.NOTIFICATION_KEY_SESSION_ID);
        String userId = (String) event.getProperty(Notification.NOTIFICATION_KEY_USER_ID);

        if (StringUtils.isBlank(userId) && StringUtils.isBlank(sessionId)) {
            throw new IllegalArgumentException("No user information was provided in the event object. userId and sessionId properties were null");
        }

        ServerSession recipient = null;
        LOGGER.debug("Getting ServerSession for userId/sessionId {}", userId);
        recipient = getSessionById(userId, sessionId);

        if (null != recipient) {
            Map<String, Object> propMap = new HashMap<>();

            for (String key : event.getPropertyNames()) {
                if (!EventConstants.EVENT_TOPIC.equals(key)
                        && !Notification.NOTIFICATION_KEY_USER_ID.equals(key)) {
                    propMap.put(key, event.getProperty(key));
                }
            }

            recipient.deliver(controllerServerSession,
                    NOTIFICATION_TOPIC_DOWNLOADS_COMETD,
                    propMap);
        } else {
            LOGGER.debug("Session with ID \"{}\" is not connected to the server. "
                    + "Ignnoring notification", sessionId);
        }
    }

    @Override
    public String getControllerRootTopic() {
        return Notification.NOTIFICATION_TOPIC_ROOT + "/*";
    }

    @Listener('/' + Notification.NOTIFICATION_TOPIC_ROOT)
    public void getPersistedNotifications(final ServerSession remote, Message message) {
        Subject subject = null;
        try {
            subject = SecurityUtils.getSubject();
        } catch (Exception e) {
            LOGGER.debug("Couldn't grab user subject from Shiro.", e);
        }

        String userId = getUserId(remote, subject);

        if (null == userId) {
            throw new IllegalArgumentException("User ID is null");
        }

        Map<String, Object> data = message.getDataAsMap();
        if (MapUtils.isEmpty(data)) {
            List<Map<String, Object>> notifications = getNotificationsForUser(userId);

            if (CollectionUtils.isNotEmpty(notifications)) {
                queuePersistedMessages(remote,
                        notifications,
                        "/" + Notification.NOTIFICATION_TOPIC_BROADCAST);
            }
        } else {
            String id = UUID.randomUUID()
                    .toString()
                    .replaceAll("-", "");
            String sessionId = remote.getId();
            Notification notification = new Notification(id,
                    sessionId,
                    (String) data.get(Notification.NOTIFICATION_KEY_APPLICATION),
                    (String) data.get(Notification.NOTIFICATION_KEY_TITLE),
                    (String) data.get(Notification.NOTIFICATION_KEY_MESSAGE),
                    (Long) data.get(Notification.NOTIFICATION_KEY_TIMESTAMP),
                    userId);
            Event event = new Event(Notification.NOTIFICATION_TOPIC_PUBLISH, notification);
            eventAdmin.postEvent(event);
        }
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

        Map<String, Object> dataAsMap = serverMessage.getDataAsMap();
        if (dataAsMap != null) {
            Object[] notifications = (Object[]) dataAsMap.get("data");

            for (Object notificationObject : notifications) {
                Map notification = (Map) notificationObject;
                String id = (String) notification.get("id");
                String action = (String) notification.get("action");

                if (action != null) {
                    if ("remove".equals(action)) {
                        //You can have a blank id for guest
                        if (id != null) {
                            try {
                                this.persistentStore.delete(PersistentStore.NOTIFICATION_TYPE,
                                        "id = '" + id + "'");
                            } catch (PersistenceException e) {
                                throw new IllegalArgumentException(
                                        "Unable to delete notification with id = " + id);
                            }
                        } else {
                            throw new IllegalArgumentException("Message id is null");
                        }
                    }
                } else {
                    throw new IllegalArgumentException("Message action is null.");
                }
            }
        } else {
            throw new IllegalArgumentException("Server Message is null.");
        }
    }

    public List<Map<String, Object>> getNotificationsForUser(String userId) {
        List<Map<String, Object>> notifications = new ArrayList<>();
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            results = persistentStore.get(PersistentStore.NOTIFICATION_TYPE,
                    Notification.NOTIFICATION_KEY_USER_ID + " = '" + userId + "'");
        } catch (PersistenceException e) {
            LOGGER.debug("PersistenceException trying to get notifications for user {}", userId, e);
        }

        for (Map<String, Object> result : results) {
            Map<String, Object> sanitizedResult = PersistentItem.stripSuffixes(result);
            notifications.add(sanitizedResult);
        }

        return notifications;
    }
}
