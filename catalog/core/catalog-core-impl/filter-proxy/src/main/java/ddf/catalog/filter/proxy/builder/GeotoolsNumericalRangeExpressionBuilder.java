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

import ddf.catalog.filter.NumericalRangeExpressionBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsBuilder;

public final class GeotoolsNumericalRangeExpressionBuilder extends GeotoolsBuilder implements
        NumericalRangeExpressionBuilder {

    public GeotoolsNumericalRangeExpressionBuilder(GeotoolsBuilder builder) {
        super(builder);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ddf.catalog.filter.proxy.builder.NumericalRangeExpressionBuilder#numbers(java.lang.Integer,
     * java.lang.Integer)
     */
    @Override
    public Filter numbers(Integer arg0, Integer arg1) {
        return build(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.NumericalRangeExpressionBuilder#numbers(java.lang.Long,
     * java.lang.Long)
     */
    @Override
    public Filter numbers(Long arg0, Long arg1) {
        return build(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.NumericalRangeExpressionBuilder#numbers(short, short)
     */
    @Override
    public Filter numbers(Short arg0, Short arg1) {
        return build(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ddf.catalog.filter.proxy.builder.NumericalRangeExpressionBuilder#numbers(java.lang.Float,
     * java.lang.Float)
     */
    @Override
    public Filter numbers(Float arg0, Float arg1) {
        return build(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ddf.catalog.filter.proxy.builder.NumericalRangeExpressionBuilder#numbers(java.lang.Double,
     * java.lang.Double)
     */
    @Override
    public Filter numbers(Double arg0, Double arg1) {
        return build(arg0, arg1);
    }

}
