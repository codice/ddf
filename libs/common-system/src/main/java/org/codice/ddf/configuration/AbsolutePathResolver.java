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
package org.codice.ddf.configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbsolutePathResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbsolutePathResolver.class);

  private final String path;

  public AbsolutePathResolver(String path) {
    this.path = path;
  }

  public String getPath() {

    if (path == null) {
      return null;
    }

    boolean trailingSeparator = path.endsWith(File.separator);

    Path absolutePath = Paths.get(path);

    if (!absolutePath.isAbsolute()) {
      if (System.getProperty("ddf.home") != null) {
        absolutePath = Paths.get(System.getProperty("ddf.home"), path);
      } else {
        LOGGER.warn(
            "Path {} is relative. System property ddf.home is not set, resolving path to: {} ",
            path,
            absolutePath.toAbsolutePath());
      }
    }

    String absolutePathStr = absolutePath.toAbsolutePath().toString();
    return trailingSeparator ? absolutePathStr + File.separator : absolutePathStr;
  }
}
