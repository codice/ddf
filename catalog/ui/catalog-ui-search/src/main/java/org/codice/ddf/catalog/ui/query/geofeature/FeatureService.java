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
package org.codice.ddf.catalog.ui.query.geofeature;

import java.util.List;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface FeatureService {
  /**
   * Searches for geographic features by name, returning the names of the closest n matches.
   * Intended for use by an autocomplete widget.
   *
   * @param query search term
   * @param maxResults maximum number of results to return
   * @return list of names found
   */
  List<String> getSuggestedFeatureNames(String query, int maxResults);

  /**
   * Retrieves a specific geographic feature by its exact name (as returned by
   * getSuggestedFeatureNames).
   *
   * @param name name of a geographic feature
   * @return the feature if found, otherwise null
   */
  Feature getFeatureByName(String name);
}
