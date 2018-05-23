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

import ddf.catalog.data.AttributeDescriptor;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * An implementation of java.util.Iterator that iterates over the supplied list of
 * AttributeDescriptors returning either the attribute name or (if provided) an alias for the
 * attribute.
 *
 * @see java.util.Iterator
 */
class ColumnHeaderIterator implements Iterator<String> {
  private Map<String, String> columnAliasMap;

  private List<AttributeDescriptor> attributeDescriptorList;

  private int index;

  /**
   * @param attributeDescriptorList an ordered list of the AttributeDescriptors to iterate over
   * @param columnAliasMap a map of Strings from attribute name to column name (alias).
   */
  ColumnHeaderIterator(
      final List<AttributeDescriptor> attributeDescriptorList,
      final Map<String, String> columnAliasMap) {
    this.attributeDescriptorList = Collections.unmodifiableList(attributeDescriptorList);
    this.columnAliasMap = Collections.unmodifiableMap(columnAliasMap);
    index = 0;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasNext() {
    return this.attributeDescriptorList.size() > index;
  }

  /** {@inheritDoc} */
  @Override
  public String next() {
    if (!this.hasNext()) {
      throw new NoSuchElementException();
    }

    AttributeDescriptor attributeDescriptor = attributeDescriptorList.get(index);
    String attributeDescriptorName = attributeDescriptor.getName();
    String columnAlias =
        Optional.ofNullable(columnAliasMap.get(attributeDescriptorName))
            .orElse(attributeDescriptorName);

    index++;
    return columnAlias;
  }
}
