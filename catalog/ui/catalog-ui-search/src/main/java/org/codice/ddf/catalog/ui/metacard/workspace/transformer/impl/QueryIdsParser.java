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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer.impl;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.Metacard;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceConstants;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceValueTransformation;

public class QueryIdsParser extends WorkspaceValueTransformation<List, List> {

  @Override
  public String getKey() {
    return WorkspaceConstants.WORKSPACE_QUERIES;
  }

  @Override
  public Class<List> getMetacardValueType() {
    return List.class;
  }

  @Override
  public Class<List> getJsonValueType() {
    return List.class;
  }

  @Override
  public Optional<List> metacardValueToJsonValue(
      WorkspaceTransformer transformer, List metacardValue, Metacard workspaceMetacard) {
    return Optional.of(
        ((List<Object>) metacardValue)
            .stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(queryId -> ImmutableMap.of("id", queryId))
            .collect(Collectors.toList()));
  }

  @Override
  public Optional<List> jsonValueToMetacardValue(WorkspaceTransformer transformer, List jsonValue) {
    return Optional.of(
        ((List<Object>) jsonValue)
            .stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(queryId -> queryId.getOrDefault("id", ""))
            .collect(Collectors.toList()));
  }
}
