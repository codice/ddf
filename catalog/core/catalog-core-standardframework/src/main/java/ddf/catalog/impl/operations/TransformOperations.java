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

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.Serializable;
import java.util.Map;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

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
  public BinaryContent transform(
      Metacard metacard, String transformerId, Map<String, Serializable> requestProperties)
      throws CatalogTransformerException {
    if (metacard == null) {
      throw new IllegalArgumentException("Metacard is null.");
    }

    ServiceReference[] refs;
    try {
      // TODO replace shortname with id
      refs =
          frameworkProperties
              .getBundleContext()
              .getServiceReferences(
                  MetacardTransformer.class.getName(),
                  "(|"
                      + "("
                      + Constants.SERVICE_SHORTNAME
                      + "="
                      + transformerId
                      + ")"
                      + "("
                      + Constants.SERVICE_ID
                      + "="
                      + transformerId
                      + ")"
                      + ")");
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Invalid transformer shortName: " + transformerId, e);
    }
    if (refs == null || refs.length == 0) {
      throw new IllegalArgumentException("Transformer " + transformerId + " not found");
    }

    MetacardTransformer transformer =
        (MetacardTransformer) frameworkProperties.getBundleContext().getService(refs[0]);
    return transformer.transform(metacard, requestProperties);
  }

  public BinaryContent transform(
      SourceResponse response, String transformerId, Map<String, Serializable> requestProperties)
      throws CatalogTransformerException {
    if (response == null) {
      throw new IllegalArgumentException("QueryResponse is null.");
    }

    ServiceReference[] refs;
    try {
      refs =
          frameworkProperties
              .getBundleContext()
              .getServiceReferences(
                  QueryResponseTransformer.class.getName(),
                  "(|"
                      + "("
                      + Constants.SERVICE_SHORTNAME
                      + "="
                      + transformerId
                      + ")"
                      + "("
                      + Constants.SERVICE_ID
                      + "="
                      + transformerId
                      + ")"
                      + ")");
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Invalid transformer id: " + transformerId, e);
    }

    if (refs == null || refs.length == 0) {
      throw new IllegalArgumentException("Transformer " + transformerId + " not found");
    } else {
      QueryResponseTransformer transformer =
          (QueryResponseTransformer) frameworkProperties.getBundleContext().getService(refs[0]);
      return transformer.transform(response, requestProperties);
    }
  }
}
