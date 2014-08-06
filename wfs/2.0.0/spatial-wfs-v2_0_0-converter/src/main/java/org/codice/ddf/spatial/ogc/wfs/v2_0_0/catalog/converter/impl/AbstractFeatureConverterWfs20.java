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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.converter.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.codice.ddf.spatial.ogc.catalog.common.converter.XmlNode;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.AbstractFeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import ddf.catalog.data.AttributeType.AttributeFormat;

public abstract class AbstractFeatureConverterWfs20 extends AbstractFeatureConverter implements FeatureConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFeatureConverterWfs20.class);
    private static final String XML_PARSE_FAILURE = "Failed to parse GML based XML into a Document.";
    private static final String CREATE_TRANSFORMER_FAILURE = "Failed to create Transformer.";
    private static final String GML_GEOMETRY_FAILURE = "Failed to transform GML to Geometry.\n";
    private String geometryXml = "";

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
            geometryXml = node.toString();
            Geometry geo = null;
            geo = readGml(geometryXml);
            
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
    
    private Geometry readGml(String xml) {
        //Add namespace into XML for processing
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        Document doc = null;
        InputStream xmlIs = null;

        //Check if GML 3.2.1 namespace exist on XML chunk
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = dBuilder.parse(is);  
        } catch (ParserConfigurationException e) {
            LOGGER.error(XML_PARSE_FAILURE);
        } catch (SAXException e) {
            LOGGER.error(XML_PARSE_FAILURE);
        } catch (IOException e) {
            LOGGER.error(XML_PARSE_FAILURE);
        }
      
        String[] namePrefix = doc.getDocumentElement().getNodeName().split(":");
        String prefix = "";
        if (namePrefix.length < 2) {
            LOGGER.debug("Incoming XML has no GML prefix");    
        } else {
            prefix = ":" + namePrefix[0];
        }
        
        String xmlNs = doc.getDocumentElement().getAttribute("xmlns" + prefix);
        if (xmlNs.equals(Wfs20Constants.GML_3_2_NAMESPACE)) {
            LOGGER.warn("Namespace already exists.");
        } else {
            //Add GML 3.2.1 namespace to XML chunk
            doc.getDocumentElement().setAttribute("xmlns:" + prefix, Wfs20Constants.GML_3_2_NAMESPACE);
        }
        
        //Convert DOM to InputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Source xmlSource = new DOMSource(doc);
        Result outputTarget = new StreamResult(outputStream);
        try {
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
        } catch (TransformerConfigurationException e) {
            LOGGER.error(CREATE_TRANSFORMER_FAILURE);
        } catch (TransformerException e) {
            LOGGER.error(CREATE_TRANSFORMER_FAILURE);
        } catch (TransformerFactoryConfigurationError e) {
            LOGGER.error(CREATE_TRANSFORMER_FAILURE);
        }

        xmlIs = new ByteArrayInputStream(outputStream.toByteArray());
        
        //Parse XML into a Geometry object
        Configuration configurationG = new org.geotools.gml3.v3_2.GMLConfiguration();
        Parser parser = new org.geotools.xml.Parser(configurationG);
        parser.setStrict(false);
        parser.setValidating(false);
        parser.setFailOnValidationError(false);
        parser.setForceParserDelegate(false);

        Geometry geo = null;
        try {
            geo = (Geometry)parser.parse(xmlIs);
        } catch (IOException e) {
            LOGGER.error("{} {}", GML_GEOMETRY_FAILURE, geometryXml);
        } catch (SAXException e) {
            LOGGER.error("{} {}", GML_GEOMETRY_FAILURE, geometryXml);
        } catch (ParserConfigurationException e) {
            LOGGER.error("{} {}", GML_GEOMETRY_FAILURE, geometryXml);
        }
        
        return geo;
    }
}
