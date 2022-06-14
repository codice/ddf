/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.platform.util;

import com.google.common.io.ByteSource;
import com.google.common.io.FileBackedOutputStream;
import dev.failsafe.Failsafe;
import dev.failsafe.RetryPolicy;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TemporaryFileBackedOutputStream buffers the written data to memory of a temporary file, and makes
 * the data available as a ByteSource. This class will make sure the temporary file is deleted when
 * {@link #close()} is called. The method {@link #asByteSource()} should not be called after {@link
 * #close()} is called.
 */
public class TemporaryFileBackedOutputStream extends OutputStream {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(TemporaryFileBackedOutputStream.class);

  private static final int DEFAULT_THRESHOLD = 1000000;

  private static final int MAX_RETRY_ATTEMPTS = 5;

  private static final long INITIAL_RETRY_SLEEP = 1;

  private static final long MAX_DELAY = 30;

  private final FileBackedOutputStream fileBackedOutputStream;

  private boolean isClosed = false;

  /**
   * @param fileThreshold the number of bytes before the stream should switch to buffering to a file
   */
  public TemporaryFileBackedOutputStream(int fileThreshold) {
    this.fileBackedOutputStream = new FileBackedOutputStream(fileThreshold);
  }

  /** Use a file threshold size of {@link #DEFAULT_THRESHOLD}. */
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
      reset();
    } finally {
      isClosed = true;
    }
  }

  /** Reset fileBackedOutputStream and retry if it fails. */
  @SuppressWarnings("unchecked")
  private void reset() {

    RetryPolicy<Object> retryPolicy =
        RetryPolicy.builder()
            .handle(IOException.class)
            .withBackoff(Duration.ofSeconds(INITIAL_RETRY_SLEEP), Duration.ofSeconds(MAX_DELAY))
            .withMaxRetries(MAX_RETRY_ATTEMPTS)
            .onFailedAttempt(
                throwable -> LOGGER.debug("failed to delete temporary file, will retry", throwable))
            .build();

    Failsafe.with(retryPolicy)
        .onFailure(throwable -> LOGGER.debug("failed to delete temporary file", throwable))
        .run(fileBackedOutputStream::reset);
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
