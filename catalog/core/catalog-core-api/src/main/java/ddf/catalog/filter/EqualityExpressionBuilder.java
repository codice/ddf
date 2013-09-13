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
package ddf.catalog.filter;

import java.util.Date;

import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsEqualTo;

/**
 * 
 * Completes the fluent API to create a {@link PropertyIsEqualTo} {@link Filter} .
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface EqualityExpressionBuilder {

    /**
     * Completes building the {@link Filter} based on a float
     * 
     * @param arg
     *            - float argument
     * @return {@link Filter}
     */
    public abstract Filter number(float arg);

    /**
     * Completes building the {@link Filter} based on a double
     * 
     * @param arg
     *            - double argument
     * @return {@link Filter}
     */
    public abstract Filter number(double arg);

    /**
     * Completes building the {@link Filter} based on a int
     * 
     * @param arg
     *            - int argument
     * @return {@link Filter}
     */
    public abstract Filter number(int arg);

    /**
     * Completes building the {@link Filter} based on a short
     * 
     * @param arg
     *            - short argument
     * @return {@link Filter}
     */
    public abstract Filter number(short arg);

    /**
     * Completes building the {@link Filter} based on a long
     * 
     * @param arg
     *            - long argument
     * @return {@link Filter}
     */
    public abstract Filter number(long arg);

    /*
     * SPATIAL
     */

    /**
     * Completes building the {@link Filter} based on a WKT shape
     * 
     * @param wkt
     *            - WKT-defined shape (2D)
     * @return {@link Filter}
     */
    public abstract Filter wkt(String wkt);

    /*
     * TEMPORAL
     */

    /**
     * Completes building the {@link Filter} based on a {@link Date}
     * 
     * @param date
     *            - {@link Date}
     * @return {@link Filter}
     */
    public abstract Filter date(Date date);

    /**
     * Completes building the {@link Filter} based on a range defined by two {@link Date} instances
     * 
     * @param begin
     *            - {@link Date} defining beginning of the range
     * @param end
     *            - {@link Date} defining end of the range
     * @return {@link Filter}
     */
    public abstract Filter dateRange(Date begin, Date end);

    /**
     * Completes building the {@link Filter} based on a boolean value
     * 
     * @param arg
     *            - boolean value to filter on
     * @return {@link Filter}
     */
    public abstract Filter bool(boolean arg);

    /**
     * Completes building the {@link Filter} based on byte value
     * 
     * @param bytes
     *            byte array
     * @return {@link Filter}
     */
    public abstract Filter bytes(byte[] bytes);

    /**
     * Completes building the {@link Filter} based on a text value
     * 
     * @param text
     *            - {@link String} argument to filter on
     * @return {@link Filter}
     */
    public abstract Filter text(String text);

}