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
package org.codice.ddf.catalog.transform;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import java.util.List;
import java.util.Optional;

public interface TransformResponse {

  /** The returned metacard represents the entire input stream to the transformer. */
  Optional<Metacard> getParentMetacard();

  /**
   * The list of metacards represents items derived from the input stream that do not directly
   * reference a resource.
   */
  List<Metacard> getDerivedMetacards();

  /**
   * The list of content items represent items derived from the input stream that directly reference
   * a resource.
   */
  List<ContentItem> getDerivedContentItems();
}
