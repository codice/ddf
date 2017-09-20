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
package org.codice.ddf.spatial.ogc.csw.catalog.common.transaction;

/** Contains attributes shared by the concrete CSW transaction action classes. */
public abstract class CswAction {

  protected String typeName;

  // This gets used in case of exception
  protected String handle;

  public CswAction(String typeName, String handle) {
    this.typeName = typeName;
    this.handle = handle;
  }

  public String getTypeName() {
    return typeName;
  }

  public String getHandle() {
    return handle;
  }
}
