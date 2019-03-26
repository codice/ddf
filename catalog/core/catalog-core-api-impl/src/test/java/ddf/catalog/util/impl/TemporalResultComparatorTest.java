/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl;

import static org.junit.Assert.assertEquals;

import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import java.util.Calendar;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.sort.SortOrder;

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
    firstMc.setCreatedDate(c.getTime());
    firstMc.setModifiedDate(c.getTime());
    firstMc.setExpirationDate(c.getTime());
    c.add(Calendar.DAY_OF_YEAR, 1);
    secondMc.setEffectiveDate(c.getTime());
    secondMc.setCreatedDate(c.getTime());
    secondMc.setModifiedDate(c.getTime());
    secondMc.setExpirationDate(c.getTime());
  }

  @Test
  public void testCompareAscendingEffective() {
    TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.ASCENDING);
    assertEquals(-1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
  }

  @Test
  public void testCompareDescendingEffective() {
    TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.DESCENDING);
    assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
  }

  @Test
  public void testCompareNullSortOrderEffective() {
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
  public void testNullDateMetacardsEffective() {
    TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.ASCENDING);
    assertEquals(-1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(nullDateMc)));
    assertEquals(0, comparer.compare(new ResultImpl(nullDateMc), new ResultImpl(nullDateMc)));
    assertEquals(1, comparer.compare(new ResultImpl(nullDateMc), new ResultImpl(firstMc)));
  }

  @Test
  public void testCompareAscendingModified() {
    TemporalResultComparator comparer =
        new TemporalResultComparator(SortOrder.ASCENDING, Core.MODIFIED);
    assertEquals(-1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
  }

  @Test
  public void testCompareDescendingModified() {
    TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.DESCENDING);
    assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
  }

  @Test
  public void testCompareNullSortOrderModified() {
    TemporalResultComparator comparer = new TemporalResultComparator(null);
    assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
  }

  @Test
  public void testCompareAscendingCreated() {
    TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.ASCENDING);
    assertEquals(-1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
  }

  @Test
  public void testCompareDescendingCreated() {
    TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.DESCENDING);
    assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
  }

  @Test
  public void testCompareNullSortOrderCreated() {
    TemporalResultComparator comparer = new TemporalResultComparator(null);
    assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
  }

  @Test
  public void testCompareAscendingExpiration() {
    TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.ASCENDING);
    assertEquals(-1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
  }

  @Test
  public void testCompareDescendingExpiration() {
    TemporalResultComparator comparer = new TemporalResultComparator(SortOrder.DESCENDING);
    assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
  }

  @Test
  public void testCompareNullSortOrderExpiration() {
    TemporalResultComparator comparer = new TemporalResultComparator(null);
    assertEquals(1, comparer.compare(new ResultImpl(firstMc), new ResultImpl(secondMc)));
    assertEquals(0, comparer.compare(new ResultImpl(firstMc), new ResultImpl(firstMc)));
    assertEquals(-1, comparer.compare(new ResultImpl(secondMc), new ResultImpl(firstMc)));
  }
}
