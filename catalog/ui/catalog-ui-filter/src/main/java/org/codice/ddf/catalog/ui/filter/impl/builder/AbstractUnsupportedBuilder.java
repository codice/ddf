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
package org.codice.ddf.catalog.ui.filter.impl.builder;

import org.codice.ddf.catalog.ui.filter.FlatFilterBuilder;

abstract class AbstractUnsupportedBuilder<T> implements FlatFilterBuilder<T> {
  @Override
  public FlatFilterBuilder not() {
    throw new UnsupportedOperationException("The <Not> element is currently not supported.");
  }

  @Override
  public FlatFilterBuilder like(
      boolean matchCase, String wildcard, String singleChar, String escape) {
    if (matchCase) {
      throw new UnsupportedOperationException(
          "The <PropertyIsLike matchCase=\"true\"> element is currently not supported.");
    }
    return this;
  }

  @Override
  public FlatFilterBuilder dwithin(double distance, String units) {
    throw new UnsupportedOperationException("The <DWithin> element is currently not supported.");
  }
}
