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
package org.codice.ddf.platform.util;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Provides path-generation functionality. */
public class PathBuilder {
  private final String root;

  private final String[] subElements;

  public PathBuilder(String root) {
    this(root, (String[]) null);
  }

  public PathBuilder(String root, String... subElements) {
    this.root = root;
    this.subElements = subElements;
  }

  public Path build() {
    if (root == null) {
      throw new IllegalArgumentException("Path cannot be constructed with no root element");
    }

    if (subElements != null) {
      return Paths.get(root, subElements);
    } else {
      return Paths.get(root);
    }
  }
}
