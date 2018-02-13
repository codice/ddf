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
package org.codice.ddf.catalog.transform;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * The MultiMetacardTransfomer is used to transform one or more {@link Metacard}s to one or more
 * {@link BinaryContent}s. The purpose of a {@code MultiMetacardTransformer} is to change the format
 * of the {@link Metacard}. For example if the {@link Metacard} content is in an XML format, then a
 * {@code MultiMetacardTransformer} implementation can be used to transform the {@link Metacard}
 * content into an HTML format.
 *
 * <p><b>Implementations of this interface <em>must</em>:</b>
 *
 * <p>Register with the OSGi Service Registry using the {@code MultiMetacardTransformer} interface.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface MultiMetacardTransformer extends TransformerProperties {

  /**
   * Transforms the provided {@link Metacard}s into a list {@link BinaryContent}s. The number of
   * BinaryContents returned is not required to equal the number of Metacards.
   *
   * @param metacards the {@link Metacard}s to be transformed
   * @param arguments any arguments to be used in the transformation. Keys are specific to each
   *     {@link MultiMetacardTransformer} implementation
   * @return {@link List<BinaryContent>} the result of the {@link Metacard} transformation
   * @throws CatalogTransformerException if one of the {@link Metacard} can not be transformed
   */
  List<BinaryContent> transform(
      List<Metacard> metacards, Map<String, ? extends Serializable> arguments)
      throws CatalogTransformerException;
}
