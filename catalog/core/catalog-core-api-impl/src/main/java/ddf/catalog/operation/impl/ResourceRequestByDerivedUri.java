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
package ddf.catalog.operation.impl;

import ddf.catalog.data.types.Core;
import ddf.catalog.operation.ResourceRequest;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;

public class ResourceRequestByDerivedUri extends OperationImpl implements ResourceRequest {

  protected String name;

  protected URI uri;

  /**
   * Implements a ResourceRequestByDerivedUri and specifies the {@link URI}
   *
   * @param uri the URI
   */
  public ResourceRequestByDerivedUri(URI uri) {
    this(uri, null);
  }

  /**
   * Implements a ResourceRequestByDerivedUri and specifies the {@link URI} and a {@link Map} of
   * properties
   *
   * @param uri the URI
   * @param properties the properties
   */
  public ResourceRequestByDerivedUri(URI uri, Map<String, Serializable> properties) {
    super(properties);
    this.name = Core.DERIVED_RESOURCE_URI;
    this.uri = uri;
  }

  @Override
  public String getAttributeName() {
    return name;
  }

  @Override
  public URI getAttributeValue() {
    return uri;
  }
}
