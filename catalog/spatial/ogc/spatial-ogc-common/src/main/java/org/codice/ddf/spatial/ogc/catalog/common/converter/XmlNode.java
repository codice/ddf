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
package org.codice.ddf.spatial.ogc.catalog.common.converter;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.Iterator;
import org.apache.commons.lang.StringUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlNode {

  private static final Logger LOGGER = LoggerFactory.getLogger(XmlNode.class);

  private static final String GT = ">";

  private static final String LT = "<";

  private static final String CLOSE_TAG = "</";

  private static final String SPACE = " ";

  private HierarchicalStreamReader reader;

  private StringBuilder buffer;

  private String node;

  public XmlNode(HierarchicalStreamReader reader) {
    this.reader = reader;
    this.buffer = new StringBuilder();
  }

  public static void writeGeometry(
      String attributeName,
      MarshallingContext context,
      HierarchicalStreamWriter writer,
      Geometry geo) {
    if (null != geo) {
      writer.startNode(attributeName);
      context.convertAnother(geo);
      writer.endNode();
    }
  }

  public static void writeEnvelope(
      String attributeName,
      MarshallingContext context,
      HierarchicalStreamWriter writer,
      Envelope envelope) {
    if (null != envelope) {
      writer.startNode(attributeName);
      context.convertAnother(envelope);
      writer.endNode();
    }
  }

  public static Geometry readGeometry(String value) {
    WKTReader wktReader = new WKTReader();
    Geometry geo = null;
    try {
      geo = wktReader.read(value);
    } catch (ParseException e) {
      LOGGER.debug("Failed to parse geometry information.", e);
    }
    return geo;
  }

  /*
   * This method is intended to reconstruct XML nodes from an XMLStreamReader
   */
  private String reconstructNode() {
    if (node != null) {
      return node;
    }
    reader.moveDown();
    buffer.append(LT);
    String nodeName = reader.getNodeName();
    buffer.append(nodeName);
    // Add all attributes
    Iterator<String> iter = reader.getAttributeNames();
    while (iter.hasNext()) {
      buffer.append(SPACE);
      String attrName = iter.next();
      buffer.append(attrName);
      buffer.append("=\"");
      buffer.append(reader.getAttribute(attrName));
      buffer.append("\"");
    }
    buffer.append(GT);
    String value = reader.getValue();
    if (StringUtils.isBlank(value)) {
      // Check if this has children
      if (reader.hasMoreChildren()) {
        reconstructNode();
        // Now close this node
        buffer.append(CLOSE_TAG);
        buffer.append(nodeName);
        buffer.append(GT);
      }
    } else {
      // add the value
      buffer.append(value);
      // Now close this node
      buffer.append(CLOSE_TAG);
      buffer.append(nodeName);
      buffer.append(GT);
    }
    reader.moveUp();
    node = buffer.toString();
    return node;
  }

  @Override
  public String toString() {
    return (reader == null) ? super.toString() : reconstructNode();
  }
}
