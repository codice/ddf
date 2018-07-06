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
package org.codice.ddf.config;

import java.util.stream.Stream;

/** Configuration event. */
public interface ConfigEvent {
  /**
   * Retrieves all new configuration objects that were added to the configuration service.
   *
   * @return a stream of all config objects that were added to the config service
   */
  public Stream<Config> addedConfigs();

  /**
   * Retrieves all configuration objects that were updated in the configuration service.
   *
   * @return a stream of all config objects that were updated in the config service
   */
  public Stream<Config> updatedConfigs();

  /**
   * Retrieves all configuration objects that were removed from the configuration service.
   *
   * @return a stream of all config objects that were removed from the config service
   */
  public Stream<Config> removedConfigs();
}
