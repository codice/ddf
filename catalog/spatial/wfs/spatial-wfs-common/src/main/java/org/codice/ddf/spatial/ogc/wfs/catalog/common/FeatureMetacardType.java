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

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.codice.ddf.spatial.ogc.wfs.catalog.MetacardTypeEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureMetacardType extends MetacardTypeImpl {

  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMetacardType.class);

  private transient List<String> properties = new ArrayList<>();

  private transient List<String> textualProperties = new ArrayList<>();

  private transient List<String> gmlProperties = new ArrayList<>();

  private transient List<String> temporalProperties = new ArrayList<>();

  private transient QName featureType;

  private transient String propertyPrefix;

  private transient List<String> nonQueryableProperties;

  private transient String gmlNamespace;

  private static final String EXT_PREFIX = "ext.";

  public static final MetacardTypeEnhancer DEFAULT_METACARD_TYPE_ENHANCER =
      new MetacardTypeEnhancer() {
        @Override
        public String getFeatureName() {
          return "";
        }

        @Override
        public Set<AttributeDescriptor> getAttributeDescriptors() {
          return Collections.emptySet();
        }
      };

  public FeatureMetacardType(
      XmlSchema schema,
      final QName featureType,
      List<String> nonQueryableProperties,
      String gmlNamespace) {
    this(schema, featureType, nonQueryableProperties, gmlNamespace, DEFAULT_METACARD_TYPE_ENHANCER);
  }

  public FeatureMetacardType(
      XmlSchema schema,
      final QName featureType,
      List<String> nonQueryableProperties,
      String gmlNamespace,
      MetacardTypeEnhancer metacardTypeEnhancer) {
    super(featureType.getLocalPart(), (Set<AttributeDescriptor>) null);

    addAllDescriptors();

    this.featureType = featureType;
    this.nonQueryableProperties = nonQueryableProperties;
    this.propertyPrefix = EXT_PREFIX + getName() + ".";
    this.gmlNamespace = gmlNamespace;
    if (schema != null) {
      processXmlSchema(schema);
    } else {
      throw new IllegalArgumentException(
          "FeatureTypeMetacard cannot be created with a null Schema.");
    }

    Set<String> existingAttributeNames =
        getAttributeDescriptors()
            .stream()
            .map(AttributeDescriptor::getName)
            .collect(Collectors.toSet());

    metacardTypeEnhancer
        .getAttributeDescriptors()
        .stream()
        .filter(
            attributeDescriptor -> !existingAttributeNames.contains(attributeDescriptor.getName()))
        .forEach(this::add);
  }

  /**
   * we don't want to expose these in a query interface ie wfs endpoint, so we need to create new
   * attributes for each and set them to stored = false note: indexed is being used to determine
   * whether or not to query certain wfs fields so it did not seem appropriate to hide those fields
   * from the endpoint schema
   */
  private void addDescriptors(Set<AttributeDescriptor> attrDescriptors) {
    for (AttributeDescriptor descriptor : attrDescriptors) {
      AttributeDescriptorImpl basicAttributeDescriptor = (AttributeDescriptorImpl) descriptor;
      AttributeDescriptor attributeDescriptor =
          new AttributeDescriptorImpl(
              basicAttributeDescriptor.getName(),
              false,
              false,
              basicAttributeDescriptor.isTokenized(),
              basicAttributeDescriptor.isMultiValued(),
              basicAttributeDescriptor.getType());
      add(attributeDescriptor);
    }
  }

  private void addAllDescriptors() {
    addDescriptors(new CoreAttributes().getAttributeDescriptors());
    addDescriptors(new ContactAttributes().getAttributeDescriptors());
    addDescriptors(new LocationAttributes().getAttributeDescriptors());
    addDescriptors(new MediaAttributes().getAttributeDescriptors());
    addDescriptors(new DateTimeAttributes().getAttributeDescriptors());
    addDescriptors(new ValidationAttributes().getAttributeDescriptors());
    addDescriptors(new MediaAttributes().getAttributeDescriptors());
  }

  public String getName() {
    return featureType.getLocalPart();
  }

  public String getPrefix() {
    return featureType.getPrefix();
  }

  public String getNamespaceURI() {
    return featureType.getNamespaceURI();
  }

  public QName getFeatureType() {
    return featureType;
  }

  private void processXmlSchema(XmlSchema schema) {
    Map<QName, XmlSchemaElement> elements = schema.getElements();
    Iterator<XmlSchemaElement> xmlSchemaElementIterator = elements.values().iterator();

    while (xmlSchemaElementIterator.hasNext()) {
      Object o = xmlSchemaElementIterator.next();
      XmlSchemaElement element = (XmlSchemaElement) o;
      XmlSchemaType schemaType = element.getSchemaType();
      if (schemaType instanceof XmlSchemaComplexType) {
        processComplexType(element);
      } else if (schemaType instanceof XmlSchemaSimpleType) {
        processSimpleType(element);
      }
    }
  }

  private void processComplexType(XmlSchemaElement xmlSchemaElement) {
    if (!processGmlType(xmlSchemaElement)) {
      XmlSchemaType schemaType = xmlSchemaElement.getSchemaType();
      if ((schemaType != null) && (schemaType instanceof XmlSchemaComplexType)) {
        XmlSchemaComplexType complexType = (XmlSchemaComplexType) schemaType;
        if (complexType.getParticle() != null) {
          processXmlSchemaParticle(complexType.getParticle());
        } else {
          if (complexType.getContentModel() instanceof XmlSchemaComplexContent) {
            XmlSchemaContent content = complexType.getContentModel().getContent();
            if (content instanceof XmlSchemaComplexContentExtension) {
              XmlSchemaComplexContentExtension extension =
                  (XmlSchemaComplexContentExtension) content;
              processXmlSchemaParticle(extension.getParticle());
            }
          }
        }
      }
    }
  }

  private void processXmlSchemaParticle(XmlSchemaParticle particle) {
    XmlSchemaSequence schemaSequence;
    if (particle instanceof XmlSchemaSequence) {
      schemaSequence = (XmlSchemaSequence) particle;
      List<XmlSchemaSequenceMember> schemaObjectCollection = schemaSequence.getItems();
      Iterator<XmlSchemaSequenceMember> iterator = schemaObjectCollection.iterator();
      while (iterator.hasNext()) {
        Object element = iterator.next();
        if (element instanceof XmlSchemaElement) {
          XmlSchemaElement innerElement = ((XmlSchemaElement) element);
          XmlSchemaType innerEleType = innerElement.getSchemaType();
          if (innerEleType instanceof XmlSchemaComplexType) {
            processComplexType(innerElement);
          } else if (innerEleType instanceof XmlSchemaSimpleType) {
            processSimpleType(innerElement);
          } else if (innerEleType == null) {
            // Check if this is the GML location Property
            processGmlType(innerElement);
          }
        }
      }
    }
  }

  private void processSimpleType(XmlSchemaElement xmlSchemaElement) {
    QName qName = xmlSchemaElement.getSchemaTypeName();
    String name = xmlSchemaElement.getName();
    AttributeType<?> attributeType = toBasicType(qName);

    if (attributeType != null) {
      boolean multiValued = xmlSchemaElement.getMaxOccurs() > 1;
      add(
          new FeatureAttributeDescriptor(
              propertyPrefix + name,
              name,
              isQueryable(name) /* indexed */,
              true /* stored */,
              false /* tokenized */,
              multiValued,
              attributeType));
    }
    if (Constants.XSD_STRING.equals(qName)) {
      textualProperties.add(propertyPrefix + name);
    }

    properties.add(propertyPrefix + name);
  }

  private Boolean processGmlType(XmlSchemaElement xmlSchemaElement) {
    QName qName = xmlSchemaElement.getSchemaTypeName();
    String name = xmlSchemaElement.getName();
    String propertyPrefixWithName = propertyPrefix + name;

    if (qName != null
        && StringUtils.isNotEmpty(name)
        && qName.getNamespaceURI().equals(gmlNamespace)
        && (qName.getLocalPart().equals("TimeInstantType")
            || qName.getLocalPart().equals("TimePeriodType"))) {
      LOGGER.debug("Adding temporal property: {}{}", propertyPrefix, name);
      temporalProperties.add(propertyPrefix + name);

      boolean multiValued = xmlSchemaElement.getMaxOccurs() > 1;
      add(
          new FeatureAttributeDescriptor(
              propertyPrefixWithName,
              name,
              isQueryable(name) /* indexed */,
              true /* stored */,
              false /* tokenized */,
              multiValued,
              BasicTypes.DATE_TYPE));

      properties.add(propertyPrefixWithName);

      return true;
    }

    if ((qName != null)
        && qName.getNamespaceURI().equals(gmlNamespace)
        && (!StringUtils.isEmpty(name))) {

      LOGGER.debug("Adding geo property: {}", propertyPrefixWithName);
      gmlProperties.add(propertyPrefixWithName);

      boolean multiValued = xmlSchemaElement.getMaxOccurs() > 1;
      add(
          new FeatureAttributeDescriptor(
              propertyPrefixWithName,
              name,
              isQueryable(name) /* indexed */,
              true /* stored */,
              false /* tokenized */,
              multiValued,
              BasicTypes.GEO_TYPE));

      properties.add(propertyPrefixWithName);

      return true;
    }

    return false;
  }

  private AttributeType<?> toBasicType(QName qName) {

    if (Constants.XSD_STRING.equals(qName)) {
      return BasicTypes.STRING_TYPE;
    }
    if (Constants.XSD_DATETIME.equals(qName) || Constants.XSD_DATE.equals(qName)) {
      return BasicTypes.DATE_TYPE;
    }
    if (Constants.XSD_BOOLEAN.equals(qName)) {
      return BasicTypes.BOOLEAN_TYPE;
    }
    if (Constants.XSD_DOUBLE.equals(qName)) {
      return BasicTypes.DOUBLE_TYPE;
    }
    if (Constants.XSD_FLOAT.equals(qName)) {
      return BasicTypes.FLOAT_TYPE;
    }
    if (Constants.XSD_INT.equals(qName)) {
      return BasicTypes.INTEGER_TYPE;
    }
    if (Constants.XSD_LONG.equals(qName)) {
      return BasicTypes.LONG_TYPE;
    }
    if (Constants.XSD_SHORT.equals(qName)) {
      return BasicTypes.SHORT_TYPE;
    }

    // these types are unbounded and unsafe to map to any BasicTypes number values.
    // Potentially the catalog should support a BigInteger type for these types to map to
    if (Constants.XSD_INTEGER.equals(qName)
        || Constants.XSD_POSITIVEINTEGER.equals(qName)
        || Constants.XSD_NEGATIVEINTEGER.equals(qName)
        || Constants.XSD_NONPOSITIVEINTEGER.equals(qName)
        || Constants.XSD_NONNEGATIVEINTEGER.equals(qName)) {
      return BasicTypes.STRING_TYPE;
    }
    return null;
  }

  private boolean isQueryable(String propertyName) {
    return !nonQueryableProperties.contains(propertyName);
  }

  public List<String> getTextualProperties() {
    return textualProperties;
  }

  public List<String> getGmlProperties() {
    return gmlProperties;
  }

  public List<String> getProperties() {
    return properties;
  }

  public List<String> getTemporalProperties() {
    return temporalProperties;
  }
}
