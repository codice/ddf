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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api;

import java.util.List;
import java.util.Map;

public interface TransformerManager {
  List<String> getAvailableMimeTypes();

  List<String> getAvailableSchemas();

  List<String> getAvailableIds();

  String getTransformerIdForSchema(String schema);

  String getTransformerSchemaForId(String id);

  String getTransformerSchemaLocationForId(String id);

  List<String> getAvailableProperty(String propertyName);

  <T> T getTransformerBySchema(String schema);

  <T> T getTransformerByMimeType(String mimeType);

  <T> T getTransformerById(String id);

  <T> T getTransformerByProperty(String property, String value);

  /**
   * Returns a list of property maps for transformers that match the given property and value
   *
   * @param property The transformer property name
   * @param value The value of the transformer property to match
   * @return List of property maps for the matching transformers
   */
  List<Map<String, Object>> getRelatedTransformerProperties(String property, String value);
}
