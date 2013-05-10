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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;

public class Polygon extends MultiPoint {

	public static final String TYPE = "Polygon";

	public Polygon(Geometry geometry) {
		super(geometry);

	}

	/**
	 * 
	 * @param coordinates
	 *            a List of coordinates formatted in the GeoJSON Array
	 *            equivalent
	 * @return
	 */
	public static CompositeGeometry toCompositeGeometry(List coordinates) {
		return new Polygon(buildPolygon(coordinates));
	}

	@Override
	public Map toJsonMap() {
		Map map = new HashMap();

		if (TYPE.equals(geometry.getGeometryType())) {

			map.put(TYPE_KEY, TYPE);

			List linearRingsList = buildJsonPolygon((com.vividsolutions.jts.geom.Polygon) geometry);

			map.put(COORDINATES_KEY, linearRingsList);

		} else {
			throw new UnsupportedOperationException("Geometry is not a " + TYPE);
		}

		return map;

	}

	protected List buildJsonPolygon(com.vividsolutions.jts.geom.Polygon polygon) {
		List linearRingsList = new ArrayList();

		// According GeoJSON spec, first LinearRing is the exterior ring
		linearRingsList.add(buildCoordinatesList(polygon.getExteriorRing().getCoordinates()));

		for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
			linearRingsList.add(buildCoordinatesList(polygon.getInteriorRingN(i).getCoordinates()));
		}
		return linearRingsList;
	}

	protected static com.vividsolutions.jts.geom.Polygon buildPolygon(List coordinates) {

		// according to the GeoJson specification, first ring is the exterior
		LinearRing exterior = geometryFactory.createLinearRing(getCoordinates((List) coordinates.get(0)));

		LinearRing[] interiorHoles = new LinearRing[coordinates.size() - 1];

		for (int i = 1; i < coordinates.size(); i++) {
			interiorHoles[i - 1] = geometryFactory.createLinearRing(getCoordinates((List) coordinates.get(i)));
		}

		return geometryFactory.createPolygon(exterior, interiorHoles);
	}

	@Override
	public List<Position> toGeoRssPositions() {

		org.apache.abdera.ext.geo.Coordinates coords = getPolygonCoordinates((com.vividsolutions.jts.geom.Polygon)geometry);

		return Arrays.asList((Position) (new org.apache.abdera.ext.geo.Polygon(coords)));

	}

	protected org.apache.abdera.ext.geo.Coordinates getPolygonCoordinates(com.vividsolutions.jts.geom.Polygon polygon) {
		
		org.apache.abdera.ext.geo.Coordinates coords = new org.apache.abdera.ext.geo.Coordinates();

		// it does not look like http://georss.org/simple or
		// http://georss.org/gml can handle
		// interior rings
		for (com.vividsolutions.jts.geom.Coordinate jtsCoordinate : polygon.getExteriorRing().getCoordinates()) {

			coords.add(convert(jtsCoordinate));

		}
		return coords;
	}
}
