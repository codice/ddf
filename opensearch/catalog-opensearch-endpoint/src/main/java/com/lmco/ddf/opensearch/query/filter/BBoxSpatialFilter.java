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
package com.lmco.ddf.opensearch.query.filter;

import ddf.catalog.impl.filter.SpatialFilter;

public class BBoxSpatialFilter extends SpatialFilter {
    private double minX;

    private double minY;

    private double maxX;

    private double maxY;

    private static final int MAX_Y_COORDINATE_INDEX = 3;

    /**
     * Comma delimited list of lat/lon (deg) bounding box coordinates (geo format: geo:bbox ~
     * West,South,East,North).
     * 
     * @param minX
     * @param minY
     * @param maxX
     * @param maxY
     */
    public BBoxSpatialFilter(String bbox) {
        super();

        String[] bboxArY = bbox.split(" |,\\p{Space}?");

        this.minX = Double.parseDouble(bboxArY[0]);
        this.minY = Double.parseDouble(bboxArY[1]);
        this.maxX = Double.parseDouble(bboxArY[2]);
        this.maxY = Double.parseDouble(bboxArY[MAX_Y_COORDINATE_INDEX]);

        this.geometryWkt = createWKT();
    }

    /**
     * Creates a WKT string from the bbox coordinates
     * 
     * @return the wkt String
     */
    private String createWKT() {
        StringBuilder wktBuilder = new StringBuilder("POLYGON((");
        wktBuilder.append(minX + " " + minY);
        wktBuilder.append("," + minX + " " + maxY);
        wktBuilder.append("," + maxX + " " + maxY);
        wktBuilder.append("," + maxX + " " + minY);
        wktBuilder.append("," + minX + " " + minY);
        wktBuilder.append("))");

        return wktBuilder.toString();
    }

}
