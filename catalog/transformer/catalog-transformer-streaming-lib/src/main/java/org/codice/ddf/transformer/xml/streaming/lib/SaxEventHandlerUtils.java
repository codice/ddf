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
package org.codice.ddf.transformer.xml.streaming.lib;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.types.ValidationAttributes;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;

public class SaxEventHandlerUtils {

  /**
   * This method iterates through the attribute list provided and combines the values of common
   * attributes. The Attribute Descriptors are used to determine if the Attribute is permitted to
   * have multiple values.
   *
   * @param descriptors A set of attribute descriptors. Used to determine if the attribute can have
   *     multiple values. If empty or null, a validation warning will be added to the attributes and
   *     the attribute will default to not allow multiple values.
   * @param attributes The list of attributes to combine.
   * @return The list of attributes with multiple attribute values combined to a list on a single
   *     attribute. Returns null or an empty list if the attribute list provided was null or empty.
   */
  public List<Attribute> getCombinedMultiValuedAttributes(
      Set<AttributeDescriptor> descriptors, List<Attribute> attributes) {
    if (CollectionUtils.isEmpty(attributes)) {
      return attributes;
    }

    Map<String, Boolean> multiValuedMap = getMultiValuedNameMap(descriptors);

    List<String> validationWarnings = new ArrayList<>();
    Map<String, Attribute> attributeMap = new HashMap<>();
    for (Attribute attribute : attributes) {
      String attributeName = attribute.getName();
      Attribute addedAttribute = attributeMap.putIfAbsent(attributeName, attribute);

      if (addedAttribute != null) {
        Boolean addValue = multiValuedMap.get(attributeName);
        if (addValue == null) {
          validationWarnings.add(
              String.format(
                  "No attribute descriptor was found for attribute: '%s'. Handling the attribute as non-multi-valued.",
                  attributeName));
          addValue = false;
        }

        if (addValue) {
          attributeMap.get(attributeName).getValues().addAll(attribute.getValues());
        } else {
          validationWarnings.add(
              String.format(
                  "Multiple values found for a non-multi-valued attribute. Attribute: '%s', Existing value: '%s', New value: '%s'. Existing value will be kept, new value will be ignored.",
                  attributeName, attributeMap.get(attributeName).getValue(), attribute.getValue()));
        }
      }
    }

    if (!validationWarnings.isEmpty()) {
      Attribute validationAttribute = attributeMap.get(ValidationAttributes.VALIDATION_WARNINGS);
      if (validationAttribute != null) {
        attributeMap
            .get(ValidationAttributes.VALIDATION_WARNINGS)
            .getValues()
            .addAll(validationWarnings);
      } else {
        attributeMap.put(
            ValidationAttributes.VALIDATION_WARNINGS,
            new AttributeImpl(
                ValidationAttributes.VALIDATION_WARNINGS, (Serializable) validationWarnings));
      }
    }

    return attributeMap.values().stream().collect(Collectors.toList());
  }

  public Map<String, Boolean> getMultiValuedNameMap(Set<AttributeDescriptor> descriptors) {
    Map<String, Boolean> multiValuedMap = new HashMap<>();

    if (CollectionUtils.isNotEmpty(descriptors)) {
      for (AttributeDescriptor descriptor : descriptors) {
        if (descriptor != null) {
          multiValuedMap.put(descriptor.getName(), descriptor.isMultiValued());
        }
      }
    }

    return multiValuedMap;
  }
}
