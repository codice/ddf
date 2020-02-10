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
package org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppFactory;
import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.ParserConfigurationException;
import org.codice.ddf.spatial.ogc.catalog.common.converter.XmlNode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.gml2.GMLReader;
import org.locationtech.jts.io.gml2.GMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class GmlGeometryConverter implements Converter {

  private static final Logger LOGGER = LoggerFactory.getLogger(GmlGeometryConverter.class);

  private static final String ERROR_SERIALIZING_MSG = "Failed to serialize GML from Geometry.";

  private static final String ERROR_PARSING_MSG = "Error parsing Geometry from feature xml.";

  @Override
  public boolean canConvert(Class clazz) {
    return Geometry.class.isAssignableFrom(clazz);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    Geometry geometry = (Geometry) value;
    GMLWriter gmlWriter = new GMLWriter();
    String gmlXml = gmlWriter.write(geometry);
    // Copy the GML XML into the writer
    XmlPullParser parser = null;
    try {
      parser = XppFactory.createDefaultParser();
      new HierarchicalStreamCopier().copy(new XppReader(new StringReader(gmlXml), parser), writer);
    } catch (XmlPullParserException e) {
      LOGGER.debug(ERROR_SERIALIZING_MSG, e);
    }
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    XmlNode gmlNode = new XmlNode(reader);
    GMLReader gmlReader = new GMLReader();
    Geometry geo = null;
    try {
      geo = gmlReader.read(gmlNode.toString(), null);
    } catch (SAXException | IOException | ParserConfigurationException e) {
      LOGGER.debug(ERROR_PARSING_MSG, e);
    }
    if (geo != null) {
      WKTWriter wktWriter = new WKTWriter();
      return wktWriter.write(geo);
    }
    return null;
  }
}
