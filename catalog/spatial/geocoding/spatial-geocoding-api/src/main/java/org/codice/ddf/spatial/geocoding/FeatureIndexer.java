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
/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */

/**
 * A {@code FeatureIndexer} provides methods for adding {@link
 * org.geotools.api.feature.simple.SimpleFeature} objects to a new or existing local index.
 */
public interface FeatureIndexer {
  /**
   * Updates a GeoNames index with {@link org.geotools.api.feature.simple.SimpleFeature} objects
   * extracted by a {@link GeoEntryExtractor}.
   *
   * @param resource the resource containing GeoNames entries
   * @param featureExtractor the {@code FeatureExtractor} that will extract {@code SimpleFeature}
   *     objects from {@code resource}
   * @param create true will create a new index and false will add to the existing index
   * @param indexCallback called each time a feature is indexed, receives the running total of
   *     features
   * @throws FeatureExtractionException if an error occurs while extracting features from the
   *     resource
   * @throws FeatureIndexingException if an error occurs while indexing the new features
   */
  void updateIndex(
      String resource,
      FeatureExtractor featureExtractor,
      boolean create,
      IndexCallback indexCallback)
      throws FeatureExtractionException, FeatureIndexingException;

  interface IndexCallback {
    void indexed(int count);
  }
}
