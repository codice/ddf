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
package org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.api;

import ddf.catalog.data.MetacardType;
import java.util.Optional;

/**
 * WfsMetacardTypeRegistry is used to associate enhanced FeatureMetacardTypes with a source and
 * simple name. This allows
 */
public interface WfsMetacardTypeRegistry {
  Optional<MetacardType> lookupMetacardTypeBySimpleName(String sourceId, String simpleName);

  void registerMetacardType(MetacardType metacardType, String sourceId, String featureSimpleName);

  void clear();
}
