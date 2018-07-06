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
package org.codice.ddf.config.mapping.impl;

import java.util.Objects;
import org.codice.ddf.config.mapping.ConfigMapping;
import org.codice.ddf.config.mapping.ConfigMappingEvent;

/** Configuration mapping event implementation. */
public class ConfigMappingEventImpl implements ConfigMappingEvent {
  private final ConfigMappingEvent.Type type;

  private final ConfigMapping mapping;

  public ConfigMappingEventImpl(ConfigMappingEvent.Type type, ConfigMapping mapping) {
    this.type = type;
    this.mapping = mapping;
  }

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public ConfigMapping getMapping() {
    return mapping;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, mapping);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof ConfigMappingEventImpl) {
      final ConfigMappingEventImpl event = (ConfigMappingEventImpl) obj;

      return (type == event.type) && mapping.equals(event.mapping);
    }
    return false;
  }

  @Override
  public String toString() {
    return "ConfigMappingEventImpl[" + type + ", " + mapping + "]";
  }
}
