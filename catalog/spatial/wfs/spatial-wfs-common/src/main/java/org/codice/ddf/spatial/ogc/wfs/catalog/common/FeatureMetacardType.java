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

import static java.util.stream.Collectors.toSet;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaContent;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaParticle;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentExtension;
import org.apache.ws.commons.schema.XmlSchemaSimpleContentRestriction;
import org.apache.ws.commons.schema.XmlSchemaSimpleType;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeContent;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeList;
import org.apache.ws.commons.schema.XmlSchemaSimpleTypeRestriction;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.codice.ddf.spatial.ogc.wfs.catalog.MetacardTypeEnhancer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureMetacardType extends MetacardTypeImpl {

  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureMetacardType.class);

  private final transient List<String> properties = new ArrayList<>();

  private final transient List<String> textualProperties = new ArrayList<>();

  private final transient List<String> gmlProperties = new ArrayList<>();

  private final transient List<String> temporalProperties = new ArrayList<>();

  private final transient QName featureType;

  private final transient Set<String> nonQueryableProperties;

  private final transient String gmlNamespace;

  private final transient XmlSchema schema;

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
      Set<String> nonQueryableProperties,
      String gmlNamespace) {
    this(schema, featureType, nonQueryableProperties, gmlNamespace, DEFAULT_METACARD_TYPE_ENHANCER);
  }

  public FeatureMetacardType(
      XmlSchema schema,
      final QName featureType,
      Set<String> nonQueryableProperties,
      String gmlNamespace,
      MetacardTypeEnhancer metacardTypeEnhancer) {
    super(featureType.getLocalPart(), (Set<AttributeDescriptor>) null);

    addAllDescriptors();

    this.schema = schema;
    this.featureType = featureType;
    this.nonQueryableProperties = nonQueryableProperties;
    this.gmlNamespace = gmlNamespace;
    if (schema != null) {
      processXmlSchema(schema);
    } else {
      throw new IllegalArgumentException(
          "FeatureTypeMetacard cannot be created with a null Schema.");
    }

    Set<String> existingAttributeNames =
        getAttributeDescriptors().stream().map(AttributeDescriptor::getName).collect(toSet());

    metacardTypeEnhancer.getAttributeDescriptors().stream()
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

  @Override
  public String getName() {
    return featureType.getLocalPart();
  }

  public String getNamespaceURI() {
    return featureType.getNamespaceURI();
  }

  public QName getFeatureType() {
    return featureType;
  }

  private void processXmlSchema(XmlSchema schema) {
    Map<QName, XmlSchemaElement> elements = schema.getElements();

    for (final XmlSchemaElement element : elements.values()) {
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
      if (schemaType instanceof XmlSchemaComplexType) {
        XmlSchemaComplexType complexType = (XmlSchemaComplexType) schemaType;
        if (complexType.getParticle() != null) {
          processXmlSchemaParticle(complexType.getParticle());
        } else if (complexType.getContentModel() != null) {
          XmlSchemaContent content = complexType.getContentModel().getContent();
          if (content instanceof XmlSchemaComplexContentExtension) {
            XmlSchemaComplexContentExtension extension = (XmlSchemaComplexContentExtension) content;
            processXmlSchemaParticle(extension.getParticle());
          } else if (content instanceof XmlSchemaSimpleContentExtension) {
            final XmlSchemaSimpleContentExtension extension =
                (XmlSchemaSimpleContentExtension) content;
            processSimpleContent(xmlSchemaElement, extension.getBaseTypeName());
          } else if (content instanceof XmlSchemaSimpleContentRestriction) {
            final XmlSchemaSimpleContentRestriction restriction =
                (XmlSchemaSimpleContentRestriction) content;
            processSimpleContent(xmlSchemaElement, restriction.getBaseTypeName());
          }
        }
      }
    }
  }

  private void processXmlSchemaParticle(XmlSchemaParticle particle) {
    if (particle instanceof XmlSchemaSequence) {
      XmlSchemaSequence schemaSequence = (XmlSchemaSequence) particle;
      for (final XmlSchemaSequenceMember element : schemaSequence.getItems()) {
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

  private void processSimpleContent(
      final XmlSchemaElement parentElement, final QName simpleContentTypeName) {
    final QName baseTypeName = getBaseTypeQName(simpleContentTypeName);

    if (baseTypeName != null) {
      mapSchemaElement(parentElement, baseTypeName);
    } else {
      LOGGER.debug("Could not find the base type for simple content: {}", simpleContentTypeName);
    }
  }

  private void processSimpleType(final XmlSchemaElement xmlSchemaElement) {
    final QName baseTypeName =
        getBaseTypeQName((XmlSchemaSimpleType) xmlSchemaElement.getSchemaType());

    if (baseTypeName != null) {
      mapSchemaElement(xmlSchemaElement, baseTypeName);
    } else {
      LOGGER.debug("Could not find the base type for simple type: {}", xmlSchemaElement.getQName());
    }
  }

  private QName getBaseTypeQName(final XmlSchemaSimpleType simpleType) {
    final boolean isBasicType = toBasicType(simpleType.getQName()) != null;
    if (isBasicType) {
      return simpleType.getQName();
    }

    final XmlSchemaSimpleTypeContent content = simpleType.getContent();
    if (content instanceof XmlSchemaSimpleTypeList) {
      final XmlSchemaSimpleTypeList simpleListType = (XmlSchemaSimpleTypeList) content;
      if (simpleListType.getItemType() != null) {
        return getBaseTypeQName(simpleListType.getItemType());
      } else {
        final QName restrictionBaseTypeName = simpleListType.getItemTypeName();
        return getBaseTypeQName(restrictionBaseTypeName);
      }
    } else if (content instanceof XmlSchemaSimpleTypeRestriction) {
      final XmlSchemaSimpleTypeRestriction simpleRestrictionType =
          (XmlSchemaSimpleTypeRestriction) content;
      if (simpleRestrictionType.getBaseType() != null) {
        return getBaseTypeQName(simpleRestrictionType.getBaseType());
      } else {
        final QName restrictionBaseTypeName = simpleRestrictionType.getBaseTypeName();
        return getBaseTypeQName(restrictionBaseTypeName);
      }
    } else {
      return null;
    }
  }

  private QName getBaseTypeQName(final QName simpleTypeQName) {
    final boolean isBasicType = toBasicType(simpleTypeQName) != null;
    if (isBasicType) {
      return simpleTypeQName;
    }

    final XmlSchemaSimpleType simpleType =
        (XmlSchemaSimpleType) schema.getTypeByName(simpleTypeQName);
    if (simpleType == null) {
      return null;
    }

    return getBaseTypeQName(simpleType);
  }

  private void mapSchemaElement(final XmlSchemaElement element, final QName elementBaseTypeName) {
    final String elementName = element.getName();

    if (Constants.XSD_STRING.equals(elementBaseTypeName)) {
      textualProperties.add(elementName);
    }

    properties.add(elementName);
  }

  private Boolean processGmlType(XmlSchemaElement xmlSchemaElement) {
    QName qName = xmlSchemaElement.getSchemaTypeName();
    String name = xmlSchemaElement.getName();

    if (qName != null
        && StringUtils.isNotEmpty(name)
        && qName.getNamespaceURI().equals(gmlNamespace)
        && (qName.getLocalPart().equals("TimeInstantType")
            || qName.getLocalPart().equals("TimePeriodType"))) {
      LOGGER.debug("Adding temporal property: {}", name);
      temporalProperties.add(name);
      properties.add(name);
      return true;
    }

    if (qName != null
        && qName.getNamespaceURI().equals(gmlNamespace)
        && StringUtils.isNotEmpty(name)) {
      LOGGER.debug("Adding geo property: {}", name);
      gmlProperties.add(name);
      properties.add(name);
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

  public boolean isQueryable(String propertyName) {
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
