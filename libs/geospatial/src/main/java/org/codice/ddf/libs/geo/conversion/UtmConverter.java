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
 * Utility class that converts UtmCoordinate objects to other coordinate formats
 * like DMS, MGRS, and Degrees Minutes Seconds.
 */
public class UtmConverter {

    private static UTM2LatLon utm2ll = new UTM2LatLon();

    /**
     * Converts UTM Coordinate to MGRS Coordinate.
     *
     * @param utmCoord the UTM Coordinate to convert
     * @return the MGRS representation of the Coordinate
     */
    public static MgrsCoordinate convertToMGRS(UtmCoordinate utmCoord) {
        int residualEasting = (int) utmCoord.getEasting() % MgrsConverter.ONE_HUNDRED_THOUSAND;
        int residualNorthing = (int) utmCoord.getNorthing() % MgrsConverter.ONE_HUNDRED_THOUSAND;

        char colId = computeMGRSGridColumnID(utmCoord.getEasting(), utmCoord.getZone());
        char rowId = computeMGRSGridRowID(utmCoord.getNorthing(), utmCoord.getZone());

        return new MgrsCoordinate(residualEasting,
                residualNorthing,
                utmCoord.getZone(),
                utmCoord.getBand(),
                colId,
                rowId);
    }

    /**
     * Converts Decimal Degree Coordinate to UTM Coordinate.
     *
     * @param utmCoord the UTM Coordinate to convert
     * @return the Decimal Degree representation of the Coordinate
     */
    public static DecimalDegreesCoordinate convertToDecimalDegrees(UtmCoordinate utmCoord) {

        return utm2ll.convertUTMToLatLong(utmCoord);
    }

    /**
     * Converts UTM Coordinate to DMS Coordinate.
     * <p>
     * Note: This method leverages the DecimalDegreeConverter to first convert
     * the coordinate to Decimal Degrees then from Decimal Degrees to DMS.
     *
     * @param utmCoord the UTM Coordinate to convert
     * @return the DMS representation of the Coordinate
     */
    public static DmsCoordinate convertToDMS(UtmCoordinate utmCoord) {
        DecimalDegreesCoordinate ddCoord = convertToDecimalDegrees(utmCoord);
        return DecimalDegreesConverter.convertToDMS(ddCoord);
    }

    /**
     * This method determines the MGRS grid column ID letter. It operates
     * according to the following scheme:
     * <p>
     * The MGRS system identifies locations with a two-character code
     * representing a grid column and a grid row. These codes identify a 100km
     * grid square. (Further precision is specified with numeric coordinates
     * depending on the scale of the source map or image.) MGRS uses the UTM
     * projection and MGRS coordinates can be computed from UTM coordinates
     * relatively easily.
     * <p>
     * Each UTM zone falls into one of six sets, numbered 1-6. Since UTM zones
     * are numbered 1-60, there are 10 zones in a set. A set consists of every
     * 6th zone: set 1 contains zones 1, 7, 13, and so on; set 2 contains zones
     * 2, 8, 14... and set 6 contains zones 6, 12, 18...
     * <p>
     * Therefore the computation of the set number from the zone number is:
     * <p>
     * ( ( zone-1 ) % 6 ) + 1
     * <p>
     * There are 8 grid columns across a zone/set from west to east. The column
     * ID is one of 24 letters between A and Z, excluding I and O, and it
     * repeats every three sets. Therefore, if we number each possible column ID
     * from 1-24, the column ID can be computed from a UTM easting value like
     * so:
     * <p>
     * column = ( (easting/100000) + (set - 1)*8 ) % 24
     * <p>
     * For additional information, see
     * http://www.fmnh.helsinki.fi/map/afe/E_utm.htm or
     * http://maps.nrcan.gc.ca/maps101/mil_grid_ref.html
     *
     * @param utmEasting The easting value to convert to a grid reference
     * @param zone       The UTM zone
     */
    public static char computeMGRSGridColumnID(double utmEasting, int zone) {
        int column = ((int) utmEasting / MgrsConverter.ONE_HUNDRED_THOUSAND) - 1;
        column += ((zone - 1) % 3) * 8;
        column = column % MgrsConverter.GRID_COLUMN_IDS.length;
        assert (column < MgrsConverter.GRID_COLUMN_IDS.length && column > 0);

        return MgrsConverter.GRID_COLUMN_IDS[column];
    }

    /**
     * This method determines the MGRS grid row ID letter. It operates according
     * to the following scheme (see getMGRSGridColumnID() for more information):
     * <p>
     * The MGRS grid row ID is defined to be a letter between A and V, excluding
     * I and O, leaving 20 character IDs. Odd-numbered zones are lettered from A
     * to V northward from the equator, while even- numbered zones are shifted
     * by 5 letters, thus starting at F and continuing to V. Grid row IDs repeat
     * every 2000km, so that after row V there is another row A. To compute the
     * ID index from the northing value:
     * <p>
     * row = (northing/100000) % 20
     * <p>
     * and add 5 if the zone is an even-numbered one.
     * <p>
     * For additional information, see
     * http://www.fmnh.helsinki.fi/map/afe/E_utm.htm or
     * http://maps.nrcan.gc.ca/maps101/mil_grid_ref.html
     *
     * @param utmNorthing The northing value to convert to a grid reference
     * @param zone        The UTM zone
     */
    public static char computeMGRSGridRowID(double utmNorthing, int zone) {
        char result;
        int row = (int) utmNorthing / MgrsConverter.ONE_HUNDRED_THOUSAND;
        if (zone % 2 == 0) {
            // zone is even
            row += 5;
        }
        row = (row) % MgrsConverter.GRID_ROW_IDS.length;
        if (!((0 <= row) && (row < MgrsConverter.GRID_ROW_IDS.length))) {
            result = 'Z'; // invalid number
        } else {
            result = MgrsConverter.GRID_ROW_IDS[row];
        }

        return result;
    }

    /**
     * Internal Private class that handles converting the UTM Coordinate to
     * Decimal Degrees.
     * <p>
     * Class was taken from IBM Developer works at:
     * http://www.ibm.com/developerworks/java/library/j-coordconvert/
     */
    private static class UTM2LatLon {
        double arc;

        double mu;

        double ei;

        double ca;

        double cb;

        double cc;

        double cd;

        double n0;

        double r0;

        double a1;

        double dd0;

        double t0;

        double q0;

        double lof1;

        double lof2;

        double lof3;

        double a2;

        double phi1;

        double fact1;

        double fact2;

        double fact3;

        double fact4;

        double zoneCM;

        double a3;

        double a = 6378137;

        double e = 0.081819191;

        double e1sq = 0.006739497;

        double k0 = 0.9996;

        private double easting;

        // double b = 6356752.314;

        private double northing;

        private int zone;

        /**
         * Converts the UtmCoordinate to DecimalDegreeCoordinate
         *
         * @param utmCoord the UtmCoordinate to convert
         * @return the DecimalDegreeCoordinate
         */
        public DecimalDegreesCoordinate convertUTMToLatLong(UtmCoordinate utmCoord) {

            zone = utmCoord.getZone();

            easting = utmCoord.getEasting();
            northing = utmCoord.getNorthing();
            String hemisphere = utmCoord.isNorthHemisphere() ? "N" : "S";
            double latitude = 0.0;
            double longitude = 0.0;

            if (hemisphere.equals("S")) {
                northing = 10000000 - northing;
            }
            setVariables();
            latitude = 180 * (phi1 - fact1 * (fact2 + fact3 + fact4)) / Math.PI;

            if (zone > 0) {
                zoneCM = 6 * zone - 183.0;
            } else {
                zoneCM = 3.0;

            }

            longitude = zoneCM - a3;
            if (hemisphere.equals("S")) {
                latitude = -latitude;
            }
            return new DecimalDegreesCoordinate(latitude, longitude);

        }

        /**
         * Sets all the variables for doing the conversion from UTM to Decimal
         * Degrees
         */
        private void setVariables() {
            arc = northing / k0;
            mu = arc / (a * (1 - Math.pow(e, 2) / 4.0 - 3 * Math.pow(e, 4) / 64.0 - 5 * Math.pow(e,
                    6) / 256.0));

            ei = (1 - Math.pow((1 - e * e), (1 / 2.0))) / (1 + Math.pow((1 - e * e), (1 / 2.0)));

            ca = 3 * ei / 2 - 27 * Math.pow(ei, 3) / 32.0;

            cb = 21 * Math.pow(ei, 2) / 16 - 55 * Math.pow(ei, 4) / 32;
            cc = 151 * Math.pow(ei, 3) / 96;
            cd = 1097 * Math.pow(ei, 4) / 512;
            phi1 = mu + ca * Math.sin(2 * mu) + cb * Math.sin(4 * mu) + cc * Math.sin(6 * mu)
                    + cd * Math.sin(8 * mu);

            n0 = a / Math.pow((1 - Math.pow((e * Math.sin(phi1)), 2)), (1 / 2.0));

            r0 = a * (1 - e * e) / Math.pow((1 - Math.pow((e * Math.sin(phi1)), 2)), (3 / 2.0));
            fact1 = n0 * Math.tan(phi1) / r0;

            a1 = 500000 - easting;
            dd0 = a1 / (n0 * k0);
            fact2 = dd0 * dd0 / 2;

            t0 = Math.pow(Math.tan(phi1), 2);
            q0 = e1sq * Math.pow(Math.cos(phi1), 2);
            fact3 = (5 + 3 * t0 + 10 * q0 - 4 * q0 * q0 - 9 * e1sq) * Math.pow(dd0, 4) / 24;

            fact4 = (61 + 90 * t0 + 298 * q0 + 45 * t0 * t0 - 252 * e1sq - 3 * q0 * q0) * Math.pow(
                    dd0,
                    6) / 720;

            //
            lof1 = a1 / (n0 * k0);
            lof2 = (1 + 2 * t0 + q0) * Math.pow(dd0, 3) / 6.0;
            lof3 = (5 - 2 * q0 + 28 * t0 - 3 * Math.pow(q0, 2) + 8 * e1sq + 24 * Math.pow(t0, 2))
                    * Math.pow(dd0, 5) / 120;
            a2 = (lof1 - lof2 + lof3) / Math.cos(phi1);
            a3 = a2 * 180 / Math.PI;

        }

    }

}
