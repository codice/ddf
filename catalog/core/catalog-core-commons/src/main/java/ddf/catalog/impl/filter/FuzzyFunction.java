/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl.filter;

import java.util.List;

import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used as a customized function in a {@link org.opengis.filter.Filter}. This {@link org.opengis.filter.expression.Function} will wrap a property
 * name to signify that it must be searched in a "fuzzy" manner
 *
 * @author Ashraf Barakat
 * @deprecated
 */
public class FuzzyFunction extends FunctionExpressionImpl {
    public static final String FUNCTION_NAME = "fuzzy";

    private static final Logger LOGGER = LoggerFactory.getLogger(FuzzyFunction.class);

    public static final FunctionName NAME = new FunctionNameImpl(FUNCTION_NAME, Expression.class,
            FunctionNameImpl.parameter("expression", Expression.class));

    public FuzzyFunction(List<Expression> parameters, Literal fallback) {
        super(FUNCTION_NAME, fallback);

        LOGGER.debug("INSIDE: FuzzyFunction constructor");

        if (parameters == null) {
            throw new NullPointerException("parameters required");
        }
        if (parameters.size() != 1) {
            throw new IllegalArgumentException("fuzzy( expression ) requires 1 parameter only");
        }
        this.params = parameters;
        this.functionName = NAME;

        LOGGER.debug("EXITING: FuzzyFunction constructor");
    }

}
