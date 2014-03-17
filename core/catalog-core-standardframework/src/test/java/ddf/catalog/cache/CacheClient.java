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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheClient implements Callable<ByteArrayOutputStream> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CacheClient.class);
    
    private InputStream inputStream;
    private int chunkSize;
    private int simulatedCancelChunkCount;
    
    
    public CacheClient(InputStream inputStream, int chunkSize) {
        this(inputStream, chunkSize, -1);
    }
    
    public CacheClient(InputStream inputStream, int chunkSize, int simulatedCancelChunkCount) {
        this.inputStream = inputStream;
        this.chunkSize = chunkSize;
        this.simulatedCancelChunkCount = simulatedCancelChunkCount;
    }
    
    @Override
    public ByteArrayOutputStream call() {
        return clientRead(chunkSize, inputStream, simulatedCancelChunkCount);
    }
    
    // Simulates client/endpoint reading product from piped input stream
    public ByteArrayOutputStream clientRead(int chunkSize, InputStream pis, int simulatedCancelChunkCount) {
        long size = 0;
        byte[] buffer = new byte[chunkSize];
        int chunkCount = 0;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {            
            int n = 0;
            while (true) {
                chunkCount++;
                n = pis.read(buffer);
                if (n == -1) {
                    break;
                }
                bos.write(buffer, 0, n);
                size += n;
                if (simulatedCancelChunkCount == chunkCount) {
                    LOGGER.info("Simulating client canceling product retrieval");
                    break;
                }
            }
            IOUtils.closeQuietly(pis);
            IOUtils.closeQuietly(bos);
        } catch (IOException e) {
            LOGGER.error("Exception", e);
        }
        LOGGER.info("Client DONE - size = " + size);
        return bos;
    }
}
