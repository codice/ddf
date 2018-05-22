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
import java.util.Optional;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.metacard.workspace.QueryMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformation;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceValueTransformation;

/**
 * This partial implementation of {@link WorkspaceTransformation} is intended to transform embedded
 * metacard data in XML strings into JSON-style data maps.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public class EmbeddedMetacardsHandler implements WorkspaceValueTransformation<List, List> {
  private final String key;

  private final MetacardType metacardType;

  public EmbeddedMetacardsHandler(String key, MetacardType metacardType) {
    this.key = key;
    this.metacardType = metacardType;
  }

  // The following static factory methods are used for OSGi blueprint factory methods.
  public static EmbeddedMetacardsHandler newQueryMetacardHandler() {
    return new EmbeddedMetacardsHandler(
        WorkspaceAttributes.WORKSPACE_QUERIES, QueryMetacardImpl.TYPE);
  }

  @Override
  public String getKey() {
    return key;
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
      WorkspaceTransformer transformer, List metacardXMLStrings, Metacard workspaceMetacard) {
    return Optional.of(
        ((List<Object>) metacardXMLStrings)
            .stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .map(transformer::xmlToMetacard)
            .map(metacard -> transformer.transform(workspaceMetacard, metacard))
            .collect(Collectors.toList()));
  }

  @Override
  public Optional<List> jsonValueToMetacardValue(
      WorkspaceTransformer transformer, List metacardJsonData) {
    return Optional.of(
        ((List<Object>) metacardJsonData)
            .stream()
            .filter(Map.class::isInstance)
            .map(Map.class::cast)
            .map(
                queryJson -> {
                  final Metacard metacard = new MetacardImpl(metacardType);
                  transformer.transformIntoMetacard((Map<String, Object>) queryJson, metacard);
                  return metacard;
                })
            .map(transformer::metacardToXml)
            .collect(Collectors.toList()));
  }
}
