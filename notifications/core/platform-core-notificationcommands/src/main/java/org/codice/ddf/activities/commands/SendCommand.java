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

package org.codice.ddf.activities.commands;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.activities.ActivityEvent.ActivityStatus;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "activities", name = "send", description = "Send activities.")
public class SendCommand extends OsgiCommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendCommand.class);

    public static final String SERVICE_PID = "org.osgi.service.event.EventAdmin";

    @Argument(name = "User ID", description = "User ID to send notifications to. ", index = 0,
            multiValued = false, required = true)

    String userId = null;

    @Override
    protected Object doExecute() throws Exception {

        sendActivity();
        return null;
    }

    private void sendActivity() throws Exception {
        String id = UUID.randomUUID().toString().replaceAll("-", "");
        String sessionId = "mockSessionId";
        Map<String, String> operations = new HashMap<String, String>();
        operations.put("cancel", "true");
        ActivityEvent eventProperties = new ActivityEvent(id,
                sessionId,
                new Date(),
                "Activity category",
                "Activity title",
                "Activity message",
                "Activity progress",
                operations, userId, ActivityStatus.RUNNING, 100L);
        Event event = new Event(ActivityEvent.EVENT_TOPIC_BROADCAST, eventProperties);

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
