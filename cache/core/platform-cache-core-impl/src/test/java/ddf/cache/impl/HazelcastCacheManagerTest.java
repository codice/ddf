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
package ddf.cache.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.config.MapConfig.EvictionPolicy;
import com.hazelcast.config.MapStoreConfig;

import ddf.cache.Cache;
import ddf.cache.CacheManager;

public class HazelcastCacheManagerTest {
    
    private static final transient Logger LOGGER = LoggerFactory.getLogger(HazelcastCacheManagerTest.class);
    
    private static final String TEST_PATH = "/src/test/resources/data/";

    private static String testCacheName = "TestCache";

    private static String testCacheItemName = "TestCacheItemName";

    private static String testCacheItemValue = "TestCacheItemValue";

    private static String testCacheItemNewValue = "TestCacheItemNEWValue";
    
    private static final String PERSISTENT_CACHE_NAME = "persistentCache";

    private CacheManager cacheMgt;

    
    @Test
    public void testCreateCache() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(testCacheName);
        assertTrue(cache != null);
        List<String> cacheList = cacheMgt.listCaches();
        assertEquals(1, cacheList.size());
        assertTrue(cacheList.contains(testCacheName));
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }
    
    @Test
    public void testCreateCacheWithProperties() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(HazelcastCacheManager.CONFIG_BACKUP_COUNT, 5);
        properties.put(HazelcastCacheManager.CONFIG_MAX_CACHE_SIZE, 100);
        properties.put(HazelcastCacheManager.CONFIG_TIME_TO_LIVE, 200);
        properties.put(HazelcastCacheManager.CONFIG_EVICTION_POLICY, HazelcastCacheManager.EVICTION_POLICY_LFU);
        
        Cache cache = cacheMgt.getCache(testCacheName, properties);
        assertTrue(cache != null);
        List<String> cacheList = cacheMgt.listCaches();
        assertEquals(1, cacheList.size());
        assertTrue(cacheList.contains(testCacheName));
        
        Map<String, Object> cacheProperties = cacheMgt.getCacheConfiguration(testCacheName);
        assertEquals(cacheProperties.get(HazelcastCacheManager.CONFIG_BACKUP_COUNT), properties.get(HazelcastCacheManager.CONFIG_BACKUP_COUNT));
        assertEquals(cacheProperties.get(HazelcastCacheManager.CONFIG_MAX_CACHE_SIZE), properties.get(HazelcastCacheManager.CONFIG_MAX_CACHE_SIZE));
        assertEquals(cacheProperties.get(HazelcastCacheManager.CONFIG_TIME_TO_LIVE), properties.get(HazelcastCacheManager.CONFIG_TIME_TO_LIVE));
        assertEquals(cacheProperties.get(HazelcastCacheManager.CONFIG_EVICTION_POLICY), EvictionPolicy.LFU);
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }
    
    @Test
    public void testCreateCacheWithXmlConfigFile() throws Exception {
        
        // Set system property that Hazelcast uses for its XML Config file
        String xmlConfigLocation = TEST_PATH + "hazelcast.xml";
        System.setProperty("hazelcast.config", xmlConfigLocation);
        
        // Create new Cache Manager that will be configured based on the XML config file
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(PERSISTENT_CACHE_NAME);
        Map<String, Object> cacheProperties = cacheMgt.getCacheConfiguration(PERSISTENT_CACHE_NAME);
        assertEquals(new Integer(2), (Integer) cacheProperties.get(HazelcastCacheManager.CONFIG_BACKUP_COUNT));
        assertEquals(new Integer(99), (Integer) cacheProperties.get(HazelcastCacheManager.CONFIG_TIME_TO_LIVE));
        assertEquals(EvictionPolicy.LFU, (EvictionPolicy) cacheProperties.get(HazelcastCacheManager.CONFIG_EVICTION_POLICY));
        MapStoreConfig mapStoreConfig = (MapStoreConfig) cacheProperties.get(HazelcastCacheManager.CONFIG_MAP_STORE);
        assertEquals("ddf.cache.impl.FileSystemMapStoreFactory", mapStoreConfig.getFactoryClassName());
        
        cacheMgt.removeCache(PERSISTENT_CACHE_NAME);
        cacheMgt.shutdown();
    }
    
    @Test
    public void testCreateCacheWithMapStore() throws Exception {
        
        // Set system property that Hazelcast uses for its XML Config file
        String xmlConfigLocation = TEST_PATH + "hazelcast.xml";
        System.setProperty("hazelcast.config", xmlConfigLocation);
        File persistenceDir = null;
        
        try {
            // Create new Cache Manager that will be configured based on the XML config file
            cacheMgt = new HazelcastCacheManager();
            Cache cache = cacheMgt.getCache(PERSISTENT_CACHE_NAME);
            MockNotification notification = new MockNotification("app", "title", "message", new Date().getTime(), "user1");
            String activityId = UUID.randomUUID().toString().replaceAll("-", "");
            cache.put(activityId, notification);
            
            FileSystemPersistenceProvider provider = new FileSystemPersistenceProvider(PERSISTENT_CACHE_NAME);
            persistenceDir = new File(provider.getPersistencePath());
            File mapStoreDir = new File(provider.getMapStorePath());
            assertTrue(mapStoreDir.exists());
            String[] persistedNotifications = mapStoreDir.list();
            assertTrue(persistedNotifications.length == 1);
            
            MockNotification n = (MockNotification) provider.loadFromPersistence(activityId);
            LOGGER.info("notification = {}", n);
            assertEquals(n.getApplication(), "app");
            assertEquals(n.getTitle(), "title");
            assertEquals(n.getMessage(), "message");
            assertEquals(n.getUserId(), "user1");

        } finally {
            cacheMgt.removeCache(PERSISTENT_CACHE_NAME);
            cacheMgt.shutdown();
            // Delete the serialized notification files - otherwise they will be read by any subsequent unit tests
            FileUtils.deleteQuietly(persistenceDir);
        }
    }
    
    @Test
    public void testQueryCacheWithMultipleUsersNotifications() throws Exception {
        
        // Set system property that Hazelcast uses for its XML Config file
        String xmlConfigLocation = TEST_PATH + "hazelcast.xml";
        System.setProperty("hazelcast.config", xmlConfigLocation);
        
        // Create new Cache Manager that will be configured based on the XML config file
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(PERSISTENT_CACHE_NAME);
        File persistenceDir = null;
        
        try {
            // Create 3 notifications for each of 3 users and add each notification to the cache.
            // These notifications should be serialized and persisted to disk by Hazelcast.
            String[] userIds = new String[] {"user1", "user2", "user3" };
            int numNotificationsPerUser = 3;
            for (int i=0; i < userIds.length; i++) {           
                int appIndex = i + 1;
                String app = "app-" + appIndex;
                for (int j=1; j <= numNotificationsPerUser; j++) {
                    String activityId = UUID.randomUUID().toString().replaceAll("-", "");
                    String title = "title-" + j;
                    String message = "message-" + j;
                    cache.put(activityId, new MockNotification(app, title, message, new Date().getTime(), userIds[i]));
                }
            }
            
            // Verify all notifications were persisted to disk
            FileSystemPersistenceProvider provider = new FileSystemPersistenceProvider(PERSISTENT_CACHE_NAME);
            String mapStorePath = provider.getMapStorePath();
            String persistencePath = provider.getPersistencePath();
            persistenceDir = new File(persistencePath);
            File mapStoreDir = new File(mapStorePath);
            assertTrue(mapStoreDir.exists());
            String[] persistedNotifications = mapStoreDir.list();
            assertTrue(persistedNotifications.length == (userIds.length * numNotificationsPerUser));
            
            // Query for specific user's notifications and verify only they are returned
            Set<MockNotification> notifications = (Set<MockNotification>) cache.query("userId = 'user2'");
            assertNotNull(notifications);
            LOGGER.info("notifications.size() = " + notifications.size());
            assertTrue(notifications.size() == numNotificationsPerUser);
            for (MockNotification n : notifications) {
                LOGGER.info("notification = {}", n);
                assertTrue(n.getUserId().equals("user2"));
                assertTrue(n.getApplication().equals("app-2"));
            }
            
            // Query with AND syntax
            notifications = (Set<MockNotification>) cache.query("userId = 'user2' AND title='title-3'");
            assertNotNull(notifications);
            LOGGER.info("notifications.size() = " + notifications.size());
            assertTrue(notifications.size() == 1);
            for (MockNotification n : notifications) {
                LOGGER.info("notification = {}", n);
                assertTrue(n.getUserId().equals("user2"));
                assertTrue(n.getTitle().equals("title-3"));
            }
            
            // Query with OR syntax
            notifications = (Set<MockNotification>) cache.query("userId = 'user2' OR userId = 'user3'");
            assertNotNull(notifications);
            LOGGER.info("notifications.size() = " + notifications.size());
            // 2 users (user2 and user3)
            assertTrue(notifications.size() == (2 * numNotificationsPerUser));
            for (MockNotification n : notifications) {
                LOGGER.info("notification = {}", n);
                assertThat(n.getUserId(), isOneOf("user2", "user3"));
            }
            
            // Get all notifications
            notifications = (Set<MockNotification>) cache.query("userId LIKE '%'");
            assertNotNull(notifications);
            LOGGER.info("notifications.size() = " + notifications.size());
            assertTrue(notifications.size() == (userIds.length * numNotificationsPerUser));
            for (MockNotification n : notifications) {
                LOGGER.info("notification = {}", n);
            }
        } finally {
            cacheMgt.removeCache(PERSISTENT_CACHE_NAME);
            cacheMgt.shutdown();
            // Delete the serialized notification files - otherwise they will be read by any subsequent unit tests
            FileUtils.deleteQuietly(persistenceDir);
        }
    }
    
    @Test
    public void testListCaches() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(testCacheName);
        Cache cache2 = cacheMgt.getCache(testCacheName + "-2");
        assertTrue(cache != null);
        assertTrue(cache2 != null);
        List<String> cacheList = cacheMgt.listCaches();
        assertTrue(cacheList.size() == 2);
        assertTrue(cacheList.contains(testCacheName));
        assertTrue(cacheList.contains(testCacheName + "-2"));
        cacheMgt.removeCache(testCacheName + "-2");
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }

    @Test
    public void testDeleteCache() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        cacheMgt.removeCache(testCacheName);
        String val = (String) cache.get(testCacheItemName);

        // Since cache removed, the values in the cache should be gone, hence null returned
        assertEquals(val, null);
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }

    @Test
    public void testPutandGetCacheItems() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        String returnVal = (String) cache.get(testCacheItemName);

        assertEquals(returnVal, testCacheItemValue);
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }

    /**
     * Verifies existing cache item is overridden with new value.
     */
    @Test
    public void testPutSameCacheItemTwice() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        cache.put(testCacheItemName, testCacheItemValue + "-2");
        String returnVal = (String) cache.get(testCacheItemName);

        assertEquals(returnVal, testCacheItemValue + "-2");
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }
    
    @Test
    public void testCacheItemUpdate() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        cache.put(testCacheItemName, testCacheItemNewValue);
        String returnVal = (String) cache.get(testCacheItemName);

        assertEquals(returnVal, testCacheItemNewValue);
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }

    @Test
    public void testListCacheItems() throws Exception {

        // given
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName + "1", testCacheItemValue + "1");
        cache.put(testCacheItemName + "2", testCacheItemValue + "2");
        cache.put(testCacheItemName + "3", testCacheItemValue + "3");
        cache.put(testCacheItemName + "4", testCacheItemValue + "4");
        cache.put(testCacheItemName + "5", testCacheItemValue + "5");
        cache.put(testCacheItemName + "6", testCacheItemValue + "6");
        cache.put(testCacheItemName + "7", testCacheItemValue + "7");
        cache.put(testCacheItemName + "8", testCacheItemValue + "8");
        cache.put(testCacheItemName + "9", testCacheItemValue + "9");
        cache.put(testCacheItemName + "10", testCacheItemValue + "10");
        cache.put(testCacheItemName + "11", testCacheItemValue + "11");
        cache.put(testCacheItemName + "12", testCacheItemValue + "12");
        cache.put(testCacheItemName + "13", testCacheItemValue + "13");
        cache.put(testCacheItemName + "14", testCacheItemValue + "14");
        cache.put(testCacheItemName + "15", testCacheItemValue + "15");
        
        // when
        Set<Object> set = cache.getKeys();
        
        // then
        for (int i = 1; i < 16; ++i) {
            assertTrue(set.contains(testCacheItemName + i));
            assertEquals(testCacheItemValue + i, cache.get(testCacheItemName + i));

        }
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }

    @Test
    public void testCacheItemDelete() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        cache.remove(testCacheItemName);
        String returnVal = (String) cache.get(testCacheItemName);

        assertEquals(returnVal, null);
        
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }
    
    /*TODO
    @Test
    public void testConfigureCacheAfterCreation() throws Exception {
        cacheMgt = new HazelcastCacheManager();
        String cache1Name = "cache-1";
        Map<String, Object> cache1Props = new HashMap<String, Object>();
        cache1Props.put(CacheManager.CONFIG_BACKUP_COUNT, 2);
        cacheMgt.createCache(cache1Name, cache1Props);
        cacheMgt.put(cache1Name, "cache-1-item-1", "abc");
        
        String cache2Name = "cache-2";
        Map<String, Object> cache2Props = new HashMap<String, Object>();
        cache2Props.put(CacheManager.CONFIG_BACKUP_COUNT, 3);
        cacheMgt.createCache(cache2Name, cache2Props);
        cacheMgt.put(cache2Name, "cache-2-item-1", "123");
        
        Map<String, Object> cache1Config = cacheMgt.getCacheConfiguration(cache1Name);
        assertTrue((Integer) cache1Config.get(CacheManager.CONFIG_BACKUP_COUNT) == 2);
        Map<Object, Object> map = cacheMgt.list(testCacheName);
        
        Map<String, Object> cache2Config = cacheMgt.getCacheConfiguration(cache2Name);
        assertTrue((Integer) cache2Config.get(CacheManager.CONFIG_BACKUP_COUNT) == 3);
        
        cacheMgt.removeCache(cache1Name);
        cacheMgt.removeCache(cache2Name);
    }
    END TODO*/

}
