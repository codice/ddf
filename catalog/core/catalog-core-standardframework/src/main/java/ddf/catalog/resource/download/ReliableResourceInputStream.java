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
package ddf.catalog.resource.download;

import com.google.common.io.ByteSource;
import com.google.common.io.CountingOutputStream;
import com.google.common.io.FileBackedOutputStream;
import ddf.catalog.operation.ResourceResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The @InputStream used by the client to read from the @FileBackedOutputStream being written to as
 * the resource is being downloaded.
 */
public class ReliableResourceInputStream extends InputStream {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReliableResourceInputStream.class);

  String downloadIdentifier;

  ResourceResponse resourceResponse;

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
   * @param countingFbos wrapped @FileBackedOutputStream that counts the number of bytes written so
   *     far
   * @param downloadState the current state of the resource's download
   */
  public ReliableResourceInputStream(
      FileBackedOutputStream fbos,
      CountingOutputStream countingFbos,
      DownloadManagerState downloadState,
      String downloadIdentifier,
      ResourceResponse resourceResponse) {
    this.fbos = fbos;
    fbosByteSource = fbos.asByteSource();
    this.countingFbos = countingFbos;
    this.downloadState = downloadState;
    this.downloadIdentifier = downloadIdentifier;
    this.resourceResponse = resourceResponse;
  }

  /**
   * Sets the @Callable and the @Future that started the @Callable that is populating the
   *
   * @param reliableResourceCallable
   * @param cachingFuture @FileBackedOutputStream is object is reading from.
   */
  public void setCallableAndItsFuture(
      ReliableResourceCallable reliableResourceCallable,
      Future<ReliableResourceStatus> downloadFuture) {
    this.reliableResourceCallable = reliableResourceCallable;
    this.downloadFuture = downloadFuture;
  }

  @Override
  public void close() throws IOException {
    LOGGER.debug("ENTERING: close() - fbosBytesRead = {}", fbosBytesRead);
    InputStream is = fbosByteSource.openStream();
    is.close();

    // If product download not yet complete, set cancellation of download
    // (ReliableResourceDownloadManager will determine if caching should continue)
    if (!downloadFuture.isDone()) {
      // Stop the caching thread. This is synchronized so that Callable can finish any writing to
      // OutputStreams before being canceled
      synchronized (reliableResourceCallable) {
        LOGGER.debug("Setting cancelDownload on ReliableResourceCallable thread");
        reliableResourceCallable.setCancelDownload(true);
        boolean status = downloadFuture.cancel(true);
        LOGGER.debug("cachingFuture cancelling status = {}", status);

        if (downloadState.getDownloadState() == DownloadManagerState.DownloadState.IN_PROGRESS) {
          downloadState.setDownloadState(DownloadManagerState.DownloadState.CANCELED);
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
    try (InputStream is = fbosByteSource.openStream()) {
      if (countingFbos.getCount() > fbosBytesRead) {
        long skipped = is.skip(fbosBytesRead);
        if (skipped != fbosBytesRead) {
          throw new IOException(
              "Tried to skip "
                  + fbosBytesRead
                  + " bytes but actually skipped "
                  + skipped
                  + " bytes");
        }
        byteRead = is.read();
        fbosBytesRead++;
      }
    }
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

    numBytesRead = readFromFbosInputStream(b, off, len);
    LOGGER.trace("First time reading inputstream, bytesRead is {}", numBytesRead);

    if (isFbosCompletelyRead(numBytesRead, fbosCount)) {
      LOGGER.debug("Sending EOF");
      // Client is done reading from this FileBackedOutputStream, so can
      // delete the backing file it created in the <INSTALL_DIR>/data/tmp directory
      fbos.reset();
    } else if (numBytesRead <= 0) {
      LOGGER.trace("Retry reading inputstream");
      LOGGER.trace(
          "numBytesRead <= 0 but client hasn't read all of the data from FBOS - block and read");
      while (downloadState.getDownloadState() == DownloadManagerState.DownloadState.IN_PROGRESS
          || (fbosCount >= fbosBytesRead
              && downloadState.getDownloadState() != DownloadManagerState.DownloadState.FAILED
              && downloadState.getDownloadState() != DownloadManagerState.DownloadState.CANCELED
              && downloadState.getDownloadState() != null)) {

        numBytesRead = readFromFbosInputStream(b, off, len);

        if (numBytesRead > 0) {
          LOGGER.trace("retry: numBytesRead = {}", numBytesRead);
          break;
        } else if (isFbosCompletelyRead(numBytesRead, fbosCount)) {
          LOGGER.debug("Got EOF - resetting FBOS");
          fbos.reset();
          break;
        } else {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            downloadState.setDownloadState(DownloadManagerState.DownloadState.CANCELED);

            Thread.currentThread().interrupt();
          }
        }
      }
      if (downloadState.getDownloadState() == DownloadManagerState.DownloadState.FAILED
          || downloadState.getDownloadState() == DownloadManagerState.DownloadState.CANCELED) {
        LOGGER.debug(
            "Throwing IOException because download failed or cancelled - cannot retrieve product");
        throw new IOException("Download failed or cancelled - cannot retrieve product");
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

  public long getBytesCached() {
    return countingFbos.getCount();
  }

  public DownloadManagerState getDownloadState() {
    return downloadState;
  }

  private boolean isFbosCompletelyRead(int numBytesRead, long fbosCount) {
    return (numBytesRead == -1
        && fbosCount == fbosBytesRead
        && (downloadState.getDownloadState() == DownloadManagerState.DownloadState.COMPLETED
            || downloadState.getDownloadState() == DownloadManagerState.DownloadState.FAILED));
  }

  private int readFromFbosInputStream(byte[] b, int off, int len) throws IOException {
    int numBytesRead;
    try (InputStream is = fbosByteSource.openStream()) {
      long skipped = is.skip(fbosBytesRead);
      if (skipped != fbosBytesRead) {
        throw new IOException(
            "Tried to skip " + fbosBytesRead + " bytes but actually skipped " + skipped + " bytes");
      }
      numBytesRead = is.read(b, off, len);
      LOGGER.trace("numBytesRead = {}", numBytesRead);
      if (numBytesRead > 0) {
        fbosBytesRead += numBytesRead;
      }
    }

    return numBytesRead;
  }
}
