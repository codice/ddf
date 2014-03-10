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

    private String name;

    private InputStream is;

    // Parameters to configure failures for testing
    private int invocationCountToThrowIOException = -1;

    private int invocationCountToTimeout = -1;

    public MockInputStream(String name) {
        this.name = name;
        try {
            this.is = new FileInputStream(name);
        } catch (FileNotFoundException e) {
            LOGGER.error("FileNotFoundException", e);
        }
    }

    public void setInvocationCountToThrowIOException(int count) {
        invocationCountToThrowIOException = count;
    }

    public void setInvocationCountToTimeout(int count) {
        invocationCountToTimeout = count;
    }

    @Override
    public int read() throws IOException {
        invocationCount++;
        return is.read();
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        invocationCount++;
        LOGGER.info("invocationCount = " + invocationCount);
        if (invocationCount == invocationCountToThrowIOException) {
            LOGGER.info("Simulating read exception by closing inputstream");
            is.close(); // by closing stream, when try to read from it an IOException will be thrown
        } else if (invocationCount == invocationCountToTimeout) {
            try {
                Thread.sleep(2000); // sleep longer than the timeout for each chunk to be read
            } catch (InterruptedException e) {
            }
        }
        return super.read(buffer);
    }

    @Override
    public int read(byte[] buffer, int startPos, int len) throws IOException {
        invocationCount++;
        return super.read(buffer, startPos, len);
    }

    public void restart(long numBytes) {
        try {
            this.is = new FileInputStream(name);
            this.is.skip(numBytes);
        } catch (FileNotFoundException e) {
            LOGGER.error("FileNotFoundException", e);
        } catch (IOException e) {
            LOGGER.error("FileNotFoundException", e);
        }
    }
}
