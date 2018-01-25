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

import ddf.catalog.data.Metacard;
import java.util.List;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.InsertAction;

/** An InsertActionImpl represents a single insert action within a CSW transaction. */
public class InsertActionImpl implements InsertAction {

  private String typeName;

  private String handle;

  private List<Metacard> records;

  /**
   * Constructs an InsertActionImpl with the specified typeName, handle, and list of records to
   * insert.
   *
   * <p>If an error occurs while processing this insert action, {@code handle} will be included in
   * the exception report response so the specific action within the transaction that caused the
   * error can be identified.
   *
   * @param typeName the type of record being inserted, such as csw:Record
   * @param handle the name to associate with this insert action
   * @param records the records to insert
   */
  public InsertActionImpl(String typeName, String handle, List<Metacard> records) {
    this.typeName = typeName;
    this.handle = handle;
    this.records = records;
  }

  @Override
  public List<Metacard> getRecords() {
    return records;
  }

  @Override
  public String getTypeName() {
    return typeName;
  }

  @Override
  public String getHandle() {
    return handle;
  }
}
