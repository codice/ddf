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
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.lang3.builder.CompareToBuilder;

/**
 * An implementation of java.util.Comparator that sorts AttributeDescriptors according to the
 * provided list of attribute names. This allows us to put the output csv columns in any order as
 * specified by the user.
 *
 * @see java.util.Comparator
 */
class CsvAttributeDescriptorComparator implements Comparator<AttributeDescriptor>, Serializable {
  private static final int EQUAL = 0;

  private static final int LESSER = -1;

  private static final int GREATER = 1;

  private List<String> attributeOrder;

  /** @param attributeOrder the order in which the attributes should be output. */
  CsvAttributeDescriptorComparator(List<String> attributeOrder) {
    this.attributeOrder = Collections.unmodifiableList(attributeOrder);
  }

  /** {@inheritDoc} */
  @Override
  public int compare(AttributeDescriptor descriptor1, AttributeDescriptor descriptor2) {
    if (descriptor1 == null && descriptor2 == null) {
      return EQUAL;
    }

    if (descriptor1 == null) {
      return LESSER;
    }

    if (descriptor2 == null) {
      return GREATER;
    }

    String descriptorName1 = descriptor1.getName();
    String descriptorName2 = descriptor2.getName();

    return new CompareToBuilder()
        .append(getAttributeIndex(descriptorName1), getAttributeIndex(descriptorName2))
        .toComparison();
  }

  /**
   * @param attributeName the name of the attribute that we need to find the index of.
   * @return the index of the attribute in the 'attributeOrder' list whose name is 'attributeName'
   */
  private int getAttributeIndex(String attributeName) {
    int descriptorIndex = attributeOrder.indexOf(attributeName);

    if (descriptorIndex == -1) {
      return attributeOrder.size();
    }

    return descriptorIndex;
  }
}
