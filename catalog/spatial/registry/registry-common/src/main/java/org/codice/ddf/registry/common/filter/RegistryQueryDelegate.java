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

package org.codice.ddf.registry.common.filter;

import java.util.List;

import org.codice.ddf.registry.common.RegistryConstants;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SimpleFilterDelegate;

public class RegistryQueryDelegate extends SimpleFilterDelegate<Boolean> {
    /*
    Returns false if this is a query that should not return registry metacards
    Return true if this is a query that should return registry metacards
     */

    @Override
    public <S> Boolean defaultOperation(Object property, S literal, Class<S> literalClass,
            Enum operation) {
        return false;
    }

    @Override
    public Boolean and(List<Boolean> operands) {
        return operands.stream()
                .anyMatch(op -> op);
    }

    @Override
    public Boolean or(List<Boolean> operands) {
        return operands.stream()
                .anyMatch(op -> op);
    }

    @Override
    public Boolean not(Boolean operand) {
        return operand;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, String pattern, boolean isCaseSensitive) {
        return propertyName.equals(Metacard.CONTENT_TYPE)
                && pattern.startsWith(RegistryConstants.REGISTRY_TAG);
    }

    @Override
    public Boolean propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        return propertyName.equals(Metacard.CONTENT_TYPE)
                && pattern.startsWith(RegistryConstants.REGISTRY_TAG);
    }
}
