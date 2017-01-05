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
package org.codice.ddf.catalog.ui.query.delegate;

import java.util.List;

import ddf.catalog.filter.impl.SimpleFilterDelegate;

public class WktQueryDelegate extends SimpleFilterDelegate<String> {

    @Override
    public <S> String defaultOperation(Object property, S literal, Class<S> literalClass,
            Enum operation) {
        return "";
    }

    @Override
    public <S> String spatialOperation(String propertyName, S wkt, Class<S> wktClass,
            SpatialPropertyOperation spatialPropertyOperation) {
        return wkt.toString();
    }

    @Override
    public String and(List<String> operands) {
        return operands.stream()
                .filter(op -> !"".equals(op))
                .findFirst()
                .orElse("");
    }

    @Override
    public String or(List<String> operands) {
        return operands.stream()
                .filter(op -> !"".equals(op))
                .findFirst()
                .orElse("");
    }

}
