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
package ddf.catalog.impl.filter;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.List;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.filter.FunctionImpl;

public class ProximityFunction extends FunctionImpl {

  public static final int NUM_PARAMETERS = 3;

  public static final String FUNCTION_NAME_STRING = "proximity";

  public static final FunctionName FUNCTION_NAME =
      functionName(
          FUNCTION_NAME_STRING,
          "result:Boolean",
          "property:String",
          "distance:Integer",
          "text:String");

  public ProximityFunction(List<Expression> parameters, Literal fallback) {
    notNull(parameters, "Parameters are required");
    isTrue(
        parameters.size() == NUM_PARAMETERS,
        String.format("Proximity expression requires at least %s parameters", NUM_PARAMETERS));

    this.setName(FUNCTION_NAME_STRING);
    this.setParameters(parameters);
    this.setFallbackValue(fallback);
    this.functionName = FUNCTION_NAME;
  }
}
