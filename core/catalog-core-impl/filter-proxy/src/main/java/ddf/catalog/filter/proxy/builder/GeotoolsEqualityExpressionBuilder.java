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

import java.util.Date;

import org.opengis.filter.Filter;

import ddf.catalog.filter.EqualityExpressionBuilder;

public class GeotoolsEqualityExpressionBuilder extends GeotoolsBuilder implements
        EqualityExpressionBuilder {

    GeotoolsEqualityExpressionBuilder() {
        super();
    }

    GeotoolsEqualityExpressionBuilder(GeotoolsBuilder builder) {
        super(builder);
    }

    /*
     * Numerical
     */

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#number(float)
     */
    @Override
    public Filter number(float arg) {
        return new GeotoolsNumericalExpressionBuilder(this).number(arg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#number(double)
     */
    @Override
    public Filter number(double arg) {
        return new GeotoolsNumericalExpressionBuilder(this).number(arg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#number(int)
     */
    @Override
    public Filter number(int arg) {
        return new GeotoolsNumericalExpressionBuilder(this).number(arg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#number(short)
     */
    @Override
    public Filter number(short arg) {
        return new GeotoolsNumericalExpressionBuilder(this).number(arg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#number(long)
     */
    @Override
    public Filter number(long arg) {
        return new GeotoolsNumericalExpressionBuilder(this).number(arg);
    }

    /*
     * SPATIAL
     */
    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#wkt(java.lang.String)
     */
    @Override
    public Filter wkt(String string) {
        return (new GeotoolsSpatialExpressionBuilder(this)).wkt(string);
    }

    /*
     * TEMPORAL
     */
    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#date(java.util.Date)
     */
    @Override
    public Filter date(Date date) {
        return (new GeotoolsTemporalInstantExpressionBuilder(this)).date(date);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#dateRange(java.util.Date,
     * java.util.Date)
     */
    @Override
    public Filter dateRange(Date date, Date date2) {
        return (new GeotoolsTemporalRangeExpressionBuilder(this)).dates(date, date2);
    }

    /*
     * BOOLEAN
     */

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#bool(boolean)
     */
    @Override
    public Filter bool(boolean bool) {
        return build(bool);
    }

    /*
     * BYTES
     */

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#bytes(byte[])
     */
    @Override
    public Filter bytes(byte[] bytes) {
        return build(bytes);
    }

    /*
     * CONTEXTUAL
     */

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.proxy.builder.EqualityExpressionBuilder#text(java.lang.String)
     */
    @Override
    public Filter text(String string) {
        return (new GeotoolsContextualExpressionBuilder(this)).text(string);
    }

}