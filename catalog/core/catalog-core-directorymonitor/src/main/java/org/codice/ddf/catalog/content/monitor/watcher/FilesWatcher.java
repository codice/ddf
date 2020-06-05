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
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Periodically calls {@link FileWatcher#check()} on all watched {@link FileWatcher}s. */
public class FilesWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(CDM_LOGGER_NAME);

  private static final long DEFAULT_POLL_TIME_SEC = 5;

  private static final String CDM_FILE_CHECK_PERIOD_PROPERTY = "org.codice.ddf.cdm.fileCheckPeriod";

  private ScheduledExecutorService executorService;

  private final Map<File, FileWatcher> watchers = new ConcurrentHashMap<>();

  private long executorPollTime;

  /**
   * Creates a new {@link FileWatcher} with the system property {@code
   * org.codice.ddf.cdm.fileCheckPeriod} poll time or {@link #DEFAULT_POLL_TIME_SEC} if not
   * available.
   */
  public FilesWatcher() {
    executorPollTime = getPollTimeOrDefault();
    init();
  }

  /**
   * Creates a new {@link FileWatcher} with the given polling interval.
   *
   * @param pollTime polling interval in seconds
   */
  public FilesWatcher(long pollTime) {
    executorPollTime = pollTime;
    init();
  }

  /**
   * Submits a {@link FileWatcher} whose {@link FileWatcher#check()} will be called periodically.
   *
   * @param fileWatcher to submit for checking
   */
  public void watch(FileWatcher fileWatcher) {
    watchers.put(fileWatcher.getWatchedFile(), fileWatcher);
  }

  private void init() {
    executorService =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("cdmFileWatcher"));

    executorService.scheduleAtFixedRate(this::checkFiles, 10, executorPollTime, TimeUnit.SECONDS);
  }

  /**
   * Calls {@link FileWatcher#check()} on all registered file watchers. For anyone that returns
   * true, the {@link FileWatcher} is removed from this {@code FilesWatcher}.
   */
  private void checkFiles() {
    for (Entry<File, FileWatcher> entry : watchers.entrySet()) {
      File watchedFile = entry.getKey();
      FileWatcher watcher = entry.getValue();

      if (watcher.check()) {
        watchers.remove(watchedFile);
      }
    }
  }

  /** Must be called to clean up ExecutorService resources. */
  public void destroy() {
    // copied from the Executor javadocs
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
          LOGGER.debug("Error terminating scheduled executor service");
      }
    } catch (InterruptedException ie) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private long getPollTimeOrDefault() {
    long pollTime;
    try {
      pollTime = Long.parseLong(System.getProperty(CDM_FILE_CHECK_PERIOD_PROPERTY));
    } catch (NumberFormatException e) {
      pollTime = DEFAULT_POLL_TIME_SEC;
      LOGGER.debug(
          "Invalid or no [{}] property as long. Defaulting to default period [{}]",
          CDM_FILE_CHECK_PERIOD_PROPERTY,
          DEFAULT_POLL_TIME_SEC);
    }
    return pollTime;
  }
}
