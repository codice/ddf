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
 */
package org.codice.ddf.opensearch.query.filter;

import ddf.catalog.impl.filter.SpatialFilter;

public class PolygonSpatialFilter extends SpatialFilter {
    private String[] latLonAry;

    public PolygonSpatialFilter(String lonLatStr) {
        super();

        latLonAry = lonLatStr.split(" |,\\p{Space}?");
        this.geometryWkt = createWKT();
    }

    private String createWKT() {
        StringBuilder wktBuilder = new StringBuilder("POLYGON((");
        for (int i = 0; i < (latLonAry.length - 1); i += 2) {
            if (i != 0) {
                wktBuilder.append(",");
            }
            // WKT is lon/lat, polygon is lat/lon
            wktBuilder.append(latLonAry[i + 1] + " " + latLonAry[i]);
        }
        wktBuilder.append("))");
        return wktBuilder.toString();
    }

}
