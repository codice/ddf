/*
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

package ddf.catalog.transformer.csv.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeDescriptor;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;

public class CsvAttributeDescriptorComparatorTest {
  private CsvAttributeDescriptorComparator comparator;

  private AttributeDescriptor attribute1;

  private AttributeDescriptor attribute2;

  private AttributeDescriptor nonExistendAttribute1;

  private AttributeDescriptor nonExistendAttribute2;

  @Before
  public void setUp() {
    String[] columnNames = {"columnC", "columnB", "columnA", "columnD"};
    this.attribute1 = buildAttribute(columnNames[0]);
    this.attribute2 = buildAttribute(columnNames[2]);
    this.nonExistendAttribute1 = buildAttribute("columnE");
    this.nonExistendAttribute2 = buildAttribute("columnF");
    this.comparator = new CsvAttributeDescriptorComparator(Arrays.asList(columnNames));
  }

  @Test
  public void testDescriptorComparator() {
    int comparison = comparator.compare(attribute1, attribute2);
    assertThat(comparison, is(-1));

    comparison = comparator.compare(attribute2, attribute1);
    assertThat(comparison, is(1));

    comparison = comparator.compare(attribute1, attribute1);
    assertThat(comparison, is(0));

    comparison = comparator.compare(attribute1, nonExistendAttribute1);
    assertThat(comparison, is(-1));

    comparison = comparator.compare(nonExistendAttribute1, attribute1);
    assertThat(comparison, is(1));

    comparison = comparator.compare(nonExistendAttribute1, nonExistendAttribute2);
    assertThat(comparison, is(0));

    comparison = comparator.compare(nonExistendAttribute1, null);
    assertThat(comparison, is(1));

    comparison = comparator.compare(null, nonExistendAttribute2);
    assertThat(comparison, is(-1));

    comparison = comparator.compare(null, null);
    assertThat(comparison, is(0));
  }

  private AttributeDescriptor buildAttribute(String name) {
    AttributeDescriptor attribute = mock(AttributeDescriptor.class);
    when(attribute.getName()).thenReturn(name);
    return attribute;
  }
}
