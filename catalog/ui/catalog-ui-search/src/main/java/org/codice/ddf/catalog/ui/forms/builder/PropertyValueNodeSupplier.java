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
package org.codice.ddf.catalog.ui.forms.builder;

import static org.apache.commons.lang3.Validate.notNull;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The {@link PropertyValueNodeSupplier} collects two, and only two, filter nodes and converts them
 * to a single node. This is useful for most terminal types.
 *
 * @param <T> the type being used to represent a node in the filter tree.
 */
class PropertyValueNodeSupplier<T> implements NodeSupplier<T> {
  private final NodeReducer<T> reducer;
  private T propertyNode = null;
  private T valueNode = null;

  PropertyValueNodeSupplier(final NodeReducer<T> reducer) {
    notNull(reducer);
    this.reducer = reducer;
  }

  @Nullable
  @Override
  public NodeSupplier<T> getParent() {
    // Always return null since most terminal nodes, unlike functions, do not require nesting
    return null;
  }

  @Override
  public void setNext(T node) {
    notNull(node);
    if (propertyNode == null) {
      propertyNode = node;
      return;
    }
    if (valueNode == null) {
      valueNode = node;
      return;
    }
    throw new IllegalStateException(
        "Expressions can occur a maximum of '2' times in the current "
            + "sequence but this limit was exceeded; no child element is expected at this point: "
            + node.toString());
  }

  @Override
  public T get() {
    // Commons lang 'notNull' used above since those errors are dataflow errors
    // Illegal state thrown here because these conditions may occur from improper use of builder
    required(propertyNode, "Expected minimum of '2' expressions but property was null");
    required(valueNode, "Expected minimum of '2' expressions but value was null");
    List<T> terminals = new ArrayList<>();
    terminals.add(propertyNode);
    terminals.add(valueNode);
    return reducer.apply(terminals);
  }

  private static void required(Object object, String message) {
    if (object == null) {
      throw new IllegalStateException(message);
    }
  }
}
