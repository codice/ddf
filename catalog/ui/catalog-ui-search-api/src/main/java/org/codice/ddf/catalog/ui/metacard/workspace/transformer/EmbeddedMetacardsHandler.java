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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This partial implementation of {@link WorkspaceTransformation} is intended to transform embedded
 * metacard data in XML strings into JSON-style data maps.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public abstract class EmbeddedMetacardsHandler extends WorkspaceValueTransformation<List, List> {

  @Override
  public Class<List> getMetacardValueType() {
    return List.class;
  }

  @Override
  public Class<List> getJsonValueType() {
    return List.class;
  }

  /**
   * Wrap the given {@link Metacard} inside a new metacard with {@link #getMetacardType() the
   * metacard type expected for this transformation}. This is necessary to prevent XML
   * transformation classes from incorrectly setting the transformed {@link Metacard}'s {@link
   * MetacardType}.
   *
   * @param metacard the {@link Metacard} to be wrapped and typed.
   * @return a {@link Metacard} wrapping the given {@code metacard} with {@link #getMetacardType()
   *     the appropriate metacard type}.
   */
  private Metacard setMetacardType(Metacard metacard) {
    Metacard newMetacard = new MetacardImpl(getMetacardType());
    getMetacardType()
        .getAttributeDescriptors()
        .forEach(
            descriptor -> newMetacard.setAttribute(metacard.getAttribute(descriptor.getName())));
    return newMetacard;
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
            .map(this::setMetacardType)
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
                  final Metacard metacard = new MetacardImpl(getMetacardType());
                  transformer.transformIntoMetacard((Map<String, Object>) queryJson, metacard);
                  return metacard;
                })
            .map(transformer::metacardToXml)
            .collect(Collectors.toList()));
  }

  /**
   * @return the {@link MetacardType} that the XML strings and JSON-style data maps are expected to
   *     describe.
   */
  public abstract MetacardType getMetacardType();
}
