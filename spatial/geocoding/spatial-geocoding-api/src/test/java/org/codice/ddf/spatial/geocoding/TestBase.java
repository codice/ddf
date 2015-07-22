/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.geocoding;

import static org.junit.Assert.assertEquals;

public abstract class TestBase {

    protected void verifyGeoEntry(final GeoEntry geoEntry, final String name, final double latitude,
            final double longitude, final String featureCode, final long population) {
        assertEquals(name, geoEntry.getName());
        assertEquals(latitude, geoEntry.getLatitude(), 0);
        assertEquals(longitude, geoEntry.getLongitude(), 0);
        assertEquals(featureCode, geoEntry.getFeatureCode());
        assertEquals(population, geoEntry.getPopulation());
    }
}
