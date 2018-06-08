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
package ddf.catalog.source.solr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class DynamicSchemaResolverTest {

  private static final int INITIAL_FIELDS_CACHE_COUNT = 8;

  private static final String TXT = "_txt";
  /**
   * Verify that when a metacard type has attribute descriptors that inherit from
   * AttributeDescriptorImpl, the attribute descriptors are recreated as AttributeDescriptorsImpls
   * before serialization into the solr cache.
   */
  @Test
  public void testAddFields() throws Exception {
    // Setup
    String metacardTypeName = "states";
    Set<AttributeDescriptor> addtributeDescriptors = new HashSet<AttributeDescriptor>(1);
    String propertyName = "title";
    String name = metacardTypeName + "." + propertyName;
    boolean indexed = true;
    boolean stored = true;
    boolean tokenized = false;
    boolean multiValued = false;
    addtributeDescriptors.add(
        new TestAttributeDescriptorImpl(
            name, propertyName, indexed, stored, tokenized, multiValued, BasicTypes.OBJECT_TYPE));
    Serializable mockValue = mock(Serializable.class);
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn(mockValue);
    Metacard mockMetacard = mock(Metacard.class, RETURNS_DEEP_STUBS);
    when(mockMetacard.getMetacardType().getName()).thenReturn(metacardTypeName);
    when(mockMetacard.getMetacardType().getAttributeDescriptors())
        .thenReturn(addtributeDescriptors);
    when(mockMetacard.getAttribute(name)).thenReturn(mockAttribute);
    ArgumentCaptor<byte[]> metacardTypeBytes = ArgumentCaptor.forClass(byte[].class);
    SolrInputDocument mockSolrInputDocument = mock(SolrInputDocument.class);
    DynamicSchemaResolver resolver = new DynamicSchemaResolver();

    // Perform Test
    resolver.addFields(mockMetacard, mockSolrInputDocument);

    // Verify: Verify that TestAttributeDescriptorImpl has been recreated as a
    // AttributeDescriptorImpl.
    verify(mockSolrInputDocument)
        .addField(eq(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME), metacardTypeBytes.capture());
    byte[] serializedMetacardType = metacardTypeBytes.getValue();
    MetacardType metacardType = deserializeMetacardType(serializedMetacardType);
    for (AttributeDescriptor attributeDescriptor : metacardType.getAttributeDescriptors()) {
      assertThat(
          attributeDescriptor.getClass().getName(), is(AttributeDescriptorImpl.class.getName()));
    }
  }

  @Test
  public void testAdditionalFieldConstructorWithEmptyList() throws Exception {
    DynamicSchemaResolver resolver =
        new DynamicSchemaResolver(Collections.emptyList(), Collections.EMPTY_LIST);
    int fieldsCacheSize = resolver.fieldsCache.size();

    assertThat(fieldsCacheSize, equalTo(INITIAL_FIELDS_CACHE_COUNT));
  }

  @Test
  public void testAdditionalFieldConstructor() throws Exception {
    String someExtraField = "someExtraField";
    String anotherExtraField = "anotherExtraField";

    List<String> additionalFields = new ArrayList<>();
    additionalFields.add(someExtraField);
    additionalFields.add(anotherExtraField);

    DynamicSchemaResolver resolver =
        new DynamicSchemaResolver(Collections.emptyList(), additionalFields);

    assertThat(
        resolver.fieldsCache.size(), equalTo(INITIAL_FIELDS_CACHE_COUNT + additionalFields.size()));
    assertThat(resolver.fieldsCache, hasItem(someExtraField));
    assertThat(resolver.fieldsCache, hasItem(anotherExtraField));
  }

  @Test
  public void testNoAnyTextFields() {
    DynamicSchemaResolver resolver = new DynamicSchemaResolver();
    verifyDefaultAttributes(resolver);
  }

  @Test
  public void testWildcardAnyTextFields() {
    DynamicSchemaResolver resolver =
        new DynamicSchemaResolver(Collections.singletonList("*"), Collections.emptyList());
    verifyDefaultAttributes(resolver);
  }

  @Test
  public void testSpecifiedAnyTextFields() {
    DynamicSchemaResolver resolver =
        new DynamicSchemaResolver(Collections.singletonList(Core.TITLE), Collections.emptyList());
    assertThat(resolver.anyTextFieldsCache, hasSize(1));
    assertThat(resolver.anyTextFieldsCache, hasItem(Core.TITLE + TXT));
  }

  private MetacardType deserializeMetacardType(byte[] serializedMetacardType)
      throws ClassNotFoundException, IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) serializedMetacardType);
    ObjectInputStream in = new ObjectInputStream(bais);
    MetacardType metacardType = (MetacardType) in.readObject();
    IOUtils.closeQuietly(bais);
    IOUtils.closeQuietly(in);
    return metacardType;
  }

  private void verifyDefaultAttributes(DynamicSchemaResolver resolver) {
    List<String> expectedDescriptors =
        MetacardImpl.BASIC_METACARD
            .getAttributeDescriptors()
            .stream()
            .filter(descriptor -> BasicTypes.STRING_TYPE.equals(descriptor.getType()))
            .map(descriptor -> descriptor.getName() + TXT)
            .collect(Collectors.toList());
    String metadataAttr = Metacard.METADATA + TXT;
    assertThat(resolver.anyTextFieldsCache, hasItems(expectedDescriptors.toArray(new String[0])));
    assertThat(resolver.anyTextFieldsCache, hasItem(metadataAttr));
  }
}
