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
package ddf.catalog.content.operation.impl;

import ddf.catalog.content.operation.ReadStorageRequest;
import ddf.catalog.operation.impl.OperationImpl;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;

public class ReadStorageRequestImpl extends OperationImpl implements ReadStorageRequest {

  private URI resourceUri;

  private String id;

  public ReadStorageRequestImpl(Map<String, Serializable> properties) {
    this(null, null, properties);
  }

  public ReadStorageRequestImpl(URI resourceUri, Map<String, Serializable> properties) {
    this(resourceUri, null, properties);
  }

  public ReadStorageRequestImpl(URI resourceUri, String id, Map<String, Serializable> properties) {
    super(properties);
    if (StringUtils.isEmpty(id)) {
      this.id = UUID.randomUUID().toString();
    } else {
      this.id = id;
    }
    this.resourceUri = resourceUri;
  }

  @Override
  public URI getResourceUri() {
    return resourceUri;
  }

  @Override
  public String getId() {
    return id;
  }
}
