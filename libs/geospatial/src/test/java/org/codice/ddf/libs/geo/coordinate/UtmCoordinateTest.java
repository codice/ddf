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
import org.codice.ddf.libs.geo.conversion.UtmConverter;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests the UTM Coordinate and the conversions into the other coordinate formats
 */
public class UtmCoordinateTest extends Assert {

    private static final String X_VALUE = "0445956";

    private static final String Y_VALUE = "3686195";

    private static final String HEMISPHERE = "N";

    private static final String ZONE = "38";

    private static final char BAND = 'S';

    private static final char COL_ID = 'M';

    private static final char ROW_ID = 'B';

    private static final double MGRS_X = 45956;

    private static final double MGRS_Y = 86195;

    private static final double LAT_VALUE = 33.31352;

    private static final double LON_VALUE = 44.41941;

    private static final int LAT_DEGREE = 33;

    private static final int LAT_MINUTE = 18;

    private static final int LAT_SECOND = 48;

    private static final int LON_DEGREE = 44;

    private static final int LON_MINUTE = 25;

    private static final int LON_SECOND = 9;

    private static double round(double num) {
        DecimalFormat df = new DecimalFormat("#.#####");
        return Double.valueOf(df.format(num));
    }

    /**
     * Tests to see if the UTM Constructor parses the MGRS String correctly
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testUTMCoodinate() throws GeoFormatException {
        UtmCoordinate coord = new UtmCoordinate(ZONE + HEMISPHERE + X_VALUE + Y_VALUE);
        assertEquals(Double.parseDouble(X_VALUE), coord.getEasting());
        assertEquals(Double.parseDouble(Y_VALUE), coord.getNorthing());
        assertEquals(HEMISPHERE.charAt(0), coord.isNorthHemisphere() ? 'N' : 'S');
        assertEquals(Integer.parseInt(ZONE), coord.getZone());
    }

    /**
     * Tests to verify a UTM Coordinate is successfully translated to Decimal Degrees
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testUTMCoodinateToDecimalDegrees() throws GeoFormatException {
        UtmCoordinate coord = new UtmCoordinate(ZONE + HEMISPHERE + X_VALUE + Y_VALUE);
        DecimalDegreesCoordinate ddCoord = UtmConverter.convertToDecimalDegrees(coord);
        assertEquals(LAT_VALUE, round(ddCoord.getLatitiudeDegrees()));
        assertEquals(LON_VALUE, round(ddCoord.getLongitudeDegrees()));

    }

    /**
     * Tests to verify a UTM Coordinate is successfully translated to DMS
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testUTMCoodinateToDMS() throws GeoFormatException {
        UtmCoordinate coord = new UtmCoordinate(ZONE + HEMISPHERE + X_VALUE + Y_VALUE);
        DmsCoordinate dmsCoord = UtmConverter.convertToDMS(coord);
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
     * Tests to verify a UTM Coordinate is successfully translated to MGRS
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testUTMCoodinateToMGRS() throws GeoFormatException {
        UtmCoordinate coord = new UtmCoordinate(ZONE + HEMISPHERE + X_VALUE + Y_VALUE);
        MgrsCoordinate mgrsCoord = UtmConverter.convertToMGRS(coord);

        assertEquals(Integer.parseInt(ZONE), mgrsCoord.getZone());
        assertEquals(BAND, mgrsCoord.getBand());
        assertEquals(COL_ID, mgrsCoord.getGridColumnID());
        assertEquals(ROW_ID, mgrsCoord.getGridRowID());
        assertEquals(MGRS_X, mgrsCoord.getX());
        assertEquals(MGRS_Y, mgrsCoord.getY());
    }

}
