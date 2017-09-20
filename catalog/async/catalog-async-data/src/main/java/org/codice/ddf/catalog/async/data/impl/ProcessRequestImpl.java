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
package org.codice.ddf.catalog.async.data.impl;

import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.operation.impl.OperationImpl;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.async.data.api.internal.ProcessItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessRequest;

public class ProcessRequestImpl<T extends ProcessItem> extends OperationImpl
    implements ProcessRequest {

  private List<T> processItems;

  public ProcessRequestImpl(List<T> processItems, Map<String, Serializable> properties) {
    super(properties);

    notNull(processItems, "ProcessRequestImpl argument processItems may not be null");

    this.processItems = processItems;
  }

  @Override
  public List<T> getProcessItems() {
    return processItems;
  }
}
