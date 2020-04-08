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
package ddf.catalog.metrics;

import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.List;

/**
 * Filter delegate to determine the types of filter features being used.
 *
 * @author Phillip Klinefelter
 */
public class QueryTypeFilterDelegate extends SimpleFilterDelegate<Boolean> {

  private boolean isSpatial = false;

  private boolean isTemporal = false;

  private boolean isLogical = false;

  private boolean isFuzzy = false;

  private boolean isCaseSensitive = false;

  private boolean isComparison = false;

  private boolean isFunction = false;

  @Override
  public <S> Boolean spatialOperation(
      String propertyName,
      S literal,
      Class<S> wktClass,
      SpatialPropertyOperation spatialPropertyOperation) {
    isSpatial = true;
    return true;
  }

  @Override
  public <S> Boolean temporalOperation(
      String propertyName,
      S literal,
      Class<S> literalClass,
      TemporalPropertyOperation temporalPropertyOperation) {
    isTemporal = true;
    return true;
  }

  @Override
  public Boolean logicalOperation(
      Object operand, LogicalPropertyOperation logicalPropertyOperation) {
    isLogical = true;
    return true;
  }

  @Override
  public <S> Boolean comparisonOperation(
      String propertyName,
      S literal,
      Class<S> literalClass,
      ComparisonPropertyOperation comparisonPropertyOperation) {
    isComparison = true;
    return true;
  }

  @Override
  public Boolean propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
    if (isCaseSensitive) {
      this.isCaseSensitive = true;
    }
    isComparison = true;
    return true;
  }

  @Override
  public Boolean propertyIsNotEqualTo(
      String propertyName, String literal, boolean isCaseSensitive) {
    if (isCaseSensitive) {
      this.isCaseSensitive = true;
    }
    isComparison = true;
    return true;
  }

  @Override
  public Boolean propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
    if (isCaseSensitive) {
      this.isCaseSensitive = true;
    }
    isComparison = true;
    return true;
  }

  @Override
  public Boolean propertyIsFuzzy(String propertyName, String literal) {
    isFuzzy = true;
    isComparison = true;
    return true;
  }

  @Override
  public Boolean propertyIsEqualTo(String functionName, List<Object> arguments, Object literal) {
    isFunction = true;
    return true;
  }

  public boolean isSpatial() {
    return isSpatial;
  }

  public boolean isTemporal() {
    return isTemporal;
  }

  public boolean isLogical() {
    return isLogical;
  }

  public boolean isFuzzy() {
    return isFuzzy;
  }

  public boolean isCaseSensitive() {
    return isCaseSensitive;
  }

  public boolean isComparison() {
    return isComparison;
  }

  public boolean isFunction() {
    return isFunction;
  }
}
