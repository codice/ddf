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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ddf.catalog.filter.impl.SimpleFilterDelegate;

public class SearchTermsDelegate extends SimpleFilterDelegate<Set<SearchTerm>> {

    @Override
    public <S> Set<SearchTerm> defaultOperation(Object property, S literal, Class<S> literalClass,
            Enum operation) {
        return Collections.emptySet();
    }

    @Override
    public Set<SearchTerm> propertyIsLike(String propertyName, String pattern,
            boolean isCaseSensitive) {
        String[] patternWords = pattern.toLowerCase()
                .split("[\\s\\p{Punct}&&[^*]]+");

        return Arrays.stream(patternWords)
                .map(SearchTerm::new)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<SearchTerm> and(List<Set<SearchTerm>> operands) {
        return operands.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<SearchTerm> or(List<Set<SearchTerm>> operands) {
        return operands.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<SearchTerm> not(Set<SearchTerm> operand) {
        return Collections.emptySet();
    }
}
