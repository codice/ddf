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
package ddf.catalog.transform.xml.adapter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transformer.xml.adapter.AttributeAdapter;
import ddf.catalog.transformer.xml.binding.AbstractAttributeType;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import org.junit.Test;

public class AttributeAdapterTest {

  @Test
  public void testSeralizeObject() throws Exception {
    MetacardType mockMetacardType = mock(MetacardType.class);
    AttributeDescriptor mockAttributeDescriptor = mock(AttributeDescriptor.class);
    AttributeType mockAttributeType = mock(AttributeType.class);
    doReturn(AttributeFormat.OBJECT).when(mockAttributeType).getAttributeFormat();
    doReturn(mockAttributeType).when(mockAttributeDescriptor).getType();
    doReturn("name").when(mockAttributeDescriptor).getName();
    doReturn(mockAttributeDescriptor).when(mockMetacardType).getAttributeDescriptor(any());

    AttributeAdapter attributeAdapter = new AttributeAdapter(mockMetacardType);

    Map<String, String> originalMap = ImmutableMap.of("key1", "val1", "key2", "val2");
    Attribute attribute = new AttributeImpl("sampleAttr", (Serializable) originalMap);

    AbstractAttributeType abstractAttributeType = attributeAdapter.marshal(attribute);
    Attribute result = attributeAdapter.unmarshal(abstractAttributeType);

    Map resultMap = (Map) result.getValue();
    for (Entry entry : originalMap.entrySet()) {
      assertThat(
          "Hashmap missing key from original: " + entry.getKey(),
          resultMap.containsKey(entry.getKey()));
      assertThat(
          "Hashmap missing value from original: " + entry.getValue(),
          resultMap.containsValue(entry.getValue()));
    }
  }
}
