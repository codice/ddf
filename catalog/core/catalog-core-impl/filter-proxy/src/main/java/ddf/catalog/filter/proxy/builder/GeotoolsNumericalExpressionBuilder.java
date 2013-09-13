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
package ddf.catalog.filter.proxy.builder;

import org.opengis.filter.Filter;

import ddf.catalog.filter.NumericalExpressionBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsBuilder;

public final class GeotoolsNumericalExpressionBuilder extends GeotoolsBuilder implements
        NumericalExpressionBuilder {

    public GeotoolsNumericalExpressionBuilder(GeotoolsBuilder builder) {
        super(builder);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.NumericalExpressionBuilder#number(int)
     */
    @Override
    public Filter number(int arg) {
        return build(arg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.NumericalExpressionBuilder#number(short)
     */
    @Override
    public Filter number(short arg) {
        return build(arg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.NumericalExpressionBuilder#number(float)
     */
    @Override
    public Filter number(float arg) {
        return build(arg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.NumericalExpressionBuilder#number(double)
     */
    @Override
    public Filter number(double arg) {
        return build(arg);
    }

    @Override
    public Filter number(long arg) {
        return build(arg);
    }

}
