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
package org.codice.ddf.spatial.ogc.wfs.catalog.message.api;

import ddf.catalog.data.Metacard;
import java.io.InputStream;
import java.util.List;
import java.util.function.BiFunction;

/**
 * The FeatureTransformationService aggregates the FeatureTransformer services. It splits the given
 * WFS response into individual FeatureMembers and passes those FeatureTransformer services. It
 * passes the FeatureMember to FeatureTransformers in some implementation-dependent order until one
 * of them returns something other than Optional.empty() or there are no FeatureTransformers left.
 */
public interface FeatureTransformationService
    extends BiFunction<InputStream, WfsMetadata, List<Metacard>> {

  /**
   * @param featureCollection - the WFS response XML to be de-serialized.
   * @param metadata - describes the structure of the WFS response.
   * @return an Optional containing a java.util.List of Metacards (one for each FeatureMember in the
   *     WFS response XML) or Optional.empty().
   */
  List<Metacard> apply(InputStream featureCollection, WfsMetadata metadata);
}
