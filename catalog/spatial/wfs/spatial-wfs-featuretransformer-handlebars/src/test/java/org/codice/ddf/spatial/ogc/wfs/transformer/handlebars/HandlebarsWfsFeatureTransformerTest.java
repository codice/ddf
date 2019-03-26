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
package org.codice.ddf.spatial.ogc.wfs.transformer.handlebars;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.WfsMetacardTypeRegistry;
import org.codice.ddf.spatial.ogc.wfs.featuretransformer.WfsMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HandlebarsWfsFeatureTransformerTest {
  private static final String EXPECTED_FEATURE_TYPE =
      "{http://www.neverland.org/peter/pan}PeterPan";
  private static final String EXPECTED_FEATURE_TYPE_LOCAL_PART = "PeterPan";

  private static final String ATTRIBUTE_NAME = "attributeName";

  private static final String FEATURE_NAME = "featureName";

  private static final String TEMPLATE = "template";

  private static final String MAPPING_FORMAT =
      "{ \""
          + ATTRIBUTE_NAME
          + "\":\"%s\", \""
          + FEATURE_NAME
          + "\":\"%s\", \""
          + TEMPLATE
          + "\":\"%s\"}";
  private static final String SOURCE_ID = "wfsId";

  @Mock private WfsMetacardTypeRegistry mockMetacardTypeRegistry;
  @Mock private WfsMetadata mockWfsMetadata;
  private InputStream inputStream;
  private HandlebarsWfsFeatureTransformer transformer;

  @Before
  public void setup() {
    setupWfsMetadata();
    setupMetacardTypeRegistry();
    transformer = new HandlebarsWfsFeatureTransformer();
    transformer.setDataUnit("G");
    transformer.setFeatureType(EXPECTED_FEATURE_TYPE);
    transformer.setAttributeMappings(getMappings());
    transformer.setMetacardTypeRegistry(mockMetacardTypeRegistry);

    inputStream =
        new BufferedInputStream(
            HandlebarsWfsFeatureTransformer.class.getResourceAsStream("/FeatureMember.xml"));
  }

  @Test
  public void testRead() {
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);

    assertThat(metacardOptional.isPresent(), is(true));
  }

  @Test
  public void metacardAttributesAsExpected() {
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);

    assertThat(metacardOptional.isPresent(), is(true));
    Metacard metacard = metacardOptional.get();

    assertDefaultAttributesExist(metacard);
    assertExpectedAttributes(metacard);
  }

  @Test
  public void setAttributeMappings() {
    transformer.setAttributeMappings(
        Collections.singletonList(
            createMapping(Location.COUNTRY_CODE, "CountryCode", "{{CountryCode}}")));
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);

    assertThat(metacardOptional.isPresent(), is(true));
    Metacard metacard = metacardOptional.get();

    assertThat(getAttributeValue(Location.COUNTRY_CODE, metacard), equalTo("GBR"));
  }

  @Test
  public void setDefaultValueEvenIfFeatureTypeNotFound() {
    transformer.setAttributeMappings(
        Collections.singletonList(createMapping(Location.COUNTRY_CODE, "YouWillNotFindMe", "USA")));
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);

    assertThat(metacardOptional.isPresent(), is(true));
    Metacard metacard = metacardOptional.get();

    assertThat(getAttributeValue(Location.COUNTRY_CODE, metacard), equalTo("USA"));
  }

  @Test
  public void invalidStateBlankFeatureTypeReturnsEmpty() {
    transformer.setFeatureType("");
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(metacardOptional, equalTo(Optional.empty()));
  }

  @Test
  public void invalidStateNoMappingsReturnsEmpty() {
    transformer.setAttributeMappings(Collections.EMPTY_LIST);
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(metacardOptional, equalTo(Optional.empty()));
  }

  @Test
  public void invalidStateNullInputStreamReturnsEmpty() {
    Optional<Metacard> metacardOptional = transformer.apply(null, mockWfsMetadata);
    assertThat(metacardOptional, equalTo(Optional.empty()));
  }

  @Test
  public void invalidStateNullWfsMetadataReturnsEmpty() {
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, null);
    assertThat(metacardOptional, equalTo(Optional.empty()));
  }

  @Test
  public void metacardLookupFailsReturnsEmpty() {
    when(mockMetacardTypeRegistry.lookupMetacardTypeBySimpleName(anyString(), anyString()))
        .thenReturn(Optional.empty());
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(metacardOptional, equalTo(Optional.empty()));
  }

  @Test
  public void invalidFeatureTypeReturnsEmpty() {
    transformer.setFeatureType("IWillNotBeHandled");
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(metacardOptional, equalTo(Optional.empty()));
  }

  @Test
  public void badMappingInListIsIgnored() {
    List<String> mappings = new ArrayList<>(getMappings());
    mappings.add(
        "{\"attributeName\": \"description\", \"featureName\": someValue{:that:}JSON[Doesn't like}, \"template\": \"defaultValue\"}");
    transformer.setAttributeMappings(mappings);

    Optional<Metacard> optionalMetacard = transformer.apply(inputStream, mockWfsMetadata);

    assertThat(optionalMetacard.isPresent(), equalTo(true));
    Metacard metacard = optionalMetacard.get();

    assertDefaultAttributesExist(metacard);
    assertExpectedAttributes(metacard);
    assertThat(((MetacardImpl) metacard).getDescription(), nullValue());
  }

  @Test
  public void badTemplateIsIgnored() {
    List<String> mappings = new ArrayList<>(getMappings());
    mappings.add(createMapping(Core.DESCRIPTION, "Keyword", "badTemplate {{Keyword}"));
    transformer.setAttributeMappings(mappings);

    Optional<Metacard> optionalMetacard = transformer.apply(inputStream, mockWfsMetadata);

    assertThat(optionalMetacard.isPresent(), equalTo(true));
    Metacard metacard = optionalMetacard.get();

    assertDefaultAttributesExist(metacard);
    assertExpectedAttributes(metacard);
    assertThat(((MetacardImpl) metacard).getDescription(), nullValue());
  }

  @Test
  public void badDateNotAddedToMetacard() {
    List<String> mappings = new ArrayList<>(getMappings());
    mappings.add(createMapping(Core.EXPIRATION, "Created", "DefaultValueNotADate"));
    transformer.setAttributeMappings(mappings);
    Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);

    assertThat(metacardOptional.isPresent(), equalTo(true));
    Metacard metacard = metacardOptional.get();

    assertDefaultAttributesExist(metacard);
    assertExpectedAttributes(metacard);
    assertThat(metacard.getExpirationDate(), nullValue());
  }

  @Test
  public void invalidFeatureMemberXmlReturnsEmpty() {
    InputStream randomXmlInputStream =
        new BufferedInputStream(
            HandlebarsWfsFeatureTransformer.class.getResourceAsStream("/notAFeatureMember.xml"));
    Optional<Metacard> metacardOptional = transformer.apply(randomXmlInputStream, mockWfsMetadata);
    assertThat(metacardOptional, equalTo(Optional.empty()));
  }

  private void assertDefaultAttributesExist(Metacard metacard) {
    assertThat(metacard.getEffectiveDate(), notNullValue());
    assertThat(metacard.getAttribute(Core.MODIFIED).getValue(), notNullValue());
    assertThat(metacard.getContentTypeName(), notNullValue());
    assertThat(metacard.getAttribute(Metacard.TARGET_NAMESPACE), notNullValue());
  }

  private void assertExpectedAttributes(Metacard metacard) {
    assertThat(getAttributeValue(Location.COUNTRY_CODE, metacard), equalTo("CC - GBR"));
    assertThat(getAttributeValue(Core.LOCATION, metacard), equalTo("POINT (-123.26 49.39)"));
    assertThat(getAttributeValue(Contact.POINT_OF_CONTACT_NAME, metacard), equalTo("Captain Hook"));
  }

  private List<String> getMappings() {
    return Arrays.asList(
        createMapping(Location.COUNTRY_CODE, "CountryCode", "CC - {{CountryCode}}"),
        createMapping(Core.LOCATION, "SpatialData", "{{SpatialData}}"),
        createMapping(Contact.POINT_OF_CONTACT_NAME, "POCName", "Captain Hook"));
  }

  private String createMapping(String attributeName, String featureName, String template) {
    return String.format(MAPPING_FORMAT, attributeName, featureName, template);
  }

  private String getAttributeValue(String attributeName, Metacard metacard) {
    Attribute attribute = metacard.getAttribute(attributeName);
    assertThat(attributeName, notNullValue());

    return (String) attribute.getValue();
  }

  private void setupMetacardTypeRegistry() {
    when(mockMetacardTypeRegistry.lookupMetacardTypeBySimpleName(eq(SOURCE_ID), anyString()))
        .thenReturn(Optional.empty());
    when(mockMetacardTypeRegistry.lookupMetacardTypeBySimpleName(
            SOURCE_ID, EXPECTED_FEATURE_TYPE_LOCAL_PART))
        .thenReturn(Optional.of(getMetacardType()));
  }

  private void setupWfsMetadata() {
    when(mockWfsMetadata.getId()).thenReturn(SOURCE_ID);
  }

  private MetacardType getMetacardType() {
    List<MetacardType> metacardTypes = new ArrayList<>();
    metacardTypes.add(new CoreAttributes());
    metacardTypes.add(new LocationAttributes());
    metacardTypes.add(new DateTimeAttributes());
    metacardTypes.add(new ContactAttributes());

    return new MetacardTypeImpl("sampleMetacardType", metacardTypes);
  }
}
