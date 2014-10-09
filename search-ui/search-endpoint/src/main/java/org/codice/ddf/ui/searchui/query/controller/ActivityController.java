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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONObject;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.activities.ActivityEvent;
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
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code ActivityController} handles the processing and routing of activities.
 */
@Service
public class ActivityController extends AbstractEventController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityController.class);
    private static final String CANCEL_ACTION = "cancel";
    private static final String REMOVE_ACTION = "remove";


    // CometD requires prepending the topic name with a '/' character, whereas
    // the OSGi Event Admin doesn't allow it.
    protected static final String ACTIVITY_TOPIC_COMETD = "/" + ActivityEvent.EVENT_TOPIC;

    public ActivityController(PersistentStore persistentStore, BundleContext bundleContext,
            EventAdmin eventAdmin) {
        super(persistentStore, bundleContext, eventAdmin);
    }

    /**
     * Implementation of {@link EventHandler#handleEvent(Event)} that receives
     * notifications published on the {@link ActivityEvent#EVENT_TOPIC} topic
     * from the OSGi eventing framework and forwards them to their intended
     * recipients.
     *
     * @throws IllegalArgumentException when any of the following required properties are either
     *                                  missing from the Event or contain empty values:
     *                                  <p/>
     *                                  <ul>
     *                                  <li>{@link ActivityEvent#ID_KEY}</li>
     *                                  <li>{@link ActivityEvent#MESSAGE_KEY}</li>
     *                                  <li>{@link ActivityEvent#TIMESTAMP_KEY}</li
     *                                  <li>{@link ActivityEvent#STATUS_KEY}</li>
     *                                  <li>{@link ActivityEvent#USER_ID_KEY}</li>
     *                                  </ul>
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

        String sessionId = (String) event.getProperty(ActivityEvent.SESSION_ID_KEY);
        if (StringUtils.isEmpty(sessionId)) {
            throw new IllegalArgumentException("Activity Event \"" + ActivityEvent.SESSION_ID_KEY
                    + "\" property is null or empty");
        }

        String userId = (String) event.getProperty(ActivityEvent.USER_ID_KEY);
        // Blank user ID is allowed as this indicates the anonymous user
        if (null == userId) {
            throw new IllegalArgumentException("Activity Event \"" + ActivityEvent.USER_ID_KEY
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
                if (!EventConstants.EVENT_TOPIC.equals(key)
                        && !ActivityEvent.USER_ID_KEY.equals(key) && event.getProperty(key) != null) {
                    jsonPropMap.put(key, event.getProperty(key));
                }
            }

            LOGGER.debug("Sending the following property map \"{}\": ", jsonPropMap.toJSONString());

            recipient.deliver(controllerServerSession, ACTIVITY_TOPIC_COMETD,
                    jsonPropMap.toJSONString(), null);

        } else {
            LOGGER.debug("Session with ID \"{}\" is not connected to the server. "
                    + "Ignoring activity", sessionId);
        }
    }

    public List<Map<String, Object>> getActivitiesForUser(String userId) {
        List<Map<String, Object>> activities = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> results = new ArrayList<Map<String, Object>>();
        try {
            results = persistentStore.get(PersistentStore.ACTIVITY_TYPE, ActivityEvent.USER_ID_KEY
                    + " = '" + userId + "'");
        } catch (PersistenceException e) {
            LOGGER.debug("PersistenceException trying to get activities for user {}", userId, e);
        }
        for (Map<String, Object> result : results) {
            Map<String, Object> sanitizedResult = PersistentItem.stripSuffixes(result);
            Map<String, Object> activity = new HashMap<String, Object>();
            activity.put(ActivityEvent.OPERATIONS_KEY, new HashMap<String, String>());
            for (Map.Entry<String, Object> entry : sanitizedResult.entrySet()) {
                if (entry.getKey().contains(ActivityEvent.OPERATIONS_KEY + "_")) {
                    ((Map) activity.get(ActivityEvent.OPERATIONS_KEY)).put(
                            entry.getKey()
                                    .substring(
                                            (ActivityEvent.OPERATIONS_KEY + "_")
                                                    .length()), entry
                                    .getValue().toString());
                } else {
                    activity.put(entry.getKey(), entry.getValue().toString());
                }
            }
            activities.add(activity);
        }

        return activities;
    }

    @Listener('/' + ActivityEvent.EVENT_TOPIC)
    public void getPersistedActivities(final ServerSession remote, Message message) {
        Map<String, Object> data = message.getDataAsMap();
        if (CollectionUtils.isEmpty((Collection) data)) {
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

            List<Map<String, Object>> activities = getActivitiesForUser(userId);

            if (CollectionUtils.isNotEmpty((Collection) activities)) {
                queuePersistedMessages(remote, activities, ACTIVITY_TOPIC_COMETD);
            }
        }
    }

    @Listener("/service/action")
    public void deletePersistentActivity(ServerSession serverSession, ServerMessage serverMessage) {
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

        Object activitiesPreCast = serverMessage.getDataAsMap().get("data");
        Object[] activities = activitiesPreCast instanceof List ?
                ((List) activitiesPreCast).toArray() :
                (Object[]) activitiesPreCast;

        for (Object activityObject : activities) {
            Map activity = (Map) activityObject;
            String id = (String) activity.get("id");
            String action = (String) activity.get("action");

            if (action != null) {
                if (REMOVE_ACTION.equals(action)) {
                    //You can have a blank id for anonymous
                    if (id != null) {
                        try {
                            this.persistentStore.delete(PersistentStore.ACTIVITY_TYPE,
                                    "id = '" + id + "'");
                        } catch (PersistenceException e) {
                            throw new IllegalArgumentException(
                                    "Unable to delete activity with id = " + id);
                        }
                    } else {
                        throw new IllegalArgumentException("Message id is null");
                    }
                } else if (CANCEL_ACTION.equals(action)) {

                    if (null == userId) {
                        throw new IllegalArgumentException("User ID is null");
                    }
                    if (null == id) {
                        throw new IllegalArgumentException("Metadata ID is null");
                    }

                    JSONObject jsonPropMap = new JSONObject();
                    jsonPropMap.put(ActivityEvent.DOWNLOAD_ID_KEY, id);

                    Event event = new Event(ActivityEvent.EVENT_TOPIC_DOWNLOAD_CANCEL, jsonPropMap);
                    eventAdmin.postEvent(event);

                }
            } else {
                throw new IllegalArgumentException("Message action is null.");
            }
        }
    }

    @Override
    public String getControllerRootTopic() {
        return ActivityEvent.EVENT_TOPIC; 
    }
}
