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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.source.solr.json.MetacardTypeMapperFactory;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class DynamicSchemaResolverTest {

  private static final int INITIAL_FIELDS_CACHE_COUNT = 8;

  private static final ObjectMapper METACARD_TYPE_MAPPER =
      MetacardTypeMapperFactory.newObjectMapper();

  private DynamicSchemaResolver dynamicSchemaResolver;

  @Before
  public void setup() {
    dynamicSchemaResolver = new DynamicSchemaResolver();
  }

  @Test
  public void testAddMetacardType() {
    assertThat(dynamicSchemaResolver.getAnonymousField(Metacard.TITLE), empty());

    dynamicSchemaResolver.addMetacardType(MetacardImpl.BASIC_METACARD);

    assertThat(dynamicSchemaResolver.getAnonymousField(Metacard.TITLE), hasSize(1));
  }

  /**
   * Verify that when a metacard type has attribute descriptors that inherit from
   * AttributeDescriptorImpl, the attribute descriptors are recreated as AttributeDescriptorsImpls
   * before serialization into the solr cache.
   */
  @Test
  public void testAddFields() throws Exception {
    // Setup
    String metacardTypeName = "states";
    Set<AttributeDescriptor> attributeDescriptors = new HashSet<>(1);
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

    // Perform Test
    dynamicSchemaResolver.addFields(mockMetacard, mockSolrInputDocument);

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
    Set<AttributeDescriptor> attributeDescriptors = new HashSet<>(1);
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
    assertThat(solrInputDocument.getFieldValue("metadata_txt_tokenized"), is(nullValue()));
  }

  @Test
  public void testAddFieldsRevertsTo5mbMetadataSizeLimitTooLarge() {
    long overflow = Integer.MAX_VALUE;
    System.setProperty("metadata.size.limit", String.valueOf(overflow + 1));
    DynamicSchemaResolver resolver = new DynamicSchemaResolver();

    int actual = DynamicSchemaResolver.getMetadataSizeLimit();
    assertThat(actual, equalTo(DynamicSchemaResolver.FIVE_MEGABYTES));
  }
  /**
   * Verify that when the metadata size limit is set to a non-numeric value that it is not added to
   * the metacard
   */
  @Test
  public void testAddFieldsRevertsTo5mbMetadataSizeLimitNotNumeric() {
    // Set
    System.setProperty("metadata.size.limit", "supercalifragilisticexpialidocious");
    DynamicSchemaResolver resolver = new DynamicSchemaResolver();

    int actual = DynamicSchemaResolver.getMetadataSizeLimit();
    assertThat(actual, equalTo(DynamicSchemaResolver.FIVE_MEGABYTES));
  }

  @Test
  public void testAdditionalFieldConstructorWithEmptyList() {
    DynamicSchemaResolver resolver = new DynamicSchemaResolver(Collections.emptyList());
    int fieldsCacheSize = resolver.fieldsCache.size();

    assertThat(fieldsCacheSize, equalTo(INITIAL_FIELDS_CACHE_COUNT));
  }

  @Test
  public void testAdditionalFieldConstructor() {
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
  public void getField() {
    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.GEOMETRY, false, Collections.emptyMap()),
        is("unknown_geo_index"));

    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.XML, false, Collections.emptyMap()),
        is("unknown_xml_tpt"));

    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.STRING, false, Collections.emptyMap()),
        is("unknown_txt_tokenized"));

    Map<String, Serializable> enabledFeatures = new HashMap<>();
    enabledFeatures.put(DynamicSchemaResolver.PHONETICS_FEATURE, Boolean.TRUE);
    assertThat(
        dynamicSchemaResolver.getField("unknown", AttributeFormat.STRING, false, enabledFeatures),
        is("unknown_txt_phonetics"));

    enabledFeatures.put(DynamicSchemaResolver.PHONETICS_FEATURE, Boolean.FALSE);
    assertThat(
        dynamicSchemaResolver.getField("unknown", AttributeFormat.STRING, false, enabledFeatures),
        is("unknown_txt_tokenized"));
  }

  @Test
  public void getFieldExactValue() {
    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.DOUBLE, true, Collections.emptyMap()),
        is("unknown_dbl"));
    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.LONG, true, Collections.emptyMap()),
        is("unknown_lng"));
    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.INTEGER, true, Collections.emptyMap()),
        is("unknown_int"));
    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.SHORT, true, Collections.emptyMap()),
        is("unknown_shr"));
    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.FLOAT, true, Collections.emptyMap()),
        is("unknown_flt"));
    assertThat(
        dynamicSchemaResolver.getField(
            "anyGeo", AttributeFormat.BINARY, true, Collections.emptyMap()),
        is("location_geo_index"));
    assertThat(
        dynamicSchemaResolver.getField(
            "unknown", AttributeFormat.STRING, true, Collections.emptyMap()),
        is("unknown_txt"));

    Map<String, Serializable> enabledFeatures = new HashMap<>();
    enabledFeatures.put(DynamicSchemaResolver.PHONETICS_FEATURE, Boolean.TRUE);
    assertThat(
        dynamicSchemaResolver.getField("unknown", AttributeFormat.STRING, true, enabledFeatures),
        is("unknown_txt"));
  }

  @Test
  public void testAnyTextWhitelist() {
    ConfigurationStore config = ConfigurationStore.getInstance();
    List<String> resultList = new ArrayList<>();
    List<String> addedFields = Arrays.asList("one_txt", "example.one_txt", "example.two_txt");
    addedFields.stream().forEach(field -> dynamicSchemaResolver.anyTextFieldsCache.add(field));

    config.setAnyTextFieldWhitelist(Arrays.asList("one"));

    dynamicSchemaResolver.anyTextFields().forEach(field -> resultList.add(field));

    assertThat(resultList.size(), is(1));
    assertThat(resultList, contains("one_txt"));

    config.setAnyTextFieldWhitelist(Arrays.asList("one", "example.*"));
    resultList.clear();
    dynamicSchemaResolver.anyTextFields().forEach(field -> resultList.add(field));

    assertThat(resultList.size(), is(3));
    assertThat(resultList, containsInAnyOrder("one_txt", "example.one_txt", "example.two_txt"));

    config.setAnyTextFieldWhitelist(new ArrayList<>());
    addedFields.stream().forEach(field -> dynamicSchemaResolver.anyTextFieldsCache.remove(field));
  }

  @Test
  public void testAnyTextBlacklist() {
    ConfigurationStore config = ConfigurationStore.getInstance();
    List<String> resultList = new ArrayList<>();
    List<String> addedFields = Arrays.asList("one_txt", "example.one_txt", "example.two_txt");
    addedFields.stream().forEach(field -> dynamicSchemaResolver.anyTextFieldsCache.add(field));

    config.setAnyTextFieldBlacklist(Arrays.asList("one"));

    dynamicSchemaResolver.anyTextFields().forEach(field -> resultList.add(field));

    assertThat(resultList, not(hasItem("one_txt")));

    config.setAnyTextFieldBlacklist(Arrays.asList("one", "example.*"));
    resultList.clear();
    dynamicSchemaResolver.anyTextFields().forEach(field -> resultList.add(field));

    assertThat(resultList, not(hasItems("one_txt", "example.one_txt", "example.two_txt")));

    config.setAnyTextFieldBlacklist(new ArrayList<>());
    addedFields.stream().forEach(field -> dynamicSchemaResolver.anyTextFieldsCache.remove(field));
  }

  @Test
  public void testAnyTextBlacklistAndWhitelist() {
    ConfigurationStore config = ConfigurationStore.getInstance();
    List<String> resultList = new ArrayList<>();
    List<String> addedFields = Arrays.asList("one_txt", "example.one_txt", "example.two_txt");
    addedFields.stream().forEach(field -> dynamicSchemaResolver.anyTextFieldsCache.add(field));

    config.setAnyTextFieldBlacklist(Arrays.asList("example.*"));

    dynamicSchemaResolver.anyTextFields().forEach(field -> resultList.add(field));

    assertThat(resultList, not(hasItems("example.one_txt", "example.two_txt")));

    config.setAnyTextFieldWhitelist(Arrays.asList("example.one"));
    resultList.clear();
    dynamicSchemaResolver.anyTextFields().forEach(field -> resultList.add(field));

    assertThat(resultList, hasItem("example.one_txt"));
    assertThat(resultList, not(hasItem("example.two_txt")));

    config.setAnyTextFieldWhitelist(new ArrayList<>());
    config.setAnyTextFieldBlacklist(new ArrayList<>());
    addedFields.stream().forEach(field -> dynamicSchemaResolver.anyTextFieldsCache.remove(field));
  }
}
