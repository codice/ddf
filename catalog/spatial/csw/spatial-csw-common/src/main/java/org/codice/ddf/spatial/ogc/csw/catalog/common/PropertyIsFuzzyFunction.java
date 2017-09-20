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
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

/**
 * The PropertyIsFuzzyFunction contains two parameters that can be used to build a {@link
 * org.opengis.filter.PropertyIsLike} filter marked to be searched in a fuzzy manner.
 */
public class PropertyIsFuzzyFunction extends FunctionExpressionImpl {
  public static final String FUNCTION_NAME = "PropertyIsFuzzy";

  public static final FunctionName NAME =
      new FunctionNameImpl(
          FUNCTION_NAME,
          Expression.class,
          FunctionNameImpl.parameter("expression", Expression.class));

  public PropertyIsFuzzyFunction(List<Expression> parameters, Literal fallback) {
    super(FUNCTION_NAME, fallback);

    if (parameters == null || parameters.size() != 2) {
      throw new IllegalArgumentException("PropertyIsFuzzy requires 2 parameters");
    }
    this.params = parameters;
    this.functionName = NAME;
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
