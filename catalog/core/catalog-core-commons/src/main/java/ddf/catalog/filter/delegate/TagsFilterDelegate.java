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

package ddf.catalog.filter.delegate;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SimpleFilterDelegate;

public class TagsFilterDelegate extends SimpleFilterDelegate<Boolean> {

    public static final String NULL_TAGS = Metacard.TAGS + "_NULL";

    private Set<String> tags;

    public TagsFilterDelegate() {

    }

    public TagsFilterDelegate(String type) {
        this(Collections.singleton(type));
    }

    public TagsFilterDelegate(Set<String> types) {
        this.tags = types;
    }

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
                .allMatch(op -> op);
    }

    @Override
    public Boolean not(Boolean operand) {
        return operand;
    }

    @Override
    public Boolean propertyIsEqualTo(String propertyName, String pattern, boolean isCaseSensitive) {
        return propertyName.equals(Metacard.TAGS) && (tags == null || tags.contains(pattern));
    }

    @Override
    public Boolean propertyIsNull(String propertyName) {
        return propertyName.equals(Metacard.TAGS) && (tags == null || tags.contains(NULL_TAGS));
    }

    @Override
    public Boolean propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        return propertyName.equals(Metacard.TAGS) && (tags == null || tags.contains(pattern));
    }
}
