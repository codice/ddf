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
package org.codice.ddf.activities;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.codice.ddf.activities.ActivityEvent.ActivityStatus;
import org.junit.Test;

public class ActivityEventTest {

    @Test
    public void testCreateActivityEvent() {
        String id = "12345";
        Date timestamp = new Date();
        String category = "Product Retrieval";
        String title = "Download 12345";
        String message = "Downloading a file.";
        String progress = "55%";
        Map<String, String> operations = new HashMap<String, String>();
        operations.put("cancel", "true");
        String user = UUID.randomUUID().toString();
        ActivityStatus type = ActivityStatus.RUNNING;
        Long bytes = 1024000000L;
        ActivityEvent event = new ActivityEvent(id, timestamp, category, title, message, progress,
                operations, user, type, bytes);

        // id
        assertEquals(id, event.getActivityId());
        assertEquals(id, event.get(ActivityEvent.ID_KEY));

        // time stamp
        assertEquals(timestamp, event.getTimestamp());
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis((Long.parseLong(event.get(ActivityEvent.TIMESTAMP_KEY).toString())));
        assertEquals(timestamp, cal.getTime());
        
        // category
        assertEquals(category, event.getCategory());
        assertEquals(category, event.get(ActivityEvent.CATEGORY_KEY));
        
        // title
        assertEquals(title, event.getTitle());
        assertEquals(title, event.get(ActivityEvent.TITLE_KEY));
        
        // message
        assertEquals(message, event.getMessage());
        assertEquals(message, event.get(ActivityEvent.MESSAGE_KEY));
        
        // progress
        assertEquals(progress, event.get(ActivityEvent.PROGRESS_KEY));
        
        // operations
        assertEquals(operations, event.get(ActivityEvent.OPERATIONS_KEY));
        
        // user
        assertEquals(user, event.getUserId());
        assertEquals(user, event.get(ActivityEvent.USER_ID_KEY));
        
        // type
        assertEquals(type, ActivityStatus.valueOf(event.getActivityType()));
        assertEquals(type, ActivityStatus.valueOf(event.get(ActivityEvent.STATUS_KEY).toString()));

        // bytes
        assertEquals(bytes, event.getBytesRead());
        assertEquals(bytes, event.get(ActivityEvent.BYTES_READ_KEY));
    }

}
