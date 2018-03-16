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
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.activation.MimeType;
import javax.annotation.Nullable;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Transform {

  /**
   * If the idSupplier is set and it returns a non-null value, then that value will be used as the
   * metacard id. If the supplier returns a null value, then a generated metacard id will be used.
   *
   * @param mimeType the mime-type of the inputFile
   * @param parentId optional ID of the parent metacard
   * @param idSupplier optional supplier of derived metacard IDs
   * @param inputStream the contents of this input stream will be transformed
   * @param transformerId optional transformer id
   * @param transformerArguments arguments to be passed to the transformer
   * @return list of metacards
   * @throws MetacardCreationException thrown if the input file cannot be transformed
   */
  TransformResponse transform(
      MimeType mimeType,
      @Nullable String parentId,
      @Nullable Supplier<String> idSupplier,
      InputStream inputStream,
      @Nullable String transformerId,
      Map<String, ? extends Serializable> transformerArguments)
      throws MetacardCreationException;

  /**
   * If the idSupplier is set and it returns a non-null value, then that value will be used as the
   * metacard id. If the supplier returns a null value, then a generated metacard id will be used.
   *
   * @param mimeType the mime-type of the inputFile
   * @param parentId optional ID of the parent metacard
   * @param idSupplier optional supplier of derived metacard IDs
   * @param fileName the Metacard title will be set to this value
   * @param inputFile the contents of this file will be transformed
   * @param transformerId optional transformer id
   * @param transformerArguments arguments to be passed to the transformer
   * @return list of metacards
   * @throws MetacardCreationException thrown if the input file cannot be transformed
   */
  TransformResponse transform(
      MimeType mimeType,
      @Nullable String parentId,
      @Nullable Supplier<String> idSupplier,
      String fileName,
      File inputFile,
      @Nullable String transformerId,
      Map<String, ? extends Serializable> transformerArguments)
      throws MetacardCreationException;

  /**
   * Transform a list of {@link Metacard}s into a list of {@link BinaryContent}s. The number of
   * BinaryContents need not match the number of Metacards. If a transformer ID is supplied, then a
   * transformer with that ID will be used. Otherwise, all transformers that match the given
   * mime-type will be tried. The first transformer that succeeds will be used.
   *
   * @param metacards list of metacards to transform
   * @param mimeType the requested mime-type of the binary content
   * @param transformerId optional transformer id
   * @param transformerArguments arguments to be passed to the transformer
   * @return list of binary contents
   * @throws CatalogTransformerException throw if the metacards cannot be transformed
   * @throws IllegalArgumentException thrown if a transformer cannot be found
   */
  List<BinaryContent> transform(
      List<Metacard> metacards,
      MimeType mimeType,
      @Nullable String transformerId,
      Map<String, Serializable> transformerArguments)
      throws CatalogTransformerException;

  /**
   * Transform a list of {@link Metacard}s into a list of {@link BinaryContent}s. The number of
   * BinaryContents need not match the number of Metacards.
   *
   * @param metacards list of metacards to transform
   * @param transformerId transformer id
   * @param transformerArguments arguments to be passed to the transformer
   * @return list of binary contents
   * @throws CatalogTransformerException throw if the metacards cannot be transformed
   * @throws IllegalArgumentException thrown if the transformerId is empty or null
   */
  List<BinaryContent> transform(
      List<Metacard> metacards,
      String transformerId,
      Map<String, Serializable> transformerArguments)
      throws CatalogTransformerException;

  /**
   * Determine of a transformer ID matches an existing transformer.
   *
   * @param transformerId the transformer ID
   * @return {@code true} if a transformer was found with the transformer ID, otherwise {@code
   *     false}
   */
  boolean isMetacardTransformerIdValid(String transformerId);

  /**
   * @param response the source response to be transformed
   * @param transformerId the id of the transformer to be used
   * @param transformerArguments arguments to passed to the transformer
   * @return the binary content
   * @throws CatalogTransformerException if the source response cannot be transformed
   * @throws IllegalArgumentException if the transformerId cannot be found or if the source response
   *     is null
   */
  BinaryContent transform(
      SourceResponse response, String transformerId, Map<String, Serializable> transformerArguments)
      throws CatalogTransformerException;

  /**
   * @param response the source response to be transformed
   * @param mimeType the requested mime-type of the binary content
   * @param transformerArguments arguments to passed to the transformer
   * @return an optional binary content
   * @throws CatalogTransformerException if the source response cannot be transformed
   * @throws IllegalArgumentException if the source response is null or if a transformer cannot be
   *     found that matches the mime-type
   */
  BinaryContent transform(
      SourceResponse response, MimeType mimeType, Map<String, Serializable> transformerArguments)
      throws CatalogTransformerException;
}
