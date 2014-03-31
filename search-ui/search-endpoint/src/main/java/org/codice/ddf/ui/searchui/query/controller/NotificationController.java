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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import net.minidev.json.JSONObject;

import org.cometd.annotation.Listener;
import org.cometd.annotation.Service;
import org.cometd.annotation.Session;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.bayeux.server.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The NotificationController handles the processing and routing of 
 * notifications.
 */
@Service
public class NotificationController implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationController.class);

    @Session
    private ServerSession notificationControllerServerSession;

    public static final String NOTIFICATIONS_TOPIC_NAME = "ddf/notification/catalog/downloads";

    public static final String NOTIFICATION_APPLICATION_KEY = "application";

    public static final String NOTIFICATION_MESSAGE_KEY = "message";

    public static final String NOTIFICATION_TIMESTAMP_KEY = "timestamp";

    public static final String NOTIFICATION_TITLE_KEY = "title";

    public static final String NOTIFICATION_USER_KEY = "user";
    
    // TODO: Should the initial size be larger? How many clients are we 
    //       anticipating per DDF instance.
    
    // Synchronize the map to protect against multiple clients triggering
    // multiple Map operations at the same time
    // Set the HashMap's initial capacity to allow for 30 users without
    // resizing/rehashing the map
    // ((30 users / .75 loadFactor) + 1) = 41 = initialCapacity.
    Map<String, ServerSession> userSessionMap = Collections
            .synchronizedMap(new HashMap<String, ServerSession>(41));
    
    /**
     * Establishes {@link NotificationController} as a listener to events 
     * published by the OSGi eventing framework on the 
     * {@link NotificationController#NOTIFICATIONS_TOPIC_NAME} topic
     * 
     * @param bundleContext
     */
    public NotificationController(BundleContext bundleContext) {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        dictionary.put(EventConstants.EVENT_TOPIC, NOTIFICATIONS_TOPIC_NAME);

        bundleContext.registerService(EventHandler.class.getName(), this, dictionary);
    }

    /**
     * Obtains the {@link ServerSession} associated with a given user id.
     * 
     * @param userId The id of the user associated with the ServerSession to be
     * retrieved.
     * @return The {@link ServerSession} associated with the received userId or 
     * null if the user does not have an established {@link ServerSession}
     */
    public ServerSession getSessionByUserId(String userId) {
        return userSessionMap.get(userId);
    }

    /**
     * Listens to the /meta/disconnect Channel for clients disconnecting and
     * deregisters the user. This should be invoked in order to remove 
     * {@link NotificationController} references to invalid 
     * {@link ServerSessions}.
     * 
     * @param serverSession The {@link ServerSession} object associated with the
     * client that is disconnecting
     * @param serverMessage The {@link ServerMessage} that was sent from the 
     * client on the /meta/disconnect Channel
     */
    // TODO: Need a onbeforeunload event in the client to trigger the disconnect message
    @Listener("/meta/disconnect")
    public void deregisterUserSession(ServerSession serverSession, ServerMessage serverMessage) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\nServerSession: {}\nServerMessage: {}", 
                    serverSession, serverMessage);
        }

        if (null == serverSession) {
            throw new NullPointerException("ServerSession is null");
        }

        // See NOTE in handleHandshakeMetadata regarding the changes that need
        // to be made when this is changed to obtain the user ID from the client.
        String userId = serverSession.getId();

        if (null == userId) {
            throw new NullPointerException("ServerSession ID is null");
        }

        if (null != getSessionByUserId(userId)) {
            userSessionMap.remove(userId);
        } else {
            LOGGER.debug("userSessionMap does not contain a user with the id \"{}\"", userId);
        }
    }

    /**
     * Enables private message delivery to a given user. As of CometD version 
     * 2.8.0, CometD this must be called from the canHandshake method of a 
     * {@link SecurityPolicy}. See 
     * <a href="http://stackoverflow.com/questions/22695516/null-serversession-on-cometd-meta-handshake">Obtaining user and session information for private message delivery</a>
     * for more information.
     * 
     * @param serverSession The {@link ServerSession} on which to deliver 
     * messages to the user for the user.
     * @param serverMessage The {@link ServerMessage} containing the userId 
     * property with which to associate the {@link ServerSession}.
     * @throws NullPointerException when the received {@link ServerSession} or 
     * the {@link ServerSession}'s id is null.
     */
    public void registerUserSession(ServerSession serverSession, 
            ServerMessage serverMessage)
        throws NullPointerException {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ServerSession: " + serverSession + "\nServerMessage: " + serverMessage);
        }

        if (null == serverSession) {
            throw new NullPointerException("ServerSession is null");
        }

        // NOTE: When this is modified to use a user ID rather than a session
        // ID, the user ID will be in the ServerMessage
        // (message.get(attributeName)). This statement will need to be
        // modified and the corresponding unit test case will need to be
        // modified to verify behavior for a null user ID rather than a null
        // ServerSession ID.
        String userId = serverSession.getId();

        if (null == userId) {
            throw new NullPointerException("ServerSession ID is null");
        }

        userSessionMap.put(userId, serverSession);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Added ServerSession to userSessionMap - New map: {}", 
                    userSessionMap);
        }
    }

    /**
     * Implementation of {@link EventHandler#handleEvent(Event)} that receives 
     * notifications published on the {@link #NOTIFICATIONS_TOPIC_NAME} topic
     * from the OSGi eventing framework and forwards them to their intended
     * recipients.
     * 
     * @throws IllegalArgumentException when any of the following required 
     * properties are either missing from the Event or contain empty values:
     * 
     * <ul>
     *    <li>{@link #NOTIFICATION_APPLICATION_KEY}</li>
     *    <li>{@link #NOTIFICATION_MESSAGE_KEY}</li>
     *    <li>{@link #NOTIFICATION_TIMESTAMP_KEY}</li
     *    <li>{@link #NOTIFICATION_TITLE_KEY}</li>
     *    <li>{@link #NOTIFICATION_USER_KEY}</li>
     * </ul>
     */
    @Override
    public void handleEvent(Event event) throws IllegalArgumentException {

        // TODO: Is it the server that should handle these error conditions, or
        // the client??
        if (null == event.getProperty(NOTIFICATION_APPLICATION_KEY)
                || event.getProperty(NOTIFICATION_APPLICATION_KEY).toString().isEmpty()) {
            throw new IllegalArgumentException("Event \"" + NOTIFICATION_APPLICATION_KEY
                    + "\" property is null or empty");
        }

        if (null == event.getProperty(NOTIFICATION_MESSAGE_KEY)
                || event.getProperty(NOTIFICATION_MESSAGE_KEY).toString().isEmpty()) {
            throw new IllegalArgumentException("Event \"" + NOTIFICATION_MESSAGE_KEY
                    + "\" property is null or empty");
        }

        if (null == event.getProperty(NOTIFICATION_TIMESTAMP_KEY)) {
            throw new IllegalArgumentException("Event \"" + NOTIFICATION_TIMESTAMP_KEY
                    + "\" property is null");
        }

        if (null == event.getProperty(NOTIFICATION_TITLE_KEY)
                || event.getProperty(NOTIFICATION_TITLE_KEY).toString().isEmpty()) {
            throw new IllegalArgumentException("Event \"" + NOTIFICATION_TITLE_KEY
                    + "\" property is null or empty");
        }

        String userId = (String) event.getProperty(NOTIFICATION_USER_KEY);
        if (null == userId || userId.isEmpty()) {
            throw new IllegalArgumentException("Event \"" + NOTIFICATION_USER_KEY
                    + "\" property is null or empty");
        }

        ServerSession recipient = getSessionByUserId(userId);
        if (null != recipient) {
            JSONObject jsonPropMap = new JSONObject();

            for (String key : event.getPropertyNames()) {
                jsonPropMap.put(key, event.getProperty(key));
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sending the following property map to \"{}\": ",
                        jsonPropMap.toJSONString());
            }

            recipient.deliver(notificationControllerServerSession, NOTIFICATIONS_TOPIC_NAME,
                    jsonPropMap.toJSONString(), null);

        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("User with ID \"{}\" is not connected to the server. "
                    + "Ignnoring notification", userId);
        }
    }
}
