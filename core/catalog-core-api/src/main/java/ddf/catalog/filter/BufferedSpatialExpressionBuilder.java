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
 * Continues the fluent API to create a Spatial {@link Filter}. Extends
 * {@link SpatialExpressionBuilder} to add the ability to buffer the shape.
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface BufferedSpatialExpressionBuilder extends SpatialExpressionBuilder {

    /**
     * Creates a {@link Filter} matching against a wkt-defined shape buffered by
     * the specified distance
     * 
     * @param wkt
     *            - WKT representing shape to be buffered.
     * @param buffer
     *            - distance in meters
     * @return {@link Filter} - the Buffered Spatial Filter
     */
    public abstract Filter wkt(String wkt, double buffer);

}