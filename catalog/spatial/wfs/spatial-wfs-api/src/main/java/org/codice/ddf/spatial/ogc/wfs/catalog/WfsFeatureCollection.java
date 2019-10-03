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
package org.codice.ddf.spatial.ogc.wfs.catalog;

import ddf.catalog.data.Metacard;
import java.util.List;

/**
 * Represents the response to a GetFeature request, after the FeatureCollection's featureMembers
 * have been transformed into {@link Metacard}s.
 */
public interface WfsFeatureCollection {
  /**
   * Returns the value of the FeatureCollection's numberOfFeatures attribute if it is present; if it
   * is not present then this method returns the size of {@link #getFeatureMembers()}.
   *
   * <p>The numberOfFeatures attribute should be present in responses to GetFeature requests that
   * have a result type of 'hits'. In this case, this method will return the total number of results
   * as opposed to the number of results returned in this response.
   *
   * @return the value of the FeatureCollection's numberOfFeatures attribute if it is present; if it
   *     is not present then this method returns the size of {@link #getFeatureMembers()}.
   */
  long getNumberOfFeatures();

  /**
   * Returns the feature members of the FeatureCollection, transformed into {@link Metacard}s.
   *
   * <p>See {@code FeatureTransformer}, {@code FeatureTransformationService}, and {@code
   * FeatureConverter} for more information about how feature members are transformed.
   *
   * @return the feature members of the FeatureCollection, transformed into {@link Metacard}s.
   */
  List<Metacard> getFeatureMembers();
}
