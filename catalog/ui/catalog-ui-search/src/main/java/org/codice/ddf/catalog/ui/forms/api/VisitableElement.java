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

import javax.annotation.Nullable;

/**
 * Can be visited by a {@link org.codice.ddf.catalog.ui.forms.api.FilterVisitor2}. Currently
 * visitability is coupled to JAXB as a result of the Filter 2.0 binding implementation used.
 *
 * <p><i>This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library.</i>
 */
public interface VisitableElement<T> {

  String getName();

  @Nullable
  String getFunctionName();

  T getValue();

  void accept(FilterVisitor2 visitor);
}
