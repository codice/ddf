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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingOutputStream;

/**
 * ReliableResourceCallable is responsible for reading product data from its {@link InputStream} and then writing that data
 * to a {@link CountingOutputStream} (that will be concurrently read by a client), and optionally caching the product to
 * the file system. It is a {@link Callable} that is started via a @Future by the {@link ReliableResourceDownloadManager} class.
 * <p>
 * The client uses the {@link ReliableResourceInputStream} to read from the {@link CountingOutputStream}.
 * <p>
 * This class will read bytes in chunks (whose size is specified by the caller) until it either reaches the EOF or it
 * is interrupted (either by an {@link IOException} or the cached resource}).
 * <p>
 * If the {@link InputStream} is read until the EOF, then a -1 is returned indicating the entire stream was read successfully.
 * Otherwise, the number of bytes read thus far is returned so that the caller can react accordingly (e.g., reopen the
 * product {@link InputStream} and skip forward that many bytes already read).
 */
public class ReliableResourceCallable implements Callable<ReliableResourceStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReliableResourceCallable.class);

    private static final int END_OF_FILE = -1;

    private final Object lock;

    private InputStream input = null;

    private CountingOutputStream countingFbos;

    private FileOutputStream cacheFileOutputStream = null;

    private File cacheFile;

    /**
     * Since this long is read by a different thread needs to be AtomicLong to make setting its
     * value thread-safe.
     */
    private AtomicLong bytesRead = new AtomicLong(0);

    private ReliableResourceStatus reliableResourceStatus;

    private int chunkSize;

    private boolean interruptDownload = false;

    private boolean cancelDownload = false;

    /**
     * Used when only downloading, no caching to {@link FileOutputStream} because caching was disabled or
     * had previous failed attempt trying to cache the product.
     *
     * @param input
     * @param countingFbos
     * @param chunkSize
     */
    public ReliableResourceCallable(InputStream input, CountingOutputStream countingFbos,
            int chunkSize, Object lock) {
        this(input, countingFbos, null, chunkSize, lock);
    }

    /**
     * Used when only caching, no writing to {@link CountingOutputStream} because no client is
     * reading from it.
     *
     * @param input
     * @param cacheFile
     * @param chunkSize
     */
    public ReliableResourceCallable(InputStream input, File cacheFile, int chunkSize, Object lock) {
        this(input, null, cacheFile, chunkSize, lock);
    }

    /**
     * Used when downloading and caching the product.
     *
     * @param input        the product {@link InputStream}
     * @param countingFbos the FileBackedOutputStream that is written to, number of bytes written to it are counted
     * @param cacheFile    the {@link File} that the cached product is written to
     * @param chunkSize    the number of bytes to read from the product @InputStream per chunk
     */
    public ReliableResourceCallable(InputStream input, CountingOutputStream countingFbos,
            File cacheFile, int chunkSize, Object lock) {
        this.input = input;
        this.countingFbos = countingFbos;
        this.cacheFile = cacheFile;
        this.chunkSize = chunkSize;
        this.lock = lock;
        if (cacheFile == null) {
            this.cacheFileOutputStream = null;
        } else {
            try {
                this.cacheFileOutputStream = FileUtils.openOutputStream(cacheFile, true);
            } catch (IOException e) {
                LOGGER.error("Could not open output stream to [{}]", cacheFile.toString());
                this.cacheFileOutputStream = null;
            }
        }
    }

    /**
     * Returns the number of bytes read from the product's @InputStream.
     *
     * @return
     */
    public long getBytesRead() {
        return bytesRead.get();
    }

    /**
     * Called when a new Callable is created for new retry attempt at product retrieval
     * and the bytesRead from previous attempt need to be skipped forward. This method
     * allows caller to account for bytes read in previous attempt(s) in case another
     * retry is attempted and the cumulative amount of bytes read can be maintained.
     *
     * @param bytesRead
     */

    public void setBytesRead(long bytesRead) {
        LOGGER.debug("Setting bytesRead = {}", bytesRead);
        this.bytesRead.set(bytesRead);
    }

    /**
     * Returns the current status of the resource download, e.g., COMPLETED, INTERRUPTED,
     * CANCELED, etc.
     *
     * @return
     */
    public ReliableResourceStatus getReliableResourceStatus() {
        return reliableResourceStatus;
    }

    /**
     * Set to true to indicate that the current resource download should be interrupted. Usually
     * set by the {@link ResourceRetrievalMonitor} when there has been a pause of n seconds where no bytes
     * have been read from the resource's @InputStream.
     *
     * @param interruptDownload
     */
    public void setInterruptDownload(boolean interruptDownload) {
        LOGGER.debug("Setting interruptDownload = {}", interruptDownload);
        this.interruptDownload = interruptDownload;

        // Set caching status here because it takes time for the Future running this
        // Callable to be canceled and the ReliableResourceStatus may be retrieved before
        // the call() method is interrupted
        LOGGER.debug("Download interrupted - returning {} bytes read", bytesRead);
        reliableResourceStatus =
                new ReliableResourceStatus(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED,
                        bytesRead.get());
        reliableResourceStatus.setMessage(
                "Download interrupted - returning " + bytesRead + " bytes read");
    }

    /**
     * Set to true when the current resource download should be canceled. Usually set by the
     * {@link ReliableResourceInputStream} when it is closed by the client.
     *
     * @param cancelDownload
     */
    public void setCancelDownload(boolean cancelDownload) {
        LOGGER.debug("Setting cancelDownload = {}", cancelDownload);
        this.cancelDownload = cancelDownload;

        // Set caching status here because it takes time for the Future running this
        // Callable to be canceled and the ReliableResourceStatus may be retrieved before
        // the call() method is interrupted
        LOGGER.debug("Download canceled - returning {} bytes read", bytesRead);
        reliableResourceStatus =
                new ReliableResourceStatus(DownloadStatus.RESOURCE_DOWNLOAD_CANCELED,
                        bytesRead.get());
        reliableResourceStatus.setMessage(
                "Download canceled - returning " + bytesRead + " bytes read");
    }

    @Override
    public ReliableResourceStatus call() {
        int chunkCount = 0;

        // File will not exist if there was a previous cache write failure
        if (cacheFile != null && !cacheFile.exists()) {
            cacheFileOutputStream = null;
        }

        byte[] buffer = new byte[chunkSize];
        int n = 0;

        while (!interruptDownload && !cancelDownload && !Thread.interrupted() && input != null) {
            chunkCount++;

            try {
                // Note that this blocking read() cannot be interrupted - this is why the 
                // ResourceRetrievalMonitor must cancel the Future that this Callable is running in.
                // Otherwise, this read will block until the original resource request, usually by
                // CXF, times out waiting for bytes to be written to the FBOS.
                n = input.read(buffer);
            } catch (IOException e) {
                if (interruptDownload || Thread.interrupted()) {
                    if (reliableResourceStatus != null) {
                        reliableResourceStatus.setMessage(
                                "IOException during read of product's InputStream");
                    }
                    return reliableResourceStatus;
                }
                LOGGER.info("IOException during read of product's InputStream - bytesRead = {}",
                        bytesRead.get(),
                        e);
                reliableResourceStatus =
                        new ReliableResourceStatus(DownloadStatus.PRODUCT_INPUT_STREAM_EXCEPTION,
                                bytesRead.get());
                reliableResourceStatus.setMessage("IOException during read of product's InputStream");

                closeAndDeleteIfCachingOnly();
                return reliableResourceStatus;
            }
            LOGGER.trace("AFTER read() - n = {}", n);
            if (n == END_OF_FILE) {
                break;
            }

            // Synchronized to prevent being interrupted in the middle of writing to the
            // OutputStreams
            synchronized (lock) {

                // If download was interrupted or canceled break now so that the bytesRead count does
                // not
                // get out of sync with the bytesWritten counts. If this count gets out of sync
                // then potentially the output streams will be one chunk off from the input stream
                // when a retry is attempted and the InputStream is skipped forward.
                if (cancelDownload || interruptDownload || Thread.interrupted()) {
                    LOGGER.debug("Breaking from download loop due to cancel or interrupt received");
                    if (reliableResourceStatus != null) {
                        reliableResourceStatus.setMessage(
                                "Breaking from download loop due to cancel or interrupt received");
                    }
                    closeAndDeleteIfCachingOnly();
                    return reliableResourceStatus;
                }

                bytesRead.addAndGet(n);

                if (cacheFileOutputStream != null) {
                    try {
                        cacheFileOutputStream.write(buffer, 0, n);
                    } catch (IOException e) {
                        LOGGER.error("Unable to write to cache file", e);
                        closeAndDeleteCacheFile();
                    }
                }

                if (countingFbos != null) {
                    try {
                        countingFbos.write(buffer, 0, n);
                        countingFbos.flush();
                    } catch (IOException e) {
                        LOGGER.info(
                                "IOException during write to FileBackedOutputStream for client to read",
                                e);
                        reliableResourceStatus =
                                new ReliableResourceStatus(DownloadStatus.CLIENT_OUTPUT_STREAM_EXCEPTION,
                                        bytesRead.get());
                    }
                }

                // Return status here so that each stream can be attempted to be updated regardless of
                // which one might have had an exception
                if (reliableResourceStatus != null) {
                    IOUtils.closeQuietly(cacheFileOutputStream);
                    return reliableResourceStatus;
                }
            }
            LOGGER.trace("chunkCount = {},  bytesRead = {}", chunkCount, bytesRead.get());
        }

        if (!interruptDownload && !cancelDownload && !Thread.interrupted()) {
            LOGGER.debug("Entire file downloaded successfully");
            reliableResourceStatus =
                    new ReliableResourceStatus(DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE,
                            bytesRead.get());
            reliableResourceStatus.setMessage("Download completed successfully");
        }
        IOUtils.closeQuietly(cacheFileOutputStream);
        return reliableResourceStatus;
    }

    private void closeAndDeleteCacheFile() {

        IOUtils.closeQuietly(cacheFileOutputStream);
        FileUtils.deleteQuietly(cacheFile);
        cacheFileOutputStream = null;
    }

    /**
     * If nothing is being streamed to a user and there is a failure, delete the cache file
     */
    private void closeAndDeleteIfCachingOnly() {
        IOUtils.closeQuietly(cacheFileOutputStream);
        cacheFileOutputStream = null;
        if (countingFbos == null) {
            LOGGER.error("Error while downloading resource to cache file.");
            FileUtils.deleteQuietly(cacheFile);
        }
    }
}
