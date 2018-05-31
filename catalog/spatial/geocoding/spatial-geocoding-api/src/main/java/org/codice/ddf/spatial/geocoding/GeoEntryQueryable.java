/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.geocoding;

import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import org.codice.ddf.spatial.geocoding.context.NearbyLocation;

/**
 * A {@code GeoEntryQueryable} provides methods for querying a resource containing GeoNames data.
 */
public interface GeoEntryQueryable {
  /**
   * Retrieves the top results for the given query up to {@code maxResults} results.
   *
   * @param queryString a {@code String} containing search terms
   * @param maxResults the maximum number of results to return
   * @return the top results for the query in descending order of relevance, or an empty {@code
   *     List} if no results are found
   * @throws IllegalArgumentException if {@code queryString} is null or empty, or if {@code
   *     maxResults} is not a positive integer
   * @throws GeoEntryQueryException if an exception occurs while querying the GeoNames resource
   */
  List<GeoEntry> query(String queryString, int maxResults) throws GeoEntryQueryException;

  /**
   * Retrieves the {@code GeoEntry} corresponding to the supplied identifier. {@code Suggestion} ids
   * may be passed to this method to retrieve the GeoEntry for a specific suggestion.
   *
   * @param id {@code String} identifier used to retrieve a specific {@code GeoEntry}
   * @return {@code GeoEntry} matching the identifier supplied or null if not found
   * @throws IllegalArgumentException if {@code id} is null or empty
   * @throws GeoEntryQueryException if an exception occurs while querying the GeoNames resource
   */
  GeoEntry queryById(String id) throws GeoEntryQueryException;

  /**
   * Retrieves auto-complete suggestions based on a partial or full word {@code queryString}
   *
   * @param queryString a partial or full search phrase
   * @param maxResults the maximum number of results to return
   * @return List of {@code Suggestion} corresponding to {@code queryString}
   * @throws GeoEntryQueryException if an exception occurs while querying the GeoNames resource
   */
  List<Suggestion> getSuggestedNames(String queryString, int maxResults)
      throws GeoEntryQueryException;

  /**
   * Retrieves the cities within {@code radiusInKm} kilometers of {@code metacard}, sorted by
   * population in descending order.
   *
   * <p>Each result is returned as a {@link NearbyLocation}, which describes the position of {@code
   * metacard} relative to the city.
   *
   * @param location a WKT identifying the area to search
   * @param radiusInKm the search radius, in kilometers
   * @param maxResults the maximum number of results to return
   * @return the position of {@code metacard} relative to each of the nearest cities along with the
   *     cities' names, sorted in descending order of population
   * @throws IllegalArgumentException if {@code metacard} is null, or if {@code radiusInKm} or
   *     {@code maxResults} is not a positive integer
   * @throws GeoEntryQueryException if an exception occurs while querying the GeoNames resource
   * @throws ParseException if an exceptions occurs while parsing {@code location}
   */
  List<NearbyLocation> getNearestCities(String location, int radiusInKm, int maxResults)
      throws ParseException, GeoEntryQueryException;

  /**
   * Retrieves the country code for the the area defined by {@code wktLocation} in a distance of up
   * to {@code radius} from the coordinate searched.
   *
   * @param wktLocation A WKT location
   * @param radius the radius in kilometers to search from the given {@code wktLocation}
   * @return a country code in ISO 3166-1 alpha-3 format or null if not found (for example, a
   *     location in the ocean)
   */
  Optional<String> getCountryCode(String wktLocation, int radius)
      throws GeoEntryQueryException, ParseException;
}
