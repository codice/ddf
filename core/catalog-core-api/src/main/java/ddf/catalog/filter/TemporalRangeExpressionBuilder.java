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

import java.util.Date;

import org.opengis.filter.Filter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

/**
 * Finishes the fluent API to create a Temporal Range {@link Filter}
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface TemporalRangeExpressionBuilder {

    /**
     * Create a {@link Filter} matching {@link Metacard}s where the indicated
     * {@link Attribute} is a time between the specified dates
     * @param start - the {@link Date} indicating the beginning of the range (inclusive)
     * @param end - the {@link Date} indicating the end of the range (inclusive)
     * @return {@link Filter}
     */
    public abstract Filter dates(Date start, Date end);

    /**
     * Create a {@link Filter} matching {@link Metacard}s where the indicated
     * {@link Attribute} is a time between now and the specified number of
     * milliseconds in the past (inclusive)
     * 
     * @param millis
     * @return {@link Filter}
     */
    public abstract Filter last(long millis);

    /**
     * Create a {@link Filter} matching {@link Metacard}s where the indicated
     * {@link Attribute} is a time between now and the specified number of
     * milliseconds in the future (inclusive)
     * 
     * @param millis
     * @return {@link Filter}
     */
    public abstract Filter next(long millis);

}