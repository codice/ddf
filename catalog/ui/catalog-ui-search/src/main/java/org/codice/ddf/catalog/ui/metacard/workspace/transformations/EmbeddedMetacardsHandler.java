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
package org.codice.ddf.catalog.ui.metacard.workspace.transformations;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformation;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;

/**
 * This partial implementation of {@link WorkspaceTransformation} is intended to transform embedded
 * metacard data in XML strings into JSON-style data maps.
 */
public interface EmbeddedMetacardsHandler extends WorkspaceTransformation<List, List> {
  @Override
  default Class<List> getExpectedMetacardType() {
    return List.class;
  }

  @Override
  default Class<List> getExpectedJsonType() {
    return List.class;
  }

  @Override
  default List<String> metacardValueToJsonValue(
      WorkspaceTransformer transformer, List metacardJsonData) {
    return ((List<Map<String, Object>>) metacardJsonData)
        .stream()
        .map(
            queryJson -> {
              final Metacard metacard = new MetacardImpl(getMetacardType());
              transformer.transformIntoMetacard(queryJson, metacard);
              return metacard;
            })
        .map(transformer::metacardToXml)
        .collect(Collectors.toList());
  }

  @Override
  default List<Map<String, Object>> jsonValueToMetacardValue(
      WorkspaceTransformer transformer, List metacardXMLStrings) {
    return ((List<String>) metacardXMLStrings)
        .stream()
        .map(transformer::xmlToMetacard)
        .map(transformer::transform)
        .collect(Collectors.toList());
  }

  /**
   * @return the {@link MetacardType} that the XML strings and JSON-style data maps are expected to
   *     describe.
   */
  MetacardType getMetacardType();
}
