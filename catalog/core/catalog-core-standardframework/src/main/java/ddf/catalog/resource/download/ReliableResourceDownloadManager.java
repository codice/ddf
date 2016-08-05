/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.resource.download;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Metacard;
import ddf.catalog.event.retrievestatus.DownloadStatusInfo;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher.ProductRetrievalStatus;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.data.ReliableResource;
import ddf.catalog.resource.download.DownloadManagerState.DownloadState;
import ddf.catalog.resourceretriever.ResourceRetriever;

/**
 * The manager for downloading a resource, including retrying the download if problems are
 * encountered, and optionally caching the resource as it is streamed to the client.
 */
public class ReliableResourceDownloadManager {

    public static final String DOWNLOAD_ID_PROPERTY_KEY = "downloadId";

    private static final int ONE_SECOND_IN_MS = 1000;

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ReliableResourceDownloadManager.class);

    private ReliableResourceDownloaderConfig downloaderConfig;

    private DownloadStatusInfo downloadStatusInfo;

    private ListeningExecutorService executor;

    /**
     * @param downloaderConfig reference to the {@link ReliableResourceDownloaderConfig}
     */
    public ReliableResourceDownloadManager(ReliableResourceDownloaderConfig downloaderConfig,
            DownloadStatusInfo downloadStatusInfo, ListeningExecutorService executor) {
        this.downloaderConfig = downloaderConfig;
        this.downloadStatusInfo = downloadStatusInfo;
        this.executor = executor;
    }

    public void init() {

    }

    /**
     * @param resourceRequest the original {@link ResourceRequest} to retrieve the resource
     * @param metacard        the {@link Metacard} associated with the resource being downloaded
     * @param retriever       the {@link ResourceRetriever} to be used to get the resource
     * @return the modified {@link ResourceResponse} with the {@link ReliableResourceInputStream} that the client
     * should read from
     * @throws DownloadException
     */
    public ResourceResponse download(ResourceRequest resourceRequest, Metacard metacard,
            ResourceRetriever retriever) throws DownloadException {

        ResourceResponse resourceResponse = null;
        String downloadIdentifier = UUID.randomUUID()
                .toString();
        ReliableResource managerReliableResource = null;

        boolean doCaching = false;

        Resource resource;

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
            resource = downloaderConfig.getResourceCache()
                    .getValid(new CacheKey(metacard, resourceRequest).generateKey(), metacard);
            if (resource != null) {
                resourceResponse = new ResourceResponseImpl(resourceRequest,
                        resourceRequest.getProperties(),
                        resource);
                LOGGER.debug("Successfully retrieved product from cache for metacard ID = {}",
                        metacard.getId());
                return resourceResponse;
            } else {
                LOGGER.debug("Unable to get resource from cache. Have to retrieve it from source");
            }
        }

        try {
            resourceResponse = retriever.retrieveResource();
            resource = resourceResponse.getResource();
        } catch (ResourceNotFoundException | ResourceNotSupportedException | IOException e) {
            throw new DownloadException("Cannot download resource", e);
        }

        resourceResponse.getProperties()
                .put(Metacard.ID, metacard.getId());
        // Sources do not create ResourceResponses with the original ResourceRequest, hence
        // it is added here because it will be needed for caching
        resourceResponse = new ResourceResponseImpl(resourceRequest,
                resourceResponse.getProperties(),
                resourceResponse.getResource());

        // TODO - this should be before retrieveResource() but eventPublisher requires a
        // resourceResponse and that resource response must have a resource request in it (to get
        // USER property)
        publishStartEvent(metacard, resourceResponse, downloadIdentifier);

        resourceResponse = startDownload(metacard,
                retriever,
                resourceResponse,
                downloadIdentifier,
                managerReliableResource,
                doCaching,
                resource);
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

    public void setCacheEnabled(boolean cacheEnabled) {
        downloaderConfig.setCacheEnabled(cacheEnabled);
    }

    public void setProductCacheDirectory(String productCacheDirectory) {
        this.downloaderConfig.getResourceCache()
                .setProductCacheDirectory(productCacheDirectory);
    }

    public List<DownloadInfo> getDownloadsInProgress() {
        List<DownloadInfo> downloadsInProgress = new ArrayList<>();
        for (String downloadIdentifier : downloadStatusInfo.getAllDownloads()) {
            Map<String, String> downloadStatus = downloadStatusInfo
                    .getDownloadStatus(downloadIdentifier);
            DownloadInfo downloadInfo = new DownloadInfo(downloadStatus);
            if (downloadInfo.isDownloadInState(DownloadState.IN_PROGRESS)) {
                downloadsInProgress.add(downloadInfo);
            }
        }

        return downloadsInProgress;
    }

    public void cleanUp() {
        executor.shutdown();
        try {
            executor.awaitTermination(ONE_SECOND_IN_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }

    private void publishStartEvent(Metacard metacard, ResourceResponse resourceResponse, String downloadIdentifier) {
        downloaderConfig.getEventPublisher()
                .postRetrievalStatus(resourceResponse,
                        ProductRetrievalStatus.STARTED,
                        metacard,
                        null,
                        0L,
                        downloadIdentifier);
    }

    private ResourceResponse startDownload(Metacard metacard, ResourceRetriever retriever,
            ResourceResponse resourceResponse, String downloadIdentifier,
            ReliableResource managerReliableResource, boolean doCaching, Resource resource) {
        AtomicBoolean downloadStarted = new AtomicBoolean(Boolean.FALSE);

        ReliableResourceDownloader downloader = new ReliableResourceDownloader(downloaderConfig,
                downloadStarted,
                downloadIdentifier,
                resourceResponse,
                retriever);

        Path filePath;
        File cacheFile = null;

        String key = getKey(metacard, resourceResponse);

        //if the cache is not pending for this key, then create a new reliable resource using that key.
        //Also, tell the resource cache through the downloader config that we have a new pending item.
        if (!downloaderConfig.getResourceCache()
                .isPending(key)) {

            // Fully qualified path to cache file that will be written to.
            // Example:
            // <INSTALL-DIR>/data/product-cache/<source-id>-<metacard-id>
            // <INSTALL-DIR>/data/product-cache/ddf.distribution-abc123
            filePath = Paths.get(downloaderConfig.getResourceCache()
                    .getProductCacheDirectory(), key);
            managerReliableResource = new ReliableResource(key,
                    filePath.toString(),
                    resource.getMimeType(),
                    resource.getName(),
                    metacard);

            try {
                cacheFile = filePath.toFile();
                if (cacheFile.canWrite() || cacheFile.createNewFile()) {
                    downloaderConfig.getResourceCache()
                            .addPendingCacheEntry(managerReliableResource);
                    doCaching = true;
                }
            } catch (IOException e) {
                LOGGER.error("Unable to open cache file {} - no caching will be done.", filePath);
                cacheFile = null;
            }

        } else {
            LOGGER.debug("Cache key {} is pending caching", key);
        }

        boolean continueDownloadingWhenCancelled =
                doCaching && downloaderConfig.isCacheWhenCanceled();

        resourceResponse = downloader.setupDownload(metacard,
                cacheFile,
                continueDownloadingWhenCancelled);

        downloadStatusInfo.addDownloadInfo(downloadIdentifier, downloader, resourceResponse);

        // Start download in separate thread so can return ResourceResponse with
        // ReliableResourceInputStream available for client to start reading from
        // downloaderConfig.getExecutor().submit(downloader);
        // Start download in separate thread so can return ResourceResponse with
        // ReliableResourceInputStream available for client to start reading from

        ListenableFuture downloadFuture = executor.submit(downloader);

        //if there is additional caching activity that is needed, such as determining the pending status, the
        // ResourceDownloadCallback is used
        if (doCaching) {
            Futures.addCallback(downloadFuture,
                    new ResourceDownloadCallback(downloader,
                            cacheFile,
                            managerReliableResource,
                            downloaderConfig.getResourceCache()));
        }

        // Wait for download to get started before returning control to client
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (!downloadStarted.get()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread()
                        .interrupt();
            }
            long elapsedTime = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            if (elapsedTime > ONE_SECOND_IN_MS) {
                LOGGER.debug("downloadStarted still FALSE - elapsedTime = {}", elapsedTime);
                break;
            }
        }
        LOGGER.debug("elapsedTime = {}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        stopwatch.stop();
        return resourceResponse;
    }

    /**
     * Creates a key for a metacard.
     *
     * @param metacard         the metacard object to which the generated key belongs.
     * @param resourceResponse used to generate the metacard's key.
     * @return a string representation of the key.
     */
    private String getKey(Metacard metacard, ResourceResponse resourceResponse) {
        String key = null;
        CacheKey keyMaker;

        try {
            keyMaker = new CacheKey(metacard, resourceResponse.getRequest());
            key = keyMaker.generateKey();
        } catch (IllegalArgumentException e) {
            LOGGER.info("Cannot create cache key for resource with metacard ID = {}",
                    metacard.getId());
        }

        return key;
    }
}
