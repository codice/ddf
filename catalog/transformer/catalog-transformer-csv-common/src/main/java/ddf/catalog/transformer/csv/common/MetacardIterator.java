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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.apache.commons.lang3.StringUtils;

/**
 * An implementation of java.util.Iterator which iterates over Metacard attribute values.
 *
 * @see java.util.Iterator
 */
class MetacardIterator implements Iterator<Serializable> {
  private static final String MULTIVALUE_DELIMITER = "\n";

  private final List<AttributeDescriptor> attributeDescriptorList;

  private final Metacard metacard;

  private int index;

  /**
   * @param metacard the metacard to be iterated over.
   * @param attributeDescriptorList the list of attributeDescriptors used to determine which
   *     metacard attributes to return.
   */
  MetacardIterator(
      final Metacard metacard, final List<AttributeDescriptor> attributeDescriptorList) {
    this.metacard = metacard;
    this.attributeDescriptorList = Collections.unmodifiableList(attributeDescriptorList);
    this.index = 0;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasNext() {
    return this.attributeDescriptorList.size() > index;
  }

  /** {@inheritDoc} */
  @Override
  public Serializable next() {
    if (!this.hasNext()) {
      throw new NoSuchElementException();
    }

    AttributeDescriptor attributeDescriptor = this.attributeDescriptorList.get(index);
    Attribute attribute = metacard.getAttribute(attributeDescriptor.getName());
    index++;

    if (attribute != null) {
      if (attributeDescriptor.isMultiValued()) {
        return StringUtils.join(attribute.getValues(), MULTIVALUE_DELIMITER);
      } else {
        return attribute.getValue();
      }
    } else if (isSourceId(attributeDescriptor) && isSourceIdSet()) {
      return metacard.getSourceId();
    } else if (isMetacardType(attributeDescriptor) && isMetacardTypeSet()) {
      return metacard.getMetacardType().getName();
    }

    return "";
  }

  private boolean isMetacardTypeSet() {
    return metacard.getMetacardType() != null && metacard.getMetacardType().getName() != null;
  }

  private boolean isMetacardType(AttributeDescriptor attributeDescriptor) {
    return MetacardType.METACARD_TYPE.equals(attributeDescriptor.getName());
  }

  private boolean isSourceIdSet() {
    return metacard.getSourceId() != null;
  }

  private boolean isSourceId(AttributeDescriptor attributeDescriptor) {
    return Core.SOURCE_ID.equals(attributeDescriptor.getName());
  }
}
