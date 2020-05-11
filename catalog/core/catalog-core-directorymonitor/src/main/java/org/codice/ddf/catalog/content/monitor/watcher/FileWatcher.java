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
package org.codice.ddf.catalog.content.monitor.watcher;

import static ddf.catalog.Constants.CDM_LOGGER_NAME;

import java.io.File;
import java.util.function.Consumer;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Watches the size of a file. When a file's length is considered "stable" (non changing between
 * calls to {@link #check()}), the file's corresponding callback will be called.
 */
public class FileWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(CDM_LOGGER_NAME);

  private final File watchedFile;

  private final Consumer<File> fileCallback;

  private long lastFileSize = -1L;

  /**
   * Creates a FileWatcher. The file's callback will be called when a file is considered "stable".
   * See {@link #check()} for a definition of stable.
   *
   * @param watchedFile the file to watch
   * @param fileCallback the file's callback
   */
  public FileWatcher(File watchedFile, Consumer<File> fileCallback) {
    Validate.notNull(watchedFile, "argument {watchedFile} cannot be null");
    Validate.notNull(fileCallback, "argument {fileCallback} cannot be null");

    this.watchedFile = watchedFile;
    this.fileCallback = fileCallback;
  }

  /** @return the file being watched */
  public File getWatchedFile() {
    return watchedFile;
  }

  /**
   * Checks if the watched file's size has changed since the last call to check. Every time this
   * method is called, the watched file's size will be cached and compared to the file's size in the
   * next call of this method. If the files length equals its cached value, it is considered
   * "stable".
   *
   * <p>A file is considered stable when its size does not vary between 2 calls to {@code check}, at
   * which point the files callback will be invoked.
   *
   * @return true if file size is considered "stable", false otherwise
   */
  public boolean check() {
    long currentSize = watchedFile.length();
    if (currentSize == lastFileSize) {
      LOGGER.debug("File [{}]'s size is stabilized.", watchedFile.getName());
      fileCallback.accept(watchedFile);
      return true;
    } else {
      lastFileSize = currentSize;
      return false;
    }
  }
}
