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
package org.codice.ddf.catalog.sourcepoller;

import static org.apache.commons.lang3.Validate.notNull;

import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nullable;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link #setPollIntervalMinutes} starts the process to periodically call {@link
 * Poller#pollItems(long, TimeUnit, ImmutableMap)}.
 *
 * <p>This class must be extended to implement the {@link #getValueLoaders()} method. Extensions of
 * this class can also specify {@link K}/{@link V}. This class contains the common logic to
 * periodically poll the injected {@link Poller} (and handle failures) regardless of these
 * variables.
 *
 * @param <K> type of the unique key used to identify the items to poll, e.g. {@link SourceKey}
 * @param <V> type of the value returned when an item is polled
 */
abstract class PollerRunner<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PollerRunner.class);

  private static final long MINIMUM_POLL_INTERVAL_MINUTES = 1;

  private final Poller<K, V> poller;

  private final ScheduledExecutorService scheduledExecutorService;

  @Nullable private volatile Future scheduledPollingFuture;

  private volatile long pollIntervalMinutes;

  private final Lock startPollingLock = new ReentrantLock();

  /**
   * {@link #startPolling(long)} must be called to schedule the polling.
   *
   * @param poller which to poll periodically at the configured poll interval
   * @param pollIntervalMinutes the poll interval. The poll interval can be updated via {@link
   *     #setPollIntervalMinutes(long)}.
   * @param scheduledExecutorService the {@link ScheduledExecutorService} used to schedule the poll
   *     task to execute at the configured poll interval
   * @throws IllegalArgumentException if {@code pollIntervalMinutes} is less than {@value
   *     #MINIMUM_POLL_INTERVAL_MINUTES} or if the {@code scheduledExecutorService} {@link
   *     ScheduledExecutorService#isShutdown()}
   * @throws NullPointerException if any of the parameters are {@code null}
   */
  protected PollerRunner(
      final Poller<K, V> poller,
      final long pollIntervalMinutes,
      final ScheduledExecutorService scheduledExecutorService) {
    this.poller = notNull(poller);
    if (scheduledExecutorService.isShutdown()) {
      throw new IllegalArgumentException("scheduledExecutorService argument may not be shutdown");
    }
    this.scheduledExecutorService = scheduledExecutorService;
    this.pollIntervalMinutes = validPollIntervalMinutes(pollIntervalMinutes);
  }

  /** @throws IllegalStateException if unable to schedule polling */
  public void init() {
    startPolling(pollIntervalMinutes);
  }

  /** Stops the {@link PollerRunner} and all {@link java.util.concurrent.ExecutorService}s */
  public void destroy() {
    MoreExecutors.shutdownAndAwaitTermination(scheduledExecutorService, 5, TimeUnit.SECONDS);
    LOGGER.debug("Destroyed PollerRunner");
  }

  /**
   * Updates the poll interval and then calls {@link #startPolling(long)}
   *
   * <p>WARNING:
   *
   * <p>The delay until the {@link Poller#getCachedValue(Object)} will begin reporting a new or
   * updated value is determined by the how often {@link Poller#pollItems(long, TimeUnit,
   * ImmutableMap)} is called.
   *
   * <p>This delay will be a maximum of 2*{@code pollIntervalMinutes} (or longer if threads are
   * backed-up in the {@link Poller}). Therefore, <b>the {@code pollIntervalMinutes} should not be
   * set to a value which results in an unacceptable maximum delay</b>.
   *
   * @param pollIntervalMinutes the interval (in minutes) of the new schedule
   * @throws IllegalArgumentException if {@code pollIntervalMinutes} is less than {@value
   *     #MINIMUM_POLL_INTERVAL_MINUTES}
   * @throws IllegalStateException if {@link #scheduledExecutorService} {@link
   *     ScheduledExecutorService#isShutdown()} or if unable to schedule polling
   */
  public synchronized void setPollIntervalMinutes(final long pollIntervalMinutes) {
    if (scheduledExecutorService.isShutdown()) {
      final String message = "Unable to start polling because scheduledExecutorService is shutdown";
      LOGGER.warn("{}. Try restarting the system.", message);
      throw new IllegalStateException(message);
    }

    startPolling(validPollIntervalMinutes(pollIntervalMinutes));
    this.pollIntervalMinutes = pollIntervalMinutes;
  }

  /**
   * Anything thrown by this method will cause the poll to fail, but future polls will still be
   * executed.
   *
   * @return a non-null {@link ImmutableMap} of current keys and a loader {@link Callable} to get
   *     the current value for each key, which may be empty
   */
  protected abstract ImmutableMap<K, Callable<V>> getValueLoaders();

  /** @throws IllegalStateException if unable to schedule polling */
  private void startPolling(final long pollIntervalMinutes) {
    if (!startPollingLock.tryLock()) {
      final String message =
          "Unable to start the schedule. Multiple threads may not start the schedule at the same time.";
      LOGGER.warn("{} The Poller will not be periodically updated. Try restarting the system.");
      throw new IllegalStateException(message);
    }

    try {
      doStartPolling(pollIntervalMinutes);
    } finally {
      startPollingLock.unlock();
    }
  }

  /** @throws IllegalStateException if unable to schedule polling */
  private void doStartPolling(long pollIntervalMinutes) {
    if (scheduledPollingFuture != null) {
      scheduledPollingFuture.cancel(true);
      LOGGER.debug(
          "Stopped the scheduled process to poll all of the current items at a fixed rate");
    }

    // From the ScheduledExecutorService#scheduleAtFixedRate javadoc: "If any execution of this task
    // takes longer than its period, then subsequent executions may start late, but will not
    // concurrently execute."
    try {
      scheduledPollingFuture =
          scheduledExecutorService.scheduleAtFixedRate(
              () -> poll(pollIntervalMinutes), 0, pollIntervalMinutes, TimeUnit.MINUTES);
    } catch (final RejectedExecutionException e) {
      final String message =
          "Unable to schedule polling at at a fixed rate of " + pollIntervalMinutes + " minute(s)";
      LOGGER.warn("{}. Try restarting the system.", message, e);
      throw new IllegalStateException(message, e);
    }
    LOGGER.debug(
        "Successfully scheduled a process to poll all of the current items at a fixed rate of {} minute(s), where polls will time out at {} minute(s)",
        pollIntervalMinutes,
        pollIntervalMinutes);
  }

  @SuppressWarnings("squid:S1181" /*Catching throwable intentionally*/)
  private void poll(final long pollIntervalMinutes) {
    try {
      final ImmutableMap<K, Callable<V>> itemsToPoll = getValueLoaders();
      final int currentItemsCount = itemsToPoll.size();

      LOGGER.trace(
          "Starting a process to poll the {} current item(s), where polls will time out at {} minute(s)",
          currentItemsCount,
          pollIntervalMinutes);
      // If there are any unhandled exceptions in this method, all future calls will be suppressed.
      poller.pollItems(pollIntervalMinutes, TimeUnit.MINUTES, itemsToPoll);
      LOGGER.trace(
          "Successfully finished the process of polling the {} source(s)", currentItemsCount);
    } catch (InterruptedException e) {
      LOGGER.debug("A scheduled poll was interrupted.", pollIntervalMinutes, e);
      Thread.currentThread().interrupt();
    } catch (final VirtualMachineError e) {
      final String message = "A scheduled poll failed";
      LOGGER.debug(message, e);
      LOGGER.warn(
          "{}. See debug log for more details. The current items will NOT continue to be polled at a fixed rate of {} minutes. Try restarting the system.",
          message,
          pollIntervalMinutes);
      throw e;
    } catch (final PollerException e) {
      e.getCauses().forEach((k, t) -> LOGGER.debug("Unable to load a new value for {}", k, t));
      LOGGER.warn(
          "A scheduled poll failed. {}. See debug log for more details. The current items will continue to be polled at a fixed rate of {} minutes. Try restarting the system if future polls also fail.",
          e.getMessage(),
          pollIntervalMinutes);
    } catch (final Throwable e) {
      final String message = "A scheduled poll failed";
      LOGGER.debug(message, e);
      LOGGER.warn(
          "A scheduled poll failed. See debug log for more details. The current items will continue to be polled at a fixed rate of {} minutes. Try restarting the system if future polls also fail.",
          pollIntervalMinutes);
    }
  }

  /**
   * @throws IllegalArgumentException if {@code pollIntervalMinutes} is less than {@value
   *     #MINIMUM_POLL_INTERVAL_MINUTES}
   */
  private static long validPollIntervalMinutes(long pollIntervalMinutes) {
    Validate.isTrue(
        pollIntervalMinutes >= MINIMUM_POLL_INTERVAL_MINUTES,
        "pollIntervalMinutes argument may not be less than %d minutes. Not updating the poll interval.",
        MINIMUM_POLL_INTERVAL_MINUTES);
    return pollIntervalMinutes;
  }
}
