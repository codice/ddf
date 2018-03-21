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

import java.util.List;

/**
 * Provides a description of the structure of the WFS response XML.
 *
 * @param <T> - typically a FeatureTypeType describing FeatureMemeber fields.
 */
public interface WfsMetadata<T> {
  /** @return The value to be used as an Id on the metacard */
  String getId();

  /** @return The order of the coordinates returned by the source */
  String getCoordinateOrder();

  /** @return an unmodifiable list of objects describing the FeatureMember */
  List<T> getDescriptors();

  /** @return the name of the XML response node containing a single feature member */
  String getFeatureMemberNodeName();
}
