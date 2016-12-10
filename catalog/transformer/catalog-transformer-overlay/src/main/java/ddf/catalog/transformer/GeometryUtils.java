/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer;

import org.apache.commons.lang.StringUtils;
import org.geotools.referencing.GeodeticCalculator;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.transform.CatalogTransformerException;

class GeometryUtils {
    static Geometry parseGeometry(String wkt) throws CatalogTransformerException {
        if (StringUtils.isBlank(wkt)) {
            throw new CatalogTransformerException(
                    "The metacard has no location: the overlay image cannot be rotated.");
        }

        try {
            return new WKTReader().read(wkt);
        } catch (ParseException e) {
            throw new CatalogTransformerException(e);
        }
    }

    static boolean canHandleGeometry(Geometry geometry) {
        return geometry.getCoordinates().length == 5;
    }

    static double getRotationAngle(String wkt) throws CatalogTransformerException {
        final Geometry geometry = parseGeometry(wkt);
        if (!canHandleGeometry(geometry)) {
            throw new CatalogTransformerException("The metacard geometry is not a rectangle.");
        }

        final Coordinate[] coordinates = geometry.getCoordinates();
        final GeodeticCalculator calculator = new GeodeticCalculator();
        final Coordinate lowerLeft = coordinates[3];
        calculator.setStartingGeographicPoint(lowerLeft.x, lowerLeft.y);
        final Coordinate upperLeft = coordinates[0];
        calculator.setDestinationGeographicPoint(upperLeft.x, upperLeft.y);
        return calculator.getAzimuth();
    }
}
