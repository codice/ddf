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
import org.locationtech.jts.geom.Envelope;

public class GmlEnvelopeConverter implements Converter {

  private static final String BOX_NODE_NAME = "gml:Box";

  private static final String COORDINATE_NODE_NAME = "gml:coordinates";

  private static final String SRS_NAME = "srsName";

  private static final String SRS_VALUE = "EPSG:4326";

  @Override
  public boolean canConvert(Class clazz) {
    return Envelope.class.isAssignableFrom(clazz);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    Envelope envelope = (Envelope) value;

    StringBuilder boxString = new StringBuilder();
    boxString
        .append(envelope.getMinX())
        .append(",")
        .append(envelope.getMinY())
        .append(" ")
        .append(envelope.getMaxX())
        .append(",")
        .append(envelope.getMaxY());

    writer.startNode(BOX_NODE_NAME);
    writer.addAttribute(SRS_NAME, SRS_VALUE);
    writer.startNode(COORDINATE_NODE_NAME);
    writer.setValue(boxString.toString());
    writer.endNode();
    writer.endNode();
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    return null;
  }
}
