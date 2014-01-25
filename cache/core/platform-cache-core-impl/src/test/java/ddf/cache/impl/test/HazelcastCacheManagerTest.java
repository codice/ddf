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
package ddf.cache.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.cache.Cache;
import ddf.cache.CacheManager;
import ddf.cache.impl.HazelcastCacheManager;

public class HazelcastCacheManagerTest {

    private static String testCacheName = "TestCache";

    private static String testCacheItemName = "TestCacheItemName";

    private static String testCacheItemValue = "TestCacheItemValue";

    private static String testCacheItemNewValue = "TestCacheItemNEWValue";

    private CacheManager cacheMgt;

    @BeforeClass
    public static void oneTimeSetup() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);
    }

    @Before
    public void setUp() {
        cacheMgt = new HazelcastCacheManager();
    }

    @After
    public void tearDown() {
        cacheMgt.removeCache(testCacheName);
        cacheMgt.shutdown();
    }

    
    @Test
    public void testCreateCache() throws Exception {
        Cache cache = cacheMgt.getCache(testCacheName);
        assertTrue(cache != null);
        List<String> cacheList = cacheMgt.listCaches();
        assertEquals(1, cacheList.size());
        assertTrue(cacheList.contains(testCacheName));
    }

    @Test
    public void testListCaches() throws Exception {
        Cache cache = cacheMgt.getCache(testCacheName);
        Cache cache2 = cacheMgt.getCache(testCacheName + "-2");
        assertTrue(cache != null);
        assertTrue(cache2 != null);
        List<String> cacheList = cacheMgt.listCaches();
        assertTrue(cacheList.size() == 2);
        assertTrue(cacheList.contains(testCacheName));
        assertTrue(cacheList.contains(testCacheName + "-2"));
        cacheMgt.removeCache(testCacheName + "-2");
    }

    @Test
    public void testDeleteCache() throws Exception {
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        cacheMgt.removeCache(testCacheName);
        String val = (String) cache.get(testCacheItemName);

        // Since cache removed, the values in the cache should be gone, hence null returned
        assertEquals(val, null);
    }

    @Test
    public void testPutandGetCacheItems() throws Exception {
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        String returnVal = (String) cache.get(testCacheItemName);

        assertEquals(returnVal, testCacheItemValue);
    }

    /**
     * Verifies existing cache item is overridden with new value.
     */
    @Test
    public void testPutSameCacheItemTwice() throws Exception {
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        cache.put(testCacheItemName, testCacheItemValue + "-2");
        String returnVal = (String) cache.get(testCacheItemName);

        assertEquals(returnVal, testCacheItemValue + "-2");
    }
    
    @Test
    public void testCacheItemUpdate() throws Exception {
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        cache.put(testCacheItemName, testCacheItemNewValue);
        String returnVal = (String) cache.get(testCacheItemName);

        assertEquals(returnVal, testCacheItemNewValue);
    }

    @Test
    public void testListCacheItems() throws Exception {

        // given
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
    }

    @Test
    public void testCacheItemDelete() throws Exception {
        Cache cache = cacheMgt.getCache(testCacheName);
        cache.put(testCacheItemName, testCacheItemValue);
        cache.remove(testCacheItemName);
        String returnVal = (String) cache.get(testCacheItemName);

        assertEquals(returnVal, null);
    }
    
    /*TODO
    @Test
    public void testConfigureCacheAfterCreation() throws Exception {
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
