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
import org.apache.commons.lang3.ClassUtils;

/**
 * Base interface for all configuration objects. Configurations will either extends the {@link
 * ConfigSingleton} or {@link ConfigGroup} sub-interfaces based on whether they support only a
 * single instance or multiple instances respectively.
 */
public interface Config {
  /**
   * Gets the type of configuration object this is. The type of configuration objects is defined by
   * the interfaces in the configuration hierarchy that are annotated using the {@link ConfigType}
   * annotation.
   *
   * @return the type of config object this is
   */
  public default Class<? extends Config> getType() {
    return Config.getType(getClass());
  }

  /**
   * Gets the version for this configuration.
   *
   * @return the version for this configuration
   */
  public String getVersion();

  /**
   * Gets the configuration object type that corresponds to a given configuration class or subclass.
   *
   * @param clazz the config object class (or subclass) for which to determine the corresponding
   *     type
   * @return the corresponding config object type
   * @throws IllegalArgumentException if <code>clazz</code> is not of any defined config type
   */
  public static Class<? extends Config> getType(Class<? extends Config> clazz) {
    // check the specified class if it is an interface
    // in addition to all the interfaces for the specified class
    return (Class<? extends Config>)
        Stream.concat(
                Stream.of(clazz).filter(Class::isInterface),
                ClassUtils.getAllInterfaces(clazz).stream())
            .filter(i -> i.isAnnotationPresent(ConfigType.class))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "configuration object classis not of any known type: " + clazz.getName()));
  }
}
