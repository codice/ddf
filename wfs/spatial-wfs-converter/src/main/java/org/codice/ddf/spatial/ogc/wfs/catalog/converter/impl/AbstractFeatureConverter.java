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

import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.B;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_GB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_KB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_MB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_PB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.BYTES_PER_TB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.GB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.KB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.MB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.PB;
import static org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants.TB;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
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
    
    private MetacardMapper metacardMapper = null;

    protected String sourceId;

    protected String wfsUrl;

    protected String prefix;

    protected MetacardType metacardType;
    
    protected String coordinateOrder;

    protected static final String FID = "fid";

    private final Set<String> basicAttributeNames = getBasicAttributeNames();

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureConverter.class);

    protected static final String ERROR_PARSING_MESSAGE = "Error parsing Geometry from feature xml.";

    protected static final String UTF8_ENCODING = "UTF-8";
    
    public AbstractFeatureConverter(){
    	
    }
    
    public AbstractFeatureConverter(MetacardMapper metacardMapper){
    	this.metacardMapper= metacardMapper;
    }

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
        this.prefix = metacardType.getName() + ".";
    }

    public MetacardType getMetacardType() {
        return this.metacardType;
    }
    
    public void setCoordinateOrder(String coordinateOrder) {
        this.coordinateOrder = coordinateOrder;
    }

    protected HierarchicalStreamReader copyXml(HierarchicalStreamReader hreader, StringWriter writer) {
        copier.copy(hreader, new CompactWriter(writer, noNameCoder));

        StaxDriver driver = new WstxDriver();
        return driver.createReader(new ByteArrayInputStream(writer.toString().getBytes()));
    }

    protected Metacard createMetacardFromFeature(HierarchicalStreamReader hreader,
            MetacardType metacardType) {
        String propertyPrefix = metacardType.getName() + ".";
        StringWriter metadataWriter = new StringWriter();
        HierarchicalStreamReader reader = copyXml(hreader, metadataWriter);
        MetacardImpl mc = new MetacardImpl(metacardType);
        mc.setContentTypeName(metacardType.getName());
        
        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String featureProperty = propertyPrefix + reader.getNodeName();
            AttributeDescriptor attributeDescriptor = metacardType.getAttributeDescriptor(featureProperty);                      
           
            //Check MetacardMapper for mappings of incoming values
            String mappedMetacardAttribute = null;
            if (metacardMapper != null) {
                LOGGER.debug("Looking up metacard attribute for feature property {} using metacard mapper", featureProperty);
                mappedMetacardAttribute = metacardMapper.getMetacardAttribute(featureProperty);
                LOGGER.debug("Found metacard attribute {} for feature property {}", mappedMetacardAttribute, featureProperty);
            }
            
            Serializable value = null;
            if (attributeDescriptor != null
                    && (StringUtils.isNotBlank(reader.getValue())
                            || BasicTypes.GEO_TYPE.getAttributeFormat().equals(
                                    attributeDescriptor.getType().getAttributeFormat()) || BasicTypes.DATE_TYPE
                            .getAttributeFormat().equals(
                                    attributeDescriptor.getType().getAttributeFormat()))) {
                if (StringUtils.isNotBlank(mappedMetacardAttribute)) {
                    if (StringUtils.equals(mappedMetacardAttribute, Metacard.RESOURCE_SIZE)) {
                        String sizeBeforeConversion = reader.getValue();
                        String bytes = convertToBytes(reader, metacardMapper.getDataUnit());
                        if (StringUtils.isNotBlank(bytes)) {
                            LOGGER.debug("Setting mapped metacard attribute {} with value {}", mappedMetacardAttribute, bytes);
                            mc.setAttribute(mappedMetacardAttribute, bytes);
                        }
                        if(StringUtils.isNotBlank(sizeBeforeConversion)) {
                            LOGGER.debug("Setting metacard attribute {} with value {}", featureProperty, sizeBeforeConversion);
                            mc.setAttribute(featureProperty, sizeBeforeConversion);
                        }
                    } else {
                        value = getValueForMetacardAttribute(attributeDescriptor.getType()
                                .getAttributeFormat(), reader);
                        if (value != null) {
                            LOGGER.debug("Setting mapped metacard attribute {} with value {}", mappedMetacardAttribute, value);
                            mc.setAttribute(mappedMetacardAttribute, value);
                            mc.setAttribute(featureProperty, value);
                        }
                    }
                } else {
                    value = getValueForMetacardAttribute(attributeDescriptor.getType()
                            .getAttributeFormat(), reader);

                    if (value != null) {
                        LOGGER.debug("Setting metacard attribute {} with value {}",
                                featureProperty, value);
                        mc.setAttribute(featureProperty, value);
                    }
                }
                    if (BasicTypes.GEO_TYPE.getAttributeFormat().equals(
                            attributeDescriptor.getType().getAttributeFormat())) {
                        mc.setLocation((String) value);
                    }
                    // if this node matches a basic metacard attribute name,
                    // populate that field as well
                    if (isBasicMetacardAttribute(reader.getNodeName())) {
                        LOGGER.debug("Setting metacard basic attribute: {} = {}",
                                reader.getNodeName(), value);

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

    protected Serializable getValueForMetacardAttribute(AttributeFormat attributeFormat,
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
            String xml = node.toString();
            GMLReader gmlReader = new GMLReader();
            Geometry geo = null;
            try {
                geo = gmlReader.read(xml, null);
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

    private String convertToBytes(HierarchicalStreamReader reader, String unit) {

        BigDecimal resourceSize = new BigDecimal(reader.getValue());
        resourceSize.setScale(1, BigDecimal.ROUND_HALF_UP);

        switch (unit) {
        case B:
            break;
        case KB:
            resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_KB));
            break;
        case MB:
            resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_MB));
            break;
        case GB:
            resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_GB));
            break;
        case TB:
            resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_TB));
            break;
        case PB:
            resourceSize = resourceSize.multiply(new BigDecimal(BYTES_PER_PB));
            break;
        }

        String resourceSizeAsString = resourceSize.toPlainString();
        LOGGER.debug("resource size in bytes: {}", resourceSizeAsString);
        return resourceSizeAsString;
    }
    
    protected Date parseDateFromXml(HierarchicalStreamReader reader) {
        Date date = null;
        boolean processingChildNode = false;

        if (reader.hasMoreChildren()) {
            reader.moveDown();
            processingChildNode = true;
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("node name: {}", reader.getNodeName());
            LOGGER.debug("value: {}", reader.getValue());
        }
        if (StringUtils.isBlank(reader.getValue())) {
            date = null;
            if (processingChildNode) {
                reader.moveUp();
            }
            return date;
        }

        try { // trying to parse xsd:date
            date = DatatypeConverter.parseDate(reader.getValue()).getTime();
        } catch (IllegalArgumentException e) {
            LOGGER.debug(
                    "Unable to parse date, attempting to parse as xsd:dateTime, Exception was {}",
                    e);
            try { // try to parse it as a xsd:dateTime
                date = DatatypeConverter.parseDateTime(reader.getValue()).getTime();
            } catch (IllegalArgumentException ie) {
                LOGGER.debug(
                        "Unable to parse date from XML; defaulting \"{}\" to current datetime.  Exception {}",
                        reader.getNodeName(), ie);
                date = new Date();
            }
        }

        if (processingChildNode) {
            reader.moveUp();
            processingChildNode = false;
        }
        LOGGER.debug("node name: {}", reader.getNodeName());
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
