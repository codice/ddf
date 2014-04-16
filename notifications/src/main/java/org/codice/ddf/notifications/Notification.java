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
package org.codice.ddf.notifications;

import java.util.HashMap;

/**
 * A {@code java.util.Map} implementation that is used in concert with the
 * OSGi Event Admin for publishing notification Events
 */
public class Notification extends HashMap<String, String> {

    private static final long serialVersionUID = -2531844838114289515L;
    
    public static final String NOTIFICATION_KEY_APPLICATION = "application";
    public static final String NOTIFICATION_KEY_MESSAGE = "message";
    public static final String NOTIFICATION_KEY_TIMESTAMP = "timestamp";
    public static final String NOTIFICATION_KEY_TITLE = "title";
    public static final String NOTIFICATION_KEY_USER_ID = "user";
    
    public static final String NOTIFICATION_TOPIC_ROOT = "ddf/notifications";
    public static final String NOTIFICATION_TOPIC_BROADCAST = 
            NOTIFICATION_TOPIC_ROOT + "/broadcast";
    public static final String NOTIFICATION_TOPIC_DOWNLOADS = 
            NOTIFICATION_TOPIC_ROOT + "/catalog/downloads";
    
    public Notification(String application, String title, String message, Long timestamp) {
        
        // TODO: modify timestamp String conversion? Format for readability?
        this(application, title, message, String.valueOf(timestamp), null);
    }
    
    public Notification(String application, String title, String message, String timestamp) {
        this(application, title, message, timestamp, null);
    }
    
    public Notification(String application, String title, String message, 
            Long timestamp, String userId) {
       this(application, title, message, String.valueOf(timestamp), userId);
    }
    
    public Notification(String application, String title, String message, String timestamp, String userId) {
        setApplication(application);
        setTitle(title);
        setMessage(message);
        setTimestamp(timestamp);
        
        if (null != userId && !userId.isEmpty()) {
            setUserId(userId);
        }
    }
    
    public String getApplication() {
        return this.get(NOTIFICATION_KEY_APPLICATION);
    }
    
    public void setApplication(String application) {
        this.put(NOTIFICATION_KEY_APPLICATION, application);
    }
    
    public String getTitle() {
        return this.get(NOTIFICATION_KEY_TITLE);
    }
    
    public void setTitle(String title) {
        this.put(NOTIFICATION_KEY_TITLE, title);
    }
    
    public String getMessage() {
        return this.get(NOTIFICATION_KEY_MESSAGE);
    }
    
    public void setMessage(String message) {
        this.put(NOTIFICATION_KEY_MESSAGE, message);
    }
    
    public String getTimestampString() {
        return this.get(NOTIFICATION_KEY_TIMESTAMP);
    }
    
    public Long getTimestampLong() {
        return Long.valueOf(this.get(NOTIFICATION_KEY_TIMESTAMP));
    }
    
    public void setTimestamp(String timestampString) {
        this.put(NOTIFICATION_KEY_TIMESTAMP, timestampString);
    }
    
    public void setTimestamp(Long timestamp) {
        this.put(NOTIFICATION_KEY_TIMESTAMP, String.valueOf(timestamp));
    }
    
    public String getUserId() {
        return this.get(NOTIFICATION_KEY_USER_ID);
    }
    
    public void setUserId(String userId) {
        this.put(NOTIFICATION_KEY_USER_ID, userId);
    }
}
