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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.FileBackedOutputStream;

public class ProductDownloadClient implements Callable<ByteArrayOutputStream> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductDownloadClient.class);
    
    private InputStream inputStream;
    private int chunkSize;
    private int simulatedCancelChunkCount;
    private int simulateCacheFileExceptionChunkCount;
    private int simulateFbosExceptionChunkCount;
    private ReliableResourceDownloadManager downloadMgr;
    
    
    public ProductDownloadClient(InputStream inputStream, int chunkSize) {
        this(inputStream, chunkSize, -1);
    }
    
    public ProductDownloadClient(InputStream inputStream, int chunkSize, int simulatedCancelChunkCount) {
        this.inputStream = inputStream;
        this.chunkSize = chunkSize;
        this.simulatedCancelChunkCount = simulatedCancelChunkCount;
    }
    
    public void setSimulateCacheFileException(int chunkCount, ReliableResourceDownloadManager downloadMgr) {
        this.simulateCacheFileExceptionChunkCount = chunkCount;
        this.downloadMgr = downloadMgr;
    }
    
    public void setSimulateFbosException(int chunkCount, ReliableResourceDownloadManager downloadMgr) {
        this.simulateFbosExceptionChunkCount = chunkCount;
        this.downloadMgr = downloadMgr;
    }
    
    @Override
    public ByteArrayOutputStream call() {
        return clientRead(chunkSize, inputStream, simulatedCancelChunkCount);
    }
    
    // Simulates client/endpoint reading product from piped input stream
    public ByteArrayOutputStream clientRead(int chunkSize, InputStream is, int simulatedCancelChunkCount) {
        long size = 0;
        byte[] buffer = new byte[chunkSize];
        int chunkCount = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {            
            int n = 0;
            while (true) {
                chunkCount++;
                n = is.read(buffer);
                if (n == -1) {
                    break;
                }
                bos.write(buffer, 0, n);
                size += n;
                if (simulatedCancelChunkCount == chunkCount) {
                    LOGGER.info("Simulating client canceling product retrieval");
                    break;
                } else if (simulateCacheFileExceptionChunkCount == chunkCount) {
                    FileOutputStream cacheFileOutputStream = downloadMgr.getFileOutputStream();
                    try {
                        LOGGER.debug("Closing cacheFileOutputStream to simulate CACHED_FILE_OUTPUT_STREAM_EXCEPTION");
                        cacheFileOutputStream.close();
                    } catch (IOException e) {
                    }
                } else if (simulateFbosExceptionChunkCount == chunkCount) {
                    FileBackedOutputStream fbos = downloadMgr.getFileBackedOutputStream();
                    try {
                        LOGGER.debug("Closing FileBackedOutputStream to simulate CLIENT_OUTPUT_STREAM_EXCEPTION");
                        fbos.close();
                    } catch (IOException e) {
                        LOGGER.debug("Could not close FileBackedOutputStream");
                    }
                }
            }
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(bos);
        } catch (IOException e) {
            LOGGER.error("Exception", e);
        }
        LOGGER.info("Client DONE - size = " + size);
        return bos;
    }
}
