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
package ddf.catalog.source.solr;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SimpleFilterDelegate;

/**
 * Filter delegate that returns all IDs used in metacard ID equality filters.
 */
public class MetacardIdEqualityFilterDelegate extends SimpleFilterDelegate<Set<String>> {

    @Override
    public <S> Set<String> defaultOperation(Object property, S literal, Class<S> literalClass,
            Enum operation) {
        return Collections.emptySet();
    }

    @Override
    public <S> Set<String> propertyIsEqualTo(String propertyName, S literal, Class<S> literalClass,
            ComparisonPropertyOperation operation) {
        if (Metacard.ID.equals(propertyName)) {
            return Collections.singleton(literal.toString());
        } else {
            return defaultOperation(propertyName, literal, literalClass, operation);
        }
    }

    @Override
    public <S> Set<String> propertyIsNotEqualTo(String propertyName, S literal,
            Class<S> literalClass, ComparisonPropertyOperation operation) {
        return Collections.emptySet();
    }

    @Override
    public Set<String> and(List<Set<String>> operands) {
        return joinIds(operands);
    }

    @Override
    public Set<String> or(List<Set<String>> operands) {
        return joinIds(operands);
    }

    @Override
    public Set<String> not(Set<String> operand) {
        return Collections.emptySet();
    }

    private Set<String> joinIds(List<Set<String>> operands) {
        return operands.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
