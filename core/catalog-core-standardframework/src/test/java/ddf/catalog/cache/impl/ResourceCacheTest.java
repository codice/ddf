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
package ddf.catalog.cache.impl;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import ddf.cache.CacheException;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.data.ReliableResource;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ResourceCacheTest {
    public String workingDir;

    public ResourceCache resourceCache;

    private HazelcastInstance instance;

    private IMap cache;

    private String defaultProductCacheDirectory;

    private static Logger logger = LoggerFactory.getLogger(ResourceCacheTest.class);

    private static final String TEST_PATH = "/src/main/resources/";

    @Before
    public void setUp() throws MalformedURLException {

        // Set system property that Hazelcast uses for its XML Config file
        String xmlConfigFilename = "reliableResource-hazelcast.xml";
        String xmlConfigLocation = System.getProperty("user.dir") + TEST_PATH
                + xmlConfigFilename;

        // Simulates how DDF script starts up setting KARAF_HOME
//        workingDir = System.getProperty("user.dir") + File.separator + "target";
        workingDir = System.getProperty("user.dir");
        System.setProperty("karaf.home", workingDir);

        defaultProductCacheDirectory = workingDir + File.separator
                + ResourceCache.DEFAULT_PRODUCT_CACHE_DIRECTORY;

        // Simulates how blueprint creates the ResourceCache instance
//        instance = mock(HazelcastInstance.class);
//        cache = mock(IMap.class);
//        when(instance.getMap(anyString())).thenReturn(cache);

        Bundle bundle = mock(Bundle.class);
        URL url = new URL("file:///" + new File(xmlConfigLocation).getAbsolutePath());
        when(bundle.getResource(anyString())).thenReturn(url);
        BundleContext context = mock(BundleContext.class);
        when(context.getBundle()).thenReturn(bundle);

        resourceCache = new ResourceCache();
        resourceCache.setContext(context);
        resourceCache.setXmlConfigFilename(xmlConfigFilename);
        resourceCache.setProductCacheDirectory("");
        resourceCache.setCache(null);
    }

    @After
    public void teardownTest() {
        try {
            FileUtils.cleanDirectory(new File(defaultProductCacheDirectory));
        } catch (IOException e) {
            logger.warn("unable to clean directory");
        }
    }

    @Test
    public void testBadKarafHomeValue() {
        System.setProperty("karaf.home", "invalid-cache");
        resourceCache.setProductCacheDirectory("");

        assertEquals("", resourceCache.getProductCacheDirectory());
    }

    @Test
    public void testDefaultProductCacheDirectory() {
        assertEquals(defaultProductCacheDirectory, resourceCache.getProductCacheDirectory());
    }

    @Test
    public void testUserDefinedProductCacheDirectory() {
        String expectedDir = workingDir + File.separator + "custom-product-cache";
        resourceCache.setProductCacheDirectory(expectedDir);

        assertEquals(expectedDir, resourceCache.getProductCacheDirectory());
    }

    @Test
    public void testContainsWithNullKey() {
        assertFalse(resourceCache.containsValid(null, new MetacardImpl()));
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
        String fileName = "15bytes.txt";
        String productLocation = System.getProperty("user.dir") + "/src/test/resources/" + fileName;
        File rrCachedFile = new File(productLocation);
        String key = "ddf-1-abc123";
        MetacardImpl metacard = new MetacardImpl();
        ReliableResource reliableResource = new ReliableResource(key, rrCachedFile.getAbsolutePath(),
                new MimeType(), fileName, metacard);

        // Return null when adding as pending entry; return resource when doing get(key)
//        when(cache.get(key)).thenReturn(null).thenReturn(reliableResource);

        resourceCache.addPendingCacheEntry(reliableResource);
        assertTrue(resourceCache.isPending(key));
        resourceCache.put(reliableResource);
        assertTrue(assertReliableResourceEquals(reliableResource, resourceCache.getValid(key, metacard)));
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
        String fileName = "15bytes.txt";
        String productLocation = System.getProperty("user.dir") + "/src/test/resources/" + fileName;
        File rrCachedFile = new File(productLocation);
        String key = "ddf-1-abc123";
        MetacardImpl metacard = new MetacardImpl();
        ReliableResource reliableResource = new ReliableResource(key, rrCachedFile.getAbsolutePath(),
                new MimeType(), fileName, metacard);

        resourceCache.put(reliableResource);
        assertFalse(resourceCache.isPending(key));
        assertTrue(assertReliableResourceEquals(reliableResource, resourceCache.getValid(key, metacard)));
    }

    @Test(expected = CacheException.class)
    public void testGetWhenNullKey() throws CacheException, MimeTypeParseException, IOException {
        resourceCache.getValid(null, new MetacardImpl());
    }

    @Test(expected = CacheException.class)
    public void testGetWhenNoProductInCache() throws CacheException, MimeTypeParseException, IOException {
        String key = "ddf-1-abc123";
        resourceCache.getValid(key, new MetacardImpl());
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
        MetacardImpl metacard = new MetacardImpl();

        resourceCache.getValid(key, metacard);
    }

    @Test
    public void testValidationEqualMetacards() throws URISyntaxException {
        MetacardImpl metacard = generateMetacard();
        MetacardImpl metacard1 = generateMetacard();

        ReliableResource cachedResource = new ReliableResource("key", "", null, null, metacard);
        assertTrue(resourceCache.validateCacheEntry(cachedResource, metacard1));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidationNullResource() throws URISyntaxException {
        resourceCache.validateCacheEntry(null, generateMetacard());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidationNullMetacard() throws URISyntaxException {
        MetacardImpl metacard = generateMetacard();
        ReliableResource cachedResource = new ReliableResource("key", "", null, null, metacard);
        resourceCache.validateCacheEntry(cachedResource, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testValidationNullParams() throws URISyntaxException {
        resourceCache.validateCacheEntry(null, null);
    }

    @Test
    public void testValidationNotEqual() throws URISyntaxException, IOException {
        MetacardImpl metacard = generateMetacard();
        MetacardImpl metacard1 = generateMetacard();
        metacard1.setId("differentId");

        String fileName = "10bytes.txt";
        simulateAddFileToCacheDir(fileName);
        String cachedResourceMetacardKey = "keyA1";
        String cachedResourceFilePath = defaultProductCacheDirectory + File.separator + fileName;
        File cachedResourceFile = new File(cachedResourceFilePath);
        assertTrue(cachedResourceFile.exists());

        ReliableResource cachedResource = new ReliableResource(cachedResourceMetacardKey, cachedResourceFilePath, null, null, metacard);
        resourceCache.validateCacheEntry(cachedResource, metacard1);
        assertFalse(cachedResourceFile.exists());
    }

    @Test
    public void testContainsTrueValid() throws URISyntaxException, CacheException {
        MetacardImpl cachedMetacard = generateMetacard();
        MetacardImpl latestMetacard = generateMetacard();

        String cacheKey = "cacheKey1";
        resourceCache.put(new ReliableResource(cacheKey, "", null, "name", cachedMetacard));
        assertTrue(resourceCache.containsValid(cacheKey, latestMetacard));
    }

    @Test
    public void testContainsFalseValid() throws URISyntaxException {
        MetacardImpl latestMetacard = generateMetacard();

        String key = "cacheKey1";
        assertFalse(resourceCache.containsValid(key, latestMetacard));
    }

    @Test
    public void testContainsNullLatestMetacard() throws URISyntaxException, CacheException {
        MetacardImpl cachedMetacard = generateMetacard();

        String cacheKey = "cacheKey1";
        resourceCache.put(new ReliableResource(cacheKey, "", null, "name", cachedMetacard));
        assertFalse(resourceCache.containsValid("cacheKey1", null));
    }

    @Test
    public void testContainsTrueInvalid() throws URISyntaxException, IOException, CacheException {
        MetacardImpl cachedMetacard = generateMetacard();
        MetacardImpl latestMetacard = generateMetacard();
        latestMetacard.setId("different-id");

        String fileName = "10bytes.txt";
        simulateAddFileToCacheDir(fileName);
        String cachedResourceMetacardKey = "keyA1";
        String cachedResourceFilePath = defaultProductCacheDirectory + File.separator + fileName;
        File cachedResourceFile = new File(cachedResourceFilePath);
        assertTrue(cachedResourceFile.exists());

        resourceCache.put(new ReliableResource(cachedResourceMetacardKey, cachedResourceFilePath, null, "name", cachedMetacard));
        assertFalse(resourceCache.containsValid(cachedResourceMetacardKey, latestMetacard));
        assertFalse(cachedResourceFile.exists());
    }

    @Test
    public void testContainsTrueInvalid2_CantFindFile() throws URISyntaxException, CacheException {
        MetacardImpl cachedMetacard = generateMetacard();
        cachedMetacard.setId("different-id");
        MetacardImpl latestMetacard = generateMetacard();

        String cacheKey = "cacheKey1";
        resourceCache.put(new ReliableResource(cacheKey, "", null, "name", cachedMetacard));
        assertFalse(resourceCache.containsValid(cacheKey, latestMetacard));
    }

    private void simulateAddFileToCacheDir(String fileName) throws IOException {
        String originalFilePath = System.getProperty("user.dir") + File.separator +
                "src" + File.separator + "test" + File.separator + "resources" + File.separator + fileName;
        String destinationFilePath = defaultProductCacheDirectory + File.separator + fileName;
        FileUtils.copyFile(new File(originalFilePath), new File(destinationFilePath));
    }

    private MetacardImpl generateMetacard() throws URISyntaxException {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId("source1");
        metacard.setId("id123");
        metacard.setContentTypeName("content-type-name");
        metacard.setContentTypeVersion("content-type-version");
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(1400673600);  //hardcode date to ensure it is the same time.
        metacard.setCreatedDate(c.getTime());
        metacard.setEffectiveDate(c.getTime());
        metacard.setExpirationDate(c.getTime());
        metacard.setModifiedDate(c.getTime());
        metacard.setLocation("POINT(0 10)");
        metacard.setMetadata("<metadata>abc</metadata>");
        metacard.setResourceSize("100");
        metacard.setResourceURI(new URI("https://github.com/codice"));
        metacard.setTitle("title");
        return metacard;
    }

    /*
     * Check the values of the ReliableResource objects since equals fails once they've been serialized and
     * reconstituted.
     */
    private boolean assertReliableResourceEquals(ReliableResource expected, Resource actual) {
        ReliableResource rrActual = null;
        boolean result = false;

        if (actual instanceof ReliableResource) {
            rrActual = (ReliableResource) actual;
            result = true;
        }

        if (result) {
            result = rrActual.getFilePath().equals(expected.getFilePath());

            if (result) {
                result = rrActual.getMimeTypeValue().equals((expected.getMimeTypeValue()));

                if (result) {
                    result = rrActual.getName().equals(expected.getName());

                    if (result) {
                        result = rrActual.getSize() == expected.getSize();

                        if (result) {
                            result = rrActual.getLastTouchedMillis() == expected.getLastTouchedMillis();
                        }
                    }
                }
            }
        }

        return result;
    }

}
