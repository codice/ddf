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

import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

/**
 * An MultiInputTransformer has the purpose of creating one or more {@link Metacard}s from a given
 * {@code InputStream}.
 *
 * <p><b>Implementations of this interface <em>must</em>:</b>
 *
 * <p>Register with the OSGi Service Registry using the {@link MultiInputTransformer} interface.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface MultiInputTransformer extends TransformerProperties {

  /**
   * Transforms the input into one or more {@link Metacard}s.
   *
   * @param input the binary {@code InputStream} to transform
   * @param arguments any arguments to be used in the transformation. Keys are specific to each
   *     {@link MultiInputTransformer} implementation
   * @return the generated {@link Metacard}s
   * @throws IOException if an I/O exception occurs when reading the {@link InputStream}
   * @throws CatalogTransformerException if an error occurs during transformation
   */
  TransformResponse transform(InputStream input, Map<String, ? extends Serializable> arguments)
      throws IOException, CatalogTransformerException;
}
