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
package org.codice.ddf.catalog.harvest.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.codice.ddf.catalog.harvest.Harvester;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A {@link Harvester} who is capable of polling a remote source for content. */
public abstract class PollingHarvester implements Harvester {

  private static final Logger LOGGER = LoggerFactory.getLogger(PollingHarvester.class);

  private final long pollInterval;

  private final TimeUnit timeUnit;

  private ScheduledExecutorService executorService;

  /**
   * Creates a new {@code PollingHarvester}. The {@link #init()} method must be called to start
   * polling.
   *
   * @param pollInterval poll interval in seconds
   */
  public PollingHarvester(long pollInterval) {
    this(pollInterval, TimeUnit.SECONDS);
  }

  /**
   * Creates a new {@code PollingHarvester}. The {@link #init()} method must be called to start
   * polling.
   *
   * @param pollInterval poll interval
   * @param timeUnit {@link TimeUnit} of time
   */
  public PollingHarvester(long pollInterval, TimeUnit timeUnit) {
    this.pollInterval = pollInterval;
    this.timeUnit = timeUnit;
  }

  /** Called every interval of polling. */
  public abstract void poll();

  /**
   * Starts the {@link ScheduledExecutorService} which will call the {@link #poll()} method for
   * every interval defined by {@code pollInterval}.
   */
  public void init() {
    executorService =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory(
                "org.codice.ddf.catalog.harvest.pollingHarvester"));
    executorService.scheduleAtFixedRate(this::poll, 5, pollInterval, timeUnit);
  }

  /** Tears down the {@link ScheduledExecutorService}. */
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
}
