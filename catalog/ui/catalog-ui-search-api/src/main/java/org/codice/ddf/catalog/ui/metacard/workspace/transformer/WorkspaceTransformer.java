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
import java.util.List;
import java.util.Map;

/**
 * A utility used to convert workspace metacard data between the JSON data expected from frontend
 * responses and the {@link Metacard} objects used in the backend.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface WorkspaceTransformer {
  /**
   * Transforms the given metacard JSON data according to available {@link WorkspaceTransformation}s
   * and inserts the results into the given {@link Metacard} object.
   *
   * @param json the JSON-style map representing the metacard data
   * @param init the {@link Metacard} into which the transformed data will be inserted
   */
  void transformIntoMetacard(Map<String, Object> json, Metacard init);

  /**
   * Transforms the given metacard JSON data into a new {@link Metacard} object.
   *
   * @param json the JSON-style map representing the metacard data
   * @return a new {@link Metacard} containing the results of {@link WorkspaceTransformation}s of
   *     the given JSON data
   */
  Metacard transform(Map<String, Object> json);

  /**
   * Transforms the given workspace {@link Metacard} into a JSON-style key-value pair {@link Map}
   * using all available {@link WorkspaceTransformation}s.
   *
   * @param workspaceMetacard the workspace-type {@link Metacard} to be transformed
   * @return a new JSON-style {@link Map} containing the results of {@link WorkspaceTransformation}s
   *     performed on the given workspace {@link Metacard}'s data
   */
  Map<String, Object> transform(Metacard workspaceMetacard);

  /**
   * Transforms the given {@link Metacard} {@code metacard} into a JSON-style key-value pair {@link
   * Map} using all available {@link WorkspaceTransformation}s.
   *
   * @param workspaceMetacard the workspace metacard that started the transformation
   * @param metacard the {@link Metacard} to transform into JSON-style data; this will either be
   *     equivalent to the argument {@code workspaceMetacard} or will be a {@link Metacard} embedded
   *     inside {@code workspaceMetacard}
   * @return a new JSON-style {@link Map} containing the results of {@link WorkspaceTransformation}s
   *     performed on the given {@link Metacard}'s data
   */
  Map<String, Object> transform(Metacard workspaceMetacard, Metacard metacard);

  /**
   * Transforms a list of {@link Metacard}s into JSON-style data maps.
   *
   * @param metacards the list of {@link Metacard}s to be transformed.
   * @return a list of new JSON-style {@link Map}s containing the result of each {@link Metacard}'s
   *     data run through all available {@link WorkspaceTransformation}s
   */
  List<Map<String, Object>> transform(List<Metacard> metacards);

  /**
   * Losslessly serializes a {@link Metacard} into an XML string.
   *
   * @param metacard the {@link Metacard} to be serialized into XML
   * @return an XML string representing all data contained within the given {@link Metacard}
   */
  String metacardToXml(Metacard metacard);

  /**
   * Losslessly deserializes an XML string into a new {@link Metacard} object.
   *
   * @param xml the XML string to parse into a {@link Metacard} object
   * @return a new {@link Metacard} containing all of the data available in the given XML string
   */
  Metacard xmlToMetacard(String xml);
}
