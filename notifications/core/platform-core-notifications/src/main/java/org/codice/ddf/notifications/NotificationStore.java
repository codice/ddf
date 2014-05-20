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

import java.util.List;
import java.util.Map;

/**
 *  Internal Notification service.
 */
public interface NotificationStore {

    /**
     * Get all notifications for all users.
     * 
     * @return
     */
    public List<Map<String, String>> getNotifications();
    
    /**
     * Get all notifications for specified user.
     * 
     * @param userId
     * @return
     */
    public List<Map<String, String>> getNotifications(String userId);
    
    public void putNotification(Map<String, String> notification);
}
