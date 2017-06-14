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
 * Base class for results returned from the FeatureService.
 */
public abstract class Feature {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract String getType();

    /**
     * Subclasses should override with behavior specific to their geometry type
     * @return  A data object representing the Feature's geometry
     */
    public abstract Map<String, Object> getGeometryJsonObject();

    /**
     * Provides a JSON-serializable representation of the Feature
     * @return  A data object representing the Feature
     */
    public Map<String, Object> getJsonObject() {
        Map<String, Object> result = new HashMap<>();
        result.putAll(this.getGeometryJsonObject());
        result.put("name", this.name);
        result.put("type", this.getType());
        return result;
    }
}
