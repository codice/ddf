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
package ddf.catalog.impl.filter;

import static org.apache.commons.lang3.Validate.isTrue;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.geotools.filter.FunctionExpressionImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;

/**
 * The DivisibleByFunction contains two parameters that can be used to build a filter.
 */
public class DivisibleByFunction extends FunctionExpressionImpl {
    public static final int NUM_PARAMETERS = 2;

    public static final String FUNCTION_NAME = "divisibleBy";

    public static final FunctionName NAME = FunctionExpressionImpl.functionName(FUNCTION_NAME,
            "return:Boolean",
            "property:String",
            "divisor:Long");

    public DivisibleByFunction(List<Expression> parameters, Literal fallback) {
        super(NAME);

        notNull(parameters, "Parameters are required");
        isTrue(parameters.size() == NUM_PARAMETERS,
                String.format("%s expression requires at least %s parameters", FUNCTION_NAME,
                        NUM_PARAMETERS));

        setParameters(parameters);
        setFallbackValue(fallback);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        DivisibleByFunction rhs = (DivisibleByFunction) obj;
        return new EqualsBuilder().appendSuper(super.equals(obj))
                .append(this.name, rhs.name)
                .append(this.params, rhs.params)
                .append(this.fallback, rhs.fallback)
                .append(this.functionName, rhs.functionName)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().appendSuper(super.hashCode())
                .append(name)
                .append(params)
                .append(fallback)
                .append(functionName)
                .toHashCode();
    }
}
