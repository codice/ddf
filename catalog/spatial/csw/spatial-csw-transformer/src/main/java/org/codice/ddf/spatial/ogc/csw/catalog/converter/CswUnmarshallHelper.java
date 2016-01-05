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

package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.BoundingBoxReader;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;

class CswUnmarshallHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(CswUnmarshallHelper.class);
    private static final CswRecordMetacardType CSW_METACARD_TYPE = new CswRecordMetacardType();
    private static final String UTF8_ENCODING = "UTF-8";

    /**
     * The map of metacard attributes that both the basic DDF MetacardTypeImpl and the CSW
     * MetacardType define as attributes. This is used to detect these element tags when
     * unmarshalling XML so that the tag name can be modified with a CSW-unique prefix before
     * attempting to lookup the attribute descriptor corresponding to the tag.
     */
    private static final List<String> CSW_OVERLAPPING_ATTRIBUTE_NAMES = Arrays
            .asList(Metacard.TITLE, Metacard.CREATED, Metacard.MODIFIED);

    static Date convertToDate(String value) {
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
            return CswMarshallHelper.XSD_FACTORY.newXMLGregorianCalendar(value).toGregorianCalendar().getTime();
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
        LOGGER.warn("Unable to convert {} to a date object, defaulting to current time", value);
        return new Date();
    }

    static void removeExistingAttributes(Map<String, String> cswAttrMap,
            Map<String, String> mappingObj) {
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

    static String convertToCswField(String name) {

        if (CSW_OVERLAPPING_ATTRIBUTE_NAMES.contains(name)) {
            return CswRecordMetacardType.CSW_ATTRIBUTE_PREFIX + name;
        }

        return name;
    }

    static MetacardImpl createMetacardFromCswRecord(HierarchicalStreamReader hreader,
            Map<String, String> cswToMetacardAttributeNames, String resourceUriMapping,
            String thumbnailMapping, CswAxisOrder cswAxisOrder, Map<String, String> namespaceMap) {

        StringWriter metadataWriter = new StringWriter();
        HierarchicalStreamReader reader = XStreamAttributeCopier
                .copyXml(hreader, metadataWriter, namespaceMap);

        MetacardImpl mc = new MetacardImpl(CSW_METACARD_TYPE);
        Map<String, Attribute> attributes = new TreeMap<>();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String nodeName = reader.getNodeName();
            LOGGER.debug("node name: {}.", nodeName);

            String name = getCswAttributeFromAttributeName(nodeName);

            LOGGER.debug("Processing node {}", name);
            AttributeDescriptor attributeDescriptor = CSW_METACARD_TYPE
                    .getAttributeDescriptor(name);

            Serializable value = null;

            // If XML node name matched an attribute descriptor in the
            // metacardType AND
            // the XML node has a non-blank value OR this is geometry/spatial
            // data,
            // then convert the CSW Record's property value for this XML node to
            // the
            // corresponding metacard attribute's value
            if (attributeDescriptor != null && (StringUtils.isNotBlank(reader.getValue())
                    || BasicTypes.GEO_TYPE.equals(attributeDescriptor.getType()))) {
                value = convertRecordPropertyToMetacardAttribute(
                        attributeDescriptor.getType().getAttributeFormat(), reader, cswAxisOrder);
            }

            if (null != value) {
                if (attributeDescriptor.isMultiValued()) {
                    if (attributes.containsKey(name)) {
                        AttributeImpl attribute = (AttributeImpl) attributes.get(name);
                        attribute.addValue(value);
                    } else {
                        attributes.put(name, new AttributeImpl(name, value));
                    }
                } else {
                    attributes.put(name, new AttributeImpl(name, value));
                }

                if (BasicTypes.GEO_TYPE.getAttributeFormat()
                        .equals(attributeDescriptor.getType().getAttributeFormat())) {
                    mc.setLocation((String) value);
                }
            }

            reader.moveUp();
        }

        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            Attribute attr = entry.getValue();
            mc.setAttribute(attr);

            String attrName = entry.getKey();

            // If this CSW attribute also maps to a basic metacard attribute,
            // (e.g., title, modified date, etc.)
            // then populate the basic metacard attribute with this attribute's
            // value.
            if (cswToMetacardAttributeNames.containsKey(attrName)) {
                String metacardAttrName = cswToMetacardAttributeNames.get(attrName);
                if (mc.getAttribute(metacardAttrName) == null) {
                    Attribute metacardAttr = getMetacardAttributeFromCswAttribute(attrName,
                            attr.getValue(), metacardAttrName);
                    mc.setAttribute(metacardAttr);
                }
            }
        }

        // Save entire CSW Record XML as the metacard's metadata string
        mc.setMetadata(metadataWriter.toString());

        // Set Metacard ID to the CSW Record's identifier
        // TODO: may need to sterilize the CSW Record identifier if it has
        // special chars that clash
        // with usage in a URL - empirical testing with various CSW sites will
        // determine this.
        mc.setId((String) mc.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue());

        try {
            URI namespaceUri = new URI(CSW_METACARD_TYPE.getNamespaceURI());
            mc.setTargetNamespace(namespaceUri);

        } catch (URISyntaxException e) {
            LOGGER.info("Error setting target namespace uri on metacard, Exception {}", e);
        }

        Date genericDate = new Date();

        if (mc.getEffectiveDate() == null) {
            mc.setEffectiveDate(genericDate);
        }

        if (mc.getCreatedDate() == null) {
            mc.setCreatedDate(genericDate);
        }

        if (mc.getModifiedDate() == null) {
            LOGGER.debug("modified date was null, setting to current date");
            mc.setModifiedDate(genericDate);
        }

        // Determine the csw field mapped to the resource uri and set that value
        // on the Metacard.RESOURCE_URI attribute
        // Default is for <source> field to define URI for product to be downloaded
        Attribute resourceUriAttr = mc.getAttribute(resourceUriMapping);

        if (resourceUriAttr != null && resourceUriAttr.getValue() != null) {
            String source = (String) resourceUriAttr.getValue();
            try {
                mc.setResourceURI(new URI(source));
            } catch (URISyntaxException e) {
                LOGGER.info("Error setting resource URI on metacard: {}, Exception {}", source, e);
            }
        }

        // determine the csw field mapped to the thumbnail and set that value on
        // the Metacard.THUMBNAIL
        // attribute
        Attribute thumbnailAttr = mc.getAttribute(thumbnailMapping);

        if (thumbnailAttr != null && thumbnailAttr.getValue() != null) {
            String thumbnail = (String) thumbnailAttr.getValue();
            URL url;
            InputStream is = null;

            try {
                url = new URL(thumbnail);
                is = url.openStream();
                mc.setThumbnail(IOUtils.toByteArray(url.openStream()));
            } catch (MalformedURLException e) {
                LOGGER.info("Error setting thumbnail data on metacard: {}, Exception {}", thumbnail,
                        e);
            } catch (IOException e) {
                LOGGER.info("Error setting thumbnail data on metacard: {}, Exception {}", thumbnail,
                        e);
            } finally {
                IOUtils.closeQuietly(is);
            }
        }

        return mc;
    }

    /**
     * Takes a CSW attribute as a name and value and returns an {@link Attribute} whose value is
     * {@code cswAttributeValue} converted to the type of the attribute
     * {@code metacardAttributeName} in a {@link Metacard}.
     *
     * @param cswAttributeName  the name of the CSW attribute
     * @param cswAttributeValue  the value of the CSW attribute
     * @param metacardAttributeName  the name of the {@code Metacard} attribute whose type
     *                               {@code cswAttributeValue} will be converted to
     * @return an {@code Attribute} with the name {@code metacardAttributeName} and the value
     *         {@code cswAttributeValue} converted to the type of the attribute
     *         {@code metacardAttributeName} in a {@code Metacard}.
     */
    static Attribute getMetacardAttributeFromCswAttribute(String cswAttributeName,
            Serializable cswAttributeValue, String metacardAttributeName) {
        AttributeType.AttributeFormat cswAttributeFormat = CSW_METACARD_TYPE
                .getAttributeDescriptor(cswAttributeName).getType().getAttributeFormat();
        AttributeDescriptor metacardAttributeDescriptor = CSW_METACARD_TYPE
                .getAttributeDescriptor(metacardAttributeName);
        AttributeType.AttributeFormat metacardAttrFormat = metacardAttributeDescriptor.getType()
                .getAttributeFormat();
        LOGGER.debug("Setting overlapping Metacard attribute [{}] to value in "
                        + "CSW attribute [{}] that has value [{}] and format {}",
                metacardAttributeName, cswAttributeName, cswAttributeValue, metacardAttrFormat);

        if (cswAttributeFormat.equals(metacardAttrFormat)) {
            return new AttributeImpl(metacardAttributeName, cswAttributeValue);
        } else {
            Serializable value = convertStringValueToMetacardValue(metacardAttrFormat,
                    cswAttributeValue.toString());
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
     * @return the value that was extracted from {@code reader} and is of the type described by
     *         {@code attributeFormat}
     */
    static Serializable convertStringValueToMetacardValue(AttributeType.AttributeFormat attributeFormat,
            String value) {
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
     * @param attributeName  the name of the attribute
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
     * @param attributeFormat  the {@link AttributeType.AttributeFormat} corresponding to the type that the value
     *                         in {@code reader} should be converted to
     * @param reader  the reader at the element whose value you want to convert
     * @param cswAxisOrder  the order of the coordinates in the XML being read by {@code reader}
     * @return the value that was extracted from {@code reader} and is of the type described by
     *         {@code attributeFormat}
     */
    static Serializable convertRecordPropertyToMetacardAttribute(
            AttributeType.AttributeFormat attributeFormat, HierarchicalStreamReader reader,
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
            ser = CswUnmarshallHelper.convertStringValueToMetacardValue(attributeFormat, reader.getValue());
            break;
        case GEOMETRY:
            // We pass in cswAxisOrder, so we can determine coord order (LAT/LON vs
            // LON/LAT).
            BoundingBoxReader bboxReader = new BoundingBoxReader(reader, cswAxisOrder);

            try {
                ser = bboxReader.getWkt();
            } catch (CswException cswException) {
                LOGGER.error("CswUnmarshallHelper.convertRecordPropertyToMetacardAttribute(): could not read BoundingBox.",
                        cswException);
            }

            LOGGER.debug("WKT = {}", (String) ser);
            break;
        case BINARY:

            try {
                ser = reader.getValue().getBytes(UTF8_ENCODING);
            } catch (UnsupportedEncodingException e) {
                LOGGER.warn("Error encoding the binary value into the metacard.", e);
            }

            break;
        default:
            break;
        }

        return ser;
    }
}
