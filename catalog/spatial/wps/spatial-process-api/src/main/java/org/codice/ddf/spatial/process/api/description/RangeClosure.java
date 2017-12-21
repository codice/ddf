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
package org.codice.ddf.spatial.process.api.description;

/** This class is Experimental and subject to change */
public enum RangeClosure {
  OPEN("open"),
  CLOSE("close"),
  OPEN_CLOSE("open-close"),
  CLOSE_OPEN("close-open");

  private String value;

  RangeClosure(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static RangeClosure fromValue(String v) {
    for (RangeClosure c : RangeClosure.values()) {
      if (c.value.equals(v)) {
        return c;
      }
    }
    throw new IllegalArgumentException(v);
  }
}
