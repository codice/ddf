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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.abdera.ext.geo.Position;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class Point extends CompositeGeometry {

	protected Geometry geometry;
	public static final String TYPE = "Point";

	public Point(Geometry geometry) {
		if (geometry == null) {
			throw new IllegalArgumentException("Geometry argument must not be null");
		}

		if (isNotType(geometry)) {
			throw new IllegalArgumentException("Geometry is not a " + this.getClass().getName());
		}
		this.geometry = geometry;
	}

	protected boolean isNotType(Geometry geo) {
		return !this.getClass().getSimpleName().equals(geo.getGeometryType());
	}

	/**
	 * 
	 * @param coordinates
	 *            [x,y] coordinate list
	 */
	public static CompositeGeometry toCompositeGeometry(List coordinates) {
		return new Point(geometryFactory.createPoint(getCoordinate(coordinates)));
	}

	@Override
	public Map toJsonMap() {

		List<Double> coordinatesList = new ArrayList<Double>();
		coordinatesList.add(geometry.getCoordinate().x);
		coordinatesList.add(geometry.getCoordinate().y);

		return createMap(COORDINATES_KEY, coordinatesList);

	}

	protected Map createMap(String key, List objects) {

		Map map = new HashMap();
		map.put(TYPE_KEY, this.getClass().getSimpleName());
		map.put(key, objects);
		return map;

	}

	@Override
	public String toWkt() {
		return geometry.toText();
	}

	@Override
	public Geometry getGeometry() {
		return this.geometry;
	}

	@Override
	public List<Position> toGeoRssPositions() {

		return Arrays.asList((Position)new org.apache.abdera.ext.geo.Point(convert(geometry.getCoordinate())));
	}

	protected org.apache.abdera.ext.geo.Coordinate convert(Coordinate jtsCoordinate){
		
		return new org.apache.abdera.ext.geo.Coordinate(jtsCoordinate.y, jtsCoordinate.x);
		
	}

}
