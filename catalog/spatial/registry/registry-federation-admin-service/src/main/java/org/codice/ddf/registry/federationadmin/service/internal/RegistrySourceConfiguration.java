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
package org.codice.ddf.registry.federationadmin.service.internal;

/** Interface for interacting with the source configuration handler/generator */
public interface RegistrySourceConfiguration {

  /**
   * Regenerates all sources from the current registry entries.
   *
   * @throws FederationAdminException If the registry entries could not be read from the catalog
   */
  void regenerateAllSources() throws FederationAdminException;

  /**
   * Regenerates the sources for a specific registry entry
   *
   * @param registryId The registryId of the registry entry to regenerate sources from
   * @throws FederationAdminException If the registry entry could not be read from the catalog
   */
  void regenerateOneSource(String registryId) throws FederationAdminException;
}
