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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.namespace.QName;

import org.codice.ddf.spatial.ogc.catalog.common.converter.XmlNode;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsQnameBuilder;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;

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

/**
 * This class works in conjunction with XStream to convert a {@link WfsFeatureCollection} to XML
 * according to the GML 2.1.2 spec. It will also convert respective XML into a
 * {@link WfsFeatureCollection}.
 * 
 * @author kcwire
 * 
 */

public class FeatureCollectionConverter implements Converter {

    private static final String FEATURE_MEMBER = "featureMember";

    private String contextRoot;

    private Map<String, FeatureConverter> featureConverterMap = new HashMap<String, FeatureConverter>();

    @Override
    public boolean canConvert(Class clazz) {
        return WfsFeatureCollection.class.isAssignableFrom(clazz);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        WfsFeatureCollection wfc = (WfsFeatureCollection) value;

        Map<String, String> prefixToUriMapping = new HashMap<String, String>();

        String schemaLoc = generateSchemaLocationFromMetacards(wfc.getFeatureMembers(),
                prefixToUriMapping);

        prefixToUriMapping.put(WfsConstants.XSI_PREFIX, WfsConstants.XSI_NAMESPACE);
        prefixToUriMapping.put(WfsConstants.WFS_NAMESPACE_PREFIX, WfsConstants.WFS_NAMESPACE);
        prefixToUriMapping.put(WfsConstants.GML_PREFIX, WfsConstants.GML_NAMESPACE);

        for (Entry<String, String> entry : prefixToUriMapping.entrySet()) {
            writer.addAttribute(WfsConstants.XMLNS_PREFIX + entry.getKey(), entry.getValue());
        }
        writer.addAttribute(WfsConstants.ATTRIBUTE_SCHEMA_LOCATION, schemaLoc);

        Geometry allGeometry = getBounds(wfc.getFeatureMembers());
        if (!allGeometry.isEmpty()) {
            XmlNode.writeEnvelope("gml:boundedBy", context, writer,
                    allGeometry.getEnvelopeInternal());
        }

        for (Metacard mc : wfc.getFeatureMembers()) {
            writer.startNode(WfsConstants.GML_PREFIX + WfsConstants.NAMESPACE_DELIMITER
                    + FEATURE_MEMBER);
            context.convertAnother(mc);
            writer.endNode();
        }
    }

    public void setContextRoot(String contextRoot) {
        if (null != contextRoot) {
            this.contextRoot = contextRoot;
        }
    }

    private String generateSchemaLocationFromMetacards(List<Metacard> metacards,
            Map<String, String> prefixToUriMapping) {

        StringBuilder descFeatureService = new StringBuilder();
        descFeatureService.append(contextRoot).append(
                "/wfs?service=wfs&request=DescribeFeatureType&version=1.0.0&typeName=");

        StringBuilder schemaLocation = new StringBuilder();
        Set<QName> qnames = new HashSet<QName>();
        for (Metacard metacard : metacards) {
            qnames.add(WfsQnameBuilder.buildQName(metacard.getMetacardType().getName(),
                    metacard.getContentTypeName()));
        }
        for (QName qname : qnames) {
            prefixToUriMapping.put(qname.getPrefix(), qname.getNamespaceURI());
            schemaLocation.append(qname.getNamespaceURI()).append(WfsConstants.SPACE)
                    .append(descFeatureService).append(qname.getPrefix())
                    .append(WfsConstants.NAMESPACE_DELIMITER).append(qname.getLocalPart())
                    .append(WfsConstants.SPACE);

        }
        return schemaLocation.toString();
    }

    private Geometry getBounds(List<Metacard> metacards) {
        List<Geometry> geometries = new ArrayList<Geometry>();
        for (Metacard card : metacards) {
            if (null != card.getLocation()) {
                Geometry geo = XmlNode.readGeometry(card.getLocation());
                if (null != geo) {
                    geometries.add(geo);
                }
            }
        }

        Geometry allGeometry = new GeometryCollection(geometries.toArray(new Geometry[0]),
                new GeometryFactory());
        return allGeometry;
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        WfsFeatureCollection featureCollection = new WfsFeatureCollection();
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            String nodeName = reader.getNodeName();
            // Its important to note that the reader appears to drop the
            // namespace.
            if (FEATURE_MEMBER.equals(nodeName)) {
                reader.moveDown();
                // lookup the converter for this featuretype
                featureCollection.getFeatureMembers().add(
                        (Metacard) context.convertAnother(null, MetacardImpl.class,
                                featureConverterMap.get(reader.getNodeName())));
                reader.moveUp();
            }
            reader.moveUp();
        }
        return featureCollection;
    }

    public void setFeatureConverterMap(Map<String, FeatureConverter> featureConverterMap) {
        this.featureConverterMap = featureConverterMap;
    }
}
