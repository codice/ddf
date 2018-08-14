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
package org.codice.ddf.spatial.geocoding;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import java.util.Set;

public class GeoEntryAttributes implements MetacardType {

  public static final String FEATURE_CODE_ATTRIBUTE_NAME = "ext.feature-code";

  public static final String FEATURE_CLASS_ATTRIBUTE_NAME = "ext.feature-class";

  public static final String POPULATION_ATTRIBUTE_NAME = "ext.population";

  public static final String IMPORT_LOCATION = "ext.import-location";

  public static final String GAZETTEER_SORT_VALUE = "ext.gazetteer-sort-value";

  private static final String METACARD_TYPENAME = "GeoEntryAttributes";

  private static final Set<AttributeDescriptor> DESCRIPTORS =
      ImmutableSet.of(
          new AttributeDescriptorImpl(
              FEATURE_CLASS_ATTRIBUTE_NAME, true, true, false, false, BasicTypes.STRING_TYPE),
          new AttributeDescriptorImpl(
              FEATURE_CODE_ATTRIBUTE_NAME, true, true, false, false, BasicTypes.STRING_TYPE),
          new AttributeDescriptorImpl(
              POPULATION_ATTRIBUTE_NAME, true, true, false, false, BasicTypes.LONG_TYPE),
          new AttributeDescriptorImpl(
              IMPORT_LOCATION, true, true, false, false, BasicTypes.STRING_TYPE),
          new AttributeDescriptorImpl(
              GAZETTEER_SORT_VALUE, true, true, false, false, BasicTypes.INTEGER_TYPE));

  @Override
  public String getName() {
    return METACARD_TYPENAME;
  }

  @Override
  public Set<AttributeDescriptor> getAttributeDescriptors() {
    return DESCRIPTORS;
  }

  @Override
  public AttributeDescriptor getAttributeDescriptor(String attributeName) {
    for (AttributeDescriptor attributeDescriptor : DESCRIPTORS) {
      if (attributeName.equals(attributeDescriptor.getName())) {
        return attributeDescriptor;
      }
    }
    return null;
  }
}
