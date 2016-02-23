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

package ddf.catalog.registry.common.filter;

import java.util.Date;
import java.util.List;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.registry.common.metacard.RegistryObjectMetacardType;

public class RegistryQueryDelegate extends FilterDelegate<Boolean> {
    /*
    Returns false if this is a query that should not return registry metacards
    Return true if this is a query that should return registry metacards
     */

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
    public Boolean nearestNeighbor(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean include() {
        return false;
    }

    @Override
    public Boolean exclude() {
        return false;
    }

    @Override
    public Boolean propertyIs(String propertyName, Object literal) {
        return false;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, String pattern, boolean isCaseSensitive) {
        return propertyName.equals(Metacard.CONTENT_TYPE) && pattern.startsWith(
                RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME);
    }

    @Override
    public Boolean propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        return propertyName.equals(Metacard.CONTENT_TYPE) && pattern.startsWith(
                RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME);
    }

    @Override
    public Boolean beyond(String propertyName, String wkt, double distance) {
        return false;
    }

    @Override
    public Boolean contains(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean crosses(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean disjoint(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean dwithin(String propertyName, String wkt, double distance) {
        return false;
    }

    @Override
    public Boolean intersects(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean overlaps(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean touches(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean within(String propertyName, String wkt) {
        return false;
    }

    @Override
    public Boolean after(String propertyName, Date date) {
        return false;
    }

    @Override
    public Boolean before(String propertyName, Date date) {
        return false;
    }

    @Override
    public Boolean during(String propertyName, Date startDate, Date endDate) {
        return false;
    }

    @Override
    public Boolean relative(String propertyName, long duration) {
        return false;
    }

}
