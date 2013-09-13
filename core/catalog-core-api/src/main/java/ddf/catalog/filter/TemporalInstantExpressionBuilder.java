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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

/**
 * Completes the fluent API to build a temporal {@link Filter} using an instant in time (vs a
 * bounded range)
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface TemporalInstantExpressionBuilder {

    /**
     * Complete building the {@link Filter} matching {@link Metacard}s where the specified
     * {@link Attribute} relates to the specified date via the specified relationship.
     * 
     * @param date
     *            - the {@link Date} to be used for filtering, inclusive
     * @return {@link Filter} - temporal {@link Filter}
     */
    public abstract Filter date(Date date);

}