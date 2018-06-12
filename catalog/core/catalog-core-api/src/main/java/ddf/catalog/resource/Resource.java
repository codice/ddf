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
package ddf.catalog.resource;

import ddf.catalog.data.BinaryContent;

/**
 * A Resource represents an item (e.g. image, video, document, etc.) that has been posted for
 * sharing. A {@link ddf.catalog.data.Metacard} is created to provide metadata about a Resource,
 * which can be accessed via a {@link ResourceReader}.
 */
public interface Resource extends BinaryContent {

  /**
   * Gets the name of the Resource. Examples of this are the name of a file or the title of the
   * Resource URL.
   *
   * @return the name of the Resource. Null if the name is unknown
   */
  public String getName();
}
