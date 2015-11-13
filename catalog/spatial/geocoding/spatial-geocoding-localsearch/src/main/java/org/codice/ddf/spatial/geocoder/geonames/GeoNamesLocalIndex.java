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

package org.codice.ddf.spatial.geocoder.geonames;

import java.text.ParseException;
import java.util.List;

import org.codice.ddf.spatial.geocoder.GeoCoder;
import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoder.GeoResultCreator;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoNamesLocalIndex implements GeoCoder {
    private static final int SEARCH_RADIUS = 50;
    private static final int SEARCH_RESULT_LIMIT = 1;

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesLocalIndex.class);

    private GeoEntryQueryable geoEntryQueryable;

    public void setGeoEntryQueryable(final GeoEntryQueryable geoEntryQueryable) {
        this.geoEntryQueryable = geoEntryQueryable;
    }

    @Override
    public GeoResult getLocation(final String location) {
        try {
            final List<GeoEntry> topResults = geoEntryQueryable.query(location, 1);

            if (topResults.size() > 0) {
                final GeoEntry topResult = topResults.get(0);
                final String name = topResult.getName();
                final double latitude = topResult.getLatitude();
                final double longitude = topResult.getLongitude();
                final String featureCode = topResult.getFeatureCode();
                final long population = topResult.getPopulation();

                return GeoResultCreator
                        .createGeoResult(name, latitude, longitude, featureCode, population);
            }
        } catch (GeoEntryQueryException e) {
            LOGGER.error("Error querying the local GeoNames index", e);
        }

        return null;
    }

    public NearbyLocation getNearbyCity(String location) {

        try {
            List<NearbyLocation> locations = geoEntryQueryable
                    .getNearestCities(location, SEARCH_RADIUS, SEARCH_RESULT_LIMIT);

            if (locations.size() > 0) {
                return locations.get(0);
            }
        } catch (ParseException parseException) {
            LOGGER.error(String.format("Error parsing the supplied wkt: %s", location), parseException);
        }

        return null;
    }
}
