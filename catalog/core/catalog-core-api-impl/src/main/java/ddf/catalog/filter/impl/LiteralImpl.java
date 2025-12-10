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

import org.geotools.api.filter.expression.ExpressionVisitor;
import org.geotools.api.filter.expression.Literal;

/**
 * Simple implementation of filter that does not depend on GeoTools. Please use {@link
 * ddf.catalog.filter.FilterBuilder} instead to create filters.
 */
public class LiteralImpl implements Literal {

  private Object value;

  /**
   * Create literal from object value.
   *
   * @param value object value
   */
  public LiteralImpl(Object value) {
    this.value = value;
  }

  @Override
  public Object evaluate(Object object) {
    return evaluate(object, null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T evaluate(Object object, Class<T> context) {
    if (object == null) {
      return null;
    }

    if (value.getClass().equals(context)) {
      return (T) value;
    }

    if (String.class.equals(context)) {
      return context.cast(object.toString());
    }

    return null;
  }

  @Override
  public Object accept(ExpressionVisitor visitor, Object extraData) {
    return visitor.visit(this, extraData);
  }

  @Override
  public Object getValue() {
    return value;
  }
}
