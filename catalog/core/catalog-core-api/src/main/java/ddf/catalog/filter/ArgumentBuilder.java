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
package ddf.catalog.filter;

import java.util.Date;

/**
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 *
 * Completes the fluent API to create a {@link org.opengis.filter.Filter} for a function that takes N parameters.
 */
public interface ArgumentBuilder {

    /**
     * Continues building the {@link ArgumentBuilder} based on a float
     *
     * @param arg - float argument
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder numberArg(float arg);

    /**
     * Continues building the {@link ArgumentBuilder} based on a double
     *
     * @param arg - double argument
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder numberArg(double arg);

    /**
     * Continues building the {@link ArgumentBuilder} based on a int
     *
     * @param arg - int argument
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder numberArg(int arg);

    /**
     * Continues building the {@link ArgumentBuilder} based on a short
     *
     * @param arg - short argument
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder numberArg(short arg);

    /**
     * Continues building the {@link ArgumentBuilder} based on a long
     *
     * @param arg - long argument
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder numberArg(long arg);

    /*
     * SPATIAL
     */

    /**
     * Continues building the {@link ArgumentBuilder} based on a WKT shape
     *
     * @param wkt - WKT-defined shape (2D)
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder wktArg(String wkt);

    /*
     * TEMPORAL
     */

    /**
     * Continues building the {@link ArgumentBuilder} based on a {@link Date}
     *
     * @param date - {@link Date}
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder dateArg(Date date);

    /**
     * Continues building the {@link ArgumentBuilder} based on a range defined by two {@link Date} instances
     *
     * @param begin - {@link Date} defining beginning of the range
     * @param end   - {@link Date} defining end of the range
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder dateRangeArg(Date begin, Date end);

    /**
     * Continues building the {@link ArgumentBuilder} based on a boolean value
     *
     * @param arg - boolean argument
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder boolArg(boolean arg);

    /**
     * Continues building the {@link ArgumentBuilder} based on byte value
     *
     * @param bytes byte array
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder bytesArg(byte[] bytes);

    /**
     * Continues building the {@link ArgumentBuilder} based on a text value
     *
     * @param text - {@link String} argument to ArgumentBuilder on
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder textArg(String text);

    /**
     * Continues building the {@link ArgumentBuilder} based on a object value
     *
     * @param obj - {@link Object} argument to ArgumentBuilder on
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder objArg(Object obj);

    /**
     * Continues building the {@link ArgumentBuilder} based on an attribute
     *
     * @param name - {@link String} argument to ArgumentBuilder on
     * @return {@link ArgumentBuilder}
     */
    ArgumentBuilder attributeArg(String name);

    /**
     * Currently only propertyIsEqualTo(Function,val) is supported so instead supporting ExpressionBuilder is() the api just has equalTo
     * <p>
     * Continue building a Filter with the "equal to" operator ( {@link org.opengis.filter.PropertyIsEqualTo})
     *
     * @return {@link EqualityExpressionBuilder} to continue building the {@link org.opengis.filter.Filter}
     */
    EqualityExpressionBuilder equalTo();

}