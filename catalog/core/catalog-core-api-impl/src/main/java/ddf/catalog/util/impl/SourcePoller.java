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
import com.google.common.util.concurrent.MoreExecutors;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceMonitor;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SourcePoller} checks the availability of all configured {@link Source}s and provides a
 * non-blocking solution to check whether a {@link Source} is available. The {@link SourcePoller}
 * can be scheduled to execute at a configured rate, i.e., the polling interval.
 *
 * <p>This class maintains a list of all of the {@link Source}s and their last known availability.
 * {@link Source}s are added to, or removed from, this list when they are created or destroyed. A
 * cached map is maintained of all the {@link Source}s and their last known availability, i.e.,
 * {@link SourceStatus}.
 *
 * @see Source#isAvailable()
 * @see Source#isAvailable(SourceMonitor)
 * @see ddf.catalog.CatalogFramework#getSourceInfo(ddf.catalog.operation.SourceInfoRequest)
 */
public class SourcePoller {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourcePoller.class);

  private static final int DEFAULT_POLL_INTERVAL = 1;

  private static final TimeUnit DEFAULT_POLL_INTERVAL_TIME_UNIT = TimeUnit.MINUTES;

  private volatile long pollInterval = DEFAULT_POLL_INTERVAL;

  private volatile TimeUnit pollIntervalTimeUnit = DEFAULT_POLL_INTERVAL_TIME_UNIT;

  private List<ConnectedSource> connectedSources = Collections.emptyList();

  private List<FederatedSource> federatedSources = Collections.emptyList();

  private List<CatalogProvider> catalogProviders = Collections.emptyList();

  private List<CatalogStore> catalogStores = Collections.emptyList();

  private final ExecutorService sourceAvailabilityChecksThreadPool;

  private ExecutorService handleAllSourcesExecutorService;

  private final Cache<SourceKey, CachedSourceAvailability> cache;

  public SourcePoller() {
    this(DEFAULT_POLL_INTERVAL, DEFAULT_POLL_INTERVAL_TIME_UNIT);
  }

  /** Starts a process to periodically call {@link #pollAllSources()} */
  @VisibleForTesting
  SourcePoller(final long pollInterval, final TimeUnit pollIntervalTimeUnit) {
    sourceAvailabilityChecksThreadPool =
        Executors.newCachedThreadPool(
            StandardThreadFactoryBuilder.newThreadFactory("sourcePollerScheduledPollingThread"));

    // If any execution of this task takes longer than its period, then subsequent executions may
    // start late, but will not concurrently execute.
    LOGGER.debug(
        "Scheduling ExecutorService at fixed rate of {} {} with an initial delay of {}",
        pollInterval,
        pollIntervalTimeUnit,
        pollInterval);
    ScheduledExecutorService scheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("SourcePoller"));
    scheduledExecutorService.scheduleAtFixedRate(
        this::pollAllSources, pollInterval, pollInterval, pollIntervalTimeUnit);
    handleAllSourcesExecutorService = scheduledExecutorService;

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

    setPollInterval(pollInterval, pollIntervalTimeUnit);
  }

  /**
   * This method is used to keep the cache up to date.
   *
   * <p>The current {@link Source}s configured by {@link #setConnectedSources(List)}, {@link
   * #setFederatedSources(List)}, {@link #setCatalogProviders(List)}, and {@link
   * #setCatalogStores(List)} are discovered.
   *
   * <p>A process to check (or recheck) the availability of each {@link Source} is started. If an
   * availability check is still running once the new check is started, it will timeout and be
   * replaced by the new availability check.
   */
  public synchronized void pollAllSources() {
    final Set<Source> sources =
        Stream.of(connectedSources, federatedSources, catalogProviders, catalogStores)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

    LOGGER.trace("Found {} sources", sources.size());

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

    final Set<SourceKey> currentSourceKeys = sourceKeyMap.keySet();
    final Set<SourceKey> sourceKeysInTheCache = cache.asMap().keySet();

    final Set<SourceKey> sourceKeysToRemove = new HashSet<>(sourceKeysInTheCache);
    sourceKeysToRemove.removeAll(currentSourceKeys);
    if (CollectionUtils.isNotEmpty(sourceKeysToRemove)) {
      LOGGER.debug(
          "Found {} SourceKeys to remove from the cache. This is caused by when sources are destroyed or modified in a way that their SourceKey changes. Removing these from the cache now",
          sourceKeysToRemove.size());
      sourceKeysToRemove.forEach(cache::invalidate);
    }

    for (final Map.Entry<SourceKey, Source> entry : sourceKeyMap.entrySet()) {
      final Source source = entry.getValue();
      final SourceKey sourceKey = entry.getKey();

      final Optional<CachedSourceAvailability> cachedSourceAvailabilityOptional =
          Optional.ofNullable(cache.getIfPresent(sourceKey));
      if (cachedSourceAvailabilityOptional.isPresent()) {
        final CachedSourceAvailability cachedSourceAvailability =
            cachedSourceAvailabilityOptional.get();
        LOGGER.trace(
            "Starting a process to recheck the availability of source id={}, which has already been added to the cache ",
            source.getId());
        cachedSourceAvailability.recheckAvailability(source, sourceAvailabilityChecksThreadPool);
      } else {
        LOGGER.debug("Found a new SourceKey for source id={}", source.getId());
        final CachedSourceAvailability cachedSourceAvailability = new CachedSourceAvailability();
        cache.put(sourceKey, cachedSourceAvailability);
        cachedSourceAvailability.recheckAvailability(source, sourceAvailabilityChecksThreadPool);
      }
    }

    LOGGER.trace("Successfully submitted availability checks for all of the sources");
  }

  private void setPollInterval(final long pollInterval, final TimeUnit pollIntervalTimeUnit) {
    this.pollInterval = pollInterval;
    this.pollIntervalTimeUnit = pollIntervalTimeUnit;
  }

  public void destroy() {
    MoreExecutors.shutdownAndAwaitTermination(handleAllSourcesExecutorService, 5, TimeUnit.SECONDS);

    MoreExecutors.shutdownAndAwaitTermination(
        sourceAvailabilityChecksThreadPool, 5, TimeUnit.SECONDS);
  }

  /**
   * This method is an non-blocking alternative to {@link Source#isAvailable()} which provides more
   * more details about the availability than just available or unavailable.
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

  @VisibleForTesting
  BundleContext getBundleContext() {
    return Optional.ofNullable(FrameworkUtil.getBundle(SourcePoller.class))
        .map(Bundle::getBundleContext)
        .orElseThrow(
            () ->
                new IllegalStateException("Unable to get the bundle context for the SourcePoller"));
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

  /**
   * @param pollIntervalMinutes the interval (in minutes) at which to recheck the availability of
   *     the {@link Source}s
   * @throws IllegalArgumentException if {@code pollIntervalMinutes} is less than {@code 1} or is
   *     greater than {@link Integer#MAX_VALUE}
   */
  public void setPollIntervalMinutes(final long pollIntervalMinutes) {
    final int minimumPollIntervalMinutes = 1;
    if (pollIntervalMinutes < minimumPollIntervalMinutes) {
      throw new IllegalArgumentException(
          "pollIntervalMinutes argument may not be less than " + minimumPollIntervalMinutes);
    }

    setPollInterval(pollIntervalMinutes, TimeUnit.MINUTES);
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
