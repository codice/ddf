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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.cache.Cache;
import ddf.cache.CacheException;
import ddf.cache.CacheManager;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;

public class ResourceCacheTest {

    private static final transient Logger LOGGER = Logger.getLogger(ResourceCacheTest.class);

    public String workingDir;

    public ResourceCache productCache;

    public Cache cache;

    @BeforeClass
    public static void oneTimeSetup() {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.DEBUG);
    }
    
    @Before
    public void setUp() {

        // Simulates how DDF script starts up setting KARAF_HOME
        workingDir = System.getProperty("user.dir");
        System.setProperty("karaf.home", workingDir);

        // Simulates how blueprint creates the ResourceCache instance
        cache = mock(Cache.class);
        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        productCache = new ResourceCache();
        productCache.setCacheManager(cacheManager);
        productCache.setProductCacheDirectory("");
    }

    @Test
    public void testBadKarafHomeValue() {
        System.setProperty("karaf.home", "invalid-cache");
        productCache.setProductCacheDirectory("");

        assertEquals("", productCache.getProductCacheDirectory());
    }

    @Test
    public void testDefaultProductCacheDirectory() {
        String expectedDir = workingDir + File.separator
                + ResourceCache.DEFAULT_PRODUCT_CACHE_DIRECTORY;
        assertEquals(expectedDir, productCache.getProductCacheDirectory());
    }

    @Test
    public void testUserDefinedProductCacheDirectory() {
        String expectedDir = workingDir + File.separator + "custom-product-cache";
        productCache.setProductCacheDirectory(expectedDir);

        assertEquals(expectedDir, productCache.getProductCacheDirectory());
    }

    @Test(expected = CacheException.class)
    public void testPutWithNullMetacard() throws CacheException {
        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        productCache.put(null, resourceResponse);
    }

    @Test(expected = CacheException.class)
    public void testPutWithNullResourceResponse() throws CacheException {
        Metacard metacard = mock(Metacard.class);
        productCache.put(metacard, null);
    }

    @Test(expected = CacheException.class)
    public void testPutWithEmptyMetacardId() throws CacheException {
        Metacard metacard = mock(Metacard.class);
        String metacardId = "";
        when(metacard.getId()).thenReturn(metacardId);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);

        productCache.put(metacard, resourceResponse);
    }

    @Test
    public void testPut() throws CacheException, MimeTypeParseException, IOException {
        Metacard metacard = mock(Metacard.class);
        String metacardId = "4567890";
        when(metacard.getId()).thenReturn(metacardId);

        ResourceRequest resourceRequest = mock(ResourceRequest.class);

        Resource resource = mock(Resource.class);
        String input = "<myXml></myXml>";
        when(resource.getInputStream()).thenReturn(IOUtils.toInputStream(input));
        when(resource.getName()).thenReturn("test-resource");
        when(resource.getMimeType()).thenReturn(new MimeType("text/xml"));

        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);
        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getProperties()).thenReturn(null);

        ResourceResponse newResourceResponse = productCache.put(metacard, resourceResponse);

        assertNotNull(newResourceResponse);
        Resource newResource = newResourceResponse.getResource();
        assertNotNull(newResource);
        InputStream is = newResource.getInputStream();
        assertNotNull(is);
        assertEquals(input, IOUtils.toString(is));
    }

    @Test
    public void testGet() throws MimeTypeParseException, CacheException, IOException {
        Metacard metacard = mock(Metacard.class);
        String metacardId = "4567890";
        when(metacard.getId()).thenReturn(metacardId);
        String metacardSourceId = "ddf.123";
        when(metacard.getSourceId()).thenReturn(metacardSourceId);

        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();
        when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());

        Resource resource = mock(Resource.class);
        String input = "<myXml></myXml>";
        when(resource.getInputStream()).thenReturn(IOUtils.toInputStream(input));
        when(resource.getName()).thenReturn("test-resource");
        MimeType mimeType = new MimeType("text/xml");
        when(resource.getMimeType()).thenReturn(mimeType);

        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);
        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getProperties()).thenReturn(requestProperties);

        productCache.put(metacard, resourceResponse);

        String mockCacheKey = metacardSourceId + "-" + metacardId;
        CachedResource cachedResource = mock(CachedResource.class);
        when(cachedResource.hasProduct()).thenReturn(true);
        when(cache.get(mockCacheKey)).thenReturn(cachedResource);

        String key = new CacheKey(metacard, resourceRequest).generateKey();
        assertTrue(productCache.contains(key));
        Resource retrievedResource = productCache.get(key);
        assertNotNull(retrievedResource);
    }

    @Test
    public void testGetWhenNoProductInCache() throws MimeTypeParseException, IOException {
        Metacard metacard = mock(Metacard.class);
        String metacardId = "4567890";
        when(metacard.getId()).thenReturn(metacardId);
        String metacardSourceId = "ddf.123";
        when(metacard.getSourceId()).thenReturn(metacardSourceId);

        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();
        when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());

        Resource resource = mock(Resource.class);
        String input = "<myXml></myXml>";
        when(resource.getInputStream()).thenReturn(IOUtils.toInputStream(input));
        when(resource.getName()).thenReturn("test-resource");
        MimeType mimeType = new MimeType("text/xml");
        when(resource.getMimeType()).thenReturn(mimeType);

        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);
        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getProperties()).thenReturn(requestProperties);

        String key = null;
        try {
            productCache.put(metacard, resourceResponse);

            String mockCacheKey = metacardSourceId + "-" + metacardId;
            CachedResource cachedResource = mock(CachedResource.class);
            when(cachedResource.hasProduct()).thenReturn(false);
            when(cache.get(mockCacheKey)).thenReturn(cachedResource);

            key = new CacheKey(metacard, resourceRequest).generateKey();
            assertTrue(productCache.contains(key));
            productCache.get(key);
        } catch (CacheException e) {
            LOGGER.debug("Caught CacheException: " + e.getMessage());
        }

        // Verify that Cache class' remove method is called once because the
        // cache had the key in it but the product did not exist on the file
        // system in the product cache directory.
        try {
            verify(cache).remove(key);
        } catch (CacheException e) {
            LOGGER.debug("Caught CacheException: " + e.getMessage());
        }
    }

    @Test
    public void testGetWhenNoProductInCacheDirectory() throws MimeTypeParseException, IOException {
        Metacard metacard = mock(Metacard.class);
        String metacardId = "4567890";
        when(metacard.getId()).thenReturn(metacardId);
        String metacardSourceId = "ddf.123";
        when(metacard.getSourceId()).thenReturn(metacardSourceId);

        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();
        when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());

        Resource resource = mock(Resource.class);
        String input = "<myXml></myXml>";
        when(resource.getInputStream()).thenReturn(IOUtils.toInputStream(input));
        when(resource.getName()).thenReturn("test-resource");
        MimeType mimeType = new MimeType("text/xml");
        when(resource.getMimeType()).thenReturn(mimeType);

        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);
        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getProperties()).thenReturn(requestProperties);

        String key = null;
        try {
            productCache.put(metacard, resourceResponse);

            String mockCacheKey = metacardSourceId + "-" + metacardId;
            CachedResource cachedResource = mock(CachedResource.class);
            when(cachedResource.hasProduct()).thenReturn(false);
            when(cache.get(mockCacheKey)).thenReturn(cachedResource);

            key = new CacheKey(metacard, resourceRequest).generateKey();
            assertTrue(productCache.contains(key));
            productCache.get(key);
        } catch (CacheException e) {
            LOGGER.debug("Caught CacheException: " + e.getMessage());
        }

        // Verify that Cache class' remove method is called once because the
        // cache had the key in it but the product did not exist on the file
        // system in the product cache directory.
        try {
            verify(cache).remove(key);
        } catch (CacheException e) {
            LOGGER.debug("Caught CacheException: " + e.getMessage());
        }
    }

}
