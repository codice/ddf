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
package org.codice.ddf.catalog.ui.metacard.internal;

import java.util.List;
import javax.activation.MimeType;

/** Locate {@link Splitter} services based on the mime-type of the content being split. */
public interface SplitterLocator {

  /**
   * Find the {@link Splitter}s that can handle the given mime-type.
   *
   * @param mimeType the mime-type of the content to be split
   * @return list of splitters
   */
  List<Splitter> find(MimeType mimeType);
}
