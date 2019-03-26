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

import com.google.common.base.Stopwatch;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.event.retrievestatus.DownloadStatusInfo;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.download.DownloadManagerState.DownloadState;
import ddf.catalog.resourceretriever.ResourceRetriever;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.resource.download.DownloadException;
import org.codice.ddf.catalog.resource.download.internal.DownloadManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The manager for downloading a resource, including retrying the download if problems are
 * encountered, and optionally caching the resource as it is streamed to the client.
 */
public class ReliableResourceDownloadManager implements DownloadManager {

  public static final String DOWNLOAD_ID_PROPERTY_KEY = "downloadId";

  private static final int ONE_SECOND_IN_MS = 1000;

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ReliableResourceDownloadManager.class);

  private ReliableResourceDownloaderConfig downloaderConfig;

  private DownloadStatusInfo downloadStatusInfo;

  private ExecutorService executor;

  /** @param downloaderConfig reference to the {@link ReliableResourceDownloaderConfig} */
  public ReliableResourceDownloadManager(
      ReliableResourceDownloaderConfig downloaderConfig,
      DownloadStatusInfo downloadStatusInfo,
      ExecutorService executor) {
    this.downloaderConfig = downloaderConfig;
    this.downloadStatusInfo = downloadStatusInfo;
    this.executor = executor;
  }

  public void init() {}

  public void cleanUp() {
    executor.shutdown();
    try {
      executor.awaitTermination(ONE_SECOND_IN_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      executor.shutdownNow();
    }
  }

  /**
   * @param resourceRequest the original @ResourceRequest to retrieve the resource
   * @param metacard the @Metacard associated with the resource being downloaded
   * @param retriever the @ResourceRetriever to be used to get the resource
   * @return the modified @ResourceResponse with the @ReliableResourceInputStream that the client
   *     should read from
   * @throws DownloadException
   */
  @Override
  public ResourceResponse download(
      ResourceRequest resourceRequest, Metacard metacard, ResourceRetriever retriever)
      throws DownloadException {

    ResourceResponse resourceResponse = null;
    String downloadIdentifier = UUID.randomUUID().toString();

    if (metacard == null) {
      throw new DownloadException("Cannot download resource if metacard is null");
    } else if (StringUtils.isBlank(metacard.getId())) {
      throw new DownloadException("Metacard must have unique id.");
    } else if (retriever == null) {
      throw new DownloadException("Cannot download resource if retriever is null");
    } else if (resourceRequest == null) {
      throw new DownloadException("Cannot download resource if request is null");
    }

    if (downloaderConfig.isCacheEnabled()) {
      Resource cachedResource =
          downloaderConfig
              .getResourceCache()
              .getValid(new CacheKey(metacard, resourceRequest).generateKey(), metacard);
      if (cachedResource != null) {
        resourceResponse =
            new ResourceResponseImpl(
                resourceRequest, resourceRequest.getProperties(), cachedResource);
        LOGGER.debug(
            "Successfully retrieved product from cache for metacard ID = {}", metacard.getId());
      } else {
        LOGGER.debug("Unable to get resource from cache. Have to retrieve it from source");
      }
    }

    if (resourceResponse == null) {
      try {
        resourceResponse = retriever.retrieveResource();
      } catch (ResourceNotFoundException | ResourceNotSupportedException | IOException e) {
        throw new DownloadException("Cannot download resource", e);
      }

      resourceResponse.getProperties().put(Core.ID, metacard.getId());
      // Sources do not create ResourceResponses with the original ResourceRequest, hence
      // it is added here because it will be needed for caching
      resourceResponse =
          new ResourceResponseImpl(
              resourceRequest, resourceResponse.getProperties(), resourceResponse.getResource());

      resourceResponse = startDownload(downloadIdentifier, resourceResponse, retriever, metacard);
    }
    return resourceResponse;
  }

  public void setMaxRetryAttempts(int maxRetryAttempts) {
    downloaderConfig.setMaxRetryAttempts(maxRetryAttempts);
  }

  public void setDelayBetweenAttempts(int delayBetweenAttempts) {
    LOGGER.debug("Delay between attempts set to {} second(s)", delayBetweenAttempts);
    downloaderConfig.setDelayBetweenAttemptsMS(delayBetweenAttempts * ONE_SECOND_IN_MS);
  }

  public void setMonitorPeriod(long monitorPeriod) {
    downloaderConfig.setMonitorPeriodMS(monitorPeriod * ONE_SECOND_IN_MS);
  }

  public void setMonitorInitialDelay(int monitorInitialDelay) {
    this.downloaderConfig.setMonitorInitialDelayMS(monitorInitialDelay * ONE_SECOND_IN_MS);
  }

  public void setCacheEnabled(boolean cacheEnabled) {
    downloaderConfig.setCacheEnabled(cacheEnabled);
  }

  public void setCacheWhenCanceled(boolean cacheWhenCanceled) {
    downloaderConfig.setCacheWhenCanceled(cacheWhenCanceled);
  }

  public void setDownloaderConfig(ReliableResourceDownloaderConfig downloaderConfig) {
    this.downloaderConfig = downloaderConfig;
  }

  public void setChunkSize(int chunkSize) {
    downloaderConfig.setChunkSize(chunkSize);
  }

  public boolean isCacheEnabled() {
    return downloaderConfig.isCacheEnabled();
  }

  public void setProductCacheDirectory(String productCacheDirectory) {
    this.downloaderConfig.getResourceCache().setProductCacheDirectory(productCacheDirectory);
  }

  public List<DownloadInfo> getDownloadsInProgress() {
    List<DownloadInfo> downloadsInProgress = new ArrayList<>();
    for (String downloadIdentifier : downloadStatusInfo.getAllDownloads()) {
      Map<String, String> downloadStatus = downloadStatusInfo.getDownloadStatus(downloadIdentifier);
      DownloadInfo downloadInfo = new DownloadInfo(downloadStatus);
      if (downloadInfo.isDownloadInState(DownloadState.IN_PROGRESS)) {
        downloadsInProgress.add(downloadInfo);
      }
    }

    return downloadsInProgress;
  }

  private ResourceResponse startDownload(
      String downloadIdentifier,
      ResourceResponse resourceResponse,
      ResourceRetriever retriever,
      Metacard metacard) {
    AtomicBoolean downloadStarted = new AtomicBoolean(Boolean.FALSE);
    ReliableResourceDownloader downloader =
        new ReliableResourceDownloader(
            downloaderConfig, downloadStarted, downloadIdentifier, resourceResponse, retriever);

    ResourceResponse response = downloader.setupDownload(metacard, downloadStatusInfo);
    response.getProperties().put(DOWNLOAD_ID_PROPERTY_KEY, downloadIdentifier);

    // Start download in separate thread so can return ResourceResponse with
    // ReliableResourceInputStream available for client to start reading from
    executor.submit(downloader);

    // Wait for download to get started before returning control to client
    Stopwatch stopwatch = Stopwatch.createStarted();
    while (!downloadStarted.get()) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      long elapsedTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
      if (elapsedTime > ONE_SECOND_IN_MS) {
        LOGGER.debug("downloadStarted still FALSE - elapsedTime = {}", elapsedTime);
        break;
      }
    }
    LOGGER.debug("elapsedTime = {}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    stopwatch.stop();
    return response;
  }
}
