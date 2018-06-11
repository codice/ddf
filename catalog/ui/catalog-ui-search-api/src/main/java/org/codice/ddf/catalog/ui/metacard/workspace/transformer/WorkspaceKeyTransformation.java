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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer;

import ddf.catalog.data.Metacard;
import java.util.Optional;

/**
 * This partial implementation of {@link
 * org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformation} contains
 * default implementations of value transformation related interface methods that accept any Objects
 * and return the same values. Implementations of this interface are therefore only able to change
 * the key between a metacard attribute and a JSON-style key-value pair; the value remains
 * consistent.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public abstract class WorkspaceKeyTransformation
    implements WorkspaceTransformation<Object, Object> {
  @Override
  public Class<Object> getMetacardValueType() {
    return Object.class;
  }

  @Override
  public Class<Object> getJsonValueType() {
    return Object.class;
  }

  @Override
  public Optional<Object> metacardValueToJsonValue(
      WorkspaceTransformer transformer, Object metacardValue, Metacard workspaceMetacard) {
    return Optional.of(metacardValue);
  }

  @Override
  public Optional<Object> jsonValueToMetacardValue(
      WorkspaceTransformer transformer, Object jsonValue) {
    return Optional.of(jsonValue);
  }
}
