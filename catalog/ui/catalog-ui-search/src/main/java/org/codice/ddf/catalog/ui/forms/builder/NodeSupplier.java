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

import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * {@link NodeSupplier}s are used by implementations of {@link
 * org.codice.ddf.catalog.ui.filter.FlatFilterBuilder} to decouple how a filter expression is built
 * from the data that should go inside. They are intended to be intermediate filter node builders
 * that can help simplify the complexities of building a hierarchical structure from a flat API.
 *
 * @param <T> the type being used to represent a node in the filter tree.
 */
interface NodeSupplier<T> extends Supplier<T> {

  /**
   * Given that Filter 2.0 supports composition of {@code fes:Function}s, the nesting of {@link
   * NodeSupplier}s can be used to support such behavior. The notion here is that any supplier that
   * has a parent is simply building up a value that will be returned to that parent.
   *
   * @return the supplier waiting on this one to finish up.
   */
  @Nullable
  NodeSupplier<T> getParent();

  /**
   * Add another child node to the parent node being built. Behavior of this method, including input
   * validation and schema enforcement, will always be implementation specific.
   *
   * @param node the node to add.
   */
  void setNext(T node);
}
