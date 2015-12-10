/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;
import javax.xml.namespace.QName;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;

/**
 * Converts CSW Record to a Metacard.
 *
 * @author rodgersh
 */

public class CswRecordConverter implements Converter, MetacardTransformer, InputTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswRecordConverter.class);

    private XStream xstream;

    public CswRecordConverter() {
        xstream = new XStream(new Xpp3Driver());
        xstream.setClassLoader(this.getClass().getClassLoader());
        xstream.registerConverter(this);
        xstream.alias(CswConstants.CSW_RECORD_LOCAL_NAME, Metacard.class);
        xstream.alias(CswConstants.CSW_RECORD, Metacard.class);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return Metacard.class.isAssignableFrom(clazz);
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        if (source == null || !(source instanceof Metacard)) {
            LOGGER.warn("Failed to marshal Metacard: {}", source);
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

        if ((fieldsToWrite == null || fieldsToWrite
                .contains(CswRecordMetacardType.CSW_TEMPORAL_QNAME))
                && metacard.getEffectiveDate() != null && metacard.getExpirationDate() != null) {
            CswMarshallHelper.writeTemporalData(writer, context, metacard);
        }

        if ((fieldsToWrite == null || fieldsToWrite
                .contains(CswRecordMetacardType.CSW_SOURCE_QNAME))
                && metacard.getSourceId() != null) {
            CswMarshallHelper
                    .writeValue(writer, context, null, CswRecordMetacardType.CSW_PUBLISHER_QNAME,
                            metacard.getSourceId());
        }

        if (fieldsToWrite == null || fieldsToWrite
                .contains(CswRecordMetacardType.OWS_BOUNDING_BOX_QNAME)) {
            CswMarshallHelper.writeBoundingBox(writer, context, metacard);
        }

        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, String> cswAttrMap = new CaseInsensitiveMap(
                DefaultCswRecordMap.getDefaultCswRecordMap().getCswToMetacardAttributeNames());
        Object mappingObj = context.get(CswConstants.CSW_MAPPING);

        if (mappingObj instanceof Map<?, ?>) {
            CswUnmarshallHelper.removeExistingAttributes(cswAttrMap,
                    (Map<String, String>) mappingObj);
        }

        String resourceUriMapping = (isString(context.get(Metacard.RESOURCE_URI))) ?
                (String) context.get(Metacard.RESOURCE_URI) :
                null;

        String thumbnailMapping = (isString(context.get(Metacard.THUMBNAIL))) ?
                (String) context.get(Metacard.THUMBNAIL) :
                null;

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

        Metacard metacard = CswUnmarshallHelper.createMetacardFromCswRecord(reader, cswAttrMap,
                resourceUriMapping, thumbnailMapping, cswAxisOrder, namespaceMap);

        Object sourceIdObj = context.get(Metacard.SOURCE_ID);

        if (sourceIdObj instanceof String) {
            metacard.setSourceId((String) sourceIdObj);
        }

        return metacard;
    }

    private boolean isString(Object object) {
        return object instanceof String;
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
                metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
            }
        } catch (XStreamException e) {
            throw new CatalogTransformerException(
                    "Unable to transform from CSW Record to Metacard.", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

        if (metacard == null) {
            throw new CatalogTransformerException(
                    "Unable to transform from CSW Record to Metacard.");
        }

        return metacard;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        StringWriter stringWriter = new StringWriter();
        Boolean omitXmlDec = (Boolean) arguments.get(CswConstants.OMIT_XML_DECLARATION);

        if (omitXmlDec == null || !omitXmlDec) {
            stringWriter.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        }

        PrettyPrintWriter writer = new PrettyPrintWriter(stringWriter);
        MarshallingContext context = new TreeMarshaller(writer, null, null);
        context.put(CswConstants.WRITE_NAMESPACES, true);
        copyArgumentsToContext(context, arguments);

        this.marshal(metacard, writer, context);

        BinaryContent transformedContent = null;

        ByteArrayInputStream bais = new ByteArrayInputStream(stringWriter.toString().getBytes(
                StandardCharsets.UTF_8));
        transformedContent = new BinaryContentImpl(bais, new MimeType());
        return transformedContent;
    }

    private void copyArgumentsToContext(MarshallingContext context,
            Map<String, Serializable> arguments) {

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
     * @return the value that was extracted from {@code reader} and is of the type described by
     *         {@code attributeFormat}
     */
    public static Serializable convertStringValueToMetacardValue(AttributeType.AttributeFormat attributeFormat,
            String value) {
        return CswUnmarshallHelper.convertStringValueToMetacardValue(attributeFormat, value);
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
    public static Serializable convertRecordPropertyToMetacardAttribute(
            AttributeType.AttributeFormat attributeFormat, HierarchicalStreamReader reader,
            CswAxisOrder cswAxisOrder) {
        return CswUnmarshallHelper.convertRecordPropertyToMetacardAttribute(attributeFormat, reader, cswAxisOrder);
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
    public static Attribute getMetacardAttributeFromCswAttribute(String cswAttributeName,
            Serializable cswAttributeValue, String metacardAttributeName) {
        return CswUnmarshallHelper.getMetacardAttributeFromCswAttribute(cswAttributeName, cswAttributeValue, metacardAttributeName);
    }

    /**
     * Converts an attribute name to the csw:Record attribute it corresponds to.
     *
     * @param attributeName  the name of the attribute
     * @return the name of the csw:Record attribute that this attribute name corresponds to
     */
    public static String getCswAttributeFromAttributeName(String attributeName) {
        return CswUnmarshallHelper.getCswAttributeFromAttributeName(attributeName);
    }
}