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

import org.geotools.api.feature.simple.SimpleFeature;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */

/**
 * A {@code FeatureExtractor} provides methods for extracting {@link SimpleFeature} objects from
 * various resources.
 */
public interface FeatureExtractor {

  /**
   * Extracts geographic features from a resource as {@link SimpleFeature} objects and passes each
   * through the callback {@code extractionCallback}. The callback is called exactly once for each
   * {@code SimpleFeature} object extracted from the resource.
   *
   * @param resource identifier for the resource containing geographic features. The implementation
   *     decides how to resolve it (filesystem, url, etc).
   * @param extractionCallback the callback that receives each extracted {@code SimpleFeature}
   *     object, must not be null
   * @throws IllegalArgumentException if {@code extractionCallback} is null
   * @throws FeatureExtractionException if an error occurs while extracting features from the
   *     resource
   */
  void pushFeaturesToExtractionCallback(String resource, ExtractionCallback extractionCallback)
      throws FeatureExtractionException;

  interface ExtractionCallback {
    void extracted(SimpleFeature feature) throws FeatureIndexingException;
  }
}
