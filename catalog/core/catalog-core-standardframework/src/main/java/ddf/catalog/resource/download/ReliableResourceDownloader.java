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
import java.io.InputStream;
import java.io.Serializable;
import java.util.Timer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.CountingOutputStream;
import com.google.common.io.FileBackedOutputStream;

import ddf.catalog.data.Metacard;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventListener;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher;
import ddf.catalog.event.retrievestatus.DownloadsStatusEventPublisher.ProductRetrievalStatus;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.download.DownloadManagerState.DownloadState;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.resourceretriever.ResourceRetriever;

public class ReliableResourceDownloader implements Runnable {

    public static final String BYTES_SKIPPED = "BytesSkipped";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReliableResourceDownloader.class);

    private static final int DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD =
            32 * ReliableResourceDownloaderConfig.KB;

    private final Object lock = new Object();

    private ReliableResourceCallable reliableResourceCallable;

    private Future<ReliableResourceStatus> downloadFuture;

    private ReliableResourceStatus reliableResourceStatus = null;

    private ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();

    private AtomicBoolean downloadStarted;

    private InputStream resourceInputStream;

    private ReliableResourceInputStream streamReadByClient;

    private File cacheFile;

    private FileBackedOutputStream fbos;

    private CountingOutputStream countingFbos;

    private DownloadManagerState downloadState;

    private Metacard metacard;

    private String downloadIdentifier;

    private ResourceResponse resourceResponse;

    private ReliableResourceDownloaderConfig downloaderConfig;

    private DownloadsStatusEventListener eventListener;

    private DownloadsStatusEventPublisher eventPublisher;

    private String filePath = "";

    private ResourceRetriever retriever;

    private Resource resource;

    private long reliableResourceByteSize = 0;

    private boolean continueDownloadingWhenCancelled = false;

    private Consumer<DownloadStatus> downloadStatusListener = downloadStatus -> {

    };

    /**
     * Only set to true if cacheEnabled is true *AND* product being downloaded is not already
     * pending caching, e.g., another client has already started downloading and caching it.
     */

    public ReliableResourceDownloader(ReliableResourceDownloaderConfig downloaderConfig,
            AtomicBoolean downloadStarted, String downloadIdentifier,
            ResourceResponse resourceResponse, ResourceRetriever retriever) {
        this.downloadStarted = downloadStarted;
        this.downloaderConfig = downloaderConfig;
        this.downloadIdentifier = downloadIdentifier;
        this.resourceResponse = resourceResponse;
        this.retriever = retriever;

        this.downloadState = new DownloadManagerState();
        this.downloadState.setDownloadState(DownloadManagerState.DownloadState.NOT_STARTED);

        this.eventListener = downloaderConfig.getEventListener();
        this.eventPublisher = downloaderConfig.getEventPublisher();
    }

    /**
     * Setup before the downloader is run.
     *
     * @param metacard  the metacard associated with the file to be downloaded.
     * @param cacheFile A cache File that may arrive null.
     * @return the resourceResponse at the end of the setup.
     */
    public ResourceResponse setupDownload(Metacard metacard, File cacheFile,
            boolean continueDownloadingWhenCancelled) {
        resource = resourceResponse.getResource();
        MimeType mimeType = resource.getMimeType();
        String resourceName = resource.getName();

        fbos = new FileBackedOutputStream(DEFAULT_FILE_BACKED_OUTPUT_STREAM_THRESHOLD);
        countingFbos = new CountingOutputStream(fbos);
        streamReadByClient = new ReliableResourceInputStream(fbos,
                countingFbos,
                downloadState,
                downloadIdentifier,
                resourceResponse);

        this.metacard = metacard;
        this.cacheFile = cacheFile;
        this.continueDownloadingWhenCancelled = continueDownloadingWhenCancelled;

        // Create new ResourceResponse to return that will encapsulate the
        // ReliableResourceInputStream that will be read by the client simultaneously as the product
        // is cached to disk (if caching is enabled)
        ResourceImpl newResource = new ResourceImpl(streamReadByClient, mimeType, resourceName);
        resourceResponse = new ResourceResponseImpl(resourceResponse.getRequest(),
                resourceResponse.getProperties(),
                newResource);

        // Get handle to retrieved product's InputStream
        resourceInputStream = resource.getInputStream();

        eventListener.setDownloadMap(downloadIdentifier, resourceResponse);
        return resourceResponse;
    }

    @Override
    public void run() {
        long bytesRead = 0;
        int retryAttempts = 0;

        downloaderConfig.getEventPublisher().postRetrievalStatus(resourceResponse,
                ProductRetrievalStatus.STARTED,
                metacard,
                null,
                0L,
                downloadIdentifier);

        try {
            reliableResourceCallable = new ReliableResourceCallable(resourceInputStream,
                    countingFbos,
                    cacheFile,
                    downloaderConfig.getChunkSize(),
                    lock);
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
                    eventPublisher.postRetrievalStatus(resourceResponse,
                            ProductRetrievalStatus.RETRYING,
                            metacard,
                            String.format("Attempt %d of %d.",
                                    retryAttempts,
                                    downloaderConfig.getMaxRetryAttempts()),
                            reliableResourceStatus.getBytesRead(),
                            downloadIdentifier);
                    delay();
                    reliableResourceCallable = retrieveResourceWithOffset(bytesRead);
                    continue;
                }
                retryAttempts++;
                LOGGER.debug("Download attempt {}", retryAttempts);
                try {
                    downloadExecutor = Executors.newSingleThreadExecutor();
                    downloadFuture = downloadExecutor.submit(reliableResourceCallable);

                    // Update callable and its Future in the ReliableResourceInputStream being read
                    // by the client so that if client cancels this download the proper Callable and
                    // Future are canceled.
                    streamReadByClient.setCallableAndItsFuture(reliableResourceCallable,
                            downloadFuture);

                    // Monitor to watch that bytes are continually being read from the resource's
                    // InputStream. This monitor is used to detect if there are long pauses or
                    // network connection loss during the product retrieval. If such a "gap" is
                    // detected, the Callable will be canceled and a new download attempt (retry)
                    // will be started.
                    final Timer downloadTimer = new Timer();
                    resourceRetrievalMonitor = new ResourceRetrievalMonitor(downloadFuture,
                            reliableResourceCallable,
                            downloaderConfig.getMonitorPeriodMS(),
                            eventPublisher,
                            resourceResponse,
                            metacard,
                            downloadIdentifier);
                    LOGGER.debug("Configuring resourceRetrievalMonitor to run every {} ms",
                            downloaderConfig.getMonitorPeriodMS());
                    downloadTimer.scheduleAtFixedRate(resourceRetrievalMonitor,
                            downloaderConfig.getMonitorInitialDelayMS(),
                            downloaderConfig.getMonitorPeriodMS());
                    downloadStarted.set(Boolean.TRUE);
                    reliableResourceStatus = downloadFuture.get();
                } catch (InterruptedException | CancellationException | ExecutionException e) {
                    LOGGER.error("{} - Unable to store product file {}",
                            e.getClass()
                                    .getSimpleName(),
                            filePath,
                            e);
                    reliableResourceStatus = reliableResourceCallable.getReliableResourceStatus();
                }

                LOGGER.debug("reliableResourceStatus = {}", reliableResourceStatus);
                downloadStatusListener.accept(reliableResourceStatus.getDownloadStatus());

                if (DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE.equals(reliableResourceStatus.getDownloadStatus())) {
                    LOGGER.debug("Cancelling resourceRetrievalMonitor");
                    resourceRetrievalMonitor.cancel();
                    if (downloadState.getDownloadState() != DownloadState.CANCELED) {
                        LOGGER.debug("Sending Product Retrieval Complete event");
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                ProductRetrievalStatus.COMPLETE,
                                metacard,
                                null,
                                reliableResourceStatus.getBytesRead(),
                                downloadIdentifier);
                    } else {
                        LOGGER.debug(
                                "Client had canceled download and caching completed - do NOT send ProductRetrievalCompleted notification");
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                ProductRetrievalStatus.COMPLETE,
                                metacard,
                                null,
                                reliableResourceStatus.getBytesRead(),
                                downloadIdentifier,
                                false,
                                true);
                    }
                    break;
                } else {
                    bytesRead = reliableResourceStatus.getBytesRead();
                    LOGGER.debug("Download not complete, only read {} bytes", bytesRead);

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

                    if (DownloadStatus.PRODUCT_INPUT_STREAM_EXCEPTION.equals(reliableResourceStatus.getDownloadStatus())) {

                        // Detected exception when reading from product's InputStream - re-retrieve
                        // product from the Source and retry caching it
                        LOGGER.info("Handling product InputStream exception");
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                ProductRetrievalStatus.RETRYING,
                                metacard,
                                String.format("Attempt %d of %d.",
                                        retryAttempts,
                                        downloaderConfig.getMaxRetryAttempts()),
                                reliableResourceStatus.getBytesRead(),
                                downloadIdentifier);
                        IOUtils.closeQuietly(resourceInputStream);
                        resourceInputStream = null;
                        delay();
                        reliableResourceCallable = retrieveResourceWithOffset(bytesRead);
                    } else if (DownloadStatus.CLIENT_OUTPUT_STREAM_EXCEPTION.equals(
                            reliableResourceStatus.getDownloadStatus())) {

                        // Detected exception when writing product data to the output stream
                        // (FileBackedOutputStream) that
                        // is being read by the client - assume this is unrecoverable, but continue
                        // to cache the file
                        LOGGER.info("Handling FileBackedOutputStream exception");
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                ProductRetrievalStatus.CANCELLED,
                                metacard,
                                "",
                                reliableResourceStatus.getBytesRead(),
                                downloadIdentifier);
                        LOGGER.debug("Cancelling resourceRetrievalMonitor");
                        resourceRetrievalMonitor.cancel();
                        closeAndNullUserOutputStreams();
                        reliableResourceCallable = new ReliableResourceCallable(resourceInputStream,
                                cacheFile,
                                downloaderConfig.getChunkSize(),
                                lock);
                        reliableResourceCallable.setBytesRead(bytesRead);

                    } else if (DownloadStatus.RESOURCE_DOWNLOAD_CANCELED.equals(
                            reliableResourceStatus.getDownloadStatus())) {

                        LOGGER.info("Handling client cancellation of product download");
                        downloadState.setDownloadState(DownloadState.CANCELED);
                        LOGGER.debug("Cancelling resourceRetrievalMonitor");
                        resourceRetrievalMonitor.cancel();
                        eventListener.removeDownloadIdentifier(downloadIdentifier);
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                ProductRetrievalStatus.CANCELLED,
                                metacard,
                                "",
                                reliableResourceStatus.getBytesRead(),
                                downloadIdentifier);
                        if (continueDownloadingWhenCancelled) {
                            LOGGER.debug("Continuing to cache product");
                            closeAndNullUserOutputStreams();
                            reliableResourceCallable = new ReliableResourceCallable(
                                    resourceInputStream,
                                    cacheFile,
                                    downloaderConfig.getChunkSize(),
                                    lock);
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
                        LOGGER.info(
                                "Handling interrupt of product caching - closing source InputStream");

                        // Set InputStream used on previous attempt to null so that any attempt to
                        // close it will not fail (CXF's DelegatingInputStream, which is the
                        // underlying InputStream being used, does a consume() which is a read() as
                        // part of its close() operation and this will result in a blocking read)
                        resourceInputStream = null;
                        eventPublisher.postRetrievalStatus(resourceResponse,
                                ProductRetrievalStatus.RETRYING,
                                metacard,
                                String.format("Attempt %d of %d.",
                                        retryAttempts,
                                        downloaderConfig.getMaxRetryAttempts()),
                                reliableResourceStatus.getBytesRead(),
                                downloadIdentifier);
                        delay();
                        reliableResourceCallable = retrieveResourceWithOffset(bytesRead);
                    }
                }
            }

            if (null != reliableResourceStatus && !DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE.equals(
                    reliableResourceStatus.getDownloadStatus())) {

                if (!DownloadStatus.RESOURCE_DOWNLOAD_CANCELED.equals(reliableResourceStatus.getDownloadStatus())) {
                    eventPublisher.postRetrievalStatus(resourceResponse,
                            ProductRetrievalStatus.FAILED,
                            metacard,
                            "Unable to retrieve product file.",
                            reliableResourceStatus.getBytesRead(),
                            downloadIdentifier);
                    FileUtils.deleteQuietly(cacheFile);
                }
            }
        } finally {
            reliableResourceByteSize = bytesRead;
            cleanupAfterDownload(reliableResourceStatus);
            downloadExecutor.shutdown();
        }
    }

    private ReliableResourceCallable retrieveResourceWithOffset(long bytesRead) {

        ReliableResourceCallable reliableResourceCallable = null;

        try {
            LOGGER.debug("Attempting to re-retrieve resource, skipping {} bytes", bytesRead);

            // Re-fetch product from the Source after setting up values to indicate the number of
            // bytes to skip. This prevents the same bytes being read again and put in the
            // PipedOutputStream that the client is still reading from and in the file being cached
            // to.
            ResourceResponse resourceResponse = retriever.retrieveResource(bytesRead);
            LOGGER.debug("Name of re-retrieved resource = {}",
                    resourceResponse.getResource()
                            .getName());
            resourceInputStream = resourceResponse.getResource()
                    .getInputStream();

            // If Source did not support the skipping of bytes, then will have to do it here.
            if (!resourceResponse.containsPropertyName(BYTES_SKIPPED)) {
                LOGGER.debug("Skipping {} bytes in re-retrieved source InputStream", bytesRead);
                long numBytesSkipped = resourceInputStream.skip(bytesRead);
                bytesRead = numBytesSkipped;
                LOGGER.debug("Actually skipped {} bytes in source InputStream", numBytesSkipped);
            } else {
                // If Source did not skip the number of bytes (even though it supposedly supported
                // skipping)
                Serializable value = resourceResponse.getPropertyValue(BYTES_SKIPPED);
                if (value instanceof Boolean) {
                    boolean bytesSkipped = (Boolean) value;
                    if (!bytesSkipped) {
                        LOGGER.debug("Skipping {} bytes in re-retrieved source InputStream",
                                bytesRead);
                        long numBytesSkipped = resourceInputStream.skip(bytesRead);
                        LOGGER.debug("Actually skipped {} bytes in source InputStream",
                                numBytesSkipped);
                    } else {
                        LOGGER.info("Source skipped bytes");
                    }
                } else {
                    LOGGER.warn("Unable to read {} property from resource response.",
                            BYTES_SKIPPED);
                }
            }

            reliableResourceCallable = new ReliableResourceCallable(resourceInputStream,
                    countingFbos,
                    cacheFile,
                    downloaderConfig.getChunkSize(),
                    lock);

            // So that Callable can account for bytes read in previous download attempt(s)
            reliableResourceCallable.setBytesRead(bytesRead);
        } catch (ResourceNotFoundException | ResourceNotSupportedException | IOException e) {
            LOGGER.warn("Unable to re-retrieve product; cannot download product file {}", filePath);
        }

        return reliableResourceCallable;
    }

    private void cleanupAfterDownload(ReliableResourceStatus reliableResourceStatus) {

        if (reliableResourceStatus != null) {
            if (reliableResourceStatus.getDownloadStatus()
                    != DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE) {

                if (reliableResourceStatus.getDownloadStatus()
                        == DownloadStatus.RESOURCE_DOWNLOAD_CANCELED) {
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
        IOUtils.closeQuietly(resourceInputStream);
        LOGGER.debug("Closed source InputStream");
    }

    private void delay() {
        try {
            LOGGER.debug("Waiting {} ms before attempting to re-retrieve and cache product {}",
                    downloaderConfig.getDelayBetweenAttemptsMS(),
                    filePath);
            Thread.sleep(downloaderConfig.getDelayBetweenAttemptsMS());
        } catch (InterruptedException e1) {
        }
    }

    /**
     * Closes FileBackedOutputStream and deletes its underlying tmp file (if any)
     */
    private void closeFileBackedOutputStream() {
        try {
            LOGGER.debug("Resetting FileBackedOutputStream");
            fbos.reset();
        } catch (IOException e) {
            LOGGER.info(
                    "Unable to reset FileBackedOutputStream - its tmp file may still be in <INSTALL_DIR>/data/tmp");
        }
    }

    private void closeAndNullUserOutputStreams() {
        try {
            fbos.reset();
        } catch (IOException e) {
            LOGGER.error("Error closing file-backed user output stream", e);
        }
        IOUtils.closeQuietly(fbos);
        IOUtils.closeQuietly(countingFbos);

        fbos = null;
        countingFbos = null;
    }

    public Long getReliableResourceInputStreamBytesCached() {
        return streamReadByClient.getBytesCached();
    }

    public String getReliableResourceInputStreamState() {
        return streamReadByClient.getDownloadState()
                .getDownloadState()
                .name();
    }

    public String getResourceSize() {
        return metacard.getResourceSize();
    }

    public ResourceResponse getResourceResponse() {
        return resourceResponse;
    }

    /**
     * Gets the download status from the reliable resource status.
     *
     * @return the DownloadStatus accessed through the ReliableResourceStatus object.
     */
    public DownloadStatus getDownloadStatus() {
        return reliableResourceStatus.getDownloadStatus();
    }

    /**
     * Gets the entire ReliableResourceStatus object.
     *
     * @return the ReliableResourceStatus object, reliableResourceStatus.
     */
    public ReliableResourceStatus getReliableResourceStatus() {
        return reliableResourceStatus;
    }

    /**
     * Get the bytes already downloaded of the reliable resource.
     *
     * @return The number of bytes already downloaded for the reliable resource.
     */
    public long getReliableResourceByteSize() {
        return reliableResourceByteSize;
    }

    /**
     * Gets the Resource object created during setup.
     *
     * @return the Resource object resource.
     */
    public Resource getResource() {
        return resource;
    }

    @VisibleForTesting
    void setFile(File cacheFile) {
        this.cacheFile = cacheFile;
    }

    @VisibleForTesting
    void setCountingOutputStream(CountingOutputStream countingFbos) {
        this.countingFbos = countingFbos;
    }
}
