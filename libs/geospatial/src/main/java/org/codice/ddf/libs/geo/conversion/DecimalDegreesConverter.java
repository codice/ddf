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
 * Utility class that converts DecimalDegreesCoordinate objects to other
 * coordinate formats like UTM, MGRS, Degrees Minutes Seconds, and GML (JAXB
 * representations).
 */
public class DecimalDegreesConverter {

    private static LatLon2UTM ll2utm = new LatLon2UTM();

    /**
     * Converts Decimal Degrees to UTM Coordinate formats
     *
     * @param ddCoord the Decimal Degrees Coordinate to convert
     * @return the UTM representation of the Coordinate
     */
    public static UtmCoordinate convertToUTM(DecimalDegreesCoordinate ddCoord) {
        return ll2utm.convertLatLonToUTM(ddCoord);
    }

    /**
     * Converts Decimal Degree Coordinate to MGRS Coordinate.
     * <p>
     * Note: This method leverages the UtmConverter to first convert the
     * coordinate to UTM then from UTM to MGRS.
     *
     * @param ddCoord the Decimal Degrees Coordinate to convert
     * @return the MGRS representation of the Coordinate
     */
    public static MgrsCoordinate convertToMGRS(DecimalDegreesCoordinate ddCoord) {
        return UtmConverter.convertToMGRS(convertToUTM(ddCoord));
    }

    /**
     * Converts Decimal Degree Coordinate to DMS (Degrees Minutes Seconds)
     * Coordinate.
     *
     * @param ddCoord the Decimal Degrees Coordinate to convert
     * @return the DMS representation of the Coordinate
     */
    public static DmsCoordinate convertToDMS(DecimalDegreesCoordinate ddCoord) {
        return new DmsCoordinate(ddCoord.getLatitude(), ddCoord.getLongitude());
    }

    /**
     * Internal Private class that handles converting the Decimal Degree
     * Coordinate to UTM.
     * <p>
     * Class was taken from IBM Developer works at:
     * http://www.ibm.com/developerworks/java/library/j-coordconvert/
     */
    private static class LatLon2UTM {
        // equatorial radius
        double equatorialRadius = 6378137;

        // polar radius
        double polarRadius = 6356752.314;

        // scale factor
        double k0 = 0.9996;

        // eccentricity
        double e = Math.sqrt(1 - Math.pow(polarRadius / equatorialRadius, 2));

        double e1sq = e * e / (1 - e * e);

        // r curv 2
        double nu = 6389236.914;

        // Lat Lon to UTM variables

        // Calculate Meridional Arc Length
        // Meridional Arc
        double s = 5103266.421;

        double a0 = 6367449.146;

        double b0 = 16038.42955;

        double c0 = 16.83261333;

        double d0 = 0.021984404;

        double e0 = 0.000312705;

        // Calculation Constants
        // Delta Long
        double p = -0.483084;

        double sin1 = 4.84814E-06;

        // Coefficients for UTM Coordinates
        double k1 = 5101225.115;

        double k2 = 3750.291596;

        double k3 = 1.397608151;

        double k4 = 214839.3105;

        double k5 = -2.995382942;

        /**
         * Converts a DecimalDegreeCoordinate to a UtmCoordinate
         *
         * @param ddCoord the DecimalDegreeCoordinate to convert
         * @return the coordinate value in UTM Format.
         */
        public UtmCoordinate convertLatLonToUTM(DecimalDegreesCoordinate ddCoord) {
            double latitude = ddCoord.getLatitiudeDegrees();
            double longitude = ddCoord.getLongitudeDegrees();

            setVariables(latitude, longitude);

            int longZone = getLongZone(longitude);
            LatZones latZones = new LatZones();
            char latZone = latZones.getLatZone(latitude);

            double easting = getEasting();
            double northing = getNorthing(latitude);

            return new UtmCoordinate(longZone, latZone, easting, northing);

        }

        /**
         * Converts the decimal degree to radians
         *
         * @param degree the decimal degree to convert
         * @return the coordinate value in radians
         */
        private double degreeToRadian(double degree) {
            return degree * Math.PI / 180;
        }

        /**
         * Sets all the variables for doing the conversion to UTM
         *
         * @param latitude  the latitude value (in decimal degree format) to convert
         *                  to UTM
         * @param longitude the longitude value (in decimal degree format) to convert
         *                  to UTM
         */
        private void setVariables(double latitude, double longitude) {
            latitude = degreeToRadian(latitude);
            nu = equatorialRadius / Math.pow(1 - Math.pow(e * Math.sin(latitude), 2), (1 / 2.0));

            double var1;
            if (longitude < 0.0) {
                var1 = ((int) ((180 + longitude) / 6.0)) + 1;
            } else {
                var1 = ((int) (longitude / 6)) + 31;
            }
            double var2 = (6 * var1) - 183;
            double var3 = longitude - var2;
            p = var3 * 3600 / 10000;

            s = a0 * latitude - b0 * Math.sin(2 * latitude) + c0 * Math.sin(4 * latitude)
                    - d0 * Math.sin(6 * latitude) + e0 * Math.sin(8 * latitude);

            k1 = s * k0;
            k2 = nu * Math.sin(latitude) * Math.cos(latitude) * Math.pow(sin1, 2) * k0 * (100000000)
                    / 2;
            k3 = ((Math.pow(sin1, 4) * nu * Math.sin(latitude) * Math.pow(Math.cos(latitude), 3))
                    / 24) * (5 - Math.pow(Math.tan(latitude), 2) + 9 * e1sq * Math.pow(Math.cos(
                    latitude), 2) + 4 * Math.pow(e1sq, 2) * Math.pow(Math.cos(latitude), 4)) * k0
                    * (10000000000000000L);

            k4 = nu * Math.cos(latitude) * sin1 * k0 * 10000;

            k5 = Math.pow(sin1 * Math.cos(latitude), 3) * (nu / 6) * (
                    1 - Math.pow(Math.tan(latitude), 2) + e1sq * Math.pow(Math.cos(latitude), 2))
                    * k0 * 1000000000000L;
        }

        /**
         * Computes the Longitude zone (UTM Zone)
         *
         * @param longitude the logitude value (in decimal degree format) to convert
         *                  the UTM Zone for.
         * @return
         */
        private int getLongZone(double longitude) {
            double longZone = 0;
            if (longitude < 0.0) {
                longZone = ((180.0 + longitude) / 6) + 1;
            } else {
                longZone = (longitude / 6) + 31;
            }
            return (int) longZone;
        }

        /**
         * Computers the UTM Northing value
         *
         * @param latitude the latitude value (in decimal degree format) to compute
         *                 the UTM Northing for.
         * @return the double value for the UTM Northing
         */
        private double getNorthing(double latitude) {
            double northing = k1 + k2 * p * p + k3 * Math.pow(p, 4);
            if (latitude < 0.0) {
                northing = 10000000 + northing;
            }
            return northing;
        }

        /**
         * Computers the UTM Easting value
         *
         * @return the double value for the UTM easting
         */
        private double getEasting() {
            return 500000 + (k4 * p + k5 * Math.pow(p, 3));
        }

    }

    /**
     * Class that represents the UTM LatitudeZones also know as the UTM band
     * <p>
     * Class was taken from IBM Developer works at:
     * http://www.ibm.com/developerworks/java/library/j-coordconvert/
     */
    public static class LatZones {

        private char[] negLetters = {'A', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M'};

        private int[] negDegrees = {-90, -84, -72, -64, -56, -48, -40, -32, -24, -16, -8};

        private char[] posLetters = {'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Z'};

        private int[] posDegrees = {0, 8, 16, 24, 32, 40, 48, 56, 64, 72, 84};

        public LatZones() {
        }

        /**
         * Gets the Latitude Zone (UTM Band) for the latitude degree
         *
         * @param latitude Latitude in decimal degree format
         * @return the UTM zone for the latitude
         */
        public char getLatZone(double latitude) {
            int latIndex = -2;
            int lat = (int) latitude;

            if (lat >= 0) {
                int len = posLetters.length;
                for (int i = 0; i < len; i++) {
                    if (lat == posDegrees[i]) {
                        latIndex = i;
                        break;
                    }

                    if (lat > posDegrees[i]) {
                        continue;
                    } else {
                        latIndex = i - 1;
                        break;
                    }
                }
            } else {
                int len = negLetters.length;
                for (int i = 0; i < len; i++) {
                    if (lat == negDegrees[i]) {
                        latIndex = i;
                        break;
                    }

                    if (lat < negDegrees[i]) {
                        latIndex = i - 1;
                        break;
                    } else {
                        continue;
                    }

                }

            }

            if (latIndex == -1) {
                latIndex = 0;
            }
            if (lat >= 0) {
                if (latIndex == -2) {
                    latIndex = posLetters.length - 1;
                }
                return posLetters[latIndex];
            } else {
                if (latIndex == -2) {
                    latIndex = negLetters.length - 1;
                }
                return negLetters[latIndex];

            }
        }

    }

}
