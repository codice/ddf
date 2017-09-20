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
package ddf.catalog.plugin.impl;

import ddf.catalog.plugin.PolicyResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Generic implementation of {@link PolicyResponse} */
public class PolicyResponseImpl implements PolicyResponse {

  private Map<String, Set<String>> operationPolicy;

  private Map<String, Set<String>> itemPolicy;

  public PolicyResponseImpl() {
    this.operationPolicy = new HashMap<>();
    this.itemPolicy = new HashMap<>();
  }

  public PolicyResponseImpl(
      Map<String, Set<String>> operationPolicy, Map<String, Set<String>> itemPolicy) {
    this();
    if (operationPolicy != null) {
      this.operationPolicy = operationPolicy;
    }
    if (itemPolicy != null) {
      this.itemPolicy = itemPolicy;
    }
  }

  @Override
  public Map<String, Set<String>> operationPolicy() {
    return operationPolicy;
  }

  @Override
  public Map<String, Set<String>> itemPolicy() {
    return itemPolicy;
  }
}
