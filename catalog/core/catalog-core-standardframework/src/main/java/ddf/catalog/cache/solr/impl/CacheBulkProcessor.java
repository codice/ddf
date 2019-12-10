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
package ddf.catalog.cache.solr.impl;

import com.google.common.collect.Lists;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Bulk adds metacards to the cache that are not needed immediately. */
public class CacheBulkProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheBulkProcessor.class);

  private final ScheduledExecutorService batchScheduler =
      Executors.newSingleThreadScheduledExecutor();

  private final Map<String, Metacard> metacardsToCache = new ConcurrentHashMap<>();

  private long flushInterval = TimeUnit.SECONDS.toMillis(10);

  private int maximumBacklogSize = 10000;

  private int batchSize = 500;

  private Date lastBulkAdd = new Date();

  private CacheStrategy cacheStrategy;

  public CacheBulkProcessor(final SolrCache cache) {
    this(cache, 1, TimeUnit.SECONDS, CacheStrategy.ALL);
  }

  /**
   * Create a new cache bulk processor that will check added metacards for bulk processing at the
   * configured delay interval.
   *
   * @param cache target Solr cache to bulk add metacards
   * @param delay delay between decision to bulk add
   * @param delayUnit units of the delay
   */
  @SuppressWarnings("squid:S1181" /*Catching throwable intentionally*/)
  public CacheBulkProcessor(
      final SolrCache cache,
      final long delay,
      final TimeUnit delayUnit,
      CacheStrategy cacheStrategy) {
    batchScheduler.scheduleWithFixedDelay(
        () -> {
          try {
            if (metacardsToCache.size() > 0
                && (metacardsToCache.size() >= batchSize || timeToFlush())) {
              LOGGER.debug("{} metacards to batch add to cache", metacardsToCache.size());

              List<Metacard> metacards = new ArrayList<>(metacardsToCache.values());
              for (Collection<Metacard> batch : Lists.partition(metacards, batchSize)) {
                LOGGER.debug("Caching a batch of {} metacards", batch.size());
                cache.put(batch);

                for (Metacard metacard : batch) {
                  metacardsToCache.remove(metacard.getId());
                }
              }

              lastBulkAdd = new Date();
            }
          } catch (VirtualMachineError vme) {
            throw vme;
          } catch (Throwable throwable) {
            LOGGER.warn("Scheduled bulk ingest to cache failed", throwable);
          }
        },
        delay,
        delay,
        delayUnit);

    this.cacheStrategy = cacheStrategy;
  }

  private boolean timeToFlush() {
    Date now = new Date();
    return now.getTime() - lastBulkAdd.getTime() > flushInterval;
  }

  /**
   * Adds metacards to be bulk added to cache. Metacards will be ignored if backlog grows too large.
   * Metacard currently in backlog will be updated if added again.
   *
   * @param results metacards to add to current batch
   */
  public void add(final List<Result> results) {
    if (metacardsToCache.size() < maximumBacklogSize) {
      cacheStrategy
          .getCacheStrategyFunction()
          .accept(results, m -> metacardsToCache.put(m.getId(), m));
    }
  }

  /** Shutdown scheduled tasks. */
  public void shutdown() {
    batchScheduler.shutdown();
  }

  int pendingMetacards() {
    return metacardsToCache.size();
  }

  public void setFlushInterval(long flushInterval) {
    this.flushInterval = flushInterval;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public void setMaximumBacklogSize(int maximumBacklogSize) {
    this.maximumBacklogSize = maximumBacklogSize;
  }

  public void setCacheStrategy(CacheStrategy cacheStrategy) {
    this.cacheStrategy = cacheStrategy;
  }
}
