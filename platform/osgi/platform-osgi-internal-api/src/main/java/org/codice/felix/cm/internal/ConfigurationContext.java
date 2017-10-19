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
package org.codice.felix.cm.internal;

import java.io.File;
import java.util.Dictionary;
import javax.annotation.Nullable;

/**
 * The purpose of the {@link ConfigurationContext} is to provide an abstraction for the oddities of
 * Apache Felix, and isolate only a single place where an implementation may need to change when
 * upgrading versions of Karaf.
 *
 * @implNote KARAF UPGRADE
 */
public interface ConfigurationContext {

  /** @return the unique identifier for the configuration. */
  String getServicePid();

  /**
   * @return a {@link File} in the {@code etc} directory that defines this config if and only if the
   *     config was created by the Felix directory watcher. Returns {@code null} otherwise.
   */
  @Nullable
  File getConfigFile();

  /**
   * @return a dictionary of properties only relevant to the configuration itself, completely free
   *     of Felix internal control values.
   */
  Dictionary<String, Object> getSanitizedProperties();
}
