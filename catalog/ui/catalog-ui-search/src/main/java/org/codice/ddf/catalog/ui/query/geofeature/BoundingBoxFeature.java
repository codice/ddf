/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.query.geofeature;

import java.util.HashMap;
import java.util.Map;

/**
 * A feature with bounding-box geometry specified by north, south, east, and west extents.
 */
public class BoundingBoxFeature extends Feature {
    private double north;

    private double south;

    private double east;

    private double west;

    public double getNorth() {
        return north;
    }

    public void setNorth(double north) {
        this.north = north;
    }

    public double getSouth() {
        return south;
    }

    public void setSouth(double south) {
        this.south = south;
    }

    public double getEast() {
        return east;
    }

    public void setEast(double east) {
        this.east = east;
    }

    public double getWest() {
        return west;
    }

    public void setWest(double west) {
        this.west = west;
    }

    @Override
    public String getType() {
        return "bbox";
    }

    @Override
    public Map<String, Object> getGeometryJsonObject() {
        Map<String, Object> result = new HashMap<>();
        result.put("north", this.north);
        result.put("south", this.south);
        result.put("east", this.east);
        result.put("west", this.west);
        return result;
    }
}
