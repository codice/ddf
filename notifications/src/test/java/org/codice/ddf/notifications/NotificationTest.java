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

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.codice.ddf.notifications.Notification;
import org.junit.Test;

/**
 * Class of test methods for the {@link Notification} class.
 */
public class NotificationTest {
    
    private static final String DEFAULT_USER_ID = "";

    /**
     * Test of the {@link Notification#Notification(String, String, String, String, String)}
     * constructor.
     * 
     * Verifies that the constructor will set a 
     * {@link Notification#NOTIFICATION_KEY_USER_ID} to an empty {@code String},
     * which simulates an anonymous user.
     */
    @Test
    public void testNotificationSetAnonymousUserId() {
        Notification notification = new Notification("myId", "mySessionId",
                "myAppName", "testing", "testing", new Date().getTime(), "");
        assertEquals("Notification constructor accepted empty userId", DEFAULT_USER_ID, 
                notification.getUserId());
    }
    
    /**
     * Test of the {@link Notification#getTimestampLong()} method.
     * 
     * Verifies that the conversion form {@code String} to <code>long</code>
     * does not modify the timestamp value.
     */
    @Test
    public void testSetAndGetTimestampLong() {
        Long timeNow = new Date().getTime();
        String timeNowString = String.valueOf(timeNow);
        Notification notification = new Notification("myId", "mySessionId",
                "myAppName", "testing", "testing", timeNowString);
        
        assertEquals("Notification did not return expected timestamp string", 
                timeNowString, notification.getTimestampString());
        
        Long newTime = new Date().getTime();
        notification.setTimestamp(String.valueOf(newTime));
        
        assertEquals("Notification did not return correct timestamp value", 
                newTime, notification.getTimestampLong());
    }
}
