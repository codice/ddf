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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.minidev.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.notifications.Notification;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.assertion.SecurityAssertion;

/**
 * The {@code AbstractEventController} handles the processing and routing of events.
 */
@Service
public abstract class AbstractEventController implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventController.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public static final java.lang.String EVENT_TOPIC_CANCEL = "download/action/cancel";

    @Session
    protected ServerSession controllerServerSession;

    // Synchronize the map to protect against multiple clients triggering
    // multiple Map operations at the same time
    // Set the HashMap's initial capacity to allow for 30 users without
    // resizing/rehashing the map
    // ((30 users / .75 loadFactor) + 1) = 41 = initialCapacity.
    // TODO: Should the initial size be larger? How many clients are we
    // anticipating per DDF instance.
    protected Map<String, ServerSession> userSessionMap = Collections
            .synchronizedMap(new HashMap<String, ServerSession>(41));

    protected PersistentStore persistentStore;

    private EventAdmin eventAdmin;

    /**
     * Establishes {@code AbstractEventController} as a listener to events published by the OSGi
     * eventing framework on the event's root topic
     *
     * @param bundleContext
     */
    public AbstractEventController(PersistentStore persistentStore, BundleContext bundleContext,
            EventAdmin eventAdmin) {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        dictionary.put(EventConstants.EVENT_TOPIC, getControllerRootTopic());

        bundleContext.registerService(EventHandler.class.getName(), this, dictionary);

        this.persistentStore = persistentStore;
        this.eventAdmin = eventAdmin;
    }

    public List<Map<String, String>> getNotificationsForUser(String userId) {
        List<Map<String, String>> notifications = new ArrayList<Map<String, String>>();
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            results = persistentStore.get(PersistentStore.NOTIFICATION_TYPE,
                    Notification.NOTIFICATION_KEY_USER_ID + " = '" + userId + "'");
        } catch (PersistenceException e) {
            LOGGER.debug("PersistenceException trying to get notifications for user {}", userId, e);
        }
        for (Map<String, Object> result : results) {
            Map<String, Object> sanitizedResult = PersistentItem.stripSuffixes(result);
            Map<String, String> notification = new HashMap<String, String>();
            for (String name : sanitizedResult.keySet()) {
                notification.put(name, sanitizedResult.get(name).toString());
            }
            notifications.add(notification);
        }

        return notifications;
    }

    public List<Map<String, Object>> getActivitiesForUser(String userId) {
        List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            results = persistentStore.get(PersistentStore.ACTIVITY_TYPE,
                    ActivityEvent.USER_ID_KEY + " = '" + userId + "'");
        } catch (PersistenceException e) {
            LOGGER.debug("PersistenceException trying to get activities for user {}", userId, e);
        }
        for (Map<String, Object> result : results) {
            Map<String, Object> sanitizedResult = PersistentItem.stripSuffixes(result);
            Map<String, Object> activity = new HashMap<String, Object>();
            activity.put(ActivityEvent.OPERATIONS_KEY, new HashMap<String, String>());
            for (Map.Entry<String, Object> entry: sanitizedResult.entrySet()) {
                if (entry.getKey().contains(ActivityEvent.OPERATIONS_KEY + "_")) {
                    ((Map) activity.get(ActivityEvent.OPERATIONS_KEY)).put(
                            entry.getKey().substring((ActivityEvent.OPERATIONS_KEY + "_").length()),
                            entry.getValue().toString());
                } else {
                    activity.put(entry.getKey(), entry.getValue().toString());
                }
            }
            activities.add(activity);
        }

        return activities;
    }

    /**
     * Obtains the {@link ServerSession} associated with a given user id.
     *
     * @param userId The id of the user associated with the {@code ServerSession} to be retrieved.
     * @return The {@code ServerSession} associated with the received userId or null if the user
     * does not have an established {@code ServerSession}
     */
    public ServerSession getSessionByUserId(String userId) {
        return userSessionMap.get(userId);
    }

    /**
     * Listens to the /meta/disconnect {@link org.cometd.bayeux.Channel} for clients disconnecting
     * and deregisters the user. This should be invoked in order to remove
     * {@code AbstractEventController} references to invalid {@link ServerSession}s.
     *
     * @param serverSession The {@code ServerSession} object associated with the client that is disconnecting
     * @param serverMessage The {@link ServerMessage} that was sent from the client on the /meta/disconnect
     *                      Channel
     */
    @Listener("/meta/disconnect")
    public void deregisterUserSession(ServerSession serverSession, ServerMessage serverMessage) {
        LOGGER.debug("\nServerSession: {}\nServerMessage: {}", serverSession, serverMessage);

        if (null == serverSession) {
            throw new IllegalArgumentException("ServerSession is null");
        }

        Subject subject = null;
        try {
            subject = SecurityUtils.getSubject();
        } catch (Exception e) {
            LOGGER.debug("Couldn't grab user subject from Shiro.", e);
        }

        String userId = getUserId(serverSession, subject);

        if (null == userId) {
            throw new IllegalArgumentException("User ID is null");
        }

        if (null != getSessionByUserId(userId)) {
            userSessionMap.remove(userId);
        } else {
            LOGGER.debug("userSessionMap does not contain a user with the id \"{}\"", userId);
        }
    }

    @Listener("/service/action")
    public void actionSession(final ServerSession serverSession, ServerMessage serverMessage) {
        LOGGER.debug("\nServerSession: {}\nServerMessage: {}", serverSession, serverMessage);

        if (null == serverSession) {
            throw new IllegalArgumentException("ServerSession is null");
        }

        if (null == serverMessage) {
            throw new IllegalArgumentException("ServerMessage is null");
        }

        Map<String, Object> actionMessage = serverMessage.getDataAsMap();
        String actionName = (String) actionMessage.get("action");
        String downloadIdentifier = (String) actionMessage.get("id");
        LOGGER.debug("\nAction: {}", actionName);

        if (StringUtils.equalsIgnoreCase(actionName, "cancel")) {
            Subject subject = null;
            try {
                subject = SecurityUtils.getSubject();
            } catch (Exception e) {
                LOGGER.debug("Couldn't grab user subject from Shiro.", e);
            }

            String userId = getUserId(serverSession, subject);

            if (null == userId) {
                throw new IllegalArgumentException("User ID is null");
            }
            if (null == downloadIdentifier) {
                throw new IllegalArgumentException("Metadata ID is null");
            }

            String downloadId = userId + downloadIdentifier;

            JSONObject jsonPropMap = new JSONObject();
            jsonPropMap.put(ActivityEvent.DOWNLOAD_ID_KEY, downloadId);

            Event event = new Event(ActivityEvent.EVENT_TOPIC_DOWNLOAD_CANCEL, jsonPropMap);
            eventAdmin.postEvent(event);

        }

    }

    /**
     * Called by {@link  ddf.catalog.event.retrievestatus.DownloadStatusInfoImpl.cancelDownload} to fire a
     * cancel event.
     *
     * @param userId             The Id assigned to the user who is downloading.
     * @param downloadIdentifier The randomly generated downloadId string assigned to the download at its start.
     */
    public void adminCancelDownload(String userId, String downloadIdentifier) {
        String downloadId = userId + downloadIdentifier;

        JSONObject jsonPropMap = new JSONObject();
        jsonPropMap.put(ActivityEvent.DOWNLOAD_ID_KEY, downloadId);

        Event event = new Event(ActivityEvent.EVENT_TOPIC_DOWNLOAD_CANCEL, jsonPropMap);
        eventAdmin.postEvent(event);
    }

    /**
     * Enables private message delivery to a given user. As of CometD version 2.8.0, this must be
     * called from the canHandshake method of a {@link SecurityPolicy}. See <a href=
     * "http://stackoverflow.com/questions/22695516/null-serversession-on-cometd-meta-handshake"
     * >Obtaining user and session information for private message delivery</a> for more
     * information.
     *
     * @param serverSession The {@link ServerSession} on which to deliver messages to the user for the user.
     * @param serverMessage The {@link ServerMessage} containing the userId property with which to associate
     *                      the {@code ServerSession}.
     * @throws IllegalArgumentException when the received {@code ServerSession} or the {@code ServerSession}'s id is
     *                                  null.
     */
    public void registerUserSession(final ServerSession serverSession, ServerMessage serverMessage)
            throws IllegalArgumentException {

        LOGGER.debug("ServerSession: {}\nServerMessage: {}", serverSession, serverMessage);

        if (null == serverSession) {
            throw new IllegalArgumentException("ServerSession is null");
        }

        Subject subject = null;
        try {
            subject = SecurityUtils.getSubject();
        } catch (Exception e) {
            LOGGER.debug("Couldn't grab user subject from Shiro.", e);
        }

        String userId = getUserId(serverSession, subject);

        if (null == userId) {
            throw new IllegalArgumentException("User ID is null");
        }

        userSessionMap.put(userId, serverSession);


        LOGGER.debug("Added ServerSession to userSessionMap - New map: {}", userSessionMap);
    }

    protected void queuePersistedMessages(final ServerSession serverSession,
            List<Map<String, Object>> messages, final String topic) {
        for (Map<String, Object> notification : messages) {
            final JSONObject jsonPropMap = new JSONObject();
            jsonPropMap.putAll(notification);

            LOGGER.debug("Sending the following property map \"{}\": ", jsonPropMap.toJSONString());

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    int maxAttempts = 10;
                    int attempts = 0;
                    while (!serverSession.isConnected() && attempts < maxAttempts) {
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) {
                        }
                        attempts++;
                        LOGGER.trace("Attempt {} of {} to send notifications back to client.",
                                attempts, maxAttempts);
                    }

                    LOGGER.trace("Sending notifications back to client.");
                        serverSession.deliver(controllerServerSession, topic,
                                jsonPropMap.toJSONString(), null);
                }
            });
        }
    }

    protected String getUserId(ServerSession serverSession, Subject subject) {
        String userId = null;
        if (subject != null) {
            PrincipalCollection principalCollection = subject.getPrincipals();
            for (Object principal : principalCollection.asList()) {
                if (principal instanceof SecurityAssertion) {
                    SecurityAssertion assertion = (SecurityAssertion) principal;

                    Principal jPrincipal = assertion.getPrincipal();
                    userId = jPrincipal.getName();
                    break;
                }
            }
        } else {
            userId = serverSession.getId();
        }
        return userId;
    }

    /**
     * Obtains the root topic of the controller that should be used when registering the
     * eventhandler.
     *
     * @return String representation of a root topic.
     */
    public abstract String getControllerRootTopic();
}
