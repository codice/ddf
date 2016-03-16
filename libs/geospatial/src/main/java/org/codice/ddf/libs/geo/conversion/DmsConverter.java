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
package org.codice.ddf.libs.geo.conversion;

import org.codice.ddf.libs.geo.coordinate.DecimalDegreesCoordinate;
import org.codice.ddf.libs.geo.coordinate.DmsCoordinate;
import org.codice.ddf.libs.geo.coordinate.MgrsCoordinate;
import org.codice.ddf.libs.geo.coordinate.UtmCoordinate;

/**
 * Utility class that converts DmsCoordinate objects to other coordinate formats
 * like UTM, MGRS, and Decimal Degrees.
 */
public class DmsConverter {

    /**
     * Converts DMS Coordinate to UTM Coordinate.
     * <p>
     * Note: This method leverages the DecimalDegreeConverter to first convert
     * the coordinate to Decimal Degrees then from Decimal Degrees to DMS.
     *
     * @param dmsCoord the DMS Coordinate to convert
     * @return the UTM representation of the Coordinate
     */
    public static UtmCoordinate convertToUTM(DmsCoordinate dmsCoord) {
        return DecimalDegreesConverter.convertToUTM(convertToDecimalDegrees(dmsCoord));
    }

    /**
     * Converts DMS Coordinate to MGRS Coordinate.
     * <p>
     * Note: This method leverages the DecimalDegreeConverter to first convert
     * the coordinate to Decimal Degrees then from Decimal Degrees to DMS.
     *
     * @param dmsCoord the DMS Coordinate to convert
     * @return the MGRS representation of the Coordinate
     */
    public static MgrsCoordinate convertToMGRS(DmsCoordinate dmsCoord) {
        return DecimalDegreesConverter.convertToMGRS(convertToDecimalDegrees(dmsCoord));
    }

    /**
     * Converts DMS Coordinate to Decimal Degree Coordinate.
     *
     * @param dmsCoord the DMS Coordinate to convert
     * @return the Decimal Degree representation of the Coordinate
     */
    public static DecimalDegreesCoordinate convertToDecimalDegrees(DmsCoordinate dmsCoord) {
        return new DecimalDegreesCoordinate(dmsCoord.getLatitude(), dmsCoord.getLongitude());
    }

}
