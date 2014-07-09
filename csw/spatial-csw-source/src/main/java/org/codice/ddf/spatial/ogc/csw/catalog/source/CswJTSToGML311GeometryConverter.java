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
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import javax.xml.bind.JAXBElement;

import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import net.opengis.gml.v_3_1_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_1_1.LinearRingType;
import net.opengis.gml.v_3_1_1.PolygonPropertyType;
import net.opengis.gml.v_3_1_1.PolygonType;

import org.jvnet.ogc.gml.v_3_1_1.ObjectFactoryInterface;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311Constants;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311ConverterInterface;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311CoordinateConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311GeometryConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311PolygonConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311SRSReferenceGroupConverterInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;

public class CswJTSToGML311GeometryConverter extends JTSToGML311GeometryConverter {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(
            CswJTSToGML311GeometryConverter.class);
    
    private final JTSToGML311CoordinateConverter cswCoordinateConverter;
    private final JTSToGML311ConverterInterface<LinearRingType, AbstractRingPropertyType, LinearRing> cswLinearRingConverter;
    private final JTSToGML311ConverterInterface<PolygonType, PolygonPropertyType, Polygon> cswPolygonConverter;

    public CswJTSToGML311GeometryConverter() {
        this(JTSToGML311Constants.DEFAULT_OBJECT_FACTORY,
             JTSToGML311Constants.DEFAULT_SRS_REFERENCE_GROUP_CONVERTER);
    }

    public CswJTSToGML311GeometryConverter(ObjectFactoryInterface objectFactory,
            JTSToGML311SRSReferenceGroupConverterInterface srsReferenceGroupConverter) {
        super(objectFactory, srsReferenceGroupConverter);
        
        this.cswCoordinateConverter = new JTSToGML311CoordinateConverter(
                objectFactory, srsReferenceGroupConverter);
        this.cswLinearRingConverter = new CswJTSToGML311LinearRingConverter(objectFactory, srsReferenceGroupConverter, this.cswCoordinateConverter);
        this.cswPolygonConverter = new JTSToGML311PolygonConverter(objectFactory,
                srsReferenceGroupConverter, this.cswLinearRingConverter);
    }
    
    @Override
    protected AbstractGeometryType doCreateGeometryType(Geometry geometry) 
            throws IllegalArgumentException {
        if (geometry instanceof LinearRing) {
            LOGGER.debug("Creating LinearRingType");
            return cswLinearRingConverter.createGeometryType((LinearRing) geometry);
        } else if (geometry instanceof Polygon) {
            LOGGER.debug("Creating PolygonType");
            return cswPolygonConverter.createGeometryType((Polygon) geometry);
        } else {
            LOGGER.debug("Passing Geometry to superclass for default doCreateGeometryType processing");
            return super.doCreateGeometryType(geometry);
        }
    }
    
    @Override
    public JAXBElement<? extends AbstractGeometryType> createElement(
            Geometry geometry) throws IllegalArgumentException {
        
        if (geometry instanceof LinearRing) {
            LOGGER.debug("Creating LinearRing");
            return cswLinearRingConverter.createElement((LinearRing) geometry);
        } else if (geometry instanceof Polygon) {
            LOGGER.debug("Creating Polygon");
            return cswPolygonConverter.createElement((Polygon) geometry);
        } else {
            LOGGER.debug("Passing Geometry to superclass for default createElement processing");
            return super.createElement(geometry);
        }
    }

}
