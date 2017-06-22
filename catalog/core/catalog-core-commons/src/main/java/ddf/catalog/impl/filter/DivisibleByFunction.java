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

import org.geotools.filter.FunctionExpressionImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * The DivisibleByFunction contains two parameters that can be used to build a filter.
 */
public class DivisibleByFunction extends FunctionExpressionImpl {
    public static final int NUM_PARAMETERS = 2;

    public static final String FUNCTION_NAME = "divisibleBy";

    public static final FunctionName NAME = FunctionExpressionImpl.functionName(FUNCTION_NAME,
            "result:Boolean",
            "property:String",
            "divisor:Long");

    public DivisibleByFunction(List<Expression> parameters, Literal fallback) {
        super(FUNCTION_NAME, fallback);

        notNull(parameters, "Parameters are required");
        isTrue(parameters.size() == NUM_PARAMETERS,
                String.format("%s expression requires at least %s parameters", FUNCTION_NAME,
                        NUM_PARAMETERS));

        //TODO we could cast the arguments here to the correct object so that it doen't need to be done in the filter delegates

        setName(FUNCTION_NAME);
        setParameters(parameters);
        setFallbackValue(fallback);
        functionName = NAME;
    }

    public String getPropertyName() {
        return ((PropertyName) params.get(0)).getPropertyName();
    }

    public Long getLiteral() {
        return Long.parseLong(((Literal) params.get(1)).getValue()
                .toString());
    }

    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    //findbugs: parent class overrides equals() but not hashcode
    public int hashCode() {

        return 31 * (getPropertyName().toString()
                .hashCode() + getLiteral().toString()
                .hashCode()) + 17;
    }

}
