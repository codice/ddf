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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import javax.activation.MimeTypeParseException;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ddf.cache.Cache;
import ddf.cache.CacheException;
import ddf.cache.CacheManager;
import ddf.catalog.resource.download.ReliableResource;

public class ResourceCacheTest {

    private static final transient Logger LOGGER = Logger.getLogger(ResourceCacheTest.class);

    public String workingDir;

    public ResourceCache resourceCache;

    public Cache cache;

    @BeforeClass
    public static void oneTimeSetup() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }

    @Before
    public void setUp() {

        // Simulates how DDF script starts up setting KARAF_HOME
        workingDir = System.getProperty("user.dir") + File.separator + "target";
        System.setProperty("karaf.home", workingDir);

        // Simulates how blueprint creates the ResourceCache instance
        cache = mock(Cache.class);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        resourceCache = new ResourceCache();
        resourceCache.setCacheManager(cacheManager);
        resourceCache.setProductCacheDirectory("");
    }

    @Test
    public void testBadKarafHomeValue() {
        System.setProperty("karaf.home", "invalid-cache");
        resourceCache.setProductCacheDirectory("");

        assertEquals("", resourceCache.getProductCacheDirectory());
    }

    @Test
    public void testDefaultProductCacheDirectory() {
        String expectedDir = workingDir + File.separator
                + ResourceCache.DEFAULT_PRODUCT_CACHE_DIRECTORY;
        assertEquals(expectedDir, resourceCache.getProductCacheDirectory());
    }

    @Test
    public void testUserDefinedProductCacheDirectory() {
        String expectedDir = workingDir + File.separator + "custom-product-cache";
        resourceCache.setProductCacheDirectory(expectedDir);

        assertEquals(expectedDir, resourceCache.getProductCacheDirectory());
    }
    
    @Test
    public void testContainsWithNullKey() {
        assertFalse(resourceCache.contains(null));
    }

    /**
     * Verifies that put() method works.
     * 
     * @throws CacheException
     * @throws MimeTypeParseException
     * @throws IOException
     */
    @Test
    public void testPutThenGet() throws CacheException, MimeTypeParseException, IOException {
        ReliableResource reliableResource = mock(ReliableResource.class);
        String key = "ddf-1-abc123";
        // Return null when adding as pending entry; return resource when doing get(key)
        when(cache.get(key)).thenReturn(null).thenReturn(reliableResource);
        when(reliableResource.getKey()).thenReturn(key);
        when(reliableResource.hasProduct()).thenReturn(true);
        
        resourceCache.addPendingCacheEntry(key);
        assertTrue(resourceCache.isPending(key));
        resourceCache.put(reliableResource);
        assertTrue(reliableResource == resourceCache.get(key));
        assertFalse(resourceCache.isPending(key));
    }
    
    /**
     * Verifies that put() method works even if entry being added was never
     * in the pending cache list.
     * 
     * @throws CacheException
     * @throws MimeTypeParseException
     * @throws IOException
     */
    @Test
    public void testPutThenGetNotPending() throws CacheException, MimeTypeParseException, IOException {
        ReliableResource reliableResource = mock(ReliableResource.class);
        String key = "ddf-1-abc123";
        when(cache.get(key)).thenReturn(reliableResource);
        when(reliableResource.getKey()).thenReturn(key);
        when(reliableResource.hasProduct()).thenReturn(true);
        
        resourceCache.put(reliableResource);
        assertFalse(resourceCache.isPending(key));
        assertTrue(reliableResource == resourceCache.get(key));
    }

    @Test(expected = CacheException.class)
    public void testGetWhenNullKey() throws CacheException, MimeTypeParseException, IOException {
        resourceCache.get(null);
    }

    @Test(expected = CacheException.class)
    public void testGetWhenNoProductInCache() throws CacheException, MimeTypeParseException, IOException {
        String key = "ddf-1-abc123";
        when(cache.get(key)).thenReturn(null);
        resourceCache.get(key);
    }

    /**
     * Verifies that if there is a entry in the cache but no associated product file in the cache directory,
     * when a get() is done on that cached entry that the missing product is detected, the cache entry is removed,
     * and a CacheException is thrown.
     * 
     * @throws CacheException
     * @throws MimeTypeParseException
     * @throws IOException
     */
    @Test(expected = CacheException.class)
    public void testGetWhenNoProductInCacheDirectory() throws CacheException, MimeTypeParseException, IOException {
        ReliableResource reliableResource = mock(ReliableResource.class);
        String key = "ddf-1-abc123";
        when(cache.get(key)).thenReturn(reliableResource);
        when(reliableResource.hasProduct()).thenReturn(false);

        resourceCache.get(key);
        // Verifies that the remove(key) method is called on the Cache object
        verify(cache).remove(key);
    }

}
