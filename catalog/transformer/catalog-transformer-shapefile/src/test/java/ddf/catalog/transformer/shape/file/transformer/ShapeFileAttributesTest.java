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
package ddf.catalog.transformer.shape.file.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.transformer.shape.file.transformer.api.ShapeFile;
import ddf.catalog.transformer.shape.file.transformer.api.ShapeFileAttributes;
import org.junit.Before;
import org.junit.Test;

/** Created by nmay on 6/29/17. */
public class ShapeFileAttributesTest {
  private ShapeFileAttributes shapeFileAttributes;

  @Before
  public void setup() {
    shapeFileAttributes = new ShapeFileAttributes();
  }

  @Test
  public void testGetAttributeDescriptor() throws Exception {
    AttributeDescriptor shapeCountAttributeDescriptor =
        shapeFileAttributes.getAttributeDescriptor(ShapeFile.SHAPE_TYPE);

    AttributeDescriptor expectedAttributeDescriptor =
        new AttributeDescriptorImpl(
            ShapeFile.SHAPE_TYPE,
            false /* indexed */,
            false /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE);

    assertThat(shapeCountAttributeDescriptor, is(expectedAttributeDescriptor));
  }

  @Test
  public void testGetNullAttributeDescriptor() throws Exception {
    AttributeDescriptor nullAttributeDescriptor = shapeFileAttributes.getAttributeDescriptor("");
    assertThat(nullAttributeDescriptor, equalTo(null));
  }
}
