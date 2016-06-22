/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.platform.util;

import java.io.IOException;
import java.io.OutputStream;

import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;

/**
 * TemporaryFileBackedOutputStream buffers the written data to memory of a temporary file, and
 * makes the data available as a ByteSource. This class will make sure the temporary file is
 * deleted when {@link #close()} is called. The method {@link #asByteSource()} should not be
 * called after {@link #close()} is called.
 */
public class TemporaryFileBackedOutputStream extends OutputStream {

    private static final int DEFAULT_THRESHOLD = 1000000;

    private final FileBackedOutputStream fileBackedOutputStream;

    private boolean isClosed = false;

    /**
     * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
     */
    public TemporaryFileBackedOutputStream(int fileThreshold) {
        this.fileBackedOutputStream = new FileBackedOutputStream(fileThreshold);
    }

    /**
     * Use a file threshold size of {@link #DEFAULT_THRESHOLD}.
     */
    public TemporaryFileBackedOutputStream() {
        this(DEFAULT_THRESHOLD);
    }

    @Override
    public void write(int b) throws IOException {
        checkIsClosed();
        fileBackedOutputStream.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("byte array must be non-null");
        }
        if (off < 0) {
            throw new IndexOutOfBoundsException("off is negative");
        }
        if (len < 0) {
            throw new IndexOutOfBoundsException("len is negative");
        }
        if (off + len > b.length) {
            throw new IndexOutOfBoundsException("off+len is greater than array length");
        }
        checkIsClosed();
        fileBackedOutputStream.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
        if (isClosed) {
            return;
        }
        try {
            fileBackedOutputStream.close();
            fileBackedOutputStream.reset();
        } finally {
            isClosed = true;
        }
    }

    @Override
    public void flush() throws IOException {
        if (isClosed) {
            return;
        }
        fileBackedOutputStream.flush();
    }

    /**
     * Returns a readable {@link ByteSource} view of the data that has been written to this stream.
     * Must not be called after {@link #close()} is called, otherwise an {@link IllegalStateException}
     * will be thrown.
     *
     * @return ByteSource of the data
     * @throws IOException throws an exception if the stream is closed
     */
    public ByteSource asByteSource() throws IOException {
        checkIsClosed();
        return fileBackedOutputStream.asByteSource();
    }

    /**
     * Throw an exception if the stream is closed.
     *
     * @throws IOException
     */
    private void checkIsClosed() throws IOException {
        if (isClosed) {
            throw new IOException("stream closed");
        }
    }

}
