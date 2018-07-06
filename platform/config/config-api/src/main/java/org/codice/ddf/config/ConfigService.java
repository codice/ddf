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

import java.util.Optional;
import java.util.stream.Stream;

/** Service interface for configuration. */
public interface ConfigService {
  /**
   * Retrieves a singleton configuration of a given type.
   *
   * @param <T> the class (or subclass) of singleton config to retrieve
   * @param clazz the class (or subclass) of singleton config to retrieve
   * @return a config object of the corresponding type or empty if it doesn't exist
   */
  public <T extends ConfigSingleton> Optional<T> get(Class<T> clazz);

  /**
   * Retrieves a specific configuration instance of a given type of configuration group.
   *
   * @param <T> the class (or subclass) of group config to retrieve an instance for
   * @param clazz the class (or subclass) of group config to retrieve an instance for
   * @param id the unique instance id for the group config object to retrieve
   * @return a group config object of the corresponding type and f the given id or empty if none
   *     exist
   */
  public <T extends ConfigGroup> Optional<T> get(Class<T> clazz, String id);

  /**
   * Retrieves all instances of a given type of configuration group.
   *
   * @param <T> the class (or subclass) of group config to retrieve all instances for
   * @param clazz the class (or subclass) of group config to retrieve all instances for
   * @return a stream of all group config objects of the corresponding type that are instances of
   *     the given class
   */
  public <T extends ConfigGroup> Stream<T> configs(Class<T> clazz);
}
