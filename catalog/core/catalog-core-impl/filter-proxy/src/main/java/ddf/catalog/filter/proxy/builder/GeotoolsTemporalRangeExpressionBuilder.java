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

import ddf.catalog.filter.TemporalRangeExpressionBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsBuilder;

public final class GeotoolsTemporalRangeExpressionBuilder extends GeotoolsBuilder implements
        TemporalRangeExpressionBuilder {

    GeotoolsTemporalRangeExpressionBuilder(GeotoolsBuilder builder) {
        super(builder);
        setOperator(Operator.DURING);
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.filter.TemporalRangeExpressionBuilder#dates(java.util.Date, java.util.Date)
     */
    @Override
    public Filter dates(Date date, Date date2) {
        return build(date, date2);
    }

    @Override
    public Filter last(long l) {
        setOperator(Operator.DURING_RELATIVE);
        return build(l);
    }

    @Override
    public Filter next(long l) {
        return last(l * -1);
    }

}
