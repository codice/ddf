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

import ddf.catalog.content.operation.DeleteStorageRequest;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.impl.OperationImpl;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;

public class DeleteStorageRequestImpl extends OperationImpl implements DeleteStorageRequest {

  private List<Metacard> metacards = new ArrayList<>();

  private String id;

  public DeleteStorageRequestImpl(Map<String, Serializable> properties) {
    this(null, null, properties);
  }

  public DeleteStorageRequestImpl(List<Metacard> metacards, Map<String, Serializable> properties) {
    this(metacards, null, properties);
  }

  public DeleteStorageRequestImpl(
      List<Metacard> metacards, String id, Map<String, Serializable> properties) {
    super(properties);
    if (StringUtils.isEmpty(id)) {
      this.id = UUID.randomUUID().toString();
    } else {
      this.id = id;
    }
    if (metacards != null) {
      this.metacards = metacards;
    }
  }

  @Override
  public List<Metacard> getMetacards() {
    return metacards;
  }

  @Override
  public String getId() {
    return id;
  }
}
