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

/**
 * Base interface for all configuration classes that can have multiple instances. Instance
 * identifiers are unique within a given type of configuration group object.
 */
public interface ConfigGroup extends Config {
  /**
   * Gets the type of group configuration object this is.
   *
   * @return the type of group config object this is
   */
  @Override
  public default Class<? extends ConfigGroup> getType() {
    return (Class<? extends ConfigGroup>) Config.getType(getClass());
  }

  /**
   * Gets the unique instance identifier within the group configuration.
   *
   * @return the unique instance id for this config object within the corresponding group
   */
  public String getId();
}
