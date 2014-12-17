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
package ddf.catalog.resource.download;

import com.google.common.io.CountingOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;


/**
 * ReliableResourceCallable is responsible for reading product data from its @InputStream and then writing that data
 * to a @FileBackedOutputStream (that will be concurrently read by a client), and optionally caching the product to 
 * the file system. It is a @Callable that is started via a @Future by the @ReliableResourceDownloadManager class.
 * 
 * The client uses the @ReliableResourceInputStream to read from the @FileBackedOutputStream. 
 * 
 * This class will read bytes in chunks (whose size is specified by the caller) until it either reaches the EOF or it 
 * is interrupted (either by an @IOException or the @CachedResource).
 * 
 * If the @InputStream is read until the EOF, then a -1 is returned indicating the entire stream was read successfully. 
 * Otherwise, the number of bytes read thus far is returned so that the caller can react accordingly (e.g., reopen the 
 * product @InputStream and skip forward that many bytes already read).
 * 
 */
public class ReliableResourceCallable implements Callable<ReliableResourceStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReliableResourceCallable.class);
    
    private static final int END_OF_FILE = -1;

    private InputStream input = null;

    private CountingOutputStream countingFbos;

    private FileOutputStream cacheFileOutputStream = null;

    /**
     * Since this long is read by a different thread needs to be AtomicLong to make setting its
     * value thread-safe.
     */
    private AtomicLong bytesRead = new AtomicLong(0);

    private ReliableResourceStatus reliableResourceStatus;

    private int chunkSize;

    private boolean interruptDownload = false;
    
    private boolean cancelDownload = false;

    private final Object lock;

    /**
     * Used when only downloading, no caching to @FileOutputStream because caching was disabled or 
     * had previous failed attempt trying to cache the product.
     * 
     * @param input
     * @param countingFbos
     * @param chunkSize
     */
    public ReliableResourceCallable(InputStream input, CountingOutputStream countingFbos, int chunkSize, Object lock) {
        this(input, countingFbos, null, chunkSize, lock);
    }

    /**
     * Used when only caching, no writing to @FileBackedOutputStream because no client is
     * reading from it.
     *  
     * @param input
     * @param fos
     * @param chunkSize
     */
    public ReliableResourceCallable(InputStream input, FileOutputStream fos, int chunkSize, Object lock) {
        this(input, null, fos, chunkSize, lock);
    }
    
    /**
     * Used when downloading and caching the product.
     * 
     * @param input the product @InputStream
     * @param countingFbos the FileBackedOutputStream that is written to, number of bytes written to it are counted
     * @param fos the @FileOutputStream that the cached product is written to
     * @param chunkSize the number of bytes to read from the product @InputStream per chunk
     */
    public ReliableResourceCallable(InputStream input, CountingOutputStream countingFbos,
            FileOutputStream fos, int chunkSize, Object lock) {
        this.input = input;
        this.countingFbos = countingFbos;
        this.cacheFileOutputStream = fos;
        this.chunkSize = chunkSize;
        this.lock = lock;
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
     * set by the @ResourceRetrievalMonitor when there has been a pause of n seconds where no bytes
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
        reliableResourceStatus = new ReliableResourceStatus(DownloadStatus.RESOURCE_DOWNLOAD_INTERRUPTED,
                bytesRead.get());
        reliableResourceStatus.setMessage("Download interrupted - returning " + bytesRead + " bytes read");
    }

    /**
     * Set to true when the current resource download should be canceled. Usually set by the
     * @ReliableResourceInputStream when it is closed by the client.
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
        reliableResourceStatus = new ReliableResourceStatus(DownloadStatus.RESOURCE_DOWNLOAD_CANCELED,
                bytesRead.get());
        reliableResourceStatus.setMessage("Download canceled - returning " + bytesRead + " bytes read");
    }

    @Override
    public ReliableResourceStatus call() {
        int chunkCount = 0;

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
                        reliableResourceStatus.setMessage("IOException during read of product's InputStream");
                    }
                    return reliableResourceStatus;
                }
                LOGGER.info("IOException during read of product's InputStream - bytesRead = {}",
                        bytesRead.get(), e);
                reliableResourceStatus = new ReliableResourceStatus(
                        DownloadStatus.PRODUCT_INPUT_STREAM_EXCEPTION, bytesRead.get());
                return reliableResourceStatus;
            }
            LOGGER.trace("AFTER read() - n = {}", n);
            if (n == END_OF_FILE) {
                break;
            }

            // Synchronized to prevent being interrupted in the middle of writing to the
            // OutputStreams
            synchronized(lock) {
                
                // If download was interrupted or canceled break now so that the bytesRead count does
                // not
                // get out of sync with the bytesWritten counts. If this count gets out of sync
                // then potentially the output streams will be one chunk off from the input stream
                // when a retry is attempted and the InputStream is skipped forward.
                if (cancelDownload || interruptDownload || Thread.interrupted()) {
                    LOGGER.debug("Breaking from download loop due to cancel or interrupt received");
                    if (reliableResourceStatus != null) {
                        reliableResourceStatus
                                .setMessage("Breaking from download loop due to cancel or interrupt received");
                    }
                    return reliableResourceStatus;
                }
    
                bytesRead.addAndGet(n);
    
                if (cacheFileOutputStream != null) {
                    try {
                        cacheFileOutputStream.write(buffer, 0, n);
                    } catch (IOException e) {
                        LOGGER.info("IOException during write to cached file's OutputStream", e);
                        reliableResourceStatus = new ReliableResourceStatus(
                                DownloadStatus.CACHED_FILE_OUTPUT_STREAM_EXCEPTION, bytesRead.get());
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
                        reliableResourceStatus = new ReliableResourceStatus(
                                DownloadStatus.CLIENT_OUTPUT_STREAM_EXCEPTION, bytesRead.get());
                    }
                }
    
    
                // Return status here so that each stream can be attempted to be updated regardless of
                // which one might have had an exception
                if (reliableResourceStatus != null) {
                    return reliableResourceStatus;
                }
            }
            LOGGER.trace("chunkCount = {},  bytesRead = {}", chunkCount, bytesRead.get());
        }

        if (!interruptDownload && !cancelDownload && !Thread.interrupted()) {
            LOGGER.debug("Entire file downloaded successfully");
            reliableResourceStatus = new ReliableResourceStatus(
                    DownloadStatus.RESOURCE_DOWNLOAD_COMPLETE, bytesRead.get());
            reliableResourceStatus.setMessage("Download completed successfully");
        }

        return reliableResourceStatus;
    }
}
