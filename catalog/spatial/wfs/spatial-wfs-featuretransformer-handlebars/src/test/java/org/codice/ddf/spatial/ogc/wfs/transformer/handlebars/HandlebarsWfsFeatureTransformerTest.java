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

import static org.codice.ddf.libs.geo.util.GeospatialUtil.LAT_LON_ORDER;
import static org.codice.ddf.libs.geo.util.GeospatialUtil.LON_LAT_ORDER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
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

  @Test
  public void coordinateOrderIsReversedWhenFeatureCoordinateOrderIsLatLon() throws Exception {
    when(mockWfsMetadata.getCoordinateOrder()).thenReturn(LAT_LON_ORDER);

    try (final InputStream is = getClass().getResourceAsStream("/FeatureMemberPolygon.xml")) {
      final Optional<Metacard> metacardOptional = transformer.apply(is, mockWfsMetadata);
      assertThat(
          "The transformer did not return a metacard.", metacardOptional.isPresent(), is(true));

      assertThat(
          metacardOptional.get().getLocation(),
          is("POLYGON ((25.35 15.25, 25.55 15.35, 25.55 15.55, 25.35 15.65, 25.35 15.25))"));
    }
  }

  @Test
  public void coordinateOrderIsNotReversedWhenFeatureCoordinateOrderIsLonLat() throws Exception {
    when(mockWfsMetadata.getCoordinateOrder()).thenReturn(LON_LAT_ORDER);

    try (final InputStream is = getClass().getResourceAsStream("/FeatureMemberPolygon.xml")) {
      final Optional<Metacard> metacardOptional = transformer.apply(is, mockWfsMetadata);
      assertThat(
          "The transformer did not return a metacard.", metacardOptional.isPresent(), is(true));

      assertThat(
          metacardOptional.get().getLocation(),
          is("POLYGON ((15.25 25.35, 15.35 25.55, 15.55 25.55, 15.65 25.35, 15.25 25.35))"));
    }
  }

  @Test
  public void xmlAttributesCanBeMapped() {
    final List<String> mappings = new ArrayList<>(getMappings());
    mappings.add(createMapping(Core.TITLE, "PeterPan@attr1", "{{PeterPan@attr1}}"));
    mappings.add(createMapping(Core.DESCRIPTION, "PanTopic@attr1", "{{PanTopic@attr1}}"));
    mappings.add(createMapping(Core.METACARD_OWNER, "PanTopic@attr2", "{{PanTopic@attr2}}"));
    transformer.setAttributeMappings(mappings);

    final Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(
        "The transformer did not return a metacard.", metacardOptional.isPresent(), equalTo(true));

    final Metacard metacard = metacardOptional.get();
    assertDefaultAttributesExist(metacard);
    assertExpectedAttributes(metacard);

    assertThat(getAttributeValue(Core.TITLE, metacard), is("value1"));
    assertThat(getAttributeValue(Core.DESCRIPTION, metacard), is("value2"));
    assertThat(getAttributeValue(Core.METACARD_OWNER, metacard), is("value3"));
  }

  @Test
  public void resourceSizeInBytes() {
    transformer.setAttributeMappings(
        Collections.singletonList(
            createMapping(Core.RESOURCE_SIZE, "ResourceSize", "{{ResourceSize}}")));
    transformer.setDataUnit(WfsConstants.B);

    final Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(
        "The transformer should have returned a metacard but didn't.",
        metacardOptional.isPresent(),
        is(true));

    final Metacard metacard = metacardOptional.get();
    assertThat(getAttributeValue(Core.RESOURCE_SIZE, metacard), is("1.0"));
  }

  @Test
  public void resourceSizeInKilobytes() {
    transformer.setAttributeMappings(
        Collections.singletonList(
            createMapping(Core.RESOURCE_SIZE, "ResourceSize", "{{ResourceSize}}")));
    transformer.setDataUnit(WfsConstants.KB);

    final Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(
        "The transformer should have returned a metacard but didn't.",
        metacardOptional.isPresent(),
        is(true));

    final Metacard metacard = metacardOptional.get();
    assertThat(
        getAttributeValue(Core.RESOURCE_SIZE, metacard), is(WfsConstants.BYTES_PER_KB + ".0"));
  }

  @Test
  public void resourceSizeInMegabytes() {
    transformer.setAttributeMappings(
        Collections.singletonList(
            createMapping(Core.RESOURCE_SIZE, "ResourceSize", "{{ResourceSize}}")));
    transformer.setDataUnit(WfsConstants.MB);

    final Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(
        "The transformer should have returned a metacard but didn't.",
        metacardOptional.isPresent(),
        is(true));

    final Metacard metacard = metacardOptional.get();
    assertThat(
        getAttributeValue(Core.RESOURCE_SIZE, metacard), is(WfsConstants.BYTES_PER_MB + ".0"));
  }

  @Test
  public void resourceSizeInGigabytes() {
    transformer.setAttributeMappings(
        Collections.singletonList(
            createMapping(Core.RESOURCE_SIZE, "ResourceSize", "{{ResourceSize}}")));
    transformer.setDataUnit(WfsConstants.GB);

    final Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(
        "The transformer should have returned a metacard but didn't.",
        metacardOptional.isPresent(),
        is(true));

    final Metacard metacard = metacardOptional.get();
    assertThat(
        getAttributeValue(Core.RESOURCE_SIZE, metacard), is(WfsConstants.BYTES_PER_GB + ".0"));
  }

  @Test
  public void resourceSizeInTerabytes() {
    transformer.setAttributeMappings(
        Collections.singletonList(
            createMapping(Core.RESOURCE_SIZE, "ResourceSize", "{{ResourceSize}}")));
    transformer.setDataUnit(WfsConstants.TB);

    final Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(
        "The transformer should have returned a metacard but didn't.",
        metacardOptional.isPresent(),
        is(true));

    final Metacard metacard = metacardOptional.get();
    assertThat(
        getAttributeValue(Core.RESOURCE_SIZE, metacard), is(WfsConstants.BYTES_PER_TB + ".0"));
  }

  @Test
  public void resourceSizeInPetabytes() {
    transformer.setAttributeMappings(
        Collections.singletonList(
            createMapping(Core.RESOURCE_SIZE, "ResourceSize", "{{ResourceSize}}")));
    transformer.setDataUnit(WfsConstants.PB);

    final Optional<Metacard> metacardOptional = transformer.apply(inputStream, mockWfsMetadata);
    assertThat(
        "The transformer should have returned a metacard but didn't.",
        metacardOptional.isPresent(),
        is(true));

    final Metacard metacard = metacardOptional.get();
    assertThat(
        getAttributeValue(Core.RESOURCE_SIZE, metacard), is(WfsConstants.BYTES_PER_PB + ".0"));
  }

  private void assertDefaultAttributesExist(Metacard metacard) {
    assertThat(metacard.getEffectiveDate(), notNullValue());
    assertThat(metacard.getModifiedDate(), notNullValue());
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
    when(mockWfsMetadata.getCoordinateOrder()).thenReturn(LAT_LON_ORDER);
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
