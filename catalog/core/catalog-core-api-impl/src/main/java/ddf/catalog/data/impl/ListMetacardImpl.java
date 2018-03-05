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
package ddf.catalog.data.impl;

import ddf.catalog.data.Metacard;
import java.util.Collections;

public class ListMetacardImpl extends MetacardImpl {
  private static final ListMetacardTypeImpl TYPE = new ListMetacardTypeImpl();

  public ListMetacardImpl() {
    super(TYPE);
    setTags(Collections.singleton(ListMetacardTypeImpl.LIST_TAG));
  }

  public ListMetacardImpl(String title) {
    this();
    setTitle(title);
  }

  public ListMetacardImpl(Metacard wrappedMetacard) {
    super(wrappedMetacard, TYPE);
    setTags(Collections.singleton(ListMetacardTypeImpl.LIST_TAG));
  }

  public static ListMetacardImpl from(Metacard metacard) {
    return new ListMetacardImpl(metacard);
  }
}
