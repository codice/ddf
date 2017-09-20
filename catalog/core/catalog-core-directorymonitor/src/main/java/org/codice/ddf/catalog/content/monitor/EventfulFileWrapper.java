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
package org.codice.ddf.catalog.content.monitor;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public class EventfulFileWrapper implements WatchEvent<Path> {

  private final Kind<Path> kind;

  private final Path context;

  private final int count;

  public EventfulFileWrapper(Kind<Path> kind, int count, Path context) {
    this.kind = kind;
    this.count = count;
    this.context = context;
  }

  @Override
  public Kind<Path> kind() {
    return this.kind;
  }

  @Override
  public int count() {
    return this.count;
  }

  @Override
  public Path context() {
    return this.context;
  }
}
