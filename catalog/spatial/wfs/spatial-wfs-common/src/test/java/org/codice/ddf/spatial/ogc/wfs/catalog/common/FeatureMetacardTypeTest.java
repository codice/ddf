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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.MetacardTypeEnhancer;
import org.junit.Test;

public class FeatureMetacardTypeTest {

  private static final QName FEATURE_TYPE = new QName("", "FeatureTypeName");

  private static final String GML_3_1_1_NAMESPACE = "http://www.opengis.net/gml";

  private static final String ELEMENT_NAME = "ELEMENT_NAME";

  private static final String ELEMENT_NAME_1 = ELEMENT_NAME + "1";

  private static final String ELEMENT_NAME_2 = ELEMENT_NAME + "2";

  private static final String ELEMENT_NAME_3 = ELEMENT_NAME + "3";

  private static final Set<String> EMPTY_NON_QUERYABLE_PROPS = Collections.emptySet();

  private XmlSchema schema;

  @Test
  public void testFeatureMetacardTypeFindSingleGmlGeospatialProperty() throws Exception {
    loadSchema("singleGmlGeospatialProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getGmlProperties(), is(singletonList(ELEMENT_NAME_1)));
    assertThat(featureMetacardType.getProperties(), is(singletonList(ELEMENT_NAME_1)));
  }

  @Test
  public void testFeatureMetacardTypeFindSingleGmlTemporalProperty() throws Exception {
    loadSchema("singleGmlTemporalProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getTemporalProperties(), is(singletonList(ELEMENT_NAME_1)));
    assertThat(featureMetacardType.getProperties(), is(singletonList(ELEMENT_NAME_1)));
  }

  @Test
  public void testFeatureMetacardTypeMultipleGmlProperties() throws Exception {
    loadSchema("multipleGmlProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getGmlProperties(), is(singletonList(ELEMENT_NAME_1)));
    assertThat(featureMetacardType.getTemporalProperties(), is(singletonList(ELEMENT_NAME_2)));
    assertThat(
        featureMetacardType.getProperties(), containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
  }

  @Test
  public void testFeatureMetacardTypeNonQueryGmlProperty() throws Exception {
    loadSchema("multipleGmlProperties.xsd");

    Set<String> nonQueryProps = new HashSet<>();
    nonQueryProps.add(ELEMENT_NAME_2);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(schema, FEATURE_TYPE, nonQueryProps, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getGmlProperties(), is(singletonList(ELEMENT_NAME_1)));
    assertThat(featureMetacardType.getTemporalProperties(), is(singletonList(ELEMENT_NAME_2)));
    assertThat(
        featureMetacardType.getProperties(), containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
    assertThat(featureMetacardType.isQueryable(ELEMENT_NAME_1), is(true));
    assertThat(featureMetacardType.isQueryable(ELEMENT_NAME_2), is(false));
  }

  @Test
  public void testFeatureMetacardTypeSingleStringProperty() throws Exception {
    loadSchema("singleStringProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList(ELEMENT_NAME_1)));
    assertThat(featureMetacardType.getProperties(), is(singletonList(ELEMENT_NAME_1)));
  }

  @Test
  public void testFeatureMetacardTypeMultipleStringProperties() throws Exception {
    loadSchema("multipleStringProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getTextualProperties(),
        containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
    assertThat(
        featureMetacardType.getProperties(), containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
  }

  @Test
  public void testFeatureMetacardTypeStringNonQueryProperty() throws Exception {
    loadSchema("multipleStringProperties.xsd");

    Set<String> nonQueryProps = new HashSet<>();
    nonQueryProps.add(ELEMENT_NAME_1);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(schema, FEATURE_TYPE, nonQueryProps, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getTextualProperties(),
        containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
    assertThat(
        featureMetacardType.getProperties(), containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
    assertThat(featureMetacardType.isQueryable(ELEMENT_NAME_1), is(false));
    assertThat(featureMetacardType.isQueryable(ELEMENT_NAME_2), is(true));
  }

  @Test
  public void testFeatureMetacardTypeStringAndGmlProperties() throws Exception {
    loadSchema("stringAndGmlProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList(ELEMENT_NAME_1)));
    assertThat(featureMetacardType.getGmlProperties(), is(singletonList(ELEMENT_NAME_2)));
    assertThat(
        featureMetacardType.getProperties(), containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
  }

  @Test
  public void testFeatureMetacardTypeComplexContentWithStringAndGmlProperties() throws Exception {
    loadSchema("stringAndGmlPropertiesComplexContent.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList(ELEMENT_NAME_2)));
    assertThat(featureMetacardType.getGmlProperties(), is(singletonList(ELEMENT_NAME_1)));
    assertThat(
        featureMetacardType.getProperties(), containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
  }

  @Test
  public void testFeatureMetacardTypeFindDateProperties() throws Exception {
    loadSchema("dateAndDateTimeProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(), containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2));
  }

  @Test
  public void testFeatureMetacardTypeFindBooleanProperties() throws Exception {
    loadSchema("singleBooleanProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getProperties(), is(singletonList(ELEMENT_NAME_1)));
  }

  @Test
  public void testFeatureMetacardTypeFindDoubleProperties() throws Exception {
    loadSchema("singleDoubleProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getProperties(), is(singletonList(ELEMENT_NAME_1)));
  }

  @Test
  public void testFeatureMetacardTypeFindFloatProperties() throws Exception {
    loadSchema("singleFloatProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getProperties(), is(singletonList(ELEMENT_NAME_1)));
  }

  @Test
  public void testFeatureMetacardTypeFindIntegerProperties() throws Exception {
    loadSchema("multipleIntegerProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(),
        containsInAnyOrder(ELEMENT_NAME_1, ELEMENT_NAME_2, ELEMENT_NAME_3));
  }

  @Test
  public void testFeatureMetacardTypeFindLongProperties() throws Exception {
    loadSchema("singleLongProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getProperties(), is(singletonList(ELEMENT_NAME_1)));
  }

  @Test
  public void testFeatureMetacardTypeFindShortProperties() throws Exception {
    loadSchema("singleShortProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getProperties(), is(singletonList(ELEMENT_NAME_1)));
  }

  @Test
  public void testFeatureMetacardTypeFindTaxonomyMetacardAttributes() throws Exception {
    loadSchema("singleStringProperty.xsd");

    FeatureMetacardType fmt =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    Set<AttributeDescriptor> descriptors = initDescriptors();

    for (AttributeDescriptor ad : descriptors) {
      assertBasicAttributeDescriptor(fmt, ad.getName(), ad.getType());
      assertThat(fmt.getAttributeDescriptor(ad.getName()).isStored(), is(false));
    }
  }

  @Test
  public void testElementTypeOfFeatureTypeIsSimpleType() throws Exception {
    loadSchema("elementTypeOfFeatureTypeIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsBaseType() throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsBaseType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsSimpleType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsInlineSimpleType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsInlineSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeListAndBaseIsBaseType() throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeListAndBaseIsBaseType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeListAndBaseIsSimpleType() throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeListAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeListAndBaseIsInlineSimpleType() throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeListAndBaseIsInlineSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsComplexTypeSimpleContentExtensionAndBaseIsBaseType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentExtensionAndBaseIsBaseType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsComplexTypeSimpleContentExtensionAndBaseIsSimpleType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentExtensionAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsBaseType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsBaseType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsSimpleType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testElementTypeOfFeatureTypeIsComplexType() throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("TheProperty")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("TheProperty")));
  }

  @Test
  public void testTopLevelSimpleType() throws Exception {
    loadSchema("simpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getProperties(), is(singletonList("FeatureTypeName")));
    assertThat(featureMetacardType.getTextualProperties(), is(singletonList("FeatureTypeName")));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExceptionThrownWhenSchemaIsNull() {
    new FeatureMetacardType(null, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
  }

  @Test
  public void testMetacardTypeNameIsLocalPartOfFeatureType() throws Exception {
    loadSchema("singleFloatProperty.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema,
            new QName("http://codice.org/test", "TheFeatureType"),
            EMPTY_NON_QUERYABLE_PROPS,
            GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getName(), is("TheFeatureType"));
  }

  @Test
  public void testGetNamespaceURI() throws Exception {
    loadSchema("singleFloatProperty.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema,
            new QName("http://codice.org/test", "TheFeatureType"),
            EMPTY_NON_QUERYABLE_PROPS,
            GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getNamespaceURI(), is("http://codice.org/test"));
  }

  @Test
  public void testGetFeatureType() throws Exception {
    loadSchema("singleFloatProperty.xsd");

    final QName featureTypeName = new QName("http://codice.org/test", "TheFeatureType");
    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, featureTypeName, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getFeatureType(), is(featureTypeName));
  }

  @Test
  public void testMetacardTypeEnhancer() {
    final AttributeDescriptor attributeDescriptor =
        new AttributeDescriptorImpl("foo", true, true, true, true, BasicTypes.STRING_TYPE);

    final MetacardTypeEnhancer metacardTypeEnhancer = mock(MetacardTypeEnhancer.class);
    doReturn(Collections.singleton(attributeDescriptor))
        .when(metacardTypeEnhancer)
        .getAttributeDescriptors();

    final QName featureTypeName = new QName("http://codice.org/test", "TheFeatureType");
    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            new XmlSchema(),
            featureTypeName,
            EMPTY_NON_QUERYABLE_PROPS,
            GML_3_1_1_NAMESPACE,
            metacardTypeEnhancer);

    assertThat(featureMetacardType.getAttributeDescriptor("foo"), is(attributeDescriptor));
  }

  private void assertBasicAttributeDescriptor(
      FeatureMetacardType featureMetacardType, String propertyName, AttributeType type) {
    AttributeDescriptor attrDesc = featureMetacardType.getAttributeDescriptor(propertyName);

    assertThat(attrDesc, notNullValue());
    assertThat(attrDesc.getType(), is(type));
  }

  private Set<AttributeDescriptor> initDescriptors() {
    return Stream.of(
            new CoreAttributes(),
            new ContactAttributes(),
            new LocationAttributes(),
            new MediaAttributes(),
            new DateTimeAttributes(),
            new ValidationAttributes(),
            new MediaAttributes())
        .map(MetacardType::getAttributeDescriptors)
        .flatMap(Set::stream)
        .collect(Collectors.toSet());
  }

  private void loadSchema(final String schemaFile) throws IOException {
    final XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
    try (final InputStream schemaStream =
        getClass().getClassLoader().getResourceAsStream(schemaFile)) {
      schema = schemaCollection.read(new StreamSource(schemaStream));
    }
  }
}
