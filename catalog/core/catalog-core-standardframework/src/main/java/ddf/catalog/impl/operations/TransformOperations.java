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
package ddf.catalog.impl.operations;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Support class for transformation delegate operations for the {@code CatalogFrameworkImpl}.
 *
 * <p>This class contains two delegated transformation methods and methods to support them. No
 * operations/support methods should be added to this class except in support of CFI transformation
 * operations.
 */
public class TransformOperations {
  private FrameworkProperties frameworkProperties;

  public TransformOperations(FrameworkProperties frameworkProperties) {
    this.frameworkProperties = frameworkProperties;
  }

  //
  // Delegate methods
  //
  public List<BinaryContent> transform(
      Metacard metacard, String transformerId, Map<String, Serializable> requestProperties)
      throws CatalogTransformerException {
    return frameworkProperties
        .getTransform()
        .transform(Collections.singletonList(metacard), transformerId, requestProperties);
  }

  public BinaryContent transform(
      SourceResponse response, String transformerId, Map<String, Serializable> requestProperties)
      throws CatalogTransformerException {
    return frameworkProperties.getTransform().transform(response, transformerId, requestProperties);
  }
}
