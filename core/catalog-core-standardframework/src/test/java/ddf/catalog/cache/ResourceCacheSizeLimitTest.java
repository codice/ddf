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
package ddf.catalog.cache;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.test.TestHazelcastInstanceFactory;

import ddf.catalog.cache.impl.ProductCacheDirListener;
import ddf.catalog.cache.impl.ResourceCache;
import ddf.catalog.resource.data.ReliableResource;


public class ResourceCacheSizeLimitTest {

    private static final String PRODUCT_CACHE_NAME = "Product_Cache";
    private static TestHazelcastInstanceFactory hcInstanceFactory;
    private static String productCacheDir;
    private static final transient Logger LOGGER = LoggerFactory.getLogger(ResourceCacheSizeLimitTest.class);

    @BeforeClass
    public static void oneTimeSetup() {
        String workingDir = System.getProperty("user.dir") + File.separator + "target";
        System.setProperty("karaf.home", workingDir);
        productCacheDir = workingDir + File.separator + ResourceCache.DEFAULT_PRODUCT_CACHE_DIRECTORY;
        hcInstanceFactory = new TestHazelcastInstanceFactory(10);
        
    }
    
    @AfterClass
    public static void oneTimeTeardown(){
        LOGGER.debug("instances still remaining" + Hazelcast.getAllHazelcastInstances().size());
    }
    
    @After
    public void teardownTest() throws IOException{
        Collection<HazelcastInstance> instances = hcInstanceFactory.getAllHazelcastInstances();
        HazelcastInstance instance = instances.iterator().next();
        instance.shutdown();
        cleanProductCacheDirectory();
    }
    
    @Test
    //@Ignore
    public void testExceedCacheDirMaxSize() throws IOException, InterruptedException {
        HazelcastInstance instance = initializeTestHazelcastInstance(15);
        IMap<String, ReliableResource> cacheMap = instance.getMap(PRODUCT_CACHE_NAME);
        
        //Simulate adding product to product cache
        String rr1Key = "rr1";
        String rr1FileName = "10bytes.txt";
        simulateAddFileToProductCache(rr1Key, rr1FileName, rr1FileName, cacheMap);
                
        //ensure that the entry has not been removed from the cache since it doesn't exceed the max size
        verifyCached(cacheMap, rr1Key, rr1FileName);
        
        //simulate adding additional product to cache
        String rr2Key = "rr2";
        String rr2FileName = "15bytes.txt";
        simulateAddFileToProductCache(rr2Key, rr2FileName, rr2FileName, cacheMap);

        //wait for poller to kick off that will remove rr1
        TimeUnit.SECONDS.sleep(1);
        
        verifyRemovedFromCache(cacheMap, rr1Key, rr1FileName);
        verifyCached(cacheMap, rr2Key, rr2FileName);
    }

    @Test
    public void testExceedCacheDirMaxSizeMultipleEvictions() throws IOException, InterruptedException {
        HazelcastInstance instance = initializeTestHazelcastInstance(28);
        IMap<String, ReliableResource> cacheMap = instance.getMap(PRODUCT_CACHE_NAME);
        
        //Simulate adding product to product cache
        String rr1Key = "rr1";
        String rr1FileName = "10bytes.txt";
        simulateAddFileToProductCache(rr1Key, rr1FileName, rr1FileName, cacheMap);
                
        //ensure that the entry has not been removed from the cache since it doesn't exceed the max size
        ReliableResource rrFromCache = (ReliableResource)cacheMap.get(rr1Key);
        assertNotNull(rrFromCache);
        
        //simulate adding additional product to cache
        String rr2Key = "rr2";
        String rr2FileName = "15bytes.txt";
        simulateAddFileToProductCache(rr2Key, rr2FileName, rr2FileName, cacheMap);
        
        //simulate adding additional product to cache
        String rr3Key = "rr3";
        String rr3FileName = "15bytes_B.txt";
        simulateAddFileToProductCache(rr3Key, rr3FileName, rr3FileName, cacheMap);

        //wait for poller to kick off that will remove rr1
        TimeUnit.SECONDS.sleep(1);
        
        verifyRemovedFromCache(cacheMap, rr1Key, rr1FileName);
        verifyRemovedFromCache(cacheMap, rr2Key, rr2FileName);
        verifyCached(cacheMap, rr3Key, rr3FileName);
    }
    
    @Test
    //@Ignore
    public void testNotExceedCacheDirMaxSize() throws IOException, InterruptedException {
        HazelcastInstance instance = initializeTestHazelcastInstance(50);
        IMap<String, ReliableResource> cacheMap = instance.getMap(PRODUCT_CACHE_NAME);
        
        //Simulate adding product to product cache
        String rr1Key = "rr1";
        String rr1FileName = "10bytes.txt";
        simulateAddFileToProductCache(rr1Key, rr1FileName, rr1FileName, cacheMap);
        
        //simulate adding additional product to cache
        String rr2Key = "rr2";
        String rr2FileName = "15bytes.txt";
        simulateAddFileToProductCache(rr2Key, rr2FileName, rr2FileName, cacheMap);
        
        //simulate adding additional product to cache
        String rr3Key = "rr3";
        String rr3FileName = "15bytes_B.txt";
        simulateAddFileToProductCache(rr3Key, rr3FileName, rr3FileName, cacheMap);

        //wait for poller to kick off that will remove rr1
        TimeUnit.SECONDS.sleep(1);
        
        verifyCached(cacheMap, rr1Key, rr1FileName);
        verifyCached(cacheMap, rr2Key, rr2FileName);
        verifyCached(cacheMap, rr3Key, rr3FileName);
    }
    
    @Test
    public void testSingleFileExceedCacheDirMaxSize() throws IOException, InterruptedException {
        HazelcastInstance instance = initializeTestHazelcastInstance(5);
        IMap<String, ReliableResource> cacheMap = instance.getMap(PRODUCT_CACHE_NAME);
        
        //Simulate adding product to product cache
        String rr1Key = "rr1";
        String rr1FileName = "10bytes.txt";
        simulateAddFileToProductCache(rr1Key, rr1FileName, rr1FileName, cacheMap);
        
        //wait for poller to kick off that will remove rr1
        TimeUnit.SECONDS.sleep(1);
        
        verifyRemovedFromCache(cacheMap, rr1Key, rr1FileName);
    }
    
    @Test
    public void testCacheDirMaxSizeManyEntries() throws IOException, InterruptedException{
        HazelcastInstance instance = initializeTestHazelcastInstance(10);
        IMap<String, ReliableResource> cacheMap = instance.getMap(PRODUCT_CACHE_NAME);
        
        //Simulate adding product to product cache
        String rrKeyPrefix = "rr";
        String rr1FileNameBase = "10bytes.txt";
        int indexOfRemainingEntry = 11;
        
        for(int i = 0; i < 11; i++){
            simulateAddFileToProductCache(rrKeyPrefix + i, rr1FileNameBase, i + rr1FileNameBase, cacheMap);
        }
        
        //not in loop in order to slightly delay this file being added to the cache so it is sorted correctly and not accidentally removed
        simulateAddFileToProductCache(rrKeyPrefix + 11, rr1FileNameBase, 11 + rr1FileNameBase, cacheMap);
        
        TimeUnit.SECONDS.sleep(1);
        
        //entries from 0-10 should be removed from cache
        for(int i = 0; i < 11; i++){
            verifyRemovedFromCache(cacheMap, rrKeyPrefix + 1, i + rr1FileNameBase);
        }
        
        verifyCached(cacheMap, rrKeyPrefix + indexOfRemainingEntry, indexOfRemainingEntry + rr1FileNameBase);
    }
    
    @Test
    public void testCacheDirMaxSizePaging() throws IOException, InterruptedException{
        HazelcastInstance instance = initializeTestHazelcastInstance(132);
        IMap<String, ReliableResource> cacheMap = instance.getMap(PRODUCT_CACHE_NAME);
        
        //Simulate adding product to product cache
        //push 12 files into cache to total a size of 120 bytes
        String rrKeyPrefix = "rr";
        String rr1FileNameBase = "10bytes.txt";
        for(int i = 0; i < 12; i++){
            simulateAddFileToProductCache(rrKeyPrefix + i, rr1FileNameBase, i + rr1FileNameBase, cacheMap);
        }
        
        TimeUnit.SECONDS.sleep(1);
        
        //ensure all 12 10-byte files are cached
        for(int i = 0; i < 12; i++){
            verifyCached(cacheMap, rrKeyPrefix + i, i + rr1FileNameBase);
        }
        
        //push 1 large file into cache to total 245 bytes.  This file will be on the second "page" when querying the cache.
        String oneTwentyFiveBytesFileName = "125bytes.txt";
        int indexOf125Bytes = 12;
        simulateAddFileToProductCache(rrKeyPrefix + indexOf125Bytes, oneTwentyFiveBytesFileName, oneTwentyFiveBytesFileName, cacheMap);
        
        //ensure 125-byte file is cached
        verifyCached(cacheMap, rrKeyPrefix + indexOf125Bytes, oneTwentyFiveBytesFileName);
        
        TimeUnit.SECONDS.sleep(1);
        
        //entries from 0-9 should be removed from cache
        for(int i = 0; i < 12; i++){
            verifyRemovedFromCache(cacheMap, rrKeyPrefix + i, i + rr1FileNameBase);
        }
        
        verifyCached(cacheMap, rrKeyPrefix + indexOf125Bytes, oneTwentyFiveBytesFileName);
    }
    
    private HazelcastInstance initializeTestHazelcastInstance(long maxDirSizeBytes) {
        HazelcastInstance instance = hcInstanceFactory.newHazelcastInstance();
        
        IMap<Object, Object> cacheMap1 = instance.getMap(PRODUCT_CACHE_NAME);
        ProductCacheDirListener<Object, Object> listener = new ProductCacheDirListener<Object, Object>(maxDirSizeBytes);
        listener.setHazelcastInstance(instance);
        cacheMap1.addEntryListener(listener, true);
        return instance;
    }
    
    private void simulateAddFileToProductCache(String key, String fileName, String destFileName, IMap<String, ReliableResource> cacheMap) throws IOException{
        String productOriginalLocation = System.getProperty("user.dir") + "/src/test/resources/" + fileName;
        File rrCachedFile = new File(productCacheDir + "/"+ destFileName);
        FileUtils.copyFile(new File(productOriginalLocation), rrCachedFile);
        ReliableResource rr = new ReliableResource(key, rrCachedFile.getAbsolutePath(), new MimeType(), fileName);
        rr.setSize(rrCachedFile.length());
        LOGGER.debug("adding entry to cache: " + key);
        cacheMap.put(key, rr);
    }

    private void verifyCached(IMap<String, ReliableResource> cacheMap, String rrKey, String rrFileName) {
        ReliableResource rrFromCache = (ReliableResource)cacheMap.get(rrKey);
        assertNotNull(rrFromCache);
        File rrCachedFile = new File(productCacheDir + File.separator + rrFileName);
        assertTrue(new File(rrCachedFile.getAbsolutePath()).exists());
    }

    private void verifyRemovedFromCache(IMap<String, ReliableResource> cacheMap, String rrKey, String rrFileName) {
        ReliableResource rrFromCache = (ReliableResource)cacheMap.get(rrKey);
        assertNull(rrFromCache);
        File rrCachedFile = new File(productCacheDir + File.separator + rrFileName);
        assertFalse(new File(rrCachedFile.getAbsolutePath()).exists());
    }
    
    private void cleanProductCacheDirectory() throws IOException{
        FileUtils.cleanDirectory(new File(productCacheDir));
    }
}
