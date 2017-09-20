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

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.core.TreeMarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Converts CSW Record to a Metacard. */
public class CswRecordConverter implements Converter, MetacardTransformer, InputTransformer {
  public static final MimeType XML_MIME_TYPE = setXmlMimeType();

  private static final Logger LOGGER = LoggerFactory.getLogger(CswRecordConverter.class);

  private XStream xstream;

  private static XMLInputFactory factory;

  private MetacardType metacardType;

  static {
    factory = XMLInputFactory.newInstance();
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    factory.setProperty(
        XMLInputFactory.SUPPORT_DTD, Boolean.FALSE); // This disables DTDs entirely for that factory
    factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
  }

  public CswRecordConverter(MetacardType metacardType) {
    xstream = new XStream(new Xpp3Driver());
    xstream.setClassLoader(this.getClass().getClassLoader());
    xstream.registerConverter(this);
    xstream.alias(CswConstants.CSW_RECORD_LOCAL_NAME, Metacard.class);
    xstream.alias(CswConstants.CSW_RECORD, Metacard.class);
    this.metacardType = getMetacardTypeWithBackwardsCompatibility(metacardType);
  }

  @Override
  public boolean canConvert(Class clazz) {
    return Metacard.class.isAssignableFrom(clazz);
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    if (source == null || !(source instanceof Metacard)) {
      LOGGER.debug("Failed to marshal Metacard: {}", source);
      return;
    }

    Map<String, Object> arguments = CswMarshallHelper.getArguments(context);

    writer.startNode((String) arguments.get(CswConstants.ROOT_NODE_NAME));

    if ((Boolean) arguments.get(CswConstants.WRITE_NAMESPACES)) {
      CswMarshallHelper.writeNamespaces(writer);
    }

    MetacardImpl metacard = new MetacardImpl((Metacard) source);

    List<QName> fieldsToWrite = (List<QName>) arguments.get(CswConstants.ELEMENT_NAMES);
    if (fieldsToWrite != null) {
      CswMarshallHelper.writeFields(writer, context, metacard, fieldsToWrite);
    } else { // write all fields
      CswMarshallHelper.writeAllFields(writer, context, metacard);
    }

    if ((fieldsToWrite == null || fieldsToWrite.contains(CswConstants.CSW_TEMPORAL_QNAME))
        && metacard.getEffectiveDate() != null
        && metacard.getExpirationDate() != null) {
      CswMarshallHelper.writeTemporalData(writer, context, metacard);
    }

    if (fieldsToWrite == null || fieldsToWrite.contains(CswConstants.OWS_BOUNDING_BOX_QNAME)) {
      CswMarshallHelper.writeBoundingBox(writer, context, metacard);
    }

    writer.endNode();
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Map<String, String> cswAttrMap =
        new CaseInsensitiveMap(
            DefaultCswRecordMap.getDefaultCswRecordMap().getCswToMetacardAttributeNames());
    Object mappingObj = context.get(CswConstants.CSW_MAPPING);

    if (mappingObj instanceof Map<?, ?>) {
      CswUnmarshallHelper.removeExistingAttributes(cswAttrMap, (Map<String, String>) mappingObj);
    }

    CswAxisOrder cswAxisOrder = CswAxisOrder.LON_LAT;
    Object cswAxisOrderObject = context.get(CswConstants.AXIS_ORDER_PROPERTY);

    if (cswAxisOrderObject != null && cswAxisOrderObject.getClass().isEnum()) {
      Enum value = (Enum) cswAxisOrderObject;
      cswAxisOrder = CswAxisOrder.valueOf(value.name());
    }

    Map<String, String> namespaceMap = null;
    Object namespaceObj = context.get(CswConstants.NAMESPACE_DECLARATIONS);

    if (namespaceObj instanceof Map<?, ?>) {
      namespaceMap = (Map<String, String>) namespaceObj;
    }

    Metacard metacard =
        CswUnmarshallHelper.createMetacardFromCswRecord(
            metacardType, reader, cswAxisOrder, namespaceMap);

    Object sourceIdObj = context.get(Core.SOURCE_ID);

    if (sourceIdObj instanceof String) {
      metacard.setSourceId((String) sourceIdObj);
    }

    return metacard;
  }

  @Override
  public Metacard transform(InputStream inputStream)
      throws IOException, CatalogTransformerException {
    return transform(inputStream, null);
  }

  @Override
  public Metacard transform(InputStream inputStream, String id)
      throws IOException, CatalogTransformerException {
    Metacard metacard = null;

    try {
      metacard = (Metacard) xstream.fromXML(inputStream);

      if (StringUtils.isNotEmpty(id)) {
        metacard.setAttribute(new AttributeImpl(Core.ID, id));
      }
    } catch (XStreamException e) {
      throw new CatalogTransformerException("Unable to transform from CSW Record to Metacard.", e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }

    if (metacard == null) {
      throw new CatalogTransformerException("Unable to transform from CSW Record to Metacard.");
    }

    return metacard;
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    if (StringUtils.isNotBlank(metacard.getMetadata())) {
      // Check if the metadata is csw:Record
      try {
        StringReader xml = new StringReader(metacard.getMetadata());
        XMLEventReader reader = factory.createXMLEventReader(xml);
        boolean rootFound = false;
        while (reader.hasNext() && !rootFound) {
          XMLEvent event = reader.nextEvent();
          if (event.isStartElement()) {
            rootFound = true;
            QName name = event.asStartElement().getName();
            if (StringUtils.equals(CswConstants.CSW_RECORD_LOCAL_NAME, name.getLocalPart())
                && StringUtils.equals(CswConstants.CSW_OUTPUT_SCHEMA, name.getNamespaceURI())) {
              return new BinaryContentImpl(
                  IOUtils.toInputStream(metacard.getMetadata()), XML_MIME_TYPE);
            }
          }
        }
      } catch (Exception e) {
        // Ignore and proceed with the transform.
      }
    }
    StringWriter stringWriter = new StringWriter();

    Boolean omitXmlDec = null;
    if (arguments != null) {
      omitXmlDec = (Boolean) arguments.get(CswConstants.OMIT_XML_DECLARATION);
    }
    if (omitXmlDec == null || !omitXmlDec) {
      stringWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
    }

    PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
    MarshallingContext context = new TreeMarshaller(writer, null, null);
    context.put(CswConstants.WRITE_NAMESPACES, true);
    copyArgumentsToContext(context, arguments);

    this.marshal(metacard, writer, context);

    BinaryContent transformedContent;

    ByteArrayInputStream byteArrayInputStream =
        new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8));
    transformedContent = new BinaryContentImpl(byteArrayInputStream, XML_MIME_TYPE);
    return transformedContent;
  }

  private void copyArgumentsToContext(
      MarshallingContext context, Map<String, Serializable> arguments) {

    if (context == null || arguments == null) {
      return;
    }

    for (Map.Entry<String, Serializable> entry : arguments.entrySet()) {
      context.put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Converts properties in CSW records that overlap with same name as a basic Metacard attribute,
   * e.g., title. This conversion method is needed mainly because CSW records express all dates as
   * strings, whereas MetacardImpl expresses them as java.util.Date types.
   *
   * @param attributeFormat the format of the attribute to be converted
   * @param value the value to be converted
   * @return the value that was extracted from {@code reader} and is of the type described by {@code
   *     attributeFormat}
   */
  public static Serializable convertStringValueToMetacardValue(
      AttributeType.AttributeFormat attributeFormat, String value) {
    return CswUnmarshallHelper.convertStringValueToMetacardValue(attributeFormat, value);
  }

  /**
   * Takes a CSW attribute as a name and value and returns an {@link Attribute} whose value is
   * {@code cswAttributeValue} converted to the type of the attribute {@code metacardAttributeName}
   * in a {@link Metacard}.
   *
   * @param cswAttributeName the name of the CSW attribute
   * @param cswAttributeValue the value of the CSW attribute
   * @param metacardAttributeName the name of the {@code Metacard} attribute whose type {@code
   *     cswAttributeValue} will be converted to
   * @return an {@code Attribute} with the name {@code metacardAttributeName} and the value {@code
   *     cswAttributeValue} converted to the type of the attribute {@code metacardAttributeName} in
   *     a {@code Metacard}.
   */
  public Attribute getMetacardAttributeFromCswAttribute(
      String cswAttributeName, Serializable cswAttributeValue, String metacardAttributeName) {
    return CswUnmarshallHelper.getMetacardAttributeFromCswAttribute(
        metacardType, cswAttributeName, cswAttributeValue, metacardAttributeName);
  }

  /**
   * Converts an attribute name to the csw:Record attribute it corresponds to.
   *
   * @param attributeName the name of the attribute
   * @return the name of the csw:Record attribute that this attribute name corresponds to
   */
  public static String getCswAttributeFromAttributeName(String attributeName) {
    return CswUnmarshallHelper.getCswAttributeFromAttributeName(attributeName);
  }

  /**
   * Adds the Effective Date and Content Type fields to the new taxonomy metacard for backwards
   * compatibility
   *
   * @param metacardType
   * @return a new metacard type with the effective date and content type attributes
   */
  private MetacardType getMetacardTypeWithBackwardsCompatibility(MetacardType metacardType) {
    Set<AttributeDescriptor> additionalDescriptors =
        ImmutableSet.of(
            BasicTypes.BASIC_METACARD.getAttributeDescriptor(Metacard.EFFECTIVE),
            BasicTypes.BASIC_METACARD.getAttributeDescriptor(Metacard.CONTENT_TYPE));
    return new MetacardTypeImpl(metacardType.getName(), metacardType, additionalDescriptors);
  }

  private static MimeType setXmlMimeType() {
    try {
      return new MimeType(com.google.common.net.MediaType.APPLICATION_XML_UTF_8.toString());
    } catch (MimeTypeParseException e) {
      return new MimeType();
    }
  }
}
