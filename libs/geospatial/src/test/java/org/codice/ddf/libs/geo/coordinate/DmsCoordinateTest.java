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
package org.codice.ddf.libs.geo.coordinate;

import java.text.DecimalFormat;

import org.codice.ddf.libs.geo.GeoFormatException;
import org.codice.ddf.libs.geo.conversion.DmsConverter;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests the DMS Coordinate and the conversions into the other coordinate formats
 */
public class DmsCoordinateTest extends Assert {

    private static final String X_VALUE = "0506417";

    private static final String Y_VALUE = "4975943";

    private static final String HEMISPHERE = "S";

    private static final String ZONE = "17";

    private static final char BAND = 'G';

    private static final char COL_ID = 'N';

    private static final char ROW_ID = 'K';

    private static final String MGRS_X = "06417";

    private static final String MGRS_Y = "75943";

    private static final double LAT_VALUE = -45.37000;

    private static final double LON_VALUE = -80.91806;

    private static final String LAT_DEGREE = "45";

    private static final String LAT_MINUTE = "22";

    private static final String LAT_SECOND = "12";

    private static final Latitude.Hemisphere LAT_HEM = Latitude.Hemisphere.S;

    private static final String LON_DEGREE = "080";

    private static final String LON_MINUTE = "55";

    private static final String LON_SECOND = "05";

    private static final Longitude.Hemisphere LON_HEM = Longitude.Hemisphere.W;

    private static double round(double num) {
        DecimalFormat df = new DecimalFormat("#.#####");
        return Double.valueOf(df.format(num));
    }

    /**
     * Tests the DMS Coordinate class to verify that lat/lon values are parsed correctly
     * and returned correctly
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testDMSCoordinate() throws GeoFormatException {
        DmsCoordinate coord = new DmsCoordinate(
                LAT_DEGREE + LAT_MINUTE + LAT_SECOND + LAT_HEM + LON_DEGREE + LON_MINUTE
                        + LON_SECOND + LON_HEM);
        assertEquals(LAT_VALUE,
                coord.getLatitude()
                        .getDecimalDegrees());
        assertEquals(LON_VALUE,
                round(coord.getLongitude()
                        .getDecimalDegrees()));

        assertEquals(Integer.parseInt(LAT_DEGREE),
                coord.getLatitude()
                        .getDegree());
        assertEquals(Integer.parseInt(LAT_MINUTE),
                coord.getLatitude()
                        .getMinute());
        assertEquals(Integer.parseInt(LAT_SECOND),
                coord.getLatitude()
                        .getSecond());
        assertEquals(LAT_HEM,
                coord.getLatitude()
                        .getHemisphere());

        assertEquals(Integer.parseInt(LON_DEGREE),
                coord.getLongitude()
                        .getDegree());
        assertEquals(Integer.parseInt(LON_MINUTE),
                coord.getLongitude()
                        .getMinute());
        assertEquals(Integer.parseInt(LON_SECOND),
                coord.getLongitude()
                        .getSecond());
        assertEquals(LON_HEM,
                coord.getLongitude()
                        .getHemisphere());
    }

    /**
     * Tests to verify a DMS Coordinate is successfully translated to MGRS
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testDMSCoodinateToMGRS() throws GeoFormatException {
        DmsCoordinate coord = new DmsCoordinate(
                LAT_DEGREE + LAT_MINUTE + LAT_SECOND + LAT_HEM + LON_DEGREE + LON_MINUTE
                        + LON_SECOND + LON_HEM);
        MgrsCoordinate mgrsCoord = DmsConverter.convertToMGRS(coord);
        assertEquals(BAND, mgrsCoord.getBand());
        assertEquals(COL_ID, mgrsCoord.getGridColumnID());
        assertEquals(ROW_ID, mgrsCoord.getGridRowID());
        assertTrue(Double.parseDouble(MGRS_X) >= mgrsCoord.getX() - 1);
        assertTrue(Double.parseDouble(MGRS_X) <= mgrsCoord.getX() + 1);
        assertTrue(Double.parseDouble(MGRS_Y) >= mgrsCoord.getY() - 1);
        assertTrue(Double.parseDouble(MGRS_Y) <= mgrsCoord.getY() + 1);
        assertEquals(Integer.parseInt(ZONE), mgrsCoord.getZone());

    }

    /**
     * Tests to verify a DMS Coordinate is successfully translated to UTM
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testMGRSCoodinateToUTM() throws GeoFormatException {
        DmsCoordinate coord = new DmsCoordinate(
                LAT_DEGREE + LAT_MINUTE + LAT_SECOND + LAT_HEM + LON_DEGREE + LON_MINUTE
                        + LON_SECOND + LON_HEM);
        UtmCoordinate utmCoord = DmsConverter.convertToUTM(coord);
        assertEquals(BAND, utmCoord.getBand());
        assertEquals(Integer.parseInt(X_VALUE), Math.round(utmCoord.getEasting()));
        assertEquals(Integer.parseInt(Y_VALUE), Math.round(utmCoord.getNorthing()));
        assertEquals(Integer.parseInt(ZONE), utmCoord.getZone());
        assertEquals(HEMISPHERE, utmCoord.isNorthHemisphere() ? "N" : "S");
    }

}
