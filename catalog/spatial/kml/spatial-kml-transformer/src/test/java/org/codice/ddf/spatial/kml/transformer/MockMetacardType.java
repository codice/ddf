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
package org.codice.ddf.spatial.kml.transformer;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import java.util.Set;

public class MockMetacardType extends MetacardTypeImpl {

  public static final String NAME = "mockMetacardType";

  private static final long serialVersionUID = 1L;

  public MockMetacardType() {
    super(NAME, (Set<AttributeDescriptor>) null);

    addAll(MetacardImpl.BASIC_METACARD.getAttributeDescriptors());

    add(
        new AttributeDescriptorImpl(
            AttributeFormat.BINARY.toString(), true, true, false, true, BasicTypes.BINARY_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.BOOLEAN.toString(), true, true, false, true, BasicTypes.BOOLEAN_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.DATE.toString(), true, true, false, true, BasicTypes.DATE_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.DOUBLE.toString(), true, true, false, true, BasicTypes.DOUBLE_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.FLOAT.toString(), true, true, false, true, BasicTypes.FLOAT_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.GEOMETRY.toString(), true, true, false, true, BasicTypes.GEO_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.INTEGER.toString(), true, true, false, true, BasicTypes.INTEGER_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.LONG.toString(), true, true, false, true, BasicTypes.LONG_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.OBJECT.toString(), true, true, false, true, BasicTypes.OBJECT_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.SHORT.toString(), true, true, false, true, BasicTypes.SHORT_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.STRING.toString(), true, true, false, true, BasicTypes.STRING_TYPE));
    add(
        new AttributeDescriptorImpl(
            AttributeFormat.XML.toString(), true, true, false, true, BasicTypes.XML_TYPE));
  }
}
