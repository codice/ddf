/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.validator.query.unsupportedattributes;

import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.impl.SimpleFilterDelegate;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opengis.filter.Filter;

public class AttributeExtractor {

  private FilterAdapter filterAdapter;

  public AttributeExtractor(FilterAdapter filterAdapter) {
    this.filterAdapter = filterAdapter;
  }

  public Set<String> extractAttributes(Filter filter) throws UnsupportedQueryException {
    AttributeFilterDelegate filterDelegate = new AttributeFilterDelegate();
    filterAdapter.adapt(filter, filterDelegate);
    return filterDelegate.getAttributes();
  }

  private class AttributeFilterDelegate extends SimpleFilterDelegate<Boolean> {

    private Set<String> attributesInFilter = new HashSet<>();

    @Override
    public <S> Boolean defaultOperation(
        Object property, S literal, Class<S> literalClass, Enum operation) {
      return Boolean.TRUE;
    }

    @Override
    public <S> Boolean comparisonOperation(
        String propertyName,
        S literal,
        Class<S> literalClass,
        ComparisonPropertyOperation comparisonPropertyOperation) {
      attributesInFilter.add(propertyName);
      return Boolean.TRUE;
    }

    @Override
    public <S> Boolean spatialOperation(
        String propertyName,
        S wkt,
        Class<S> wktClass,
        SpatialPropertyOperation spatialPropertyOperation) {
      attributesInFilter.add(propertyName);
      return Boolean.TRUE;
    }

    @Override
    public <S> Boolean temporalOperation(
        String propertyName,
        S literal,
        Class<S> literalClass,
        TemporalPropertyOperation temporalPropertyOperation) {
      attributesInFilter.add(propertyName);
      return Boolean.TRUE;
    }

    @Override
    public Boolean and(List<Boolean> operands) {
      return Boolean.TRUE;
    }

    @Override
    public Boolean or(List<Boolean> operands) {
      return Boolean.TRUE;
    }

    @Override
    public Boolean not(Boolean operand) {
      return Boolean.TRUE;
    }

    public Set<String> getAttributes() {
      return this.attributesInFilter;
    }
  }
}
