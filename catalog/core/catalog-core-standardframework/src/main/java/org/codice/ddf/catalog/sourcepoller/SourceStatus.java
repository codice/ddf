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

/**
 * Describes a state of availability of a {@link Source}. {@link SourceStatus} includes more states
 * than a {@code boolean} for available/unavailable.
 */
public enum SourceStatus {

  /** Indicates that {@link Source#isAvailable()} returned {@code true} */
  AVAILABLE,

  /** Indicates that {@link Source#isAvailable()} returned {@code false} */
  UNAVAILABLE,

  /** Indicates that {@link Source#isAvailable()} threw an exception */
  EXCEPTION,

  /** Indicates that {@link Source#isAvailable()} timed out */
  TIMEOUT
}
