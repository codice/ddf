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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.geotools.feature.NameImpl;
import org.geotools.filter.FunctionFactory;
import org.opengis.feature.type.Name;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoToolsFunctionFactory implements FunctionFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoToolsFunctionFactory.class);

    private static final List<FunctionName> FUNCTION_NAMES =
            Collections.unmodifiableList(Arrays.asList(FuzzyFunction.NAME,
                    ProximityFunction.NAME, DivisibleByFunction.NAME));

    @Override
    public List<FunctionName> getFunctionNames() {
        return FUNCTION_NAMES;
    }

    @Override
    public Function function(String name, List<Expression> args, Literal fallback) {
        LOGGER.trace("INSIDE: function(String name, ...)");
        return function(new NameImpl(name), args, fallback);
    }

    @Override
    public Function function(Name name, List<Expression> args, Literal fallback) {
        String methodName = "function";
        LOGGER.trace("ENTERING: {}", methodName);

        if (FuzzyFunction.NAME.getName()
                .equals(name.getLocalPart())) {
            LOGGER.trace("EXITING: {} - returning FuzzyFunction instance", methodName);
            return new FuzzyFunction(args, fallback);
        }

        if (ProximityFunction.NAME.getName()
                .equals(name.getLocalPart())) {
            LOGGER.trace("Inside function : returning {}", ProximityFunction.NAME);
            return new ProximityFunction(args, fallback);
        }

        if (DivisibleByFunction.NAME.getName()
                .equals(name.getLocalPart())) {
            LOGGER.trace("Inside function : returning {}", DivisibleByFunction.NAME);
            return new DivisibleByFunction(args, fallback);
        }

        LOGGER.trace("EXITING: {} - returning null", methodName);

        return null; // we do not implement that function
    }
}
