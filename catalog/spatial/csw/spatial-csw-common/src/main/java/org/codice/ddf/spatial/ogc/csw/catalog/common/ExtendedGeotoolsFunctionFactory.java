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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

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

/**
 * The ExtendedGeotoolsFunctionFactory is used to provide the GeoTools CommonFactoryFinder a list of custom
 * functions.  This allows for DDF to extend the OGC Filter 1.1.0 schema and pass along the extension
 * changes to Geotools via a service loader .
 */
public class ExtendedGeotoolsFunctionFactory implements FunctionFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedGeotoolsFunctionFactory.class);

    public List<FunctionName> getFunctionNames() {
        return Collections.singletonList(PropertyIsFuzzyFunction.NAME);
    }

    public Function function(String name, List<Expression> args, Literal fallback) {
        return function(new NameImpl(name), args, fallback);
    }

    public Function function(Name name, List<Expression> args, Literal fallback) {

        if (PropertyIsFuzzyFunction.NAME.getName()
                .equals(name.getLocalPart())) {

            return new PropertyIsFuzzyFunction(args, fallback);
        }

        return null; // we do not implement that function
    }
}
