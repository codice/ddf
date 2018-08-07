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

import static org.apache.commons.lang3.Validate.notEmpty;
import static org.apache.commons.lang3.Validate.notNull;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * The {@link UnboundedNodeSupplier} collects an arbitrary number of filter nodes and converts them
 * to a single node. This is useful for logic operators and functions.
 *
 * @param <T> the type being used to represent a node in the filter tree.
 */
class UnboundedNodeSupplier<T> implements NodeSupplier<T> {
  private final NodeReducer<T> reducer;
  private final NodeSupplier<T> parent;
  private List<T> args;

  UnboundedNodeSupplier(final NodeReducer<T> reducer, final NodeSupplier<T> parent) {
    notNull(reducer);
    this.reducer = reducer;
    this.parent = parent;
    this.args = new ArrayList<>();
  }

  @Nullable
  @Override
  public NodeSupplier<T> getParent() {
    return parent;
  }

  @Override
  public void setNext(T node) {
    notNull(node);
    args.add(node);
  }

  @Override
  public T get() {
    notEmpty(args);
    return reducer.apply(args);
  }
}
