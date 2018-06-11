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
package ddf.catalog.transform;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.operation.SourceResponse;
import java.io.Serializable;
import java.util.Map;

/**
 * The {@code QueryResponseTransformer} is used to transform a list of {@link
 * ddf.catalog.data.Result} objects from a {@link SourceResponse}. For example, if the list of
 * results contains XML data, the entire list can be transformed to HTML data.
 */
public interface QueryResponseTransformer {

  /**
   * Transforms the list of results into the {@link BinaryContent}.
   *
   * @param arguments the arguments that may be used to execute the transform
   * @return the transformed content
   * @throws CatalogTransformerException if the response cannot be transformed
   */
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException;
}
