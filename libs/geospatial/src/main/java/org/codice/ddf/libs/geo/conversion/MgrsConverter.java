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
 * Utility class that converts MgrsCoordinate objects to other coordinate
 * formats like UTM, DMS, and Decimal Degrees.
 */
public class MgrsConverter {

    static final char[] GRID_ROW_IDS =
            {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S',
                    'T', 'U', 'V'};

    static final char[] GRID_COLUMN_IDS =
            {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S',
                    'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    public static final int ONE_HUNDRED_THOUSAND = 100000;

    /**
     * Converts MGRS Coordinate to UTM Coordinate format
     *
     * @param mgrsCoord the MGRS Coordinate to convert
     * @return the UTM representation of the Coordinate
     */
    public static UtmCoordinate convertToUTM(MgrsCoordinate mgrsCoord) {

        char band = mgrsCoord.getBand();
        int zone = mgrsCoord.getZone();

        int gridColIndex = indexOf(mgrsCoord.getGridColumnID(), GRID_COLUMN_IDS);

        gridColIndex = gridColIndex % 8;

        double easting = (gridColIndex + 1) * ONE_HUNDRED_THOUSAND + mgrsCoord.getX();
        double northing = computeGridRowNumber(band, mgrsCoord.getGridRowID(), zone)
                * ONE_HUNDRED_THOUSAND + mgrsCoord.getY();

        return new UtmCoordinate(zone, band, easting, northing);
    }

    /**
     * Converts MGRS Coordinate to Decimal Degree Coordinate.
     * <p>
     * Note: This method leverages the UtmConverter to first convert the
     * coordinate to UTM then from UTM to Decimal Degrees.
     *
     * @param mgrsCoord the MGRS Coordinate to convert
     * @return the Decimal Degree representation of the Coordinate
     */
    public static DecimalDegreesCoordinate convertToDecimalDegrees(MgrsCoordinate mgrsCoord) {
        UtmCoordinate utmCoord = convertToUTM(mgrsCoord);
        return UtmConverter.convertToDecimalDegrees(utmCoord);
    }

    /**
     * Converts MGRS Coordinate to DMS Coordinate.
     * <p>
     * Note: This method leverages the UtmConverter to first convert the
     * coordinate to UTM then from UTM to Decimal Degrees.
     *
     * @param mgrsCoord the MGRS Coordinate to convert
     * @return the DMS representation of the Coordinate
     */
    public static DmsCoordinate convertToDMS(MgrsCoordinate mgrsCoord) {
        UtmCoordinate utmCoord = convertToUTM(mgrsCoord);
        return UtmConverter.convertToDMS(utmCoord);
    }

    /**
     * Computes the index of the given character in an array. Used for
     * determining the grid row and column number from the letter.
     *
     * @param id   The identifier to search on
     * @param list The array of characters to search in
     * @return The index of the character, or -1 if not found
     */
    private static int indexOf(char id, char[] list) {
        int result = -1;
        int i = 0;

        while (result == -1 && i < list.length) {
            if (list[i] == id) {
                result = i;
            }
            i++;
        }
        return result;
    }

    /**
     * Computes the MGRS grid row number based on the band, the grid row letter,
     * and the zone.
     * <p>
     * This method essentially computes the "northing base" or the northing
     * value for the lower edge of a particular MGRS grid square. The number
     * returned represents how many grid squares above the baseline (northing 0)
     * this particular square is located.
     * <p>
     * For additional information, see
     * http://www.fmnh.helsinki.fi/map/afe/E_utm.htm or
     * http://maps.nrcan.gc.ca/maps101/mil_grid_ref.html
     *
     * @param band    The letter representing the band in which this grid square is
     *                located
     * @param gridRow The letter identifier of this grid square
     * @param zone    The zone number in which the grid square is located
     * @return The "row number" of this square above northing 0;
     */
    public static int computeGridRowNumber(char band, char gridRow, int zone) {
        int result = 0;

        // the offset is adjusted for certain longitudinal zones which
        // are treated differently from others. In particular, odd and
        // even zones have a 5-row offset in their lettering schemes
        // according to standard MGRS encoding
        int offset = 0;

        // each "cycle" refers to one set of all 20 letters in the grid
        // row designation alphabet. Therefore, the square designated 'C'
        // in cycle 0 would be numbered 3, square C in cycle 1 would be
        // numbered 23, square C in cycle 2 would be 43, and so on.
        int cycle = 0;

        // the following code segment simply computes the cycle and
        // offset of a particular grid square based on its band, row ID,
        // and zone. However, since a given band is not easily mapped to
        // a set of grid rows, each band must be handled separately.
        // The lettered comments for each case indicate the grid rows
        // which are entirely or partially enclosed in the specified band.
        if (zone % 2 == 1) {
            // odd zone -- "normal" lettering

            switch (band) {
            case 'C': // M,N,P,Q,R,S,T,U,V,A
                if ('L' < gridRow && gridRow <= 'V') {
                    cycle = 0;
                } else {
                    cycle = 1;
                }
                break;
            case 'D': // A,B,C,D,E,F,G,H,J,K
                cycle = 1;
                break;
            case 'E': // K,L,M,N,P,Q,R,S,T
                cycle = 1;
                break;
            case 'F': // T,U,V,A,B,C,D,E,F,G
                if ('S' < gridRow && gridRow <= 'V') {
                    cycle = 1;
                } else {
                    cycle = 2;
                }
                break;
            case 'G': // G,H,J,K,L,M,N,P,Q,R
                cycle = 2;
                break;
            case 'H': // R,S,T,U,V,A,B,C,D,E
                if ('Q' < gridRow && gridRow <= 'V') {
                    cycle = 2;
                } else {
                    cycle = 3;
                }
                break;
            case 'J': // E,F,G,H,J,K,L,M,N,P
                cycle = 3;
                break;
            case 'K': // P,Q,R,S,T,U,V,A,B,C
                if ('N' < gridRow && gridRow <= 'V') {
                    cycle = 3;
                } else {
                    cycle = 4;
                }
                break;
            case 'L': // C,D,E,F,G,H,J,K,L,M
                cycle = 4;
                break;
            case 'M': // M,N,P,Q,R,S,T,U,V
                cycle = 4;
                break;
            case 'N': // A,B,C,D,E,F,G,H,J
                cycle = 0;
                break;
            case 'P': // J,K,L,M,N,P,Q,R,S,T
                cycle = 0;
                break;
            case 'Q': // T,U,V,A,B,C,D,E,F,G
                if ('S' < gridRow && gridRow <= 'V') {
                    cycle = 0;
                } else {
                    cycle = 1;
                }
                break;
            case 'R': // G,H,J,K,L,M,N,P,Q,R
                cycle = 1;
                break;
            case 'S': // R,S,T,U,V,A,B,C,D,E
                if ('Q' < gridRow && gridRow <= 'V') {
                    cycle = 1;
                } else {
                    cycle = 2;
                }
                break;
            case 'T': // E,F,G,H,J,K,L,M,N,P
                cycle = 2;
                break;
            case 'U': // P,Q,R,S,T,U,V,A,B,C
                if ('N' < gridRow && gridRow <= 'V') {
                    cycle = 2;
                } else {
                    cycle = 3;
                }
                break;
            case 'V': // C,D,E,F,G,H,J,K,L
                cycle = 3;
                break;
            case 'W': // L,M,N,P,Q,R,S,T,U,V
                cycle = 4;
                break;
            case 'X': // V,A,B,C,D,E,F,G,H,J,K,L,M,N,P
                if (gridRow == 'V') {
                    cycle = 4;
                } else {
                    cycle = 5;
                }
                break;
            default:
                break;
            }
        } else {
            // zone is even

            offset = -5;
            switch (band) {
            case 'C': // S,T,U,V,A,B,C,D,E,F
                if ('R' < gridRow && gridRow <= 'V') {
                    cycle = 0;
                } else {
                    cycle = 1;
                }
                break;
            case 'D': // F,G,H,J,K,L,M,N,P,Q
                cycle = 1;
                break;
            case 'E': // Q,R,S,T,U,V,A,B,C
                if ('P' < gridRow && gridRow <= 'V') {
                    cycle = 1;
                } else {
                    cycle = 2;
                }
                break;
            case 'F': // C,D,E,F,G,H,J,K,L,M
                cycle = 2;
                break;
            case 'G': // M,N,P,Q,R,S,T,U,V,A
                if ('L' < gridRow && gridRow <= 'V') {
                    cycle = 2;
                } else {
                    cycle = 3;
                }
                break;
            case 'H': // A,B,C,D,E,F,G,H,J,K
                cycle = 3;
                break;
            case 'J': // K,L,M,N,P,Q,R,S,T,U
                cycle = 3;
                break;
            case 'K': // U,V,A,B,C,D,E,F,G,H
                if ('T' < gridRow && gridRow <= 'V') {
                    cycle = 3;
                } else {
                    cycle = 4;
                }
                break;
            case 'L': // H,J,K,L,M,N,P,Q,R,S
                cycle = 4;
                break;
            case 'M': // S,T,U,V,A,B,C,D,E
                if ('R' < gridRow && gridRow <= 'V') {
                    cycle = 4;
                } else {
                    cycle = 5;
                }
                break;
            case 'N': // F,G,H,J,K,L,M,N,P
                cycle = 0;
                break;
            case 'P': // P,Q,R,S,T,U,V,A,B,C
                if ('N' < gridRow && gridRow <= 'V') {
                    cycle = 0;
                } else {
                    cycle = 1;
                }
                break;
            case 'Q': // C,D,E,F,G,H,J,K,L,M
                cycle = 1;
                break;
            case 'R': // M,N,P,Q,R,S,T,U,V,A
                if ('L' < gridRow && gridRow <= 'V') {
                    cycle = 1;
                } else {
                    cycle = 2;
                }
                break;
            case 'S': // A,B,C,D,E,F,G,H,J,K
                cycle = 2;
                break;
            case 'T': // K,L,M,N,P,Q,R,S,T,U
                cycle = 2;
                break;
            case 'U': // U,V,A,B,C,D,E,F,G,H
                if ('T' < gridRow && gridRow <= 'V') {
                    cycle = 2;
                } else {
                    cycle = 3;
                }
                break;
            case 'V': // H,J,K,L,M,N,P,Q,R
                cycle = 3;
                break;
            case 'W': // R,S,T,U,V,A,B,C,D,E
                if ('Q' < gridRow && gridRow <= 'V') {
                    cycle = 3;
                } else {
                    cycle = 4;
                }
                break;
            case 'X': // E,F,G,H,J,K,L,M,N,P,Q,R,S,T,U
                cycle = 4;
                break;
            default:
                break;
            }
        }
        result = indexOf(gridRow, GRID_ROW_IDS) + offset + cycle * GRID_ROW_IDS.length;
        return result;
    }

}
