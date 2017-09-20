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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public abstract class TestBase {

  protected void verifyGeoEntry(
      final GeoEntry geoEntry,
      final String name,
      final double latitude,
      final double longitude,
      final String featureCode,
      final long population,
      final String alternateNames,
      final String countryCode) {
    assertThat(name, is(geoEntry.getName()));
    assertThat(latitude, is(geoEntry.getLatitude()));
    assertThat(longitude, is(geoEntry.getLongitude()));
    assertThat(featureCode, is(geoEntry.getFeatureCode()));
    assertThat(population, is(geoEntry.getPopulation()));
    assertThat(alternateNames, is(geoEntry.getAlternateNames()));
    assertThat(countryCode, is(geoEntry.getCountryCode()));
  }
}
