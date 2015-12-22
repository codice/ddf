/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.reader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.UpdateAction;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.XStreamAttributeCopier;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswEndpoint;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import net.opengis.cat.csw.v_2_0_2.DeleteType;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;

/**
 */
public class TransactionRequestConverter implements Converter {
    private static final CswRecordMetacardType CSW_RECORD_METACARD_TYPE = new CswRecordMetacardType();

    private Converter inputTransformProvider;

    public TransactionRequestConverter(Converter itp) {
        this.inputTransformProvider = itp;
    }

    @Override
    public void marshal(Object o, HierarchicalStreamWriter hierarchicalStreamWriter,
            MarshallingContext marshallingContext) {

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        CswTransactionRequest cswTransactionRequest = new CswTransactionRequest();

        cswTransactionRequest.setVersion(reader.getAttribute(CswConstants.VERSION));
        cswTransactionRequest.setService(reader.getAttribute(CswConstants.SERVICE));
        cswTransactionRequest
                .setVerbose(Boolean.valueOf(reader.getAttribute(CswConstants.VERBOSE_RESPONSE)));

        XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            if (reader.getNodeName().contains("Insert")) {
                String typeName = StringUtils
                        .defaultIfEmpty(reader.getAttribute(CswConstants.TYPE_NAME_PARAMETER),
                                CswConstants.CSW_RECORD);
                String handle = StringUtils
                        .defaultIfEmpty(reader.getAttribute(CswConstants.HANDLE_PARAMETER), "");
                context.put(CswConstants.TYPE_NAME_PARAMETER, typeName);
                List<Metacard> metacards = new ArrayList<>();
                // Loop through the individual records to be inserted, converting each into a Metacard
                while (reader.hasMoreChildren()) {
                    reader.moveDown(); // move down to the record's tag
                    Metacard metacard = (Metacard) context
                            .convertAnother(null, MetacardImpl.class, inputTransformProvider);
                    if (metacard != null) {
                        metacards.add(metacard);
                    }

                    // move back up to the <SearchResults> parent of the <csw:Record> tags
                    reader.moveUp();
                }
                cswTransactionRequest.getInsertActions()
                        .add(new InsertAction(typeName, handle, metacards));
            } else if (reader.getNodeName().contains("Delete")) {
                XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);

                Map<String, String> xmlnsAttributeToUriMappings = getXmlnsAttributeToUriMappingsFromContext(
                        context);
                Map<String, String> prefixToUriMappings = getPrefixToUriMappingsFromXmlnsAttributes(
                        xmlnsAttributeToUriMappings);

                StringWriter writer = new StringWriter();
                XStreamAttributeCopier.copyXml(reader, writer, xmlnsAttributeToUriMappings);

                DeleteType deleteType = getElementFromXml(writer.toString(), DeleteType.class);

                cswTransactionRequest.getDeleteActions()
                        .add(new DeleteAction(deleteType, prefixToUriMappings));
            } else if (reader.getNodeName().contains("Update")) {
                XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);
                UpdateAction updateAction = parseUpdateAction(reader, context);
                cswTransactionRequest.getUpdateActions().add(updateAction);
            }
            reader.moveUp();
        }

        return cswTransactionRequest;
    }

    private UpdateAction parseUpdateAction(HierarchicalStreamReader reader,
            UnmarshallingContext context) {
        Map<String, String> xmlnsAttributeToUriMappings = getXmlnsAttributeToUriMappingsFromContext(
                context);
        Map<String, String> prefixToUriMappings = getPrefixToUriMappingsFromXmlnsAttributes(
                xmlnsAttributeToUriMappings);

        String typeName = StringUtils
                .defaultIfEmpty(reader.getAttribute(CswConstants.TYPE_NAME_PARAMETER),
                        CswConstants.CSW_RECORD);
        String handle = StringUtils
                .defaultIfEmpty(reader.getAttribute(CswConstants.HANDLE_PARAMETER), "");

        // Move down to the content of the <Update>.
        reader.moveDown();

        UpdateAction updateAction;

        // Do we have a list of <RecordProperty> elements or a new <csw:Record>?
        if (reader.getNodeName().contains("RecordProperty")) {
            Map<String, Serializable> cswRecordProperties = new HashMap<>();

            while (reader.getNodeName().contains("RecordProperty")) {
                String cswField;
                Serializable newValue = null;

                // Move down to the <Name>.
                reader.moveDown();
                if (reader.getNodeName().contains("Name")) {
                    String attribute = reader.getValue();
                    cswField = CswRecordConverter.getCswAttributeFromAttributeName(attribute);
                } else {
                    throw new ConversionException(
                            "Missing Parameter Value: missing a Name in a RecordProperty.");
                }
                // Move back up to the <RecordProperty>.
                reader.moveUp();

                // Is there a <Value>?
                if (reader.hasMoreChildren()) {
                    // Move down to the <Value>.
                    reader.moveDown();

                    if (reader.getNodeName().contains("Value")) {
                        newValue = getRecordPropertyValue(reader, cswField);
                    } else {
                        throw new ConversionException(
                                "Invalid Parameter Value: invalid element in a RecordProperty.");
                    }

                    // Back to the <RecordProperty>.
                    reader.moveUp();
                }

                cswRecordProperties.put(cswField, newValue);

                // Back to the <Update>, look for the next <RecordProperty>.
                reader.moveUp();

                if (!reader.hasMoreChildren()) {
                    // If there aren't any more children of the <Update>, that means there's no
                    // Constraint, which is required.
                    throw new ConversionException("Missing Parameter Value: missing a Constraint.");
                }

                // What's the next element in the <Update>?
                reader.moveDown();
            }

            // Now there should be a <Constraint> element.
            if (reader.getNodeName().contains("Constraint")) {
                StringWriter writer = new StringWriter();
                XStreamAttributeCopier.copyXml(reader, writer, xmlnsAttributeToUriMappings);

                QueryConstraintType constraint = getElementFromXml(writer.toString(),
                        QueryConstraintType.class);

                // For any CSW attributes that map to basic metacard attributes (e.g. title,
                // modified date, etc.), update the basic metacard attributes as well.
                Map<String, String> cswToMetacardAttributeNames = DefaultCswRecordMap
                        .getDefaultCswRecordMap().getCswToMetacardAttributeNames();
                Map<String, Serializable> cswRecordPropertiesWithMetacardAttributes = new HashMap<>(
                        cswRecordProperties);

                for (Entry<String, Serializable> recordProperty : cswRecordProperties.entrySet()) {
                    String cswAttributeName = recordProperty.getKey();

                    // If this CSW attribute maps to a basic metacard attribute, attempt to set the
                    // basic metacard attribute.
                    if (cswToMetacardAttributeNames.containsKey(cswAttributeName)) {
                        String metacardAttrName = cswToMetacardAttributeNames.get(cswAttributeName);
                        // If this basic metacard attribute hasn't already been set, set it.
                        if (!cswRecordPropertiesWithMetacardAttributes
                                .containsKey(metacardAttrName)) {
                            Attribute metacardAttr = CswRecordConverter
                                    .getMetacardAttributeFromCswAttribute(cswAttributeName,
                                            recordProperty.getValue(), metacardAttrName);
                            cswRecordPropertiesWithMetacardAttributes
                                    .put(metacardAttrName, metacardAttr.getValue());
                        }
                    }
                }

                updateAction = new UpdateAction(cswRecordPropertiesWithMetacardAttributes, typeName,
                        handle, constraint, prefixToUriMappings);
            } else {
                throw new ConversionException("Missing Parameter Value: missing a Constraint.");
            }
        } else if (reader.getNodeName().contains(CswConstants.CSW_RECORD)) {
            Metacard metacard = (Metacard) context
                    .convertAnother(null, MetacardImpl.class, inputTransformProvider);

            updateAction = new UpdateAction(metacard, typeName, handle);
            // Move back to the <Update>.
            reader.moveUp();
        } else {
            throw new ConversionException(
                    "Missing Parameter Value: missing a RecordProperty or an updated record.");
        }

        return updateAction;
    }

    private Serializable getRecordPropertyValue(HierarchicalStreamReader reader, String cswField) {
        AttributeDescriptor attributeDescriptor = CSW_RECORD_METACARD_TYPE
                .getAttributeDescriptor(cswField);

        if (attributeDescriptor != null) {
            try {
                Serializable newValue;
                if (reader.hasMoreChildren()) {
                    reader.moveDown();
                    newValue = CswRecordConverter.convertRecordPropertyToMetacardAttribute(
                            attributeDescriptor.getType().getAttributeFormat(), reader,
                            CswAxisOrder.LON_LAT);
                    reader.moveUp();
                } else {
                    newValue = CswRecordConverter.convertRecordPropertyToMetacardAttribute(
                            attributeDescriptor.getType().getAttributeFormat(), reader,
                            CswAxisOrder.LON_LAT);
                }

                return newValue;
            } catch (NumberFormatException e) {
                throw new ConversionException("Invalid Parameter Value: a RecordProperty " +
                        "specified a Value that does not match the type " +
                        attributeDescriptor.getType().getBinding() + " expected by " +
                        CSW_RECORD_METACARD_TYPE.getName() + " for the field " + cswField, e);
            }
        } else {
            throw new ConversionException("Invalid Parameter Value: a RecordProperty specified " +
                    cswField + " as the Name, which is not a valid record attribute for " +
                    CSW_RECORD_METACARD_TYPE.getName());
        }
    }

    private <T> T getElementFromXml(String xml, Class<T> clazz) {
        JAXBElement<T> root;

        try {
            JAXBContext jaxbContext = CswEndpoint.getJaxBContext();
            InputStream xmlInputStream = IOUtils.toInputStream(xml, StandardCharsets.UTF_8.name());
            StreamSource xmlStreamSource = new StreamSource(xmlInputStream);
            root = jaxbContext.createUnmarshaller().unmarshal(xmlStreamSource, clazz);
        } catch (IOException | JAXBException e) {
            throw new ConversionException(e);
        }

        return root.getValue();
    }

    private Map<String, String> getXmlnsAttributeToUriMappingsFromContext(
            UnmarshallingContext context) {
        Object namespaceObj = context.get(CswConstants.NAMESPACE_DECLARATIONS);
        if (namespaceObj instanceof Map<?, ?>) {
            return (Map<String, String>) namespaceObj;
        }

        return null;
    }

    private Map<String, String> getPrefixToUriMappingsFromXmlnsAttributes(
            Map<String, String> xmlnsAttributeToUriMappings) {
        if (xmlnsAttributeToUriMappings != null) {
            // The xmlns attributes on the top-level Transaction element have been copied
            // into the UnmarshallingContext by
            // XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext().
            Map<String, String> prefixToUriMappings = new HashMap<>();
            for (Entry<String, String> entry : xmlnsAttributeToUriMappings.entrySet()) {
                String xmlnsAttribute = entry.getKey();
                if (StringUtils.contains(xmlnsAttribute, CswConstants.NAMESPACE_DELIMITER)) {
                    String prefix = xmlnsAttribute.split(CswConstants.NAMESPACE_DELIMITER)[1];
                    prefixToUriMappings.put(prefix, entry.getValue());
                }
            }
            return prefixToUriMappings;
        }

        return DefaultCswRecordMap.getDefaultCswRecordMap().getPrefixToUriMapping();
    }

    @Override
    public boolean canConvert(Class aClass) {
        return CswTransactionRequest.class.isAssignableFrom(aClass);
    }
}
