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
package ddf.catalog.resource.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.cache.MockInputStream;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.cache.impl.ResourceCacheImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.event.retrievestatus.DownloadStatusInfo;
import ddf.catalog.event.retrievestatus.DownloadStatusInfoImpl;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventListener;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.data.ReliableResource;
import ddf.catalog.resource.download.DownloadManagerState.DownloadState;
import ddf.catalog.resourceretriever.ResourceRetriever;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.resource.download.DownloadException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReliableResourceDownloadManagerTest {
  public static final int MONITOR_PERIOD = 5;

  public static final String EXPECTED_METACARD_ID = "abc123";

  public static final String EXPECTED_METACARD_SOURCE_ID = "ddf-1";

  public static final String EXPECTED_CACHE_KEY =
      EXPECTED_METACARD_SOURCE_ID + "-" + EXPECTED_METACARD_ID;

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ReliableResourceDownloadManagerTest.class);

  private static String productCacheDirectory;

  private static String productInputFilename;

  private static long expectedFileSize;

  private static String expectedFileContents;

  private static final String DOWNLOAD_ID_KEY = "downloadId";

  private static final String FILE_NAME_KEY = "fileName";

  private static final String BYTES_DOWNLOADED_KEY = "bytesDownloaded";

  private static final String PERCENT_KEY = "percent";

  private static final String USER_KEY = "user";

  private static final String STATUS_KEY = "status";

  @Rule
  public MethodRule watchman =
      new TestWatchman() {
        public void starting(FrameworkMethod method) {
          LOGGER.debug(
              "***************************  STARTING: {}  **************************\n",
              method.getName());
        }

        public void finished(FrameworkMethod method) {
          LOGGER.debug(
              "***************************  END: {}  **************************\n",
              method.getName());
        }
      };

  private ResourceCacheImpl resourceCache;

  private DownloadsStatusEventPublisher eventPublisher;

  private DownloadsStatusEventListener eventListener;

  private ReliableResourceDownloadManager downloadMgr;

  private ResourceRequest resourceRequest;

  private ResourceResponse resourceResponse;

  private Resource resource;

  private MockInputStream mis;

  private InputStream productInputStream;

  private ExecutorService executor;

  private Future<ByteArrayOutputStream> future;

  private DownloadStatusInfo downloadStatusInfo;

  @BeforeClass
  public static void oneTimeSetup() throws IOException {
    String workingDir = System.getProperty("user.dir");
    productCacheDirectory = workingDir + "/target/tests/product-cache";
    productInputFilename = workingDir + "/src/test/resources/foo_10_lines.txt";
    File productInputFile = new File(productInputFilename);
    expectedFileSize = productInputFile.length();
    expectedFileContents = FileUtils.readFileToString(productInputFile);
  }

  @Before
  public void setup() {
    resourceCache = mock(ResourceCacheImpl.class);
    when(resourceCache.getProductCacheDirectory()).thenReturn(productCacheDirectory);
    eventPublisher = mock(DownloadsStatusEventPublisher.class);
    eventListener = mock(DownloadsStatusEventListener.class);
    downloadStatusInfo = new DownloadStatusInfoImpl();

    downloadMgr =
        new ReliableResourceDownloadManager(
            getDownloaderConfig(), downloadStatusInfo, Executors.newSingleThreadExecutor());
  }

  @Test(expected = DownloadException.class)
  public void testDownloadWithNullMetacard() throws Exception {
    resourceRequest = mock(ResourceRequest.class);
    ResourceRetriever retriever = mock(ResourceRetriever.class);

    downloadMgr.download(resourceRequest, null, retriever);
  }

  @Test(expected = DownloadException.class)
  public void testDownloadWithEmptyMetacardId() throws Exception {
    Metacard metacard = getMockMetacard("", EXPECTED_METACARD_SOURCE_ID);
    resourceRequest = mock(ResourceRequest.class);
    ResourceRetriever retriever = mock(ResourceRetriever.class);

    downloadMgr.download(resourceRequest, metacard, retriever);
  }

  @Test(expected = DownloadException.class)
  public void testDownloadWithNullResourceRetriever() throws Exception {
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceRequest = mock(ResourceRequest.class);

    downloadMgr.download(resourceRequest, metacard, null);
  }

  @Test(expected = DownloadException.class)
  public void testDownloadWithNullResourceRequest() throws Exception {
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    ResourceRetriever retriever = mock(ResourceRetriever.class);

    downloadMgr.download(null, metacard, retriever);
  }

  @Test(expected = DownloadException.class)
  public void testDownloadResourceNotFound() throws Exception {
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceRequest = mock(ResourceRequest.class);
    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource()).thenThrow(new ResourceNotFoundException());

    downloadMgr.download(resourceRequest, metacard, retriever);
  }

  @Test(expected = DownloadException.class)
  public void testDownloadResourceNotSupported() throws Exception {
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceRequest = mock(ResourceRequest.class);
    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource()).thenThrow(new ResourceNotSupportedException());

    downloadMgr.download(resourceRequest, metacard, retriever);
  }

  @Test(expected = DownloadException.class)
  public void testDownloadIOException() throws Exception {
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceRequest = mock(ResourceRequest.class);
    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource()).thenThrow(new IOException());

    downloadMgr.download(resourceRequest, metacard, retriever);
  }

  @Test
  // @Ignore
  public void testDownloadWithoutCaching() throws Exception {
    mis = new MockInputStream(productInputFilename);
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource()).thenReturn(resourceResponse);

    int chunkSize = 50;
    downloadMgr.setChunkSize(chunkSize);

    ResourceResponse newResourceResponse =
        downloadMgr.download(resourceRequest, metacard, retriever);
    assertThat(newResourceResponse, is(notNullValue()));
    productInputStream = newResourceResponse.getResource().getInputStream();
    assertThat(productInputStream, is(instanceOf(ReliableResourceInputStream.class)));

    ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, productInputStream);

    verifyClientBytesRead(clientBytesRead);

    cleanup();
  }

  @Test
  // @Ignore
  public void testDownloadWithCaching() throws Exception {
    mis = new MockInputStream(productInputFilename);
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource()).thenReturn(resourceResponse);

    CacheKey cacheKey = new CacheKey(metacard, resourceResponse.getRequest());
    String key = cacheKey.generateKey();
    when(resourceCache.isPending(key)).thenReturn(false);

    int chunkSize = 50;
    startDownload(true, chunkSize, false, metacard, retriever);

    ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, productInputStream);

    // Captures the ReliableResource object that should have been put in the ResourceCacheImpl's map
    ArgumentCaptor<ReliableResource> argument = ArgumentCaptor.forClass(ReliableResource.class);
    verify(resourceCache).put(argument.capture());

    verifyCaching(argument.getValue(), EXPECTED_CACHE_KEY);

    verifyClientBytesRead(clientBytesRead);

    cleanup();
  }

  /**
   * Verifies that if client is reading from @ReliableResourceInputStream slower than {@link
   * ReliableResourceCallable} is reading from product InputStream and writing to
   * FileBackedOutputStream, that complete product is still successfully downloaded by the client.
   * (This will be the case with CXF and @ReliableResourceCallable)
   *
   * @throws Exception
   */
  @Test
  // @Ignore
  public void testDownloadWithCachingDifferentChunkSizes() throws Exception {
    mis = new MockInputStream(productInputFilename);
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource()).thenReturn(resourceResponse);

    CacheKey cacheKey = new CacheKey(metacard, resourceResponse.getRequest());
    String key = cacheKey.generateKey();
    when(resourceCache.isPending(key)).thenReturn(false);

    int chunkSize = 50;
    startDownload(true, chunkSize, false, metacard, retriever);

    int clientChunkSize = 2;
    ByteArrayOutputStream clientBytesRead = clientRead(clientChunkSize, productInputStream);

    // Captures the ReliableResource object that should have been put in the ResourceCacheImpl's map
    ArgumentCaptor<ReliableResource> argument = ArgumentCaptor.forClass(ReliableResource.class);
    verify(resourceCache).put(argument.capture());

    verifyCaching(argument.getValue(), EXPECTED_CACHE_KEY);

    verifyClientBytesRead(clientBytesRead);

    cleanup();
  }

  /**
   * Test that if an Exception is thrown while reading the product's InputStream that download is
   * interrupted, retried and successfully completes on the second attempt.
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testStoreWithInputStreamRecoverableErrorCachingDisabled() throws Exception {

    mis = new MockInputStream(productInputFilename);

    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();
    ResourceRetriever retriever =
        getMockResourceRetrieverWithRetryCapability(RetryType.INPUT_STREAM_IO_EXCEPTION);

    int chunkSize = 50;
    startDownload(false, chunkSize, false, metacard, retriever);

    ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, productInputStream);

    // Verifies client read same contents as product input file
    verifyClientBytesRead(clientBytesRead);

    cleanup();
  }

  /**
   * Test that if an Exception is thrown while reading the product's InputStream that download and
   * caching is interrupted, retried and both successfully complete on the second attempt.
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testStoreWithInputStreamRecoverableErrorCachingEnabled() throws Exception {

    mis = new MockInputStream(productInputFilename);

    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();
    ResourceRetriever retriever =
        getMockResourceRetrieverWithRetryCapability(RetryType.INPUT_STREAM_IO_EXCEPTION);

    int chunkSize = 50;
    startDownload(true, chunkSize, false, metacard, retriever);

    ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, productInputStream);

    // Captures the ReliableResource object that should have been put in the ResourceCacheImpl's map
    ArgumentCaptor<ReliableResource> argument = ArgumentCaptor.forClass(ReliableResource.class);
    verify(resourceCache).put(argument.capture());

    verifyCaching(argument.getValue(), EXPECTED_CACHE_KEY);

    // Verifies client read same contents as product input file
    verifyClientBytesRead(clientBytesRead);

    cleanup();
  }

  /**
   * Test storing product in cache and one of the chunks being stored takes too long, triggering the
   * CacheMonitor to interrupt the caching. Verify that caching is retried and successfully
   * completes on the second attempt.
   *
   * @throws Exception
   */
  @Test
  // @Ignore
  public void testStoreWithTimeoutExceptionCachingEnabled() throws Exception {

    mis = new MockInputStream(productInputFilename);
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();
    ResourceRetriever retriever =
        getMockResourceRetrieverWithRetryCapability(RetryType.TIMEOUT_EXCEPTION);

    int chunkSize = 50;
    startDownload(true, chunkSize, false, metacard, retriever);

    ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, productInputStream);

    // Captures the ReliableResource object that should have been put in the ResourceCacheImpl's map
    ArgumentCaptor<ReliableResource> argument = ArgumentCaptor.forClass(ReliableResource.class);
    verify(resourceCache).put(argument.capture());

    verifyCaching(argument.getValue(), EXPECTED_CACHE_KEY);

    verifyClientBytesRead(clientBytesRead);

    cleanup();
  }

  /**
   * Tests that if user/client cancels a product retrieval that is in progress and actively being
   * cached, and the admin has configured caching to continue, that the caching continues and cached
   * file is placed in the cache map.
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testClientCancelProductDownloadCachingContinues() throws Exception {

    mis = new MockInputStream(productInputFilename);
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    ResourceRetriever retriever =
        getMockResourceRetrieverWithRetryCapability(RetryType.CLIENT_CANCELS_DOWNLOAD);

    int chunkSize = 50;
    startDownload(true, chunkSize, true, metacard, retriever);

    // On second read of ReliableResourceInputStream, client will close the stream simulating a
    // cancel
    // of the product download
    clientRead(chunkSize, productInputStream, 2);

    // Captures the ReliableResource object that should have been put in the ResourceCacheImpl's map
    ArgumentCaptor<ReliableResource> argument = ArgumentCaptor.forClass(ReliableResource.class);
    verify(resourceCache, timeout(3000)).put(argument.capture());

    verifyCaching(argument.getValue(), EXPECTED_CACHE_KEY);

    cleanup();
  }

  /**
   * Tests that if user/client cancels a product retrieval that is in progress and actively being
   * cached, and the admin has not configured caching to continue, that the (partially) cached file
   * is deleted and not placed in the cache map.
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testClientCancelProductDownloadCachingStops() throws Exception {

    mis = new MockInputStream(productInputFilename, true);
    mis.setReadDelay(MONITOR_PERIOD - 2, TimeUnit.MILLISECONDS);

    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    // Need the product InputStream (MockInputStream) to read slower so that client has time to
    // start reading from the ReliableResourceInputStream and close it, simulating a cancel of
    // the product download
    ResourceRetriever retriever =
        getMockResourceRetrieverWithRetryCapability(RetryType.CLIENT_CANCELS_DOWNLOAD, true);

    int chunkSize = 50;
    startDownload(true, chunkSize, false, metacard, retriever);

    // On second read of ReliableResourceInputStream, client will close the stream simulating a
    // cancel
    // of the product download
    clientRead(chunkSize, productInputStream, 2);

    // Verify product was not cached, i.e., its pending caching entry was removed
    String cacheKey = new CacheKey(metacard, resourceResponse.getRequest()).generateKey();
    verify(resourceCache, timeout(3000)).removePendingCacheEntry(cacheKey);

    cleanup();
  }

  /**
   * Tests that if network connection drops repeatedly during a product retrieval such that all of
   * the retry attempts are used up before entire product is downloaded, then the (partially) cached
   * file is deleted and not placed in the cache map, and product download fails.
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testRetryAttemptsExhaustedDuringProductDownload() throws Exception {

    mis = new MockInputStream(productInputFilename);
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    ResourceRetriever retriever =
        getMockResourceRetrieverWithRetryCapability(RetryType.NETWORK_CONNECTION_UP_AND_DOWN);

    int chunkSize = 50;
    startDownload(true, chunkSize, false, metacard, retriever);

    ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, productInputStream);

    // Verify client did not receive entire product download
    assertTrue(clientBytesRead.size() < expectedFileSize);

    // Verify product was not cached, i.e., its pending caching entry was removed
    String cacheKey = new CacheKey(metacard, resourceResponse.getRequest()).generateKey();
    verify(resourceCache, timeout(3000)).removePendingCacheEntry(cacheKey);

    cleanup();
  }

  /**
   * Tests that if network connection dropped during a product retrieval that is in progress and
   * actively being cached, that the (partially) cached file is deleted and not placed in the cache
   * map.
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testNetworkConnectionDroppedDuringProductDownload() throws Exception {

    mis = new MockInputStream(productInputFilename);
    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    ResourceRetriever retriever =
        getMockResourceRetrieverWithRetryCapability(RetryType.NETWORK_CONNECTION_DROPPED);

    int chunkSize = 50;
    startDownload(true, chunkSize, false, metacard, retriever);

    ByteArrayOutputStream clientBytesRead = clientRead(chunkSize, productInputStream);

    // Verify client did not receive entire product download
    assertTrue(clientBytesRead.size() < expectedFileSize);

    // Verify product was not cached, i.e., its pending caching entry was removed
    String cacheKey = new CacheKey(metacard, resourceResponse.getRequest()).generateKey();
    verify(resourceCache, timeout(3000)).removePendingCacheEntry(cacheKey);

    cleanup();
  }

  /**
   * Tests that if exception with file being cached to occurs during a product retrieval, then the
   * (partially) cached file is deleted and it is not placed in the cache map, but the product
   * continues to be streamed to the client until the EOF is detected.
   *
   * @throws Exception
   */
  @Test
  @Ignore
  public void testCacheFileExceptionDuringProductDownload() throws Exception {

    // Need the product InputStream (MockInputStream) to read slower so that client has time to
    // start reading from the ReliableResourceInputStream and close the FileOutputStream the
    // download manager is writing to, simulating a cache file exception during
    // the product download
    mis = new MockInputStream(productInputFilename, true);
    mis.setReadDelay(MONITOR_PERIOD - 2, TimeUnit.MILLISECONDS);

    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource()).thenReturn(resourceResponse);

    int chunkSize = 2;
    startDownload(true, chunkSize, false, metacard, retriever);

    // On second chunk read by client it will close the download manager's cache file output stream
    // to simulate a cache file exception that should be detected by the ReliableResourceCallable
    executor = Executors.newCachedThreadPool();
    ProductDownloadClient productDownloadClient =
        new ProductDownloadClient(productInputStream, chunkSize);
    productDownloadClient.setSimulateCacheFileException(2, downloadMgr);
    future = executor.submit(productDownloadClient);
    ByteArrayOutputStream clientBytesRead = future.get();

    verifyClientBytesRead(clientBytesRead);

    // Verify product was not cached, i.e., its pending caching entry was removed
    String cacheKey = new CacheKey(metacard, resourceResponse.getRequest()).generateKey();
    verify(resourceCache, timeout(3000)).removePendingCacheEntry(cacheKey);

    cleanup();
  }

  /**
   * Tests that if exception with the FileBackedOutputStream being written to and concurrently read
   * by the client occurs during a product retrieval, then the product download to the client is
   * stopped, but the caching of the file continues.
   *
   * @throws Exception
   */
  @Test
  @Ignore
  // Currently Ignored because cannot figure out how to get FileBackedOutputStream (FBOS) to throw
  // exception
  // during product download - this test successfully closes the FBOS, but the
  // ReliableResourceCallable
  // does not seem to detect this and continues to stream successfully to the client.
  public void testStreamToClientExceptionDuringProductDownloadCachingEnabled() throws Exception {

    mis = new MockInputStream(productInputFilename);

    Metacard metacard = getMockMetacard(EXPECTED_METACARD_ID, EXPECTED_METACARD_SOURCE_ID);
    resourceResponse = getMockResourceResponse();

    downloadMgr =
        new ReliableResourceDownloadManager(
            getDownloaderConfig(), downloadStatusInfo, Executors.newSingleThreadExecutor());

    // Use small chunk size so download takes long enough for client
    // to have time to simulate FileBackedOutputStream exception
    int chunkSize = 2;
    downloadMgr.setChunkSize(chunkSize);

    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource()).thenReturn(resourceResponse);

    ArgumentCaptor<ReliableResource> argument = ArgumentCaptor.forClass(ReliableResource.class);

    ResourceResponse newResourceResponse =
        downloadMgr.download(resourceRequest, metacard, retriever);
    assertThat(newResourceResponse, is(notNullValue()));
    productInputStream = newResourceResponse.getResource().getInputStream();
    assertThat(productInputStream, is(instanceOf(ReliableResourceInputStream.class)));

    // On second chunk read by client it will close the download manager's cache file output stream
    // to simulate a cache file exception that should be detected by the ReliableResourceCallable
    executor = Executors.newCachedThreadPool();
    ProductDownloadClient productDownloadClient =
        new ProductDownloadClient(productInputStream, chunkSize);
    productDownloadClient.setSimulateFbosException(chunkSize, downloadMgr);
    future = executor.submit(productDownloadClient);
    ByteArrayOutputStream clientBytesRead = future.get();

    // Verify client did not receive entire product download
    assertTrue(clientBytesRead.size() < expectedFileSize);

    // Captures the ReliableResource object that should have been put in the ResourceCacheImpl's map
    verify(resourceCache, timeout(3000)).put(argument.capture());

    verifyCaching(argument.getValue(), EXPECTED_CACHE_KEY);

    cleanup();
  }

  @Test
  public void testGetDownloadsInProgress() {
    List<String> downloadIds = new ArrayList<>();
    downloadIds.add("03e3f850-240c-4cc6-a5a3-318dc34ad4bd");
    downloadIds.add("5tygH67-345t-3er5-r86y-5tyZHU7UGD092");
    downloadIds.add("9tY75f4-345t-4er8-jj87-Y67hJJK098yaq");
    downloadIds.add("56lJOJl-45gg-3wf5-ww23-tldf7HewfhweJ");
    downloadIds.add("rRdlefj-ggal-erty-rr6e-ZZefoeje546kL");

    List<Map<String, String>> downloadStatusMaps = getDownloadStatusMaps(downloadIds);

    DownloadStatusInfo downloadStatusInfo = mock(DownloadStatusInfo.class);
    when(downloadStatusInfo.getAllDownloads()).thenReturn(downloadIds);
    when(downloadStatusInfo.getDownloadStatus(downloadIds.get(0)))
        .thenReturn(downloadStatusMaps.get(0));
    when(downloadStatusInfo.getDownloadStatus(downloadIds.get(1)))
        .thenReturn(downloadStatusMaps.get(1));
    when(downloadStatusInfo.getDownloadStatus(downloadIds.get(2)))
        .thenReturn(downloadStatusMaps.get(2));
    when(downloadStatusInfo.getDownloadStatus(downloadIds.get(3)))
        .thenReturn(downloadStatusMaps.get(3));
    when(downloadStatusInfo.getDownloadStatus(downloadIds.get(4)))
        .thenReturn(downloadStatusMaps.get(4));

    ReliableResourceDownloadManager reliableResorceDownloadManager =
        new ReliableResourceDownloadManager(null, downloadStatusInfo, null);

    List<DownloadInfo> downloadInfoList = reliableResorceDownloadManager.getDownloadsInProgress();

    assertThat(downloadInfoList.size(), is(2));
    for (DownloadInfo downloadInfo : downloadInfoList) {
      assertThat(downloadInfo.isDownloadInState(DownloadState.IN_PROGRESS), is(true));
    }
  }

  private List<Map<String, String>> getDownloadStatusMaps(List<String> downloadIds) {
    List<Map<String, String>> downloads = new ArrayList<>();
    Map<String, String> downloadStatus1 =
        createDownloadStatusMap(
            downloadIds.get(0),
            "image1.jpg",
            DownloadState.IN_PROGRESS.name(),
            "862978048",
            "76",
            "admin");
    downloads.add(downloadStatus1);
    Map<String, String> downloadStatus2 =
        createDownloadStatusMap(
            downloadIds.get(1),
            "image2.jpg",
            DownloadState.IN_PROGRESS.name(),
            "456212",
            "21",
            "localhost");
    downloads.add(downloadStatus2);
    Map<String, String> downloadStatus3 =
        createDownloadStatusMap(
            downloadIds.get(2),
            "image3.jpg",
            DownloadState.COMPLETED.name(),
            "3487",
            "11",
            "andrewreynolds");
    downloads.add(downloadStatus3);
    Map<String, String> downloadStatus4 =
        createDownloadStatusMap(
            downloadIds.get(3),
            "image4.jpg",
            DownloadState.FAILED.name(),
            "6567544543",
            "11",
            "markjohnson");
    downloads.add(downloadStatus4);
    Map<String, String> downloadStatus5 =
        createDownloadStatusMap(
            downloadIds.get(4),
            "image5.jpg",
            DownloadState.CANCELED.name(),
            "243",
            "1",
            "chriscole");
    downloads.add(downloadStatus5);

    return downloads;
  }

  private Map<String, String> createDownloadStatusMap(
      String downloadId,
      String fileName,
      String status,
      String bytesDownloaded,
      String percent,
      String user) {
    Map<String, String> downloadStatus1 = new HashMap<>();
    downloadStatus1.put(DOWNLOAD_ID_KEY, downloadId);
    downloadStatus1.put(FILE_NAME_KEY, fileName);
    downloadStatus1.put(STATUS_KEY, status);
    downloadStatus1.put(BYTES_DOWNLOADED_KEY, bytesDownloaded);
    downloadStatus1.put(PERCENT_KEY, percent);
    downloadStatus1.put(USER_KEY, user);
    return downloadStatus1;
  }

  private void startDownload(
      boolean cacheEnabled,
      int chunkSize,
      boolean cacheWhenCanceled,
      Metacard metacard,
      ResourceRetriever retriever)
      throws Exception {
    //        downloadMgr = new ReliableResourceDownloadManager(getDownloaderConfig());
    downloadMgr.setCacheEnabled(cacheEnabled);
    downloadMgr.setChunkSize(chunkSize);
    downloadMgr.setCacheWhenCanceled(cacheWhenCanceled);

    ResourceResponse newResourceResponse =
        downloadMgr.download(resourceRequest, metacard, retriever);
    assertThat(newResourceResponse, is(notNullValue()));
    productInputStream = newResourceResponse.getResource().getInputStream();
    assertThat(productInputStream, is(instanceOf(ReliableResourceInputStream.class)));
  }

  /////////////////////////////////////////////////////////////////////////////////////////////////////

  private void verifyCaching(ReliableResource reliableResource, String expectedCacheKey)
      throws IOException {
    assertEquals(expectedCacheKey, reliableResource.getKey());
    byte[] cachedData = reliableResource.getByteArray();
    assertNotNull(cachedData);

    // Verifies cached data read in was same contents as product input file
    assertTrue(cachedData.length == expectedFileSize);
    assertEquals(expectedFileContents, new String(cachedData));

    // Verifies cached file on disk has same contents as product input file
    assertEquals(
        expectedFileContents,
        IOUtils.toString(reliableResource.getInputStream(), StandardCharsets.UTF_8));
  }

  private void verifyClientBytesRead(ByteArrayOutputStream clientBytesRead) {

    // Verifies client read same contents as product input file
    if (clientBytesRead != null) {
      assertThat(clientBytesRead.size(), is(Long.valueOf(expectedFileSize).intValue()));
      assertEquals(expectedFileContents, new String(clientBytesRead.toByteArray()));
    }
  }

  private Metacard getMockMetacard(String id, String source) {

    Metacard metacard = mock(Metacard.class);

    when(metacard.getId()).thenReturn(id);

    when(metacard.getSourceId()).thenReturn(source);

    when(metacard.getMetacardType()).thenReturn(MetacardImpl.BASIC_METACARD);

    return metacard;
  }

  private ResourceResponse getMockResourceResponse() throws Exception {
    resourceRequest = mock(ResourceRequest.class);
    Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();
    when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());

    resource = mock(Resource.class);
    when(resource.getInputStream()).thenReturn(mis);
    when(resource.getName()).thenReturn("test-resource");
    when(resource.getMimeType()).thenReturn(new MimeType("text/plain"));

    resourceResponse = mock(ResourceResponse.class);
    when(resourceResponse.getRequest()).thenReturn(resourceRequest);
    when(resourceResponse.getResource()).thenReturn(resource);
    Map<String, Serializable> responseProperties = new HashMap<String, Serializable>();
    when(resourceResponse.getProperties()).thenReturn(responseProperties);

    return resourceResponse;
  }

  private ResourceRetriever getMockResourceRetrieverWithRetryCapability(final RetryType retryType)
      throws Exception {
    return getMockResourceRetrieverWithRetryCapability(retryType, false);
  }

  private ResourceRetriever getMockResourceRetrieverWithRetryCapability(
      final RetryType retryType, final boolean readSlow) throws Exception {

    // Mocking to support re-retrieval of product when error encountered
    // during caching.
    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource())
        .thenAnswer(
            new Answer<Object>() {
              int invocationCount = 0;

              public Object answer(InvocationOnMock invocation) throws ResourceNotFoundException {
                // Create new InputStream for retrieving the same product. This
                // simulates re-retrieving the product from the remote source.
                invocationCount++;
                if (readSlow) {
                  mis = new MockInputStream(productInputFilename, true);
                  mis.setReadDelay(MONITOR_PERIOD - 2, TimeUnit.MILLISECONDS);
                } else {
                  mis = new MockInputStream(productInputFilename);
                }

                if (retryType == RetryType.INPUT_STREAM_IO_EXCEPTION) {
                  if (invocationCount == 1) {
                    mis.setInvocationCountToThrowIOException(5);
                  } else {
                    mis.setInvocationCountToThrowIOException(-1);
                  }
                } else if (retryType == RetryType.TIMEOUT_EXCEPTION) {
                  if (invocationCount == 1) {
                    mis.setInvocationCountToTimeout(3);
                    mis.setReadDelay(MONITOR_PERIOD * 2, TimeUnit.SECONDS);
                  } else {
                    mis.setInvocationCountToTimeout(-1);
                    mis.setReadDelay(0, TimeUnit.SECONDS);
                  }
                } else if (retryType == RetryType.NETWORK_CONNECTION_UP_AND_DOWN) {
                  mis.setInvocationCountToThrowIOException(2);
                } else if (retryType == RetryType.NETWORK_CONNECTION_DROPPED) {
                  if (invocationCount == 1) {
                    mis.setInvocationCountToThrowIOException(2);
                  } else {
                    throw new ResourceNotFoundException();
                  }
                }

                // Reset the mock Resource so that it can be reconfigured to return
                // the new InputStream
                reset(resource);
                when(resource.getInputStream()).thenReturn(mis);
                when(resource.getName()).thenReturn("test-resource");
                try {
                  when(resource.getMimeType()).thenReturn(new MimeType("text/plain"));
                } catch (MimeTypeParseException e) {
                }

                // Reset the mock ResourceResponse so that it can be reconfigured to return
                // the new Resource
                reset(resourceResponse);
                when(resourceResponse.getRequest()).thenReturn(resourceRequest);
                when(resourceResponse.getResource()).thenReturn(resource);
                when(resourceResponse.getProperties())
                    .thenReturn(new HashMap<String, Serializable>());

                return resourceResponse;
              }
            });

    ArgumentCaptor<Long> bytesReadArg = ArgumentCaptor.forClass(Long.class);

    // Mocking to support re-retrieval of product when error encountered
    // during caching. This resource retriever supports skipping.
    when(retriever.retrieveResource(anyLong()))
        .thenAnswer(
            new Answer<Object>() {
              int invocationCount = 0;

              public Object answer(InvocationOnMock invocation)
                  throws ResourceNotFoundException, IOException {
                // Create new InputStream for retrieving the same product. This
                // simulates re-retrieving the product from the remote source.
                invocationCount++;
                if (readSlow) {
                  mis = new MockInputStream(productInputFilename, true);
                  mis.setReadDelay(MONITOR_PERIOD - 2, TimeUnit.MILLISECONDS);
                } else {
                  mis = new MockInputStream(productInputFilename);
                }

                // Skip the number of bytes that have already been read
                Object[] args = invocation.getArguments();
                long bytesToSkip = (Long) args[0];

                mis.skip(bytesToSkip);

                if (retryType == RetryType.INPUT_STREAM_IO_EXCEPTION) {
                  if (invocationCount == 1) {
                    mis.setInvocationCountToThrowIOException(5);
                  } else {
                    mis.setInvocationCountToThrowIOException(-1);
                  }
                } else if (retryType == RetryType.TIMEOUT_EXCEPTION) {
                  if (invocationCount == 1) {
                    mis.setInvocationCountToTimeout(3);
                    mis.setReadDelay(MONITOR_PERIOD * 2, TimeUnit.SECONDS);
                  } else {
                    mis.setInvocationCountToTimeout(-1);
                    mis.setReadDelay(0, TimeUnit.MILLISECONDS);
                  }
                } else if (retryType == RetryType.NETWORK_CONNECTION_UP_AND_DOWN) {
                  mis.setInvocationCountToThrowIOException(2);
                } else if (retryType == RetryType.NETWORK_CONNECTION_DROPPED) {
                  if (invocationCount == 1) {
                    mis.setInvocationCountToThrowIOException(2);
                  } else {
                    throw new ResourceNotFoundException();
                  }
                }

                // Reset the mock Resource so that it can be reconfigured to return
                // the new InputStream
                reset(resource);
                when(resource.getInputStream()).thenReturn(mis);
                when(resource.getName()).thenReturn("test-resource");
                try {
                  when(resource.getMimeType()).thenReturn(new MimeType("text/plain"));
                } catch (MimeTypeParseException e) {
                }

                // Reset the mock ResourceResponse so that it can be reconfigured to return
                // the new Resource
                reset(resourceResponse);
                when(resourceResponse.getRequest()).thenReturn(resourceRequest);
                when(resourceResponse.getResource()).thenReturn(resource);
                Map<String, Serializable> responseProperties = new HashMap<>();
                responseProperties.put("BytesSkipped", true);
                when(resourceResponse.getProperties()).thenReturn(responseProperties);
                when(resourceResponse.containsPropertyName("BytesSkipped")).thenReturn(true);
                when(resourceResponse.getPropertyValue("BytesSkipped")).thenReturn(true);

                return resourceResponse;
              }
            });

    return retriever;
  }

  public ByteArrayOutputStream clientRead(int chunkSize, InputStream is) throws Exception {
    return clientRead(chunkSize, is, -1);
  }

  public ByteArrayOutputStream clientRead(
      int chunkSize, InputStream is, int simulatedCancelChunkCount) throws Exception {
    executor = Executors.newCachedThreadPool();
    ProductDownloadClient productDownloadClient =
        new ProductDownloadClient(is, chunkSize, simulatedCancelChunkCount);
    future = executor.submit(productDownloadClient);
    ByteArrayOutputStream clientBytesRead = future.get();

    return clientBytesRead;
  }

  private void cleanup() {
    try {
      IOUtils.closeQuietly(productInputStream);
      FileUtils.deleteDirectory(new File(productCacheDirectory));
    } catch (IOException e) {
    }
    future.cancel(true);
    executor.shutdownNow();
  }

  private ReliableResourceDownloaderConfig getDownloaderConfig() {
    ReliableResourceDownloaderConfig downloaderConfig = new ReliableResourceDownloaderConfig();
    downloaderConfig.setResourceCache(resourceCache);
    downloaderConfig.setEventPublisher(eventPublisher);
    downloaderConfig.setEventListener(eventListener);
    return downloaderConfig;
  }

  private enum RetryType {
    INPUT_STREAM_IO_EXCEPTION,
    TIMEOUT_EXCEPTION,
    NETWORK_CONNECTION_UP_AND_DOWN,
    NETWORK_CONNECTION_DROPPED,
    CLIENT_CANCELS_DOWNLOAD,
    CACHE_FILE_EXCEPTION
  }
}
