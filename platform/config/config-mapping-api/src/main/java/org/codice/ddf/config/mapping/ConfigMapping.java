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
package org.codice.ddf.config.mapping;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * The ConfigMapping interface provides an interface to a given mapping from which one can resolve
 * the mapped properties.
 *
 * <p>A configuration mapping is identified using a unique name and an optional instance.
 */
public interface ConfigMapping {
  /**
   * Gets the identifier for this configuration mapping.
   *
   * @return the id for this config mapping
   */
  public Id getId();

  /**
   * Resolves this configuration mapping by re-evaluating all internal rules.
   *
   * @return a map corresponding to all mapped properties and their current values after
   *     re-evaluating any internal rules defined
   * @throws ConfigMappingException if a failure occurred while resolving this config mapping
   */
  public Map<String, Object> resolve() throws ConfigMappingException;

  /** Configuration mapping identifier. */
  public static class Id {
    private final String name;

    @Nullable private final String instance;

    private Id(String name, @Nullable String instance) {
      this.name = name;
      this.instance = instance;
    }

    /**
     * Gets the name for the referenced configuration mapping.
     *
     * @return the name for the config mapping
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the instance for the referenced configuration mapping if the mapping can have multiple
     * instances (e.g. a config mapping for a managed service factory).
     *
     * @return the optional instance for the config mapping
     */
    public Optional<String> getInstance() {
      return Optional.ofNullable(instance);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, instance);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj instanceof Id) {
        final Id id = (Id) obj;

        return name.equals(id.name) && Objects.equals(instance, id.instance);
      }
      return false;
    }

    @Override
    public String toString() {
      return (instance != null) ? name + '-' + instance : name;
    }

    public static Id of(String name) {
      return new Id(name, null);
    }

    public static Id of(String name, String instance) {
      return new Id(name, instance);
    }
  }
}
