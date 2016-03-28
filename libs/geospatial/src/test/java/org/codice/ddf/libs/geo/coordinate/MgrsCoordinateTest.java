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
import org.codice.ddf.libs.geo.conversion.MgrsConverter;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests the MGRS Coordinate and the conversions into the other coordinate formats
 */
public class MgrsCoordinateTest extends Assert {

    private static final String X_VALUE = "0373575";

    private static final String Y_VALUE = "3700216";

    private static final String HEMISPHERE = "N";

    private static final String ZONE = "12";

    private static final char BAND = 'S';

    private static final char COL_ID = 'U';

    private static final char ROW_ID = 'C';

    private static final String MGRS_X = "73575";

    private static final String MGRS_Y = "00216";

    private static final double LAT_VALUE = 33.43388;

    private static final double LON_VALUE = -112.36000;

    private static final int LAT_DEGREE = 33;

    private static final int LAT_MINUTE = 26;

    private static final int LAT_SECOND = 01;

    private static final int LON_DEGREE = -112;

    private static final int LON_MINUTE = 21;

    private static final int LON_SECOND = 36;

    private static double round(double num) {
        DecimalFormat df = new DecimalFormat("#.#####");
        return Double.valueOf(df.format(num));
    }

    /**
     * Tests to see if the MGRS Constructor parses the MGRS String correctly
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testMGRSCoodinate() throws GeoFormatException {
        MgrsCoordinate coord = new MgrsCoordinate(ZONE + BAND + COL_ID + ROW_ID + MGRS_X + MGRS_Y);
        assertEquals(Double.parseDouble(MGRS_X), coord.getX());
        assertEquals(Double.parseDouble(MGRS_Y), coord.getY());
        assertEquals(BAND, coord.getBand());
        assertEquals(Integer.parseInt(ZONE), coord.getZone());
        assertEquals(COL_ID, coord.getGridColumnID());
        assertEquals(ROW_ID, coord.getGridRowID());
    }

    /**
     * Tests to verify a MGRS Coordinate is successfully translated to Decimal Degrees
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testMGRSCoodinateToDecimalDegrees() throws GeoFormatException {

        MgrsCoordinate coord = new MgrsCoordinate(ZONE + BAND + COL_ID + ROW_ID + MGRS_X + MGRS_Y);
        DecimalDegreesCoordinate ddCoord = MgrsConverter.convertToDecimalDegrees(coord);
        assertEquals(LAT_VALUE, round(ddCoord.getLatitiudeDegrees()));
        assertEquals(LON_VALUE, round(ddCoord.getLongitudeDegrees()));

    }

    /**
     * Tests to verify a MGRS Coordinate is successfully translated to DMS
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testMGRSCoodinateToDMS() throws GeoFormatException {
        MgrsCoordinate coord = new MgrsCoordinate(ZONE + BAND + COL_ID + ROW_ID + MGRS_X + MGRS_Y);
        DmsCoordinate dmsCoord = MgrsConverter.convertToDMS(coord);
        assertEquals(Math.abs(LAT_DEGREE),
                dmsCoord.getLatitude()
                        .getDegree());
        assertEquals(LAT_MINUTE,
                dmsCoord.getLatitude()
                        .getMinute());
        assertEquals(LAT_SECOND,
                dmsCoord.getLatitude()
                        .getSecond());
        assertEquals(LAT_DEGREE > 0,
                dmsCoord.getLatitude()
                        .getHemisphere()
                        .equals(Latitude.Hemisphere.N));
        assertEquals(Math.abs(LON_DEGREE),
                dmsCoord.getLongitude()
                        .getDegree());
        assertEquals(LON_MINUTE,
                dmsCoord.getLongitude()
                        .getMinute());
        assertEquals(LON_SECOND,
                dmsCoord.getLongitude()
                        .getSecond());
        assertEquals(LON_DEGREE > 0,
                dmsCoord.getLongitude()
                        .getHemisphere()
                        .equals(Longitude.Hemisphere.E));
    }

    /**
     * Tests to verify a MGRS Coordinate is successfully translated to UTM
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testMGRSCoodinateToUTM() throws GeoFormatException {
        MgrsCoordinate coord = new MgrsCoordinate(ZONE + BAND + COL_ID + ROW_ID + MGRS_X + MGRS_Y);
        UtmCoordinate utmCoord = MgrsConverter.convertToUTM(coord);

        assertEquals(Integer.parseInt(ZONE), utmCoord.getZone());
        assertEquals(BAND, utmCoord.getBand());
        assertEquals(Double.parseDouble(X_VALUE), utmCoord.getEasting());
        assertEquals(Double.parseDouble(Y_VALUE), utmCoord.getNorthing());
        assertEquals(HEMISPHERE.equals("N"), utmCoord.isNorthHemisphere());
    }

}
