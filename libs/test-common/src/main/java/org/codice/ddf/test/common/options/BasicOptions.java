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
package org.codice.ddf.test.common.options;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.codice.ddf.test.common.configurators.PortFinder;

/** Base class for common test options logic. */
public abstract class BasicOptions {

  private static final Path CONFIGURATION_LOG_PATH = Paths.get("target", "test-configuration.log");

  private static final PortFinder PORT_FINDER = new PortFinder();

  public static void recordConfiguration(String format, String... args) {
    try {
      Files.write(
          CONFIGURATION_LOG_PATH,
          String.format(format + System.lineSeparator(), args).getBytes(),
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static PortFinder getPortFinder() {
    return PORT_FINDER;
  }
}
