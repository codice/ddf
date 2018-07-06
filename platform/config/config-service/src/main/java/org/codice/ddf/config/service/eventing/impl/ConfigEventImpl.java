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
package org.codice.ddf.config.service.eventing.impl;

import java.util.Set;
import java.util.stream.Stream;
import org.codice.ddf.config.Config;
import org.codice.ddf.config.ConfigEvent;

public class ConfigEventImpl implements ConfigEvent {

  private final Set<Config> addedConfigs;

  private final Set<Config> updatedConfigs;

  private final Set<Config> removedConfigs;

  public ConfigEventImpl(
      Set<Config> addedConfigs, Set<Config> updatedConfigs, Set<Config> removedConfigs) {
    this.addedConfigs = addedConfigs;
    this.updatedConfigs = updatedConfigs;
    this.removedConfigs = removedConfigs;
  }

  @Override
  public Stream<Config> addedConfigs() {
    return addedConfigs.stream();
  }

  @Override
  public Stream<Config> updatedConfigs() {
    return updatedConfigs.stream();
  }

  @Override
  public Stream<Config> removedConfigs() {
    return removedConfigs.stream();
  }

  @Override
  public String toString() {
    return "ConfigEventImpl[added="
        + addedConfigs
        + ", updated="
        + updatedConfigs
        + ", removed="
        + removedConfigs
        + "]";
  }
}
