/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package org.codice.ddf.ui.searchui.geocoder.geonames;

import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.spatial.geocoding.GeoCodingConstants;
import org.codice.ddf.ui.searchui.geocoder.GeoResult;
import org.geotools.geometry.jts.spatialschema.geometry.DirectPositionImpl;
import org.geotools.geometry.jts.spatialschema.geometry.primitive.PointImpl;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.primitive.Point;

final class GeoResultCreator {
    static GeoResult createGeoResult(final String name, final double latitude,
            final double longitude, final String featureCode, final double population) {
        double latitudeOffset = 0;
        double longitudeOffset = 0;
        if (featureCode.startsWith(GeoCodingConstants.ADMINISTRATIVE_DIVISION)) {
            if (featureCode.endsWith(GeoCodingConstants.DIVISION_FIRST_ORDER)) {
                latitudeOffset = longitudeOffset = 5;
            } else if (featureCode.endsWith(GeoCodingConstants.DIVISION_SECOND_ORDER)) {
                latitudeOffset = longitudeOffset = 4;
            } else if (featureCode.endsWith(GeoCodingConstants.DIVISION_THIRD_ORDER)) {
                latitudeOffset = longitudeOffset = 3;
            } else if (featureCode.endsWith(GeoCodingConstants.DIVISION_FOURTH_ORDER)) {
                latitudeOffset = longitudeOffset = 2;
            } else if (featureCode.endsWith(GeoCodingConstants.DIVISION_FIFTH_ORDER)) {
                latitudeOffset = longitudeOffset = 1;
            }
        } else if (featureCode.startsWith(GeoCodingConstants.POLITICAL_ENTITY)) {
            latitudeOffset = longitudeOffset = 6;
            if (population > 100000000) {
                latitudeOffset *= 2;
                longitudeOffset *= 2;
            } else if (population > 10000000) {
                latitudeOffset *= 1;
                longitudeOffset *= 1;
            } else if (population > 1000000) {
                latitudeOffset *= 0.8;
                longitudeOffset *= 0.8;
            } else if (population > 0){
                latitudeOffset *= 0.5;
                longitudeOffset *= 0.5;
            }
        } else if (featureCode.startsWith(GeoCodingConstants.POPULATED_PLACE)) {
            latitudeOffset = longitudeOffset = 0.5;
            if (population > 10000000) {
                latitudeOffset *= 1.5;
                longitudeOffset *= 1.5;
            } else if (population > 1000000) {
                latitudeOffset *= 0.8;
                longitudeOffset *= 0.8;
            } else if (population > 100000) {
                latitudeOffset *= 0.5;
                longitudeOffset *= 0.5;
            } else if (population > 10000) {
                latitudeOffset *= 0.3;
                longitudeOffset *= 0.3;
            } else if (population > 0) {
                latitudeOffset *= 0.2;
                longitudeOffset *= 0.2;
            }
        } else {
            latitudeOffset = longitudeOffset = 0.1;
        }

        final DirectPosition northWest = new DirectPositionImpl(longitude - longitudeOffset,
                latitude + latitudeOffset);
        final DirectPosition southEast = new DirectPositionImpl(longitude + longitudeOffset,
                latitude - latitudeOffset);
        final List<Point> bbox = new ArrayList<>();
        bbox.add(new PointImpl(northWest));
        bbox.add(new PointImpl(southEast));

        final DirectPosition directPosition = new DirectPositionImpl(longitude, latitude);

        final GeoResult geoResult = new GeoResult();
        geoResult.setPoint(new PointImpl(directPosition));
        geoResult.setBbox(bbox);
        geoResult.setFullName(name);
        return geoResult;
    }
}
