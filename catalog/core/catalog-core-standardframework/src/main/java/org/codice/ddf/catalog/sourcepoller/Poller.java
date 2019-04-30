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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Poller} is a cache where the entries are maintained (i.e. updated, added, removed)
 * manually by calling {@link #pollItems(long, TimeUnit, ImmutableMap)}.
 *
 * <p>The class can be extended to override the {@link #handleTimeout(Object)} and {@link
 * #handleException(Object, RuntimeException)} behavior and/or to specify {@link K}/{@link V}. This
 * class contains the common polling logic regardless of these variables.
 *
 * <p>Some important notes of this implementation, compared to previous Source Poller iterations:
 *
 * <ul>
 *   <li/>Any details about how to load a new value are not cached. See DDF-2789.
 *   <li/>The {@link Poller} does not pick up live value or key changes. {@link #pollItems(long,
 *       TimeUnit, ImmutableMap)} must be called to update the cache.
 *   <li/>The cache may only be accessed by one thread, to help prevent threading issues.
 *   <li/>The logging is improved to log at DEBUG when values change or there are errors when
 *       loading.
 * </ul>
 *
 * @param <K> the key type for the cache entries. Keys should be unique and comparable with {@link
 *     Object#equals(Object)} and {@link Object#hashCode()}.
 * @param <V> the value type for the key to store in the cache. The value may not be {@code null}.
 */
class Poller<K, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Poller.class);

  @VisibleForTesting protected static final long MINIMUM_TIMEOUT_MS = 1_000;

  private final ExecutorService pollThreadPool;

  private final ExecutorService pollTimeoutWatcherThreadPool;

  private final Lock pollItemsLock = new ReentrantLock();

  private final Cache<K, V> cache = CacheBuilder.newBuilder().maximumSize(1000).build();

  /**
   * {@link #pollItems(long, TimeUnit, ImmutableMap)} must be called to initialize the cache with
   * values.
   *
   * @param pollThreadPool {@link ExecutorService} used to execute the loader {@link Callable<V>}s
   *     in {@link #pollItems(long, TimeUnit, ImmutableMap)}
   * @param pollTimeoutWatcherThreadPool {@link ExecutorService} used to wait for the loader {@link
   *     Callable<V>}s in {@link #pollItems(long, TimeUnit, ImmutableMap)} to complete or time them
   *     out if they take longer than the given timeout
   * @throws NullPointerException if either argument is {@code null}
   * @throws IllegalArgumentException if either {@link ExecutorService} {@link
   *     ExecutorService#isShutdown()}
   */
  protected Poller(
      final ExecutorService pollThreadPool, final ExecutorService pollTimeoutWatcherThreadPool) {
    this.pollThreadPool = notShutdown(notNull(pollThreadPool));
    this.pollTimeoutWatcherThreadPool = notShutdown(notNull(pollTimeoutWatcherThreadPool));
  }

  /** Stops the {@link Poller} and all {@link java.util.concurrent.ExecutorService}s */
  public void destroy() {
    MoreExecutors.shutdownAndAwaitTermination(pollThreadPool, 5, TimeUnit.SECONDS);
    MoreExecutors.shutdownAndAwaitTermination(pollTimeoutWatcherThreadPool, 5, TimeUnit.SECONDS);
  }

  /**
   * This method does not block. This method is protected so that it may called by {@link
   * #handleTimeout(Object)} and {@link #handleException(Object, RuntimeException)} and accessed in
   * sub-classes.
   *
   * @return {@link Optional#empty()} if the {@code key} is unknown or if the poll has not yet
   *     completed for the {@code key}
   * @return {@link Optional} of the value for the {@code key} the last time that {@link
   *     #pollItems(long, TimeUnit, ImmutableMap)} started
   * @throws NullPointerException if the {@code key} is {@code null}
   */
  protected Optional<V> getCachedValue(final K key) {
    final V cachedValue = cache.getIfPresent(notNull(key));
    if (cachedValue == null) {
      LOGGER.debug(
          "{} is unknown. This only happens for keys that have not yet successfully completed a poll yet. Returning \"unknown\"",
          key);
      return Optional.empty();
    } else {
      LOGGER.trace("The cached value for {} is {}", key, cachedValue);
      return Optional.of(cachedValue);
    }
  }

  /**
   * This method updates the cache given the current keys and loader {@link Callable<V>}s. All of
   * the cache updates (i.e. updating values, adding entries, removing entries) happen in the same
   * thread. Multiple threads may not poll at the same time.
   *
   * <ol>
   *   <li>Any keys in the cache that are not in {@code itemsToPoll} are removed from the cache.
   *   <li>A process to get the new value for each of the keys in the {@code itemsToPoll} is started
   *       in parallel using the loader {@link Callable<V>}. Each {@link Callable<V>#call()} will be
   *       cancelled if is does not complete within {@code timeout} {@code timeoutTimeUnit}.
   *   <li>In order of completion time, the new value from each {@link Callable<V>#call()} is loaded
   *       into the cache, overwriting the old value. If {@link Callable<V>#call()} timed out,
   *       {@link #handleTimeout(Object)} is called for special timeout handling. If {@link
   *       Callable<V>#call()} threw a {@link RuntimeException}, {@link #handleException(Object,
   *       RuntimeException)} is called for special exception handling.
   * </ol>
   *
   * @param timeout the maximum time that any loader {@link Callable<V>} is allowed to execute
   * @param timeoutTimeUnit the unit for the {@code timeout}
   * @param itemsToPoll the current item keys to poll along with their corresponding loader {@link
   *     Callable<V>}s with which to retrieve the current value and update the cache
   * @throws IllegalArgumentException if the timeout is less than {@value MINIMUM_TIMEOUT_MS}
   *     milliseconds
   * @throws NullPointerException if {@code timeoutTimeUnit} or {@code itemsToPoll} is {@code null}
   * @throws IllegalStateException if {@link #pollThreadPool} or {@link
   *     #pollTimeoutWatcherThreadPool} {@link ExecutorService#isShutdown()}, if another thread is
   *     currently polling, or if unable to wait for polls
   * @throws InterruptedException if the current thread was interrupted
   * @throws CancellationException if the task to wait for the loader {@link Callable<V>} to be
   *     complete was cancelled
   * @throws ExecutionException if the the task to wait for the loader {@link Callable<V>} threw an
   *     exception
   * @throws PollerException if unable to commit the value for any of the {@code itemsToPoll}
   */
  protected void pollItems(
      final long timeout,
      final TimeUnit timeoutTimeUnit,
      final ImmutableMap<K, Callable<V>> itemsToPoll)
      throws InterruptedException, ExecutionException, PollerException {
    Validate.isTrue(
        notNull(timeoutTimeUnit).toMillis(timeout) >= MINIMUM_TIMEOUT_MS,
        "timeout argument may not be less than %d ms",
        MINIMUM_TIMEOUT_MS);
    notNull(itemsToPoll);
    if (pollThreadPool.isShutdown() || pollTimeoutWatcherThreadPool.isShutdown()) {
      final String message =
          "Unable to poll because pollThreadPool or pollTimeoutWatcherThreadPool is shutdown";
      throw new IllegalStateException(message);
    }

    if (!pollItemsLock.tryLock()) {
      final String message =
          "Unable to poll items. Multiple threads may not pollItems at the same time";
      LOGGER.debug(message);
      throw new IllegalStateException(message);
    }

    try {
      doPollItems(timeout, timeoutTimeUnit, itemsToPoll);
    } finally {
      pollItemsLock.unlock();
    }
  }

  /**
   * Adds a entry to the cache or updates an existing value. This method may optionally be called by
   * {@link #handleTimeout(Object)} and {@link #handleException(Object, RuntimeException)}.
   */
  protected void cacheNewValue(final K key, final V newValue) {
    notNull(newValue);

    final V nullableOldValue = cache.getIfPresent(key);

    LOGGER.trace("Caching value={} for {}", newValue, key);
    cache.put(key, newValue);

    if (nullableOldValue != null) {
      final V oldValue = nullableOldValue;
      if (!java.util.Objects.equals(newValue, oldValue)) {
        LOGGER.info("The value for {} was updated from {} to {}", key, oldValue, newValue);
      }
    } else {
      LOGGER.debug("Polled {} for the first time. Found that the value is {}", key, newValue);
    }
  }

  /**
   * This method is used for special handling when the loader {@link Callable} to get the current
   * value for the {@code key} does not complete within the timeout given in {@link #pollItems(long,
   * TimeUnit, ImmutableMap)}. For example, this method could be overridden to log a message or
   * cache a special "timeout" value via {@link #cacheNewValue(Object, Object)}. By default,
   * timeouts are ignored. I.e., the cache is not updated.
   *
   * @param key the non-null key for which the loader {@link Callable} timed out
   * @see #pollItems(long, TimeUnit, ImmutableMap)
   */
  protected void handleTimeout(final K key) {
    LOGGER.debug("Timeout occurred while getting the value for {}. Not updating the cache", key);
  }

  /**
   * This method is used for special handling when the loader {@link Callable} to get the current
   * value for the {@code key} throws a {@link RuntimeException} in {@link #pollItems(long,
   * TimeUnit, ImmutableMap)}. For example, this method could be overridden to log a message or
   * cache a special "exception encountered" value via {@link #cacheNewValue(Object, Object)}. By
   * default, loader {@link RuntimeException}s are ignored. I.e., the cache is not updated.
   *
   * @param key the key for which the loader {@link Callable} threw the {@code e}
   * @param e the {@link RuntimeException} that was thrown
   * @see #pollItems(long, TimeUnit, ImmutableMap)
   */
  protected void handleException(final K key, final RuntimeException e) {
    LOGGER.debug("Timeout occurred while getting the value for {}. Not updating the cache", key);
  }

  /**
   * @throws IllegalStateException if unable to wait for polls
   * @throws InterruptedException if the current thread was interrupted
   * @throws CancellationException if the task to wait for the loader {@link Callable<V>} to be
   *     complete was cancelled
   * @throws ExecutionException if the the task to wait for the loader {@link Callable<V>} threw an
   *     exception
   * @throws PollerException if unable to commit the value for any of the {@code itemsToPoll}
   */
  private void doPollItems(
      long timeout, TimeUnit timeoutTimeUnit, ImmutableMap<K, Callable<V>> itemsToPoll)
      throws InterruptedException, ExecutionException, PollerException {
    removeNoncurrentKeysFromTheCache(itemsToPoll.keySet());

    if (itemsToPoll.isEmpty()) {
      LOGGER.debug("itemsToPoll is empty. Nothing to poll");
      return;
    }

    // Gather any exceptions while loading or committing new values
    final Map<K, Throwable> exceptions = new HashMap<>();
    final CompletionService<Pair<K, Commitable>> completionService =
        new ExecutorCompletionService<>(pollTimeoutWatcherThreadPool);
    final int startedLoadsCount =
        startLoads(timeout, timeoutTimeUnit, itemsToPoll, completionService, exceptions);

    boolean interrupted = false;
    try {
      for (int i = 0; i < startedLoadsCount; i++) {
        // Use CompletionService#poll(long, TimeUnit) instead of CompletionService#take() even
        // though the timeout has already been accounted for in #load(K, Callable<V>, long,
        // TimeUnit) to prevent blocking forever
        // @throws InterruptedException if interrupted while waiting
        final Future<Pair<K, Commitable>> nextCompletedLoadFuture =
            completionService.poll(timeout, timeoutTimeUnit);
        if (nextCompletedLoadFuture == null) {
          final String message =
              String.format(
                  "Unable to wait for polls to finish within %d %s", timeout, timeoutTimeUnit);
          LOGGER.debug(message);
          throw new IllegalStateException(message);
        }

        // @throws CancellationException if the computation was cancelled
        // @throws ExecutionException if the computation threw an exception
        // @throws InterruptedException if the current thread was interrupted
        final Pair<K, Commitable> nextCompletedLoad = nextCompletedLoadFuture.get();

        try {
          attemptToCommitLoadedValue(
              nextCompletedLoad.getKey(), nextCompletedLoad.getValue(), exceptions);
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }

    if (!exceptions.isEmpty()) {
      throw new PollerException(exceptions);
    }
  }

  /** Removes keys in the cache that are not in the {@code keysToPoll} */
  private void removeNoncurrentKeysFromTheCache(final Set<K> keysToPoll) {
    final Set<K> keysInTheCache = cache.asMap().keySet();
    final Set<K> keysToRemoveFromTheCache = Sets.difference(keysInTheCache, keysToPoll);
    if (!keysToRemoveFromTheCache.isEmpty()) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Found {} entries to remove from the cache: {}",
            keysToRemoveFromTheCache.size(),
            StringUtils.join(keysToRemoveFromTheCache, ", "));
      }
      keysToRemoveFromTheCache
          .stream()
          .peek(key -> LOGGER.debug("Removing {} from the cache", key))
          .forEach(cache::invalidate);
    } else {
      LOGGER.trace("Found no entries to remove from the cache");
    }
  }

  /**
   * For each of the {@code itemsToPoll}, uses the {@code completionService} to kick off the process
   * to load the new value. If unable to start the process, an exception is added to {@code
   * gatheredExceptions}.
   *
   * @return the number of processes started
   */
  private int startLoads(
      final long timeout,
      final TimeUnit timeoutTimeUnit,
      final ImmutableMap<K, Callable<V>> itemsToPoll,
      final CompletionService<Pair<K, Commitable>> completionService,
      final Map<K, Throwable> gatheredExceptions) {
    int startedLoadsCount = 0;
    for (final Entry<K, Callable<V>> entry : itemsToPoll.entrySet()) {
      final K key1 = entry.getKey();
      final Callable<V> loader = entry.getValue();
      try {
        completionService.submit(
            () -> new ImmutablePair<>(key1, load(key1, loader, timeout, timeoutTimeUnit)));
        startedLoadsCount++;
      } catch (final RuntimeException e) {
        LOGGER.debug("Unable to start the load task for {}", key1, e);
        gatheredExceptions.put(key1, e);
      }
    }

    return startedLoadsCount;
  }

  private Commitable load(
      final K key, final Callable<V> loader, final long timeout, final TimeUnit timeoutTimeUnit) {
    final Future<V> loaderFuture;
    try {
      loaderFuture = pollThreadPool.submit(loader);
    } catch (final RejectedExecutionException e) {
      // the loader {@link Callable} could not be scheduled for execution
      return () -> {
        throw e;
      };
    }

    try {
      final V newValue = loaderFuture.get(timeout, timeoutTimeUnit);
      if (newValue == null) {
        return () -> {
          throw new IllegalArgumentException(
              "Poller values may not be null, but the loaded value for " + key + " was null.");
        };
      }

      return () -> cacheNewValue(key, newValue);
    } catch (TimeoutException e) {
      LOGGER.debug(
          "The loader for {} did not complete within {} {}. Cancelling the loader task",
          key,
          timeout,
          timeoutTimeUnit);
      loaderFuture.cancel(true);
      return () -> handleTimeout(key);
    } catch (ExecutionException e) {
      final Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        final RuntimeException runtimeException = (RuntimeException) cause;
        return () -> handleException(key, runtimeException);
      } else {
        return () -> {
          throw cause;
        };
      }
    } catch (CancellationException e) {
      return () -> {
        throw e;
      };
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return () -> {
        throw e;
      };
    }
  }

  /**
   * Attempts to commit (update the cache, {@link #handleTimeout(Object)}, or {@link
   * #handleException(Object, RuntimeException)}) for a loaded value. If unable to commit, an
   * exception is added to {@code gatheredExceptions}.
   *
   * @throws InterruptedException if the current thread was interrupted
   */
  @SuppressWarnings("squid:S1181" /*Catching throwable intentionally*/)
  private void attemptToCommitLoadedValue(
      final K key, final Commitable commitable, final Map<K, Throwable> gatheredExceptions)
      throws InterruptedException {
    try {
      commitable.commit();
    } catch (final RejectedExecutionException e) {
      LOGGER.debug(
          "Nothing to commit for {} because the loader could not be scheduled for execution",
          key,
          e);
      gatheredExceptions.put(key, e);
    } catch (final IllegalArgumentException e) {
      LOGGER.debug("Nothing to commit for {} because the loader returned null", key);
      gatheredExceptions.put(key, e);
    } catch (final CancellationException e) {
      LOGGER.debug("Nothing to commit for {} because the loader was cancelled", key, e);
      gatheredExceptions.put(key, e);
    } catch (final RuntimeException e) {
      LOGGER.debug(
          "Nothing to commit for {} because handleTimeout(Object) or handleException(Object, RuntimeException) threw a RuntimeException",
          key,
          e);
      gatheredExceptions.put(key, e);
    } catch (final InterruptedException e) {
      LOGGER.debug(
          "Nothing to commit for {} because the current thread was interrupted while waiting for the loader to complete",
          key,
          e);
      gatheredExceptions.put(key, e);
      throw e;
    } catch (VirtualMachineError e) {
      throw e;
    } catch (final Throwable e) {
      LOGGER.debug(
          "Nothing to commit for {} because the loader threw something other than a RuntimeException",
          key,
          e);
      gatheredExceptions.put(key, e);
    }
  }

  /** @throws IllegalArgumentException if the {@code executorService} is shutdown */
  private static <T extends ExecutorService> T notShutdown(final T executorService) {
    if (executorService.isShutdown()) {
      final String message = "executorService may not be shutdown";
      LOGGER.debug(message);
      throw new IllegalArgumentException(message);
    }

    return executorService;
  }

  private interface Commitable {

    /**
     * Commits a loaded value to the cache or handles known cases where loading is unsuccessful
     * (i.e. timeout or {@link RuntimeException} while loading). Throws an {@link Exception} when
     * unable to load or commit the value for some other reason.
     *
     * @throws RejectedExecutionException if the loader {@link Callable} could not be scheduled for
     *     execution
     * @throws IllegalArgumentException if the loader {@link Callable} returned {@code null}
     * @throws RuntimeException if {@link #handleTimeout(Object)} or {@link #handleException(Object,
     *     RuntimeException)} threw a {@link RuntimeException}
     * @throws Throwable if the loader {@link Callable} threw something other than a {@link
     *     RuntimeException}
     * @throws CancellationException if the loader {@link Callable} was cancelled
     * @throws InterruptedException if the current thread was interrupted while waiting for the
     *     loader {@link Callable} to complete
     */
    @SuppressWarnings(
        "squid:S00112" /*InterruptedException and Throwable can be thrown while trying to load the value. Exceptions should be handled by the caller.*/)
    void commit() throws Throwable;
  }
}
