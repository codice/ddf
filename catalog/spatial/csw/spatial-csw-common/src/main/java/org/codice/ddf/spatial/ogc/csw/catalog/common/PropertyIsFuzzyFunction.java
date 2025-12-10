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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.util.List;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;

/**
 * The PropertyIsFuzzyFunction contains two parameters that can be used to build a {@link
 * org.geotools.api.filter.PropertyIsLike} filter marked to be searched in a fuzzy manner.
 */
public class PropertyIsFuzzyFunction extends FunctionExpressionImpl {

  public static final String FUNCTION_NAME_STRING = "PropertyIsFuzzy";

  public static final FunctionName FUNCTION_NAME =
      new FunctionNameImpl(
          FUNCTION_NAME_STRING,
          Expression.class,
          FunctionNameImpl.parameter("expression", Expression.class));

  public PropertyIsFuzzyFunction(List<Expression> parameters, Literal fallback) {
    super(FUNCTION_NAME_STRING, fallback);

    if (parameters == null || parameters.size() != 2) {
      throw new IllegalArgumentException("PropertyIsFuzzy requires 2 parameters");
    }
    this.params = parameters;
    this.functionName = FUNCTION_NAME;
  }

  public Expression getPropertyName() {
    return params.get(0);
  }

  public Expression getLiteral() {
    return params.get(1);
  }

  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  // findbugs: parent class overrides equals() but not hashcode
  public int hashCode() {

    return 31
            * (this.getPropertyName().toString().hashCode()
                + this.getLiteral().toString().hashCode())
        + 17;
  }
}
