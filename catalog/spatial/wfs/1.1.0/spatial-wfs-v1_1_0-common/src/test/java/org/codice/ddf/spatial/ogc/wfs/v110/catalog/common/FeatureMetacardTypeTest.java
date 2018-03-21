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
package org.codice.ddf.spatial.ogc.wfs.v110.catalog.common;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.junit.Before;
import org.junit.Test;

public class FeatureMetacardTypeTest {

  private static final QName FEATURE_TYPE = new QName("", "FeatureTypeName");

  private static final String PROPERTY_PREFIX = FEATURE_TYPE.getLocalPart() + ".";

  private static final String GML = "GML";

  private static final String ELEMENT_NAME = "ELEMENT_NAME";

  private static final String ELEMENT_NAME_1 = ELEMENT_NAME + "1";

  private static final String ELEMENT_NAME_2 = ELEMENT_NAME + "2";

  private static final String ELEMENT_NAME_3 = ELEMENT_NAME + "3";

  private static final String EXT_PREFIX = "ext.";

  private static final List<String> EMPTY_NON_QUERYABLE_PROPS = Collections.emptyList();

  private XmlSchema schema;

  @Before
  public void setup() {
    schema = new XmlSchema();
  }

  @Test
  public void testFeatureMetacardTypeFindSingleGmlProperty() {
    createGmlElement(ELEMENT_NAME_1);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);

    assertThat(featureMetacardType.getGmlProperties(), hasSize(1));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeMultipleGmlProperties() {
    createGmlElement(ELEMENT_NAME_1);
    createGmlElement(ELEMENT_NAME_2);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getGmlProperties(), hasSize(2));
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.GEO_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeNonQueryGmlProperty() {
    createGmlElement(ELEMENT_NAME_1);
    createGmlElement(ELEMENT_NAME_2);

    List<String> nonQueryProps = new ArrayList<>();
    nonQueryProps.add(ELEMENT_NAME_2);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, nonQueryProps, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getGmlProperties(), hasSize(2));
    AttributeDescriptor attrDesc =
        featureMetacardType.getAttributeDescriptor(prefix(ELEMENT_NAME_1));
    assertThat(attrDesc, notNullValue());
    assertThat(attrDesc.getType(), is(BasicTypes.GEO_TYPE));
    assertThat(attrDesc.isIndexed(), is(true));

    AttributeDescriptor attrDesc2 =
        featureMetacardType.getAttributeDescriptor(prefix(ELEMENT_NAME_2));
    assertThat(attrDesc2, notNullValue());
    assertThat(attrDesc2.getType(), is(BasicTypes.GEO_TYPE));
    assertThat(attrDesc2.isIndexed(), is(false));
  }

  @Test
  public void testFeatureMetacardTypeSingleStringProperty() {
    createStringGmlElement(ELEMENT_NAME_1);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getTextualProperties(), hasSize(1));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeMultipleStringProperties() {
    createStringGmlElement(ELEMENT_NAME_1);
    createStringGmlElement(ELEMENT_NAME_2);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getTextualProperties(), hasSize(2));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.STRING_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeStringNonQueryProperty() {
    createStringGmlElement(ELEMENT_NAME_1);
    createStringGmlElement(ELEMENT_NAME_2);

    List<String> nonQueryProps = new ArrayList<>();
    nonQueryProps.add(ELEMENT_NAME_1);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, nonQueryProps, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getTextualProperties(), hasSize(2));
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
  public void testFeatureMetacardTypeStringAndGmlProperties() {
    createStringGmlElement(ELEMENT_NAME_1);
    createGmlElement(ELEMENT_NAME_2);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getTextualProperties(), hasSize(1));
    assertThat(featureMetacardType.getGmlProperties(), hasSize(1));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.GEO_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeComplexContentWithStringAndGmlProperties() {
    // Create the GML and String types
    createGmlElement(ELEMENT_NAME_1);

    XmlSchemaElement stringElement = createStringGmlElement(ELEMENT_NAME_2);

    // build the complex objects
    XmlSchemaElement complexElement = new XmlSchemaElement(schema, true);
    XmlSchemaComplexType complexType = new XmlSchemaComplexType(schema, false);
    XmlSchemaComplexContent complexContent = new XmlSchemaComplexContent();
    XmlSchemaComplexContentExtension contentExtension = new XmlSchemaComplexContentExtension();
    XmlSchemaSequence particle = new XmlSchemaSequence();
    particle.getItems().add(stringElement);
    contentExtension.setParticle(particle);
    complexContent.setContent(contentExtension);
    complexType.setContentModel(complexContent);
    complexElement.setSchemaType(complexType);
    complexElement.setSchemaTypeName(new QName("Complex"));

    schema.getElements().put(new QName(ELEMENT_NAME_2), complexElement);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertThat(featureMetacardType.getTextualProperties(), hasSize(1));
    assertThat(featureMetacardType.getGmlProperties(), hasSize(1));

    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.STRING_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindDateProperties() {
    createElement(ELEMENT_NAME_1, new XmlSchemaSimpleType(schema, false), Constants.XSD_DATE);
    createElement(ELEMENT_NAME_2, new XmlSchemaSimpleType(schema, false), Constants.XSD_DATETIME);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.DATE_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.DATE_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindBooleanProperties() {
    createElement(ELEMENT_NAME_1, new XmlSchemaSimpleType(schema, false), Constants.XSD_BOOLEAN);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.BOOLEAN_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindDoubleProperties() {
    createElement(ELEMENT_NAME_1, new XmlSchemaSimpleType(schema, false), Constants.XSD_DOUBLE);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.DOUBLE_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindFloatProperties() {
    createElement(ELEMENT_NAME_1, new XmlSchemaSimpleType(schema, false), Constants.XSD_FLOAT);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.FLOAT_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindIntegerProperties() {
    createElement(ELEMENT_NAME_1, new XmlSchemaSimpleType(schema, false), Constants.XSD_INTEGER);
    createElement(ELEMENT_NAME_2, new XmlSchemaSimpleType(schema, false), Constants.XSD_INT);
    createElement(
        ELEMENT_NAME_3, new XmlSchemaSimpleType(schema, false), Constants.XSD_POSITIVEINTEGER);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.INTEGER_TYPE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_3, BasicTypes.STRING_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindLongProperties() {
    createElement(ELEMENT_NAME_1, new XmlSchemaSimpleType(schema, false), Constants.XSD_LONG);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.LONG_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindShortProperties() {
    createElement(ELEMENT_NAME_1, new XmlSchemaSimpleType(schema, false), Constants.XSD_SHORT);

    FeatureMetacardType featureMetacardType =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);
    assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.SHORT_TYPE);
  }

  @Test
  public void testFeatureMetacardTypeFindTaxonomyMetacardAttributes() {
    createStringGmlElement(ELEMENT_NAME);

    FeatureMetacardType fmt =
        new FeatureMetacardType(
            schema, FEATURE_TYPE, EMPTY_NON_QUERYABLE_PROPS, Wfs11Constants.GML_3_1_1_NAMESPACE);

    Set<AttributeDescriptor> descriptors = initDescriptors();

    for (AttributeDescriptor ad : descriptors) {
      assertBasicAttributeDescriptor(fmt, ad.getName(), ad.getType());
      assertThat(fmt.getAttributeDescriptor(ad.getName()).isStored(), is(false));
    }

    // +1 to account for one element added to schema.
    assertThat(fmt.getAttributeDescriptors().size(), is(descriptors.size() + 1));
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

  private XmlSchemaElement createElement(
      String name, XmlSchemaType schemaType, QName schemaTypeName) {
    XmlSchemaElement stringElement = new XmlSchemaElement(schema, true);
    stringElement.setSchemaType(schemaType);
    stringElement.setSchemaTypeName(schemaTypeName);
    stringElement.setName(name);
    schema.getElements().put(new QName(name), stringElement);
    return stringElement;
  }

  private XmlSchemaElement createStringGmlElement(String name) {
    return createElement(name, new XmlSchemaSimpleType(schema, false), Constants.XSD_STRING);
  }

  private void createGmlElement(String name) {
    createElement(
        name,
        new XmlSchemaComplexType(schema, false),
        new QName(Wfs11Constants.GML_3_1_1_NAMESPACE, GML));
  }
}
