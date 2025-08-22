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
package org.codice.felix.cm.file;

import java.util.Optional;

public class FactoryPidParser {
  public record ParsedFactoryPid( // suppress checkstyle:MethodName TODO update checkstyle
      String factoryPid, String serviceName) {}

  /**
   * Attempt to parse a service PID into a factory PID and a service name, following the conventions
   * used by Felix's ConfigurationAdmin.
   *
   * @param pid the service PID to parse
   * @return an Optional containing the parsed parts, if the PID is a factory PID. If the PID is not
   *     a factory PID, then an empty Optional is returned.
   */
  public static Optional<ParsedFactoryPid> parseFactoryParts(String pid) {
    if (pid == null) {
      return Optional.empty();
    }

    // Check for the ~ used as the separator between the factory PID and the name by
    // ConfigurationAdmin's getFactoryConfiguration() methods.
    final var tilde = pid.indexOf('~');
    if (tilde > -1) {
      return Optional.of(new ParsedFactoryPid(pid.substring(0, tilde), pid.substring(tilde + 1)));
    }

    // Check for the - in the UUID appended to the factory PID by ConfigurationAdmin's
    // createFactoryConfiguration() methods.
    if (pid.contains("-")) {
      final var lastPeriod = pid.lastIndexOf('.');
      return lastPeriod > -1
          ? Optional.of(
              new ParsedFactoryPid(pid.substring(0, lastPeriod), pid.substring(lastPeriod + 1)))
          : Optional.empty();
    }

    return Optional.empty();
  }
}
