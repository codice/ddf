/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.event.retrievestatus;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet; 
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.test.TestHazelcastInstanceFactory;

import ddf.catalog.cache.impl.ResourceCache;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.download.DownloadException;
import ddf.catalog.resource.download.DownloadManagerState;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.catalog.resource.impl.URLResourceReader;
import ddf.catalog.resourceretriever.LocalResourceRetriever;

public class DownloadsStatusEventListenerTest {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(DownloadsStatusEventListenerTest.class);

    private static ReliableResourceDownloadManager testDownloadManager;

    private static TestHazelcastInstanceFactory hcInstanceFactory;

    private static String productCacheDir;

    private static DownloadsStatusEventListener testEventListener;

    private static DownloadStatusInfoImpl testDownloadStatusInfo;

    @BeforeClass
    public static void setUp() {

        testDownloadStatusInfo = new DownloadStatusInfoImpl();
        hcInstanceFactory = new TestHazelcastInstanceFactory(10);
        ResourceCache testResourceCache = new ResourceCache();
        testResourceCache.setCache(hcInstanceFactory.newHazelcastInstance());
        productCacheDir = System.getProperty("user.dir") + "/target" + File.separator
                + ResourceCache.DEFAULT_PRODUCT_CACHE_DIRECTORY;
        testResourceCache.setProductCacheDirectory(productCacheDir);
        DownloadsStatusEventPublisher testEventPublisher = mock(
                DownloadsStatusEventPublisher.class);
        testEventListener = new DownloadsStatusEventListener();
        testDownloadManager = new ReliableResourceDownloadManager(Executors.newSingleThreadExecutor(), testResourceCache,
                testEventPublisher, testEventListener, testDownloadStatusInfo);
        testDownloadManager.setMaxRetryAttempts(1);
        testDownloadManager.setDelayBetweenAttempts(0);
        testDownloadManager.setMonitorPeriod(5);

    }

    @Test
    public void testGetDownloadStatus()
            throws URISyntaxException, DownloadException, InterruptedException {
        File downloadFile = new File(
                System.getProperty("user.dir") + "/src/test/resources/125bytes.txt");
        MetacardImpl testMetacard = new MetacardImpl();
        testMetacard.setId("easyas123");
        testMetacard.setResourceURI(downloadFile.toURI());
        testMetacard.setResourceSize("125");
        testMetacard.setType(BasicTypes.BASIC_METACARD);
        URLResourceReader testURLResourceReader = new URLResourceReader();
        testURLResourceReader.setRootResourceDirectories(new HashSet<String>(Arrays.asList(new String[] {System.getProperty("user.dir")})));
        List<ResourceReader> testResourceReaderList = Collections
                .singletonList((ResourceReader) testURLResourceReader);
        Map<String, Serializable> tmpMap = Collections.emptyMap();
        Map<String, Integer> idToBytes = new HashMap<String, Integer>();
        testGetDownloadStatusHelper(null, null, null);

        testDownloadManager.download(mock(ResourceRequest.class), testMetacard,
                new LocalResourceRetriever(testResourceReaderList, testMetacard.getResourceURI(),
                        tmpMap));

        TimeUnit.SECONDS.sleep(2);
        testGetDownloadStatusHelper(idToBytes, DownloadManagerState.DownloadState.COMPLETED.name(),
                downloadFile.getName());

    }

    private void testGetDownloadStatusHelper(Map<String, Integer> idToBytes, String status,
            String fileName) {
        List<String> allDownloads = testDownloadStatusInfo.getAllDownloads();
        LOGGER.debug(allDownloads.toString());
        for (String item : allDownloads) {
            Map<String, String> downloadInfo = testDownloadStatusInfo.getDownloadStatus(item);
            for (Map.Entry<String, String> downloadItem : downloadInfo.entrySet()) {
                LOGGER.debug(downloadItem.getKey() + ": " + downloadItem.getValue());
            }
            if (idToBytes.get(item) == null) {
                idToBytes.put(item, 0);
            }
            LOGGER.debug(downloadInfo.get("bytesDownloaded"));
            assertTrue(
                    idToBytes.get(item) <= Integer.parseInt(downloadInfo.get("bytesDownloaded")));
            System.out.println(downloadInfo.get("status"));
            assertTrue(status.equals(downloadInfo.get("status")));
            assertTrue(fileName.equals(downloadInfo.get("fileName")));
            idToBytes.put(item, Integer.parseInt(downloadInfo.get("bytesDownloaded")));
        }
    }
}
