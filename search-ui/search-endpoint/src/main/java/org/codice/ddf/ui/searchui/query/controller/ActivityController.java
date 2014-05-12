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

import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.notifications.Notification;
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
 * The {@code NotificationController} handles the processing and routing of 
 * notifications.
 */
@Service
public class ActivityController implements EventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ActivityController.class);

    @Session
    private ServerSession activityControllerServerSession;
    
    // CometD requires prepending the topic name with a '/' character, whereas
    // the OSGi Event Admin doesn't allow it.
    protected static final String ACTIVITY_TOPIC_COMETD = 
            "/" + ActivityEvent.EVENT_TOPIC_BROADCAST;
    
    // Synchronize the map to protect against multiple clients triggering
    // multiple Map operations at the same time
    // Set the HashMap's initial capacity to allow for 30 users without
    // resizing/rehashing the map
    // ((30 users / .75 loadFactor) + 1) = 41 = initialCapacity.
    // TODO: Should the initial size be larger? How many clients are we 
    //       anticipating per DDF instance.
    Map<String, ServerSession> userSessionMap = Collections
            .synchronizedMap(new HashMap<String, ServerSession>(41));
    
    /**
     * Establishes {@code NotificationController} as a listener to events 
     * published by the OSGi eventing framework on the 
     * {@link Notification#NOTIFICATION_TOPIC_ROOT} topic
     * 
     * @param bundleContext
     */
    public ActivityController(BundleContext bundleContext) {
        Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
        dictionary.put(EventConstants.EVENT_TOPIC, 
                ActivityEvent.EVENT_TOPIC + "/*");

        bundleContext.registerService(EventHandler.class.getName(), this, 
                dictionary);
    }

    /**
     * Obtains the {@link ServerSession} associated with a given user id.
     * 
     * @param userId The id of the user associated with the 
     * {@code ServerSession} to be retrieved.
     * @return The {@code ServerSession} associated with the received userId or 
     * null if the user does not have an established {@code ServerSession}
     */
    public ServerSession getSessionByUserId(String userId) {
        return userSessionMap.get(userId);
    }

    /**
     * Listens to the /meta/disconnect {@link org.cometd.bayeux.Channel} for 
     * clients disconnecting and deregisters the user. This should be invoked in
     * order to remove {@code NotificationController} references to invalid 
     * {@link ServerSession}s.
     * 
     * @param serverSession The {@code ServerSession} object associated with the
     * client that is disconnecting
     * @param serverMessage The {@link ServerMessage} that was sent from the 
     * client on the /meta/disconnect Channel
     */
    @Listener("/meta/disconnect")
    public void deregisterUserSession(ServerSession serverSession, 
            ServerMessage serverMessage) {
        LOGGER.debug("\nServerSession: {}\nServerMessage: {}", serverSession, 
                serverMessage);

        if (null == serverSession) {
            throw new NullPointerException("ServerSession is null");
        }

        // See NOTE in registerUserSession regarding the changes that need
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
     * 2.8.0, this must be called from the canHandshake method of a 
     * {@link SecurityPolicy}. See 
     * <a href="http://stackoverflow.com/questions/22695516/null-serversession-on-cometd-meta-handshake">Obtaining user and session information for private message delivery</a>
     * for more information.
     * 
     * @param serverSession The {@link ServerSession} on which to deliver 
     * messages to the user for the user.
     * @param serverMessage The {@link ServerMessage} containing the userId 
     * property with which to associate the {@code ServerSession}.
     * @throws NullPointerException when the received {@code ServerSession} or 
     * the {@code ServerSession}'s id is null.
     */
    public void registerUserSession(ServerSession serverSession, 
            ServerMessage serverMessage)
        throws NullPointerException {

        LOGGER.debug("ServerSession: {}\nServerMessage: {}", serverSession, 
                serverMessage);

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

        LOGGER.debug("Added ServerSession to userSessionMap - New map: {}", 
                userSessionMap);
    }

    /**
     * Implementation of {@link EventHandler#handleEvent(Event)} that receives 
     * notifications published on the {@link ActivityEvent#EVENT_TOPIC} topic
     * from the OSGi eventing framework and forwards them to their intended
     * recipients.
     * 
     * @throws IllegalArgumentException when any of the following required 
     * properties are either missing from the Event or contain empty values:
     * 
     * <ul>
     *    <li>{@link ActivityEvent#ID_KEY}</li>
     *    <li>{@link ActivityEvent#MESSAGE_KEY}</li>
     *    <li>{@link ActivityEvent#TIMESTAMP_KEY}</li
     *    <li>{@link ActivityEvent#STATUS_KEY}</li>
     *    <li>{@link ActivityEvent#USER_ID_KEY}</li>
     * </ul>
     */
    @Override
    public void handleEvent(Event event) throws IllegalArgumentException {

        if (null == event.getProperty(ActivityEvent.ID_KEY)
                || event.getProperty(ActivityEvent.ID_KEY).toString().isEmpty()) {
            throw new IllegalArgumentException("Activity Event \"" + ActivityEvent.ID_KEY
                    + "\" property is null or empty");
        }

        if (null == event.getProperty(ActivityEvent.MESSAGE_KEY)
                || event.getProperty(ActivityEvent.MESSAGE_KEY).toString().isEmpty()) {
            throw new IllegalArgumentException("Activity Event \"" + ActivityEvent.MESSAGE_KEY
                    + "\" property is null or empty");
        }

        if (null == event.getProperty(ActivityEvent.TIMESTAMP_KEY)) {
            throw new IllegalArgumentException("Activity Event \"" + ActivityEvent.TIMESTAMP_KEY
                    + "\" property is null");
        }

        if (null == event.getProperty(ActivityEvent.STATUS_KEY)
                || event.getProperty(ActivityEvent.STATUS_KEY).toString().isEmpty()) {
            throw new IllegalArgumentException("Activity Event \"" + ActivityEvent.MESSAGE_KEY
                    + "\" property is null or empty");
        }

        String userId = (String) event.getProperty(ActivityEvent.USER_ID_KEY);
        if (null == userId || userId.isEmpty()) {
            throw new IllegalArgumentException("Activity Event \"" + ActivityEvent.USER_ID_KEY
                    + "\" property is null or empty");
        }

        ServerSession recipient = getSessionByUserId(userId);
        
        if (null != recipient) {
            JSONObject jsonPropMap = new JSONObject();

            for (String key : event.getPropertyNames()) {
                if (event.getProperty(key) != null) {
                    jsonPropMap.put(key, event.getProperty(key));
                }
            }

            LOGGER.debug("Sending the following property map \"{}\": ",
                    jsonPropMap.toJSONString());

            recipient.deliver(activityControllerServerSession, 
                    ACTIVITY_TOPIC_COMETD,
                    jsonPropMap.toJSONString(), null);

        } else {
            LOGGER.debug("User with ID \"{}\" is not connected to the server. "
                    + "Ignoring activity", userId);
        }
    }
}
