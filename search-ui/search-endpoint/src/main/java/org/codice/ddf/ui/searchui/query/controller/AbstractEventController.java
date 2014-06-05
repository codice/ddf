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

import ddf.security.assertion.SecurityAssertion;
import net.minidev.json.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.notifications.Notification;
import org.codice.ddf.notifications.store.NotificationStore;
import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.server.SecurityPolicy;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The {@code AbstractEventController} handles the processing and routing of
 * events.
 */
@Service
public abstract class AbstractEventController implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEventController.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

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

    protected NotificationStore notificationStore;

    /**
     * Establishes {@code AbstractEventController} as a listener to events
     * published by the OSGi eventing framework on the event's root topic
     * 
     * @param bundleContext
     */
    public AbstractEventController(NotificationStore notificationStore, BundleContext bundleContext) {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        dictionary.put(EventConstants.EVENT_TOPIC, getControllerRootTopic());

        bundleContext.registerService(EventHandler.class.getName(), this, dictionary);

        this.notificationStore = notificationStore;
    }

    public List<Map<String, String>> getNotificationsForUser(String userId) {
        return notificationStore.getNotifications(userId);
    }

    /**
     * Obtains the {@link ServerSession} associated with a given user id.
     * 
     * @param userId
     *            The id of the user associated with the {@code ServerSession}
     *            to be retrieved.
     * @return The {@code ServerSession} associated with the received userId or
     *         null if the user does not have an established
     *         {@code ServerSession}
     */
    public ServerSession getSessionByUserId(String userId) {
        return userSessionMap.get(userId);
    }

    /**
     * Listens to the /meta/disconnect {@link org.cometd.bayeux.Channel} for
     * clients disconnecting and deregisters the user. This should be invoked in
     * order to remove {@code AbstractEventController} references to invalid
     * {@link ServerSession}s.
     * 
     * @param serverSession
     *            The {@code ServerSession} object associated with the client
     *            that is disconnecting
     * @param serverMessage
     *            The {@link ServerMessage} that was sent from the client on the
     *            /meta/disconnect Channel
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

    /**
     * Enables private message delivery to a given user. As of CometD version
     * 2.8.0, this must be called from the canHandshake method of a
     * {@link SecurityPolicy}. See <a href=
     * "http://stackoverflow.com/questions/22695516/null-serversession-on-cometd-meta-handshake"
     * >Obtaining user and session information for private message delivery</a>
     * for more information.
     * 
     * @param serverSession
     *            The {@link ServerSession} on which to deliver messages to the
     *            user for the user.
     * @param serverMessage
     *            The {@link ServerMessage} containing the userId property with
     *            which to associate the {@code ServerSession}.
     * @throws IllegalArgumentException
     *             when the received {@code ServerSession} or the
     *             {@code ServerSession}'s id is null.
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

        if(subject != null) {
            List<Map<String, String>> notifications = getNotificationsForUser(userId);

            //TODO need to also get the activities for the user and send them back here as well

            if(notifications != null && !notifications.isEmpty()) {
                queuePersistedMessages(serverSession, notifications, "/" + Notification.NOTIFICATION_TOPIC_DOWNLOADS);
            }
        }

        LOGGER.debug("Added ServerSession to userSessionMap - New map: {}", userSessionMap);
    }

    private void queuePersistedMessages(final ServerSession serverSession,
            List<Map<String, String>> messages, final String topic) {
        for (Map<String, String> notification : messages) {
            final JSONObject jsonPropMap = new JSONObject();
            jsonPropMap.putAll(notification);

            LOGGER.debug("Sending the following property map \"{}\": ",
                    jsonPropMap.toJSONString());

            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    int maxAttempts = 10;
                    int attempts = 0;
                    while(!serverSession.isConnected() && attempts < maxAttempts) {
                        try {
                            TimeUnit.SECONDS.sleep(1);
                        } catch (InterruptedException e) { }
                        attempts++;
                        LOGGER.trace("Attemp {} of {} to send notifications back to client.", attempts, maxAttempts);
                    }

                    LOGGER.trace("Sending notifications back to client.");
                    serverSession.deliver(controllerServerSession, topic,
                            jsonPropMap.toJSONString(), null);
                }
            });
        }
    }

    private String getUserId(ServerSession serverSession, Subject subject) {
        String userId = null;
        if(subject != null) {
            PrincipalCollection principalCollection = subject.getPrincipals();
            for(Object principal : principalCollection.asList()) {
                if(principal instanceof SecurityAssertion) {
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
     * Obtains the root topic of the controller that should be used when
     * registering the eventhandler.
     * 
     * @return String representation of a root topic.
     */
    public abstract String getControllerRootTopic();
}
