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

import static org.apache.commons.lang3.Validate.notNull;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.source.Source;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This abstract class is used to poll {@link Source}s periodically to support a non-blocking way to
 * retrieve the last-known {@link V} for the {@link Source}. This class contains the logic of
 * retrieving the current {@link Source}s to poll.
 *
 * @param <V> type of the value returned when a {@link Source} is polled
 */
abstract class SourcePollerRunner<V> extends PollerRunner<SourceKey, V> {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourcePollerRunner.class);

  private final SourceRegistry sourceRegistry;

  /** @throws NullPointerException if {@code sourceRegistry} is {@code null} */
  protected SourcePollerRunner(
      final Poller<SourceKey, V> poller,
      final long pollIntervalMinutes,
      final ScheduledExecutorService scheduledExecutorService,
      final SourceRegistry sourceRegistry) {
    super(poller, pollIntervalMinutes, scheduledExecutorService);
    this.sourceRegistry = notNull(sourceRegistry);
  }

  /**
   * @throws NullPointerException if {@link SourceRegistry#getCurrentSources()} contains a {@code
   *     null} {@link Source}
   */
  @Override
  protected ImmutableMap<SourceKey, Callable<V>> getValueLoaders() {
    final Map<SourceKey, Callable<V>> map = new HashMap<>();
    for (final Source source : sourceRegistry.getCurrentSources()) {
      notNull(source);
      final SourceKey sourceKey = new SourceKey(source);
      if (map.put(sourceKey, () -> getCurrentValueForSource(source)) != null) {
        LOGGER.warn(
            "Duplicate key {}. The Pollers may not be reporting correct values for the matching Sources. Confirm that each Source has a unique id, and try restarting."
                + sourceKey);
      }
    }
    return ImmutableMap.copyOf(map);
  }

  /**
   * Gets the current value for the {@code source}
   *
   * <p>This method blocks.
   *
   * @throws NullPointerException if {@code source} is {@code null}
   */
  protected abstract V getCurrentValueForSource(final Source source);
}
