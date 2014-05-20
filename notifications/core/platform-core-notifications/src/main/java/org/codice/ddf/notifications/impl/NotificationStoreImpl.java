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
package org.codice.ddf.notifications.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.notifications.Notification;
import org.codice.ddf.notifications.NotificationStore;
import org.codice.ddf.notifications.PersistentNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.cache.Cache;
import ddf.cache.CacheException;
import ddf.cache.CacheManager;


/**
 * Stores (persists) the notifications for *all* users in a single
 * persistent notifications cache to disk. The default location for the
 * persisted notification is "<INSTALL_DIR>/data/persistentNotifications".
 * Notifications are stored as a HashMap of attributes vs. a Notification
 * Java object. The object persisted must be Serializable.
 *
 */
public class NotificationStoreImpl implements NotificationStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationStoreImpl.class);
    
    public static final String NOTIFICATION_CACHE_NAME = "persistentNotifications";
    
    private Cache notificationsCache;
    
    
    public NotificationStoreImpl(CacheManager cacheManager) {
        LOGGER.info("Creating {} cache", NOTIFICATION_CACHE_NAME);
        notificationsCache = cacheManager.getCache(NOTIFICATION_CACHE_NAME);
    }
    
    @Override
    public void putNotification(Map<String, String> notification) {
//        String activityId = UUID.randomUUID().toString().replaceAll("-", "");
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // Note that the notification is stored as a Map<String, String> vs. a Notification object
            // because the Notification class (from ddf-libs) is embedded in the bundle and thus is 
            // not able to be used by other bundles.
            notificationsCache.put(notification.get(PersistentNotification.NOTIFICATION_KEY_UUID), notification);
//            notificationsCache.put(activityId, notification);
            LOGGER.debug("Successfully cached notification for user = " + notification.get(Notification.NOTIFICATION_KEY_USER_ID));
        } catch (CacheException e) {
            LOGGER.info("Unable to cache notification for user = " + notification.get(Notification.NOTIFICATION_KEY_USER_ID));
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        } 
    }
    
    @Override
    public List<Map<String, String>> getNotifications() {
        List<Map<String, String>> notificationsList = new ArrayList<Map<String, String>>();
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            // Retrieve the notifications as Map<String, String> because the Notification class (from ddf-libs) 
            // is embedded in the bundle and thus is not able to be used by other bundles.
            @SuppressWarnings("unchecked")
            Set<Map<String, String>> notifications = (Set<Map<String, String>>) notificationsCache.query("userId LIKE '%'");
            for (Map<String, String> notificationMap : notifications) {
                notificationsList.add(notificationMap);
            }
        } catch (CacheException e) {
            LOGGER.info("CacheException retrieving notifications for all users", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        return notificationsList;
    }

    @Override
    public List<Map<String, String>> getNotifications(String userId) {
        List<Map<String, String>> notificationsList = new ArrayList<Map<String, String>>();
        if (StringUtils.isNotBlank(userId)) {
            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                // Retrieve the notifications as Map<String, String> because the Notification class (from ddf-libs) 
                // is embedded in the bundle and thus is not able to be used by other bundles.
                @SuppressWarnings("unchecked")
                Set<Map<String, String>> notifications = (Set<Map<String, String>>) notificationsCache.query("userId = '" + userId + "'");
                for (Map<String, String> notificationMap : notifications) {
                    notificationsList.add(notificationMap);
                }
            } catch (CacheException e) {
                LOGGER.info("CacheException retrieving notifications for all users", e);
            } finally {
                Thread.currentThread().setContextClassLoader(tccl);
            }
        }

        return notificationsList;
    }
}
