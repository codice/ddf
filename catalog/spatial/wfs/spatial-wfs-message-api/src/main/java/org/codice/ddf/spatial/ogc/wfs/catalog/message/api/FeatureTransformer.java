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
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Given an InputStream containing a WFS FeatureMemeber XML and the required Metadata,
 * implementations of this interface will de-serialize it into a java.util.Optional containing a
 * Metacard.
 */
public interface FeatureTransformer<T>
    extends BiFunction<InputStream, WfsMetadata<T>, Optional<Metacard>> {

  /**
   * @param featureMember - the featureMember to be de-serialized
   * @param metadata - information about the FeatureMember structure
   * @return a java.util.Optional containing the de-serialized metacard or Optional.empty() in cases
   *     where the "featureMember" couldn't be de-serialized
   */
  Optional<Metacard> apply(InputStream featureMember, WfsMetadata<T> metadata);
}
