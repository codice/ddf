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

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GazetteerGeoCoder implements GeoCoder {
  private static final int SEARCH_RADIUS = 50;

  private static final int SEARCH_RESULT_LIMIT = 1;

  private static final Logger LOGGER = LoggerFactory.getLogger(GazetteerGeoCoder.class);

  private GeoEntryQueryable geoEntryQueryable;

  public void setGeoEntryQueryable(final GeoEntryQueryable geoEntryQueryable) {
    this.geoEntryQueryable = geoEntryQueryable;
  }

  @Override
  @Nullable
  public GeoResult getLocation(final String location) {
    try {
      final List<GeoEntry> topResults = geoEntryQueryable.query(location, 1);

      if (!topResults.isEmpty()) {
        return GeoResultCreator.createGeoResult(topResults.get(0));
      }
    } catch (GeoEntryQueryException e) {
      LOGGER.debug("Error querying GeoNames", e);
    }

    return null;
  }

  @Nullable
  public NearbyLocation getNearbyCity(String location) throws GeoEntryQueryException {

    try {
      List<NearbyLocation> locations =
          geoEntryQueryable.getNearestCities(location, SEARCH_RADIUS, SEARCH_RESULT_LIMIT);

      if (!locations.isEmpty()) {
        return locations.get(0);
      }
    } catch (ParseException parseException) {
      LOGGER.debug(
          "Error parsing the supplied wkt: {}", LogSanitizer.sanitize(location), parseException);
    }

    return null;
  }

  @Override
  public Optional<String> getCountryCode(String locationWKT, int radius) {
    try {
      return geoEntryQueryable.getCountryCode(locationWKT, radius);
    } catch (GeoEntryQueryException e) {
      LOGGER.debug("Error querying GeoNames", e);
    } catch (ParseException e) {
      LOGGER.debug("Error parsing WKT: {} ", locationWKT, e);
    }
    return Optional.empty();
  }
}
