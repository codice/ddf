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
 * Thrown by a {@link GeoEntryExtractor} when an error occurs during the extraction of GeoNames
 * entries from a resource.
 */
public class GeoEntryExtractionException extends Exception {
  /**
   * Instantiates a new exception with the provided message.
   *
   * @param message the message
   */
  public GeoEntryExtractionException(final String message) {
    super(message);
  }

  /**
   * Instantiates a new exception with the provided message and {@link Throwable}.
   *
   * @param message the message
   * @param throwable the throwable
   */
  public GeoEntryExtractionException(final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
