/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.event.retrievestatus;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.hazelcast.test.TestHazelcastInstanceFactory;
import ddf.catalog.cache.impl.ResourceCacheImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.download.DownloadManagerState;
import ddf.catalog.resource.download.DownloadStatus;
import ddf.catalog.resource.download.ReliableResourceDownloadManager;
import ddf.catalog.resource.download.ReliableResourceDownloaderConfig;
import ddf.catalog.resource.impl.URLResourceReader;
import ddf.catalog.resourceretriever.LocalResourceRetriever;
import ddf.security.service.impl.SubjectUtils;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.catalog.resource.download.DownloadException;
import org.codice.ddf.cxf.client.ClientFactoryFactory;
import org.codice.ddf.cxf.client.impl.ClientFactoryFactoryImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DownloadsStatusEventListenerTest {

  private ReliableResourceDownloadManager testDownloadManager;

  private Path localResourcePath;

  private DownloadStatusInfoImpl testDownloadStatusInfo;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    testFolder.create();
    String productCacheDir = testFolder.newFolder("cache").toString();
    localResourcePath = testFolder.newFolder("resources").toPath();
    ReliableResourceDownloaderConfig downloaderConfig = new ReliableResourceDownloaderConfig();
    testDownloadStatusInfo = new DownloadStatusInfoImpl();
    testDownloadStatusInfo.setSubjectOperations(new SubjectUtils());
    TestHazelcastInstanceFactory hcInstanceFactory = new TestHazelcastInstanceFactory(10);
    ResourceCacheImpl testResourceCache = new ResourceCacheImpl(productCacheDir);
    testResourceCache.setCache(hcInstanceFactory.newHazelcastInstance());
    DownloadsStatusEventPublisher testEventPublisher = mock(DownloadsStatusEventPublisher.class);
    DownloadsStatusEventListener testEventListener = new DownloadsStatusEventListener();
    downloaderConfig.setResourceCache(testResourceCache);
    downloaderConfig.setEventPublisher(testEventPublisher);
    downloaderConfig.setEventListener(testEventListener);
    testDownloadManager =
        new ReliableResourceDownloadManager(
            downloaderConfig, testDownloadStatusInfo, Executors.newSingleThreadExecutor());
    testDownloadManager.setMaxRetryAttempts(1);
    testDownloadManager.setDelayBetweenAttempts(0);
    testDownloadManager.setMonitorPeriod(5);
  }

  @Test
  public void testGetDownloadStatus()
      throws URISyntaxException, DownloadException, InterruptedException, IOException {
    File downloadSrcFile = new File(this.getClass().getResource("/125bytes.txt").toURI());
    File downloadFile = prepareDownloadFile(downloadSrcFile);
    MetacardImpl testMetacard = new MetacardImpl();
    testMetacard.setId("easyas123");
    testMetacard.setResourceURI(downloadFile.toURI());
    testMetacard.setResourceSize("125");
    ClientFactoryFactory clientFactoryFactory = new ClientFactoryFactoryImpl();
    URLResourceReader testURLResourceReader = new URLResourceReader(clientFactoryFactory);
    testURLResourceReader.setRootResourceDirectories(
        new HashSet<String>(Arrays.asList(localResourcePath.toString())));
    List<ResourceReader> testResourceReaderList =
        Collections.singletonList((ResourceReader) testURLResourceReader);
    Map<String, Serializable> tmpMap = Collections.emptyMap();
    Map<String, Integer> idToBytes = new HashMap<String, Integer>();
    testGetDownloadStatusHelper(null, null, null);

    testDownloadManager.download(
        mock(ResourceRequest.class),
        testMetacard,
        new LocalResourceRetriever(
            testResourceReaderList, testMetacard.getResourceURI(), null, tmpMap));

    TimeUnit.SECONDS.sleep(2);
    testGetDownloadStatusHelper(
        idToBytes, DownloadManagerState.DownloadState.COMPLETED.name(), downloadFile.getName());
  }

  private void testGetDownloadStatusHelper(
      Map<String, Integer> idToBytes, String status, String fileName) {
    List<String> allDownloads = testDownloadStatusInfo.getAllDownloads();
    for (String item : allDownloads) {
      Map<String, String> downloadStatus = testDownloadStatusInfo.getDownloadStatus(item);
      if (idToBytes.get(item) == null) {
        idToBytes.put(item, 0);
      }
      assertTrue(
          idToBytes.get(item)
              <= Integer.parseInt(downloadStatus.get(DownloadStatus.BYTES_DOWNLOADED_KEY)));
      assertTrue(downloadStatus.get(DownloadStatus.STATUS_KEY).equals(status));
      assertTrue(downloadStatus.get(DownloadStatus.FILE_NAME_KEY).equals(fileName));
      idToBytes.put(
          item, Integer.parseInt(downloadStatus.get(DownloadStatus.BYTES_DOWNLOADED_KEY)));
    }
  }

  private File prepareDownloadFile(File sourceFile) throws IOException {
    Path downloadFilePath = Paths.get(localResourcePath.toString(), sourceFile.getName());
    Files.copy(sourceFile.toPath(), downloadFilePath);
    return downloadFilePath.toFile();
  }
}
