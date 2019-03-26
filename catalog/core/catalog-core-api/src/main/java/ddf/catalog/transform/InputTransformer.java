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

import ddf.catalog.data.Metacard;
import java.io.IOException;
import java.io.InputStream;

/**
 * An InputTransformer has the purpose of creating a {@link Metacard} from a given {@code
 * InputStream}. Alternatively a {@link Metacard} implementation's constructor can be used to create
 * {@link Metacard}s.
 *
 * <p><b>Implementations of this interface <em>must</em>:</b>
 *
 * <ul>
 *   <li/>Register with the OSGi Service Registry using the {@link InputTransformer} interface
 *   <li/>Include a Service property with name "id" ({@link ddf.catalog.Constants#SERVICE_ID}) and a
 *       {@code String} value uniquely identifying the particular implementation
 * </ul>
 */
public interface InputTransformer {

  /**
   * Transforms the input into a {@link Metacard}.
   *
   * @param input the binary {@code InputStream} to transform
   * @return the generated {@link Metacard}
   * @throws IOException if an I/O exception occurs when reading the {@link InputStream}
   * @throws CatalogTransformerException if an error occurs during transformation
   */
  public Metacard transform(InputStream input) throws IOException, CatalogTransformerException;

  /**
   * Transforms the input into a {@link Metacard} and associates the id with the {@link Metacard} on
   * creation.
   *
   * @param input the binary {@code InputStream} to transform
   * @param id the attribute value for the {@link Core#ID} attribute that should be set in the
   *     generated {@link Metacard}
   * @return the generated {@link Metacard}
   * @throws IOException if an I/O exception occurs when reading the {@link InputStream}
   * @throws CatalogTransformerException if an error occurs during transformation
   */
  public Metacard transform(InputStream input, String id)
      throws IOException, CatalogTransformerException;
}
