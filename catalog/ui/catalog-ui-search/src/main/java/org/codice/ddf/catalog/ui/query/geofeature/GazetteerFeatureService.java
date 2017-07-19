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

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;

import java.util.Collections;
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

    private GeoEntryQueryable geoEntryQueryable;

    public void setGeoEntryQueryable(GeoEntryQueryable geoEntryQueryable) {
        this.geoEntryQueryable = geoEntryQueryable;
    }

    @Override
    public List<String> getSuggestedFeatureNames(String query, int maxResults) {
        try {
            return geoEntryQueryable.query(query, maxResults)
                    .stream()
                    .map(GeoEntry::getName)
                    .collect(toList());
        } catch (GeoEntryQueryException e) {
            LOGGER.debug("Error while making feature service request.", e);
            return Collections.emptyList();
        }
    }

    @Override
    public Feature getFeatureByName(String name) {
        try {
            List<GeoEntry> entries = this.geoEntryQueryable.query(name, 1);
            if (!entries.isEmpty()) {
                GeoResult geoResult = getGeoResultFromGeoEntry(entries.get(0));
                return getFeatureFromGeoResult(geoResult);
            }
        } catch (GeoEntryQueryException e) {
            LOGGER.debug("Error while making feature service request.", e);
        }
        return null;
    }

    protected GeoResult getGeoResultFromGeoEntry(GeoEntry entry) {
        return GeoResultCreator.createGeoResult(entry);
    }

    private Feature getFeatureFromGeoResult(GeoResult geoResult) {

        BoundingBoxFeature boundingBoxFeature = new BoundingBoxFeature();
        boundingBoxFeature.setName(geoResult.getFullName());

        List<Point> bbox = geoResult.getBbox();
        if (isNotEmpty(bbox)) {
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
        }
        return boundingBoxFeature;
    }
}
