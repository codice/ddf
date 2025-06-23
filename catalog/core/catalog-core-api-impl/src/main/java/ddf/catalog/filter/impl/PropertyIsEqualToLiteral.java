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
package ddf.catalog.filter.impl;

import org.geotools.api.filter.FilterVisitor;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;

/**
 * Simple implementation of filter that does not depend on GeoTools. Please use {@link
 * ddf.catalog.filter.FilterBuilder} instead to create filters.
 */
public class PropertyIsEqualToLiteral implements PropertyIsEqualTo {

  private final PropertyName propertyName;

  private final Literal literal;

  /**
   * Create PropertyIsEqualTo filter with property name as expression 1 and literal as expression 2.
   *
   * @param propertyName property name
   * @param literal literal
   */
  public PropertyIsEqualToLiteral(PropertyName propertyName, Literal literal) {
    this.propertyName = propertyName;
    this.literal = literal;
  }

  @Override
  public Expression getExpression1() {
    return propertyName;
  }

  @Override
  public Expression getExpression2() {
    return literal;
  }

  @Override
  public boolean isMatchingCase() {
    return true;
  }

  @Override
  public MatchAction getMatchAction() {
    return MatchAction.ANY;
  }

  @Override
  public boolean evaluate(Object object) {
    if (object == null || literal == null || literal.getValue() == null) {
      return false;
    }

    return object.equals(literal.getValue());
  }

  @Override
  public Object accept(FilterVisitor visitor, Object extraData) {
    return visitor.visit(this, extraData);
  }
}
