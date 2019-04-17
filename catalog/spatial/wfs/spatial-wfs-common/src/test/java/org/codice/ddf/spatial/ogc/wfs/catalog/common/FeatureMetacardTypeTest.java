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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.junit.Test;

public class FeatureMetacardTypeTest {

  private static final QName FEATURE_TYPE = new QName("", "FeatureTypeName");

  private static final String PROPERTY_PREFIX = FEATURE_TYPE.getLocalPart() + ".";

  private static final String GML_3_1_1_NAMESPACE = "http://www.opengis.net/gml";

  private static final String ELEMENT_NAME = "ELEMENT_NAME";

  private static final String ELEMENT_NAME_1 = ELEMENT_NAME + "1";

  private static final String ELEMENT_NAME_2 = ELEMENT_NAME + "2";

  private static final String ELEMENT_NAME_3 = ELEMENT_NAME + "3";

  private static final String EXT_PREFIX = "ext.";

  private static final List<String> EMPTY_NON_QUERYABLE_PROPS = Collections.emptyList();

  private XmlSchema schema;

  @Test
  public void testFeatureMetacardTypeFindSingleGmlGeospatialProperty() throws Exception {
    loadSchema("singleGmlGeospatialProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getGmlProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));
    assertThat(
        featureMetacardType.getProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindSingleGmlTemporalProperty() throws Exception {
    loadSchema("singleGmlTemporalProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getTemporalProperties(), hasSize(1));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.DATE_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeMultipleGmlProperties() throws Exception {
    loadSchema("multipleGmlProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getGmlProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));
    assertThat(
        featureMetacardType.getTemporalProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME2")));
    assertThat(
        featureMetacardType.getProperties(),
        containsInAnyOrder(
            "ext.FeatureTypeName.ELEMENT_NAME1", "ext.FeatureTypeName.ELEMENT_NAME2"));
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.DATE_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeNonQueryGmlProperty() throws Exception {
    loadSchema("multipleGmlProperties.xsd");

    List<String> nonQueryProps = new ArrayList<>();
    nonQueryProps.add(ELEMENT_NAME_2);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(schema, FEATURE_TYPE, nonQueryProps, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getGmlProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));
    AttributeDescriptor attrDesc =
        featureMetacardType.getAttributeDescriptor(prefix(ELEMENT_NAME_1));
    assertThat(attrDesc, notNullValue());
    assertThat(attrDesc.getType(), is(BasicTypes.GEO_TYPE));
    assertThat(attrDesc.isIndexed(), is(true));

    assertThat(
        featureMetacardType.getTemporalProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME2")));
    AttributeDescriptor attrDesc2 =
        featureMetacardType.getAttributeDescriptor(prefix(ELEMENT_NAME_2));
    assertThat(attrDesc2, notNullValue());
    assertThat(attrDesc2.getType(), is(BasicTypes.DATE_TYPE));
    assertThat(attrDesc2.isIndexed(), is(false));
  }

  @Test
  public void testFeatureMetacardTypeSingleStringProperty() throws Exception {
    loadSchema("singleStringProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));
    assertThat(
        featureMetacardType.getProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeMultipleStringProperties() throws Exception {
    loadSchema("multipleStringProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getTextualProperties(),
        containsInAnyOrder(
            "ext.FeatureTypeName.ELEMENT_NAME1", "ext.FeatureTypeName.ELEMENT_NAME2"));
    assertThat(
        featureMetacardType.getProperties(),
        containsInAnyOrder(
            "ext.FeatureTypeName.ELEMENT_NAME1", "ext.FeatureTypeName.ELEMENT_NAME2"));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.STRING_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeStringNonQueryProperty() throws Exception {
    loadSchema("multipleStringProperties.xsd");

    List<String> nonQueryProps = new ArrayList<>();
    nonQueryProps.add(ELEMENT_NAME_1);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(schema, FEATURE_TYPE, nonQueryProps, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getTextualProperties(),
        containsInAnyOrder(
            "ext.FeatureTypeName.ELEMENT_NAME1", "ext.FeatureTypeName.ELEMENT_NAME2"));
    AttributeDescriptor attrDesc =
        featureMetacardType.getAttributeDescriptor(prefix(ELEMENT_NAME_1));
    assertThat(attrDesc, notNullValue());
    assertThat(attrDesc.isIndexed(), is(false));

    AttributeDescriptor attrDesc2 =
        featureMetacardType.getAttributeDescriptor(prefix(ELEMENT_NAME_2));
    assertThat(attrDesc2, notNullValue());
    assertThat(attrDesc2.isIndexed(), is(true));
  }

  @Test
  public void testFeatureMetacardTypeStringAndGmlProperties() throws Exception {
    loadSchema("stringAndGmlProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));
    assertThat(
        featureMetacardType.getGmlProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME2")));
    assertThat(
        featureMetacardType.getProperties(),
        containsInAnyOrder(
            "ext.FeatureTypeName.ELEMENT_NAME1", "ext.FeatureTypeName.ELEMENT_NAME2"));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.GEO_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeComplexContentWithStringAndGmlProperties() throws Exception {
    loadSchema("stringAndGmlPropertiesComplexContent.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME2")));
    assertThat(
        featureMetacardType.getGmlProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));
    assertThat(
        featureMetacardType.getProperties(),
        containsInAnyOrder(
            "ext.FeatureTypeName.ELEMENT_NAME1", "ext.FeatureTypeName.ELEMENT_NAME2"));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.STRING_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindDateProperties() throws Exception {
    loadSchema("dateAndDateTimeProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(),
        containsInAnyOrder(
            "ext.FeatureTypeName.ELEMENT_NAME1", "ext.FeatureTypeName.ELEMENT_NAME2"));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.DATE_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.DATE_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindBooleanProperties() throws Exception {
    loadSchema("singleBooleanProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.BOOLEAN_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindDoubleProperties() throws Exception {
    loadSchema("singleDoubleProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.DOUBLE_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindFloatProperties() throws Exception {
    loadSchema("singleFloatProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.FLOAT_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindIntegerProperties() throws Exception {
    loadSchema("multipleIntegerProperties.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(),
        containsInAnyOrder(
            "ext.FeatureTypeName.ELEMENT_NAME1",
            "ext.FeatureTypeName.ELEMENT_NAME2",
            "ext.FeatureTypeName.ELEMENT_NAME3"));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.INTEGER_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_3, BasicTypes.STRING_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindLongProperties() throws Exception {
    loadSchema("singleLongProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.LONG_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindShortProperties() throws Exception {
    loadSchema("singleShortProperty.xsd");

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);

    assertThat(
        featureMetacardType.getProperties(),
        is(singletonList("ext.FeatureTypeName.ELEMENT_NAME1")));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.SHORT_TYPE);
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

    // +1 to account for one element added to schema.
    assertThat(fmt.getAttributeDescriptors(), hasSize(descriptors.size() + 1));
    assertThat(
        "The feature metacard type does not contain an attribute for the feature property.",
        fmt.getAttributeDescriptor("ext.FeatureTypeName.ELEMENT_NAME1"),
        is(notNullValue()));
  }

  @Test
  public void testElementTypeOfFeatureTypeIsSimpleType() throws Exception {
    loadSchema("elementTypeOfFeatureTypeIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsBaseType() throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsBaseType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsSimpleType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeRestrictionAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeListAndBaseIsBaseType() throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeListAndBaseIsBaseType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsSimpleTypeListAndBaseIsSimpleType() throws Exception {
    loadSchema("elementOfFeatureTypeIsSimpleTypeListAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsComplexTypeSimpleContentExtensionAndBaseIsBaseType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentExtensionAndBaseIsBaseType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsComplexTypeSimpleContentExtensionAndBaseIsSimpleType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentExtensionAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsBaseType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsBaseType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsSimpleType()
      throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  @Test
  public void testElementTypeOfFeatureTypeIsComplexType() throws Exception {
    loadSchema("elementOfFeatureTypeIsComplexTypeSimpleContentRestrictionAndBaseIsSimpleType.xsd");

    final FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, GML_3_1_1_NAMESPACE);
    assertThat(
        featureMetacardType.getProperties(), is(singletonList("ext.FeatureTypeName.TheProperty")));
    assertThat(
        featureMetacardType.getTextualProperties(),
        is(singletonList("ext.FeatureTypeName.TheProperty")));
  }

  private String prefix(String element) {
    return EXT_PREFIX + PROPERTY_PREFIX + element;
  }

  private void assertAttributeDescriptor(
      FeatureMetacardType featureMetacardType, String propertyName, AttributeType type) {
    assertBasicAttributeDescriptor(featureMetacardType, prefix(propertyName), type);
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
