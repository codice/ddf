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

import java.util.List;
import org.geotools.api.feature.simple.SimpleFeature;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */

/**
 * A {@code FeatureQueryable} provides methods for querying a resource containing geographic
 * features.
 */
public interface FeatureQueryable {
  /**
   * Retrieves the top results for the given query up to {@code maxResults} results.
   *
   * @param queryString a {@code String} containing search terms
   * @param featureCode geonames feature code identifying the type of feature to search for. this
   *     can be null and the implementation can choose to ignore it.
   * @param maxResults the maximum number of results to return
   * @return the top results for the query in descending order of relevance, or an empty {@code
   *     List} if no results are found
   * @throws IllegalArgumentException if {@code queryString} is null or empty, or if {@code
   *     maxResults} is not a positive integer
   * @throws FeatureQueryException if an exception occurs while querying
   */
  List<SimpleFeature> query(String queryString, String featureCode, int maxResults)
      throws FeatureQueryException;
}
