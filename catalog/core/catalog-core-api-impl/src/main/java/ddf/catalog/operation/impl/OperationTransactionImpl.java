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
package ddf.catalog.operation.impl;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.OperationTransaction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class for storing operation information. This is primarily for capturing the state before an
 * operation is performed so it can be referenced later if needed.
 */
public class OperationTransactionImpl implements OperationTransaction {

  private OperationType type;

  private List<Metacard> previousStateMetacards;

  public OperationTransactionImpl(OperationType type, Collection<Metacard> previousStateMetacards) {
    this.type = type;
    this.previousStateMetacards =
        Collections.unmodifiableList(new ArrayList(previousStateMetacards));
  }

  @Override
  public List<Metacard> getPreviousStateMetacards() {
    return previousStateMetacards;
  }

  @Override
  public OperationType getType() {
    return type;
  }
}
