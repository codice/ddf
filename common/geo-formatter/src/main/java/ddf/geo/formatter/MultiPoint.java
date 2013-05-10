/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.geo.formatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.abdera.ext.geo.Position;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class MultiPoint extends Point {

	public static final String TYPE = "MultiPoint";

	public MultiPoint(Geometry geometry) {
		super(geometry);
	}

	public static CompositeGeometry toCompositeGeometry(List coordinates) {
		com.vividsolutions.jts.geom.Point[] allPoints = new com.vividsolutions.jts.geom.Point[coordinates.size()];

		for (int i = 0; i < allPoints.length; i++) {
			allPoints[i] = geometryFactory.createPoint(getCoordinate((List) coordinates.get(i)));
		}

		return new MultiPoint(geometryFactory.createMultiPoint(allPoints));
	}

	@Override
	public Map toJsonMap() {

		return createMap(COORDINATES_KEY, buildCoordinatesList(geometry.getCoordinates()));

	}

	protected List<List<Double>> buildCoordinatesList(Coordinate[] coordinates) {
		List<List<Double>> allCoordinatesList = new ArrayList<List<Double>>();

		for (int i = 0; i < coordinates.length; i++) {
			List<Double> singleCoordinatesList = new ArrayList<Double>();
			Coordinate coord = coordinates[i];
			singleCoordinatesList.add(coord.x);
			singleCoordinatesList.add(coord.y);

			allCoordinatesList.add(singleCoordinatesList);
		}
		return allCoordinatesList;
	}

	@Override
	public String toWkt() {
		return this.geometry.toText();
	}

	@Override
	public Geometry getGeometry() {
		return this.geometry;
	}
	
	@Override
	public List<Position> toGeoRssPositions() {
		
		List<Position> list = new ArrayList<Position>();
		
		for (int i = 0; i < geometry.getCoordinates().length; i++) {
			
			Coordinate jtsCoordinate = geometry.getCoordinates()[i];
			
			list.add(new org.apache.abdera.ext.geo.Point(convert(jtsCoordinate)));
			
		}
		
		return list;
	}
}
