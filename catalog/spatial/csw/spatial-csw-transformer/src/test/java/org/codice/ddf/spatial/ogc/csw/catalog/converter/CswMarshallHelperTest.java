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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.joda.time.DateTime;
import org.junit.Test;

public class CswMarshallHelperTest {

  @Test
  public void testWriteTemporalData() {
    HierarchicalStreamWriter writer = mock(HierarchicalStreamWriter.class);
    MarshallingContext context = mock(MarshallingContext.class);
    MetacardImpl metacard = mock(MetacardImpl.class);

    DateTime effectiveDate = new DateTime(2015, 01, 01, 13, 15);
    DateTime expirationDate = new DateTime(2015, 06, 01, 10, 45);
    when(metacard.getEffectiveDate()).thenReturn(effectiveDate.toDate());
    when(metacard.getExpirationDate()).thenReturn(expirationDate.toDate());

    CswMarshallHelper.writeTemporalData(writer, context, metacard);

    verify(writer, times(1)).startNode(any(String.class));
    verify(writer, times(1)).setValue(any(String.class));
    verify(writer, times(1)).endNode();
  }

  @Test
  public void testWriteAllFields() {
    HierarchicalStreamWriter writer = mock(HierarchicalStreamWriter.class);
    MarshallingContext context = mock(MarshallingContext.class);

    Attribute attribute = mock(Attribute.class);
    when(attribute.getValues()).thenReturn(Arrays.asList(new String[] {"TEST1", "TEST2", "TEST3"}));

    MetacardImpl metacard = mock(MetacardImpl.class);
    MetacardType metacardType = mock(MetacardType.class);
    when(metacard.getMetacardType()).thenReturn(metacardType);
    when(metacard.getAttribute(any(String.class))).thenReturn(attribute);

    Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();
    AttributeDescriptor ad = mock(AttributeDescriptor.class);
    when(ad.isMultiValued()).thenReturn(true);
    when(ad.getName()).thenReturn(CswConstants.CSW_SOURCE_QNAME.toString());
    attributeDescriptors.add(ad);

    when(metacardType.getAttributeDescriptors()).thenReturn(attributeDescriptors);
    CswMarshallHelper.writeAllFields(writer, context, metacard);

    verify(writer, times(3)).startNode(any(String.class));
    verify(writer, times(3)).setValue(any(String.class));
    verify(writer, times(3)).endNode();
  }
}
