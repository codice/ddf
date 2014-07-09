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

import java.util.ArrayList;
import java.util.List;

import net.opengis.gml.v_3_1_1.DirectPositionListType;
import net.opengis.gml.v_3_1_1.LinearRingType;

import org.jvnet.ogc.gml.v_3_1_1.ObjectFactoryInterface;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311CoordinateConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311LinearRingConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311SRSReferenceGroupConverterInterface;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LinearRing;

public class CswJTSToGML311LinearRingConverter extends JTSToGML311LinearRingConverter {

    public CswJTSToGML311LinearRingConverter(ObjectFactoryInterface objectFactory,
            JTSToGML311SRSReferenceGroupConverterInterface srsReferenceGroupConverter,
            JTSToGML311CoordinateConverter coordinateConverter) {
        super(objectFactory, srsReferenceGroupConverter, coordinateConverter);
    }

    @Override
    protected LinearRingType doCreateGeometryType(LinearRing linearRing) {
        final LinearRingType resultLinearRing;        

        resultLinearRing = getObjectFactory().createLinearRingType();
        
        List<Double> posDoubleList = new ArrayList<Double>();
        for (Coordinate coordinate : linearRing.getCoordinates()) {
            posDoubleList.add(coordinate.x);
            posDoubleList.add(coordinate.y);
        }
        
        DirectPositionListType directPosListType = new DirectPositionListType();
        directPosListType.setValue(posDoubleList);
        resultLinearRing.setPosList(directPosListType);

        return resultLinearRing;
    }
}
