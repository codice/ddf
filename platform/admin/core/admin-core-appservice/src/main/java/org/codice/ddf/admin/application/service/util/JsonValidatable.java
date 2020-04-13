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
package org.codice.ddf.admin.application.service.util;

/**
 * Interface used by Json classes that can be validated by the {@link JsonUtils} class after
 * deserialization.
 */
public interface JsonValidatable {
  /**
   * Validates this object.
   *
   * <p><i>Note:</i> This method is required because objects of this class are deserialized with
   * Gson which bypasses setters which can lead to an inconsistent and unexpected state.
   *
   * @throws IllegalArgumentException if this object is invalid
   */
  public void validate();
}
