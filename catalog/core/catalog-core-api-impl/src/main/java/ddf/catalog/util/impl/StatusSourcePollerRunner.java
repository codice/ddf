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

import ddf.catalog.source.Source;
import java.util.concurrent.ScheduledExecutorService;

public class StatusSourcePollerRunner extends SourcePollerRunner<SourceStatus> {

  public StatusSourcePollerRunner(
      final Poller<SourceKey, SourceStatus> poller,
      final long pollIntervalMinutes,
      final ScheduledExecutorService scheduledExecutorService,
      final SourceRegistry sourceRegistry) {
    super(poller, pollIntervalMinutes, scheduledExecutorService, sourceRegistry);
  }

  /**
   * Gets the current {@link SourceStatus} for the {@code source}
   *
   * @throws NullPointerException if {@code source} is {@code null}
   */
  @Override
  protected SourceStatus getCurrentValueForSource(final Source source) {
    return notNull(source).isAvailable() ? SourceStatus.AVAILABLE : SourceStatus.UNAVAILABLE;
  }
}
