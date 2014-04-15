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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockInputStream extends InputStream {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(MockInputStream.class);

    private int invocationCount = 0;

    private InputStream is;

    // Parameters to configure failures for testing
    private int invocationCountToThrowIOException = -1;

    private int invocationCountToTimeout = -1;
    
    private int readDelay = 0;
    
    private boolean readSlow;
    

    public MockInputStream(String name) {
        this(name, false);
    }
    
    public MockInputStream(String name, boolean readSlow) {
        try {
            this.is = new FileInputStream(name);
        } catch (FileNotFoundException e) {
            LOGGER.error("FileNotFoundException", e);
        }
        
        this.readSlow = readSlow;
    }

    public void setInvocationCountToThrowIOException(int count) {
        invocationCountToThrowIOException = count;
    }

    public void setInvocationCountToTimeout(int count) {
        invocationCountToTimeout = count;
    }

    public void setReadDelay(int delay) {
        this.readDelay = delay;
    }
    
    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        invocationCount++;
        //LOGGER.debug("invocationCount = " + invocationCount);
        if (invocationCount == invocationCountToThrowIOException) {
            LOGGER.info("Simulating read exception by closing inputstream");
            // Simulates IOException while reading from the remote source.
            // By closing stream, when try to read from it an IOException will be thrown.
            is.close();
        } else if (invocationCount == invocationCountToTimeout) {
            LOGGER.info("Simulating read taking too long by sleeping for {} ms", readDelay);
            try {
                // Simulates read of InputStream chunk from remote source taking
                // longer than the timeout for each chunk to be read
                Thread.sleep(readDelay); 
            } catch (InterruptedException e) {
                LOGGER.info("Thread sleep interrupted");
                return 0;
            }
            LOGGER.info("Simulated read timeout completed");
        } else if (readSlow && readDelay > 0L) {
            try {
                // Slows down reading of product input stream so that client
                // has time to come up
                Thread.sleep(readDelay); 
            } catch (InterruptedException e) {
                LOGGER.info("Thread sleep interrupted");
                return 0;
            }
        }
        return super.read(buffer);
    }

    @Override
    public int read(byte[] buffer, int startPos, int len) throws IOException {
        return super.read(buffer, startPos, len);
    }

}
