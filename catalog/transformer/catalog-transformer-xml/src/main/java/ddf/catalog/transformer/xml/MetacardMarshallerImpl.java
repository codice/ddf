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
 */
package ddf.catalog.transformer.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.xerces.impl.dv.util.Base64;
import org.codice.ddf.parser.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.google.common.collect.ImmutableMap;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.api.MetacardMarshaller;
import ddf.catalog.transformer.api.PrintWriter;
import ddf.catalog.transformer.api.PrintWriterProvider;

public class MetacardMarshallerImpl implements MetacardMarshaller {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardMarshallerImpl.class);

    /*
     * This lookup map is...unfortunate. The current JAXB, which will remain in use for many
     * contexts until/unless we refactor and rewrite all XML processing, determines the attribute
     * names from the metacard schema. This lookup map provides an ugly shortcut for our purposes.
     */
    private static final Map<AttributeType.AttributeFormat, String> TYPE_NAME_LOOKUP;

    // see https://en.wikipedia.org/wiki/ISO_8601
    private static final String DF_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

    private static final String GML_PREFIX = "gml";

    private static final Map<String, String> NAMESPACE_MAP;

    public static final String OMIT_XML_DECL = "OMIT_XML_DECLARATION";

    static {
        TYPE_NAME_LOOKUP = new ImmutableMap.Builder<AttributeType.AttributeFormat, String>()
                .put(AttributeType.AttributeFormat.BINARY, "base64Binary")
                .put(AttributeType.AttributeFormat.STRING, "string")
                .put(AttributeType.AttributeFormat.BOOLEAN, "boolean")
                .put(AttributeType.AttributeFormat.DATE, "dateTime")
                .put(AttributeType.AttributeFormat.DOUBLE, "double")
                .put(AttributeType.AttributeFormat.SHORT, "short")
                .put(AttributeType.AttributeFormat.INTEGER, "int")
                .put(AttributeType.AttributeFormat.LONG, "long")
                .put(AttributeType.AttributeFormat.FLOAT, "float")
                .put(AttributeType.AttributeFormat.GEOMETRY, "geometry")
                .put(AttributeType.AttributeFormat.XML, "stringxml")
                .put(AttributeType.AttributeFormat.OBJECT, "object").build();

        String nsPrefix = "xmlns";

        NAMESPACE_MAP = new ImmutableMap.Builder<String, String>()
                .put(nsPrefix, "urn:catalog:metacard")
                .put(nsPrefix + ":" + GML_PREFIX, "http://www.opengis.net/gml")
                .put(nsPrefix + ":xlink", "http://www.w3.org/1999/xlink")
                .put(nsPrefix + ":smil", "http://www.w3.org/2001/SMIL20/")
                .put(nsPrefix + ":smillang", "http://www.w3.org/2001/SMIL20/Language").build();
    }

    private final GeometryTransformer geometryTransformer;

    private final PrintWriterProvider writerProvider;

    private static final String XML_DECL = "<?xml version=\'1.0\' encoding=\'UTF-8\'?>\n";

    public MetacardMarshallerImpl(Parser parser, PrintWriterProvider writerProvider) {
        this.geometryTransformer = new GeometryTransformer(parser);
        this.writerProvider = writerProvider;
    }

    @Override
    public String marshal(Metacard metacard)
            throws XmlPullParserException, IOException, CatalogTransformerException {

        return marshal(metacard, Collections.EMPTY_MAP);
    }

    @Override
    public String marshal(Metacard metacard, Map<String, Serializable> arguments)
            throws XmlPullParserException, IOException, CatalogTransformerException {
        PrintWriter writer = this.writerProvider.build(Metacard.class);

        Boolean omitXmlDec = (Boolean) arguments.get(OMIT_XML_DECL);
        if (omitXmlDec == null || !omitXmlDec) {
            writer.setRawValue(XML_DECL);
        }

        writer.startNode("metacard");
        for (Map.Entry<String, String> nsRow : NAMESPACE_MAP.entrySet()) {
            writer.addAttribute(nsRow.getKey(), nsRow.getValue());
        }

        if (metacard.getId() != null) {
            writer.addAttribute(GML_PREFIX + ":id", metacard.getId());
        }

        writer.startNode("type");
        if (StringUtils.isBlank(metacard.getMetacardType().getName())) {
            writer.setValue(MetacardType.DEFAULT_METACARD_TYPE_NAME);
        } else {
            writer.setValue(metacard.getMetacardType().getName());
        }
        writer.endNode(); // type

        if (StringUtils.isNotBlank(metacard.getSourceId())) {
            writer.startNode("source");
            writer.setValue(metacard.getSourceId());
            writer.endNode(); // source
        }

        // if multi-threading, cannot abstract XmlPullParser creation to class member.
        // xmlPullParser used only for geometry
        XmlPullParser xmlPullParser = XppFactory.createDefaultParser();

        Set<AttributeDescriptor> attributeDescriptors = metacard.getMetacardType()
                .getAttributeDescriptors();

        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
            String attributeName = attributeDescriptor.getName();
            if (attributeName.equals("id")) {
                continue;
            }

            Attribute attribute = metacard.getAttribute(attributeName);

            if (attribute != null) {
                AttributeType.AttributeFormat format = attributeDescriptor.getType()
                        .getAttributeFormat();
                writeAttributeToXml(writer, xmlPullParser, attribute, format);
            }
        }
        writer.endNode(); // metacard
        return writer.makeString();
    }

    private void writeAttributeToXml(PrintWriter writer, XmlPullParser parser, Attribute attribute,
            AttributeType.AttributeFormat format) throws IOException, CatalogTransformerException {
        String attributeName = attribute.getName();

        for (Serializable value : attribute.getValues()) {
            String xmlValue = null;

            switch (format) {

            case STRING:
            case BOOLEAN:
            case SHORT:
            case INTEGER:
            case LONG:
            case FLOAT:
            case DOUBLE:
                xmlValue = value.toString();
                break;
            case DATE:
                Date date = (Date) value;
                xmlValue = DateFormatUtils.formatUTC(date, DF_PATTERN);
                break;
            case GEOMETRY:
                xmlValue = geoToXml(geometryTransformer.transform(attribute), parser);
                break;
            case OBJECT:
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (ObjectOutput output = new ObjectOutputStream(bos)) {
                    output.writeObject(attribute.getValue());
                    xmlValue = Base64.encode(bos.toByteArray());
                }
                break;
            case BINARY:
                xmlValue = Base64.encode((byte[]) value);
                break;
            case XML:
                xmlValue = value.toString().replaceAll("[<][?]xml.*[?][>]", "");
                break;
            }

            // Write the node if we were able to convert it.
            if (xmlValue != null) {
                // The GeometryTransformer creates an XML fragment containing
                // both the name - with namespaces declared - and the value
                if (format != AttributeType.AttributeFormat.GEOMETRY) {
                    writer.startNode(TYPE_NAME_LOOKUP.get(format));
                    writer.addAttribute("name", attributeName);
                    writer.startNode("value");
                }

                if (format == AttributeType.AttributeFormat.XML
                        || format == AttributeType.AttributeFormat.GEOMETRY) {
                    writer.setRawValue(xmlValue);
                } else {
                    writer.setValue(xmlValue);
                }

                if (format != AttributeType.AttributeFormat.GEOMETRY) {
                    writer.endNode(); // value
                    writer.endNode(); // type
                }
            }
        }
    }

    private String geoToXml(BinaryContent content, XmlPullParser parser) {
        XppReader source = new XppReader(new InputStreamReader(content.getInputStream()), parser);

        // if multi-threading, cannot abstract PrintWriter to class member
        PrintWriter destination = writerProvider.build(Metacard.class);

        new HierarchicalStreamCopier().copy(source, destination);

        return destination.makeString();
    }
}
