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

import ddf.catalog.data.ContentType;
import ddf.catalog.source.Source;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

public class ContentTypesSourcePollerRunner extends SourcePollerRunner<Set<ContentType>> {

  public ContentTypesSourcePollerRunner(
      final Poller<SourceKey, Set<ContentType>> poller,
      final long pollIntervalMinutes,
      final ScheduledExecutorService scheduledExecutorService,
      final SourceRegistry sourceRegistry) {
    super(poller, pollIntervalMinutes, scheduledExecutorService, sourceRegistry);
  }

  /**
   * Gets the current {@link Set<ContentType>} for the {@code source}, correcting for {@code null}
   * returned by {@link Source#getContentTypes()}
   *
   * @throws NullPointerException if {@code source} is {@code null}
   */
  @Override
  protected Set<ContentType> getCurrentValueForSource(final Source source) {
    return Optional.ofNullable(notNull(source).getContentTypes()).orElseGet(Collections::emptySet);
  }
}
