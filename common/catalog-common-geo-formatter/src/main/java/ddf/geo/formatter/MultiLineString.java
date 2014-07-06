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
package ddf.geo.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.abdera.ext.geo.Line;
import org.apache.abdera.ext.geo.Position;

import com.vividsolutions.jts.geom.Geometry;

public class MultiLineString extends LineString {

    public static final String TYPE = "MultiLineString";

    public MultiLineString(Geometry geometry) {
        super(geometry);
    }

    public static CompositeGeometry toCompositeGeometry(List coordinates) {

        com.vividsolutions.jts.geom.LineString[] allLineStrings = new com.vividsolutions.jts.geom.LineString[coordinates
                .size()];

        for (int i = 0; i < allLineStrings.length; i++) {
            allLineStrings[i] = geometryFactory.createLineString(getCoordinates((List) coordinates
                    .get(i)));
        }

        return new MultiLineString(geometryFactory.createMultiLineString(allLineStrings));
    }

    @Override
    public Map toJsonMap() {

        List overallCoordsList = new ArrayList();

        for (int i = 0; i < geometry.getNumGeometries(); i++) {
            overallCoordsList.add(buildCoordinatesList(geometry.getGeometryN(i).getCoordinates()));

        }

        return createMap(COORDINATES_KEY, overallCoordsList);

    }

    @Override
    public List<Position> toGeoRssPositions() {

        List<Position> positions = new ArrayList<Position>();

        for (int i = 0; i < geometry.getNumGeometries(); i++) {

            positions.add(new Line(getLineStringCoordinates(geometry.getGeometryN(i))));
        }

        return positions;
    }
}
