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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A feature with polygonal geometry represented by a list of Coordinates
 */
public class PolygonFeature extends Feature {
    private List<Coordinate> coordinates = new ArrayList<>();

    @Override
    public String getType() {
        return "polygon";
    }

    @Override
    public Map<String, Object> getGeometryJsonObject() {
        Map<String, Object> result = new HashMap<>();
        result.put("coordinates", this.getCoordinatesJsonObject());
        return result;
    }

    public List<Coordinate> getCoordinates() {
        return coordinates;
    }

    private List<Object> getCoordinatesJsonObject() {
        return this.coordinates.stream()
                .map(c -> c.getJsonObject())
                .collect(Collectors.toList());
    }
}
