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
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transformer.xml.adapter.AttributeAdapter;
import ddf.catalog.transformer.xml.binding.AbstractAttributeType;
import ddf.catalog.transformer.xml.binding.ObjectElement;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.Test;

public class AttributeAdapterTest {

  @Test
  public void testUnmarshalSeralizedObject() throws Exception {
    AttributeAdapter attributeAdapter = getObjectAttributeAdapter();

    Map<String, String> originalMap = ImmutableMap.of("key1", "val1", "key2", "val2");
    Attribute attribute = new AttributeImpl("sampleAttr", (Serializable) originalMap);

    AbstractAttributeType abstractAttributeType = attributeAdapter.marshal(attribute);
    Attribute result = attributeAdapter.unmarshal(abstractAttributeType);

    Map<String, String> resultMap = (Map) result.getValue();
    assertThat(
        "Maps are not equal.", resultMap.entrySet(), everyItem(isIn(originalMap.entrySet())));
  }

  @Test
  public void testUnmarshalSeralizedObjectCollectionContainingNull() throws Exception {
    AttributeAdapter attributeAdapter = getObjectAttributeAdapter();

    List<String> originalList = Collections.unmodifiableList(Arrays.asList("val1", null, "val2"));

    Attribute attribute = new AttributeImpl("sampleAttr", (Serializable) originalList);

    AbstractAttributeType abstractAttributeType = attributeAdapter.marshal(attribute);
    Attribute result = attributeAdapter.unmarshal(abstractAttributeType);

    List<String> resultList = (List) result.getValues();
    assertThat("Lists are not equal.", resultList, everyItem(isIn(originalList)));
  }

  @Test
  public void testUnmarshalSeralizedObjectEmpty() throws Exception {
    AttributeAdapter attributeAdapter = getObjectAttributeAdapter();

    Attribute attribute = new AttributeImpl("sampleAttr", Collections.emptyList());

    AbstractAttributeType abstractAttributeType = attributeAdapter.marshal(attribute);
    Attribute result = attributeAdapter.unmarshal(abstractAttributeType);

    assertThat("Null attribute not returned.", result, is(Matchers.nullValue()));
  }

  @Test
  public void testUnmarshalSeralizedObjectInvalidByteArray() throws Exception {
    AttributeAdapter attributeAdapter = getObjectAttributeAdapter();

    ObjectElement mockObjectElement = mock(ObjectElement.class);
    doReturn("name").when(mockObjectElement).getName();
    doReturn(ImmutableList.of(new byte[] {-31, 0, 0, 0})).when(mockObjectElement).getValue();
    Attribute result = attributeAdapter.unmarshal(mockObjectElement);

    assertThat(
        "Unmarshalled byte array not returned", result.getValue(), is(instanceOf(byte[].class)));
  }

  private AttributeAdapter getObjectAttributeAdapter() {
    MetacardType mockMetacardType = mock(MetacardType.class);
    AttributeDescriptor mockAttributeDescriptor = mock(AttributeDescriptor.class);
    AttributeType mockAttributeType = mock(AttributeType.class);
    doReturn(AttributeFormat.OBJECT).when(mockAttributeType).getAttributeFormat();
    doReturn(mockAttributeType).when(mockAttributeDescriptor).getType();
    doReturn("name").when(mockAttributeDescriptor).getName();
    doReturn(mockAttributeDescriptor).when(mockMetacardType).getAttributeDescriptor(any());

    return new AttributeAdapter(mockMetacardType);
  }
}
