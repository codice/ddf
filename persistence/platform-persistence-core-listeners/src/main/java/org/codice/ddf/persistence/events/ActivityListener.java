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

package org.codice.ddf.persistence.events;

import org.codice.ddf.activities.ActivityEvent;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ActivityListener implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityListener.class);

    private PersistentStore persistentStore;

    public ActivityListener(PersistentStore persistentStore) {
        this.persistentStore = persistentStore;
        List<Map<String, Object>> results = null;
        try {
            results = persistentStore.get(PersistentStore.ACTIVITY_TYPE,
                    ActivityEvent.STATUS_KEY + " = '" + ActivityEvent.ActivityStatus.STARTED
                            .toString() + "'");
            for (Map<String, Object> result : results) {
                result.put(ActivityEvent.STATUS_KEY + "_txt",
                        ActivityEvent.ActivityStatus.FAILED.toString());
                result.put(ActivityEvent.MESSAGE_KEY + "_txt", "Resource retrieval failed");
                result.put(ActivityEvent.PROGRESS_KEY + "_txt", "");

                persistentStore.add(PersistentStore.ACTIVITY_TYPE, result);
            }
        } catch (PersistenceException e) {
            LOGGER.debug("PersistenceException while creating ActivityListener", e);
        } catch (Exception e) {
            LOGGER.debug("Exception while creating ActivityListener", e);
        }

    }

    @Override
    public void handleEvent(Event event) throws IllegalArgumentException {
        LOGGER.debug("Received activity on topic {}", event.getTopic());

        String id = (String) event.getProperty(ActivityEvent.ID_KEY);
        String session = (String) event.getProperty(ActivityEvent.SESSION_ID_KEY);
        String status = (String) event.getProperty(ActivityEvent.STATUS_KEY);
        if (status.equals(ActivityEvent.ActivityStatus.RUNNING.toString())) {
            return;
        }
        String title = (String) event.getProperty(ActivityEvent.TITLE_KEY);
        String message = (String) event.getProperty(ActivityEvent.MESSAGE_KEY);
        String timestamp = (String) event.getProperty(ActivityEvent.TIMESTAMP_KEY);
        Map<String, String> operations = (Map<String, String>) event
                .getProperty(ActivityEvent.OPERATIONS_KEY);
        String progress = event.getProperty(ActivityEvent.PROGRESS_KEY).toString();
        String user = (String) event.getProperty(ActivityEvent.USER_ID_KEY);
        String category = (String) event.getProperty(ActivityEvent.CATEGORY_KEY);
        Long bytes = (Long) event.getProperty(ActivityEvent.BYTES_READ_KEY);
        String downloadId = (String) event.getProperty(ActivityEvent.DOWNLOAD_ID_KEY);

        PersistentItem activityToStore = new PersistentItem();
        activityToStore.addIdProperty(id);
        activityToStore.addProperty(ActivityEvent.SESSION_ID_KEY, session);
        activityToStore.addProperty(ActivityEvent.STATUS_KEY, status);
        activityToStore.addProperty(ActivityEvent.TITLE_KEY, title);
        activityToStore.addProperty(ActivityEvent.MESSAGE_KEY, message);
        activityToStore.addProperty(ActivityEvent.TIMESTAMP_KEY, timestamp);
        for (Map.Entry<String, String> entry : operations.entrySet()) {
            activityToStore.addProperty(ActivityEvent.OPERATIONS_KEY + "_" + entry.getKey(),
                    entry.getValue());
        }
        activityToStore.addProperty(ActivityEvent.PROGRESS_KEY, progress);
        activityToStore.addProperty(ActivityEvent.USER_ID_KEY, user);
        activityToStore.addProperty(ActivityEvent.CATEGORY_KEY, category);
        activityToStore.addProperty(ActivityEvent.BYTES_READ_KEY, bytes);
        activityToStore.addProperty(ActivityEvent.DOWNLOAD_ID_KEY, downloadId);
        try {
            persistentStore.add(PersistentStore.ACTIVITY_TYPE, activityToStore);
        } catch (PersistenceException e) {
            LOGGER.info("Caught PersistenceException {}", e.getMessage());
        }
    }

}