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

import ddf.catalog.data.Metacard;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resourceretriever.ResourceRetriever;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CachedResourceIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachedResourceIntegrationTest.class);

    public static String workingDir;
    public static String productCacheDirectory;
    public static String productInputFilename;
    public static long expectedFileSize;
    public static String expectedFileContents;
    public static final int maxRetryAttempts = 3;
    public static final int delayBetweenAttempts = 1;
    public static final int cachingMonitorPeriod = 5;

    public MockInputStream mis;

    public ResourceRequest resourceRequest;

    public ResourceResponse resourceResponse;

    public Resource resource;

    public ExecutorService executor;


    @BeforeClass
    public static void oneTimeSetup() throws IOException {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.DEBUG);

        workingDir = System.getProperty("user.dir");
        productCacheDirectory = workingDir + "/target/tests/product-cache";
        productInputFilename = workingDir + "/src/test/resources/foo_10_lines.txt";
        File productInputFile = new File(productInputFilename);
        expectedFileSize = productInputFile.length();
        expectedFileContents = FileUtils.readFileToString(productInputFile);
    }

    /**
     * Test that if an Exception is thrown while reading the product's InputStream that
     * caching is interrupted, retried and successfully completes on the second attempt.
     *
     * @throws Exception
     */
    @Test
    //@Ignore
    public void testStoreWithInputStreamRecoverableError() throws Exception {
        /*HUGH
        mis = new MockInputStream(productInputFilename);
        mis.setInvocationCountToThrowIOException(5);

        Metacard metacard = getMetacardStub("abc123", "ddf-1");
        resourceResponse = getResourceResponseStub();
        CacheKey keyMaker = new CacheKey(metacard, resourceResponse.getRequest());
        String key = keyMaker.generateKey();
        ResourceRetriever retriever = getResourceRetrieverStubWithRetryCapability();

        ArgumentCaptor<CachedResource> argument = ArgumentCaptor.forClass(CachedResource.class);
        ResourceCache resourceCache = mock(ResourceCache.class);

        CachedResource cachedResource = new CachedResource(productCacheDirectory, maxRetryAttempts,
                delayBetweenAttempts, cachingMonitorPeriod, getEventPublisher());
        int chunkSize = 50;
        cachedResource.setChunkSize(chunkSize);

        Resource newResource = cachedResource.store(key, resourceResponse, resourceCache,
                retriever, false);
        assertNotNull(newResource);
        ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, newResource.getInputStream());

        // Captures the CachedResource object that should have been put in the ResourceCache's map
        verify(resourceCache).put(argument.capture());

        verifyCaching(argument.getValue(), "ddf-1-abc123", clientBytesRead);

        // Cleanup
//        FileUtils.deleteDirectory(new File(productCacheDirectory));
        executor.shutdown();
        END HUGH*/
    }

    /**
     * Test storing product in cache and one of the chunks being stored takes too long, triggering
     * the CacheMonitor to interrupt the caching. Verify that caching is retried and successfully
     * completes on the second attempt.
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testStoreWithTimeoutException() throws Exception {
        /*HUGH
        mis = new MockInputStream(productInputFilename);

        Metacard metacard = getMetacardStub("abc123", "ddf-1");
        resourceResponse = getResourceResponseStub();
        CacheKey keyMaker = new CacheKey(metacard, resourceResponse.getRequest());
        String key = keyMaker.generateKey();
        ResourceRetriever retriever = getResourceRetrieverStubWithRetryCapability();

        ArgumentCaptor<CachedResource> argument = ArgumentCaptor.forClass(CachedResource.class);
        ResourceCache resourceCache = mock(ResourceCache.class);

        CachedResource cachedResource = new CachedResource(productCacheDirectory, maxRetryAttempts,
                delayBetweenAttempts, cachingMonitorPeriod, getEventPublisher());
        int chunkSize = 50;
        cachedResource.setChunkSize(chunkSize);
        cachedResource.setCachingMonitorInitialDelay(1);
        mis.setInvocationCountToTimeout(3);
        mis.setReadDelay(cachingMonitorPeriod * 2);

        Resource newResource = cachedResource.store(key, resourceResponse, resourceCache,
                retriever, false);
        assertNotNull(newResource);
        ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, newResource.getInputStream());

        // Captures the CachedResource object that should have been put in the ResourceCache's map
        verify(resourceCache).put(argument.capture());

        verifyCaching(argument.getValue(), "ddf-1-abc123", clientBytesRead);

        // Cleanup
//        FileUtils.deleteDirectory(new File(productCacheDirectory));
        executor.shutdown();
        END HUGH*/
    }

    /**
     * Tests that if user/client cancels a product retrieval that is in progress and
     * actively being cached, that the (partially) cached file is deleted and not
     * placed in the cache map.
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testClientCancelProductDownloadDuringCaching() throws Exception {
        /*HUGH
        mis = new MockInputStream(productInputFilename);

        Metacard metacard = getMetacardStub("abc123", "ddf-1");
        resourceResponse = getResourceResponseStub();
        CacheKey keyMaker = new CacheKey(metacard, resourceResponse.getRequest());
        String key = keyMaker.generateKey();

        ResourceRetriever retriever = mock(ResourceRetriever.class);
        when(retriever.retrieveResource()).thenReturn(resourceResponse);

        ArgumentCaptor<CachedResource> argument = ArgumentCaptor.forClass(CachedResource.class);
        ResourceCache resourceCache = mock(ResourceCache.class);

        CachedResource cachedResource = new CachedResource(productCacheDirectory, maxRetryAttempts,
                delayBetweenAttempts, cachingMonitorPeriod, getEventPublisher());
        int chunkSize = 50;
        cachedResource.setChunkSize(chunkSize);

        Resource newResource = cachedResource.store(key, resourceResponse, resourceCache,
                retriever, false);
        assertNotNull(newResource);

        // On third read of PipedInputStream, client will close the stream simulating a cancel
        // of the product download
        clientRead(chunkSize, newResource.getInputStream(), 3);

        //TODO: how to wait for caching to finish? Since client cancels download, the clientRead() returns
        // before caching completes and ends this unit test too soon
        LOGGER.info("Sleeping 3 seconds to see what happens with caching");
        Thread.sleep(3000);
        LOGGER.info("DONE");

        // Captures the CachedResource object that should have been put in the ResourceCache's map
        verify(resourceCache).put(argument.capture());

        verifyCaching(argument.getValue(), "ddf-1-abc123", null);

        // Cleanup

//      FileUtils.deleteDirectory(new File(productCacheDirectory));
        executor.shutdown();
        END HUGH*/
    }

    /**
     * Tests that when caching a product if an exception occurs on the OutputStream that the
     * product is being cached to that caching to the OutputStream is considered non-recoverable
     * and the product is just streamed to the client (no caching done to disk).
     * @throws Exception
     */
    @Test
    @Ignore
    public void testCachingRecoveryWhenExceptionFromCachedOutputStream() throws Exception {

    }

}
