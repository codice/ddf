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
 * A {@code GeoEntryCreator} provides methods for creating {@link GeoEntry} objects from various
 * GeoNames data formats.
 */
public interface GeoEntryCreator {
  /**
   * Creates a {@link GeoEntry} from a {@link String} representing a GeoNames entry.
   *
   * @param line a {@code String} that represents one GeoNames entry
   * @param entryResource resource name that represents the source of the geonames entry (e.g. file)
   * @return a {@code GeoEntry} object that represents the GeoNames entry in the {@code String}
   */
  GeoEntry createGeoEntry(String line, String entryResource);
}
