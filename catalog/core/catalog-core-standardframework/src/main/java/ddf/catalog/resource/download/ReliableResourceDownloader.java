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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.CountingOutputStream;
import com.google.common.io.FileBackedOutputStream;
import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.cache.impl.ResourceCacheImpl;
import ddf.catalog.data.Metacard;
import ddf.catalog.event.retrievestatus.DownloadStatusInfo;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventListener;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher.ProductRetrievalStatus;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.data.ReliableResource;
import ddf.catalog.resource.download.DownloadManagerState.DownloadState;
import ddf.catalog.resource.download.ReliableResourceStatus.DownloadStatus;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.resourceretriever.ResourceRetriever;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.activation.MimeType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReliableResourceDownloader implements Runnable {

  public static final String BYTES_SKIPPED = "BytesSkipped";

  private static final Logger LOGGER = LoggerFactory.getLogger(ReliableResourceDownloader.class);

  private static final int DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD =
      32 * ReliableResourceDownloaderConfig.KB;

  private final Object lock = new Object();

  private ReliableResourceCallable reliableResourceCallable;

  private Future<ReliableResourceStatus> downloadFuture;

  private ExecutorService downloadExecutor;

  private AtomicBoolean downloadStarted;

  private InputStream resourceInputStream;

  private ReliableResourceInputStream streamReadByClient;

  private FileOutputStream fos;

  private FileBackedOutputStream fbos;

  private CountingOutputStream countingFbos;

  private ReliableResource reliableResource;

  private DownloadManagerState downloadState;

  private Metacard metacard;

  private String downloadIdentifier;

  private ResourceResponse resourceResponse;

  private ReliableResourceDownloaderConfig downloaderConfig;

  private DownloadsStatusEventListener eventListener;

  private ResourceCacheImpl resourceCache;

  private DownloadsStatusEventPublisher eventPublisher;

  private String filePath;

  private ResourceRetriever retriever;

  /**
   * Only set to true if cacheEnabled is true *AND* product being downloaded is not already pending
   * caching, e.g., another client has already started downloading and caching it.
   */
  private boolean doCaching;

  public ReliableResourceDownloader(
      ReliableResourceDownloaderConfig downloaderConfig,
      AtomicBoolean downloadStarted,
      String downloadIdentifier,
      ResourceResponse resourceResponse,
      ResourceRetriever retriever) {
    this.downloadStarted = downloadStarted;
    this.downloaderConfig = downloaderConfig;
    this.downloadIdentifier = downloadIdentifier;
    this.resourceResponse = resourceResponse;
    this.retriever = retriever;

    this.downloadState = new DownloadManagerState();
    this.downloadState.setDownloadState(DownloadManagerState.DownloadState.NOT_STARTED);

    // Do not enable caching yet - wait until determine if this product about to be downloaded
    // is already pending caching by another download in progress
    this.downloadState.setCacheEnabled(false);

    this.eventListener = downloaderConfig.getEventListener();
    this.eventPublisher = downloaderConfig.getEventPublisher();
    this.resourceCache = downloaderConfig.getResourceCache();
    this.downloadState.setContinueCaching(this.downloaderConfig.isCacheWhenCanceled());
  }

  public ResourceResponse setupDownload(Metacard metacard, DownloadStatusInfo downloadStatusInfo) {
    Resource resource = resourceResponse.getResource();
    MimeType mimeType = resource.getMimeType();
    String resourceName = resource.getName();

    fbos = new FileBackedOutputStream(DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD);
    countingFbos = new CountingOutputStream(fbos);
    streamReadByClient =
        new ReliableResourceInputStream(
            fbos, countingFbos, downloadState, downloadIdentifier, resourceResponse);

    this.metacard = metacard;

    // Create new ResourceResponse to return that will encapsulate the
    // ReliableResourceInputStream that will be read by the client simultaneously as the product
    // is cached to disk (if caching is enabled)
    ResourceImpl newResource = new ResourceImpl(streamReadByClient, mimeType, resourceName);
    resourceResponse =
        new ResourceResponseImpl(
            resourceResponse.getRequest(), resourceResponse.getProperties(), newResource);

    // Get handle to retrieved product's InputStream
    resourceInputStream = resource.getInputStream();

    eventListener.setDownloadMap(downloadIdentifier, resourceResponse);
    downloadStatusInfo.addDownloadInfo(downloadIdentifier, this, resourceResponse);

    if (downloaderConfig.isCacheEnabled()) {

      CacheKey keyMaker = null;
      String key = null;
      try {
        keyMaker = new CacheKey(metacard, resourceResponse.getRequest());
        key = keyMaker.generateKey();
      } catch (IllegalArgumentException e) {
        LOGGER.info("Cannot create cache key for resource with metacard ID = {}", metacard.getId());
        return resourceResponse;
      }

      if (!resourceCache.isPending(key)) {

        // Fully qualified path to cache file that will be written to.
        // Example:
        // <INSTALL-DIR>/data/product-cache/<source-id>-<metacard-id>
        // <INSTALL-DIR>/data/product-cache/ddf.distribution-abc123
        filePath = FilenameUtils.concat(resourceCache.getProductCacheDirectory(), key);
        if (filePath == null) {
          LOGGER.info(
              "Unable to create cache for cache directory {} and key {} - no caching will be done.",
              resourceCache.getProductCacheDirectory(),
              key);
          return resourceResponse;
        }

        reliableResource = new ReliableResource(key, filePath, mimeType, resourceName, metacard);
        resourceCache.addPendingCacheEntry(reliableResource);

        try {
          fos = FileUtils.openOutputStream(new File(filePath));
          doCaching = true;
          this.downloadState.setCacheEnabled(true);
        } catch (IOException e) {
          LOGGER.info("Unable to open cache file {} - no caching will be done.", filePath);
        }
      } else {
        LOGGER.debug("Cache key {} is already pending caching", key);
      }
    }

    return resourceResponse;
  }

  @Override
  public void run() {
    long bytesRead = 0;
    ReliableResourceStatus reliableResourceStatus = null;
    int retryAttempts = 0;

    downloaderConfig
        .getEventPublisher()
        .postRetrievalStatus(
            resourceResponse,
            ProductRetrievalStatus.STARTED,
            metacard,
            null,
            0L,
            downloadIdentifier);

    try {
      reliableResourceCallable =
          constructReliableResourceCallable(
              resourceInputStream, countingFbos, fos, downloaderConfig.getChunkSize(), lock);
      downloadFuture = null;
      ResourceRetrievalMonitor resourceRetrievalMonitor = null;
      this.downloadState.setDownloadState(DownloadManagerState.DownloadState.IN_PROGRESS);

      while (retryAttempts < downloaderConfig.getMaxRetryAttempts()) {
        if (reliableResourceCallable == null) {
          // This usually occurs on retry attempts to download and the
          // ReliableResourceCallable cannot be successfully created. In this case, a
          // partial cache file may have been created from the previous caching attempt(s)
          // and needs to be deleted from the product cache directory.
          LOGGER.debug("ReliableResourceCallable is null - cannot download resource");
          retryAttempts++;
          LOGGER.debug("Download attempt {}", retryAttempts);
          eventPublisher.postRetrievalStatus(
              resourceResponse,
              ProductRetrievalStatus.RETRYING,
              metacard,
              String.format(
                  "Attempt %d of %d.", retryAttempts, downloaderConfig.getMaxRetryAttempts()),
              (null == reliableResourceStatus) ? null : reliableResourceStatus.getBytesRead(),
              downloadIdentifier);
          delay();
          reliableResourceCallable = retrieveResource(bytesRead);
          continue;
        }
        retryAttempts++;
        LOGGER.debug("Download attempt {}", retryAttempts);
        try {
          downloadExecutor =
              Executors.newSingleThreadExecutor(
                  StandardThreadFactoryBuilder.newThreadFactory(
                      "reliableResourceDownloaderThread"));
          downloadFuture = downloadExecutor.submit(reliableResourceCallable);

          // Update callable and its Future in the ReliableResourceInputStream being read
          // by the client so that if client cancels this download the proper Callable and
          // Future are canceled.
          streamReadByClient.setCallableAndItsFuture(reliableResourceCallable, downloadFuture);

          // Monitor to watch that bytes are continually being read from the resource's
          // InputStream. This monitor is used to detect if there are long pauses or
          // network connection loss during the product retrieval. If such a "gap" is
          // detected, the Callable will be canceled and a new download attempt (retry)
          // will be started.
          final Timer downloadTimer = new Timer();
          resourceRetrievalMonitor = constructResourceRetrievalMonitor();
          LOGGER.debug(
              "Configuring resourceRetrievalMonitor to run every {} ms",
              downloaderConfig.getMonitorPeriodMS());
          downloadTimer.scheduleAtFixedRate(
              resourceRetrievalMonitor,
              downloaderConfig.getMonitorInitialDelayMS(),
              downloaderConfig.getMonitorPeriodMS());
          downloadStarted.set(Boolean.TRUE);
          reliableResourceStatus = downloadFuture.get();
        } catch (InterruptedException | CancellationException | ExecutionException e) {
          LOGGER.info(
              "{} - Unable to store product file {}", e.getClass().getSimpleName(), filePath, e);
          reliableResourceStatus = reliableResourceCallable.getReliableResourceStatus();
        }

        LOGGER.debug("reliableResourceStatus = {}", reliableResourceStatus);

        if (DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE.equals(
            reliableResourceStatus.getDownloadStatus())) {
          LOGGER.debug("Cancelling resourceRetrievalMonitor");
          resourceRetrievalMonitor.cancel();
          if (downloadState.getDownloadState() != DownloadState.CANCELED) {
            LOGGER.debug("Sending Product Retrieval Complete event");
            eventPublisher.postRetrievalStatus(
                resourceResponse,
                ProductRetrievalStatus.COMPLETE,
                metacard,
                null,
                reliableResourceStatus.getBytesRead(),
                downloadIdentifier);
          } else {
            LOGGER.debug(
                "Client had canceled download and caching completed - do NOT send ProductRetrievalCompleted notification");
            eventPublisher.postRetrievalStatus(
                resourceResponse,
                ProductRetrievalStatus.COMPLETE,
                metacard,
                null,
                reliableResourceStatus.getBytesRead(),
                downloadIdentifier,
                false,
                true);
          }
          if (doCaching) {
            LOGGER.debug("Setting reliableResource size");
            reliableResource.setSize(reliableResourceStatus.getBytesRead());
            LOGGER.debug("Adding caching key = {} to cache map", reliableResource.getKey());
            resourceCache.put(reliableResource);
          }
          break;
        } else {
          bytesRead = reliableResourceStatus.getBytesRead();
          LOGGER.debug("Download not complete, only read {} bytes", bytesRead);
          if (fos != null) {
            fos.flush();
          }

          // Synchronized so that the Callable is not shutdown while in the middle of
          // writing to the
          // FileBackedOutputStream and cache file (need to keep both of these in sync
          // with number of bytes
          // written to each of them).
          synchronized (lock) {

            // Simply doing Future.cancel(true) or a plain shutdown() is not enough.
            // The downloadExecutor (or its underlying Future/thread) is holding on
            // to a resource or is blocking on a read - undetermined at this point,
            // but shutdownNow() along with re-instantiating the executor at top of
            // while loop fixes this.
            downloadExecutor.shutdownNow();
          }

          if (DownloadStatus.PRODUCT_INPUT_STREAM_EXCEPTION.equals(
              reliableResourceStatus.getDownloadStatus())) {

            // Detected exception when reading from product's InputStream - re-retrieve
            // product from the Source and retry caching it
            LOGGER.info("Handling product InputStream exception");
            eventPublisher.postRetrievalStatus(
                resourceResponse,
                ProductRetrievalStatus.RETRYING,
                metacard,
                String.format(
                    "Attempt %d of %d.", retryAttempts, downloaderConfig.getMaxRetryAttempts()),
                reliableResourceStatus.getBytesRead(),
                downloadIdentifier);
            IOUtils.closeQuietly(resourceInputStream);
            resourceInputStream = null;
            delay();
            reliableResourceCallable = retrieveResource(bytesRead);
          } else if (DownloadStatus.CACHED_FILE_OUTPUT_STREAM_EXCEPTION.equals(
              reliableResourceStatus.getDownloadStatus())) {

            // Detected exception when writing the product data to the product cache
            // directory - assume this OutputStream cannot be fixed (e.g., disk full)
            // and just continue streaming product to the client, i.e., writing to the
            // FileBackedOutputStream
            LOGGER.info("Handling FileOutputStream exception");
            eventPublisher.postRetrievalStatus(
                resourceResponse,
                ProductRetrievalStatus.RETRYING,
                metacard,
                String.format(
                    "Attempt %d of %d.", retryAttempts, downloaderConfig.getMaxRetryAttempts()),
                reliableResourceStatus.getBytesRead(),
                downloadIdentifier);
            if (doCaching) {
              deleteCacheFile(fos);
              resourceCache.removePendingCacheEntry(reliableResource.getKey());
              // Disable caching since the cache file being written to had issues
              downloaderConfig.setCacheEnabled(false);
              doCaching = false;
              downloadState.setCacheEnabled(downloaderConfig.isCacheEnabled());
              downloadState.setContinueCaching(doCaching);
            }
            reliableResourceCallable =
                constructReliableResourceCallable(
                    resourceInputStream, countingFbos, null, downloaderConfig.getChunkSize(), lock);
            reliableResourceCallable.setBytesRead(bytesRead);

          } else if (DownloadStatus.CLIENT_OUTPUT_STREAM_EXCEPTION.equals(
              reliableResourceStatus.getDownloadStatus())) {

            // Detected exception when writing product data to the output stream
            // (FileBackedOutputStream) that
            // is being read by the client - assume this is unrecoverable, but continue
            // to cache the file
            LOGGER.info("Handling FileBackedOutputStream exception");
            eventPublisher.postRetrievalStatus(
                resourceResponse,
                ProductRetrievalStatus.CANCELLED,
                metacard,
                "",
                reliableResourceStatus.getBytesRead(),
                downloadIdentifier);
            IOUtils.closeQuietly(fbos);
            IOUtils.closeQuietly(countingFbos);
            LOGGER.debug("Cancelling resourceRetrievalMonitor");
            resourceRetrievalMonitor.cancel();
            reliableResourceCallable =
                constructReliableResourceCallable(
                    resourceInputStream, null, fos, downloaderConfig.getChunkSize(), lock);
            reliableResourceCallable.setBytesRead(bytesRead);

          } else if (DownloadStatus.RESOURCE_DOWNLOAD_CANCELED.equals(
              reliableResourceStatus.getDownloadStatus())) {

            LOGGER.info("Handling client cancellation of product download");
            downloadState.setDownloadState(DownloadState.CANCELED);
            LOGGER.debug("Cancelling resourceRetrievalMonitor");
            resourceRetrievalMonitor.cancel();
            eventListener.removeDownloadIdentifier(downloadIdentifier);
            eventPublisher.postRetrievalStatus(
                resourceResponse,
                ProductRetrievalStatus.CANCELLED,
                metacard,
                "",
                reliableResourceStatus.getBytesRead(),
                downloadIdentifier);
            if (doCaching && downloaderConfig.isCacheWhenCanceled()) {
              LOGGER.debug("Continuing to cache product");
              reliableResourceCallable =
                  constructReliableResourceCallable(
                      resourceInputStream, null, fos, downloaderConfig.getChunkSize(), lock);
              reliableResourceCallable.setBytesRead(bytesRead);
            } else {
              break;
            }

          } else if (DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED.equals(
              reliableResourceStatus.getDownloadStatus())) {

            // Caching has been interrupted (possibly resourceRetrievalMonitor detected
            // too much time being taken to retrieve a chunk of product data from the
            // InputStream) - re-retrieve product from the Source, skip forward in the
            // product InputStream the number of bytes already read successfully, and
            // retry caching it
            LOGGER.info("Handling interrupt of product caching - closing source InputStream");

            // Set InputStream used on previous attempt to null so that any attempt to
            // close it will not fail (CXF's DelegatingInputStream, which is the
            // underlying InputStream being used, does a consume() which is a read() as
            // part of its close() operation and this will result in a blocking read)
            resourceInputStream = null;
            eventPublisher.postRetrievalStatus(
                resourceResponse,
                ProductRetrievalStatus.RETRYING,
                metacard,
                String.format(
                    "Attempt %d of %d.", retryAttempts, downloaderConfig.getMaxRetryAttempts()),
                reliableResourceStatus.getBytesRead(),
                downloadIdentifier);
            delay();
            reliableResourceCallable = retrieveResource(bytesRead);
          }
        }
      }

      if (null != reliableResourceStatus
          && !DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE.equals(
              reliableResourceStatus.getDownloadStatus())) {
        if (doCaching) {
          deleteCacheFile(fos);
        }
        if (!DownloadStatus.RESOURCE_DOWNLOAD_CANCELED.equals(
            reliableResourceStatus.getDownloadStatus())) {
          eventPublisher.postRetrievalStatus(
              resourceResponse,
              ProductRetrievalStatus.FAILED,
              metacard,
              "Unable to retrieve product file.",
              reliableResourceStatus.getBytesRead(),
              downloadIdentifier);
        }
      }
    } catch (IOException e) {
      LOGGER.info("Unable to store product file {}", filePath, e);
      downloadState.setDownloadState(DownloadState.FAILED);
      eventPublisher.postRetrievalStatus(
          resourceResponse,
          ProductRetrievalStatus.FAILED,
          metacard,
          "Unable to store product file.",
          reliableResourceStatus.getBytesRead(),
          downloadIdentifier);
    } finally {
      cleanupAfterDownload(reliableResourceStatus);
      downloadExecutor.shutdown();
    }
  }

  private ReliableResourceCallable retrieveResource(long bytesRead) {

    ReliableResourceCallable reliableResourceCallable = null;

    try {
      LOGGER.debug("Attempting to re-retrieve resource, skipping {} bytes", bytesRead);

      // Re-fetch product from the Source after setting up values to indicate the number of
      // bytes to skip. This prevents the same bytes being read again and put in the
      // PipedOutputStream that the client is still reading from and in the file being cached
      // to. It also allows for range headers to be used in the request so that already read
      // bytes do not need to be re-retrieved.
      ResourceResponse resourceResponse = retriever.retrieveResource(bytesRead);
      LOGGER.debug("Name of re-retrieved resource = {}", resourceResponse.getResource().getName());
      resourceInputStream = resourceResponse.getResource().getInputStream();

      reliableResourceCallable =
          constructReliableResourceCallable(
              resourceInputStream, countingFbos, fos, downloaderConfig.getChunkSize(), lock);

      // So that Callable can account for bytes read in previous download attempt(s)
      reliableResourceCallable.setBytesRead(bytesRead);
    } catch (ResourceNotFoundException | ResourceNotSupportedException | IOException e) {
      LOGGER.info("Unable to re-retrieve product; cannot download product file {}", filePath);
    }

    return reliableResourceCallable;
  }

  private void deleteCacheFile(FileOutputStream fos) {
    LOGGER.debug("Deleting partially cached file {}", filePath);
    IOUtils.closeQuietly(fos);

    // Delete the cache file since it will no longer be written to and it currently has
    // incomplete or corrupted data in it
    boolean result = FileUtils.deleteQuietly(new File(filePath));
    LOGGER.debug("result of deleting partial cache file = {}", result);
  }

  private void cleanupAfterDownload(ReliableResourceStatus reliableResourceStatus) {

    if (reliableResourceStatus != null) {
      // If caching was not successful, then remove this product from the pending cache list
      // (Otherwise partially cached files will remain in pending list and returned to
      // subsequent clients)
      if (!DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE.equals(
          reliableResourceStatus.getDownloadStatus())) {
        if (doCaching) {
          resourceCache.removePendingCacheEntry(reliableResource.getKey());
        }
        if (DownloadStatus.RESOURCE_DOWNLOAD_CANCELED.equals(
            reliableResourceStatus.getDownloadStatus())) {
          this.downloadState.setDownloadState(DownloadManagerState.DownloadState.CANCELED);
        } else {
          this.downloadState.setDownloadState(DownloadManagerState.DownloadState.FAILED);
        }
        closeFileBackedOutputStream();
      } else {
        this.downloadState.setDownloadState(DownloadManagerState.DownloadState.COMPLETED);
        // FileBackedOutputStream should be closed by ReliableResourceInputStream for
        // successful downloads since client reading from this InputStream will lag when
        // Callable finishes reading product's InputStream
      }
    }
    IOUtils.closeQuietly(countingFbos);
    if (doCaching) {
      IOUtils.closeQuietly(fos);
    }
    LOGGER.debug("Closing source InputStream");
    IOUtils.closeQuietly(resourceInputStream);
    LOGGER.debug("Closed source InputStream");
  }

  private void delay() {
    try {
      LOGGER.debug(
          "Waiting {} ms before attempting to re-retrieve and cache product {}",
          downloaderConfig.getDelayBetweenAttemptsMS(),
          filePath);
      Thread.sleep(downloaderConfig.getDelayBetweenAttemptsMS());
    } catch (InterruptedException e1) {
    }
  }

  /** Closes FileBackedOutputStream and deletes its underlying tmp file (if any) */
  private void closeFileBackedOutputStream() {
    try {
      LOGGER.debug("Resetting FileBackedOutputStream");
      fbos.reset();
    } catch (IOException e) {
      LOGGER.info(
          "Unable to reset FileBackedOutputStream - its tmp file may still be in <INSTALL_DIR>/data/tmp");
    }
  }

  public Long getReliableResourceInputStreamBytesCached() {
    return streamReadByClient.getBytesCached();
  }

  public String getReliableResourceInputStreamState() {
    return streamReadByClient.getDownloadState().getDownloadState().name();
  }

  public String getResourceSize() {
    return metacard.getResourceSize();
  }

  public ResourceResponse getResourceResponse() {
    return resourceResponse;
  }

  @VisibleForTesting
  void setFileOutputStream(FileOutputStream fos) {
    this.fos = fos;
  }

  @VisibleForTesting
  void setCountingOutputStream(CountingOutputStream countingFbos) {
    this.countingFbos = countingFbos;
  }

  @VisibleForTesting
  ReliableResourceCallable constructReliableResourceCallable(
      InputStream input,
      CountingOutputStream countingFbos,
      FileOutputStream fos,
      int chunkSize,
      Object lock) {
    return new ReliableResourceCallable(input, countingFbos, fos, chunkSize, lock);
  }

  @VisibleForTesting
  ResourceRetrievalMonitor constructResourceRetrievalMonitor() {
    return new ResourceRetrievalMonitor(
        downloadFuture,
        reliableResourceCallable,
        downloaderConfig.getMonitorPeriodMS(),
        eventPublisher,
        resourceResponse,
        metacard,
        downloadIdentifier);
  }
}
