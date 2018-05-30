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
package org.codice.ddf.catalog.ui.metacard.impl;

import java.util.List;
import javax.activation.MimeType;
import org.codice.ddf.catalog.ui.metacard.internal.Splitter;
import org.codice.ddf.catalog.ui.metacard.internal.SplitterLocator;

public class SplitterLocatorImpl extends BaseLocator implements SplitterLocator {

  @Override
  public List<Splitter> find(MimeType mimeType) {
    return findServices(Splitter.class, null, t -> filterByMimeType(t, mimeType));
  }
}
