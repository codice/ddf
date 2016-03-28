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
package org.codice.ddf.libs.geo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeospatialUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeospatialUtil.class);

    /**
     * Takes in an array of coordinates and converts it to a (rough
     * approximation) bounding box.
     * <p>
     * Note: Searches being performed where the polygon goes through the
     * international date line may return a bad bounding box.
     *
     * @param polyAry array of coordinates (lon,lat,lon,lat,lon,lat..etc)
     * @return Array of bounding box coordinates in the following order: West
     * South East North. Also described as minX, minY, maxX, maxY (where
     * longitude is the X-axis, and latitude is the Y-axis).
     */
    public static double[] createBBoxFromPolygon(String[] polyAry) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double curX, curY;
        for (int i = 0; i < polyAry.length - 1; i += 2) {
            LOGGER.debug("polyToBBox: lon - " + polyAry[i] + " lat - " + polyAry[i + 1]);
            curX = Double.parseDouble(polyAry[i]);
            curY = Double.parseDouble(polyAry[i + 1]);
            if (curX < minX) {
                minX = curX;
            }
            if (curX > maxX) {
                maxX = curX;
            }

            if (curY < minY) {
                minY = curY;
            }
            if (curY > maxY) {
                maxY = curY;
            }
        }
        return new double[] {minX, minY, maxX, maxY};
    }
}
