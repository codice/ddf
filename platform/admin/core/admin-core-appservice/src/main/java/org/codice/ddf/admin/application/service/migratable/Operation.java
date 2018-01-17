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
package org.codice.ddf.admin.application.service.migratable;

/**
 * A list of operations that can be performed on applications, features, or bundles.
 *
 * <p><i>Note:</i> The order defined here is important and represents the order tasks will be
 * executed.
 */
public enum Operation {
  INSTALL("Installing"),
  START("Starting"),
  STOP("Stopping"),
  UNINSTALL("Uninstalling");

  private final String operating;

  Operation(String operating) {
    this.operating = operating;
  }

  /**
   * Gets an operating name for this operation (e.g. Starting).
   *
   * @return an operating name for this operation
   */
  public String operatingName() {
    return operating;
  }
}
