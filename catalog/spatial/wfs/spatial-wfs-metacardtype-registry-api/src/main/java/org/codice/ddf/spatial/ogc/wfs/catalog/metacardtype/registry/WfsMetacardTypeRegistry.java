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
package org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry;

import ddf.catalog.data.MetacardType;
import java.util.Optional;

/**
 * WfsMetacardTypeRegistry is used to associate enhanced FeatureMetacardTypes with a source and
 * simple name. This allows the FeatureTransformers to access MetacardType without relying on the
 * source directly.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface WfsMetacardTypeRegistry {

  /**
   * Lookup a MetacardType by source Id and simple name
   *
   * @param sourceId Id of the source that registered the MetacardType
   * @param simpleName name of the MetacardType's FeatureType
   * @return Optional containing the MetacardType if found
   */
  Optional<MetacardType> lookupMetacardTypeBySimpleName(String sourceId, String simpleName);

  /**
   * Add a MetacardType to the registry. Stored by sourceId and simpleName
   *
   * @param metacardType MetacardType to be stored
   * @param sourceId Id of the source that is storing the MetacardType
   * @param featureSimpleName The MetacardType's FeatureType name
   */
  void registerMetacardType(MetacardType metacardType, String sourceId, String featureSimpleName);

  /** Used to clear all entries of the registry */
  void clear();
}
