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
package ddf.catalog.data;

import ddf.catalog.operation.Query;

/**
 * The Result is used to supplement {@link Metacard} fields when a {@link Metacard} is returned from
 * a query. It adds a relevance score and distance in meters to the {@link Metacard}'s existing
 * attributes.
 * 
 * @see Query
 */
public interface Result {

    /**
     * Constant used to specify a sort policy based on relevance.
     */
    public static final String RELEVANCE = "RELEVANCE";

    /**
     * Constant used to specify a sort policy based on distance.
     */
    public static final String DISTANCE = "DISTANCE";

    /**
     * Constant used to specify a sort policy based on temporal data.
     */
    public static final String TEMPORAL = "TEMPORAL";

    /**
     * Gets the {@link Metacard} wrapped by this {@code Result}.
     * 
     * @return the {@link Metacard} wrapped by this {@code Result}
     */
    public Metacard getMetacard();

    /**
     * Gets the relevance score.
     * 
     * @return relevance score (typical range of 0-1, values over 1 are those that have been
     *         boosted)
     */
    public Double getRelevanceScore();

    /**
     * Gets the distance in meters.
     * 
     * @return distance in meters, null if {@code Result} was not part of a search that was sorted
     *         by distance.
     */
    public Double getDistanceInMeters();

}
