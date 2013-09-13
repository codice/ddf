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

import ddf.catalog.data.MetacardImpl;
import ddf.catalog.data.Result;
import ddf.catalog.data.ResultImpl;

public class RelevanceResultComparatorTest {
    private ResultImpl lowRelevance;

    private ResultImpl highRelevance;

    private ResultImpl negativeRelevance;

    private ResultImpl nullRelevance;

    private ResultImpl zeroRelevance;

    private Result nullResult;

    @Before
    public void setUp() throws Exception {
        lowRelevance = new ResultImpl();
        lowRelevance.setRelevanceScore(new Double(Short.MAX_VALUE));
        highRelevance = new ResultImpl();
        highRelevance.setRelevanceScore(new Double(Long.MAX_VALUE));
        negativeRelevance = new ResultImpl();
        negativeRelevance.setRelevanceScore(new Double(Short.MIN_VALUE));
        nullRelevance = new ResultImpl();
        nullRelevance.setRelevanceScore(null);
        zeroRelevance = new ResultImpl();
        zeroRelevance.setRelevanceScore(0.0);
        nullResult = null;
    }

    @Test
    public void testCompareAscending() {
        RelevanceResultComparator comparer = new RelevanceResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(lowRelevance, highRelevance));
        assertEquals(0, comparer.compare(lowRelevance, lowRelevance));
        assertEquals(1, comparer.compare(highRelevance, lowRelevance));

    }

    @Test
    public void testCompareDescending() {
        RelevanceResultComparator comparer = new RelevanceResultComparator(SortOrder.DESCENDING);
        assertEquals(-1, comparer.compare(highRelevance, lowRelevance));
        assertEquals(0, comparer.compare(lowRelevance, lowRelevance));
        assertEquals(1, comparer.compare(lowRelevance, highRelevance));
    }

    @Test
    public void testCompareNullSortOrder() {
        RelevanceResultComparator comparer = new RelevanceResultComparator(null);
        assertEquals(0, comparer.compare(lowRelevance, highRelevance));
        assertEquals(0, comparer.compare(highRelevance, lowRelevance));
    }

    @Test
    @Ignore
    public void testNullResults() {
        RelevanceResultComparator comparer = new RelevanceResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(lowRelevance, nullResult));
        assertEquals(0, comparer.compare(nullResult, nullResult));
        assertEquals(1, comparer.compare(nullResult, lowRelevance));
    }

    @Test
    @Ignore
    public void testNullRelevance() {
        RelevanceResultComparator comparer = new RelevanceResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(lowRelevance, nullRelevance));
        assertEquals(0, comparer.compare(nullRelevance, nullRelevance));
        assertEquals(0, comparer.compare(nullRelevance, nullResult));
        assertEquals(0, comparer.compare(nullResult, nullRelevance));
        assertEquals(1, comparer.compare(nullRelevance, lowRelevance));
    }

    @Test
    @Ignore
    public void testNegativeRelevance() {
        RelevanceResultComparator comparer = new RelevanceResultComparator(SortOrder.ASCENDING);
        assertEquals(1, comparer.compare(negativeRelevance, lowRelevance));
        assertEquals(-1, comparer.compare(negativeRelevance, nullRelevance));
        assertEquals(-1, comparer.compare(negativeRelevance, nullResult));
        assertEquals(1, comparer.compare(negativeRelevance, zeroRelevance));
        assertEquals(0, comparer.compare(negativeRelevance, negativeRelevance));
        assertEquals(-1, comparer.compare(lowRelevance, negativeRelevance));
        assertEquals(1, comparer.compare(nullRelevance, negativeRelevance));
        assertEquals(1, comparer.compare(nullResult, negativeRelevance));
        assertEquals(-1, comparer.compare(zeroRelevance, negativeRelevance));
    }

    @Test
    @Ignore
    public void testZeroRelevance() {
        RelevanceResultComparator comparer = new RelevanceResultComparator(SortOrder.ASCENDING);
        assertEquals(-1, comparer.compare(zeroRelevance, lowRelevance));
        assertEquals(-1, comparer.compare(zeroRelevance, nullRelevance));
        assertEquals(-1, comparer.compare(zeroRelevance, nullResult));
        assertEquals(-1, comparer.compare(zeroRelevance, negativeRelevance));
        assertEquals(0, comparer.compare(zeroRelevance, zeroRelevance));
        assertEquals(1, comparer.compare(lowRelevance, zeroRelevance));
        assertEquals(1, comparer.compare(nullRelevance, zeroRelevance));
        assertEquals(1, comparer.compare(nullResult, zeroRelevance));
        assertEquals(-1, comparer.compare(negativeRelevance, zeroRelevance));
    }

}
