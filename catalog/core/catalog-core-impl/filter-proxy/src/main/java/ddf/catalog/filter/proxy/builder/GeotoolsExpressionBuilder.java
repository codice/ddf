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

import ddf.catalog.filter.BufferedSpatialExpressionBuilder;
import ddf.catalog.filter.ContextualExpressionBuilder;
import ddf.catalog.filter.EqualityExpressionBuilder;
import ddf.catalog.filter.ExpressionBuilder;
import ddf.catalog.filter.NumericalExpressionBuilder;
import ddf.catalog.filter.NumericalRangeExpressionBuilder;
import ddf.catalog.filter.SpatialExpressionBuilder;
import ddf.catalog.filter.TemporalInstantExpressionBuilder;
import ddf.catalog.filter.TemporalRangeExpressionBuilder;

public class GeotoolsExpressionBuilder extends GeotoolsEqualityExpressionBuilder implements
        ExpressionBuilder {

    GeotoolsExpressionBuilder() {
        super();
    }

    GeotoolsExpressionBuilder(GeotoolsBuilder builder) {
        super(builder);
    }

    /*
     * SCALAR
     */
    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#lessThan()
     */
    @Override
    public NumericalExpressionBuilder lessThan() {
        setOperator(Operator.LT);
        return new GeotoolsNumericalExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#lessThanOrEqualTo()
     */
    @Override
    public NumericalExpressionBuilder lessThanOrEqualTo() {
        setOperator(Operator.LTE);
        return new GeotoolsNumericalExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#greaterThan()
     */
    @Override
    public NumericalExpressionBuilder greaterThan() {
        setOperator(Operator.GT);
        return new GeotoolsNumericalExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#greaterThanOrEqualTo()
     */
    @Override
    public NumericalExpressionBuilder greaterThanOrEqualTo() {
        setOperator(Operator.GTE);
        return new GeotoolsNumericalExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#equalTo()
     */
    @Override
    public EqualityExpressionBuilder equalTo() {
        setOperator(Operator.EQ);
        return new GeotoolsEqualityExpressionBuilder(this);
    }

    /*
     * CONTEXTUAL
     */
    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#like()
     */
    @Override
    public ContextualExpressionBuilder like() {
        setOperator(Operator.LIKE);
        return new GeotoolsContextualExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#between()
     */
    @Override
    public NumericalRangeExpressionBuilder between() {
        setOperator(Operator.BETWEEN);
        return new GeotoolsNumericalRangeExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#notEqualTo()
     */
    @Override
    public EqualityExpressionBuilder notEqualTo() {
        setOperator(Operator.NEQ);
        return (EqualityExpressionBuilder) this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#empty()
     */
    @Override
    public Filter empty() {
        setOperator(Operator.NULL);
        return build(null);
    }

    /*
     * TEMPORAL
     */

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#after()
     */
    @Override
    public TemporalInstantExpressionBuilder after() {
        setOperator(Operator.AFTER);
        return new GeotoolsTemporalInstantExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#before()
     */
    @Override
    public TemporalInstantExpressionBuilder before() {
        setOperator(Operator.BEFORE);
        return new GeotoolsTemporalInstantExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#during()
     */
    @Override
    public TemporalRangeExpressionBuilder during() {
        setOperator(Operator.DURING);
        return new GeotoolsTemporalRangeExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#overlapping()
     */
    @Override
    public TemporalRangeExpressionBuilder overlapping() {
        setOperator(Operator.TOVERLAPS);
        return new GeotoolsTemporalRangeExpressionBuilder(this);
    }

    /*
     * SPATIAL
     */
    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#intersecting()
     */
    @Override
    public SpatialExpressionBuilder intersecting() {
        setOperator(Operator.INTERSECTS);
        return new GeotoolsSpatialExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#containing()
     */
    @Override
    public SpatialExpressionBuilder containing() {
        setOperator(Operator.CONTAINS);
        return new GeotoolsSpatialExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#beyond()
     */
    @Override
    public BufferedSpatialExpressionBuilder beyond() {
        setOperator(Operator.BEYOND);
        return new GeotoolsBufferedSpatialExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#within()
     */
    @Override
    public SpatialExpressionBuilder within() {
        setOperator(Operator.WITHIN);
        return new GeotoolsSpatialExpressionBuilder(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.ExpressionBuilder#withinBuffer()
     */
    @Override
    public BufferedSpatialExpressionBuilder withinBuffer() {
        setOperator(Operator.DWITHIN);
        return new GeotoolsBufferedSpatialExpressionBuilder(this);
    }

    @Override
    public SpatialExpressionBuilder nearestTo() {
        setOperator(Operator.BEYOND);
        return new GeotoolsSpatialExpressionBuilder(this);
    }

}
