/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.impl.filter;

import static org.junit.Assert.*;

import org.junit.Ignore;
import org.junit.Test;

public class SpatialDistanceFilterTest {
    public SpatialDistanceFilter toTest;

    @Test
    @Ignore
    public void testCreateFromEmptyInput() {
        // should this never happen? What should be done in this case?
        toTest = new SpatialDistanceFilter("", "", "");
        assertEquals("POINT(0 0)", toTest.geometryWkt);
    }

    @Test
    @Ignore
    public void testCreateFromNonDigitCoordinates() {
        // should this never happen? What should be done in this case?
        toTest = new SpatialDistanceFilter("foo", "bar", "baz");
        assertEquals("POINT(0 0)", toTest.geometryWkt);
    }

    @Test
    public void testCreateFromNonNumberCoordinates() {
        toTest = new SpatialDistanceFilter(Double.NaN, Double.NaN, Double.NaN);
        assertEquals("POINT(NaN NaN)", toTest.geometryWkt);
    }

    @Test
    public void testCreateFromInfiniteCoordinates() {
        toTest = new SpatialDistanceFilter(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
                Double.MIN_VALUE);
        assertEquals("POINT(-Infinity Infinity)", toTest.geometryWkt);
    }

    // What is going on here?
    @Test
    @Ignore
    public void testCreateFromValidStringCoordinates() {
        toTest = new SpatialDistanceFilter("14", "91", "100");
        assertEquals("POINT(14 91)", toTest.geometryWkt); // comes back as expected:<POINT(14[ 91])>
                                                          // but was:<POINT(14[.0 91.0])>
        assertEquals(100, toTest.getDistanceInMeters(), 0.0001);
    }

}
