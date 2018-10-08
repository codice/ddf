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
package ddf.catalog.util.impl;

import static org.apache.commons.lang.Validate.notNull;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceMonitor;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.ListenerHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SourcePoller} checks the availability of all configured {@link Source}s provides a
 * non-blocking solution to check whether a {@link Source} is available. The {@link SourcePoller}
 * can be scheduled to execute at a configured rate, i.e., the polling interval.
 *
 * <p>This class maintains a list of all of the {@link Source}s and their last known availability.
 * {@link Source}s are added to this list when they are created or destroyed, and {@link Source}
 * availability is re-checked when the {@link Source} is modified. A cached map is maintained of all
 * the {@link Source}s and their last known availability, i.e., {@link SourceStatus}.
 *
 * @see Source#isAvailable()
 * @see Source#isAvailable(SourceMonitor)
 * @see ddf.catalog.CatalogFramework#getSourceInfo(ddf.catalog.operation.SourceInfoRequest)
 */
public class SourcePoller implements EventListenerHook {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourcePoller.class);

  private static final int EXECUTOR_SERVICE_TERMINATION_TIMEOUT = 5;

  private static final TimeUnit EXECUTOR_SERVICE_TERMINATION_TIMEOUT_TIME_UNIT = TimeUnit.SECONDS;

  private static final long DEFAULT_AVAILABILITY_CHECK_TIMEOUT = 1;

  private static final TimeUnit AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT = TimeUnit.MINUTES;

  private long availabilityCheckTimeout = DEFAULT_AVAILABILITY_CHECK_TIMEOUT;

  private List<ConnectedSource> connectedSources = Collections.emptyList();

  private List<FederatedSource> federatedSources = Collections.emptyList();

  private List<CatalogProvider> catalogProviders = Collections.emptyList();

  private List<CatalogStore> catalogStores = Collections.emptyList();

  private final ExecutorService sourceAvailabilityChecksThreadPool;

  private final Cache<SourceKey, CachedSourceAvailability> cache;

  @Nullable private ExecutorService pollingExecutorService;

  /** Does not start polling */
  public SourcePoller() {
    final ThreadFactory threadFactory =
        StandardThreadFactoryBuilder.newThreadFactory("sourcePollerScheduledPollingThread");
    sourceAvailabilityChecksThreadPool = Executors.newCachedThreadPool(threadFactory);

    cache =
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .removalListener(
                (RemovalNotification<SourceKey, CachedSourceAvailability> removalNotification) -> {
                  LOGGER.debug(
                      "Removing SourceKey {} from cache. cause={}. Cancelling any futures that are checking availability for that entry",
                      removalNotification.getKey(),
                      removalNotification.getCause());
                  removalNotification.getValue().cancel();
                })
            .build();
  }

  public void destroy() {
    if (pollingExecutorService != null) {
      MoreExecutors.shutdownAndAwaitTermination(
          pollingExecutorService,
          EXECUTOR_SERVICE_TERMINATION_TIMEOUT,
          EXECUTOR_SERVICE_TERMINATION_TIMEOUT_TIME_UNIT);
    }

    MoreExecutors.shutdownAndAwaitTermination(
        sourceAvailabilityChecksThreadPool,
        EXECUTOR_SERVICE_TERMINATION_TIMEOUT,
        EXECUTOR_SERVICE_TERMINATION_TIMEOUT_TIME_UNIT);
  }

  /**
   * This method is an non-blocking alternative to {@link Source#isAvailable()} which provides more
   * more details about the availability than just available or unavailable. The last availability
   * check is guaranteed to be started when the source is bound with {@link #bind(Source)} or at the
   * start of the lat poll interval, configured by {@link #setPollIntervalMinutes(long)}.
   *
   * @return a {@link SourceAvailability} for the availability of the {@code source}, or {@link
   *     Optional#empty()} if the availability of the {@code source} has not yet been checked
   * @throws IllegalArgumentException if the {@code source} is {@code null}
   */
  public Optional<SourceAvailability> getSourceAvailability(final Source source) {
    notNull(source);

    LOGGER.trace("Getting status of source id={}", source.getId());
    return Optional.ofNullable(cache.getIfPresent(new SourceKey(source)))
        .flatMap(CachedSourceAvailability::getSourceAvailability);
  }

  /**
   * Starts a process to periodically check the availability of each of the {@link Source}s
   * configured by {@link #setConnectedSources(List)}, {@link #setFederatedSources(List)}, {@link
   * #setCatalogProviders(List)}, and {@link #setCatalogStores(List)}.
   *
   * <p>At the start of each interval, a process to check the availability of a {@link Source} will
   * not be started if the {@link Source} was bound or modified since the last poll and the
   * availability check started by that action has not yet completed.
   *
   * @param pollIntervalMinutes the interval (in minutes) at which to recheck the availability of
   *     the {@link Source}s
   * @throws IllegalArgumentException if {@code pollIntervalMinutes} is less than {@code 1}
   */
  public void setPollIntervalMinutes(final long pollIntervalMinutes) {
    final long minimumPollIntervalMinutes = 1;
    if (pollIntervalMinutes < minimumPollIntervalMinutes) {
      throw new IllegalArgumentException(
          "pollIntervalMinutes argument may not be less than " + minimumPollIntervalMinutes);
    }

    if (pollingExecutorService != null) {
      MoreExecutors.shutdownAndAwaitTermination(
          pollingExecutorService,
          EXECUTOR_SERVICE_TERMINATION_TIMEOUT,
          EXECUTOR_SERVICE_TERMINATION_TIMEOUT_TIME_UNIT);
    }

    LOGGER.trace("Initializing ExecutorService");
    final ScheduledExecutorService scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("SourcePoller"));

    LOGGER.debug(
        "Scheduling ExecutorService at fixed rate of {} {}", pollIntervalMinutes, TimeUnit.MINUTES);

    // If any execution of this task takes longer than its period, then subsequent executions may
    // start late, but will not concurrently execute.
    scheduledExecutorService.scheduleAtFixedRate(
        this::recheckAvailabilityOfAllSourcesAndWait, 0, pollIntervalMinutes, TimeUnit.MINUTES);
    pollingExecutorService = scheduledExecutorService;

    // Allow more time to check the availability
    this.availabilityCheckTimeout =
        AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT.convert(pollIntervalMinutes, TimeUnit.MINUTES);
  }

  /**
   * Used to listen for when a {@link Source} is modified. If a {@link Source} is modified in a way
   * that changes its {@link SourceKey}, the {@link Source} will be bound.
   *
   * <p>Does not block
   *
   * @throws IllegalArgumentException if {@code serviceEvent} is {@code null}
   */
  @Override
  public void event(
      final ServiceEvent serviceEvent,
      final Map<BundleContext, Collection<ListenerHook.ListenerInfo>> listeners) {
    notNull(serviceEvent);

    final ServiceReference<?> serviceReference = serviceEvent.getServiceReference();
    final BundleContext bundleContext = getBundleContext();
    final Object service = bundleContext.getService(serviceReference);

    if (service instanceof Source) {
      final Source source = (Source) service;
      final int serviceEventType = serviceEvent.getType();

      // ServiceEvent.REGISTERED and ServiceEvent.UNREGISTERING are already handled by the
      // bind and unbind methods, respectively.
      if (serviceEventType != ServiceEvent.REGISTERED
          && serviceEventType != ServiceEvent.UNREGISTERING) {
        final String sourceId = source.getId();
        final SourceKey sourceKey = new SourceKey(source);

        final Optional<CachedSourceAvailability> cachedSourceStatusOptional =
            Optional.ofNullable(cache.getIfPresent(sourceKey));
        if (cachedSourceStatusOptional.isPresent()) {
          LOGGER.debug(
              "The source id={} has been updated but its SourceKey is still the same. Cancelling the current availability check of the source, if not completed. Rechecking the availability of the source.",
              sourceId);
          final CachedSourceAvailability cachedSourceAvailability =
              cachedSourceStatusOptional.get();
          cachedSourceAvailability.cancel();
          cachedSourceAvailability.recheckOrGetCurrentCheck(source);
        } else {
          LOGGER.debug(
              "Even though the source id={} has already been bound, the SourceKey for the source has been changed. The entry for the old SourceKey will expire.",
              sourceId);
          bind(source);
        }
      }
    }
  }

  /**
   * Starts a process to check {@link Source#isAvailable()} of the {@code source}
   *
   * <p>Does not block
   *
   * @throws IllegalArgumentException if {@code source} is {@code null} or if the {@code source} has
   *     already been bound
   */
  public void bind(final Source source) {
    notNull(source);

    final SourceKey sourceKey = new SourceKey(source);

    final Optional<CachedSourceAvailability> cachedSourceAvailabilityOptional =
        Optional.ofNullable(cache.getIfPresent(sourceKey));
    if (cachedSourceAvailabilityOptional.isPresent()) {
      // There might be a race condition where a new source is bound after it is found with during a
      // poll. Recheck the availability of the source if it is not currently being checked.
      LOGGER.debug("Binding a source that has a source key that has already been bound");
      final CachedSourceAvailability cachedSourceAvailability =
          cachedSourceAvailabilityOptional.get();
      cachedSourceAvailability.recheckOrGetCurrentCheck(source);
    } else {
      LOGGER.debug("Binding source id={}", source.getId());
      final CachedSourceAvailability cachedSourceAvailability = new CachedSourceAvailability();
      cache.put(sourceKey, cachedSourceAvailability);
      cachedSourceAvailability.recheckOrGetCurrentCheck(source);
    }
  }

  /**
   * Clears the {@link SourceAvailability} for the {@code source} so that {@link
   * #getSourceAvailability(Source)} will return {@link Optional#empty()}
   *
   * <p>Does not block
   */
  public void unbind(@Nullable final Source source) {
    if (source != null) {
      final SourceKey sourceKey = new SourceKey(source);

      final Optional<CachedSourceAvailability> cachedSourceStatusOptional =
          Optional.ofNullable(cache.getIfPresent(sourceKey));
      if (cachedSourceStatusOptional.isPresent()) {
        LOGGER.debug("Unbinding source id={}", source.getId());
        // Cancelling SourceKeys is handled in the RemovalListener of the cache.
        cache.invalidate(sourceKey);
      } else {
        LOGGER.warn(
            "Cannot unbind a source that has not yet been bound. This means that the SourcePoller might not report accurate availability for these sources in the CatalogFramework. Try restarting.");
      }
    }
  }

  /**
   * Sets the {@link ConnectedSource}s of which to check the availability periodically according to
   * {@link #setPollIntervalMinutes(long)}
   *
   * @throws IllegalArgumentException if {@code connectedSources} is {@code null}
   */
  public void setConnectedSources(final List<ConnectedSource> connectedSources) {
    notNull(connectedSources);

    LOGGER.trace(
        "Setting connected sources: old size={}, new size={}",
        this.connectedSources.size(),
        connectedSources.size());
    this.connectedSources = connectedSources;
  }

  /**
   * Sets the {@link FederatedSource}s of which to check the availability periodically according to
   * {@link #setPollIntervalMinutes(long)}
   *
   * @throws IllegalArgumentException if {@code federatedSource} is {@code null}
   */
  public void setFederatedSources(final List<FederatedSource> federatedSources) {
    notNull(federatedSources);

    LOGGER.trace(
        "Setting federated sources: old size={}, new size={}",
        this.federatedSources.size(),
        federatedSources.size());
    this.federatedSources = federatedSources;
  }

  /**
   * Sets the {@link CatalogProvider}s of which to check the availability periodically according to
   * {@link #setPollIntervalMinutes(long)}
   *
   * @throws IllegalArgumentException if {@code catalogProvider} is {@code null}
   */
  public void setCatalogProviders(final List<CatalogProvider> catalogProviders) {
    notNull(catalogProviders);

    LOGGER.trace(
        "Setting catalog providers: old size={}, new size={}",
        this.catalogProviders.size(),
        catalogProviders.size());
    this.catalogProviders = catalogProviders;
  }

  /**
   * Sets the {@link CatalogStore}s of which to check the availability periodically according to
   * {@link #setPollIntervalMinutes(long)}
   *
   * @throws IllegalArgumentException if {@code catalogStore} is {@code null}
   */
  public void setCatalogStores(final List<CatalogStore> catalogStores) {
    notNull(catalogStores);

    LOGGER.trace(
        "Setting catalog stores: old size={}, new size={}",
        this.catalogStores.size(),
        catalogStores.size());
    this.catalogStores = catalogStores;
  }

  /** @throws IllegalArgumentException if one of the sources has not already been bound */
  private void recheckAvailabilityOfAllSourcesAndWait() {
    final Set<Source> sources =
        Stream.of(connectedSources, federatedSources, catalogProviders, catalogStores)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    LOGGER.trace(
        "Found {} sources. Starting a process to check the availability of each source",
        sources.size());

    // Unbind orphaned entries in the map caused by when sources are modified and their key is
    // different
    final Map<SourceKey, Source> sourceKeyMap =
        sources
            .stream()
            .collect(
                Collectors.toMap(
                    SourceKey::new,
                    source -> source,
                    (source, anotherSourceWithTheSameSourceKey) -> {
                      LOGGER.warn(
                          "Found different sources with the same SourceKey with id={}. This means that the SourcePoller might not report accurate availability for these sources in the CatalogFramework. Confirm that every source has a unique id, and try restarting.",
                          source.getId());
                      return source;
                    }));
    final Set<SourceKey> orphanedSourceKeys = cache.asMap().keySet();
    orphanedSourceKeys.removeAll(sourceKeyMap.keySet());
    if (CollectionUtils.isNotEmpty(orphanedSourceKeys)) {
      LOGGER.debug(
          "Found {} orphaned SourceKeys in the cache. This is caused by when sources are modified and their SourceKey changes. Removing these from the cache now",
          orphanedSourceKeys.size());
      orphanedSourceKeys.forEach(cache::invalidate);
    }

    final Set<Future> futures =
        sourceKeyMap
            .entrySet()
            .stream()
            .map(
                sourceKeySourceEntry -> {
                  final SourceKey sourceKey = sourceKeySourceEntry.getKey();
                  final Source source = sourceKeySourceEntry.getValue();

                  final Optional<CachedSourceAvailability> cachedSourceStatusOptional =
                      Optional.ofNullable(cache.getIfPresent(sourceKey));
                  if (cachedSourceStatusOptional.isPresent()) {
                    final CachedSourceAvailability cachedSourceAvailability =
                        cachedSourceStatusOptional.get();
                    LOGGER.trace(
                        "Starting a process to recheck the availability of source id={}",
                        source.getId());
                    return cachedSourceAvailability.recheckOrGetCurrentCheck(source);
                  } else {
                    // There might be a race condition where a new source is set with the setSources
                    // methods but has not yet been bound. In this case, bind the source anyway.
                    LOGGER.debug(
                        "Found a source that has not yet been bound. Binding source id={}",
                        source.getId());
                    final CachedSourceAvailability cachedSourceAvailability =
                        new CachedSourceAvailability();
                    cache.put(sourceKey, cachedSourceAvailability);
                    return cachedSourceAvailability.recheckOrGetCurrentCheck(source);
                  }
                })
            .collect(Collectors.toSet());

    LOGGER.trace(
        "Successfully submitted availability checks for all of the sources. Waiting for them to complete");
    futures.forEach(
        future -> {
          try {
            future.get();
          } catch (CancellationException e) {
            LOGGER.debug(
                "Cancelled a process to check the availability of a source. This is expected when a source is updated in the middle of a poll and when shutting down.");
          } catch (ExecutionException | RuntimeException e) {
            LOGGER.debug("Unable to wait for a poll thread to complete", e);
            future.cancel(true);
          } catch (InterruptedException e) {
            LOGGER.debug("InterruptedException waiting for a poll thread to complete", e);
            future.cancel(true);
            Thread.currentThread().interrupt();
          }
        });

    LOGGER.trace("Successfully checked the availability of all of the sources");
  }

  @VisibleForTesting
  BundleContext getBundleContext() {
    return Optional.ofNullable(FrameworkUtil.getBundle(SourcePoller.class))
        .map(Bundle::getBundleContext)
        .orElseThrow(
            () ->
                new IllegalStateException("Unable to get the bundle context for the SourcePoller"));
  }

  /**
   * Used to keep track of the last {@link SourceAvailability} in the {@link #cache} and to cancel
   * the current availability check
   */
  private class CachedSourceAvailability {

    /** {@link Optional#empty()} indicates that an availability check has not yet completed */
    private Optional<SourceAvailability> sourceAvailabilityOptional;

    private Future<Boolean> isAvailableFuture;

    private Future watcherFuture;

    public CachedSourceAvailability() {
      sourceAvailabilityOptional = Optional.empty();
      isAvailableFuture = Futures.immediateCancelledFuture();
      watcherFuture = Futures.immediateCancelledFuture();
    }

    private void cancel() {
      watcherFuture.cancel(true);
      isAvailableFuture.cancel(true);
    }

    /**
     * Availability checks will be cancelled if they take longer than the poll interval (or a
     * default of {@value #DEFAULT_AVAILABILITY_CHECK_TIMEOUT} {@value
     * #AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT} if the poll interval has not yet been configured), and
     * the {@link SourceStatus} will be updated to {@link SourceStatus#TIMEOUT}.
     */
    public Future recheckOrGetCurrentCheck(final Source source) {
      notNull(source);
      final String sourceId = source.getId();

      if (watcherFuture.isDone()) {
        if (!isAvailableFuture.isDone()) {
          isAvailableFuture.cancel(true);
        }

        LOGGER.trace("Starting a process to check the availability of source id={}", sourceId);
        isAvailableFuture =
            sourceAvailabilityChecksThreadPool.submit((Callable<Boolean>) source::isAvailable);
        watcherFuture =
            sourceAvailabilityChecksThreadPool.submit(
                () -> waitAndUpdateAvailability(isAvailableFuture, sourceId));
      } else {
        LOGGER.debug(
            "A process has already been started to check the availability of source id={}. This is expected when the source is recently modified or bound. Not starting a new check",
            sourceId);
      }

      return watcherFuture;
    }

    private void waitAndUpdateAvailability(
        final Future<Boolean> isAvailableFuture, final String sourceId) {
      SourceStatus newSourceStatus;

      try {
        final boolean isAvailable =
            isAvailableFuture.get(availabilityCheckTimeout, AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT);
        LOGGER.trace("Successfully checked the availability of source id={}", sourceId);
        newSourceStatus = isAvailable ? SourceStatus.AVAILABLE : SourceStatus.UNAVAILABLE;
      } catch (TimeoutException e) {
        isAvailableFuture.cancel(true);
        LOGGER.debug(
            "Unable to check the availability of source id={} within {} {}. Cancelling the check",
            sourceId,
            availabilityCheckTimeout,
            AVAILABILITY_CHECK_TIMEOUT_TIME_UNIT);
        newSourceStatus = SourceStatus.TIMEOUT;
      } catch (CancellationException e) {
        isAvailableFuture.cancel(true);
        LOGGER.debug("Unable to check the availability of source id={}", sourceId, e);
        return;
      } catch (RuntimeException | ExecutionException e) {
        isAvailableFuture.cancel(true);
        LOGGER.debug("Exception checking the availability of source id={}", sourceId, e);
        newSourceStatus = SourceStatus.EXCEPTION;
      } catch (InterruptedException e) {
        isAvailableFuture.cancel(true);
        LOGGER.debug("Interrupted while checking the availability of source id={}", sourceId);
        Thread.currentThread().interrupt();
        return;
      }

      if (sourceAvailabilityOptional.isPresent()) {
        final SourceAvailability oldSourceAvailability = sourceAvailabilityOptional.get();

        if (oldSourceAvailability.getSourceStatus() != newSourceStatus) {
          LOGGER.info(
              "The source status of source id={} has changed to {}. The last known source status was {} at {}.",
              sourceId,
              newSourceStatus,
              oldSourceAvailability.getSourceStatus(),
              oldSourceAvailability.getSourceStatusDate());
        }

        final SourceAvailability newSourceAvailability = new SourceAvailability(newSourceStatus);
        sourceAvailabilityOptional = Optional.of(newSourceAvailability);
      } else {
        final SourceAvailability initialSourceAvailability =
            new SourceAvailability(newSourceStatus);
        LOGGER.info(
            "Checked the availability of source id={} for the first time. Setting the source status to {}",
            sourceId,
            initialSourceAvailability.getSourceStatus());
        sourceAvailabilityOptional = Optional.of(initialSourceAvailability);
      }
    }

    public Optional<SourceAvailability> getSourceAvailability() {
      return sourceAvailabilityOptional;
    }
  }

  /**
   * Used as a way to compare {@link Source}s in the {@link #cache}. Do not keep a reference to the
   * {@link Source} to prevent the issue addressed by DDF-2789.
   */
  private static class SourceKey {

    private String id;

    private String title;

    private String version;

    private String description;

    private String organization;

    /** @throws IllegalArgumentException if the {@link Source#getId()} is empty */
    public SourceKey(final Source source) {
      this.id = source.getId();
      this.title = source.getTitle();
      this.version = source.getVersion();
      this.description = source.getDescription();
      this.organization = source.getOrganization();
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (!(obj instanceof SourceKey)) {
        return false;
      }

      final SourceKey sourceKey = (SourceKey) obj;
      return StringUtils.equals(id, sourceKey.id)
          && StringUtils.equals(title, sourceKey.title)
          && StringUtils.equals(version, sourceKey.version)
          && StringUtils.equals(description, sourceKey.description)
          && StringUtils.equals(organization, sourceKey.organization);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(id, title, version, description, organization);
    }

    /** Only used to in the {@link com.google.common.cache.RemovalListener} log message */
    @Override
    public String toString() {
      return String.format("id=%s, title=%s", id, title);
    }
  }
}
