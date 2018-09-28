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

import java.util.Date;

/**
 * Contains details about the availability of a {@link ddf.catalog.source.Source} at the last time
 * that it was checked by the {@link SourcePoller}
 */
public class SourceAvailability {

  private final SourceStatus sourceStatus;

  private final Date sourceStatusDate;

  /** @throws IllegalArgumentException if the {@code sourceStatus} is {@code null} */
  public SourceAvailability(final SourceStatus sourceStatus) {
    notNull(sourceStatus);

    this.sourceStatus = sourceStatus;
    this.sourceStatusDate = new Date();
  }

  /**
   * @return the {@link SourceStatus} of the {@link ddf.catalog.source.Source} at the last time that
   *     the availability was checked by the {@link SourcePoller}
   */
  public SourceStatus getSourceStatus() {
    return sourceStatus;
  }

  /**
   * @return the {@link Date} that the availability of the {@link ddf.catalog.source.Source} was
   *     last checked by the {@link SourcePoller}
   */
  public Date getSourceStatusDate() {
    return sourceStatusDate;
  }
}
