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
package org.codice.ddf.catalog.ui.metacard.workspace.transformations;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.Optional;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceTransformer;
import org.codice.ddf.catalog.ui.metacard.workspace.transformer.WorkspaceValueTransformation;

public class RemoveMetacardTags implements WorkspaceValueTransformation<Object, Object> {
  @Override
  public String getKey() {
    return Core.METACARD_TAGS;
  }

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
    return Optional.empty();
  }
}
