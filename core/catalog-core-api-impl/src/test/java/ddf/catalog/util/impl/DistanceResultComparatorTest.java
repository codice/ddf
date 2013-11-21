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
package ddf.catalog.util;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.util.impl.DistanceResultComparator;

public class DistanceResultComparatorTest {
    private ResultImpl shortDistance;

    private ResultImpl longDistance;

    private ResultImpl negativeDistance;

    private ResultImpl nullDistance;

    private ResultImpl zeroDistance;

    private Result nullResult;

    @Before
    public void setUp() throws Exception {
        shortDistance = new ResultImpl();
        shortDistance.setDistanceInMeters(new Double(Short.MAX_VALUE));
        longDistance = new ResultImpl();
        longDistance.setDistanceInMeters(new Double(Long.MAX_VALUE));
        negativeDistance = new ResultImpl();
        negativeDistance.setDistanceInMeters(new Double(Short.MIN_VALUE));
        nullDistance = new ResultImpl();
        nullDistance.setDistanceInMeters(null);
        zeroDistance = new ResultImpl();
        zeroDistance.setDistanceInMeters(0.0);
        nullResult = null;

    }

    @Test
    public void testCompareAscending() {
        DistanceResultComparator comparer = new DistanceResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(shortDistance, longDistance));
        assertEquals(0, comparer.compare(shortDistance, shortDistance));
        assertEquals(1, comparer.compare(longDistance, shortDistance));

    }

    @Test
    public void testCompareDescending() {
        DistanceResultComparator comparer = new DistanceResultComparator(SortOrder.DESCENDING);
        assertEquals(-1, comparer.compare(longDistance, shortDistance));
        assertEquals(0, comparer.compare(shortDistance, shortDistance));
        assertEquals(1, comparer.compare(shortDistance, longDistance));
    }

    @Test
    public void testCompareNullSortOrder() {
        DistanceResultComparator comparer = new DistanceResultComparator(null);
        assertEquals(0, comparer.compare(shortDistance, longDistance));
        assertEquals(0, comparer.compare(longDistance, shortDistance));
    }

    @Test
    @Ignore
    public void testNullResults() {
        DistanceResultComparator comparer = new DistanceResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(shortDistance, nullResult));
        assertEquals(0, comparer.compare(nullResult, nullResult));
        assertEquals(1, comparer.compare(nullResult, shortDistance));
    }

    @Test
    @Ignore
    public void testNullDistance() {
        DistanceResultComparator comparer = new DistanceResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(shortDistance, nullDistance));
        assertEquals(0, comparer.compare(nullDistance, nullDistance));
        assertEquals(0, comparer.compare(nullDistance, nullResult));
        assertEquals(0, comparer.compare(nullResult, nullDistance));
        assertEquals(1, comparer.compare(nullDistance, shortDistance));
    }

    @Test
    @Ignore
    public void testNegativeDistance() {
        DistanceResultComparator comparer = new DistanceResultComparator(SortOrder.ASCENDING);
        assertEquals(1, comparer.compare(negativeDistance, shortDistance));
        assertEquals(-1, comparer.compare(negativeDistance, nullDistance));
        assertEquals(-1, comparer.compare(negativeDistance, nullResult));
        assertEquals(1, comparer.compare(negativeDistance, zeroDistance));
        assertEquals(0, comparer.compare(negativeDistance, negativeDistance));
        assertEquals(-1, comparer.compare(shortDistance, negativeDistance));
        assertEquals(1, comparer.compare(nullDistance, negativeDistance));
        assertEquals(1, comparer.compare(nullResult, negativeDistance));
        assertEquals(-1, comparer.compare(zeroDistance, negativeDistance));
    }

    @Test
    @Ignore
    public void testZeroDistance() {
        DistanceResultComparator comparer = new DistanceResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(zeroDistance, shortDistance));
        assertEquals(-1, comparer.compare(zeroDistance, nullDistance));
        assertEquals(-1, comparer.compare(zeroDistance, nullResult));
        assertEquals(-1, comparer.compare(zeroDistance, negativeDistance));
        assertEquals(0, comparer.compare(zeroDistance, zeroDistance));
        assertEquals(1, comparer.compare(shortDistance, zeroDistance));
        assertEquals(1, comparer.compare(nullDistance, zeroDistance));
        assertEquals(1, comparer.compare(nullResult, zeroDistance));
        assertEquals(-1, comparer.compare(negativeDistance, zeroDistance));
    }

}
