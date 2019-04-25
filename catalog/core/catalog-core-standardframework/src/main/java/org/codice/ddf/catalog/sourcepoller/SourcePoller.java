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

import ddf.catalog.source.Source;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * The {@link SourcePoller} provides a non-blocking alternative to {@link Source} methods (e.g.
 * {@link Source#isAvailable()} and {@link Source#getContentTypes()}, which can block because they
 * submit a request to a remote client). In this {@link Poller}, {@link SourceKey}s are used to
 * compare {@link Source}s. This class also provides a method not available in {@link Poller}:
 * {@link #getCachedValueForSource(Source)}.
 */
public class SourcePoller<V> extends Poller<SourceKey, V> {

  protected SourcePoller(
      final ExecutorService pollThreadPool, final ExecutorService pollTimeoutWatcherThreadPool) {
    super(pollThreadPool, pollTimeoutWatcherThreadPool);
  }

  /** @throws NullPointerException if the {@code source} is {@code null} */
  public Optional<V> getCachedValueForSource(final Source source) {
    return getCachedValue(new SourceKey(source));
  }
}
