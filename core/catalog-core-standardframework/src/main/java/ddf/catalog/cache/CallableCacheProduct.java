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
package ddf.catalog.cache;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CallableCacheProduct is responsible for caching a product to the file system. It is a @Callable
 * that is started via a @Future by the @CachedResource class.
 * 
 * CallableCacheProduct reads from a @TeeInputStream whose source is the product's
 * 
 * @InputStream. The @TeeInputStream while being read simultaneously sends the bytes read to a @PipedOutputStream
 *               (which is being read by a client using the connected
 * @PipedInputStream). This class will read bytes in chunks (whose size is specified by the caller)
 *                     until it either reaches the EOF or it is interrupted (either by an
 * @IOException or the @CachedResource).
 * 
 *              If the @InputStream is read until the EOF, then a -1 is returned indicating the
 *              entire stream was read successfully. otherwise, the number of bytes read thus far is
 *              returned so that the caller can react accordingly (e.g., reopen the @InputStream and
 *              skip forward that many bytes already read).
 * 
 * @author rodgers
 * 
 */
public class CallableCacheProduct implements Callable<CachedResourceStatus> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CallableCacheProduct.class);

    private static final int END_OF_FILE = -1;

    private InputStream input = null;

    private CounterOutputStream pos;

    private CounterOutputStream output = null;

    /**
     * Since this long is read by a different thread needs to be AtomicLong to make setting its
     * value thread-safe.
     */
    private AtomicLong bytesRead = new AtomicLong(0);

    private CachedResourceStatus cachedResourceStatus;

    private int chunkSize;

    private boolean interruptCaching = false;


    public CallableCacheProduct(InputStream input, CounterOutputStream pos,
            CounterOutputStream output, int chunkSize) {
        LOGGER.debug("Creating CallableCacheProduct");
        this.input = input;
        this.pos = pos;
        this.output = output;
        this.chunkSize = chunkSize;
    }

    public long getBytesRead() {
        return bytesRead.get();
    }

    // Called when a new Callable is created for new retry attempt at product retrieval
    // and the bytesRead from previous attempt need to be skipped forward. This method
    // allows caller to account for bytes read in previous attempt(s) in case another
    // retry is attempted and the cumulative amount of bytes read can be maintained.
    public void setBytesRead(long bytesRead) {
        LOGGER.debug("Setting bytesRead = {}", bytesRead);
        this.bytesRead.set(bytesRead);
    }

    public CachedResourceStatus getCachedResourceStatus() {
        return cachedResourceStatus;
    }

    public void setInterruptCaching(boolean interruptCaching) {
        LOGGER.debug("Setting interruptCaching = " + interruptCaching);
        this.interruptCaching = interruptCaching;

        // Set caching status here because it takes time for the Future running this
        // Callable to be canceled and the CachedResourceStatus may be retrieved before
        // the call() method is interrupted
        LOGGER.debug("Caching interrupted - returning {} bytes read", bytesRead);
        cachedResourceStatus = new CachedResourceStatus(CachingStatus.RESOURCE_CACHING_INTERRUPTED,
                bytesRead.get(), output.getBytesWritten(), pos.getBytesWritten());
    }

    @Override
    public CachedResourceStatus call() {
        int chunkCount = 0;

        byte[] buffer = new byte[chunkSize];
        int n = 0;

        while (!interruptCaching) {
            chunkCount++;

            // This read will block if the PipedOutputStream's circular buffer is filled
            // before the client reads from it via the PipedInputStream
            try {
                n = input.read(buffer);
            } catch (IOException e) {
                LOGGER.info("IOException during read of product's InputStream - bytesRead = {}",
                        bytesRead.get());
                cachedResourceStatus = new CachedResourceStatus(
                        CachingStatus.PRODUCT_INPUT_STREAM_EXCEPTION, bytesRead.get(),
                        output.getBytesWritten(), pos.getBytesWritten());
                return cachedResourceStatus;
            }
            LOGGER.trace("AFTER read() - n = {}", n);
            if (n == END_OF_FILE) {
                break;
            }

            // Synchronized to prevent being interrupted in the middle of writing to the
            // OutputStreams
            synchronized (this) {
                
                // If caching was interrupted break now so that the bytesRead count does not
                // get out of sync with the bytesWritten counts. If this count gets out of sync
                // then potentially the output streams will be one chunk off from the input stream
                // when a retry is attempted and the InputStream is skipped forward.
                if (interruptCaching) {
                    LOGGER.debug("Breaking from caching loop due to interrupt received");
                    LOGGER.trace("chunkCount = {},  bytesRead = {},  fosBytesWritten = {},  posBytesWritten = {}", 
                            chunkCount, bytesRead.get(), output.getBytesWritten(), pos.getBytesWritten());
                    break;
                }

                bytesRead.addAndGet(n);

                if (output != null) {
                    try {
                        output.write(buffer, 0, n);
                    } catch (IOException e) {
                        LOGGER.info("IOException during write to cached file's OutputStream");
                        cachedResourceStatus = new CachedResourceStatus(
                                CachingStatus.CACHED_FILE_OUTPUT_STREAM_EXCEPTION, bytesRead.get(),
                                output.getBytesWritten(), pos.getBytesWritten());
                        return cachedResourceStatus;
                    }
                }

                if (pos != null) {
                    try {
                        pos.write(buffer, 0, n);
                    } catch (IOException e) {
                        LOGGER.info("IOException during write to PipedOutputStream for client to read");
                        cachedResourceStatus = new CachedResourceStatus(
                                CachingStatus.PIPED_OUTPUT_STREAM_EXCEPTION, bytesRead.get(),
                                output.getBytesWritten(), pos.getBytesWritten());
                        return cachedResourceStatus;
                    }
                }
            }
            LOGGER.trace("chunkCount = {},  bytesRead = {},  fosBytesWritten = {},  posBytesWritten = {}", 
                    chunkCount, bytesRead.get(), output.getBytesWritten(), pos.getBytesWritten());
        }

        if (!interruptCaching) {
            LOGGER.debug("Returning -1 to indicate entire file cached successfully");
            cachedResourceStatus = new CachedResourceStatus(
                    CachingStatus.RESOURCE_CACHING_COMPLETE, bytesRead.get(),
                    output.getBytesWritten(), pos.getBytesWritten());
        }

        return cachedResourceStatus;
    }
}
