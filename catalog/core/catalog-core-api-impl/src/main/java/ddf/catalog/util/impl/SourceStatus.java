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

import ddf.catalog.source.Source;

/**
 * Describes the last known availability of a {@link Source} by the {@link SourcePoller}. {@link
 * SourceStatus} includes more statuses than the {@code boolean} for available/unavailable returned
 * by {@link Source#isAvailable()}. See the javadoc for the {@link SourcePoller} for more details
 * about when {@link Source#isAvailable()} is checked for {@link Source}s.
 */
public enum SourceStatus {

  /**
   * Indicates that {@link Source#isAvailable()} returned {@code true} the last time it was checked
   * for the {@link Source} in the {@link SourcePoller}
   */
  AVAILABLE,

  /**
   * Indicates that {@link Source#isAvailable()} returned {@code false} the last time it was checked
   * for the {@link Source} in the {@link SourcePoller}
   */
  UNAVAILABLE,

  /**
   * Indicates that the {@link Source} has not yet been checked for availability or could not be
   * found by the {@link SourcePoller}
   */
  UNKNOWN,

  /**
   * Indicates that {@link Source#isAvailable()} threw an exception the last time it was checked for
   * the {@link Source} in the {@link SourcePoller}
   */
  EXCEPTION,

  /**
   * Indicates that {@link Source#isAvailable()} timed out the last time it was checked for the
   * {@link Source} in the {@link SourcePoller}
   */
  TIMEOUT
}
