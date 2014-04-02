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
package org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.catalog.common.converter.XmlNode;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import com.vividsolutions.jts.io.gml2.GMLReader;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;

public abstract class AbstractFeatureConverter implements FeatureConverter {

    private HierarchicalStreamCopier copier = new HierarchicalStreamCopier();

    private NoNameCoder noNameCoder = new NoNameCoder();

    protected String sourceId;

    protected String wfsUrl;

    protected String prefix;

    protected MetacardType metacardType;

    protected static final String FID = "fid";

    private final Set<String> basicAttributeNames = getBasicAttributeNames();

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureConverter.class);

    private static final String ERROR_PARSING_MESSAGE = "Error parsing Geometry from feature xml.";

    private static final String UTF8_ENCODING = "UTF-8";

    @Override
    public boolean canConvert(Class clazz) {
        return Metacard.class.isAssignableFrom(clazz);
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public void setWfsUrl(String url) {
        this.wfsUrl = url;
    }

    public void setMetacardType(MetacardType metacardType) {
        this.metacardType = metacardType;
        this.prefix = metacardType.getName() + WfsConstants.DECIMAL;
    }

    public MetacardType getMetacardType() {
        return this.metacardType;
    }

    protected HierarchicalStreamReader copyXml(HierarchicalStreamReader hreader, StringWriter writer) {
        copier.copy(hreader, new CompactWriter(writer, noNameCoder));

        StaxDriver driver = new WstxDriver();
        return driver.createReader(new ByteArrayInputStream(writer.toString().getBytes()));
    }

    protected Metacard createMetacardFromFeature(HierarchicalStreamReader hreader,
            MetacardType metacardType) {

        String propertyPrefix = metacardType.getName() + WfsConstants.DECIMAL;

        StringWriter metadataWriter = new StringWriter();
        HierarchicalStreamReader reader = copyXml(hreader, metadataWriter);

        MetacardImpl mc = new MetacardImpl(metacardType);
        mc.setContentTypeName(metacardType.getName());

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String name = propertyPrefix + reader.getNodeName();
            AttributeDescriptor attributeDescriptor = metacardType.getAttributeDescriptor(name);

            Serializable value = null;

            if (attributeDescriptor != null
                    && (StringUtils.isNotBlank(reader.getValue()) || BasicTypes.GEO_TYPE
                            .equals(attributeDescriptor.getType()))) {
                value = writeFeaturePropertyToMetacardAttribute(attributeDescriptor.getType()
                        .getAttributeFormat(), reader);

            }

            if (null != value) {
                mc.setAttribute(name, value);
                if (attributeDescriptor.getType().equals(BasicTypes.GEO_TYPE)) {
                    mc.setLocation((String) value);
                }
                // if this node matches a basic metacard attribute name,
                // populate that field as well
                if (isBasicMetacardAttribute(reader.getNodeName())) {
                    LOGGER.info("Setting metacard basic attribute: {} = {}", reader.getNodeName(),
                            value);
                    mc.setAttribute(reader.getNodeName(), value);
                }

            }

            reader.moveUp();
        }

        mc.setMetadata(metadataWriter.toString());

        try {
            if (metacardType instanceof FeatureMetacardType) {
                URI namespaceUri = new URI(((FeatureMetacardType) metacardType).getNamespaceURI());
                mc.setTargetNamespace(namespaceUri);
            }
        } catch (URISyntaxException e) {
            LOGGER.error("Error setting target namespace uri on metacard.  Exception {}", e);
        }

        return mc;
    }

    protected Serializable writeFeaturePropertyToMetacardAttribute(AttributeFormat attributeFormat,
            HierarchicalStreamReader reader) {

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
        case GEOMETRY:
            XmlNode node = new XmlNode(reader);

            GMLReader gmlReader = new GMLReader();
            Geometry geo = null;
            try {
                geo = gmlReader.read(node.toString(), null);
            } catch (SAXException e) {
                LOGGER.warn(ERROR_PARSING_MESSAGE, e);
            } catch (IOException e) {
                LOGGER.warn(ERROR_PARSING_MESSAGE, e);
            } catch (ParserConfigurationException e) {
                LOGGER.warn(ERROR_PARSING_MESSAGE, e);
            }
            if (geo != null) {
                WKTWriter wktWriter = new WKTWriter();
                ser = wktWriter.write(geo);
            }
            break;
        case BINARY:
            try {
                ser = reader.getValue().getBytes(UTF8_ENCODING);
            } catch (UnsupportedEncodingException e) {
                LOGGER.warn("Error encoding the binary value into the metacard.", e);
            }
            break;
        case DATE:
            ser = parseDateFromXml(reader);
            break;
        default:
            break;
        }
        return ser;

    }

    protected Date parseDateFromXml(HierarchicalStreamReader reader) {
        Date date = null;
        try { // trying to parse xsd:date
            date = DatatypeConverter.parseDate(reader.getValue()).getTime();
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Unable to parse date, attempting to parse as xsd:dateTime, Exception was {}", e);
            try { // try to parse it as a xsd:dateTime
                date = DatatypeConverter.parseDateTime(reader.getValue()).getTime();
            } catch (IllegalArgumentException ie) {
                LOGGER.warn(
                        "Unable to parse date from XML; defaulting \"{}\" to current datetime.  Exception {}",
                        reader.getNodeName(), ie);
                date = new Date();
            }
        }
        return date;
    }

    protected Boolean isAttributeNotNull(final String attributeName, Metacard mc) {
        return (mc.getAttribute(attributeName) != null && mc.getAttribute(attributeName).getValue() != null);
    }

    private Set<String> getBasicAttributeNames() {
        Set<String> attrNames = new HashSet<String>(BasicTypes.BASIC_METACARD
                .getAttributeDescriptors().size());
        for (AttributeDescriptor ad : BasicTypes.BASIC_METACARD.getAttributeDescriptors()) {
            attrNames.add(ad.getName());
        }
        return attrNames;
    }

    private boolean isBasicMetacardAttribute(String attrName) {
        return basicAttributeNames.contains(attrName);
    }

}
