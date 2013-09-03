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
package ddf.catalog.impl.filter;

import static org.junit.Assert.*;

import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class JTSGeometryWrapperTest {
	JTSGeometryWrapper toTest;
	
	@Test
	public void testComputeJTSPeerWithNullGeometry() {
		toTest = new JTSGeometryWrapper(null);
		assertNull(toTest.computeJTSPeer());
	}
	
	@Test
	public void testComputeJTSPeerWithGeometry() {
		GeometryFactory fac = new GeometryFactory();
		Geometry toWrap = fac.createPoint(new Coordinate(0,0));
		toTest = new JTSGeometryWrapper(toWrap);
		assertEquals(toWrap, toTest.computeJTSPeer());
	}

}
