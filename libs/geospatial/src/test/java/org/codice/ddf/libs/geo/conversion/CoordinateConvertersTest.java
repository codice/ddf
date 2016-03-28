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

import java.text.DecimalFormat;

import org.codice.ddf.libs.geo.GeoFormatException;
import org.codice.ddf.libs.geo.coordinate.DecimalDegreesCoordinate;
import org.codice.ddf.libs.geo.coordinate.DmsCoordinate;
import org.codice.ddf.libs.geo.coordinate.MgrsCoordinate;
import org.codice.ddf.libs.geo.coordinate.UtmCoordinate;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Tests the different coordinate converters
 */
public class CoordinateConvertersTest extends Assert {

    private static double roundWholeNum(double num) {
        DecimalFormat df = new DecimalFormat("#");
        return Double.valueOf(df.format(num));
    }

    private static double round(double num) {
        DecimalFormat df = new DecimalFormat("#.#####");
        return Double.valueOf(df.format(num));

    }

    private static double round3(double num) {
        DecimalFormat df = new DecimalFormat("#.###");
        return Double.valueOf(df.format(num));

    }

    /**
     * Tests the Decimal Degrees Coordinate converter by converting a random lat/lon
     * into the other formats (UTM, MGRS, DMS) then converting back to Decimal Degrees
     * and verify the values are the same.
     * <p>
     * Note: Validates to 3 decimal points for lat/lons that were converted from UTM and MGRS.
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testDecimalDegreesConversions() throws GeoFormatException {
        double lat = round(0.123 * 160.0 - 80);
        double lon = round(0.123 * 360.0 - 180);
        DecimalDegreesCoordinate ddCoord1 = new DecimalDegreesCoordinate(lat, lon);

        DmsCoordinate dmsCoord = DecimalDegreesConverter.convertToDMS(ddCoord1);
        DecimalDegreesCoordinate ddCoord2 = DmsConverter.convertToDecimalDegrees(dmsCoord);
        assertEquals(ddCoord1.getLatitiudeDegrees(), ddCoord2.getLatitiudeDegrees());
        assertEquals(ddCoord1.getLongitudeDegrees(), ddCoord2.getLongitudeDegrees());

        UtmCoordinate utmCoord = DecimalDegreesConverter.convertToUTM(ddCoord1);
        ddCoord2 = UtmConverter.convertToDecimalDegrees(utmCoord);
        assertEquals(round3(ddCoord1.getLatitiudeDegrees()),
                round3(ddCoord2.getLatitiudeDegrees()));
        assertEquals(round3(ddCoord1.getLongitudeDegrees()),
                round3(ddCoord2.getLongitudeDegrees()));

        // MGRS Conversion is not precise so don't handle decimals
        MgrsCoordinate mgrsCoord = DecimalDegreesConverter.convertToMGRS(ddCoord1);
        ddCoord2 = MgrsConverter.convertToDecimalDegrees(mgrsCoord);
        assertEquals(round3(ddCoord1.getLatitiudeDegrees()),
                round3(ddCoord2.getLatitiudeDegrees()));
        assertEquals(round3(ddCoord1.getLongitudeDegrees()),
                round3(ddCoord2.getLongitudeDegrees()));
    }

    /**
     * Tests the UTM Coordinate converter by converting a UTM String
     * into the other formats (DEcimal Degrees, MGRS, DMS) then converting back to UTM
     * and verify the values are the same.
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testUTMConversions() throws GeoFormatException {
        String utmCoordString = "38N04459563686195";
        UtmCoordinate utmCoord = new UtmCoordinate(utmCoordString);

        DecimalDegreesCoordinate ddCoord = UtmConverter.convertToDecimalDegrees(utmCoord);
        UtmCoordinate utmCoord2 = DecimalDegreesConverter.convertToUTM(ddCoord);
        assertEquals(utmCoord.getBand(), utmCoord2.getBand());
        assertEquals(roundWholeNum(utmCoord.getEasting()), roundWholeNum(utmCoord2.getEasting()));
        assertEquals(roundWholeNum(utmCoord.getNorthing()), roundWholeNum(utmCoord2.getNorthing()));
        assertEquals(utmCoord.getZone(), utmCoord2.getZone());
        assertEquals(utmCoord.isNorthHemisphere(), utmCoord2.isNorthHemisphere());

        DmsCoordinate dmsCoord = UtmConverter.convertToDMS(utmCoord);
        utmCoord2 = DmsConverter.convertToUTM(dmsCoord);
        assertEquals(utmCoord.getBand(), utmCoord2.getBand());
        assertEquals(roundWholeNum(utmCoord.getEasting()), roundWholeNum(utmCoord2.getEasting()));
        assertEquals(roundWholeNum(utmCoord.getNorthing()), roundWholeNum(utmCoord2.getNorthing()));
        assertEquals(utmCoord.getZone(), utmCoord2.getZone());
        assertEquals(utmCoord.isNorthHemisphere(), utmCoord2.isNorthHemisphere());

        // MGRS Conversion is not precise so don't handle decimals
        MgrsCoordinate mmgrsCoord = UtmConverter.convertToMGRS(utmCoord);
        utmCoord2 = MgrsConverter.convertToUTM(mmgrsCoord);
        assertEquals(utmCoord.getBand(), utmCoord2.getBand());
        assertEquals(roundWholeNum(utmCoord.getEasting()), roundWholeNum(utmCoord2.getEasting()));
        assertEquals(roundWholeNum(utmCoord.getNorthing()), roundWholeNum(utmCoord2.getNorthing()));
        assertEquals(utmCoord.getZone(), utmCoord2.getZone());
        assertEquals(utmCoord.isNorthHemisphere(), utmCoord2.isNorthHemisphere());
    }

    /**
     * Tests the MGRS Coordinate converter by converting a MGRS String
     * into the other formats (Decimal Degrees, UTM, DMS) then converting back to MGRS
     * and verify the values are the same.
     *
     * @throws GeoFormatException if the test fails
     */
    @Test
    public void testMGRSConversions() throws GeoFormatException {
        String mgrsCoordString = "38SMB4595686195";
        MgrsCoordinate mgrsCoord = new MgrsCoordinate(mgrsCoordString);

        DecimalDegreesCoordinate ddCoord = MgrsConverter.convertToDecimalDegrees(mgrsCoord);
        MgrsCoordinate mgrsCoord2 = DecimalDegreesConverter.convertToMGRS(ddCoord);
        assertEquals(mgrsCoord.getBand(), mgrsCoord2.getBand());
        //Conversions are somewhat inprecise so meters could be off by one
        assertTrue(roundWholeNum(mgrsCoord.getX()) >= roundWholeNum(mgrsCoord2.getX()) - 1);
        assertTrue(roundWholeNum(mgrsCoord.getX()) <= roundWholeNum(mgrsCoord2.getX()) + 1);
        assertEquals(roundWholeNum(mgrsCoord.getY()), roundWholeNum(mgrsCoord2.getY()));
        assertEquals(mgrsCoord.getZone(), mgrsCoord2.getZone());
        assertEquals(mgrsCoord.getGridColumnID(), mgrsCoord2.getGridColumnID());
        assertEquals(mgrsCoord.getGridRowID(), mgrsCoord2.getGridRowID());

        DmsCoordinate dmsCoord = MgrsConverter.convertToDMS(mgrsCoord);
        mgrsCoord2 = DmsConverter.convertToMGRS(dmsCoord);
        assertEquals(mgrsCoord.getBand(), mgrsCoord2.getBand());
        //Conversions are somewhat inprecise so meters could be off by one
        assertTrue(roundWholeNum(mgrsCoord.getX()) >= roundWholeNum(mgrsCoord2.getX()) - 1);
        assertTrue(roundWholeNum(mgrsCoord.getX()) <= roundWholeNum(mgrsCoord2.getX()) + 1);
        assertEquals(roundWholeNum(mgrsCoord.getY()), roundWholeNum(mgrsCoord2.getY()));
        assertEquals(mgrsCoord.getZone(), mgrsCoord2.getZone());
        assertEquals(mgrsCoord.getGridColumnID(), mgrsCoord2.getGridColumnID());
        assertEquals(mgrsCoord.getGridRowID(), mgrsCoord2.getGridRowID());

        // MGRS Conversion is not precise so don't handle decimals
        UtmCoordinate utmCoord = MgrsConverter.convertToUTM(mgrsCoord);
        mgrsCoord2 = UtmConverter.convertToMGRS(utmCoord);
        assertEquals(mgrsCoord.getBand(), mgrsCoord2.getBand());
        assertEquals(roundWholeNum(mgrsCoord.getX()), roundWholeNum(mgrsCoord2.getX()));
        assertEquals(roundWholeNum(mgrsCoord.getY()), roundWholeNum(mgrsCoord2.getY()));
        assertEquals(mgrsCoord.getZone(), mgrsCoord2.getZone());
        assertEquals(mgrsCoord.getGridColumnID(), mgrsCoord2.getGridColumnID());
        assertEquals(mgrsCoord.getGridRowID(), mgrsCoord2.getGridRowID());
    }

}
