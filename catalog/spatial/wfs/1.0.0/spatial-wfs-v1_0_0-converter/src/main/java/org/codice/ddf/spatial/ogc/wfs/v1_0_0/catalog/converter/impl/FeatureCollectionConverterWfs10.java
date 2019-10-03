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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.converter.impl;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import org.codice.ddf.spatial.ogc.catalog.common.converter.XmlNode;
import org.codice.ddf.spatial.ogc.wfs.catalog.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollectionImpl;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsQnameBuilder;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class works in conjunction with XStream to convert a {@link WfsFeatureCollection} to XML
 * according to the GML 3.2.1 spec. It will also convert respective XML into a {@link
 * WfsFeatureCollection}.
 */
public class FeatureCollectionConverterWfs10 implements Converter {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureCollectionConverterWfs10.class);

  private String featureMember;

  private Map<String, String> prefixToUriMapping = new HashMap<>();

  private String contextRoot;

  private Map<String, FeatureConverter> featureConverterMap = new HashMap<>();

  public FeatureCollectionConverterWfs10() {
    featureMember = "featureMember";
    prefixToUriMapping.put(Wfs10Constants.XSI_PREFIX, XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    prefixToUriMapping.put(Wfs10Constants.WFS_NAMESPACE_PREFIX, Wfs10Constants.WFS_NAMESPACE);
    prefixToUriMapping.put(Wfs10Constants.GML_PREFIX, Wfs10Constants.GML_NAMESPACE);
  }

  @Override
  public boolean canConvert(Class clazz) {
    if (!WfsFeatureCollection.class.isAssignableFrom(clazz)) {
      LOGGER.debug("Cannot convert: {}", clazz.getName());
    }
    return WfsFeatureCollection.class.isAssignableFrom(clazz);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    WfsFeatureCollection wfc = (WfsFeatureCollection) value;

    String schemaLoc =
        generateSchemaLocationFromMetacards(wfc.getFeatureMembers(), prefixToUriMapping);

    for (Entry<String, String> entry : prefixToUriMapping.entrySet()) {
      writer.addAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + entry.getKey(), entry.getValue());
    }
    writer.addAttribute(WfsConstants.ATTRIBUTE_SCHEMA_LOCATION, schemaLoc);

    Geometry allGeometry = getBounds(wfc.getFeatureMembers());
    if (allGeometry != null && !allGeometry.isEmpty()) {
      XmlNode.writeEnvelope(
          WfsConstants.GML_PREFIX + ":" + "boundedBy",
          context,
          writer,
          allGeometry.getEnvelopeInternal());
    }

    for (Metacard mc : wfc.getFeatureMembers()) {
      writer.startNode(WfsConstants.GML_PREFIX + ":" + featureMember);
      context.convertAnother(mc);
      writer.endNode();
    }
  }

  public void setContextRoot(String contextRoot) {
    if (null != contextRoot) {
      this.contextRoot = contextRoot;
    }
  }

  private String generateSchemaLocationFromMetacards(
      List<Metacard> metacards, Map<String, String> prefixToUriMapping) {

    StringBuilder descFeatureService = new StringBuilder();
    descFeatureService
        .append(contextRoot)
        .append("/wfs?service=wfs&request=DescribeFeatureType&version=1.0.0&typeName=");

    StringBuilder schemaLocation = new StringBuilder();
    Set<QName> qnames = new HashSet<>();
    for (Metacard metacard : metacards) {
      qnames.add(
          WfsQnameBuilder.buildQName(
              metacard.getMetacardType().getName(), metacard.getContentTypeName()));
    }
    for (QName qname : qnames) {
      prefixToUriMapping.put(qname.getPrefix(), qname.getNamespaceURI());
      schemaLocation
          .append(qname.getNamespaceURI())
          .append(" ")
          .append(descFeatureService)
          .append(qname.getPrefix())
          .append(":")
          .append(qname.getLocalPart())
          .append(" ");
    }
    return schemaLocation.toString();
  }

  private Geometry getBounds(List<Metacard> metacards) {
    if (metacards != null) {
      List<Geometry> geometries = new ArrayList<>();
      for (Metacard card : metacards) {
        if (null != card.getLocation()) {
          Geometry geo = XmlNode.readGeometry(card.getLocation());
          if (null != geo) {
            geometries.add(geo);
          }
        }
      }

      return new GeometryCollection(geometries.toArray(new Geometry[0]), new GeometryFactory());
    } else {
      LOGGER.debug("List of metacards was null.");
      return null;
    }
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    final List<Metacard> featureMembers = new ArrayList<>();
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      String nodeName = reader.getNodeName();
      // Its important to note that the reader appears to drop the
      // namespace.
      if (featureMember.equals(nodeName)) {
        reader.moveDown();
        // lookup the converter for this featuretype
        featureMembers.add(
            (Metacard)
                context.convertAnother(
                    null, MetacardImpl.class, featureConverterMap.get(reader.getNodeName())));
        reader.moveUp();
      }
      reader.moveUp();
    }
    return new WfsFeatureCollectionImpl(featureMembers.size(), featureMembers);
  }

  public void setFeatureConverterMap(Map<String, FeatureConverter> featureConverterMap) {
    this.featureConverterMap = featureConverterMap;
  }
}
