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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;

/**
 * Class to determine what transformer to use based on the schema and transforms the data appropriately.
 */
public class CswTransformProvider implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswTransformProvider.class);

    private TransformerManager metacardTransformerManager;

    private TransformerManager inputTransformerManager;

    public CswTransformProvider(TransformerManager metacardTransformers,
            TransformerManager inputTransformers) {
        this.metacardTransformerManager = metacardTransformers;
        this.inputTransformerManager = inputTransformers;
    }

    /**
     * Marshals Metacards to an xml. This method is not typically be called directly, instead it is
     * called by another XStream Converter using MarshallingContext.convertAnother();
     *
     * @param o       - metacard to transform.
     * @param writer  - writes the XML.
     * @param context - the marshalling context. Should contain a map entry for {@link
     *                org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants.OUTPUT_SCHEMA_PARAMETER}
     *                to identify which transformer to use. Also contains properties for any
     *                arguments to provide the transformer.
     */
    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext context) {
        if (o == null) {
            return;
        }
        Metacard metacard = (Metacard) o;
        Object arg = context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER);
        MetacardTransformer transformer = null;

        if (arg != null && StringUtils.isNotBlank((String) arg)) {
            String outputSchema = (String) arg;
            transformer = metacardTransformerManager.getTransformerBySchema(outputSchema);
        } else {
            transformer = metacardTransformerManager
                    .getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA);
        }

        if (transformer == null) {
            throw new ConversionException(
                    "Unable to locate a transformer for output schema: " + arg);
        }

        BinaryContent content = null;
        try {
            content = transformer.transform(metacard, getArguments(context));
        } catch (CatalogTransformerException e) {
            throw new ConversionException("Unable to transform Metacard", e);
        }

        writeXml(content, writer);
    }

    private void writeXml(BinaryContent content, HierarchicalStreamWriter writer) {
        try {
            XmlPullParser parser = XppFactory.createDefaultParser();
            new HierarchicalStreamCopier()
                    .copy(new XppReader(new InputStreamReader(content.getInputStream(), StandardCharsets.UTF_8), parser),
                            writer);
        } catch (XmlPullParserException e) {
            throw new ConversionException("Unable to copy metadata to XML Output.", e);
        }

    }

    private Map<String, Serializable> getArguments(DataHolder holder) {
        Map<String, Serializable> arguments = new HashMap<>();
        Iterator<Object> contextIterator = holder.keys();
        while (contextIterator.hasNext()) {
            Object key = contextIterator.next();
            if (key instanceof String) {
                Object value = holder.get(key);
                if (value instanceof Serializable) {
                    arguments.put((String) key, (Serializable) value);
                }
            }
        }
        return arguments;
    }

    /**
     * Creates a Metacard from the given XML. This method is not typically be called directly, instead it is
     * called by another XStream Converter using UnmarshallingContext.convertAnother();
     *
     * @param reader
     * @param context
     * @return
     */
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Object outputSchema = context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER);
        Object typeName = context.get(CswConstants.TYPE_NAME_PARAMETER);
        InputTransformer transformer = null;
        if (StringUtils.equals(CswConstants.CSW_OUTPUT_SCHEMA, (String) outputSchema) || StringUtils
                .equals(CswConstants.CSW_RECORD, (String) typeName) ||
                (outputSchema == null && typeName == null)) {
            transformer = inputTransformerManager
                    .<InputTransformer>getTransformerBySchema(CswConstants.CSW_OUTPUT_SCHEMA);
            if (transformer != null) {
                return ((CswRecordConverter) transformer).unmarshal(reader, context);
            }
        } else if (outputSchema != null) {
            String outputSchemaStr = (String) outputSchema;
            transformer = inputTransformerManager
                    .<InputTransformer>getTransformerBySchema(outputSchemaStr);
        } else {
            String typeNameStr = (String) typeName;
            transformer = inputTransformerManager.<InputTransformer>getTransformerById(typeNameStr);
        }

        if (transformer == null) {
            throw new ConversionException(
                    "Unable to locate a transformer for output schema: " + outputSchema);
        }

        Metacard metacard = null;
        try (InputStream is = readXml(reader, context)) {
            InputStream inputStream = is;
            if (LOGGER.isDebugEnabled()) {
                String originalInputStream = IOUtils
                        .toString(inputStream, StandardCharsets.UTF_8.name());
                LOGGER.debug("About to transform\n{}", originalInputStream);
                inputStream = new ByteArrayInputStream(
                        originalInputStream.getBytes(StandardCharsets.UTF_8.name()));
            }
            metacard = transformer.transform(inputStream);
        } catch (IOException | CatalogTransformerException e) {
            throw new ConversionException("Unable to transform Metacard", e);
        }
        return metacard;
    }

    private InputStream readXml(HierarchicalStreamReader reader, UnmarshallingContext context)
            throws IOException {
        InputStream is = null;

        Map<String, String> namespaces = null;
        Object namespaceObj = context.get(CswConstants.NAMESPACE_DECLARATIONS);
        if (namespaceObj instanceof Map<?, ?>) {
            namespaces = (Map<String, String>) namespaceObj;
        }

        StringWriter writer = new StringWriter();
        XStreamAttributeCopier.copyXml(reader, writer, namespaces);
        return IOUtils.toInputStream(writer.toString(), StandardCharsets.UTF_8.name());
    }

    @Override
    public boolean canConvert(Class clazz) {
        return Metacard.class.isAssignableFrom(clazz);
    }
}
