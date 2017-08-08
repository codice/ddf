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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.util.HashMap;
import java.util.Map;

import org.geotools.filter.visitor.DuplicatingFilterVisitor;
import org.opengis.filter.expression.PropertyName;

/**
 * This Visitor creates a new Filter with the PropertyNames remapped
 * as defined in the passed in Map. If a given property name doesn't
 * exist in the map it remains unchanged.
 * For example if you pass in a map with key->key1 value->key2 and run
 * it on a filter like `key1 = 10 AND key3 = 20` the resulting filter will be `key2 = 10 AND key3 = 20`
 */
public class PropertyMapperVisitor extends DuplicatingFilterVisitor {
    private Map<String, String> mappings = new HashMap<>();

    public PropertyMapperVisitor(Map<String, String> mappings) {
        this.mappings.putAll(mappings);
    }

    @Override
    public Object visit(PropertyName expression, Object extraData) {
        if (expression == null) {
            return null;
        }

        String name = expression.getPropertyName();

        return getFactory(extraData).property(mappings.getOrDefault(name, name));
    }
}
