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
package org.codice.ddf.catalog.ui.metacard.internal;

import java.util.Set;

/**
 * This interface informs the UI that a specific metacard type is allowed to be manually created for
 * metacard lists, and which attributes may be edited by the user. The UI may further restrict the
 * set of visible attributes.
 */
public interface UserCreatableMetacardType {

  /**
   * Get the name of the available type.
   *
   * @return name of the type
   */
  String getAvailableType();

  /**
   * Get the basic set of attribute names that may be visible to the user.
   *
   * @return set of attribute names
   */
  Set<String> getUserVisibleAttributes();
}
