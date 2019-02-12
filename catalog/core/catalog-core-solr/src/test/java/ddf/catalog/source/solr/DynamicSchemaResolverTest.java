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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.source.solr.json.MetacardTypeMapperFactory;
import java.io.IOException;
import java.io.Serializable;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;

public class DynamicSchemaResolverTest {

  private static final int INITIAL_FIELDS_CACHE_COUNT = 8;

  private static final ObjectMapper METACARD_TYPE_MAPPER =
      MetacardTypeMapperFactory.newObjectMapper();

  /**
   * Verify that when a metacard type has attribute descriptors that inherit from
   * AttributeDescriptorImpl, the attribute descriptors are recreated as AttributeDescriptorsImpls
   * before serialization into the solr cache.
   */
  @Test
  public void testAddFields() throws Exception {
    // Setup
    String metacardTypeName = "states";
    Set<AttributeDescriptor> attributeDescriptors = new HashSet<AttributeDescriptor>(1);
    String propertyName = "title";
    String name = metacardTypeName + "." + propertyName;
    boolean indexed = true;
    boolean stored = true;
    boolean tokenized = false;
    boolean multiValued = false;
    attributeDescriptors.add(
        new TestAttributeDescriptorImpl(
            name, propertyName, indexed, stored, tokenized, multiValued, BasicTypes.OBJECT_TYPE));
    Serializable mockValue = mock(Serializable.class);
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn(mockValue);
    Metacard mockMetacard = mock(Metacard.class, RETURNS_DEEP_STUBS);
    when(mockMetacard.getMetacardType().getName()).thenReturn(metacardTypeName);
    when(mockMetacard.getMetacardType().getAttributeDescriptors()).thenReturn(attributeDescriptors);
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

  /**
   * Verify that when the metadata size limit is set to 10 (less than the size of what is being
   * added to the SolrInputDocument) that it is not added to the SolrInputDocument
   */
  @Test
  public void testAddFieldsDoesntAddMetadataIfSizeGreaterThanSizeLimit() throws Exception {
    // Set
    System.setProperty("metadata.size.limit", "10");

    // Setup
    String metacardTypeName = "states";
    Set<AttributeDescriptor> attributeDescriptors = new HashSet<AttributeDescriptor>(1);
    String propertyName = "title";
    String name = metacardTypeName + "." + propertyName;
    boolean indexed = true;
    boolean stored = true;
    boolean tokenized = false;
    boolean multiValued = false;
    attributeDescriptors.add(
        new TestAttributeDescriptorImpl(
            name, propertyName, indexed, stored, tokenized, multiValued, BasicTypes.OBJECT_TYPE));
    Serializable mockValue = mock(Serializable.class);
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn(mockValue);
    Metacard mockMetacard = mock(Metacard.class, RETURNS_DEEP_STUBS);
    when(mockMetacard.getId()).thenReturn("FAKE ID 1");
    when(mockMetacard.getMetadata())
        .thenReturn("<?xml version=\"1.1\" encoding=\"UTF-8\"?><metadata></metadata>");
    when(mockMetacard.getMetacardType().getName()).thenReturn(metacardTypeName);
    when(mockMetacard.getMetacardType().getAttributeDescriptors()).thenReturn(attributeDescriptors);
    when(mockMetacard.getAttribute(name)).thenReturn(mockAttribute);
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    DynamicSchemaResolver resolver = new DynamicSchemaResolver();

    // Perform Test
    resolver.addFields(mockMetacard, solrInputDocument);
    assertThat(solrInputDocument.getFieldValue("lux_xml"), is(nullValue()));
  }

  /**
   * Verify that when the metadata size limit is set to 100 (more than the size of what is being
   * added to the SolrInputDocument) that it is added to the SolrInputDocument
   */
  @Test
  public void testAddFieldsAddsMetadataIfSizeLessThanSizeLimit() throws Exception {
    // Set
    System.setProperty("metadata.size.limit", "100");

    // Setup
    String metacardTypeName = "states";
    Set<AttributeDescriptor> attributeDescriptors = new HashSet<AttributeDescriptor>(1);
    String propertyName = "title";
    String name = metacardTypeName + "." + propertyName;
    boolean indexed = true;
    boolean stored = true;
    boolean tokenized = false;
    boolean multiValued = false;
    attributeDescriptors.add(
        new TestAttributeDescriptorImpl(
            name, propertyName, indexed, stored, tokenized, multiValued, BasicTypes.OBJECT_TYPE));
    Serializable mockValue = mock(Serializable.class);
    Attribute mockAttribute = mock(Attribute.class);
    when(mockAttribute.getValue()).thenReturn(mockValue);
    Metacard mockMetacard = mock(Metacard.class, RETURNS_DEEP_STUBS);
    when(mockMetacard.getId()).thenReturn("FAKE ID 2");
    when(mockMetacard.getMetadata())
        .thenReturn("<?xml version=\"1.1\" encoding=\"UTF-8\"?><metadata></metadata>");
    when(mockMetacard.getMetacardType().getName()).thenReturn(metacardTypeName);
    when(mockMetacard.getMetacardType().getAttributeDescriptors()).thenReturn(attributeDescriptors);
    when(mockMetacard.getAttribute(name)).thenReturn(mockAttribute);
    SolrInputDocument solrInputDocument = new SolrInputDocument();
    DynamicSchemaResolver resolver = new DynamicSchemaResolver();

    // Perform Test
    resolver.addFields(mockMetacard, solrInputDocument);
    assertThat(solrInputDocument.getFieldValue("lux_xml"), is(notNullValue()));
  }

  @Test
  public void testAddFieldsRevertsTo5mbMetadataSizeLimitTooLarge() throws Exception {
    long overflow = Integer.MAX_VALUE;
    System.setProperty("metadata.size.limit", String.valueOf(overflow + 1));
    DynamicSchemaResolver resolver = new DynamicSchemaResolver();

    int actual = resolver.getMetadataSizeLimit();
    assertThat(actual, equalTo(DynamicSchemaResolver.FIVE_MEGABYTES));
  }
  /**
   * Verify that when the metadata size limit is set to a non-numeric value that it is not added to
   * the metacard
   */
  @Test
  public void testAddFieldsRevertsTo5mbMetadataSizeLimitNotNumeric() throws Exception {
    // Set
    System.setProperty("metadata.size.limit", "supercalifragilisticexpialidocious");
    DynamicSchemaResolver resolver = new DynamicSchemaResolver();

    int actual = resolver.getMetadataSizeLimit();
    assertThat(actual, equalTo(DynamicSchemaResolver.FIVE_MEGABYTES));
  }

  /**
   * Verify that even unchecked exceptions generating xpath support do not prevent a metacard from
   * being stored.
   */
  @Test
  public void testBufferOverflow() throws MetacardCreationException {
    // Setup
    String metacardTypeName = "states";
    Set<AttributeDescriptor> attributeDescriptors = new HashSet<>(1);
    String attributeName = Metacard.METADATA;
    attributeDescriptors.add(new CoreAttributes().getAttributeDescriptor(attributeName));
    StringBuilder mockValue = new StringBuilder();
    mockValue.append("<?xml version=\"1.1\" encoding=\"UTF-32\"?><metadata></metadata>");
    Attribute mockAttribute = new AttributeImpl(Metacard.METADATA, mockValue.toString());
    Metacard mockMetacard = mock(Metacard.class, RETURNS_DEEP_STUBS);
    when(mockMetacard.getMetacardType().getName()).thenReturn(metacardTypeName);
    when(mockMetacard.getMetacardType().getAttributeDescriptors()).thenReturn(attributeDescriptors);
    when(mockMetacard.getAttribute(attributeName)).thenReturn(mockAttribute);
    when(mockMetacard.getMetadata()).thenReturn(mockValue.toString());
    SolrInputDocument mockSolrInputDocument = mock(SolrInputDocument.class);
    DynamicSchemaResolver resolver =
        new DynamicSchemaResolver(
            Collections.EMPTY_LIST,
            tinyTree -> {
              throw new BufferOverflowException();
            });

    // Perform Test
    resolver.addFields(mockMetacard, mockSolrInputDocument);

    // Verify: Verify that no exception was thrown
    // called from inside catch block, indicating safe error handling
    verify(mockMetacard).getId();
    verify(mockSolrInputDocument)
        .addField(eq(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME), Matchers.any());
    verify(mockSolrInputDocument, times(0)).addField(eq("lux_xml"), Matchers.any());
  }

  @Test
  public void testAdditionalFieldConstructorWithEmptyList() throws Exception {
    DynamicSchemaResolver resolver = new DynamicSchemaResolver(Collections.EMPTY_LIST);
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

    DynamicSchemaResolver resolver = new DynamicSchemaResolver(additionalFields);

    assertThat(
        resolver.fieldsCache.size(), equalTo(INITIAL_FIELDS_CACHE_COUNT + additionalFields.size()));
    assertThat(resolver.fieldsCache, hasItem(someExtraField));
    assertThat(resolver.fieldsCache, hasItem(anotherExtraField));
  }

  private MetacardType deserializeMetacardType(byte[] serializedMetacardType) throws IOException {
    return METACARD_TYPE_MAPPER.readValue(serializedMetacardType, MetacardType.class);
  }

  @Test
  public void getFieldGeo() {
    assertThat(
        new DynamicSchemaResolver().getField("anyGeo", AttributeFormat.DOUBLE, false),
        is("location_geo_index"));
  }

  @Test
  public void getFieldMatchingNumerical() {
    assertThat(
        new DynamicSchemaResolver().getField("unknown", AttributeFormat.DOUBLE, false),
        is("unknown_dbl"));
    assertThat(
        new DynamicSchemaResolver().getField("unknown", AttributeFormat.LONG, false),
        is("unknown_lng"));
    assertThat(
        new DynamicSchemaResolver().getField("unknown", AttributeFormat.INTEGER, false),
        is("unknown_int"));
    assertThat(
        new DynamicSchemaResolver().getField("unknown", AttributeFormat.SHORT, false),
        is("unknown_shr"));
    assertThat(
        new DynamicSchemaResolver().getField("unknown", AttributeFormat.FLOAT, false),
        is("unknown_flt"));
  }

  @Test
  public void getFieldNonNumerical() {
    assertThat(
        new DynamicSchemaResolver().getField("unknown", AttributeFormat.STRING, false),
        is("unknown_txt_tokenized"));
  }
}
