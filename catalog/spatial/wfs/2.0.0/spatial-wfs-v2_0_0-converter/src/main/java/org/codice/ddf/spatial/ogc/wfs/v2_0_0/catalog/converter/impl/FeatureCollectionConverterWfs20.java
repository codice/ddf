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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.converter.impl;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
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
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsQnameBuilder;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20FeatureCollection;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class works in conjunction with XStream to convert a {@link Wfs20FeatureCollection} to XML
 * according to the GML 3.2.1 spec. It will also convert respective XML into a {@link
 * Wfs20FeatureCollection}.
 */
public class FeatureCollectionConverterWfs20 implements Converter {

  private static final String FEATURE_MEMBER = "member";

  private static final String FEATURE_COLLECTION = "FeatureCollection";

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureCollectionConverterWfs20.class);

  private String contextRoot;

  private Map<String, FeatureConverter> featureConverterMap =
      new HashMap<String, FeatureConverter>();

  private Map<String, String> prefixToUriMapping = new HashMap<String, String>();

  public FeatureCollectionConverterWfs20() {
    prefixToUriMapping.put(Wfs20Constants.WFS_NAMESPACE_PREFIX, Wfs20Constants.WFS_2_0_NAMESPACE);
    prefixToUriMapping.put(Wfs20Constants.GML_PREFIX, Wfs20Constants.GML_3_2_NAMESPACE);
  }

  @Override
  public boolean canConvert(Class clazz) {
    if (!WfsFeatureCollection.class.isAssignableFrom(clazz)) {
      LOGGER.debug("Cannot convert: {}", clazz.getName());
    }
    return Wfs20FeatureCollection.class.isAssignableFrom(clazz);
  }

  @Override
  public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
    if (value != null) {
      Wfs20FeatureCollection wfc = (Wfs20FeatureCollection) value;

      String schemaLoc = generateSchemaLocationFromMetacards(wfc.getMembers(), prefixToUriMapping);

      for (Entry<String, String> entry : prefixToUriMapping.entrySet()) {
        writer.addAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + entry.getKey(), entry.getValue());
      }
      writer.addAttribute(Wfs20Constants.ATTRIBUTE_SCHEMA_LOCATION, schemaLoc);

      Geometry allGeometry = getBounds(wfc.getMembers());
      if (allGeometry != null && !allGeometry.isEmpty()) {
        XmlNode.writeEnvelope(
            Wfs20Constants.GML_PREFIX + ":" + "boundedBy",
            context,
            writer,
            allGeometry.getEnvelopeInternal());
      }

      for (Metacard mc : wfc.getMembers()) {
        writer.startNode(Wfs20Constants.GML_PREFIX + ":" + FEATURE_MEMBER);
        context.convertAnother(mc);
        writer.endNode();
      }
    } else {
      LOGGER.debug("Incoming value was null");
    }
  }

  public void setContextRoot(String contextRoot) {
    if (null != contextRoot) {
      this.contextRoot = contextRoot;
    }
  }

  private String generateSchemaLocationFromMetacards(
      List<Metacard> metacards, Map<String, String> prefixToUriMapping) {
    if (metacards != null) {
      StringBuilder descFeatureService = new StringBuilder();
      descFeatureService
          .append(contextRoot)
          .append("/wfs?service=wfs&request=DescribeFeatureType&version=2.0.0&typeName=");

      StringBuilder schemaLocation = new StringBuilder();
      Set<QName> qnames = new HashSet<QName>();
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
    } else {
      LOGGER.debug("Metacard list is null");
      return null;
    }
  }

  private Geometry getBounds(List<Metacard> metacards) {
    if (metacards != null) {
      List<Geometry> geometries = new ArrayList<Geometry>();
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
      LOGGER.debug("Metacard list is null");
      return null;
    }
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Wfs20FeatureCollection featureCollection = new Wfs20FeatureCollection();
    while (reader.hasMoreChildren()) {
      reader.moveDown();
      String nodeName = reader.getNodeName();

      // Its important to note that the reader appears to drop the
      // namespace.
      if (FEATURE_MEMBER.equals(nodeName)) {
        reader.moveDown();
        String subNodeName = reader.getNodeName();
        // If the member contains a sub feature collection, step in and get members
        if (subNodeName.equals(FEATURE_COLLECTION)) {

          while (reader.hasMoreChildren()) {
            reader.moveDown();
            String subNodeName2 = reader.getNodeName();
            if (FEATURE_MEMBER.equals(subNodeName2)) {
              reader.moveDown();
              // lookup the converter for this featuretype
              featureCollection =
                  addMetacardToFeatureCollection(featureCollection, context, reader);
              reader.moveUp();
            }
            reader.moveUp();
          }

        } else {
          // lookup the converter for this featuretype
          featureCollection = addMetacardToFeatureCollection(featureCollection, context, reader);
        }
        reader.moveUp();
      }
      reader.moveUp();
    }
    return featureCollection;
  }

  private Wfs20FeatureCollection addMetacardToFeatureCollection(
      Wfs20FeatureCollection featureCollection,
      UnmarshallingContext context,
      HierarchicalStreamReader reader) {
    featureCollection
        .getMembers()
        .add(
            (Metacard)
                context.convertAnother(
                    null, MetacardImpl.class, featureConverterMap.get(reader.getNodeName())));
    return featureCollection;
  }

  public void setFeatureConverterMap(Map<String, FeatureConverter> featureConverterMap) {
    this.featureConverterMap = featureConverterMap;
  }
}
