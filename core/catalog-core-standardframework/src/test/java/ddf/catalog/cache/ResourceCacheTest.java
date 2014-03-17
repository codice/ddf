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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.commons.io.IOUtils;
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
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resourceretriever.ResourceRetriever;

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
        workingDir = System.getProperty("user.dir") + File.separator + "target";
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
        ResourceRetriever retriever = mock(ResourceRetriever.class);
        productCache.put(null, resourceResponse, retriever);
    }

    @Test(expected = CacheException.class)
    public void testPutWithNullResourceResponse() throws CacheException {
        Metacard metacard = mock(Metacard.class);
        ResourceRetriever retriever = mock(ResourceRetriever.class);
        productCache.put(metacard, null, retriever);
    }

    @Test(expected = CacheException.class)
    public void testPutWithNullResourceRetriever() throws CacheException {
        Metacard metacard = mock(Metacard.class);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        productCache.put(metacard, resourceResponse, null);
    }

    @Test(expected = CacheException.class)
    public void testPutWithEmptyMetacardId() throws CacheException {
        Metacard metacard = mock(Metacard.class);
        String metacardId = "";
        when(metacard.getId()).thenReturn(metacardId);
        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        ResourceRetriever retriever = mock(ResourceRetriever.class);

        productCache.put(metacard, resourceResponse, retriever);
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
        
        ResourceRetriever retriever = mock(ResourceRetriever.class);

        ResourceResponse newResourceResponse = productCache.put(metacard, resourceResponse,
                retriever);

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
        
        ResourceRetriever retriever = mock(ResourceRetriever.class);

        productCache.put(metacard, resourceResponse, retriever);

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
        
        ResourceRetriever retriever = mock(ResourceRetriever.class);

        String key = null;
        try {
            productCache.put(metacard, resourceResponse, retriever);

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
        
        ResourceRetriever retriever = mock(ResourceRetriever.class);

        String key = null;
        try {
            productCache.put(metacard, resourceResponse, retriever);

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
    
    /**
     * Tests that if a product is actively being cached and a new (second) request comes in
     * for the same product that the first request's caching continues and the second request
     * just retrieves the product from the Source and returns it to the client without attempting
     * to cache it.
     * 
     * @throws Exception
     */
    @Test
    @Ignore
    public void testPutWhenProductIsInProcessOfBeingCached() throws Exception {
        Metacard metacard = mock(Metacard.class);
        String sourceId = "ddf-1";
        String metacardId = "4567890";
        when(metacard.getId()).thenReturn(metacardId);
        when(metacard.getSourceId()).thenReturn(sourceId);

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
        
        ResourceRetriever retriever = mock(ResourceRetriever.class);

        ResourceResponse newResourceResponse = productCache.put(metacard, resourceResponse,
                retriever);

        int chunkSize = 50;
        CacheClient cacheClient1 = new CacheClient(newResourceResponse.getResource().getInputStream(), chunkSize);
        
        newResourceResponse = productCache.put(metacard, resourceResponse,
                retriever);
        CacheClient cacheClient2 = new CacheClient(newResourceResponse.getResource().getInputStream(), chunkSize);

        ExecutorService executor = Executors.newCachedThreadPool();
        
        executor.submit(cacheClient1);       
        executor.submit(cacheClient2);
        
        LOGGER.info("Sleeping 3 seconds to see what happens with caching");
        Thread.sleep(3000);
        LOGGER.info("DONE");

    }

}
