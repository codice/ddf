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

package org.codice.ddf.spatial.geo.create;

import org.codice.ddf.spatial.geo.GeoEntry;
import org.codice.ddf.spatial.geo.GeoEntryCreator;

/**
 * An implementation of {@link GeoEntryCreator} for GeoNames data obtained from
 * <a href="http://download.geonames.org/export/dump">geonames.org</a>.
 */
public class GeoNamesCreator implements GeoEntryCreator {
    @Override
    public GeoEntry createGeoEntry(final String line) {
        // Passing a negative value to preserve empty fields.
        final String[] fields = line.split("\\t", -1);

        return new GeoEntry.Builder()
                .name(fields[1])
                .latitude(Double.parseDouble(fields[4]))
                .longitude(Double.parseDouble(fields[5]))
                .featureCode(fields[7])
                .population(Long.parseLong(fields[14]))
                .build();
    }
}
