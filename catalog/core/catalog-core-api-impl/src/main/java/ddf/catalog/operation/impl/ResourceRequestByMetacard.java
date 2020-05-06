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

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import java.io.Serializable;
import java.util.Map;

public class ResourceRequestByMetacard extends OperationImpl implements ResourceRequest {

  protected String name;

  protected Metacard metacard;

  /**
   * Implements a ResourceRequestByMetacard and specifies the metacard
   *
   * @param metacard the metacard
   */
  public ResourceRequestByMetacard(Metacard metacard) {
    this(metacard, null);
  }

  /**
   * Implements a ResourceRequestByMetacard and specifies the metacard and a ${@link Map} of
   * properties
   *
   * @param metacard the metacard
   * @param properties the properties
   */
  public ResourceRequestByMetacard(Metacard metacard, Map<String, Serializable> properties) {
    super(properties);
    this.name = GET_RESOURCE_BY_METACARD;
    this.metacard = metacard;
  }

  @Override
  public String getAttributeName() {
    return name;
  }

  @Override
  public Metacard getAttributeValue() {
    return metacard;
  }
}
