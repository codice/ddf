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

import org.opengis.filter.Filter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

/**
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 * 
 */
public interface NumericalRangeExpressionBuilder {

    /**
     * Completes building a Numerical Range Filter, using a range between two {@link Integer}s
     * 
     * @param arg0
     *            the bottom range, inclusive
     * @param arg1
     *            the top of the range, inclusive
     * @return {@link Filter} a filter that will pass only {@link Metacard}s where the indicated
     *         {@link Attribute} is within the indicated range
     */
    public abstract Filter numbers(Integer arg0, Integer arg1);

    /**
     * Completes building a Numerical Range Filter, using a range between two {@link Long}s
     * 
     * @param arg0
     *            the bottom range, inclusive
     * @param arg1
     *            the top of the range, inclusive
     * @return {@link Filter} a filter that will pass only {@link Metacard}s where the indicated
     *         {@link Attribute} is within the indicated range
     */
    public abstract Filter numbers(Long arg0, Long arg1);

    /**
     * Completes building a Numerical Range Filter, using a range between two numbers of type
     * {@link Short}
     * 
     * @param arg0
     *            the bottom range, inclusive
     * @param arg1
     *            the top of the range, inclusive
     * @return {@link Filter} a filter that will pass only {@link Metacard}s where the indicated
     *         {@link Attribute} is within the indicated range
     */
    public abstract Filter numbers(Short arg0, Short arg1);

    /**
     * Completes building a Numerical Range Filter, using a range between two numbers of type
     * {@link Float}
     * 
     * @param arg0
     *            the bottom range, inclusive
     * @param arg1
     *            the top of the range, inclusive
     * @return {@link Filter} a filter that will pass only {@link Metacard}s where the indicated
     *         {@link Attribute} is within the indicated range
     */
    public abstract Filter numbers(Float arg0, Float arg1);

    /**
     * Completes building a Numerical Range Filter, using a range between two numbers of type
     * {@link Double}
     * 
     * @param arg0
     *            the bottom range, inclusive
     * @param arg1
     *            the top of the range, inclusive
     * @return {@link Filter} a filter that will pass only {@link Metacard}s where the indicated
     *         {@link Attribute} is within the indicated range
     */
    public abstract Filter numbers(Double arg0, Double arg1);

}