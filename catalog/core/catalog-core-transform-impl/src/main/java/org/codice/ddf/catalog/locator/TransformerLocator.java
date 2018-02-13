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
package org.codice.ddf.catalog.locator;

import ddf.catalog.transform.QueryResponseTransformer;
import java.util.List;
import javax.activation.MimeType;
import org.codice.ddf.catalog.transform.MultiInputTransformer;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface TransformerLocator {

  /**
   * Find all input transformers.
   *
   * @return non-null list of transformers
   */
  List<MultiInputTransformer> findMultiInputTransformers();

  /**
   * Find all input transformers that support the given mime type.
   *
   * @param mimeType search for this mime type
   * @return non-null list of transformers
   */
  List<MultiInputTransformer> findMultiInputTransformers(MimeType mimeType);

  /**
   * Find all input transformers that match the given ID.
   *
   * @param transformerId search for this ID
   * @return non-null list of transformers
   */
  List<MultiInputTransformer> findMultiInputTransformers(String transformerId);

  /**
   * Find all metacard transformers that match the given ID.
   *
   * @param transformerId search for this ID
   * @return non-null list of transformers
   */
  List<MultiMetacardTransformer> findMultiMetacardTransformers(String transformerId);

  /**
   * Find all metacard transformers that match the given mime-type.
   *
   * @param mimeType search for this mime type
   * @return non-null list of transformers
   */
  List<MultiMetacardTransformer> findMultiMetacardTransformers(MimeType mimeType);

  /**
   * Find all query response transformers that support the given mime type.
   *
   * @param mimeType search for this mime type
   * @return non-null list of transformers
   */
  List<QueryResponseTransformer> findQueryResponseTransformers(MimeType mimeType);

  /**
   * Find all query response transformers that match the given ID.
   *
   * @param transformerId search for this ID
   * @return non-null list of transformers
   */
  List<QueryResponseTransformer> findQueryResponseTransformers(String transformerId);
}
