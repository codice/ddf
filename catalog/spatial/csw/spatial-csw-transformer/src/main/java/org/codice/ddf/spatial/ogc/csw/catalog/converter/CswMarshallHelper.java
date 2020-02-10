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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import java.io.Serializable;
import java.util.Base64;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.catalog.common.converter.XmlNode;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.joda.time.format.ISODateTimeFormat;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CswMarshallHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(CswMarshallHelper.class);

  static final DatatypeFactory XSD_FACTORY;

  static {
    DatatypeFactory factory = null;

    try {
      factory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      LOGGER.info("Failed to create xsdFactory: {}", e.getMessage());
    }

    XSD_FACTORY = factory;
  }

  static void writeNamespaces(HierarchicalStreamWriter writer) {
    writer.addAttribute(
        "xmlns:" + CswConstants.CSW_NAMESPACE_PREFIX, CswConstants.CSW_OUTPUT_SCHEMA);
    writer.addAttribute(
        "xmlns:" + CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX, CswConstants.DUBLIN_CORE_SCHEMA);
    writer.addAttribute(
        "xmlns:" + CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX,
        CswConstants.DUBLIN_CORE_TERMS_SCHEMA);
    writer.addAttribute("xmlns:" + CswConstants.OWS_NAMESPACE_PREFIX, CswConstants.OWS_NAMESPACE);
  }

  static void writeTemporalData(
      HierarchicalStreamWriter writer, MarshallingContext context, MetacardImpl metacard) {
    StringBuilder sb = new StringBuilder();
    sb.append(ISODateTimeFormat.dateTime().print(metacard.getEffectiveDate().getTime()))
        .append(" to ")
        .append(ISODateTimeFormat.dateTime().print((metacard.getExpirationDate()).getTime()));
    writeValue(writer, context, null, CswConstants.CSW_TEMPORAL_QNAME, sb.toString());
  }

  static void writeFields(
      HierarchicalStreamWriter writer,
      MarshallingContext context,
      MetacardImpl metacard,
      List<QName> fieldsToWrite) {
    for (QName qName : fieldsToWrite) {
      if (qName != null && !qName.equals(CswConstants.OWS_BOUNDING_BOX_QNAME)) {

        String attrName =
            DefaultCswRecordMap.getDefaultCswRecordMap().getDefaultMetacardFieldFor(qName);
        AttributeDescriptor ad = metacard.getMetacardType().getAttributeDescriptor(attrName);

        /*  Backwards Compatibility */
        if (ad == null) {
          ad = MetacardImpl.BASIC_METACARD.getAttributeDescriptor(attrName);
        }
        writeAttribute(writer, context, metacard, ad, qName);
      }
    }
  }

  static void writeAttribute(
      HierarchicalStreamWriter writer,
      MarshallingContext context,
      Metacard metacard,
      AttributeDescriptor attributeDescriptor,
      QName field) {
    if (attributeDescriptor != null) {

      Attribute attr = metacard.getAttribute(attributeDescriptor.getName());
      if (attr != null) {
        if (attributeDescriptor.isMultiValued()) {

          for (Serializable value : attr.getValues()) {

            writeValue(writer, context, attributeDescriptor, field, value);
          }
        } else {
          writeValue(writer, context, attributeDescriptor, field, attr.getValue());
        }
      } else if (CswConstants.REQUIRED_FIELDS.contains(field)) {
        writeValue(writer, context, attributeDescriptor, field, "");
      }
    }
  }

  static void writeAllFields(
      HierarchicalStreamWriter writer, MarshallingContext context, MetacardImpl metacard) {
    Set<AttributeDescriptor> attrDescs = metacard.getMetacardType().getAttributeDescriptors();

    for (AttributeDescriptor ad : attrDescs) {
      List<QName> qNames =
          DefaultCswRecordMap.getDefaultCswRecordMap().getCswFieldsFor(ad.getName());

      for (QName qName : qNames) {
        CswMarshallHelper.writeAttribute(writer, context, metacard, ad, qName);
      }
    }
  }

  static Map<String, Object> getArguments(MarshallingContext context) {
    Map<String, Object> args = new HashMap<>();

    Object writeNamespaceObj = context.get(CswConstants.WRITE_NAMESPACES);
    Boolean doWriteNamespaces = false;

    if (writeNamespaceObj instanceof Boolean) {
      doWriteNamespaces = (Boolean) writeNamespaceObj;
      args.put(CswConstants.WRITE_NAMESPACES, doWriteNamespaces);
    } else {
      args.put(CswConstants.WRITE_NAMESPACES, doWriteNamespaces);
    }

    Object elementSetObj = context.get(CswConstants.ELEMENT_SET_TYPE);
    Object elementNamesObj = context.get(CswConstants.ELEMENT_NAMES);

    String rootNodeName = CswConstants.CSW_RECORD;

    if (elementSetObj instanceof ElementSetType) {
      List<QName> elementsToWrite;
      ElementSetType elementSetType = (ElementSetType) elementSetObj;

      switch (elementSetType) {
        case BRIEF:
          elementsToWrite = CswConstants.BRIEF_CSW_RECORD_FIELDS;
          rootNodeName = CswConstants.CSW_BRIEF_RECORD;
          break;
        case SUMMARY:
          elementsToWrite = CswConstants.SUMMARY_CSW_RECORD_FIELDS;
          rootNodeName = CswConstants.CSW_SUMMARY_RECORD;
          break;
        case FULL:
        default:
          elementsToWrite = CswConstants.FULL_CSW_RECORD_FIELDS;
          break;
      }

      args.put(CswConstants.ELEMENT_NAMES, elementsToWrite);
      args.put(CswConstants.ROOT_NODE_NAME, rootNodeName);
    } else if (elementNamesObj instanceof List<?>) {
      args.put(CswConstants.ELEMENT_NAMES, elementNamesObj);
      args.put(CswConstants.ROOT_NODE_NAME, rootNodeName);
    } else {
      args.put(CswConstants.ROOT_NODE_NAME, rootNodeName);
      args.put(CswConstants.ELEMENT_NAMES, CswConstants.FULL_CSW_RECORD_FIELDS);
    }

    return args;
  }

  static void writeBoundingBox(
      HierarchicalStreamWriter writer, MarshallingContext context, Metacard metacard) {
    Set<AttributeDescriptor> attrDescs = metacard.getMetacardType().getAttributeDescriptors();
    List<Geometry> geometries = new LinkedList<>();

    for (AttributeDescriptor ad : attrDescs) {
      if (ad.getType() != null
          && AttributeType.AttributeFormat.GEOMETRY.equals(ad.getType().getAttributeFormat())) {

        Attribute attr = metacard.getAttribute(ad.getName());

        if (attr != null) {
          if (ad.isMultiValued()) {
            for (Serializable value : attr.getValues()) {
              geometries.add(XmlNode.readGeometry((String) value));
            }
          } else {
            geometries.add(XmlNode.readGeometry((String) attr.getValue()));
          }
        }
      }
    }

    Geometry allGeometry =
        new GeometryCollection(
            geometries.toArray(new Geometry[geometries.size()]), new GeometryFactory());
    Envelope bounds = allGeometry.getEnvelopeInternal();

    if (!bounds.isNull()) {
      String bbox =
          CswConstants.OWS_NAMESPACE_PREFIX
              + CswConstants.NAMESPACE_DELIMITER
              + CswConstants.OWS_BOUNDING_BOX;
      String lower =
          CswConstants.OWS_NAMESPACE_PREFIX
              + CswConstants.NAMESPACE_DELIMITER
              + CswConstants.OWS_LOWER_CORNER;
      String upper =
          CswConstants.OWS_NAMESPACE_PREFIX
              + CswConstants.NAMESPACE_DELIMITER
              + CswConstants.OWS_UPPER_CORNER;
      writer.startNode(bbox);
      writer.addAttribute(CswConstants.CRS, CswConstants.SRS_URL);
      writer.startNode(lower);
      writer.setValue(bounds.getMinX() + " " + bounds.getMinY());
      writer.endNode();
      writer.startNode(upper);
      writer.setValue(bounds.getMaxX() + " " + bounds.getMaxY());
      writer.endNode();
      writer.endNode();
    }
  }

  static void writeValue(
      HierarchicalStreamWriter writer,
      MarshallingContext context,
      AttributeDescriptor attributeDescriptor,
      QName field,
      Serializable value) {

    String xmlValue = null;
    AttributeType.AttributeFormat attrFormat = null;

    if (attributeDescriptor != null && attributeDescriptor.getType() != null) {
      attrFormat = attributeDescriptor.getType().getAttributeFormat();
    }

    if (attrFormat == null) {
      attrFormat = AttributeType.AttributeFormat.STRING;
    }

    String name;

    if (!StringUtils.isBlank(field.getNamespaceURI())) {
      if (!StringUtils.isBlank(field.getPrefix())) {
        name = field.getPrefix() + CswConstants.NAMESPACE_DELIMITER + field.getLocalPart();
      } else {
        name = field.getLocalPart();
      }
    } else {
      name = field.getLocalPart();
    }

    switch (attrFormat) {
      case BINARY:
        xmlValue = Base64.getEncoder().encodeToString((byte[]) value);
        break;
      case DATE:
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime((Date) value);
        xmlValue = XSD_FACTORY.newXMLGregorianCalendar(cal).toXMLFormat();
        break;
      case OBJECT:
        break;
      case GEOMETRY:
      case XML:
      default:
        xmlValue = value.toString();
        break;
    }

    // Write the node if we were able to convert it.
    if (xmlValue != null) {
      writer.startNode(name);

      if (!StringUtils.isBlank(field.getNamespaceURI())) {
        if (StringUtils.isBlank(field.getPrefix())) {
          writer.addAttribute(XMLConstants.XMLNS_ATTRIBUTE, field.getNamespaceURI());
        }
      }

      writer.setValue(xmlValue);
      writer.endNode();
    }
  }
}
