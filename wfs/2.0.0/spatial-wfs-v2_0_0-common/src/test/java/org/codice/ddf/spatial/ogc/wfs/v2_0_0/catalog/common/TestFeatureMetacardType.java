/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.constants.Constants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.junit.Test;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.impl.BasicTypes;

public class TestFeatureMetacardType {

    private static final QName FEATURE_TYPE = new QName("", "FeatureTypeName");

    private static final String PROPERTY_PREFIX = FEATURE_TYPE.getLocalPart()
            + ".";

    private static final String GML = "GML";

    private static final String ELEMENT_NAME = "ELEMENT_NAME";

    private static final String ELEMENT_NAME_1 = ELEMENT_NAME + "1";

    private static final String ELEMENT_NAME_2 = ELEMENT_NAME + "2";

    private static final String ELEMENT_NAME_3 = ELEMENT_NAME + "3";

    private static final List<String> NON_QUERYABLE_PROPS = Collections.emptyList();

    @Test
    public void testfeatureMetacardTypeFindSingleGmlProperty() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
        gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
        gmlElement.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
        gmlElement.setName(ELEMENT_NAME_1);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);

        assertTrue(featureMetacardType.getGmlProperties().size() == 1);

        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);

    }

    @Test
    public void testfeatureMetacardTypeMultipleGmlProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
        gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
        gmlElement.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
        gmlElement.setName(ELEMENT_NAME_1);

        XmlSchemaElement gmlElement2 = new XmlSchemaElement(schema, true);
        gmlElement2.setSchemaType(new XmlSchemaComplexType(schema, false));
        gmlElement2.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
        gmlElement2.setName(ELEMENT_NAME_2);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertTrue(featureMetacardType.getGmlProperties().size() == 2);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.GEO_TYPE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);

    }

    @Test
    public void testFeatureMetacardTypeNonQueryGmlProperty() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
        gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
        gmlElement.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
        gmlElement.setName(ELEMENT_NAME_1);

        XmlSchemaElement gmlElement2 = new XmlSchemaElement(schema, true);
        gmlElement2.setSchemaType(new XmlSchemaComplexType(schema, false));
        gmlElement2.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
        gmlElement2.setName(ELEMENT_NAME_2);

        List<String> nonQueryProps = new ArrayList<String>();
        nonQueryProps.add(ELEMENT_NAME_2);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                nonQueryProps, Wfs20Constants.GML_3_2_NAMESPACE);
        assertTrue(featureMetacardType.getGmlProperties().size() == 2);
        AttributeDescriptor attrDesc = featureMetacardType
                .getAttributeDescriptor(prefix(ELEMENT_NAME_1));
        assertNotNull(attrDesc);
        assertTrue(attrDesc.getType() == BasicTypes.GEO_TYPE);
        assertTrue(attrDesc.isIndexed());

        AttributeDescriptor attrDesc2 = featureMetacardType
                .getAttributeDescriptor(prefix(ELEMENT_NAME_2));
        assertNotNull(attrDesc2);
        assertTrue(attrDesc2.getType() == BasicTypes.GEO_TYPE);
        assertFalse(attrDesc2.isIndexed());

    }

    @Test
    public void testFeatureMetacardTypeSingleStringProperty() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement stringElement = new XmlSchemaElement(schema, true);
        stringElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        stringElement.setSchemaTypeName(Constants.XSD_STRING);
        stringElement.setName(ELEMENT_NAME_1);
        schema.getElements().put(new QName(ELEMENT_NAME_1), stringElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertTrue(featureMetacardType.getTextualProperties().size() == 1);

        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
    }

    @Test
    public void testFeatureMetacardTypeMultipleStringProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement stringElement = new XmlSchemaElement(schema, true);
        stringElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        stringElement.setSchemaTypeName(Constants.XSD_STRING);
        stringElement.setName(ELEMENT_NAME_1);

        XmlSchemaElement stringElement2 = new XmlSchemaElement(schema, true);
        stringElement2.setSchemaType(new XmlSchemaSimpleType(schema, false));
        stringElement2.setSchemaTypeName(Constants.XSD_STRING);
        stringElement2.setName(ELEMENT_NAME_2);

        schema.getElements().put(new QName(ELEMENT_NAME_1), stringElement);
        schema.getElements().put(new QName(ELEMENT_NAME_2), stringElement2);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertTrue(featureMetacardType.getTextualProperties().size() == 2);

        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.STRING_TYPE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);

    }

    @Test
    public void testFeatureMetacardTypeStringNonQueryProperty() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement stringElement = new XmlSchemaElement(schema, true);
        stringElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        stringElement.setSchemaTypeName(Constants.XSD_STRING);
        stringElement.setName(ELEMENT_NAME_1);

        XmlSchemaElement stringElement2 = new XmlSchemaElement(schema, true);
        stringElement2.setSchemaType(new XmlSchemaSimpleType(schema, false));
        stringElement2.setSchemaTypeName(Constants.XSD_STRING);
        stringElement2.setName(ELEMENT_NAME_2);

        List<String> nonQueryProps = new ArrayList<String>();
        nonQueryProps.add(ELEMENT_NAME_1);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                nonQueryProps, Wfs20Constants.GML_3_2_NAMESPACE);
        assertTrue(featureMetacardType.getTextualProperties().size() == 2);
        AttributeDescriptor attrDesc = featureMetacardType
                .getAttributeDescriptor(prefix(ELEMENT_NAME_1));
        assertNotNull(attrDesc);
        assertFalse(attrDesc.isIndexed());

        AttributeDescriptor attrDesc2 = featureMetacardType
                .getAttributeDescriptor(prefix(ELEMENT_NAME_2));
        assertNotNull(attrDesc2);
        assertTrue(attrDesc2.isIndexed());

    }

    @Test
    public void testFeatureMetacardTypeStringAndGmlProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement stringElement = new XmlSchemaElement(schema, true);
        stringElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        stringElement.setSchemaTypeName(Constants.XSD_STRING);
        stringElement.setName(ELEMENT_NAME_1);
        XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
        gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
        gmlElement.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
        gmlElement.setName(ELEMENT_NAME_2);
        schema.getElements().put(new QName(ELEMENT_NAME_1), stringElement);
        schema.getElements().put(new QName(ELEMENT_NAME_2), gmlElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertTrue(featureMetacardType.getTextualProperties().size() == 1);
        assertTrue(featureMetacardType.getGmlProperties().size() == 1);

        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.GEO_TYPE);

    }

    @Test
    public void testFeatureMetacardTypeComplexContentWithStringAndGmlProperties() {
        XmlSchema schema = new XmlSchema();
        // Create the GML and String types
        XmlSchemaElement gmlElement = new XmlSchemaElement(schema, true);
        gmlElement.setSchemaType(new XmlSchemaComplexType(schema, false));
        gmlElement.setSchemaTypeName(new QName(Wfs20Constants.GML_3_2_NAMESPACE, GML));
        gmlElement.setName(ELEMENT_NAME_1);

        XmlSchemaElement stringElement = new XmlSchemaElement(schema, true);
        stringElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        stringElement.setSchemaTypeName(Constants.XSD_STRING);
        stringElement.setName(ELEMENT_NAME_2);

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

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertTrue(featureMetacardType.getTextualProperties().size() == 1);
        assertTrue(featureMetacardType.getGmlProperties().size() == 1);

        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.STRING_TYPE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.GEO_TYPE);
    }

    @Test
    public void testfeatureMetacardTypeFindDateProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement dateElement = new XmlSchemaElement(schema, true);
        dateElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        dateElement.setSchemaTypeName(Constants.XSD_DATE);
        dateElement.setName(ELEMENT_NAME_1);
        XmlSchemaElement dateTimeElement = new XmlSchemaElement(schema, true);
        dateTimeElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        dateTimeElement.setSchemaTypeName(Constants.XSD_DATETIME);
        dateTimeElement.setName(ELEMENT_NAME_2);

        schema.getElements().put(new QName(ELEMENT_NAME_1), dateElement);
        schema.getElements().put(new QName(ELEMENT_NAME_2), dateTimeElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.DATE_TYPE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.DATE_TYPE);

    }

    @Test
    public void testfeatureMetacardTypeFindBooleanProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement booleanElement = new XmlSchemaElement(schema, true);
        booleanElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        booleanElement.setSchemaTypeName(Constants.XSD_BOOLEAN);
        booleanElement.setName(ELEMENT_NAME_1);

        schema.getElements().put(new QName(ELEMENT_NAME_1), booleanElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.BOOLEAN_TYPE);

    }

    @Test
    public void testfeatureMetacardTypeFindDoubleProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement doubleElement = new XmlSchemaElement(schema, true);
        doubleElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        doubleElement.setSchemaTypeName(Constants.XSD_DOUBLE);
        doubleElement.setName(ELEMENT_NAME_1);

        schema.getElements().put(new QName(ELEMENT_NAME_1), doubleElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.DOUBLE_TYPE);

    }

    @Test
    public void testfeatureMetacardTypeFindFloatProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement floatElement = new XmlSchemaElement(schema, true);
        floatElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        floatElement.setSchemaTypeName(Constants.XSD_FLOAT);
        floatElement.setName(ELEMENT_NAME_1);

        schema.getElements().put(new QName(ELEMENT_NAME_1), floatElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.FLOAT_TYPE);

    }

    @Test
    public void testfeatureMetacardTypeFindIntegerProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement integerElement = new XmlSchemaElement(schema, true);
        integerElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        integerElement.setSchemaTypeName(Constants.XSD_INTEGER);
        integerElement.setName(ELEMENT_NAME_1);
        XmlSchemaElement intElement = new XmlSchemaElement(schema, true);
        intElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        intElement.setSchemaTypeName(Constants.XSD_INT);
        intElement.setName(ELEMENT_NAME_2);
        XmlSchemaElement positivieIntegerElement = new XmlSchemaElement(schema, true);
        positivieIntegerElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        positivieIntegerElement.setSchemaTypeName(Constants.XSD_POSITIVEINTEGER);
        positivieIntegerElement.setName(ELEMENT_NAME_3);

        schema.getElements().put(new QName(ELEMENT_NAME_1), integerElement);
        schema.getElements().put(new QName(ELEMENT_NAME_2), intElement);
        schema.getElements().put(new QName(ELEMENT_NAME_3), positivieIntegerElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.STRING_TYPE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_2, BasicTypes.INTEGER_TYPE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_3, BasicTypes.STRING_TYPE);

    }

    @Test
    public void testfeatureMetacardTypeFindLongProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement longElement = new XmlSchemaElement(schema, true);
        longElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        longElement.setSchemaTypeName(Constants.XSD_LONG);
        longElement.setName(ELEMENT_NAME_1);

        schema.getElements().put(new QName(ELEMENT_NAME_1), longElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.LONG_TYPE);

    }

    @Test
    public void testfeatureMetacardTypeFindShortProperties() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement shortElement = new XmlSchemaElement(schema, true);
        shortElement.setSchemaType(new XmlSchemaSimpleType(schema, false));
        shortElement.setSchemaTypeName(Constants.XSD_SHORT);
        shortElement.setName(ELEMENT_NAME_1);

        schema.getElements().put(new QName(ELEMENT_NAME_1), shortElement);

        FeatureMetacardType featureMetacardType = new FeatureMetacardType(schema, FEATURE_TYPE,
                NON_QUERYABLE_PROPS, Wfs20Constants.GML_3_2_NAMESPACE);
        assertAttributeDescriptor(featureMetacardType, ELEMENT_NAME_1, BasicTypes.SHORT_TYPE);

    }

    @Test
    public void testFeatureMetacardTypeFindBasicMetacardAttributes() {
        XmlSchema schema = new XmlSchema();
        XmlSchemaElement element = new XmlSchemaElement(schema, true);
        element.setSchemaType(new XmlSchemaSimpleType(schema, false));
        element.setSchemaTypeName(Constants.XSD_STRING);
        element.setName(ELEMENT_NAME);

        schema.getElements().put(new QName(ELEMENT_NAME), element);
        FeatureMetacardType fmt = new FeatureMetacardType(schema, FEATURE_TYPE, NON_QUERYABLE_PROPS,
        		Wfs20Constants.GML_3_2_NAMESPACE);

        for (AttributeDescriptor ad : BasicTypes.BASIC_METACARD.getAttributeDescriptors()) {
            assertBasicAttributeDescriptor(fmt, ad.getName(), ad.getType());
            assertFalse(fmt.getAttributeDescriptor(ad.getName()).isStored());
        }

    }

    public String prefix(String element) {
        return PROPERTY_PREFIX + element;
    }

    private void assertAttributeDescriptor(FeatureMetacardType featureMetacardType,
            String propertyName, AttributeType type) {
        assertBasicAttributeDescriptor(featureMetacardType, prefix(propertyName), type);
    }

    private void assertBasicAttributeDescriptor(FeatureMetacardType featureMetacardType,
            String propertyName, AttributeType type) {
        AttributeDescriptor attrDesc = featureMetacardType.getAttributeDescriptor(propertyName);

        assertNotNull(attrDesc);
        assertTrue(attrDesc.getType() == type);
    }

}
