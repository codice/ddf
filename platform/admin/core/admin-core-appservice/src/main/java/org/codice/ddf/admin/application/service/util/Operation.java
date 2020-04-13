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
 * A list of operations that can be performed on features or bundles.
 *
 * <p><i>Note:</i> The order defined here is important and represents the order tasks will be
 * executed. When it comes to bundles, it is important to have the bundles started in the right
 * order as such we have to give priority to installation. We will then uninstall and stop
 * everything else and finish off with starting. This will make sure that we will not have 2 bundles
 * that might provide the same implementation for an API started at the same time.
 *
 * <p>The update operation is a special operation used by features where, for example, feature
 * requirements needs to be added or removed to mark the feature as required or not so it will match
 * the state it was on the original system. By Karaf's design, a required feature can be uninstalled
 * whereas as a non-required cannot. Since the goal of the profile migratable is to reset the system
 * in the exact same state it was at the time of export, updating this particular state for features
 * is also important. We therefore will do so using a final step using the {@link #UPDATE}
 * operation.
 */
public enum Operation {
  INSTALL("Installing"),
  UNINSTALL("Uninstalling"),
  STOP("Stopping"),
  START("Starting"),
  UPDATE("Updating");

  private final String operating;

  Operation(String operating) {
    this.operating = operating;
  }

  /**
   * Gets an operating name for this operation (e.g. Starting).
   *
   * @return an operating name for this operation
   */
  public String getOperatingName() {
    return operating;
  }
}
