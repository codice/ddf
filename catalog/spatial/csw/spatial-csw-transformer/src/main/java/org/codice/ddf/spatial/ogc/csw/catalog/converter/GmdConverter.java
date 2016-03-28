/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdMetacardType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingWriter;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class GmdConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GmdConverter.class);

    static final DatatypeFactory XSD_FACTORY;
    
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

    static {
        DatatypeFactory factory = null;

        try {
            factory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            LOGGER.error("Failed to create xsdFactory", e);
        }

        XSD_FACTORY = factory;
    }

    public GmdConverter() {

        XStream xstream = new XStream(new Xpp3Driver(new NoNameCoder()));

        xstream.setClassLoader(xstream.getClass()
                .getClassLoader());

        xstream.registerConverter(this);
        xstream.alias(GmdMetacardType.GMD_LOCAL_NAME, Metacard.class);
        xstream.alias(GmdMetacardType.GMD_METACARD_TYPE_NAME, Metacard.class);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return Metacard.class.isAssignableFrom(clazz);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        throw new NotImplementedException();
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter inWriter,
            MarshallingContext context) {
        if (source == null || !(source instanceof Metacard)) {
            LOGGER.warn("Failed to marshal Metacard: {}", source);
            return;
        }
        MetacardImpl metacard = new MetacardImpl((Metacard) source);

        PathTracker tracker = new PathTracker();
        PathTrackingWriter trackingWriter = new PathTrackingWriter(inWriter, tracker);

        XstreamPathValueTracker pathValueTracker = buildPaths(metacard);
        XmlTree tree = buildTree(pathValueTracker.getPaths());

        tree.accept(new XstreamTreeWriter(trackingWriter, tracker, pathValueTracker));

    }

    protected XmlTree buildTree(Set<Path> paths) {

        XmlTree gmdTree = new XmlTree(GmdMetacardType.GMD_LOCAL_NAME);

        XmlTree current = gmdTree;

        for (Path path : paths) {
            String tree = path.toString();
            XmlTree root = current;

            tree = StringUtils.substringAfter(tree, GmdMetacardType.GMD_LOCAL_NAME);
            for (String data : tree.split("/")) {
                if (StringUtils.isNotBlank(data)) {
                    current = current.addChild(data);
                }
            }

            current = root;
        }
        return gmdTree;
    }

    /**
     * Builds up the xml paths and values to write.
     * Order matters!  Paths should be added in the order they must be written.
     *
     * @param metacard
     * @return XstreamPathValueTracker containing XML paths and values to write
     */
    protected XstreamPathValueTracker buildPaths(MetacardImpl metacard) {

        XstreamPathValueTracker pathValueTracker = new XstreamPathValueTracker();

        pathValueTracker.add(new Path("/MD_Metadata/@xmlns"), GmdMetacardType.GMD_NAMESPACE);

        pathValueTracker.add(new Path("/MD_Metadata/@xmlns:" + GmdMetacardType.GCO_PREFIX),
                GmdMetacardType.GCO_NAMESPACE);
        pathValueTracker.add(new Path(GmdMetacardType.FILE_IDENTIFIER_PATH), metacard.getId());

        pathValueTracker.add(new Path(GmdMetacardType.CODE_LIST_VALUE_PATH),
                StringUtils.defaultIfEmpty(metacard.getContentTypeName(), "dataset"));
        pathValueTracker.add(new Path(GmdMetacardType.CODE_LIST_PATH),
                GmdMetacardType.METACARD_URI);

        pathValueTracker.add(new Path(GmdMetacardType.CONTACT_PATH), (String) null);

        GregorianCalendar modifiedCal = new GregorianCalendar();
        if (metacard.getModifiedDate() != null) {

            modifiedCal.setTime(metacard.getModifiedDate());
        }
        modifiedCal.setTimeZone(UTC_TIME_ZONE);

        pathValueTracker.add(new Path(GmdMetacardType.DATE_TIME_STAMP_PATH),
                XSD_FACTORY.newXMLGregorianCalendar(modifiedCal)
                        .toXMLFormat());

        addIdentificationInfo(metacard, pathValueTracker);

        addDistributionInfo(metacard, pathValueTracker);

        return pathValueTracker;

    }

    protected void addDistributionInfo(MetacardImpl metacard,
            XstreamPathValueTracker pathValueTracker) {

        if (metacard.getResourceURI() != null) {
            pathValueTracker.add(new Path(GmdMetacardType.LINKAGE_URI_PATH),
                    metacard.getResourceURI()
                            .toASCIIString());
        }

    }

    protected void addIdentificationInfo(MetacardImpl metacard,
            XstreamPathValueTracker pathValueTracker) {

        pathValueTracker.add(new Path(GmdMetacardType.TITLE_PATH), StringUtils.defaultString(
                metacard.getTitle()));

        GregorianCalendar createdCal = new GregorianCalendar();

        if (metacard.getCreatedDate() != null) {

            createdCal.setTime(metacard.getCreatedDate());
        }
        createdCal.setTimeZone(UTC_TIME_ZONE);
        pathValueTracker.add(new Path(GmdMetacardType.CREATED_DATE_PATH),
                XSD_FACTORY.newXMLGregorianCalendar(createdCal)
                        .toXMLFormat());

        pathValueTracker.add(new Path(GmdMetacardType.CREATED_DATE_TYPE_CODE_PATH),
                GmdMetacardType.METACARD_URI);
        pathValueTracker.add(new Path(GmdMetacardType.CREATED_DATE_TYPE_CODE_VALUE_PATH),
                Metacard.CREATED);

        pathValueTracker.add(new Path(GmdMetacardType.ABSTRACT_PATH), StringUtils.defaultString(
                metacard.getDescription()));
        pathValueTracker.add(new Path(GmdMetacardType.POINT_OF_CONTACT_PATH),
                StringUtils.defaultString(metacard.getPointOfContact()));

        pathValueTracker.add(new Path(GmdMetacardType.POINT_OF_CONTACT_ROLE_PATH), (String) null);

        pathValueTracker.add(new Path(GmdMetacardType.LANGUAGE_PATH), StringUtils.defaultIfEmpty(
                Locale.getDefault()
                        .getLanguage(),
                Locale.ENGLISH.getLanguage()));
        addExtent(metacard, pathValueTracker);

    }

    protected void addExtent(MetacardImpl metacard, XstreamPathValueTracker pathValueTracker) {

        String wkt = metacard.getLocation();
        if (StringUtils.isNotBlank(wkt)) {
            WKTReader reader = new WKTReader();
            Geometry geometry = null;

            try {
                geometry = reader.read(wkt);
            } catch (ParseException e) {
                LOGGER.warn("Unable to parse geometry {}", wkt, e);
            }

            if (geometry != null) {
                Envelope bounds = geometry.getEnvelopeInternal();
                String westLon = Double.toString(bounds.getMinX());
                String eastLon = Double.toString(bounds.getMaxX());
                String southLat = Double.toString(bounds.getMinY());
                String northLat = Double.toString(bounds.getMaxY());
                pathValueTracker.add(new Path(GmdMetacardType.BBOX_WEST_LON_PATH), westLon);
                pathValueTracker.add(new Path(GmdMetacardType.BBOX_EAST_LON_PATH), eastLon);
                pathValueTracker.add(new Path(GmdMetacardType.BBOX_SOUTH_LAT_PATH), southLat);
                pathValueTracker.add(new Path(GmdMetacardType.BBOX_NORTH_LAT_PATH), northLat);

            }

        }
    }

}