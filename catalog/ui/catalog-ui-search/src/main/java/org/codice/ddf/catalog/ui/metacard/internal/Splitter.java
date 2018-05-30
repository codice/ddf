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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.stream.Stream;

/** Split a resource into a stream of resources. */
public interface Splitter extends ServiceProperties {

  /**
   * Callers must call {@link Stream#close()} on the stream and must call {@link
   * StorableResource#close()} on each item in the stream.
   *
   * @param storableResource the resource to split
   * @param arguments any arguments to be used in the transformation. Keys are specific to each
   *     {@link Splitter} implementation
   * @return a stream of StorableResource
   * @throws IOException
   */
  Stream<StorableResource> split(
      StorableResource storableResource, Map<String, ? extends Serializable> arguments)
      throws IOException;
}
