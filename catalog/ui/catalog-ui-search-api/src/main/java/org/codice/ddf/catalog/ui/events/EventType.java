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
package org.codice.ddf.catalog.ui.events;

import com.google.gson.annotations.SerializedName;

public enum EventType {
  @SerializedName("resultform")
  RESULTFORM("resultform"),
  @SerializedName("searchform")
  SEARCHFORM("searchform"),
  @SerializedName("workspace")
  WORKSPACE("workspace"),
  @SerializedName("close")
  CLOSE("close");

  private final String id;

  EventType(String id) {
    this.id = id;
  }

  public String identifier() {
    return this.id;
  }
}
