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

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.ByteSource;
import com.google.common.io.CountingOutputStream;
import com.google.common.io.FileBackedOutputStream;

/**
 * The @InputStream used by the client to read from the @FileBackedOutputStream being written to as the
 * resource is being downloaded.
 *
 */
public class ReliableResourceInputStream extends InputStream {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReliableResourceInputStream.class);
    
    private Future<ReliableResourceStatus> downloadFuture;

    // The Callable that is writing to the FileBackedOutputStream that this object is reading from
    private ReliableResourceCallable reliableResourceCallable;
    
    // The current state of the resource's download, e.g., IN_PROGRESS, COMPLETED, FAILED, etc.
    private DownloadManagerState downloadState;

    // The FileBackedOutputStream that this object is reading from
    private FileBackedOutputStream fbos;

    private CountingOutputStream countingFbos;

    private ByteSource fbosByteSource;

    private long fbosBytesRead = 0;
    
    // Indicates if this InputStream is closed or not
    private boolean streamClosed = false;
    
    
    /**
     * @param fbos the @FileBackedOutputStream this object will read from
     * @param countingFbos wrapped @FileBackedOutputStream that counts the number of bytes written so far
     * @param downloadState the current state of the resource's download
     */
    public ReliableResourceInputStream(FileBackedOutputStream fbos,
            CountingOutputStream countingFbos, DownloadManagerState downloadState) {
        this.fbos = fbos;
        fbosByteSource = fbos.asByteSource();
        this.countingFbos = countingFbos;
        this.downloadState = downloadState;
    }
    
    /**
     * Sets the @Callable and the @Future that started the @Callable that is populating the
     * @FileBackedOutputStream is object is reading from.
     * 
     * @param reliableResourceCallable
     * @param cachingFuture
     */
    public void setCallableAndItsFuture(ReliableResourceCallable reliableResourceCallable,
            Future<ReliableResourceStatus> downloadFuture) {
        this.reliableResourceCallable = reliableResourceCallable;
        this.downloadFuture = downloadFuture;
    }

    @Override
    public void close() throws IOException {
        LOGGER.debug("ENTERING: close() - fbosBytesRead = {}", fbosBytesRead);
        InputStream is = fbosByteSource.openStream();
        is.close();

        // Stop caching the product unless admin specifically
        // set option to continue caching even if client cancels
        // the product download
        if (downloadState.isCacheEnabled() && !downloadState.isContinueCaching()) {
            if (!downloadFuture.isDone()) {
                // Stop the caching thread
                // synchronized so that Callable can finish any writing to OutputStreams before being canceled
                synchronized(reliableResourceCallable) {
                    LOGGER.debug("Setting cancelDownload on ReliableResourceCallable thread");
                    reliableResourceCallable.setCancelDownload(true);
                    boolean status = downloadFuture.cancel(true);
                    LOGGER.debug("cachingFuture cancelling status = {}", status);
                }
            }
        }

        // Resetting the FileBackedOutputStream should delete the tmp file
        // it created.
        LOGGER.debug("Resetting FBOS");
        fbos.reset();

        streamClosed = true;
    }
    
    public boolean isClosed() {
        return streamClosed;
    }

    @Override
    public int read() throws IOException {
        LOGGER.trace("ENTERING: read()");
        int byteRead = 0;
        InputStream is = fbosByteSource.openStream();
        if (countingFbos.getCount() > fbosBytesRead) {
            is.skip(fbosBytesRead);
            byteRead = is.read();
            fbosBytesRead++;
        }
        is.close();
        return byteRead;
    }

    @Override
    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        } else if (off < 0 || len < 0 || len > b.length - off) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return 0;
        }

        int numBytesRead = 0;

        long fbosCount = countingFbos.getCount();
        if (fbosCount != fbosBytesRead) {
            LOGGER.trace("fbos count = {}, fbosBytesRead = {}", fbosCount, fbosBytesRead);
        }

        // More bytes written to FileBackedOutputStream than have been read by the client -
        // ok to skip and do a read
        if (fbosCount > fbosBytesRead) {
            numBytesRead = readFromFbosInputStream(b, off, len);
        } else if (fbosCount > 0) {
            // bytes have been written to the FileBackedOutputStream
            numBytesRead = readFromFbosInputStream(b, off, len);
            if (isFbosCompletelyRead(numBytesRead, fbosCount)) {
                LOGGER.debug("Sending EOF");
                // Client is done reading from this FileBackedOutputStream, so can
                // delete the backing file it created in the <INSTALL_DIR>/data/tmp directory
                fbos.reset();
            } else if (numBytesRead <= 0) {
                LOGGER.debug("numBytesRead <= 0 but client hasn't read all of the data from FBOS - block and read");
                while (downloadState.getDownloadState() == DownloadManagerState.DownloadState.IN_PROGRESS || 
                        (fbosCount >= fbosBytesRead && downloadState.getDownloadState() != DownloadManagerState.DownloadState.FAILED && downloadState.getDownloadState() != null)) {
                    numBytesRead = readFromFbosInputStream(b, off, len);
                    if (numBytesRead > 0) {
                        LOGGER.debug("retry: numBytesRead = {}", numBytesRead);
                        break;
                    } else if (isFbosCompletelyRead(numBytesRead, fbosCount)) {
                        LOGGER.debug("Got EOF - resetting FBOS");
                        fbos.reset();
                        break;
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }
                if (downloadState.getDownloadState() == DownloadManagerState.DownloadState.FAILED) {
                    LOGGER.debug("Throwing IOException because download failed - cannot retrieve product");
                    throw new IOException("Download failed - cannot retrieve product");
                }
            }
        }

        return numBytesRead;
    }
    
    /**
     * Returns the number of bytes read thus far from the @FileBackedOutputStream
     * 
     * @return
     */
    public long getBytesRead() {
        return fbosBytesRead;
    }
    
    private boolean isFbosCompletelyRead(int numBytesRead, long fbosCount) {
        if (numBytesRead == -1 && fbosCount == fbosBytesRead &&
                (downloadState.getDownloadState() == DownloadManagerState.DownloadState.COMPLETED || 
                 downloadState.getDownloadState() == DownloadManagerState.DownloadState.FAILED)) {
            return true;
        }
        
        return false;
    }

    private int readFromFbosInputStream(byte[] b, int off, int len) throws IOException {
        InputStream is = fbosByteSource.openStream();
        is.skip(fbosBytesRead);
        int numBytesRead = is.read(b, off, len);
        LOGGER.trace("numBytesRead = {}", numBytesRead);
        if (numBytesRead > 0) {
            fbosBytesRead += numBytesRead;
        }

        is.close();

        return numBytesRead;
    }
}
