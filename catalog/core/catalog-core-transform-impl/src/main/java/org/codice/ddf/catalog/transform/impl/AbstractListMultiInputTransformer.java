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
package org.codice.ddf.catalog.transform.impl;

import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import org.codice.ddf.catalog.transform.ListMultiInputTransformer;
import org.codice.ddf.catalog.transform.TransformResponse;

public abstract class AbstractListMultiInputTransformer implements ListMultiInputTransformer {

  @Override
  public final TransformResponse transform(
      InputStream input, Map<String, ? extends Serializable> arguments)
      throws IOException, CatalogTransformerException {

    if (!arguments.containsKey(ListMultiInputTransformer.LIST_TYPE)) {
      throw new CatalogTransformerException(
          "This transformer may only be called in the context of a list transformation.");
    }

    return doTransform(input, arguments);
  }

  protected abstract TransformResponse doTransform(
      InputStream input, Map<String, ? extends Serializable> arguments)
      throws IOException, CatalogTransformerException;
}
