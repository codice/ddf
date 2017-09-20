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
package org.codice.ddf.spatial.geocoding;

/**
 * A {@code ProgressCallback} provides a method for receiving progress updates on long-running
 * tasks.
 */
public interface ProgressCallback {
  /**
   * Receives an update on the progress of a long-running task.
   *
   * @param progress a value in the range [0, 100] that represents the percentage of work completed
   */
  void updateProgress(int progress);
}
