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
package org.codice.ddf.spatial.geocoder;

import java.util.Optional;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;

public interface GeoCoder {
  /**
   * Takes a query for a place and returns the most relevant result.
   *
   * @param location a string representing a simple placename query, such as "Washington, D.C." or
   *     "France" (i.e. the string just contains search terms, not query logic)
   * @return the {@link GeoResult} most relevant to the query, null if no results were found
   */
  GeoResult getLocation(String location);

  /**
   * @param locationWKT - a WKT string describing the area to search
   * @return a description of the "nearest city"
   * @throws GeoEntryQueryException if an exception occurs while querying the GeoNames resource
   */
  NearbyLocation getNearbyCity(String locationWKT) throws GeoEntryQueryException;

  /**
   * Retrieves the country code for a given location. The center point is used if {@code
   * locationWKT} is a polygon.
   *
   * @param locationWKT WKT location for which to get the country code of
   * @param radius Radius in kilometers to search from the center of {@code locationWKT}
   * @return a country code in ISO 3166-1 alpha-3 format or null if not found (for example, a
   *     location in the ocean)
   */
  Optional<String> getCountryCode(String locationWKT, int radius);
}
