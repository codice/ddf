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
package ddf.catalog.source.solr;

import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PropertyExistsDelegate extends SimpleFilterDelegate<Boolean> {

  private List<String> propertyNames;

  public PropertyExistsDelegate(String... propertyName) {
    propertyNames = Arrays.asList(propertyName);
  }

  public PropertyExistsDelegate(List<String> propertyNames) {
    this.propertyNames = new ArrayList<>(propertyNames);
  }

  @Override
  public <S> Boolean defaultOperation(
      Object property, S literal, Class<S> literalClass, Enum operation) {
    return false;
  }

  @Override
  public Boolean and(List<Boolean> operands) {
    return operands.stream().anyMatch(op -> op);
  }

  @Override
  public Boolean or(List<Boolean> operands) {
    return operands.stream().anyMatch(op -> op);
  }

  @Override
  public Boolean not(Boolean operand) {
    return operand;
  }

  @Override
  public <S> Boolean comparisonOperation(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation comparisonPropertyOperation) {
    return propertyNames.contains(propertyName);
  }
}
