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
package org.codice.ddf.catalog.content.monitor;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.validation.constraints.NotNull;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DurableFileAlterationListener extends FileAlterationListenerAdaptor {

  public static final long DEFAULT_PERIOD = 5;

  public static final String CDM_FILE_CHECK_PERIOD_PROPERTY = "org.codice.ddf.cdm.fileCheckPeriod";

  private static final Logger LOGGER = LoggerFactory.getLogger(DurableFileAlterationListener.class);

  private Map<File, Pair<Long, WatchEvent.Kind<Path>>> fileMap = new ConcurrentHashMap<>();

  private ScheduledExecutorService executorService;

  private AbstractDurableFileConsumer consumer;

  public DurableFileAlterationListener(@NotNull AbstractDurableFileConsumer consumer) {
    this.consumer = consumer;
    executorService =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("directoryMonitorFileChecker"));
    this.startExecutor();
  }

  public DurableFileAlterationListener(
      @NotNull AbstractDurableFileConsumer consumer,
      @NotNull ScheduledExecutorService executorService) {
    this.consumer = consumer;
    this.executorService = executorService;
    this.startExecutor();
  }

  private void startExecutor() {
    long period = DEFAULT_PERIOD;
    try {
      period =
          Long.parseLong(
              System.getProperty(CDM_FILE_CHECK_PERIOD_PROPERTY, Long.toString(DEFAULT_PERIOD)));
      if (period < 1) {
        period = DEFAULT_PERIOD;
      }
    } catch (NumberFormatException e) {
      LOGGER.debug(
          "Invalid value for system property org.codice.ddf.cdm.fileCheckPeriod. Expected an integer but was {}. Defaulting to {}",
          System.getProperty(CDM_FILE_CHECK_PERIOD_PROPERTY),
          period);
    }
    executorService.scheduleAtFixedRate(this::checkFiles, 10, period, TimeUnit.SECONDS);
  }

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

  @Override
  public void onFileChange(File file) {
    if (!fileMap.containsKey(file)) {
      fileMap.put(file, new ImmutablePair<>(-1L, StandardWatchEventKinds.ENTRY_MODIFY));
    }
  }

  @Override
  public void onFileCreate(File file) {
    fileMap.put(file, new ImmutablePair<>(-1L, StandardWatchEventKinds.ENTRY_CREATE));
  }

  @Override
  public void onFileDelete(File file) {
    consumer.createExchangeHelper(file, StandardWatchEventKinds.ENTRY_DELETE);
  }

  void checkFiles() {
    List<File> completedFiles = new ArrayList<>();
    for (Map.Entry<File, Pair<Long, WatchEvent.Kind<Path>>> entry : fileMap.entrySet()) {
      if (entry.getKey().length() == entry.getValue().getKey()) {
        completedFiles.add(entry.getKey());
        consumer.createExchangeHelper(entry.getKey(), entry.getValue().getValue());
      } else {
        entry.setValue(new ImmutablePair<>(entry.getKey().length(), entry.getValue().getValue()));
      }
    }
    completedFiles.stream().forEach(file -> fileMap.remove(file));
  }
}
