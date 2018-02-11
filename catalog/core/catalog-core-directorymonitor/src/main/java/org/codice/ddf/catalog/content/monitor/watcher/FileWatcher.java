package org.codice.ddf.catalog.content.monitor.watcher;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors the size of files. When a file's length is considered "stable" (non changing between
 * poll times), the file's corresponding callback will be called.
 */
public class FileWatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileWatcher.class);

  private static final long DEFAULT_POLL_TIME_SEC = 5;

  private static final String CDM_FILE_CHECK_PERIOD_PROPERTY = "org.codice.ddf.cdm.fileCheckPeriod";

  private Map<File, Consumer<File>> fileMap;

  private Map<File, Long> fileSizeMap;

  private ScheduledExecutorService executorService;

  private long executorPollTime;

  /**
   * Creates a FileWather with default poll time as defined by the {@code
   * org.codice.ddf.cdm.fileCheckPeriod} system property, or 5 seconds if it is not defined.
   */
  public FileWatcher() {
    this.executorPollTime = getPollTime();
    fileMap = new HashMap<>();
    fileSizeMap = new HashMap<>();
    init();
  }

  /**
   * Creates a FileWatcher with the given poll time.
   *
   * @param pollTime pollTime in seconds
   */
  public FileWatcher(long pollTime) {
    this.executorPollTime = pollTime;
    fileMap = new HashMap<>();
    fileSizeMap = new HashMap<>();
    init();
  }

  public void submit(File file, Consumer<File> callback) {
    if (!fileMap.containsKey(file)) {
      fileMap.put(file, callback);
      fileSizeMap.put(file, file.length());
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

  private void init() {
    executorService =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("cdmFileWatcher"));

    executorService.scheduleAtFixedRate(this::checkFiles, 10, executorPollTime, TimeUnit.SECONDS);
  }

  private void checkFiles() {
    for (Entry<File, Consumer<File>> entry : fileMap.entrySet()) {
      File entryFile = entry.getKey();

      if (fileSizeMap.get(entryFile) != null) {
        if (entryFile.length() == fileSizeMap.get(entryFile)) {
          Consumer<File> fileCallback = entry.getValue();
          fileCallback.accept(entryFile);

          fileMap.remove(entryFile);
          fileSizeMap.remove(entryFile);
        } else {
          fileSizeMap.put(entryFile, entryFile.length());
        }
      }
    }
  }

  private long getPollTime() {
    long pollTime;
    try {
      pollTime = Long.parseLong(System.getProperty(CDM_FILE_CHECK_PERIOD_PROPERTY));
    } catch (NumberFormatException e) {
      pollTime = DEFAULT_POLL_TIME_SEC;
      LOGGER.debug("Default to default period [{}]", DEFAULT_POLL_TIME_SEC);
    }
    return pollTime;
  }
}
