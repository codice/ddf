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
package org.codice.ddf.catalog.ui.query.geofeature;

import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.spatial.geocoder.GeoResult;
import org.codice.ddf.spatial.geocoder.GeoResultCreator;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryException;
import org.codice.ddf.spatial.geocoding.GeoEntryQueryable;
import org.opengis.geometry.primitive.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a FeatureService using gazetteer(s).
 * Currently the only gazetteer is provided by the GeoEntryQueryable interface,
 * which queries geonames.org data. It is intended that more will be added and the results federated.
 */
public class GazetteerFeatureService implements FeatureService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GazetteerFeatureService.class);

    GeoEntryQueryable geoEntryQueryable;

    public void setGeoEntryQueryable(GeoEntryQueryable geoEntryQueryable) {
        this.geoEntryQueryable = geoEntryQueryable;
    }

    @Override
    public List<String> getSuggestedFeatureNames(String query, int maxResults) {
        List<String> results = new ArrayList<>();
        try {
            this.geoEntryQueryable.query(query, maxResults)
                    .forEach(entry -> results.add(entry.getName()));
        } catch (GeoEntryQueryException e) {
            LOGGER.debug("Error while making feature service request.", e);
        }

        return results;
    }

    @Override
    public Feature getFeatureByName(String name) {
        try {
            List<GeoEntry> entries = this.geoEntryQueryable.query(name, 1);
            if (entries.size() > 0) {
                return getFeatureFromGeoEntry(entries.get(0));
            }
        } catch (GeoEntryQueryException e) {
            LOGGER.debug("Error while making feature service request.", e);
        }
        return null;
    }

    private Feature getFeatureFromGeoEntry(GeoEntry entry) {
        GeoResult geoResult = GeoResultCreator.createGeoResult(entry);
        BoundingBoxFeature boundingBoxFeature = new BoundingBoxFeature();
        boundingBoxFeature.setName(geoResult.getFullName());

        List<Point> bbox = geoResult.getBbox();
        boundingBoxFeature.setWest(bbox.get(0)
                .getDirectPosition()
                .getCoordinate()[0]);
        boundingBoxFeature.setNorth(bbox.get(0)
                .getDirectPosition()
                .getCoordinate()[1]);
        boundingBoxFeature.setEast(bbox.get(1)
                .getDirectPosition()
                .getCoordinate()[0]);
        boundingBoxFeature.setSouth(bbox.get(1)
                .getDirectPosition()
                .getCoordinate()[1]);

        return boundingBoxFeature;
    }
}
