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
import org.junit.Test;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.ResultImpl;

public class TemporalResultComparatorTest {

    MetacardImpl firstMc;

    MetacardImpl secondMc;

    MetacardImpl nullDateMc;

    @Before
    public void setUp() throws Exception {
        Calendar c = Calendar.getInstance();
        firstMc = new MetacardImpl();
        secondMc = new MetacardImpl();
        nullDateMc = new MetacardImpl();

        firstMc.setEffectiveDate(c.getTime());
        c.add(Calendar.DAY_OF_YEAR, 1);
        secondMc.setEffectiveDate(c.getTime());

    }

    @Test
    public void testCompareAscending() {
        TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
        assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
        assertEquals(1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
    }

    @Test
    public void testCompareDescending() {
        TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.DESCENDING);
        assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
        assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
        assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
    }

    @Test
    public void testCompareNullSortOrder() {
        TemporalResultComparator comparer = new TemporalResultComparator(null);
        assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
        assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
        assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
    }

    @Test
    public void testNullResults() {
        TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(new ResultImpl(firstMc), null));
        assertEquals(0, comparer.compare(null, null));
        assertEquals(1, comparer.compare(null, new ResultImpl(firstMc)));
    }

    @Test
    public void testNullMetacards() {
        TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(new ResultImpl(firstMc), new ResultImpl()));
        assertEquals(0, comparer.compare(new ResultImpl(), new ResultImpl()));
        assertEquals(1, comparer.compare(new ResultImpl(), new ResultImpl(firstMc)));
    }

    @Test
    public void testNullDateMetacards() {
        TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(nullDateMc)));
        assertEquals(0, comparer.compare(new ResultImpl(nullDateMc), new ResultImpl(nullDateMc)));
        assertEquals(1, comparer.compare(new ResultImpl(nullDateMc), new ResultImpl(firstMc)));
    }
}
