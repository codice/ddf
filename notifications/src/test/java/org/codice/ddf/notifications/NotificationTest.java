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

import static org.junit.Assert.*;

import java.util.Date;

import org.codice.ddf.notifications.Notification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author birda
 *
 */
public class NotificationTest {
    
    

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void notificationConstructorsDoNotSetEmptyUserId() {
        Notification notification = new Notification(
                "myAppName", "testing", "testing", new Date().getTime(), "");
        assertEquals("Notification constructor accepted empty userId", null, 
                notification.getUserId());
    }
    
    @Test
    public void testSetAndGetTimestampLong() {
        Long timeNow = new Date().getTime();
        String timeNowString = String.valueOf(timeNow);
        Notification notification = new Notification(
                "myAppName", "testing", "testing", timeNowString);
        
        assertEquals("Notification did not return expected timestamp string", 
                timeNowString, notification.getTimestampString());
        
        Long newTime = new Date().getTime();
        notification.setTimestamp(String.valueOf(newTime));
        
        assertEquals("Notification did not return correct timestamp value", 
                newTime, notification.getTimestampLong());
    }
}
