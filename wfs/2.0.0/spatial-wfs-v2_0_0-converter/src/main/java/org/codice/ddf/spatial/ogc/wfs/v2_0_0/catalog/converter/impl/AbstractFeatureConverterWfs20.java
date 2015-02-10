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
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.AbstractFeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateFilter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import ddf.catalog.data.AttributeType.AttributeFormat;

public abstract class AbstractFeatureConverterWfs20 extends AbstractFeatureConverter
        implements FeatureConverter {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(AbstractFeatureConverterWfs20.class);

    private static final String XML_PARSE_FAILURE = "Failed to parse GML based XML into a Document.";

    private static final String CREATE_TRANSFORMER_FAILURE = "Failed to create Transformer.";

    private static final String GML_FAILURE = "Failed to transform GML.\n";

    public AbstractFeatureConverterWfs20() {

    }

    public AbstractFeatureConverterWfs20(MetacardMapper metacardMapper) {
        super(metacardMapper);
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
            String geometryXml = node.toString();
            Geometry geo = null;
            geo = (Geometry) readGml(geometryXml);

            LOGGER.debug("coordinateOrder = {}", coordinateOrder);
            if (WfsConstants.LAT_LON_ORDER.equals(coordinateOrder)) {
                swapCoordinates(geo);
            }

            if (geo != null) {
                WKTWriter wktWriter = new WKTWriter();
                ser = wktWriter.write(geo);
                LOGGER.debug("wkt = {}", ser);
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

    protected Object readGml(String xml) {
        LOGGER.debug("readGml() input XML: {}", xml);
        //Add namespace into XML for processing
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        Document doc = null;
        Object gml = null;
        InputStream xmlIs = null;

        //Check if GML 3.2.1 namespace exist on XML chunk
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = dBuilder.parse(is);
        } catch (ParserConfigurationException|SAXException|IOException e) {
            LOGGER.error(XML_PARSE_FAILURE);
        }

        if(null != doc) {
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
                doc.createElementNS(Wfs20Constants.GML_3_2_NAMESPACE,
                        doc.getDocumentElement().getNodeName());

            }
            //Convert DOM to InputStream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(doc);
            Result outputTarget = new StreamResult(outputStream);
            try {
                TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            } catch (TransformerException | TransformerFactoryConfigurationError e) {
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

            try {
                gml = parser.parse(xmlIs);
            } catch (IOException | SAXException | ParserConfigurationException e) {
                LOGGER.error("{} {}", GML_FAILURE, xml);
            }
        }

        return gml;
    }

    private void swapCoordinates(Geometry geo) {
        LOGGER.debug("Swapping Lat/Lon Coords to Lon/Lat using Geometry");

        geo.apply(new CoordinateFilter() {

            @Override
            public void filter(Coordinate coordinate) {
                double x = coordinate.x;
                double y = coordinate.y;
                coordinate.y = x;
                coordinate.x = y;
            }

        });

        geo.geometryChanged();
    }
}
