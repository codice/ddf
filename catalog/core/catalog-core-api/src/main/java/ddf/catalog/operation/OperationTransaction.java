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
package ddf.catalog.operation;

import ddf.catalog.data.Metacard;
import java.io.Serializable;
import java.util.List;

/**
 * Interface describing an operation transaction for the catalog. An operation transaction is added
 * to ingest requests properties by the framework so that downstream plugins can have access to the
 * original metacards. This also provides the framework with the ability to rollback a failed ingest
 * request.
 */
@Deprecated
public interface OperationTransaction extends Serializable {

  /**
   * Returns the list of metacards in the state they existed before the operation was performed.
   *
   * @return List of metacards pre operation
   */
  List<Metacard> getPreviousStateMetacards();

  /**
   * Get the type of the underlying transaction.
   *
   * @return The OperationType enum
   */
  OperationType getType();

  /** Operation transaction type */
  enum OperationType {
    CREATE,
    UPDATE,
    DELETE
  }
}
