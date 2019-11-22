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
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbsolutePathResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbsolutePathResolver.class);

  private final String path;

  public AbsolutePathResolver(@Nullable String path) {
    this.path = path;
  }

  /**
   * This method will attempt to convert the already given path into an absolute path: first using
   * the system property {@code ddf.home} and then using {@code Path}'s {@code toAbsolutePath}'s
   * method
   *
   * @return Absolute path as a String
   */
  public String getPath() {
    if (System.getProperty("ddf.home") == null) {
      LOGGER.warn("System property ddf.home is not set");
    }
    return getPath(System.getProperty("ddf.home"));
  }

  /**
   * This method will attempt to convert the already given path into an absolute path: first by
   * appending the given {@code rootPath}, then by using {@code Path}'s {@code toAbsolutePath}'s
   * method
   *
   * @param rootPath String to append to path
   * @return Absolute path as a String
   */
  public String getPath(String rootPath) {
    if (path == null) {
      return null;
    }

    boolean trailingSeparator = path.endsWith(File.separator);

    Path absolutePath = Paths.get(path);

    if (!absolutePath.isAbsolute()) {
      if (StringUtils.isNotBlank(rootPath)) {
        absolutePath = Paths.get(rootPath, path);
      } else {
        LOGGER.debug(
            "Root path is blank. Resolving relative path [{}] to: {}",
            path,
            LogSanitizer.sanitize(absolutePath.toAbsolutePath()));
      }
    }

    String absolutePathStr = absolutePath.toAbsolutePath().toString();
    return trailingSeparator ? absolutePathStr + File.separator : absolutePathStr;
  }
}
