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
package org.codice.ddf.catalog.ui.forms.api;

import org.codice.ddf.catalog.ui.forms.model.FunctionFilterNode;
import org.codice.ddf.catalog.ui.forms.model.IntermediateFilterNode;
import org.codice.ddf.catalog.ui.forms.model.LeafFilterNode;

/**
 * Represents a single node in a filter data structure.
 *
 * <p><i>This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library.</i>
 */
public interface FilterNode {

  /**
   * Get the operator assocated with this node. Every node has an operator.
   *
   * @return this node's operator.
   */
  String getOperator();

  void accept(Visitor visitor);

  interface Visitor {
    void visit(IntermediateFilterNode filterNode);

    void visit(LeafFilterNode filterNode);

    void visit(FunctionFilterNode filterNode);
  }
}
