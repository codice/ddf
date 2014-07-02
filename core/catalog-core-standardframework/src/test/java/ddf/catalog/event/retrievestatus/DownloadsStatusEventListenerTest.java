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
package ddf.catalog.event.retrievestatus;


import com.hazelcast.test.TestHazelcastInstanceFactory;
import ddf.catalog.cache.impl.ResourceCache;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.download.DownloadManagerState;
import ddf.catalog.resource.impl.URLResourceReader;
import ddf.catalog.resource.download.DownloadException;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.catalog.resourceretriever.LocalResourceRetriever;
import org.junit.Test;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class DownloadsStatusEventListenerTest {

    private static ReliableResourceDownloadManager testDownloadManager;
    private static TestHazelcastInstanceFactory hcInstanceFactory;
    private static String productCacheDir;
    private static DownloadsStatusEventListener testEventListener;
    private static DownloadStatusInfoImpl testDownloadStatusInfo;
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadsStatusEventListenerTest.class);


    @Test
    public void testGetDownloadStatus() throws URISyntaxException, DownloadException, InterruptedException {
        File downloadFile = new File(System.getProperty("user.dir") + "/src/test/resources/125bytes.txt");
        MetacardImpl testMetacard = new MetacardImpl();
        testMetacard.setId("easyas123");
        testMetacard.setResourceURI(downloadFile.toURI());
        testMetacard.setResourceSize("125");
        testMetacard.setType(BasicTypes.BASIC_METACARD);
        List<ResourceReader> testResourceReaderList = Collections.singletonList((ResourceReader)new URLResourceReader());
        Map<String, Serializable> tmpMap = Collections.emptyMap();
        Map<String, Integer> idToBytes = new HashMap<String, Integer>();
        testGetDownloadStatusHelper(null, null, null);

        testDownloadManager.download(mock(ResourceRequest.class), testMetacard, new LocalResourceRetriever(testResourceReaderList,
                testMetacard.getResourceURI(), tmpMap));


        testGetDownloadStatusHelper(idToBytes, DownloadManagerState.DownloadState.IN_PROGRESS.name(), downloadFile.getName());
        TimeUnit.SECONDS.sleep(2);
        testGetDownloadStatusHelper(idToBytes, DownloadManagerState.DownloadState.COMPLETED.name(), downloadFile.getName());

    }

    @BeforeClass
    public static void setUp() {

        testDownloadStatusInfo = new DownloadStatusInfoImpl();
        hcInstanceFactory = new TestHazelcastInstanceFactory(10);
        ResourceCache testResourceCache = new ResourceCache();
        testResourceCache.setCache(hcInstanceFactory.newHazelcastInstance());
        productCacheDir = System.getProperty("user.dir") +"/target" + File.separator + ResourceCache.DEFAULT_PRODUCT_CACHE_DIRECTORY;
        testResourceCache.setProductCacheDirectory(productCacheDir);
        DownloadsStatusEventPublisher testEventPublisher = mock(DownloadsStatusEventPublisher.class);
        testEventListener = new DownloadsStatusEventListener();
        testDownloadManager = new ReliableResourceDownloadManager(1, 0, 5000, true, testResourceCache, false,
                testEventPublisher, testEventListener, testDownloadStatusInfo);


    }

    private void testGetDownloadStatusHelper(Map<String, Integer> idToBytes, String status, String fileName) {
        ArrayList<String> allDownloads = testDownloadStatusInfo.getAllDownloads();
        LOGGER.debug(allDownloads.toString());
        for (String item : allDownloads) {
            Map<String, String> downloadInfo = testDownloadStatusInfo.getDownloadStatus(item);
            for (Map.Entry<String, String> downloadItem : downloadInfo.entrySet()) {
                LOGGER.debug(downloadItem.getKey() +": " +downloadItem.getValue());
            }
            if (idToBytes.get(item) == null) {
                idToBytes.put(item, 0);
            }
            LOGGER.debug(downloadInfo.get("bytesDownloaded"));
            assertTrue(idToBytes.get(item) <= Integer.parseInt(downloadInfo.get("bytesDownloaded")));
            assertTrue(status.equals(downloadInfo.get("status")));
            assertTrue(fileName.equals(downloadInfo.get("fileName")));
            idToBytes.put(item, Integer.parseInt(downloadInfo.get("bytesDownloaded")));
        }
    }
}
