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

import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.workspace.QueryMetacardTypeImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceValueTransformation;

public class QuerySortsParser implements WorkspaceValueTransformation<List, List> {
  @Override
  public Class<List> getExpectedJsonType() {
    return List.class;
  }

  @Override
  public Class<List> getExpectedMetacardType() {
    return List.class;
  }

  @Override
  public String getKey() {
    return QueryMetacardTypeImpl.QUERY_SORTS;
  }

  @Override
  public List<String> jsonValueToMetacardValue(WorkspaceTransformer transformer, List jsonValue) {
    return ((List<Object>) jsonValue)
        .stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(
            sortsObject ->
                sortsObject.getOrDefault("attribute", "")
                    + ","
                    + sortsObject.getOrDefault("direction", ""))
        .collect(Collectors.toList());
  }

  @Override
  public List<Map<String, String>> metacardValueToJsonValue(
      WorkspaceTransformer transformer, List metacardValue) {
    return ((List<Object>) metacardValue)
        .stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(
            sortString -> {
              final String[] sortParameters = sortString.split(",");
              // @formatter:off
              return ImmutableMap.of(
                  "attribute", sortParameters[0],
                  "direction", sortParameters[1]);
              // @formatter:on
            })
        .collect(Collectors.toList());
  }
}
