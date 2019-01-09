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

import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link StatusSourcePoller} is a non-blocking alternative to {@link
 * ddf.catalog.source.Source#isAvailable()}.
 *
 * @see SourceStatus
 */
public class StatusSourcePoller extends SourcePoller<SourceStatus> {

  private static final Logger LOGGER = LoggerFactory.getLogger(StatusSourcePoller.class);

  public StatusSourcePoller(
      final ExecutorService pollThreadPool, final ExecutorService pollTimeoutWatcherThreadPool) {
    super(pollThreadPool, pollTimeoutWatcherThreadPool);
  }

  @Override
  protected void handleTimeout(final SourceKey key) {
    LOGGER.debug("Timeout occurred while getting the availability for source {}", key);
    cacheNewValue(key, SourceStatus.TIMEOUT);
  }

  @Override
  protected void handleException(final SourceKey sourceKey, final RuntimeException e) {
    LOGGER.debug("RuntimeException getting the availability for source {}", sourceKey, e);
    cacheNewValue(sourceKey, SourceStatus.EXCEPTION);
  }
}
