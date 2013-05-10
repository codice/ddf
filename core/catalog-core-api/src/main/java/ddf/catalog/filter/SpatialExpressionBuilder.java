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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

/**
 * Complete the fluent API to build a spatial {@link Filter}
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface SpatialExpressionBuilder {

    /**
     * Complete building a spatial {@link Filter} for {@link Metacard}s where
     * the specified {@link Attribute} relates to the specified WKT per the
     * specified operator.
     * 
     * @param wkt
     *            - WKT-formatted shape definition (2D)
     * @return {@link Filter} - spatial filter for specified WKT-defined shape
     */
    public abstract Filter wkt(String wkt);

}