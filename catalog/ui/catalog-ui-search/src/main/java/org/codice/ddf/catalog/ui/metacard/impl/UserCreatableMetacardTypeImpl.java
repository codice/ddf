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
package org.codice.ddf.catalog.ui.metacard.impl;

import ddf.catalog.data.MetacardType;
import java.util.Set;
import org.codice.ddf.catalog.ui.metacard.internal.UserCreatableMetacardType;

/** Uses a {@link MetacardType} to get the available type string. */
public class UserCreatableMetacardTypeImpl implements UserCreatableMetacardType {

  private final MetacardType metacardType;
  private final Set<String> visibleAttributes;

  public UserCreatableMetacardTypeImpl(MetacardType metacardType, Set<String> visibleAttributes) {
    this.metacardType = metacardType;
    this.visibleAttributes = visibleAttributes;
  }

  @Override
  public String getAvailableType() {
    return metacardType.getName();
  }

  @Override
  public Set<String> getUserVisibleAttributes() {
    return visibleAttributes;
  }
}
