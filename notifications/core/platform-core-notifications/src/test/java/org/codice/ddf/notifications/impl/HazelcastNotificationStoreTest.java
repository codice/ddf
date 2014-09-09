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

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapConfig.EvictionPolicy;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.core.HazelcastInstance;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class HazelcastNotificationStoreTest {

    private static final transient Logger LOGGER = LoggerFactory
            .getLogger(HazelcastNotificationStoreTest.class);

    private static final String TEST_PATH = "/src/main/resources/";

    private static final String PERSISTENT_CACHE_NAME = "persistentNotifications";


    @Test
    public void testCreateCacheWithXmlConfigFile() throws Exception {

        // Set system property that Hazelcast uses for its XML Config file
        String xmlConfigFilename = "notifications-hazelcast.xml";
        String xmlConfigLocation = System.getProperty("user.dir") + TEST_PATH
                + "notifications-hazelcast.xml";
        System.setProperty("hazelcast.config", xmlConfigLocation);

        Bundle bundle = mock(Bundle.class);
        URL url = new URL("file:///" + new File(xmlConfigLocation).getAbsolutePath());
        when(bundle.getResource(anyString())).thenReturn(url);
        BundleContext context = mock(BundleContext.class);
        when(context.getBundle()).thenReturn(bundle);

        // Create new NotificationStore that will be configured based on the XML config file
        HazelcastNotificationStore store = new HazelcastNotificationStore(context,
                xmlConfigFilename);
        HazelcastInstance instance = store.getHazelcastInstance();

        MapConfig mapConfig = instance.getConfig().getMapConfig(PERSISTENT_CACHE_NAME);
        assertNotNull(mapConfig);
        assertEquals(0, mapConfig.getBackupCount());
        assertEquals(Integer.MAX_VALUE, mapConfig.getMaxSizeConfig().getSize());
        assertEquals(0, mapConfig.getTimeToLiveSeconds());
        assertEquals(EvictionPolicy.NONE, mapConfig.getEvictionPolicy());
        MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
        assertEquals("org.codice.ddf.notifications.impl.FileSystemMapStoreFactory",
                mapStoreConfig.getFactoryClassName());

        instance.getMap(PERSISTENT_CACHE_NAME).destroy();
        instance.shutdown();
    }

    @Test
    public void testCreateCacheWithMapStore() throws Exception {

        // Set system property that Hazelcast uses for its XML Config file
        String xmlConfigFilename = "notifications-hazelcast.xml";
        String xmlConfigLocation = System.getProperty("user.dir") + TEST_PATH
                + "notifications-hazelcast.xml";
        System.setProperty("hazelcast.config", xmlConfigLocation);

        Bundle bundle = mock(Bundle.class);
        URL url = new URL("file:///" + new File(xmlConfigLocation).getAbsolutePath());
        when(bundle.getResource(anyString())).thenReturn(url);
        BundleContext context = mock(BundleContext.class);
        when(context.getBundle()).thenReturn(bundle);

        // Create new NotificationStore that will be configured based on the XML config file
        HazelcastNotificationStore store = new HazelcastNotificationStore(context,
                xmlConfigFilename);
        HazelcastInstance instance = store.getHazelcastInstance();
        File persistenceDir = null;

        try {
            MockNotification notification = new MockNotification("app", "title", "message",
                    new Date().getTime(), "user1");
            store.putNotification(notification);

            FileSystemPersistenceProvider provider = new FileSystemPersistenceProvider(
                    PERSISTENT_CACHE_NAME);
            persistenceDir = new File(provider.getPersistencePath());
            File mapStoreDir = new File(provider.getMapStorePath());
            assertTrue(mapStoreDir.exists());
            String[] persistedNotifications = mapStoreDir.list();
            assertTrue(persistedNotifications.length == 1);

            MockNotification n = (MockNotification) provider.loadFromPersistence(notification
                    .getId());
            LOGGER.info("notification = {}", n);
            assertEquals(n.getApplication(), "app");
            assertEquals(n.getTitle(), "title");
            assertEquals(n.getMessage(), "message");
            assertEquals(n.getUserId(), "user1");

        } finally {
            instance.getMap(PERSISTENT_CACHE_NAME).destroy();
            instance.shutdown();
            // Delete the serialized notification files - otherwise they will be read by any
            // subsequent unit tests
            FileUtils.deleteQuietly(persistenceDir);
        }
    }

    @Test
    public void testQueryCacheWithMultipleUsersNotifications() throws Exception {

        // Set system property that Hazelcast uses for its XML Config file
        String xmlConfigFilename = "notifications-hazelcast.xml";
        String xmlConfigLocation = System.getProperty("user.dir") + TEST_PATH
                + "notifications-hazelcast.xml";
        System.setProperty("hazelcast.config", xmlConfigLocation);

        Bundle bundle = mock(Bundle.class);
        URL url = new URL("file:///" + new File(xmlConfigLocation).getAbsolutePath());
        when(bundle.getResource(anyString())).thenReturn(url);
        BundleContext context = mock(BundleContext.class);
        when(context.getBundle()).thenReturn(bundle);

        // Create new NotificationStore that will be configured based on the XML config file
        HazelcastNotificationStore store = new HazelcastNotificationStore(context,
                xmlConfigFilename);
        HazelcastInstance instance = store.getHazelcastInstance();
        File persistenceDir = null;

        try {
            // Create 3 notifications for each of 3 users and add each notification to the cache.
            // These notifications should be serialized and persisted to disk by Hazelcast.
            String[] userIds = new String[] {"user1", "user2", "user3"};
            int numNotificationsPerUser = 3;
            for (int i = 0; i < userIds.length; i++) {
                int appIndex = i + 1;
                String app = "app-" + appIndex;
                for (int j = 1; j <= numNotificationsPerUser; j++) {
                    String title = "title-" + j;
                    String message = "message-" + j;
                    store.putNotification(new MockNotification(app, title, message, new Date()
                            .getTime(), userIds[i]));
                }
            }

            // Verify all notifications were persisted to disk
            FileSystemPersistenceProvider provider = new FileSystemPersistenceProvider(
                    PERSISTENT_CACHE_NAME);
            String mapStorePath = provider.getMapStorePath();
            String persistencePath = provider.getPersistencePath();
            persistenceDir = new File(persistencePath);
            File mapStoreDir = new File(mapStorePath);
            assertTrue(mapStoreDir.exists());
            String[] persistedNotifications = mapStoreDir.list();
            assertTrue(persistedNotifications.length == (userIds.length * numNotificationsPerUser));

            // Query for specific user's notifications and verify only they are returned
            List<Map<String, String>> notifications = store.getNotifications("user2");
            assertNotNull(notifications);
            LOGGER.info("notifications.size() = " + notifications.size());
            assertTrue(notifications.size() == numNotificationsPerUser);
            for (Map<String, String> n : notifications) {
                LOGGER.info("notification = {}", n);
                assertTrue(n.get(MockNotification.NOTIFICATION_KEY_USER_ID).equals("user2"));
                assertTrue(n.get(MockNotification.NOTIFICATION_KEY_APPLICATION).equals("app-2"));
            }

            // Get all notifications
            notifications = store.getNotifications();
            assertNotNull(notifications);
            LOGGER.info("notifications.size() = " + notifications.size());
            assertTrue(notifications.size() == (userIds.length * numNotificationsPerUser));
            for (Map<String, String> n : notifications) {
                LOGGER.info("notification = {}", n);
            }

            // Remove all persisted notifications from a user, then get all notifications to
            // verify that only notifications from specified user were removed
            notifications = store.getNotifications();
            assertNotNull(notifications);
            for (Map<String, String> n : notifications) {
                if (n.get(MockNotification.NOTIFICATION_KEY_USER_ID).equals("user2")) {
                    store.removeNotification(n.get(MockNotification.NOTIFICATION_KEY_UUID), "user2");
                }
            }
            notifications = store.getNotifications();
            for (Map<String, String> n : notifications) {
                if (n.get(MockNotification.NOTIFICATION_KEY_USER_ID).equals("user2")) {
                    fail("Notification was not removed.");
                }
            }

        } finally {
            instance.getMap(PERSISTENT_CACHE_NAME).destroy();
            instance.shutdown();
            // Delete the serialized notification files - otherwise they will be read by any
            // subsequent unit tests
            FileUtils.deleteQuietly(persistenceDir);
        }
    }

}
