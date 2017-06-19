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
 * A feature with circle geometry specified by a center coordinate and a radius.
 */
public class PointRadiusFeature extends Feature {
    private Coordinate center;

    private double radius;

    public void setCenter(Coordinate center) {
        this.center = center;
    }

    /**
     * Sets the length of the circle's radius
     *
     * @param radius value in meters
     */
    public void setRadius(double radius) {
        this.radius = radius;
    }

    @Override
    public String getType() {
        return "point-radius";
    }

    @Override
    public Map<String, Object> getGeometryJsonObject() {
        Map<String, Object> result = new HashMap<>();
        result.put("center", this.center.getJsonObject());
        result.put("radius", this.radius);
        return result;
    }
}
