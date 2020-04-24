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

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.BoundingBoxReader;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswUnmarshallHelper {
  private static final Logger LOGGER = LoggerFactory.getLogger(CswUnmarshallHelper.class);

  /**
   * The map of metacard attributes that both the basic DDF MetacardTypeImpl and the CSW
   * MetacardType define as attributes. This is used to detect these element tags when unmarshalling
   * XML so that the tag name can be modified with a CSW-unique prefix before attempting to lookup
   * the attribute descriptor corresponding to the tag.
   */
  private static final List<String> CSW_OVERLAPPING_ATTRIBUTE_NAMES =
      Arrays.asList(Core.TITLE, Core.CREATED, Core.MODIFIED);

  public static Date convertToDate(String value) {
    // Dates are strings and expected to be in ISO8601 format, YYYY-MM-DD'T'hh:mm:ss.sss,
    // per annotations in the CSW Record schema. At least the date portion must be present;
    // the time zone and time are optional.
    try {
      return ISODateTimeFormat.dateOptionalTimeParser().parseDateTime(value).toDate();
    } catch (IllegalArgumentException e) {
      LOGGER.debug("Failed to convert to date {} from ISO Format: {}", value, e);
    }

    // failed to convert iso format, attempt to convert from xsd:date or xsd:datetime format
    // this format is used by the NSG interoperability CITE tests
    try {
      return CswMarshallHelper.XSD_FACTORY
          .newXMLGregorianCalendar(value)
          .toGregorianCalendar()
          .getTime();
    } catch (IllegalArgumentException e) {
      LOGGER.debug("Unable to convert date {} from XSD format {} ", value, e);
    }

    // try from java date serialization for the default locale
    try {
      return DateFormat.getDateInstance().parse(value);
    } catch (ParseException e) {
      LOGGER.debug("Unable to convert date {} from default locale format {} ", value, e);
    }

    // default to current date
    LOGGER.debug("Unable to convert {} to a date object, defaulting to current time", value);
    return new Date();
  }

  public static void removeExistingAttributes(
      Map<String, String> cswAttrMap, Map<String, String> mappingObj) {
    // If we got mappings passed in, remove the existing mappings for that attribute
    Map<String, String> customMappings = new CaseInsensitiveMap(mappingObj);
    Map<String, String> convertedMappings = new CaseInsensitiveMap();

    for (Map.Entry<String, String> customMapEntry : customMappings.entrySet()) {
      Iterator<Map.Entry<String, String>> existingMapIter = cswAttrMap.entrySet().iterator();

      while (existingMapIter.hasNext()) {
        Map.Entry<String, String> existingMapEntry = existingMapIter.next();
        if (existingMapEntry.getValue().equalsIgnoreCase(customMapEntry.getValue())) {
          existingMapIter.remove();
        }
      }

      String key = convertToCswField(customMapEntry.getKey());
      String value = customMapEntry.getValue();
      LOGGER.debug("Adding key: {} & value: {}", key, value);
      convertedMappings.put(key, value);
    }

    cswAttrMap.putAll(convertedMappings);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Map contents: {}", Arrays.toString(cswAttrMap.entrySet().toArray()));
    }
  }

  public static String convertToCswField(String name) {

    if (CSW_OVERLAPPING_ATTRIBUTE_NAMES.contains(name)) {
      return CswConstants.CSW_ATTRIBUTE_PREFIX + name;
    }

    return name;
  }

  public static MetacardImpl createMetacardFromCswRecord(
      MetacardType metacardType,
      HierarchicalStreamReader hreader,
      CswAxisOrder cswAxisOrder,
      Map<String, String> namespaceMap) {

    StringWriter metadataWriter = new StringWriter();
    HierarchicalStreamReader reader =
        XStreamAttributeCopier.copyXml(hreader, metadataWriter, namespaceMap);

    MetacardImpl mc = new MetacardImpl(metacardType);

    while (reader.hasMoreChildren()) {
      reader.moveDown();

      String nodeName = reader.getNodeName();
      LOGGER.debug("node name: {}.", nodeName);
      String name = getCswAttributeFromAttributeName(nodeName);
      LOGGER.debug("Processing node {}", name);

      if (DefaultCswRecordMap.hasDefaultMetacardFieldFor(name)) {
        String defaultMetacardField = DefaultCswRecordMap.getDefaultMetacardFieldFor(name);

        AttributeDescriptor attributeDescriptor =
            metacardType.getAttributeDescriptor(defaultMetacardField);
        Serializable value =
            convertRecordPropertyToMetacardAttribute(
                attributeDescriptor.getType().getAttributeFormat(), reader, cswAxisOrder);

        if (isNotEmpty(value)) {

          String attributeName = attributeDescriptor.getName();

          if (attributeName.equals(Core.THUMBNAIL)) {
            String uri = new String((byte[]) value, StandardCharsets.UTF_8);
            URL url;
            InputStream is = null;
            try {
              url = new URL(uri);
              is = url.openStream();
              mc.setThumbnail(IOUtils.toByteArray(is));
            } catch (IOException e) {
              mc.setThumbnail((byte[]) value);
            } finally {
              IOUtils.closeQuietly(is);
            }
          } else if (attributeDescriptor.isMultiValued()
              && mc.getAttribute(attributeName) != null) {

            ArrayList<Serializable> serializables =
                new ArrayList<>(mc.getAttribute(attributeName).getValues());
            if (CollectionUtils.isNotEmpty(serializables)) {
              serializables.add(value);
              mc.setAttribute(attributeName, serializables);
            }
          } else {
            mc.setAttribute(attributeName, value);
          }
        }
        /* Set Content Type for backwards compatibility */
      } else if (name.equals(CswConstants.CSW_TYPE)) {
        mc.setContentTypeName(reader.getValue());
      }
      reader.moveUp();
    }

    /* Save entire CSW Record XML as the metacard's metadata string */
    mc.setMetadata(metadataWriter.toString());

    return mc;
  }

  private static boolean isNotEmpty(Serializable serializable) {
    if (serializable instanceof String) {
      String compString = (String) serializable;
      if (StringUtils.isNotEmpty(compString.trim())) {
        return true;
      }
    } else if (serializable != null) {
      return true;
    }
    return false;
  }

  /**
   * Takes a CSW attribute as a name and value and returns an {@link Attribute} whose value is
   * {@code cswAttributeValue} converted to the type of the attribute {@code metacardAttributeName}
   * in a {@link ddf.catalog.data.Metacard}.
   *
   * @param cswAttributeName the name of the CSW attribute
   * @param cswAttributeValue the value of the CSW attribute
   * @param metacardAttributeName the name of the {@code Metacard} attribute whose type {@code
   *     cswAttributeValue} will be converted to
   * @return an {@code Attribute} with the name {@code metacardAttributeName} and the value {@code
   *     cswAttributeValue} converted to the type of the attribute {@code metacardAttributeName} in
   *     a {@code Metacard}.
   */
  public static Attribute getMetacardAttributeFromCswAttribute(
      MetacardType metacardType,
      String cswAttributeName,
      Serializable cswAttributeValue,
      String metacardAttributeName) {
    AttributeType.AttributeFormat cswAttributeFormat =
        metacardType.getAttributeDescriptor(metacardAttributeName).getType().getAttributeFormat();
    AttributeDescriptor metacardAttributeDescriptor =
        metacardType.getAttributeDescriptor(metacardAttributeName);
    AttributeType.AttributeFormat metacardAttrFormat =
        metacardAttributeDescriptor.getType().getAttributeFormat();
    LOGGER.debug(
        "Setting overlapping Metacard attribute [{}] to value in "
            + "CSW attribute [{}] that has value [{}] and format {}",
        metacardAttributeName,
        cswAttributeName,
        cswAttributeValue,
        metacardAttrFormat);

    if (cswAttributeFormat.equals(metacardAttrFormat)) {
      return new AttributeImpl(metacardAttributeName, cswAttributeValue);
    } else {
      Serializable value =
          convertStringValueToMetacardValue(metacardAttrFormat, cswAttributeValue.toString());
      return new AttributeImpl(metacardAttributeName, value);
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
    LOGGER.debug("converting csw record property {}", value);
    Serializable ser = null;

    if (attributeFormat == null) {
      LOGGER.debug("AttributeFormat was null when converting {}", value);
      return ser;
    }

    switch (attributeFormat) {
      case BOOLEAN:
        ser = Boolean.valueOf(value);
        break;
      case DOUBLE:
        ser = Double.valueOf(value);
        break;
      case FLOAT:
        ser = Float.valueOf(value);
        break;
      case INTEGER:
        ser = Integer.valueOf(value);
        break;
      case LONG:
        ser = Long.valueOf(value);
        break;
      case SHORT:
        ser = Short.valueOf(value);
        break;
      case XML:
      case STRING:
        ser = value;
        break;
      case DATE:
        ser = CswUnmarshallHelper.convertToDate(value);
        break;
      default:
        break;
    }

    return ser;
  }

  /**
   * Converts an attribute name to the csw:Record attribute it corresponds to.
   *
   * @param attributeName the name of the attribute
   * @return the name of the csw:Record attribute that this attribute name corresponds to
   */
  static String getCswAttributeFromAttributeName(String attributeName) {
    // Remove the prefix if it exists
    if (StringUtils.contains(attributeName, CswConstants.NAMESPACE_DELIMITER)) {
      attributeName = StringUtils.split(attributeName, CswConstants.NAMESPACE_DELIMITER)[1];
    }

    // Some attribute names overlap with basic Metacard attribute names,
    // e.g., "title".
    // So if this is one of those attribute names, get the CSW
    // attribute for the name to be looked up.
    return CswUnmarshallHelper.convertToCswField(attributeName);
  }

  /**
   * Converts the CSW record property {@code reader} is currently at to the specified Metacard
   * attribute format.
   *
   * @param reader the reader at the element whose value you want to convert
   * @param cswAxisOrder the order of the coordinates in the XML being read by {@code reader}
   * @return the value that was extracted from {@code reader} and is of the type described by {@code
   *     attributeFormat}
   */
  public static Serializable convertRecordPropertyToMetacardAttribute(
      AttributeType.AttributeFormat attributeFormat,
      HierarchicalStreamReader reader,
      CswAxisOrder cswAxisOrder) {
    LOGGER.debug("converting csw record property {}", reader.getValue());
    Serializable ser = null;

    switch (attributeFormat) {
      case BOOLEAN:
        ser = Boolean.valueOf(reader.getValue());
        break;
      case DOUBLE:
        ser = Double.valueOf(reader.getValue());
        break;
      case FLOAT:
        ser = Float.valueOf(reader.getValue());
        break;
      case INTEGER:
        ser = Integer.valueOf(reader.getValue());
        break;
      case LONG:
        ser = Long.valueOf(reader.getValue());
        break;
      case SHORT:
        ser = Short.valueOf(reader.getValue());
        break;
      case XML:
      case STRING:
        ser = reader.getValue();
        break;
      case DATE:
        ser =
            CswUnmarshallHelper.convertStringValueToMetacardValue(
                attributeFormat, reader.getValue());
        break;
      case GEOMETRY:
        // We pass in cswAxisOrder, so we can determine coord order (LAT/LON vs
        // LON/LAT).
        BoundingBoxReader bboxReader = new BoundingBoxReader(reader, cswAxisOrder);

        try {
          ser = bboxReader.getWkt();
        } catch (CswException cswException) {
          LOGGER.debug(
              "CswUnmarshallHelper.convertRecordPropertyToMetacardAttribute(): could not read BoundingBox.",
              cswException);
        }

        LOGGER.debug("WKT = {}", ser);
        break;
      case BINARY:
        ser = reader.getValue().getBytes(StandardCharsets.UTF_8);

        break;
      default:
        break;
    }

    return ser;
  }
}
