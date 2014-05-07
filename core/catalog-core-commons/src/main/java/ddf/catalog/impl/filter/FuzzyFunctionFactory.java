/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.impl.filter;

import java.util.ArrayList;
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

public class FuzzyFunctionFactory implements FunctionFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuzzyFunctionFactory.class);

    public List<FunctionName> getFunctionNames() {
        String methodName = "getFunctionNames";
        LOGGER.debug("ENTERING: {}", methodName);

        List<FunctionName> functionList = new ArrayList<FunctionName>();
        functionList.add(FuzzyFunction.NAME);

        LOGGER.debug("EXITING: {}", methodName);

        return Collections.unmodifiableList(functionList);
    }

    public Function function(String name, List<Expression> args, Literal fallback) {
        LOGGER.debug("INSIDE: function(String name, ...)");
        return function(new NameImpl(name), args, fallback);
    }

    public Function function(Name name, List<Expression> args, Literal fallback) {
        String methodName = "function";
        LOGGER.debug("ENTERING: {}", methodName);

        LOGGER.debug("Comparing [{}] to [{}]", FuzzyFunction.NAME.getName(), name.getLocalPart());

        if (FuzzyFunction.NAME.getName().equals(name.getLocalPart())) {
            LOGGER.debug("EXITING: {}    - returning FuzzyFunction instance", methodName);
            return new FuzzyFunction(args, fallback);
        }

        LOGGER.debug("EXITING: {}    - returning null", methodName);

        return null; // we do not implement that function
    }
}
