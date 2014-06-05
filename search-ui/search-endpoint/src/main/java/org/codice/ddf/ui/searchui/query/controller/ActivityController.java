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
import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.notifications.store.NotificationStore;
import org.cometd.annotation.Service;
import org.cometd.bayeux.server.ServerSession;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@code ActivityController} handles the processing and routing of
 * activities.
 */
@Service
public class ActivityController extends AbstractEventController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityController.class);

    // CometD requires prepending the topic name with a '/' character, whereas
    // the OSGi Event Admin doesn't allow it.
    protected static final String ACTIVITY_TOPIC_COMETD = "/" + ActivityEvent.EVENT_TOPIC_BROADCAST;

    public ActivityController(NotificationStore notificationStore, BundleContext bundleContext) {
        super(notificationStore, bundleContext);
    }

    /**
     * Implementation of {@link EventHandler#handleEvent(Event)} that receives
     * notifications published on the {@link ActivityEvent#EVENT_TOPIC} topic
     * from the OSGi eventing framework and forwards them to their intended
     * recipients.
     * 
     * @throws IllegalArgumentException
     *             when any of the following required properties are either
     *             missing from the Event or contain empty values:
     * 
     *             <ul>
     *             <li>{@link ActivityEvent#ID_KEY}</li>
     *             <li>{@link ActivityEvent#MESSAGE_KEY}</li>
     *             <li>{@link ActivityEvent#TIMESTAMP_KEY}</li
     *             <li>{@link ActivityEvent#STATUS_KEY}</li>
     *             <li>{@link ActivityEvent#USER_ID_KEY}</li>
     *             </ul>
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

            LOGGER.debug("Sending the following property map \"{}\": ", jsonPropMap.toJSONString());

            recipient.deliver(controllerServerSession, ACTIVITY_TOPIC_COMETD,
                    jsonPropMap.toJSONString(), null);

        } else {
            LOGGER.debug("User with ID \"{}\" is not connected to the server. "
                    + "Ignoring activity", userId);
        }
    }

    @Override
    public String getControllerRootTopic() {
        return ActivityEvent.EVENT_TOPIC + "/*";
    }
}
