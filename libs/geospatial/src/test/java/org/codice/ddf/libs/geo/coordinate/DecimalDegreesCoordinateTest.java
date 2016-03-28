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
import org.codice.ddf.libs.geo.conversion.DecimalDegreesConverter;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests the Decimal Degrees Coordinate and the conversions into the other coordinate formats
 */
public class DecimalDegreesCoordinateTest extends Assert {

    private static final String X_VALUE = "0515124";

    private static final String Y_VALUE = "2755424";

    private static final String HEMISPHERE = "S";

    private static final String ZONE = "58";

    private static final char BAND = 'D';

    private static final char COL_ID = 'E';

    private static final char ROW_ID = 'N';

    private static final String MGRS_X = "15124";

    private static final String MGRS_Y = "55424";

    private static final double LAT_VALUE = -65.32375;

    private static final double LON_VALUE = 165.32465;

    private static final String LAT_DEGREE = "65";

    private static final String LAT_MINUTE = "19";

    private static final String LAT_SECOND = "25";

    private static final Latitude.Hemisphere LAT_HEM = Latitude.Hemisphere.S;

    private static final String LON_DEGREE = "165";

    private static final String LON_MINUTE = "19";

    private static final String LON_SECOND = "28";

    private static final Longitude.Hemisphere LON_HEM = Longitude.Hemisphere.E;

    private static double round(double num) {
        DecimalFormat df = new DecimalFormat("#.#####");
        return Double.valueOf(df.format(num));
    }

    /**
     * Tests the Decimal Degrees Coordinate class to verify that lat/lon values are parsed correctly
     * and returned correctly
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testDecimalDegreesCoordinate() throws GeoFormatException {
        DecimalDegreesCoordinate coord = new DecimalDegreesCoordinate(LAT_VALUE, LON_VALUE);
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
     * Tests to verify a Decimal Degrees Coordinate is successfully translated to MGRS
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testDMSCoodinateToMGRS() throws GeoFormatException {
        DecimalDegreesCoordinate coord = new DecimalDegreesCoordinate(LAT_VALUE, LON_VALUE);
        MgrsCoordinate mgrsCoord = DecimalDegreesConverter.convertToMGRS(coord);
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
     * Tests to verify a Decimal Degrees Coordinate is successfully translated to UTM
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testMGRSCoodinateToUTM() throws GeoFormatException {
        DecimalDegreesCoordinate coord = new DecimalDegreesCoordinate(LAT_VALUE, LON_VALUE);
        UtmCoordinate utmCoord = DecimalDegreesConverter.convertToUTM(coord);
        assertEquals(BAND, utmCoord.getBand());
        assertEquals(Integer.parseInt(X_VALUE), Math.round(utmCoord.getEasting()));
        assertEquals(Integer.parseInt(Y_VALUE), Math.round(utmCoord.getNorthing()));
        assertEquals(Integer.parseInt(ZONE), utmCoord.getZone());
        assertEquals(HEMISPHERE, utmCoord.isNorthHemisphere() ? "N" : "S");
    }

}
