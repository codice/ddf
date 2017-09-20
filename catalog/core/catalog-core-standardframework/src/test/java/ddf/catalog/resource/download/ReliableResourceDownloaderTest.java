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
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.CountingOutputStream;
import ddf.catalog.cache.MockInputStream;
import ddf.catalog.cache.impl.ResourceCacheImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.event.retrievestatus.DownloadStatusInfoImpl;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventListener;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher.ProductRetrievalStatus;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.data.ReliableResource;
import ddf.catalog.resourceretriever.ResourceRetriever;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.activation.MimeType;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

@PrepareForTest(ReliableResourceDownloader.class)
public class ReliableResourceDownloaderTest {
  private static final String DOWNLOAD_ID = "123";

  public static String productCacheDirectory;

  private static String productInputFilename;

  private Resource mockResource;

  private ResourceResponse mockResponse;

  private MockInputStream mis;

  private InputStream mockStream = mock(InputStream.class);

  private ReliableResourceDownloaderConfig downloaderConfig;

  private DownloadsStatusEventPublisher mockPublisher = mock(DownloadsStatusEventPublisher.class);

  private Metacard mockMetacard;

  @Rule public PowerMockRule rule = new PowerMockRule();

  @BeforeClass
  public static void oneTimeSetup() throws IOException {
    String workingDir = System.getProperty("user.dir");
    productCacheDirectory = workingDir + "/target/tests/product-cache";
    productInputFilename = workingDir + "/src/test/resources/foo_10_lines.txt";
  }

  @Before
  public void setUp() {
    downloaderConfig = new ReliableResourceDownloaderConfig();
    downloaderConfig.setEventListener(mock(DownloadsStatusEventListener.class));
    downloaderConfig.setEventPublisher(mockPublisher);
    // Don't wait between attempts
    downloaderConfig.setDelayBetweenAttemptsMS(0);
    mockMetacard = getMockMetacard(DOWNLOAD_ID, "sauce");
  }

  @Test
  public void testBadKeyName() throws Exception {
    Metacard metacard = getMockMetacard(DOWNLOAD_ID, ":badsourcename");

    downloaderConfig.setCacheEnabled(true);

    ResourceResponse mockResponse = getMockResourceResponse(mockStream);

    ResourceCacheImpl mockCache = mock(ResourceCacheImpl.class);
    when(mockCache.isPending(anyString())).thenReturn(false);
    when(mockCache.getProductCacheDirectory()).thenReturn(productCacheDirectory);
    downloaderConfig.setResourceCache(mockCache);

    ReliableResourceDownloader downloader =
        new ReliableResourceDownloader(
            downloaderConfig, new AtomicBoolean(), DOWNLOAD_ID, mockResponse, getMockRetriever());

    downloader.setupDownload(metacard, new DownloadStatusInfoImpl());
    verify(mockCache, never()).addPendingCacheEntry(any(ReliableResource.class));
  }

  @Test
  public void testIOExceptionDuringRead() throws Exception {
    ResourceResponse mockResponse = getMockResourceResponse(mockStream);
    when(mockStream.read(any(byte[].class))).thenThrow(new IOException());

    int retries = 5;
    downloaderConfig.setMaxRetryAttempts(retries);

    ReliableResourceDownloader downloader =
        new ReliableResourceDownloader(
            downloaderConfig, new AtomicBoolean(), DOWNLOAD_ID, mockResponse, getMockRetriever());
    downloader.setupDownload(mockMetacard, new DownloadStatusInfoImpl());
    downloader.run();

    verify(mockPublisher, times(retries))
        .postRetrievalStatus(
            any(ResourceResponse.class),
            eq(ProductRetrievalStatus.RETRYING),
            any(Metacard.class),
            anyString(),
            anyLong(),
            eq(DOWNLOAD_ID));
  }

  @Test
  public void testCacheExceptionDuringWrite() throws Exception {

    downloaderConfig.setCacheEnabled(true);

    ResourceCacheImpl mockCache = mock(ResourceCacheImpl.class);
    when(mockCache.isPending(anyString())).thenReturn(false);
    when(mockCache.getProductCacheDirectory()).thenReturn(productCacheDirectory);
    downloaderConfig.setResourceCache(mockCache);

    mis = new MockInputStream(productInputFilename);
    ResourceResponse mockResponse = getMockResourceResponse(mis);

    ReliableResourceDownloader downloader =
        new ReliableResourceDownloader(
            downloaderConfig, new AtomicBoolean(), "123", mockResponse, getMockRetriever());
    downloader.setupDownload(mockMetacard, new DownloadStatusInfoImpl());

    FileOutputStream mockFos = mock(FileOutputStream.class);
    doThrow(new IOException()).when(mockFos).write(any(byte[].class), anyInt(), anyInt());

    downloader.setFileOutputStream(mockFos);
    downloader.run();

    verify(mockPublisher, times(1))
        .postRetrievalStatus(
            any(ResourceResponse.class),
            eq(ProductRetrievalStatus.RETRYING),
            any(Metacard.class),
            anyString(),
            anyLong(),
            eq(DOWNLOAD_ID));
    verify(mockCache, times(1)).removePendingCacheEntry(anyString());
    assertThat(downloaderConfig.isCacheEnabled(), is(false));
  }

  @Test
  @Ignore
  // Can't figure out how to throw IOExcetion from CountingOutputStream
  public void testClientOutputStreamException() throws Exception {

    downloaderConfig.setCacheEnabled(true);

    ResourceCacheImpl mockCache = mock(ResourceCacheImpl.class);
    when(mockCache.isPending(anyString())).thenReturn(false);
    when(mockCache.getProductCacheDirectory()).thenReturn(productCacheDirectory);
    downloaderConfig.setResourceCache(mockCache);

    mis = new MockInputStream(productInputFilename);
    ResourceResponse mockResponse = getMockResourceResponse(mis);

    ReliableResourceDownloader downloader =
        new ReliableResourceDownloader(
            downloaderConfig, new AtomicBoolean(), "123", mockResponse, getMockRetriever());
    downloader.setupDownload(mockMetacard, new DownloadStatusInfoImpl());

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    CountingOutputStream mockCountingFbos = new CountingOutputStream(baos);
    IOUtils.closeQuietly(baos);

    downloader.setCountingOutputStream(mockCountingFbos);

    downloader.run();

    verify(mockPublisher, times(1))
        .postRetrievalStatus(
            any(ResourceResponse.class),
            eq(ProductRetrievalStatus.CANCELLED),
            any(Metacard.class),
            anyString(),
            anyLong(),
            eq(DOWNLOAD_ID));
    verify(mockCache, times(1)).removePendingCacheEntry(anyString());
    assertThat(downloaderConfig.isCacheEnabled(), is(false));
  }

  @Test
  public void testNullReliableResourceCallableAndStatus() throws Exception {
    ResourceResponse mockResponse = getMockResourceResponse(mockStream);

    ResourceRetriever mockResourceRetriever = mock(ResourceRetriever.class);
    when(mockResourceRetriever.retrieveResource(any(Byte.class))).thenReturn(mockResponse);

    ReliableResourceStatus resourceStatus =
        new ReliableResourceStatus(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED, 0L);

    ReliableResourceCallable mockCallable = mock(ReliableResourceCallable.class);
    when(mockCallable.getReliableResourceStatus()).thenReturn(resourceStatus);

    PowerMockito.whenNew(ReliableResourceCallable.class)
        .withAnyArguments()
        .thenReturn(null, mockCallable);
    PowerMockito.whenNew(ResourceRetrievalMonitor.class)
        .withAnyArguments()
        .thenThrow(new CancellationException());

    int retries = 5;
    downloaderConfig.setMaxRetryAttempts(retries);

    ReliableResourceDownloader downloader =
        new ReliableResourceDownloader(
            downloaderConfig,
            new AtomicBoolean(),
            DOWNLOAD_ID,
            mockResponse,
            mockResourceRetriever);
    downloader.setupDownload(mockMetacard, new DownloadStatusInfoImpl());
    downloader.run();

    verify(mockPublisher, times(retries))
        .postRetrievalStatus(
            any(ResourceResponse.class),
            eq(ProductRetrievalStatus.RETRYING),
            any(Metacard.class),
            anyString(),
            anyLong(),
            eq(DOWNLOAD_ID));
  }

  private Metacard getMockMetacard(String id, String source) {

    Metacard metacard = mock(Metacard.class);

    when(metacard.getId()).thenReturn(id);

    when(metacard.getSourceId()).thenReturn(source);

    when(metacard.getMetacardType()).thenReturn(BasicTypes.BASIC_METACARD);

    return metacard;
  }

  private ResourceResponse getMockResourceResponse(InputStream stream) throws Exception {
    ResourceRequest resourceRequest = mock(ResourceRequest.class);
    Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();
    when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());

    mockResource = mock(Resource.class);
    when(mockResource.getInputStream()).thenReturn(stream);
    when(mockResource.getName()).thenReturn("test-resource");
    when(mockResource.getMimeType()).thenReturn(new MimeType("text/plain"));

    mockResponse = mock(ResourceResponse.class);
    when(mockResponse.getRequest()).thenReturn(resourceRequest);
    when(mockResponse.getResource()).thenReturn(mockResource);
    Map<String, Serializable> responseProperties = new HashMap<String, Serializable>();
    when(mockResponse.getProperties()).thenReturn(responseProperties);

    return mockResponse;
  }

  private ResourceRetriever getMockRetriever()
      throws ResourceNotFoundException, ResourceNotSupportedException, IOException {
    ResourceRetriever retriever = mock(ResourceRetriever.class);
    when(retriever.retrieveResource(anyLong())).thenReturn(mockResponse);
    return retriever;
  }
}
