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
package ddf.catalog.source.solr;

import org.apache.log4j.Logger;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class WktNormalizer {
    
    private static final Logger LOGGER = Logger.getLogger(WktNormalizer.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    private WktNormalizer() {
        
    }
    
    // Spatial4j detects the orientation of rectangles and will flip it
    // across the date line if it is clockwise. Other spatial libraries like
    // OpenLayers are WKT orientation agnostic. Convert clockwise rectangles to
    // counter-clockwise.
    public static String normalizeWkt(String wkt) {
        Geometry geo = readWkt(wkt);

        if (isClockwiseRectangle(geo)) {
            return ((Polygon) geo).reverse().toText();
        } else {
            return wkt;
        }
    }

    public static Geometry readWkt(String wkt) {
        WKTReader reader = new WKTReader(GEOMETRY_FACTORY);
        Geometry geo = null;

        try {
            geo = reader.read(wkt);
        } catch (ParseException e) {
            LOGGER.info("Failed to read WKT: " + wkt, e);
        }
        
        return geo;
    }

    private static boolean isClockwiseRectangle(Geometry geo) {
        return geo != null && geo.isRectangle()
                && !CGAlgorithms.isCCW(geo.getCoordinates());
    }

}
