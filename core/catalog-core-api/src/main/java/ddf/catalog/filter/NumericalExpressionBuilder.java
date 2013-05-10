/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.filter;

import org.opengis.filter.Filter;

/**
 * Completes the fluent API to create {@link Filter} based on a numerical value.
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface NumericalExpressionBuilder {

    /**
     * Complete building a {@link Filter} with the previously specified
     * operation using an {@code int} as an argument
     * 
     * @param arg
     *            - int number to filter on
     * @return {@link Filter}
     */
    public abstract Filter number(int arg);

    /**
     * Complete building a {@link Filter} with the previously specified
     * operation using a {@code short} as an argument
     * 
     * @param arg
     *            - double number to filter on 
     * @return {@link Filter}
     */
    public abstract Filter number(short arg);

    /**
     * Complete building a {@link Filter} with the previously specified
     * operation using a {@code float}as an argument
     * 
     * @param arg
     *            - float number to filter on
     * @return {@link Filter}
     */
    public abstract Filter number(float arg);

    /**
     * Complete building a {@link Filter} with the previously specified
     * operation using a {@code double} as an argument
     * 
     * @param arg
     *            - double number to filter on
     * @return {@link Filter}
     */
    public abstract Filter number(double arg);
    
    
    /**
     * Complete building a {@link Filter} with the previously specified
     * operation using a {@code long} as an argument
     * 
     * @param arg
     *            - long number to filter on
     * @return {@link Filter}
     */
    public abstract Filter number(long arg);

}