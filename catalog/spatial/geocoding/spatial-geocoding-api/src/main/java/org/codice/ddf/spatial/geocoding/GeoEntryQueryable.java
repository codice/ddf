/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.geocoding;

import java.text.ParseException;
import java.util.List;

import org.codice.ddf.spatial.geocoding.context.NearbyLocation;

/**
 * A {@code GeoEntryQueryable} provides methods for querying a resource containing GeoNames data.
 */
public interface GeoEntryQueryable {
    /**
     * Retrieves the top results for the given query up to {@code maxResults} results.
     *
     * @param queryString  a {@code String} containing search terms
     * @param maxResults  the maximum number of results to return
     * @return the top results for the query in descending order of relevance, or an empty
     *         {@code List} if no results are found
     * @throws IllegalArgumentException if {@code queryString} is null or empty, or if
     *                                  {@code maxResults} is not a positive integer
     * @throws GeoEntryQueryException if an exception occurs while querying the GeoNames resource
     */
    List<GeoEntry> query(String queryString, int maxResults);

    /**
     * Retrieves the cities within {@code radiusInKm} kilometers of {@code metacard}, sorted by
     * population in descending order.
     * <p>
     * Each result is returned as a {@link NearbyLocation}, which describes the position of
     * {@code metacard} relative to the city.
     *
     * @param location  a WKT identifying the area to search
     * @param radiusInKm  the search radius, in kilometers
     * @param maxResults  the maximum number of results to return
     * @return the position of {@code metacard} relative to each of the nearest cities along with
     *         the cities' names, sorted in descending order of population
     * @throws IllegalArgumentException if {@code metacard} is null, or if {@code radiusInKm} or
     *                                  {@code maxResults} is not a positive integer
     * @throws GeoEntryQueryException if an exception occurs while querying the GeoNames resource
     */
    List<NearbyLocation> getNearestCities(String location, int radiusInKm, int maxResults)
            throws ParseException;
}
