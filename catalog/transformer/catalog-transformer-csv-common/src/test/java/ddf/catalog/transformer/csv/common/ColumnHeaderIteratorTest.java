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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.Before;
import org.junit.Test;

public class ColumnHeaderIteratorTest {
  private static final String[][] TEST_DATA = {
    {"attribute1", "column1"}, {"attribute2", "column2"}, {"attribute3"}
  };

  private List<AttributeDescriptor> attributeDescriptorList;

  private Map<String, String> aliasMap;

  @Before
  public void setUp() {
    this.attributeDescriptorList = new ArrayList<AttributeDescriptor>();
    this.aliasMap = new HashMap<>();

    for (String[] data : TEST_DATA) {
      AttributeDescriptor descriptor = buildAttribute(data[0]);
      attributeDescriptorList.add(descriptor);

      if (data.length == 2) {
        aliasMap.put(data[0], data[1]);
      }
    }
  }

  @Test
  public void testColumnHeaderIterator() {
    Iterator<String> iterator = new ColumnHeaderIterator(attributeDescriptorList, aliasMap);

    for (int i = 0; i < TEST_DATA.length - 1; i++) {
      assertThat(iterator.hasNext(), is(true));
      assertThat(iterator.next(), is(TEST_DATA[i][1]));
    }

    assertThat(iterator.hasNext(), is(true));
    assertThat(iterator.next(), is(TEST_DATA[2][0]));
    assertThat(iterator.hasNext(), is(false));
  }

  @Test(expected = NoSuchElementException.class)
  public void testHasNext() {
    Iterator<String> iterator = new ColumnHeaderIterator(attributeDescriptorList, aliasMap);

    while (iterator.hasNext()) {
      iterator.next();
    }

    iterator.next();
  }

  private AttributeDescriptor buildAttribute(String name) {
    AttributeDescriptor attribute = mock(AttributeDescriptor.class);
    when(attribute.getName()).thenReturn(name);
    return attribute;
  }
}
