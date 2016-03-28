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
package org.codice.ddf.libs.geo;

import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

public class GeoUtilTest extends Assert {

    @Test
    public void createBBoxFromPolygonTest() {
        String[] polyAry = new String[5 * 2];

        polyAry[0] = "2";
        polyAry[1] = "1";
        polyAry[2] = "3";
        polyAry[3] = "2";
        polyAry[4] = "4";
        polyAry[5] = "1";
        polyAry[6] = "3";
        polyAry[7] = "0";
        polyAry[8] = "2";
        polyAry[9] = "1";

        double[] limits = GeospatialUtil.createBBoxFromPolygon(polyAry);

        assertEquals(limits[0], 2.0);
        assertEquals(limits[1], 0.0);
        assertEquals(limits[2], 4.0);
        assertEquals(limits[3], 2.0);

    }

    @Test
    @Ignore("COpenlayers indicates that wkt can't cross international line")
    public void createBBoxFromPolygonDateLineTest() {
        String[] polyAry = new String[5 * 2];

        // Goes over the international date line (ie the furthest east are lower
        // negative numbers
        // while the furthest west are small positive numbers).

        // TODO: incorrect coordinate, if wrap clockwise, it is not crossing
        // date line
        polyAry[0] = "-164.18";
        polyAry[1] = "35.46";
        polyAry[2] = "165.23";
        polyAry[3] = "23.88";
        polyAry[4] = "172.97";
        polyAry[5] = "28.3";
        polyAry[6] = "-174.37";
        polyAry[7] = "21.62";
        polyAry[8] = "-164.18";
        polyAry[9] = "35.46";

        double[] limits = GeospatialUtil.createBBoxFromPolygon(polyAry);

        assertEquals(165.23, limits[0]);
        assertEquals(21.62, limits[1]);
        assertEquals(-164.18, limits[2]);
        assertEquals(35.46, limits[3]);
    }

    @Test
    public void createBBoxFromPolygonWrapTest() {
        String[] polyAry = new String[5 * 2];

        polyAry[0] = "-164.18";
        polyAry[1] = "35.46";
        polyAry[2] = "165.23";
        polyAry[3] = "23.88";
        polyAry[4] = "172.97";
        polyAry[5] = "28.3";
        polyAry[6] = "-174.37";
        polyAry[7] = "21.62";
        polyAry[8] = "-164.18";
        polyAry[9] = "35.46";

        double[] limits = GeospatialUtil.createBBoxFromPolygon(polyAry);

        assertEquals(-174.37, limits[0]);
        assertEquals(21.62, limits[1]);
        assertEquals(172.97, limits[2]);
        assertEquals(35.46, limits[3]);
    }

    @Test
    public void createBBoxFromPolygonEquatorTest() {
        String[] polyAry = new String[5 * 2];

        polyAry[0] = "-69.61";
        polyAry[1] = "3.86";
        polyAry[2] = "-68.03";
        polyAry[3] = "-8.41";
        polyAry[4] = "-83.67";
        polyAry[5] = "-8.06";
        polyAry[6] = "-83.14";
        polyAry[7] = "5.79";
        polyAry[8] = "-69.61";
        polyAry[9] = "3.86";

        double[] limits = GeospatialUtil.createBBoxFromPolygon(polyAry);

        assertEquals(-83.67, limits[0]);
        assertEquals(-8.41, limits[1]);
        assertEquals(-68.03, limits[2]);
        assertEquals(5.79, limits[3]);
    }

    @Test
    // box on north pole
    public void createBBoxFromPolygonPoleTest() {
        String[] polyAry = new String[5 * 2];

        polyAry[0] = "162.42";
        polyAry[1] = "79.69";
        polyAry[2] = "41.48";
        polyAry[3] = "75.50";
        polyAry[4] = "-46.41";
        polyAry[5] = "84.20";
        polyAry[6] = "-164.63";
        polyAry[7] = "80.53";
        polyAry[8] = "162.42";
        polyAry[9] = "79.69";

        double[] limits = GeospatialUtil.createBBoxFromPolygon(polyAry);

        assertEquals(-164.63, limits[0]);
        assertEquals(75.50, limits[1]);
        assertEquals(162.42, limits[2]);
        assertEquals(84.20, limits[3]);
    }

}
